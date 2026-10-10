package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import java.util.*;

public class ExpMultiplierModule implements Module, Listener {
    private final MeowTools plugin;
    private boolean enabled;
    private double defaultMultiplier;
    private final Map<String, Double> customMultipliers = new LinkedHashMap<>();
    private boolean optimizeNearby;
    private double optimizeRadius;
    private int maxMerge;

    public ExpMultiplierModule(MeowTools plugin) { this.plugin = plugin; }
    @Override public String getName() { return "exp_multiplier"; }
    @Override public void enable() {
        loadSettings();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    @Override public void disable() { HandlerList.unregisterAll(this); }
    @Override public void reload() { loadSettings(); }

    private void loadSettings() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("server_settings.exp_multiplier");
        enabled = config != null && config.getBoolean("enabled", true);
        customMultipliers.clear();
        if (config == null) return;
        defaultMultiplier = validMultiplier(config.getDouble("default_multiplier", 1.0), 1.0);
        optimizeNearby = config.getBoolean("optimize_nearby", true);
        double radius = config.getDouble("optimize_radius", 1.5);
        optimizeRadius = Double.isFinite(radius) ? Math.clamp(radius, 0, 4) : 1.5;
        maxMerge = Math.clamp(config.getInt("max_merge_per_pickup", 32), 0, 128);
        ConfigurationSection custom = config.getConfigurationSection("custom");
        if (custom != null) {
            for (String key : custom.getKeys(false)) {
                customMultipliers.put(key, validMultiplier(custom.getDouble(key), defaultMultiplier));
            }
        }
    }

    private static double validMultiplier(double value, double fallback) {
        return Double.isFinite(value) && value >= 0 && value <= 1000 ? value : fallback;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(PlayerExpChangeEvent event) {
        if (!enabled || event.getAmount() <= 0) return;
        event.setAmount((int) Math.min(Integer.MAX_VALUE,
                Math.round(event.getAmount() * getMultiplier(event.getPlayer()))));
    }

    // Merge the entities, then let vanilla perform pickup, Mending and exp events.
    // No giveExp/remove shortcut that bypasses the server's pickup pipeline.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExpPickup(PlayerPickupExperienceEvent event) {
        if (event.isCancelled() || !enabled || !optimizeNearby || maxMerge == 0 || optimizeRadius == 0) return;
        ExperienceOrb orb = event.getExperienceOrb();
        int merged = 0;
        for (var entity : orb.getNearbyEntities(optimizeRadius, optimizeRadius, optimizeRadius)) {
            if (merged >= maxMerge) break;
            if (!(entity instanceof ExperienceOrb source) || source.equals(orb)
                    || !canMerge(orb, source)) continue;
            long total = totalExperience(orb) + totalExperience(source);
            if (total <= 0 || total > Integer.MAX_VALUE) continue;
            var mergeEvent = new ExperienceOrbMergeEvent(orb, source);
            Bukkit.getPluginManager().callEvent(mergeEvent);
            if (mergeEvent.isCancelled() || !canMerge(orb, source)) continue;
            // Listeners can change orb values during the merge event.
            total = totalExperience(orb) + totalExperience(source);
            if (total <= 0 || total > Integer.MAX_VALUE) continue;
            orb.setExperience((int) total);
            orb.setCount(1);
            source.remove();
            merged++;
        }
    }

    private static long totalExperience(ExperienceOrb orb) {
        return (long) orb.getExperience() * orb.getCount();
    }

    private static boolean canMerge(ExperienceOrb target, ExperienceOrb source) {
        return target.isValid() && source.isValid() && target.getExperience() > 0 && source.getExperience() > 0
                && target.getCount() > 0 && source.getCount() > 0
                && target.getPersistentDataContainer().isEmpty() && source.getPersistentDataContainer().isEmpty()
                && target.getSpawnReason() == source.getSpawnReason()
                && Objects.equals(target.getTriggerEntityId(), source.getTriggerEntityId())
                && Objects.equals(target.getSourceEntityId(), source.getSourceEntityId());
    }

    double getMultiplier(Player player) {
        double maximum = -1;
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            if (!permission.getValue()) continue;
            String node = permission.getPermission().toLowerCase(Locale.ROOT);
            if (!node.startsWith("meowtools.exp.")) continue;
            try {
                double value = Double.parseDouble(node.substring("meowtools.exp.".length()));
                if (Double.isFinite(value) && value >= 0 && value <= 1000) maximum = Math.max(maximum, value);
            } catch (NumberFormatException ignored) { }
        }
        if (maximum >= 0) return maximum;
        for (var entry : customMultipliers.entrySet()) {
            if (player.hasPermission("meowtools.exp." + entry.getKey())) return entry.getValue();
        }
        return defaultMultiplier;
    }
}
