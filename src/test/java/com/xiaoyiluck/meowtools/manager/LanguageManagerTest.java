package com.xiaoyiluck.meowtools.manager;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.logging.Logger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LanguageManagerTest {
    @TempDir Path directory;
    @Test void missingOrUnsafeLanguageFallsBackToBundledEnglish() throws Exception {
        var plugin = mock(MeowTools.class); var config = new YamlConfiguration();
        config.set("language", "../outside"); when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(directory.toFile()); when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource(anyString())).thenAnswer(call -> getClass().getClassLoader().getResourceAsStream(call.getArgument(0)));
        Files.createDirectories(directory.resolve("lang"));
        for (String language : new String[] {"en_us", "zh_cn"}) {
            try (var resource = getClass().getClassLoader().getResourceAsStream("lang/" + language + ".yml")) {
                Files.copy(resource, directory.resolve("lang/" + language + ".yml"));
            }
        }
        var manager = new LanguageManager(plugin); manager.load();
        assertEquals("&cUnknown command.", manager.getRawMessage("unknown_command"));
        assertFalse(Files.exists(directory.resolve("outside.yml")));
    }

    @Test void optionalPlaceholderDependencyIsNotLinkedWhenPluginIsAbsent() throws Exception {
        String managerName = LanguageManager.class.getName();
        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("me.clip.placeholderapi.")) throw new ClassNotFoundException(name);
                if (!name.equals(managerName)) return super.loadClass(name, resolve);
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    try (var stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        byte[] bytes = stream.readAllBytes(); loaded = defineClass(name, bytes, 0, bytes.length);
                    } catch (Exception failure) { throw new ClassNotFoundException(name, failure); }
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        assertThrows(ClassNotFoundException.class, () -> isolated.loadClass("me.clip.placeholderapi.PlaceholderAPI"));
        Class<?> type = isolated.loadClass(managerName);
        Object manager = type.getConstructor(MeowTools.class).newInstance(new Object[] {null});
        var pluginManager = mock(PluginManager.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            assertEquals("%player_name%", type.getMethod("applyPlaceholders", Player.class, String.class)
                    .invoke(manager, mock(Player.class), "%player_name%"));
        }
    }
}
