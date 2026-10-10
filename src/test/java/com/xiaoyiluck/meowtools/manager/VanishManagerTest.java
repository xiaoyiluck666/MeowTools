package com.xiaoyiluck.meowtools.manager;
import com.xiaoyiluck.meowtools.MeowTools;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class VanishManagerTest {
    @Test void repeatedOnCreatesOnlyOneBossBarAndOffRemovesIt() {
        var plugin = mock(MeowTools.class); var config = new YamlConfiguration();
        config.set("vanish_settings.night_vision", false); when(plugin.getConfig()).thenReturn(config);
        var language = mock(LanguageManager.class); when(plugin.getLanguageManager()).thenReturn(language);
        when(language.getMessage(anyString())).thenReturn(Component.empty());
        var player = mock(Player.class); when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            var manager = new VanishManager(plugin);
            manager.setVanish(player, true); manager.setVanish(player, true);
            assertTrue(manager.isVanished(player));
            verify(player, times(1)).showBossBar(any(BossBar.class));
            manager.setVanish(player, false); manager.setVanish(player, false);
            assertFalse(manager.isVanished(player));
            verify(player, times(1)).hideBossBar(any(BossBar.class));
        }
    }
}
