package com.bocrace.command;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.RaceRecord;
import com.bocrace.model.CourseType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles /racestats command for players to view their race statistics
 */
public class RaceStatsCommand implements CommandExecutor, TabCompleter {
    
    private final BOCRacePlugin plugin;
    
    public RaceStatsCommand(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show help
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "recent":
                return handleRecent(sender, player);
            case "top":
                return handleTop(sender, args, player);
            case "mytimes":
                return handleMyTimes(sender, args, player);
            case "stats":
                return handleStats(sender, player);
            default:
                sender.sendMessage("§cUnknown racestats command. Use /racestats to see available options.");
                return true;
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== Race Statistics ===");
        sender.sendMessage("§a/racestats recent §7- Show your recent races");
        sender.sendMessage("§a/racestats top <course> §7- Show top times for a course");
        sender.sendMessage("§a/racestats mytimes <course> §7- Show your times for a course");
        sender.sendMessage("§a/racestats stats §7- Show your overall statistics");
    }
    
    private boolean handleRecent(CommandSender sender, Player player) {
        plugin.debugLog("RaceStats recent command called - Player: " + player.getName());
        
        List<RaceRecord> recentRaces = plugin.getRecordManager().getPlayerRecent(player.getName(), 5);
        
        sender.sendMessage("§6=== Your Recent Races ===");
        
        if (recentRaces.isEmpty()) {
            sender.sendMessage("§7No recent races found.");
            sender.sendMessage("§7");
            sender.sendMessage("§7Complete some races to see your history here!");
        } else {
            for (int i = 0; i < recentRaces.size(); i++) {
                RaceRecord record = recentRaces.get(i);
                sender.sendMessage("§a" + (i + 1) + ". §e" + record.getCourse() + " §7- §b" + record.getFormattedTime() + "s §7(" + record.getFormattedDate() + ")");
            }
        }
        
        plugin.debugLog("Recent races displayed for player: " + player.getName() + " (" + recentRaces.size() + " races)");
        return true;
    }
    
    private boolean handleTop(CommandSender sender, String[] args, Player player) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /racestats top <course>");
            sender.sendMessage("§7Example: /racestats top Herewego1");
            return true;
        }
        
        String courseName = args[1];
        plugin.debugLog("RaceStats top command called - Player: " + player.getName() + ", Course: " + courseName);
        
        List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes(courseName, 5);
        
        sender.sendMessage("§6=== Top Times for " + courseName + " ===");
        
        if (topTimes.isEmpty()) {
            sender.sendMessage("§7No records found for course '" + courseName + "'.");
            sender.sendMessage("§7");
            sender.sendMessage("§7Be the first to set a time on this course!");
        } else {
            for (int i = 0; i < topTimes.size(); i++) {
                RaceRecord record = topTimes.get(i);
                String medal = getMedal(i);
                sender.sendMessage("§" + medal + (i + 1) + ". §e" + record.getPlayer() + " §7- §b" + record.getFormattedTime() + "s §7(" + record.getFormattedDate() + ")");
            }
        }
        
        plugin.debugLog("Top times displayed for course: " + courseName + " (" + topTimes.size() + " records)");
        return true;
    }
    
    private boolean handleMyTimes(CommandSender sender, String[] args, Player player) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /racestats mytimes <course>");
            sender.sendMessage("§7Example: /racestats mytimes Herewego1");
            return true;
        }
        
        String courseName = args[1];
        plugin.debugLog("RaceStats mytimes command called - Player: " + player.getName() + ", Course: " + courseName);
        
        // Get player's best time for this course
        RaceRecord bestTime = plugin.getRecordManager().getPlayerBestTime(player.getName(), courseName);
        
        sender.sendMessage("§6=== Your Times for " + courseName + " ===");
        
        if (bestTime == null) {
            sender.sendMessage("§7No times found for course '" + courseName + "'.");
            sender.sendMessage("§7");
            sender.sendMessage("§7Complete a race on this course to set your first time!");
        } else {
            sender.sendMessage("§aBest Time: §b" + bestTime.getFormattedTime() + "s §7(" + bestTime.getFormattedDate() + ")");
            
            // Show where they rank
            List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes(courseName, 10);
            int rank = -1;
            for (int i = 0; i < topTimes.size(); i++) {
                if (topTimes.get(i).getPlayer().equals(player.getName())) {
                    rank = i + 1;
                    break;
                }
            }
            
            if (rank > 0) {
                sender.sendMessage("§7Rank: §e#" + rank + " §7out of " + topTimes.size() + " total times");
            }
        }
        
        plugin.debugLog("Player times displayed for: " + player.getName() + " on course: " + courseName);
        return true;
    }
    
    private boolean handleStats(CommandSender sender, Player player) {
        plugin.debugLog("RaceStats stats command called - Player: " + player.getName());
        
        int totalRaces = plugin.getRecordManager().getPlayerTotalRaces(player.getName());
        int spRaces = plugin.getRecordManager().getPlayerRacesByType(player.getName(), CourseType.SINGLEPLAYER);
        int mpRaces = plugin.getRecordManager().getPlayerRacesByType(player.getName(), CourseType.MULTIPLAYER);
        String favoriteCourse = plugin.getRecordManager().getPlayerFavoriteCourse(player.getName());
        
        sender.sendMessage("§6=== Your Race Statistics ===");
        sender.sendMessage("§aTotal Races: §b" + totalRaces);
        sender.sendMessage("§aSingleplayer: §b" + spRaces);
        sender.sendMessage("§aMultiplayer: §b" + mpRaces);
        
        if (favoriteCourse != null) {
            sender.sendMessage("§aFavorite Course: §e" + favoriteCourse);
        } else {
            sender.sendMessage("§aFavorite Course: §7None yet");
        }
        
        if (totalRaces == 0) {
            sender.sendMessage("§7");
            sender.sendMessage("§7Complete your first race to start building your statistics!");
        }
        
        plugin.debugLog("Player stats displayed for: " + player.getName() + " (Total: " + totalRaces + ", SP: " + spRaces + ", MP: " + mpRaces + ")");
        return true;
    }
    
    private String getMedal(int position) {
        switch (position) {
            case 0: return "6"; // Gold
            case 1: return "7"; // Silver
            case 2: return "c"; // Bronze
            default: return "a"; // Green
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("recent", "top", "mytimes", "stats");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Second argument - course names for top and mytimes
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("top") || firstArg.equals("mytimes")) {
                // Get available course names
                for (String courseName : plugin.getStorageManager().getCourseNames()) {
                    if (courseName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(courseName);
                    }
                }
            }
        }
        
        return completions;
    }
}
