package com.xiaoyiluck.meowtools;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class PackagedResourcesTest {
    private YamlConfiguration yaml(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var yaml = new YamlConfiguration(); yaml.load(reader); return yaml;
        }
    }
    @Test void packagedMetadataIsExpandedAndLocalesHaveIdenticalKeys() throws Exception {
        var plugin = yaml("plugin.yml");
        assertEquals(MeowTools.class.getName(), plugin.getString("main"));
        assertFalse(plugin.getString("version").contains("${"));
        assertEquals("1.21.10", plugin.getString("api-version"));
        assertTrue(yaml("config.yml").getInt("config-version") > 0);
        assertEquals(yaml("lang/en_us.yml").getKeys(false), yaml("lang/zh_cn.yml").getKeys(false));
    }
}
