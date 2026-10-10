package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitModule implements Module, Listener {

    private final MeowTools plugin;

    public JoinQuitModule(MeowTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
    }

    @Override
    public void reload() {
        // Config reload handled by plugin
    }

    @Override
    public String getName() {
        return "JoinQuitModule";
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        handleMessage(event.getPlayer(), true, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        handleMessage(event.getPlayer(), false, event);
    }

    private void handleMessage(Player player, boolean isJoin, Object event) {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("server_settings.join_quit_messages");
        
        // If entire feature is disabled in config, do nothing (let vanilla handle it, or other plugins)
        if (config == null || !config.getBoolean("enabled", true)) {
            return;
        }

        // Check separate toggles
        boolean show = isJoin ? config.getBoolean("show_join", true) : config.getBoolean("show_quit", true);
        if (!show) {
            // User explicitly wants to hide it
            if (isJoin) {
                ((PlayerJoinEvent) event).joinMessage(null);
            } else {
                ((PlayerQuitEvent) event).quitMessage(null);
            }
            return;
        }

        // If user wants to "silence" messages completely, they might set enabled: true but messages to empty/none.
        // However, if enabled is true, we assume we are taking control.
        // We set the event message to null first to suppress vanilla, then broadcast our own if needed?
        // Actually, PlayerJoinEvent.setJoinMessage(null) suppresses the message.

        String message = getCustomMessage(player, config, isJoin);

        Component component = null;
        if (message != null && !message.isEmpty() && !message.equalsIgnoreCase("none")) {
            message = message.replace("{player}", player.getName());
            component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }

        if (isJoin) {
            ((PlayerJoinEvent) event).joinMessage(component);
        } else {
            ((PlayerQuitEvent) event).quitMessage(component);
        }
    }

    private String getCustomMessage(Player player, ConfigurationSection config, boolean isJoin) {
        String type = isJoin ? "join" : "quit";
        String customSection = isJoin ? "custom_join" : "custom_quit";

        // Check custom permissions first
        if (config.contains(customSection)) {
            ConfigurationSection custom = config.getConfigurationSection(customSection);
            if (custom != null) {
                for (String key : custom.getKeys(false)) {
                    String permission = "meowtools." + type + "." + key;
                    if (player.hasPermission(permission)) {
                        return custom.getString(key);
                    }
                }
            }
        }

        // Fallback to default
        return config.getString(type);
    }
}
