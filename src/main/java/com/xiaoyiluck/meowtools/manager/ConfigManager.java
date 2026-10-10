package com.xiaoyiluck.meowtools.manager;

import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.logging.Level;

public class ConfigManager {
    private final MeowTools plugin;
    public ConfigManager(MeowTools plugin) { this.plugin = plugin; }

    public void load() {
        plugin.saveDefaultConfig();
        Path file = plugin.getDataFolder().toPath().resolve("config.yml");
        Path temporary = null;
        try (var stream = plugin.getResource("config.yml")) {
            if (stream == null) throw new IllegalStateException("Missing bundled config.yml");
            var defaults = new YamlConfiguration();
            defaults.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            var current = new YamlConfiguration();
            current.load(file.toFile());
            if (current.getInt("config-version", 0) < defaults.getInt("config-version")) {
                Path backup = Files.createTempFile(file.getParent(), "config.backup.", ".yml");
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
                mergeMissing(current, defaults);
                current.set("config-version", defaults.getInt("config-version"));
                temporary = Files.createTempFile(file.getParent(), "config.update.", ".yml");
                current.save(temporary.toFile());
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
                plugin.getLogger().info("Configuration migrated; backup: " + backup.getFileName());
            }
            plugin.reloadConfig();
        } catch (Exception failure) {
            plugin.getLogger().log(Level.SEVERE, "Configuration could not be loaded safely", failure);
            throw new IllegalStateException("Invalid MeowTools configuration", failure);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (Exception failure) { plugin.getLogger().warning("Could not remove " + temporary); }
            }
        }
    }

    static void mergeMissing(YamlConfiguration current, YamlConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key) && !current.contains(key, true)) {
                String parent = key;
                boolean blocked = false;
                while (parent.contains(".")) {
                    parent = parent.substring(0, parent.lastIndexOf('.'));
                    if (current.contains(parent, true) && !current.isConfigurationSection(parent)) {
                        blocked = true;
                        break;
                    }
                }
                if (!blocked) current.set(key, defaults.get(key));
            }
        }
    }
}
