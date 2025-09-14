package com.bocrace.command;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.PlayerStats;
import com.bocrace.model.RaceSession;
import com.bocrace.model.SetupSession;
import com.bocrace.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command handler for BOCRacePlugin
 */
public class BOCRaceCommand implements CommandExecutor, TabCompleter {
    private final BOCRacePlugin plugin;
    
    public BOCRaceCommand(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
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
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "create":
                handleCreate(sender, args);
                break;
                
            case "edit":
                handleEdit(sender, args);
                break;
                
            case "delete":
                handleDelete(sender, args);
                break;
                
            case "list":
                handleList(sender);
                break;
                
            case "tp":
                handleTp(sender, args);
                break;
                
            case "info":
                handleInfo(sender, args);
                break;
                
            case "join":
                handleJoin(sender, args);
                break;
                
            case "leave":
                handleLeave(sender);
                break;
                
            case "start":
                handleStart(sender);
                break;
                
            case "stats":
                handleStats(sender, args);
                break;
                
            case "recent":
                handleRecent(sender, args);
                break;
                
            default:
                MessageUtil.sendMessage(sender, "general.invalid-arguments");
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList(
                "help", "reload", "create", "edit", "delete", "list", "tp", "info",
                "join", "leave", "start", "stats", "recent"
            );
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Second argument - course names for most commands
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            switch (subCommand) {
                case "edit":
                case "delete":
                case "tp":
                case "info":
                case "join":
                case "stats":
                case "recent":
                    for (String courseName : plugin.getStorageManager().getCourses().keySet()) {
                        if (courseName.toLowerCase().startsWith(input)) {
                            completions.add(courseName);
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            // Third argument - course types for create command
            String subCommand = args[0].toLowerCase();
            if ("create".equals(subCommand)) {
                String input = args[2].toLowerCase();
                List<String> types = Arrays.asList("singleplayer", "multiplayer");
                for (String type : types) {
                    if (type.startsWith(input)) {
                        completions.add(type);
                    }
                }
            }
        }
        
        return completions;
    }
    
    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "help.header");
        
        if (sender.hasPermission("bocrace.admin")) {
            MessageUtil.sendMessage(sender, "help.admin-commands");
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace create <name> <type>", "description", "Create a new race course"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace edit <name>", "description", "Edit an existing race course"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace delete <name>", "description", "Delete a race course"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace list", "description", "List all available courses"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace tp <name>", "description", "Teleport to a course"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace info <name>", "description", "Get information about a course"));
            MessageUtil.sendMessage(sender, "help.command-format", 
                Map.of("command", "/bocrace reload", "description", "Reload plugin configuration"));
        }
        
        MessageUtil.sendMessage(sender, "help.player-commands");
        MessageUtil.sendMessage(sender, "help.command-format", 
            Map.of("command", "/bocrace join <name>", "description", "Join a multiplayer race lobby"));
        MessageUtil.sendMessage(sender, "help.command-format", 
            Map.of("command", "/bocrace leave", "description", "Leave a race lobby"));
        MessageUtil.sendMessage(sender, "help.command-format", 
            Map.of("command", "/bocrace start", "description", "Start a race (lobby leader only)"));
        MessageUtil.sendMessage(sender, "help.command-format", 
            Map.of("command", "/bocrace stats <name>", "description", "View your race statistics"));
        MessageUtil.sendMessage(sender, "help.command-format", 
            Map.of("command", "/bocrace recent <name>", "description", "View your most recent race results"));
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("bocrace.reload")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.reloadConfig();
        plugin.getStorageManager().loadCourses();
        plugin.getStorageManager().loadPlayerStats();
        MessageUtil.sendMessage(sender, "general.plugin-reloaded");
    }
    
    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (!sender.hasPermission("bocrace.create")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        String typeStr = args[2].toLowerCase();
        
        if (plugin.getStorageManager().hasCourse(courseName)) {
            MessageUtil.sendMessage(sender, "course.already-exists", Map.of("course", courseName));
            return;
        }
        
        Course.CourseType type;
        try {
            type = Course.CourseType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        Player player = (Player) sender;
        Course course = new Course(courseName, type, player.getWorld(), player.getUniqueId());
        plugin.getStorageManager().addCourse(course);
        
        // Start interactive setup
        SetupSession setupSession = new SetupSession(player, courseName, type);
        plugin.getSetupManager().startSetup(player, setupSession);
        
        MessageUtil.sendMessage(sender, "course.created", Map.of("course", courseName));
        MessageUtil.sendMessage(sender, "setup.started", Map.of("course", courseName));
    }
    
    private void handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (!sender.hasPermission("bocrace.edit")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        Player player = (Player) sender;
        SetupSession setupSession = new SetupSession(player, courseName, course.getType());
        plugin.getSetupManager().startSetup(player, setupSession);
        
        MessageUtil.sendMessage(sender, "setup.started", Map.of("course", courseName));
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bocrace.delete")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        plugin.getStorageManager().removeCourse(courseName);
        plugin.getStorageManager().saveCourses();
        
        MessageUtil.sendMessage(sender, "course.deleted", Map.of("course", courseName));
    }
    
    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("bocrace.list")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        Map<String, Course> courses = plugin.getStorageManager().getCourses();
        
        if (courses.isEmpty()) {
            MessageUtil.sendMessage(sender, "course.no-courses");
            return;
        }
        
        MessageUtil.sendMessage(sender, "course.list-header");
        for (Course course : courses.values()) {
            MessageUtil.sendMessage(sender, "course.list-item", 
                Map.of("course", course.getName(), "type", course.getType().name().toLowerCase()));
        }
    }
    
    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (!sender.hasPermission("bocrace.tp")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        Player player = (Player) sender;
        org.bukkit.Location tpLocation = course.getCourseLobby();
        if (tpLocation == null) {
            MessageUtil.sendMessage(sender, "error.location-invalid");
            return;
        }
        
        player.teleport(tpLocation);
        MessageUtil.sendMessage(sender, "course.teleported", Map.of("course", courseName));
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bocrace.info")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        MessageUtil.sendMessage(sender, "course.info-header", Map.of("course", courseName));
        MessageUtil.sendMessage(sender, "course.info-type", Map.of("type", course.getType().name().toLowerCase()));
        MessageUtil.sendMessage(sender, "course.info-status", 
            Map.of("status", course.isComplete() ? "Complete" : "Incomplete"));
        
        if (!course.isComplete()) {
            List<String> missing = course.getMissingElements();
            MessageUtil.sendMessage(sender, "course.info-required-elements", 
                Map.of("elements", String.join(", ", missing)));
        }
    }
    
    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        if (course.getType() != Course.CourseType.MULTIPLAYER) {
            MessageUtil.sendMessage(sender, "race.singleplayer-only");
            return;
        }
        
        Player player = (Player) sender;
        plugin.getRaceManager().joinRace(player, course);
    }
    
    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        Player player = (Player) sender;
        plugin.getRaceManager().leaveRace(player);
    }
    
    private void handleStart(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        Player player = (Player) sender;
        plugin.getRaceManager().startRace(player);
    }
    
    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        Player player = (Player) sender;
        PlayerStats stats = plugin.getStorageManager().getOrCreatePlayerStats(player.getUniqueId());
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        
        if (courseStats.getBestTime() == 0) {
            MessageUtil.sendMessage(sender, "stats.no-stats");
            return;
        }
        
        MessageUtil.sendMessage(sender, "stats.personal-best", 
            Map.of("course", courseName, "time", courseStats.getFormattedBestTime()));
    }
    
    private void handleRecent(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[1];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        Player player = (Player) sender;
        PlayerStats stats = plugin.getStorageManager().getOrCreatePlayerStats(player.getUniqueId());
        Optional<PlayerStats.RaceRecord> recentRace = stats.getMostRecentRace(courseName);
        
        if (recentRace.isEmpty()) {
            MessageUtil.sendMessage(sender, "stats.no-recent-race");
            return;
        }
        
        PlayerStats.RaceRecord record = recentRace.get();
        MessageUtil.sendMessage(sender, "stats.recent-race", 
            Map.of("course", courseName, "time", record.getFormattedRaceTime(), "date", record.getFormattedDate()));
    }
}
