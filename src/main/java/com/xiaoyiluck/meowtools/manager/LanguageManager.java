package com.xiaoyiluck.meowtools.manager;

import com.xiaoyiluck.meowtools.MeowTools;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final MeowTools plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public LanguageManager(MeowTools plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String lang = plugin.getConfig().getString("language", "en_us");
        if (lang == null || !lang.matches("[a-zA-Z0-9_]+")
                || (!lang.equals("en_us") && !lang.equals("zh_cn")
                && !new File(plugin.getDataFolder(), "lang/" + lang + ".yml").isFile())) {
            plugin.getLogger().warning("Unknown language; using en_us");
            lang = "en_us";
        }
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File enFile = new File(langDir, "en_us.yml");
        if (!enFile.exists()) {
            plugin.saveResource("lang/en_us.yml", false);
        }

        File zhFile = new File(langDir, "zh_cn.yml");
        if (!zhFile.exists()) {
            plugin.saveResource("lang/zh_cn.yml", false);
        }

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }

        YamlConfiguration config = new YamlConfiguration();
        try { config.load(langFile); }
        catch (Exception failure) { throw new IllegalStateException("Invalid language file: " + langFile, failure); }
        messages.clear();
        
        // Load default values if missing
        InputStream defStream = plugin.getResource("lang/" + lang + ".yml");
        if (defStream == null) defStream = plugin.getResource("lang/en_us.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
            try { defStream.close(); } catch (Exception ignored) { }
        }

        // Save new keys to file
        config.options().copyDefaults(true);
        try {
            config.save(langFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save language file: " + e.getMessage());
        }

        prefix = config.getString("prefix", "&8[&bMeowTools&8] &r");

        // Load messages from defaults first (to ensure we have all keys)
        if (config.getDefaults() != null) {
            for (String key : config.getDefaults().getKeys(false)) {
                messages.put(key, config.getString(key, ""));
            }
        }

        // Load messages from file (overrides)
        for (String key : config.getKeys(false)) {
            messages.put(key, config.getString(key, ""));
        }
    }

    public Component getMessage(String key) {
        String msg = messages.getOrDefault(key, key);
        String fullMsg = (key.equals("prefix") ? msg : (prefix + msg));
        return LEGACY_SERIALIZER.deserialize(fullMsg == null ? "" : fullMsg);
    }

    public Component getMessage(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, key);

        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        String fullMsg = (key.equals("prefix") ? msg : (prefix + msg));
        return LEGACY_SERIALIZER.deserialize(fullMsg == null ? "" : fullMsg);
    }


    public String getRawMessage(String key) {
        String msg = messages.getOrDefault(key, key);
        return msg == null ? "" : msg;
    }

    public String applyPlaceholders(Player player, String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        if (player == null) {
            return text;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
