package com.xiaoyiluck.meowtools.command;

import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements CommandExecutor {

    private final MeowTools plugin;

    public VanishCommand(MeowTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("meowtools.vanish")) {
            plugin.handleNoPermission(sender, command, label, args, "meowtools.vanish");
            return true;
        }

        Player target;
        boolean forceState = false;
        boolean forcedVanishState = false;

        // Argument parsing logic
        if (args.length > 0) {
            String lastArg = args[args.length - 1].toLowerCase();
            if (lastArg.equals("on") || lastArg.equals("off")) {
                forcedVanishState = lastArg.equals("on");
                forceState = true;

                // If there's another argument, it must be the player name
                if (args.length > 1) {
                    if (sender.hasPermission("meowtools.vanish.other")) {
                        target = Bukkit.getPlayer(args[0]);
                        if (target == null) {
                            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", args[0]));
                            return true;
                        }
                    } else {
                        plugin.handleNoPermission(sender, command, label, args, "meowtools.vanish.other");
                        return true;
                    }
                } else {
                    // Only on/off provided, target is self
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("console_target_required"));
                        return true;
                    }
                    target = (Player) sender;
                }
            } else {
                // No on/off, treat as player name if permission allows
                if (sender.hasPermission("meowtools.vanish.other")) {
                    target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found", "{player}", args[0]));
                        return true;
                    }
                } else {
                    plugin.handleNoPermission(sender, command, label, args, "meowtools.vanish.other");
                    return true;
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("console_target_required"));
                return true;
            }
            target = (Player) sender;
        }

        if (forceState) {
            plugin.getVanishManager().setVanish(target, forcedVanishState);
        } else {
            plugin.getVanishManager().toggleVanish(target);
        }

        // If toggling for other
        if (target != sender) {
            boolean isVanished = plugin.getVanishManager().isVanished(target);
            String msgKey = isVanished ? "vanish_other_on" : "vanish_other_off";
            sender.sendMessage(plugin.getLanguageManager().getMessage(msgKey, "{player}", target.getName()));
        }

        return true;
    }
}
