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
                sender.sendMessage("§aPlugin reloaded! (Not implemented yet)");
                return true;
            default:
                sender.sendMessage("§cUnknown command. Use /bocrace help for available commands.");
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== BOCRacePlugin Commands ===");
        sender.sendMessage("§e/bocrace help §7- Show this help menu");
        sender.sendMessage("§e/bocrace reload §7- Reload plugin configuration");
        sender.sendMessage("§7More commands coming in Stage 2...");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        }
        
        return completions;
    }
}
