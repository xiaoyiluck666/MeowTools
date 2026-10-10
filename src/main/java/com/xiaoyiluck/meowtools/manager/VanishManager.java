package com.xiaoyiluck.meowtools.manager;

import com.xiaoyiluck.meowtools.MeowTools;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishManager implements Listener {

    private final MeowTools plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final Set<UUID> vanishNightVisionPlayers = new HashSet<>();

    public VanishManager(MeowTools plugin) {
        this.plugin = plugin;
    }

    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
        HandlerList.unregisterAll(this);

        for (UUID uuid : new HashSet<>(vanishedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                activeBossBars.remove(uuid);
                continue;
            }

            showPlayer(player);

            BossBar bossBar = activeBossBars.remove(uuid);
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }

            removeNightVision(player);
        }

        vanishedPlayers.clear();
        activeBossBars.clear();
        vanishNightVisionPlayers.clear();
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public void setVanish(Player player, boolean state) {
        if (isVanished(player) == state) return;
        if (state) {
            vanishedPlayers.add(player.getUniqueId());
            hidePlayer(player);
            player.sendMessage(plugin.getLanguageManager().getMessage("vanish_on"));
            
            // Show BossBar
            BossBar bossBar = BossBar.bossBar(
                    plugin.getLanguageManager().getMessage("vanish_bossbar"),
                    1.0f,
                    BossBar.Color.BLUE,
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            activeBossBars.put(player.getUniqueId(), bossBar);
            
            // Add Night Vision if enabled in config
            if (plugin.getConfig().getBoolean("vanish_settings.night_vision", true)) {
                addNightVision(player);
            }
        } else {
            vanishedPlayers.remove(player.getUniqueId());
            showPlayer(player);
            player.sendMessage(plugin.getLanguageManager().getMessage("vanish_off"));
            
            // Remove BossBar
            BossBar bossBar = activeBossBars.remove(player.getUniqueId());
            if (bossBar != null) {
                player.hideBossBar(bossBar);
            }
            
            // Remove Night Vision
            removeNightVision(player);
        }
    }

    private void addNightVision(Player player) {
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false));
        vanishNightVisionPlayers.add(player.getUniqueId());
    }

    private void removeNightVision(Player player) {
        if (vanishNightVisionPlayers.remove(player.getUniqueId())) {
            PotionEffect effect = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (effect != null && effect.getDuration() == -1 && effect.getAmplifier() == 0
                    && !effect.isAmbient() && !effect.hasParticles() && !effect.hasIcon()) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }
    }

    public void toggleVanish(Player player) {
        setVanish(player, !isVanished(player));
    }

    private void hidePlayer(Player target) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("meowtools.vanish.see")) {
                online.hidePlayer(plugin, target);
            }
        }
    }

    private void showPlayer(Player target) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, target);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();

        // 1. Hide existing vanished players from the joiner
        for (UUID uuid : vanishedPlayers) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && !joiner.hasPermission("meowtools.vanish.see")) {
                joiner.hidePlayer(plugin, vanishedPlayer);
            }
        }

        // 2. If joiner was vanished (persisted? No, we don't persist currently), do nothing.
        // If we wanted to persist, we'd need to save to file.
        // For now, vanish resets on quit/restart as per standard simple implementation.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeNightVision(event.getPlayer());
        vanishedPlayers.remove(event.getPlayer().getUniqueId());
        BossBar bar = activeBossBars.remove(event.getPlayer().getUniqueId());
        if (bar != null) event.getPlayer().hideBossBar(bar);
        showPlayer(event.getPlayer());
    }
    public void reload() {
        for (UUID uuid : vanishedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            hidePlayer(player);
            BossBar bar = activeBossBars.get(uuid);
            if (bar != null) bar.name(plugin.getLanguageManager().getMessage("vanish_bossbar"));
            if (plugin.getConfig().getBoolean("vanish_settings.night_vision", true)) addNightVision(player);
            else removeNightVision(player);
        }
    }
}
