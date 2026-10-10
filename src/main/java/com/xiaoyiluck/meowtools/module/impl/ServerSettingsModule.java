package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.Module;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class ServerSettingsModule implements Module, Listener {

    private final MeowTools plugin;
    private final Map<UUID, Long> lastDeathTime = new HashMap<>();
    private final Map<UUID, Component> lastDeathMessage = new HashMap<>();
    private boolean deathMessageControlEnabled = true;
    private long deathMessageCooldownMillis = 10_000L;
    private boolean deathMessageSameOnly = true;

    // Cached Explosion Settings
    private boolean tntBreakBlocks = true;
    private boolean tntDamageEntities = true;
    private boolean creeperBreakBlocks = true;
    private boolean creeperDamageEntities = true;
    private boolean otherBreakBlocks = true;
    private boolean otherDamageEntities = true;
    private static final Set<NamespacedKey> EXPLOSION_DAMAGE_TYPES = Set.of(
            NamespacedKey.minecraft("explosion"), NamespacedKey.minecraft("player_explosion"),
            NamespacedKey.minecraft("bad_respawn_point"), NamespacedKey.minecraft("fireworks"));

    public ServerSettingsModule(MeowTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        lastDeathTime.clear();
        lastDeathMessage.clear();
    }

    @Override
    public void reload() {
        loadConfig();
    }
    
    private void loadConfig() {
        ConfigurationSection explosion = plugin.getConfig().getConfigurationSection("server_settings.explosion_settings");
        if (explosion != null) {
            tntBreakBlocks = explosion.getBoolean("tnt.break_blocks", true);
            tntDamageEntities = explosion.getBoolean("tnt.damage_entities", true);
            creeperBreakBlocks = explosion.getBoolean("creeper.break_blocks", true);
            creeperDamageEntities = explosion.getBoolean("creeper.damage_entities", true);
            otherBreakBlocks = explosion.getBoolean("other.break_blocks", true);
            otherDamageEntities = explosion.getBoolean("other.damage_entities", true);
        } else {
            tntBreakBlocks = true;
            tntDamageEntities = true;
            creeperBreakBlocks = true;
            creeperDamageEntities = true;
            otherBreakBlocks = true;
            otherDamageEntities = true;
        }

        ConfigurationSection deathControl = plugin.getConfig().getConfigurationSection("server_settings.death_message_control");
        if (deathControl != null) {
            deathMessageControlEnabled = deathControl.getBoolean("enabled", true);
            int cooldownSeconds = deathControl.getInt("cooldown_seconds", 10);
            if (cooldownSeconds < 0) {
                cooldownSeconds = 0;
            }
            deathMessageCooldownMillis = cooldownSeconds * 1000L;
            deathMessageSameOnly = deathControl.getBoolean("same_message_only", true);
        } else {
            deathMessageControlEnabled = true;
            deathMessageCooldownMillis = 10_000L;
            deathMessageSameOnly = true;
        }
    }

    @Override
    public String getName() {
        return "ServerSettingsModule";
    }

    // --- Death Handling ---

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handleDeathMessageControl(event);
    }

    private void handleDeathMessageControl(PlayerDeathEvent event) {
        if (!deathMessageControlEnabled) {
            return;
        }

        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        Component message = event.deathMessage();
        if (message == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTime = lastDeathTime.get(uuid);
        Component lastMessage = lastDeathMessage.get(uuid);

        if (lastTime != null) {
            long delta = now - lastTime;
            boolean sameMessage = lastMessage != null && lastMessage.equals(message);
            if (delta < deathMessageCooldownMillis) {
                if (!deathMessageSameOnly || sameMessage) {
                    event.deathMessage(null);
                    return;
                }
            }
        }

        lastDeathTime.put(uuid, now);
        lastDeathMessage.put(uuid, message);
    }

    // --- Explosion Handling ---

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            if (!tntBreakBlocks) event.blockList().clear();
        } else if (entity instanceof Creeper) {
            if (!creeperBreakBlocks) event.blockList().clear();
        } else {
            if (!otherBreakBlocks) event.blockList().clear();
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Block explosions (beds, anchors) fall under 'other'
        if (!otherBreakBlocks) {
            event.blockList().clear();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                // Paper can expose source-less World#createExplosion damage as CUSTOM.
                && !EXPLOSION_DAMAGE_TYPES.contains(event.getDamageSource().getDamageType().getKey())) return;
        Entity damager = event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager()
                : event.getDamageSource().getDirectEntity();
        boolean allowed = damager instanceof TNTPrimed ? tntDamageEntities
                : damager instanceof Creeper ? creeperDamageEntities : otherDamageEntities;
        if (!allowed) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastDeathTime.remove(event.getPlayer().getUniqueId());
        lastDeathMessage.remove(event.getPlayer().getUniqueId());
    }
}
