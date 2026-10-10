package com.xiaoyiluck.meowtools.command;

import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand implements CommandExecutor {

    private final MeowTools plugin;

    public PingCommand(MeowTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("meowtools.ping")) {
            plugin.handleNoPermission(sender, command, label, args, "meowtools.ping");
            return true;
        }

        Player target;

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("console_target_required"));
                return true;
            }
            target = (Player) sender;
            int ping = target.getPing();
            sender.sendMessage(plugin.getLanguageManager().getMessage("ping_self",
                    "{ping}", String.valueOf(ping)));
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found",
                        "{player}", args[0]));
                return true;
            }

            // Check if the sender is pinging themselves
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
                int ping = target.getPing();
                sender.sendMessage(plugin.getLanguageManager().getMessage("ping_self",
                        "{ping}", String.valueOf(ping)));
                return true;
            }

            int ping = target.getPing();
            sender.sendMessage(plugin.getLanguageManager().getMessage("ping_other",
                    "{player}", target.getName(),
                    "{ping}", String.valueOf(ping)));
        }

        return true;
    }
}
