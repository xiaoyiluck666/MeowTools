package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.*;

public class CommandBlockModule implements Module, Listener {
    private final MeowTools plugin;
    private final Set<String> blocked = new HashSet<>();
    private boolean enabled;
    private Component message;
    public CommandBlockModule(MeowTools plugin) { this.plugin = plugin; }
    @Override public void enable() { reload(); Bukkit.getPluginManager().registerEvents(this, plugin); }
    @Override public void disable() { HandlerList.unregisterAll(this); }
    @Override public String getName() { return "CommandBlockModule"; }
    @Override public void reload() {
        var config = plugin.getConfig().getConfigurationSection("server_settings.blocked_commands");
        enabled = config != null && config.getBoolean("enabled", true);
        blocked.clear();
        message = null;
        if (config == null) return;
        for (String command : config.getStringList("list")) {
            String normalized = command.strip().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
            if (!normalized.isBlank()) blocked.add(normalized);
        }
        String text = config.getString("message", "");
        if (!text.isEmpty() && !text.equalsIgnoreCase("none")) {
            message = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
    }

    boolean blocks(String raw) {
        String command = raw.startsWith("/") ? raw.substring(1) : raw;
        command = command.stripLeading().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (blocked.contains(command)) return true;
        int namespace = command.indexOf(':');
        return namespace >= 0 && blocked.contains(command.substring(namespace + 1));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        if (!enabled || event.getPlayer().isOp()
                || event.getPlayer().hasPermission("meowtools.bypass.blocked_commands")) return;
        if (blocks(event.getMessage())) {
            event.setCancelled(true);
            if (message != null) event.getPlayer().sendMessage(message);
        }
    }
}
