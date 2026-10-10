package com.xiaoyiluck.meowtools.module.impl;

import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.*;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class ServerSettingsModuleTest {
    private ServerSettingsModule module(boolean tnt, boolean creeper, boolean other) {
        var plugin = mock(MeowTools.class);
        var config = new YamlConfiguration();
        config.set("server_settings.explosion_settings.tnt.damage_entities", tnt);
        config.set("server_settings.explosion_settings.creeper.damage_entities", creeper);
        config.set("server_settings.explosion_settings.other.damage_entities", other);
        when(plugin.getConfig()).thenReturn(config);
        var module = new ServerSettingsModule(plugin); module.reload(); return module;
    }

    // Avoid bootstrapping Bukkit's static registry constants in a unit-test JVM.
    private DamageType type(NamespacedKey key) {
        return new DamageType() {
            @Override public NamespacedKey getKey() { return key; }
            @Override public String getTranslationKey() { return key.toString(); }
            @Override public DamageScaling getDamageScaling() { return DamageScaling.NEVER; }
            @Override public DamageEffect getDamageEffect() { return DamageEffect.HURT; }
            @Override public DeathMessageType getDeathMessageType() { return DeathMessageType.DEFAULT; }
            @Override public float getExhaustion() { return 0; }
        };
    }

    private EntityDamageEvent event(EntityDamageEvent.DamageCause cause, NamespacedKey key, Entity direct) {
        var source = mock(DamageSource.class);
        when(source.getDamageType()).thenReturn(type(key)); when(source.getDirectEntity()).thenReturn(direct);
        var event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(cause); when(event.getDamageSource()).thenReturn(source); return event;
    }

    @Test void sourceLessExplosionDamageIsBlockedEvenWhenPaperReportsCustom() {
        var module = module(false, false, false);
        for (String key : new String[]{"explosion", "player_explosion", "bad_respawn_point", "fireworks"}) {
            var event = event(EntityDamageEvent.DamageCause.CUSTOM, NamespacedKey.minecraft(key), null);
            module.onEntityDamage(event); verify(event).setCancelled(true);
        }
    }

    @Test void unrelatedCustomDamageAndForeignKeysRemainUnaffected() {
        var module = module(false, false, false);
        for (NamespacedKey key : new NamespacedKey[]{NamespacedKey.minecraft("generic"), new NamespacedKey("other", "explosion")}) {
            var event = event(EntityDamageEvent.DamageCause.CUSTOM, key, null);
            module.onEntityDamage(event); verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Test void directExplosionSourcesHonorIndependentSettings() {
        var module = module(false, true, false);
        var tnt = event(EntityDamageEvent.DamageCause.CUSTOM, NamespacedKey.minecraft("explosion"), mock(TNTPrimed.class));
        var creeper = event(EntityDamageEvent.DamageCause.CUSTOM, NamespacedKey.minecraft("explosion"), mock(Creeper.class));
        var block = event(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, NamespacedKey.minecraft("bad_respawn_point"), null);
        module.onEntityDamage(tnt); module.onEntityDamage(creeper); module.onEntityDamage(block);
        verify(tnt).setCancelled(true); verify(creeper, never()).setCancelled(anyBoolean()); verify(block).setCancelled(true);
    }
}
