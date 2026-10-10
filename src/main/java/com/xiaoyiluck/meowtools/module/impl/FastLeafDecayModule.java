package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class FastLeafDecayModule implements Module, Listener {
    private final MeowTools plugin;
    private final Queue<Candidate> queue = new ArrayDeque<>();
    private final Set<Position> queued = new HashSet<>();
    private BukkitTask task;
    private boolean enabled;
    private int budget;
    private int capacity;
    private record Position(UUID world, int x, int y, int z) { }
    private record Candidate(Position position, int readyTick, int attempts) { }

    public FastLeafDecayModule(MeowTools plugin) { this.plugin = plugin; }
    @Override public String getName() { return "FastLeafDecayModule"; }
    @Override public void enable() { reload(); Bukkit.getPluginManager().registerEvents(this, plugin); }
    @Override public void disable() {
        HandlerList.unregisterAll(this);
        stopTask();
        queue.clear();
        queued.clear();
    }
    @Override public void reload() {
        var config = plugin.getConfig();
        enabled = config.getBoolean("server_settings.fast_leaf_decay.enabled", true);
        budget = Math.clamp(config.getInt("server_settings.fast_leaf_decay.max_decay_per_tick", 10), 1, 1000);
        capacity = Math.clamp(config.getInt("server_settings.fast_leaf_decay.max_queue_size", 10000), 1, 100000);
        if (!enabled) { stopTask(); queue.clear(); queued.clear(); }
    }
    private void stopTask() { if (task != null) task.cancel(); task = null; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { handleBlockUpdate(event.getBlock()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) { handleBlockUpdate(event.getBlock()); }

    private void handleBlockUpdate(Block block) {
        if (!enabled || (!Tag.LOGS.isTagged(block.getType()) && !Tag.LEAVES.isTagged(block.getType()))) return;
        World world = block.getWorld();
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            if (dx == 0 && dy == 0 && dz == 0) continue;
            enqueue(world, block.getX() + dx, block.getY() + dy, block.getZ() + dz);
        }
    }

    void enqueue(World world, int x, int y, int z) {
        if (!enabled || queue.size() >= capacity || y < world.getMinHeight() || y >= world.getMaxHeight()
                || !world.isChunkLoaded(x >> 4, z >> 4)) return;
        Position position = new Position(world.getUID(), x, y, z);
        if (queued.contains(position)) return;
        Block leaf = world.getBlockAt(x, y, z);
        if (leaf.getBlockData() instanceof Leaves data && !data.isPersistent()) {
            queued.add(position);
            queue.add(new Candidate(position, Bukkit.getCurrentTick() + 2, 0));
            if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 1L, 1L);
        }
    }

    void processQueue() {
        for (int i = 0; enabled && i < budget && !queue.isEmpty(); i++) {
            Candidate candidate = queue.peek();
            if (candidate.readyTick() > Bukkit.getCurrentTick()) break;
            queue.remove();
            Position position = candidate.position();
            World world = Bukkit.getWorld(position.world());
            if (world == null || !world.isChunkLoaded(position.x() >> 4, position.z() >> 4)) {
                queued.remove(position);
                continue;
            }
            Block block = world.getBlockAt(position.x(), position.y(), position.z());
            if (!(block.getBlockData() instanceof Leaves leaves) || leaves.isPersistent()) {
                queued.remove(position);
                continue;
            }
            // Distance propagation happens after BlockBreakEvent; allow it to settle.
            if (leaves.getDistance() <= 6) {
                if (candidate.attempts() < 5 && queue.size() < capacity) {
                    queue.add(new Candidate(position, Bukkit.getCurrentTick() + 2, candidate.attempts() + 1));
                } else queued.remove(position);
                continue;
            }
            queued.remove(position);
            LeavesDecayEvent event = new LeavesDecayEvent(block);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled() && block.getBlockData() instanceof Leaves current
                    && !current.isPersistent() && current.getDistance() > 6) block.breakNaturally();
        }
        if (queue.isEmpty()) stopTask();
    }
}
