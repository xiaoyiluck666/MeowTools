package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import java.util.*;

public class ShortcutModule implements Module, Listener {
    private final MeowTools plugin;
    private final Map<String, Shortcut> shortcuts = new HashMap<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private long cooldownMillis;
    private record Shortcut(List<String> commands, double threshold) { }
    public ShortcutModule(MeowTools plugin) { this.plugin = plugin; }
    @Override public String getName() { return "ShortcutModule"; }
    @Override public void enable() { reload(); Bukkit.getPluginManager().registerEvents(this, plugin); }
    @Override public void disable() { HandlerList.unregisterAll(this); lastUse.clear(); }
    @Override public void reload() {
        shortcuts.clear(); lastUse.clear();
        var config = plugin.getConfig().getConfigurationSection("shortcuts");
        if (config == null) return;
        cooldownMillis = Math.clamp(config.getLong("cooldown_millis", 250), 0, 60000);
        for (String key : config.getKeys(false)) {
            var section = config.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", false)) continue;
            List<String> commands = section.getStringList("commands").stream().filter(command -> !command.isBlank()).toList();
            double threshold = section.getDouble("threshold", key.contains("up") ? -45 : 45);
            if (!Double.isFinite(threshold)) threshold = key.contains("up") ? -45 : 45;
            if (!commands.isEmpty()) shortcuts.put(key, new Shortcut(commands, Math.clamp(threshold, -90, 90)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("meowtools.use")) return;
        String base = player.isSneaking() ? "shift_f" : "f";
        float pitch = player.getLocation().getPitch();
        Shortcut up = shortcuts.get("look_up_" + base), down = shortcuts.get("look_down_" + base);
        Shortcut selected = up != null && pitch < up.threshold() ? up
                : down != null && pitch > down.threshold() ? down : shortcuts.get(base);
        if (selected == null) return;
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long previous = lastUse.get(player.getUniqueId());
        if (previous != null && now - previous < cooldownMillis) return;
        lastUse.put(player.getUniqueId(), now);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            for (String command : selected.commands()) {
                String normalized = command.startsWith("/") ? command.substring(1) : command;
                player.chat("/" + normalized.replace("%player%", player.getName()));
            }
        });
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { lastUse.remove(event.getPlayer().getUniqueId()); }
}
