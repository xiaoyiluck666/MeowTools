package com.xiaoyiluck.meowtools.command;

import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final MeowTools plugin;
    private final PingCommand pingCommand;
    private final InvCommand invCommand;
    private final VanishCommand vanishCommand;

    public MainCommand(MeowTools plugin) {
        this.plugin = plugin;
        this.pingCommand = new PingCommand(plugin);
        this.invCommand = new InvCommand(plugin);
        this.vanishCommand = new VanishCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(java.util.Locale.ROOT);
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;
            case "reload":
                if (!sender.hasPermission("meowtools.admin")) {
                    plugin.handleNoPermission(sender, command, label, args, "meowtools.admin");
                    return true;
                }
                try {
                    plugin.reloadPlugin();
                    sender.sendMessage(plugin.getLanguageManager().getMessage("reload_success"));
                } catch (RuntimeException failure) {
                    sender.sendMessage(plugin.getLanguageManager().getMessage("reload_failed"));
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Reload failed", failure);
                }
                return true;

            case "ping":
                return pingCommand.onCommand(sender, command, label, subArgs);

            case "inv":
                return invCommand.onCommand(sender, command, label, subArgs);

            case "vanish":
            case "v":
                return vanishCommand.onCommand(sender, command, label, subArgs);

            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("unknown_command"));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("help_header"));
        if (sender.hasPermission("meowtools.ping")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("help_ping"));
        }
        if (sender.hasPermission("meowtools.inv")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("help_inv"));
        }
        if (sender.hasPermission("meowtools.vanish")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("help_vanish"));
        }
        if (sender.hasPermission("meowtools.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("help_reload"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("help");
            if (sender.hasPermission("meowtools.ping")) suggestions.add("ping");
            if (sender.hasPermission("meowtools.inv")) suggestions.add("inv");
            if (sender.hasPermission("meowtools.vanish")) suggestions.add("vanish");
            if (sender.hasPermission("meowtools.admin")) suggestions.add("reload");
            
            return filter(suggestions, args[0]);
        }

        if (args.length >= 2) {
            String sub = args[0].toLowerCase(java.util.Locale.ROOT);
            switch (sub) {
                case "ping":
                case "inv":
                     if (args.length == 2) return null; // Players
                     break;
                     
                case "vanish":
                case "v":
                     if (args.length == 2) {
                         List<String> suggestions = new ArrayList<>();
                         suggestions.add("on");
                         suggestions.add("off");
                         // Also suggest players if has permission to vanish others
                         if (sender.hasPermission("meowtools.vanish.other")) {
                             List<String> combined = new ArrayList<>(suggestions);
                             org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> combined.add(p.getName()));
                             return filter(combined, args[1]);
                         }
                         return filter(suggestions, args[1]);
                     }
                     if (args.length == 3 && sender.hasPermission("meowtools.vanish.other")) {
                         return filter(Arrays.asList("on", "off"), args[2]);
                     }
                     break;
            }
        }

        return Collections.emptyList();
    }

    public void disable() { invCommand.disable(); }

    public void reload() { invCommand.reload(); }

    private List<String> filter(List<String> list, String input) {
        String lowerInput = input.toLowerCase(java.util.Locale.ROOT);
        return list.stream()
                .filter(s -> s.toLowerCase(java.util.Locale.ROOT).startsWith(lowerInput))
                .collect(Collectors.toList());
    }
}
