package com.xiaoyiluck.meowtools;

import com.xiaoyiluck.meowtools.command.MainCommand;
import com.xiaoyiluck.meowtools.manager.ConfigManager;
import com.xiaoyiluck.meowtools.manager.LanguageManager;
import com.xiaoyiluck.meowtools.manager.VanishManager;
import com.xiaoyiluck.meowtools.module.ModuleManager;
import com.xiaoyiluck.meowtools.utils.PermissionNodeResolver;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MeowTools extends JavaPlugin {

    private static @Nullable MeowTools instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ModuleManager moduleManager;
    private VanishManager vanishManager;
    private MainCommand mainCommand;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Managers
        this.configManager = new ConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.moduleManager = new ModuleManager(this);
        this.vanishManager = new VanishManager(this);

        // Load Managers
        this.configManager.load();
        this.languageManager.load();
        this.moduleManager.loadModules();
        this.moduleManager.enableModules();
        this.vanishManager.load();

        // Register Commands
        @Nullable PluginCommand meowToolsCommand = getCommand("meowtools");
        if (meowToolsCommand != null) {
            mainCommand = new MainCommand(this);
            meowToolsCommand.setExecutor(mainCommand);
            meowToolsCommand.setTabCompleter(mainCommand);
        }

        getLogger().info("MeowTools has been enabled!");
    }

    @Override
    public void onDisable() {
        if (mainCommand != null) mainCommand.disable();
        if (vanishManager != null) {
            vanishManager.disable();
        }
        if (moduleManager != null) {
            moduleManager.disableModules();
        }
        instance = null;
        getLogger().info("MeowTools has been disabled!");
    }

    public static @Nullable MeowTools getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public void handleNoPermission(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args, String permissionHint) {
        sender.sendMessage(languageManager.getMessage("no_permission"));

        String permissionNode = PermissionNodeResolver.resolve(command, label, args, permissionHint);
        String commandLine = "/" + label + (args.length > 0 ? " " + String.join(" ", args) : "");
        getLogger().warning("[PermissionDenied] sender=" + sender.getName() + ", permission=" + permissionNode + ", command=\"" + commandLine + "\"");
    }

    public void reloadPlugin() {
        configManager.load();
        languageManager.load();
        moduleManager.reloadModules();
        vanishManager.reload();
        if (mainCommand != null) mainCommand.reload();
    }
}
