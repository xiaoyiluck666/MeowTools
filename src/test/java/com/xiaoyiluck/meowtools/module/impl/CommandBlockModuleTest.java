package com.xiaoyiluck.meowtools.module.impl;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CommandBlockModuleTest {
    @Test void normalizesCaseWhitespaceAndNamespacesAndReloads() {
        var plugin = mock(MeowTools.class); var config = new YamlConfiguration();
        config.set("server_settings.blocked_commands.list", List.of(" /PL ", "example:secret"));
        when(plugin.getConfig()).thenReturn(config);
        var module = new CommandBlockModule(plugin); module.reload();
        assertTrue(module.blocks("/bukkit:PL\targument"));
        assertTrue(module.blocks("/pl")); assertTrue(module.blocks("/example:secret"));
        assertFalse(module.blocks("/other:secret")); assertFalse(module.blocks("/please"));
        config.set("server_settings.blocked_commands.list", List.of()); module.reload();
        assertFalse(module.blocks("/pl"));
    }
}
