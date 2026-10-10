package com.xiaoyiluck.meowtools.module.impl;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
class FastLeafDecayModuleTest {
    @Test void duplicateCandidatesAndCapacityCannotExceedDecayBudget() {
        var plugin = mock(MeowTools.class); var config = new YamlConfiguration();
        config.set("server_settings.fast_leaf_decay.max_queue_size", 2);
        config.set("server_settings.fast_leaf_decay.max_decay_per_tick", 1);
        when(plugin.getConfig()).thenReturn(config);
        var world = mock(World.class); var id = UUID.randomUUID(); when(world.getUID()).thenReturn(id);
        when(world.getMinHeight()).thenReturn(-64); when(world.getMaxHeight()).thenReturn(320);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        var block = mock(Block.class); var leaves = mock(Leaves.class); when(leaves.getDistance()).thenReturn(7);
        when(block.getBlockData()).thenReturn(leaves); when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        var scheduler = mock(BukkitScheduler.class); var task = mock(BukkitTask.class);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);
        AtomicInteger tick = new AtomicInteger(0);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getCurrentTick).thenAnswer(call -> tick.get());
            bukkit.when(() -> Bukkit.getWorld(id)).thenReturn(world);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            var module = new FastLeafDecayModule(plugin); module.reload();
            module.enqueue(world, 0, 64, 0); module.enqueue(world, 0, 64, 0);
            module.enqueue(world, 1, 64, 0); module.enqueue(world, 2, 64, 0);
            module.processQueue(); verify(block, never()).breakNaturally();
            tick.set(2); module.processQueue(); verify(block, times(1)).breakNaturally();
            module.processQueue(); verify(block, times(2)).breakNaturally();
            module.processQueue(); verify(block, times(2)).breakNaturally(); verify(task).cancel();
        }
    }
    @Test void unloadedChunksAreNeverRead() {
        var plugin = mock(MeowTools.class); when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        var world = mock(World.class); when(world.getMinHeight()).thenReturn(-64); when(world.getMaxHeight()).thenReturn(320);
        var module = new FastLeafDecayModule(plugin); module.reload(); module.enqueue(world, 0, 64, 0);
        verify(world, never()).getBlockAt(anyInt(), anyInt(), anyInt());
    }
}
