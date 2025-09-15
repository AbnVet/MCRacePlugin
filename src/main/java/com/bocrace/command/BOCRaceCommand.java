package com.bocrace.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BOCRaceCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sendHelp(sender);
                return true;
            case "reload":
                if (!sender.hasPermission("bocrace.admin")) {
                    sender.sendMessage("§cYou don't have permission to reload the plugin!");
                    return true;
                }
                sender.sendMessage("§aReload not implemented yet.");
                return true;
            case "singleplayer":
                return handleSingleplayerCommand(sender, args);
            case "multiplayer":
                return handleMultiplayerCommand(sender, args);
            default:
                sender.sendMessage("§cUnknown command. Use /bocrace help for available commands.");
                return true;
        }
    }
    
    private boolean handleSingleplayerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bocrace singleplayer <create|edit|delete|list|tp|info|reload|stats|recent>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "create":
                sender.sendMessage("§eSingleplayer create not implemented yet.");
                return true;
            case "edit":
                sender.sendMessage("§eSingleplayer edit not implemented yet.");
                return true;
            case "delete":
                sender.sendMessage("§eSingleplayer delete not implemented yet.");
                return true;
            case "list":
                sender.sendMessage("§eSingleplayer list not implemented yet.");
                return true;
            case "tp":
                sender.sendMessage("§eSingleplayer tp not implemented yet.");
                return true;
            case "info":
                sender.sendMessage("§eSingleplayer info not implemented yet.");
                return true;
            case "reload":
                sender.sendMessage("§eSingleplayer reload not implemented yet.");
                return true;
            case "stats":
                sender.sendMessage("§eSingleplayer stats not implemented yet.");
                return true;
            case "recent":
                sender.sendMessage("§eSingleplayer recent not implemented yet.");
                return true;
            default:
                sender.sendMessage("§cUnknown singleplayer command. Use /bocrace help for available commands.");
                return true;
        }
    }
    
    private boolean handleMultiplayerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bocrace multiplayer <create|edit|delete|list|tp|info|reload|stats|recent>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "create":
                sender.sendMessage("§eMultiplayer create not implemented yet.");
                return true;
            case "edit":
                sender.sendMessage("§eMultiplayer edit not implemented yet.");
                return true;
            case "delete":
                sender.sendMessage("§eMultiplayer delete not implemented yet.");
                return true;
            case "list":
                sender.sendMessage("§eMultiplayer list not implemented yet.");
                return true;
            case "tp":
                sender.sendMessage("§eMultiplayer tp not implemented yet.");
                return true;
            case "info":
                sender.sendMessage("§eMultiplayer info not implemented yet.");
                return true;
            case "reload":
                sender.sendMessage("§eMultiplayer reload not implemented yet.");
                return true;
            case "stats":
                sender.sendMessage("§eMultiplayer stats not implemented yet.");
                return true;
            case "recent":
                sender.sendMessage("§eMultiplayer recent not implemented yet.");
                return true;
            default:
                sender.sendMessage("§cUnknown multiplayer command. Use /bocrace help for available commands.");
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== BOCRacePlugin Commands ===");
        
        // Singleplayer Commands (Green)
        sender.sendMessage("§6Singleplayer Commands:");
        sender.sendMessage("§a/bocrace singleplayer create <name> §7- Create a new singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer edit <name> §7- Edit an existing singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer delete <name> §7- Delete a singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer list §7- List all singleplayer courses");
        sender.sendMessage("§a/bocrace singleplayer tp <name> §7- Teleport to a singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer info <name> §7- Show course information");
        sender.sendMessage("§a/bocrace singleplayer reload §7- Reload singleplayer courses");
        sender.sendMessage("§a/bocrace singleplayer stats <name> §7- Show course statistics");
        sender.sendMessage("§a/bocrace singleplayer recent <name> §7- Show recent race results");
        
        // Multiplayer Commands (Aqua)
        sender.sendMessage("§6Multiplayer Commands:");
        sender.sendMessage("§b/bocrace multiplayer create <name> §7- Create a new multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer edit <name> §7- Edit an existing multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer delete <name> §7- Delete a multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer list §7- List all multiplayer courses");
        sender.sendMessage("§b/bocrace multiplayer tp <name> §7- Teleport to a multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer info <name> §7- Show course information");
        sender.sendMessage("§b/bocrace multiplayer reload §7- Reload multiplayer courses");
        sender.sendMessage("§b/bocrace multiplayer stats <name> §7- Show course statistics");
        sender.sendMessage("§b/bocrace multiplayer recent <name> §7- Show recent race results");
        
        // Global Commands (Yellow)
        sender.sendMessage("§6Global Commands:");
        sender.sendMessage("§e/bocrace help §7- Show this help menu");
        sender.sendMessage("§e/bocrace reload §7- Reload plugin configuration");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First level: singleplayer, multiplayer, help, reload
            List<String> subCommands = Arrays.asList("singleplayer", "multiplayer", "help", "reload");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("singleplayer")) {
                // Singleplayer subcommands
                List<String> spCommands = Arrays.asList("create", "edit", "delete", "list", "tp", "info", "reload", "stats", "recent");
                for (String spCommand : spCommands) {
                    if (spCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(spCommand);
                    }
                }
            } else if (firstArg.equals("multiplayer")) {
                // Multiplayer subcommands
                List<String> mpCommands = Arrays.asList("create", "edit", "delete", "list", "tp", "info", "reload", "stats", "recent");
                for (String mpCommand : mpCommands) {
                    if (mpCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(mpCommand);
                    }
                }
            }
        }
        
        return completions;
    }
}
