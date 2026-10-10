package com.xiaoyiluck.meowtools.manager;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigManagerTest {
    @TempDir Path directory;
    private MeowTools plugin(String defaults) {
        MeowTools plugin = mock(MeowTools.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource("config.yml")).thenAnswer(call -> new ByteArrayInputStream(defaults.getBytes(StandardCharsets.UTF_8)));
        return plugin;
    }
    @Test void legacyMigrationPreservesSettingsAndOriginalBackup() throws Exception {
        String old = "version: 1.1.0\nserver_settings:\n  fast_leaf_decay:\n    enabled: false\ncustom-key: retained\n";
        Files.writeString(directory.resolve("config.yml"), old);
        MeowTools plugin = plugin("config-version: 1\nserver_settings:\n  fast_leaf_decay:\n    enabled: true\n    max_queue_size: 10000\n");
        new ConfigManager(plugin).load();
        var result = YamlConfiguration.loadConfiguration(directory.resolve("config.yml").toFile());
        assertEquals(1, result.getInt("config-version"));
        assertFalse(result.getBoolean("server_settings.fast_leaf_decay.enabled"));
        assertEquals(10000, result.getInt("server_settings.fast_leaf_decay.max_queue_size"));
        assertEquals("retained", result.getString("custom-key"));
        try (var paths = Files.list(directory)) {
            Path backup = paths.filter(p -> p.getFileName().toString().startsWith("config.backup.")).findFirst().orElseThrow();
            assertEquals(old, Files.readString(backup));
        }
        new ConfigManager(plugin).load();
        try (var paths = Files.list(directory)) { assertEquals(2, paths.count()); }
    }
    @Test void malformedYamlNeverOverwritesUserFile() throws Exception {
        String broken = "values: [unterminated";
        Files.writeString(directory.resolve("config.yml"), broken);
        MeowTools plugin = plugin("config-version: 1\n");
        assertThrows(IllegalStateException.class, () -> new ConfigManager(plugin).load());
        assertEquals(broken, Files.readString(directory.resolve("config.yml")));
        verify(plugin, never()).reloadConfig();
    }
    @Test void explicitDisabledSectionSurvivesMerge() throws Exception {
        var current = new YamlConfiguration(); current.loadFromString("feature: false\n");
        var defaults = new YamlConfiguration(); defaults.loadFromString("feature:\n  enabled: true\n");
        ConfigManager.mergeMissing(current, defaults);
        assertFalse(current.getBoolean("feature"));
        assertFalse(current.isConfigurationSection("feature"));
    }
}
