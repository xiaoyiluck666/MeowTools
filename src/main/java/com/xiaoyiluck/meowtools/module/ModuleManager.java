package com.xiaoyiluck.meowtools.module;

import com.xiaoyiluck.meowtools.MeowTools;
import com.xiaoyiluck.meowtools.module.impl.CommandBlockModule;
import com.xiaoyiluck.meowtools.module.impl.CommandPermissionProbeModule;
import com.xiaoyiluck.meowtools.module.impl.ExpMultiplierModule;
import com.xiaoyiluck.meowtools.module.impl.FastLeafDecayModule;
import com.xiaoyiluck.meowtools.module.impl.JoinQuitModule;
import com.xiaoyiluck.meowtools.module.impl.ServerSettingsModule;
import com.xiaoyiluck.meowtools.module.impl.ShortcutModule;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModuleManager {

    private final MeowTools plugin;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(MeowTools plugin) {
        this.plugin = plugin;
    }

    public void loadModules() {
        registerModule(new ShortcutModule(plugin));
        registerModule(new ServerSettingsModule(plugin));
        registerModule(new FastLeafDecayModule(plugin));
        registerModule(new JoinQuitModule(plugin));
        registerModule(new CommandBlockModule(plugin));
        registerModule(new CommandPermissionProbeModule(plugin));
        registerModule(new ExpMultiplierModule(plugin));
    }

    public void registerModule(Module module) {
        modules.put(module.getName(), module);
    }

    public void enableModules() {
        for (Module module : modules.values()) {
            try {
                module.enable();
                plugin.getLogger().info("Enabled module: " + module.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enable module: " + module.getName());
                e.printStackTrace();
            }
        }
    }

    public void disableModules() {
        for (Module module : modules.values()) {
            try {
                module.disable();
                plugin.getLogger().info("Disabled module: " + module.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to disable module: " + module.getName());
                e.printStackTrace();
            }
        }
    }

    public void reloadModules() {
        boolean failed = false;
        for (Module module : modules.values()) {
            try {
                module.reload();
                plugin.getLogger().info("Reloaded module: " + module.getName());
            } catch (Exception e) {
                failed = true;
                plugin.getLogger().severe("Failed to reload module: " + module.getName());
                e.printStackTrace();
            }
        }
        if (failed) throw new IllegalStateException("One or more modules failed to reload; see server log");
    }
}
