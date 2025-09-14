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
 * Main command handler for BOCRacePlugin with nested command structure
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
        
        String firstArg = args[0].toLowerCase();
        
        // Handle global commands
        switch (firstArg) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "reload":
                handleReload(sender);
                return true;
        }
        
        // Handle nested commands
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return true;
        }
        
        String category = firstArg;
        String subCommand = args[1].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 2, args.length);
        
        switch (category) {
            case "singleplayer":
                handleSingleplayerCommand(sender, subCommand, subArgs);
                break;
                
            case "multiplayer":
                handleMultiplayerCommand(sender, subCommand, subArgs);
                break;
                
            default:
                MessageUtil.sendMessage(sender, "general.invalid-arguments");
                break;
        }
        
        return true;
    }
    
    /**
     * Handle singleplayer commands
     */
    private void handleSingleplayerCommand(CommandSender sender, String subCommand, String[] args) {
        switch (subCommand) {
            case "create":
                handleCreate(sender, args, Course.CourseType.SINGLEPLAYER);
                break;
                
            case "edit":
                handleEdit(sender, args);
                break;
                
            case "delete":
                handleDelete(sender, args);
                break;
                
            case "list":
                handleList(sender, Course.CourseType.SINGLEPLAYER);
                break;
                
            case "tp":
                handleTp(sender, args);
                break;
                
            case "info":
                handleInfo(sender, args);
                break;
                
            case "reload":
                handleReload(sender);
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
    }
    
    /**
     * Handle multiplayer commands
     */
    private void handleMultiplayerCommand(CommandSender sender, String subCommand, String[] args) {
        switch (subCommand) {
            case "create":
                handleCreate(sender, args, Course.CourseType.MULTIPLAYER);
                break;
                
            case "edit":
                handleEdit(sender, args);
                break;
                
            case "delete":
                handleDelete(sender, args);
                break;
                
            case "list":
                handleList(sender, Course.CourseType.MULTIPLAYER);
                break;
                
            case "tp":
                handleTp(sender, args);
                break;
                
            case "info":
                handleInfo(sender, args);
                break;
                
            case "reload":
                handleReload(sender);
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
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - categories and global commands
            List<String> options = Arrays.asList("singleplayer", "multiplayer", "help", "reload");
            String input = args[0].toLowerCase();
            for (String option : options) {
                if (option.startsWith(input)) {
                    completions.add(option);
                }
            }
        } else if (args.length == 2) {
            // Second argument - subcommands based on category
            String category = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            List<String> subCommands = new ArrayList<>();
            
            switch (category) {
                case "singleplayer":
                    subCommands = Arrays.asList("create", "edit", "delete", "list", "tp", "info", "reload", "stats", "recent");
                    break;
                case "multiplayer":
                    subCommands = Arrays.asList("create", "edit", "delete", "list", "tp", "info", "reload", "join", "leave", "start", "stats", "recent");
                    break;
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 3) {
            // Third argument - course names for most commands
            String category = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();
            String input = args[2].toLowerCase();
            
            List<String> validCommands = Arrays.asList("edit", "delete", "tp", "info", "join", "stats", "recent");
            
            if (validCommands.contains(subCommand)) {
                Course.CourseType filterType = null;
                if ("singleplayer".equals(category)) {
                    filterType = Course.CourseType.SINGLEPLAYER;
                } else if ("multiplayer".equals(category)) {
                    filterType = Course.CourseType.MULTIPLAYER;
                }
                
                for (String courseName : plugin.getStorageManager().getCourses().keySet()) {
                    if (filterType == null || plugin.getStorageManager().getCourse(courseName).getType() == filterType) {
                        if (courseName.toLowerCase().startsWith(input)) {
                            completions.add(courseName);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
    
    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "help.header");
        
        // Singleplayer Commands (Green)
        MessageUtil.sendMessage(sender, "help.singleplayer-header");
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer create <name>", "description", "Create a new singleplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer edit <name>", "description", "Edit a singleplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer delete <name>", "description", "Delete a singleplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer list", "description", "List all singleplayer courses"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer tp <name>", "description", "Teleport to a singleplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer info <name>", "description", "Get singleplayer course information"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer stats <name>", "description", "View your singleplayer statistics"));
        MessageUtil.sendMessage(sender, "help.command-format-sp", 
            Map.of("command", "/bocrace singleplayer recent <name>", "description", "View your recent singleplayer races"));
        
        // Multiplayer Commands (Aqua)
        MessageUtil.sendMessage(sender, "help.multiplayer-header");
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer create <name>", "description", "Create a new multiplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer edit <name>", "description", "Edit a multiplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer delete <name>", "description", "Delete a multiplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer list", "description", "List all multiplayer courses"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer tp <name>", "description", "Teleport to a multiplayer course"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer info <name>", "description", "Get multiplayer course information"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer join <name>", "description", "Join a multiplayer race lobby"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer leave", "description", "Leave a multiplayer race lobby"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer start", "description", "Start a multiplayer race (lobby leader only)"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer stats <name>", "description", "View your multiplayer statistics"));
        MessageUtil.sendMessage(sender, "help.command-format-mp", 
            Map.of("command", "/bocrace multiplayer recent <name>", "description", "View your recent multiplayer races"));
        
        // Global Commands (Yellow)
        MessageUtil.sendMessage(sender, "help.global-header");
        MessageUtil.sendMessage(sender, "help.command-format-global", 
            Map.of("command", "/bocrace help", "description", "Show this help menu"));
        MessageUtil.sendMessage(sender, "help.command-format-global", 
            Map.of("command", "/bocrace reload", "description", "Reload plugin configuration"));
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
    
    private void handleCreate(CommandSender sender, String[] args, Course.CourseType type) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        String permission = "bocrace." + type.name().toLowerCase() + ".create";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        
        if (plugin.getStorageManager().hasCourse(courseName)) {
            MessageUtil.sendMessage(sender, "course.already-exists", Map.of("course", courseName));
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
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".edit";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        Player player = (Player) sender;
        SetupSession setupSession = new SetupSession(player, courseName, course.getType());
        plugin.getSetupManager().startSetup(player, setupSession);
        
        MessageUtil.sendMessage(sender, "setup.started", Map.of("course", courseName));
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".delete";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.getStorageManager().removeCourse(courseName);
        plugin.getStorageManager().saveCourses();
        
        MessageUtil.sendMessage(sender, "course.deleted", Map.of("course", courseName));
    }
    
    private void handleList(CommandSender sender, Course.CourseType type) {
        String permission = "bocrace." + type.name().toLowerCase() + ".list";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        Map<String, Course> courses = plugin.getStorageManager().getCourses();
        List<Course> filteredCourses = courses.values().stream()
                .filter(course -> course.getType() == type)
                .collect(Collectors.toList());
        
        if (filteredCourses.isEmpty()) {
            MessageUtil.sendMessage(sender, "course.no-courses");
            return;
        }
        
        MessageUtil.sendMessage(sender, "course.list-header");
        for (Course course : filteredCourses) {
            MessageUtil.sendMessage(sender, "course.list-item", 
                Map.of("course", course.getName(), "type", course.getType().name().toLowerCase()));
        }
    }
    
    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendMessage(sender, "general.player-only");
            return;
        }
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".tp";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".info";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        if (course.getType() != Course.CourseType.MULTIPLAYER) {
            MessageUtil.sendMessage(sender, "race.singleplayer-only");
            return;
        }
        
        if (!sender.hasPermission("bocrace.multiplayer.join")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        
        if (!sender.hasPermission("bocrace.multiplayer.leave")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        
        if (!sender.hasPermission("bocrace.multiplayer.start")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".stats";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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
        
        if (args.length < 1) {
            MessageUtil.sendMessage(sender, "general.invalid-arguments");
            return;
        }
        
        String courseName = args[0];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            MessageUtil.sendMessage(sender, "course.not-found", Map.of("course", courseName));
            return;
        }
        
        String permission = "bocrace." + course.getType().name().toLowerCase() + ".recent";
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
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