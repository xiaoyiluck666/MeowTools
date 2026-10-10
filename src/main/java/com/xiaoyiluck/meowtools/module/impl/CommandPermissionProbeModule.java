package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import com.xiaoyiluck.meowtools.utils.PermissionNodeResolver;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandPermissionProbeModule implements Module, Listener {

    private final MeowTools plugin;
    private boolean enabled;

    public CommandPermissionProbeModule(MeowTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
    }

    @Override
    public void reload() {
        enabled = plugin.getConfig().getBoolean("server_settings.permission_probe.enabled", false);
    }

    @Override
    public String getName() {
        return "CommandPermissionProbeModule";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        if (!enabled) return;

        String permissionNode = PermissionNodeResolver.detectDeniedPermission(event.getPlayer(), event.getMessage());
        if (permissionNode == null) {
            return;
        }

        plugin.getLogger().warning(
                "[PermissionDenied][CommandProbe] sender=" + event.getPlayer().getName()
                        + ", permission=" + permissionNode
                        + ", command=\"" + event.getMessage() + "\""
        );
    }
}
