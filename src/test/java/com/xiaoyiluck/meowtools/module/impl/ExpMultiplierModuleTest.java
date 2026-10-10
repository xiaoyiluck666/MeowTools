package com.xiaoyiluck.meowtools.module.impl;
import com.xiaoyiluck.meowtools.MeowTools;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpMultiplierModuleTest {
    ExpMultiplierModule module;
    Player player;
    @BeforeEach void setup() {
        MeowTools plugin = mock(MeowTools.class);
        var config = new YamlConfiguration();
        config.set("server_settings.exp_multiplier.enabled", true);
        config.set("server_settings.exp_multiplier.optimize_nearby", true);
        config.set("server_settings.exp_multiplier.custom.vip", 1.5);
        when(plugin.getConfig()).thenReturn(config);
        module = new ExpMultiplierModule(plugin); module.reload();
        player = mock(Player.class);
        when(player.getEffectivePermissions()).thenReturn(Set.of());
    }
    @Test void deniedAndNonFinitePermissionsAreIgnored() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(
            new PermissionAttachmentInfo(player, "meowtools.exp.9", null, false),
            new PermissionAttachmentInfo(player, "meowtools.exp.infinity", null, true),
            new PermissionAttachmentInfo(player, "meowtools.exp.2.5", null, true)));
        assertEquals(2.5, module.getMultiplier(player));
    }
    @Test void explicitZeroMultiplierAndIntegerSaturation() {
        when(player.getEffectivePermissions()).thenReturn(Set.of(new PermissionAttachmentInfo(player, "meowtools.exp.0", null, true)));
        var event = new PlayerExpChangeEvent(player, 9); module.onExpChange(event); assertEquals(0, event.getAmount());
        when(player.getEffectivePermissions()).thenReturn(Set.of(new PermissionAttachmentInfo(player, "meowtools.exp.1000", null, true)));
        event = new PlayerExpChangeEvent(player, Integer.MAX_VALUE); module.onExpChange(event);
        assertEquals(Integer.MAX_VALUE, event.getAmount());
    }
    private ExperienceOrb orb(int experience, int count) {
        var orb = mock(ExperienceOrb.class);
        when(orb.isValid()).thenReturn(true); when(orb.getExperience()).thenReturn(experience); when(orb.getCount()).thenReturn(count);
        var data = mock(PersistentDataContainer.class); when(data.isEmpty()).thenReturn(true);
        when(orb.getPersistentDataContainer()).thenReturn(data);
        return orb;
    }
    @Test void mergesStackedExperienceWithoutBypassingVanillaPickup() {
        var target = orb(5, 2); var source = orb(3, 4);
        when(target.getNearbyEntities(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(source));
        PluginManager manager = mock(PluginManager.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            module.onExpPickup(new PlayerPickupExperienceEvent(player, target));
        }
        verify(target).setExperience(22); verify(target).setCount(1); verify(source).remove();
        verify(player, never()).giveExp(anyInt());
    }
    @Test void cancelledMergeRetainsBothOrbs() {
        var target = orb(5, 1); var source = orb(3, 1);
        when(target.getNearbyEntities(anyDouble(), anyDouble(), anyDouble())).thenReturn(List.of(source));
        PluginManager manager = mock(PluginManager.class);
        doAnswer(call -> { ((ExperienceOrbMergeEvent) call.getArgument(0)).setCancelled(true); return null; }).when(manager).callEvent(any());
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            module.onExpPickup(new PlayerPickupExperienceEvent(player, target));
        }
        verify(source, never()).remove(); verify(target, never()).setExperience(anyInt());
    }
    @Test void cancelledPickupDoesNotScanOrbs() {
        var target = orb(5, 1);
        var event = new PlayerPickupExperienceEvent(player, target); event.setCancelled(true);
        module.onExpPickup(event);
        verify(target, never()).getNearbyEntities(anyDouble(), anyDouble(), anyDouble());
    }
}
