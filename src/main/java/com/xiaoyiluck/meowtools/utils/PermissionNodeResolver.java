package com.xiaoyiluck.meowtools.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PermissionNodeResolver {

    private PermissionNodeResolver() {
    }

    public static String resolve(Command command, String label, String[] args, String fallbackPermission) {
        if (fallbackPermission != null && !fallbackPermission.isBlank()) return fallbackPermission;
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        String resolvedPermission = resolveFromCommand(commandName, args);

        if (resolvedPermission == null && label != null && !label.isBlank()) {
            resolvedPermission = resolveFromCommand(label.toLowerCase(Locale.ROOT), args);
        }

        if (resolvedPermission == null && fallbackPermission != null && !fallbackPermission.isBlank()) {
            return fallbackPermission;
        }

        return resolvedPermission == null ? "unknown" : resolvedPermission;
    }

    public static String detectDeniedPermission(CommandSender sender, String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return null;
        }

        String commandLine = rawMessage.startsWith("/") ? rawMessage.substring(1) : rawMessage;
        if (commandLine.isBlank()) {
            return null;
        }

        String[] parts = commandLine.split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return null;
        }

        String label = parts[0].toLowerCase(Locale.ROOT);
        Command command = findCommand(label);
        if (command == null && label.contains(":")) {
            String stripped = label.substring(label.indexOf(':') + 1);
            command = findCommand(stripped);
        }
        if (command == null) {
            return null;
        }

        if (command.testPermissionSilent(sender)) {
            return null;
        }

        String fromMetadata = extractMissingPermissions(sender, command.getPermission());
        if (fromMetadata != null) {
            return fromMetadata;
        }

        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
        return resolve(command, label, args, null);
    }

    private static String resolveFromCommand(String commandName, String[] args) {
        switch (commandName) {
            case "meowtools":
            case "mt":
                if (args.length == 0) {
                    return "meowtools.use";
                }
                return resolveSubCommand(args[0]);
            case "ping":
                return "meowtools.ping";
            case "inv":
                return "meowtools.inv";
            case "vanish":
            case "v":
                return "meowtools.vanish";
            default:
                return null;
        }
    }

    private static String resolveSubCommand(String subCommand) {
        String normalized = subCommand.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "reload":
                return "meowtools.admin";
            case "ping":
                return "meowtools.ping";
            case "inv":
                return "meowtools.inv";
            case "vanish":
            case "v":
                return "meowtools.vanish";
            default:
                return "meowtools.use";
        }
    }

    private static String extractMissingPermissions(CommandSender sender, String permissionExpression) {
        if (permissionExpression == null || permissionExpression.isBlank()) {
            return null;
        }

        List<String> permissions = new ArrayList<>();
        for (String node : permissionExpression.split("[;,]")) {
            String trimmed = node.trim();
            if (!trimmed.isEmpty()) {
                permissions.add(trimmed);
            }
        }

        if (permissions.isEmpty()) {
            return null;
        }

        if (permissions.size() == 1) {
            return permissions.get(0);
        }

        List<String> missing = new ArrayList<>();
        for (String node : permissions) {
            if (!sender.hasPermission(node)) {
                missing.add(node);
            }
        }

        if (!missing.isEmpty()) {
            return String.join(";", missing);
        }
        return String.join(";", permissions);
    }

    private static Command findCommand(String label) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return null;
        }
        return commandMap.getCommand(label);
    }

    private static CommandMap getCommandMap() {
        return Bukkit.getServer().getCommandMap();
    }
}
