package com.xiaoyiluck.meowtools.module.impl;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class ShortcutModuleTest {
    @Test void disabledDirectionalBindingFallsBackAndCooldownCoalescesCommands() {
        var plugin = mock(MeowTools.class); var config = new YamlConfiguration();
        config.set("shortcuts.f.enabled", true); config.set("shortcuts.f.commands", List.of("say one", "say two"));
        config.set("shortcuts.look_up_f.enabled", false); config.set("shortcuts.cooldown_millis", 60000);
        when(plugin.getConfig()).thenReturn(config);
        var player = mock(Player.class); when(player.hasPermission("meowtools.use")).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(player.isOnline()).thenReturn(true); when(player.getName()).thenReturn("tester");
        when(player.getLocation()).thenReturn(new Location(null, 0, 64, 0, 0, -80));
        var event = mock(PlayerSwapHandItemsEvent.class); when(event.getPlayer()).thenReturn(player);
        var scheduler = mock(BukkitScheduler.class);
        doAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return null; }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            var module = new ShortcutModule(plugin); module.reload();
            module.onPlayerSwapHandItems(event); module.onPlayerSwapHandItems(event);
            verify(scheduler, times(1)).runTask(eq(plugin), any(Runnable.class));
            verify(player).chat("/say one"); verify(player).chat("/say two");
            verify(event, times(2)).setCancelled(true);
            config.set("shortcuts.f.enabled", false); module.reload();
            var unbound = mock(PlayerSwapHandItemsEvent.class); when(unbound.getPlayer()).thenReturn(player);
            module.onPlayerSwapHandItems(unbound); verify(unbound, never()).setCancelled(anyBoolean());
        }
    }
}
