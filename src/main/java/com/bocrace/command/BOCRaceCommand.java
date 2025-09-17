package com.bocrace.command;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import com.bocrace.model.RaceRecord;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                return true;
            case "reload":
                if (!sender.hasPermission("bocrace.admin")) {
                    sender.sendMessage("§cYou don't have permission to reload the plugin!");
                    return true;
                }
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage("§aConfigs reloaded successfully.");
                return true;
            case "debugcourses":
                if (!sender.hasPermission("bocrace.debug")) {
                    sender.sendMessage("§cYou don't have permission to use debug commands!");
                    return true;
                }
                showDebugCourses(sender);
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
            sender.sendMessage("§cUsage: /bocrace singleplayer <create|setup|delete|list|tp|info|reload|testdata>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "create":
                return handleSingleplayerCreate(sender, args);
            case "setup":
                return handleSingleplayerSetup(sender, args);
            case "delete":
                return handleSingleplayerDelete(sender, args);
            case "list":
                return handleSingleplayerList(sender);
            case "tp":
                return handleSingleplayerTp(sender, args);
            case "info":
                return handleSingleplayerInfo(sender, args);
            case "reload":
                return handleSingleplayerReload(sender);
            case "testdata":
                return handleTestData(sender);
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
                return handleMultiplayerCreate(sender, args);
            case "edit":
                sender.sendMessage("§eMultiplayer edit not implemented yet.");
                return true;
            case "delete":
                return handleMultiplayerDelete(sender, args);
            case "list":
                return handleMultiplayerList(sender);
            case "tp":
                sender.sendMessage("§eMultiplayer tp not implemented yet.");
                return true;
            case "info":
                return handleMultiplayerInfo(sender, args);
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
        sender.sendMessage("§a/bocrace singleplayer setup <name> §7- Setup course components (start button, boat spawn, etc.)");
        sender.sendMessage("§a/bocrace singleplayer delete <name> §7- Delete a singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer list §7- List all singleplayer courses");
        sender.sendMessage("§a/bocrace singleplayer tp <name> §7- Teleport to a singleplayer course");
        sender.sendMessage("§a/bocrace singleplayer info <name> §7- Show course information and usage statistics");
        sender.sendMessage("§a/bocrace singleplayer reload §7- Reload singleplayer courses");
        
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
        
        // Player Statistics Commands (Light Purple)
        sender.sendMessage("§6Player Statistics Commands:");
        sender.sendMessage("§d/racestats recent §7- Show your 5 most recent races");
        sender.sendMessage("§d/racestats top <course> §7- Show top 5 times for a specific course");
        sender.sendMessage("§d/racestats mytimes <course> §7- Show your times for a specific course");
        sender.sendMessage("§d/racestats stats §7- Show your overall race statistics");
        
        // Global Commands (Yellow)
        sender.sendMessage("§6Global Commands:");
        sender.sendMessage("§e/bocrace help §7- Show this help menu");
        sender.sendMessage("§e/bocrace reload §7- Reload plugin configuration");
    }
    
    private void showDebugCourses(CommandSender sender) {
        sender.sendMessage("§6=== Loaded Courses ===");
        
        var courses = plugin.getStorageManager().getAllCourses();
        if (courses.isEmpty()) {
            sender.sendMessage("§7No courses loaded.");
            return;
        }
        
        for (var course : courses) {
            String prefixDisplay = course.getPrefixDisplay();
            sender.sendMessage("§7- " + course.getDisplayName() + " [Prefix: " + prefixDisplay + "]");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First level: singleplayer, multiplayer, help, reload, debugcourses
            List<String> subCommands = Arrays.asList("singleplayer", "multiplayer", "help", "reload", "debugcourses");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("singleplayer")) {
                // Singleplayer subcommands
                List<String> spCommands = Arrays.asList("create", "setup", "delete", "list", "tp", "info", "reload", "testdata");
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
        } else if (args.length == 3) {
            // Third argument - course names for commands that need them
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            
            if (firstArg.equals("singleplayer")) {
                if (secondArg.equals("delete") || secondArg.equals("info") || secondArg.equals("tp") || secondArg.equals("setup")) {
                    // Add singleplayer course names
                    var courses = plugin.getStorageManager().getCoursesByType(CourseType.SINGLEPLAYER);
                    for (Course course : courses) {
                        if (course.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(course.getName());
                        }
                    }
                }
            } else if (firstArg.equals("multiplayer")) {
                if (secondArg.equals("delete") || secondArg.equals("info") || secondArg.equals("tp") || 
                    secondArg.equals("stats") || secondArg.equals("recent")) {
                    // Add multiplayer course names
                    var courses = plugin.getStorageManager().getCoursesByType(CourseType.MULTIPLAYER);
                    for (Course course : courses) {
                        if (course.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(course.getName());
                        }
                    }
                }
            }
        } else if (args.length == 4) {
            // Fourth argument - setup actions for singleplayer setup command
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            
            if (firstArg.equals("singleplayer") && secondArg.equals("setup")) {
                List<String> setupActions = Arrays.asList(
                    "setstartbutton", "setboatspawn", "setstart", "setfinish", 
                    "setreturn", "setcourselobby", "setmainlobby"
                );
                for (String action : setupActions) {
                    if (action.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(action);
                    }
                }
            }
        } else if (args.length == 5) {
            // Fifth argument - point numbers for setstart/setfinish
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            String thirdArg = args[3].toLowerCase();
            
            if (firstArg.equals("singleplayer") && secondArg.equals("setup") && 
                (thirdArg.equals("setstart") || thirdArg.equals("setfinish"))) {
                List<String> pointNumbers = Arrays.asList("1", "2");
                for (String point : pointNumbers) {
                    if (point.startsWith(args[4].toLowerCase())) {
                        completions.add(point);
                    }
                }
            }
        }
        
        return completions;
    }
    
    // Singleplayer command handlers
    private boolean handleSingleplayerCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer create <name>");
            return true;
        }
        
        String courseName = args[2];
        
        // DEBUG: Log command execution
        plugin.debugLog("Singleplayer create command - Player: " + sender.getName() + ", Course: " + courseName);
        
        // Check if course already exists
        if (plugin.getStorageManager().courseExists(courseName)) {
            sender.sendMessage("§cCourse '" + courseName + "' already exists!");
            plugin.debugLog("Course creation failed - course already exists: " + courseName);
            return true;
        }
        
        // Create new course
        Course course = new Course(courseName, CourseType.SINGLEPLAYER, sender.getName());
        plugin.getLogger().info("[DEBUG] Created course object - Name: " + course.getName() + 
            ", Type: " + course.getType() + ", CreatedBy: " + course.getCreatedBy());
        
        plugin.getStorageManager().addCourse(course);
        plugin.getStorageManager().saveCourse(course);
        
        sender.sendMessage("§aSingleplayer course '" + courseName + "' created successfully!");
        plugin.getLogger().info("[DEBUG] Course creation completed successfully: " + courseName);
        return true;
    }
    
    private boolean handleSingleplayerDelete(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] Singleplayer delete command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer delete <name>");
            plugin.getLogger().info("[DEBUG] Delete command failed - insufficient arguments");
            return true;
        }
        
        String courseName = args[2];
        plugin.getLogger().info("[DEBUG] Looking for course to delete: " + courseName);
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
            plugin.getLogger().info("[DEBUG] Delete command failed - course not found or wrong type: " + courseName);
            return true;
        }
        
        plugin.getLogger().info("[DEBUG] Course found for deletion - Name: " + course.getName() + ", Type: " + course.getType());
        plugin.getStorageManager().removeCourse(courseName);
        sender.sendMessage("§aSingleplayer course '" + courseName + "' deleted successfully!");
        plugin.getLogger().info("[DEBUG] Course deleted successfully: " + courseName);
        return true;
    }
    
    private boolean handleSingleplayerList(CommandSender sender) {
        plugin.getLogger().info("[DEBUG] Singleplayer list command called - Player: " + sender.getName());
        
        var courses = plugin.getStorageManager().getCoursesByType(CourseType.SINGLEPLAYER);
        plugin.getLogger().info("[DEBUG] Found " + courses.size() + " singleplayer courses");
        
        sender.sendMessage("§6=== Singleplayer Courses ===");
        if (courses.isEmpty()) {
            sender.sendMessage("§7No singleplayer courses found.");
            plugin.getLogger().info("[DEBUG] No singleplayer courses to display");
        } else {
            for (Course course : courses) {
                sender.sendMessage("§7- " + course.getName() + " [Prefix: " + course.getPrefixDisplay() + "]");
                plugin.getLogger().info("[DEBUG] Displaying course: " + course.getName() + " (Type: " + course.getType() + ")");
            }
        }
        plugin.getLogger().info("[DEBUG] List command completed successfully");
        return true;
    }
    
    private boolean handleSingleplayerInfo(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] Singleplayer info command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer info <name>");
            plugin.getLogger().info("[DEBUG] Info command failed - insufficient arguments");
            return true;
        }
        
        String courseName = args[2];
        plugin.getLogger().info("[DEBUG] Looking for course info: " + courseName);
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
            plugin.getLogger().info("[DEBUG] Info command failed - course not found or wrong type: " + courseName);
            return true;
        }
        
        plugin.getLogger().info("[DEBUG] Course found for info display - Name: " + course.getName() + ", Type: " + course.getType());
        
        sender.sendMessage("§6=== Course Information ===");
        sender.sendMessage("§7Name: §f" + course.getName());
        sender.sendMessage("§7Type: §f" + course.getType());
        sender.sendMessage("§7Prefix: §f" + course.getPrefixDisplay());
        sender.sendMessage("§7Created by: §f" + course.getCreatedBy());
        sender.sendMessage("§7Created on: §f" + course.getCreatedOn());
        sender.sendMessage("§7Last edited: §f" + course.getLastEdited());
        
        // Show setup status
        sender.sendMessage("§6Setup Status:");
        sender.sendMessage("§7- Start Button: " + (course.getSpstartbutton() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Boat Spawn: " + (course.getSpboatspawn() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Start Line Point 1: " + (course.getSpstart1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Start Line Point 2: " + (course.getSpstart2() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 1: " + (course.getSpfinish1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 2: " + (course.getSpfinish2() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Return Point: " + (course.getSpreturn() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Course Lobby: " + (course.getSpcourselobby() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Main Lobby: " + (course.getSpmainlobby() != null ? "§aSET" : "§cNOT SET"));
        
        // Show completion status
        int setCount = 0;
        int totalCount = 9;
        if (course.getSpstartbutton() != null) setCount++;
        if (course.getSpboatspawn() != null) setCount++;
        if (course.getSpstart1() != null) setCount++;
        if (course.getSpstart2() != null) setCount++;
        if (course.getSpfinish1() != null) setCount++;
        if (course.getSpfinish2() != null) setCount++;
        if (course.getSpreturn() != null) setCount++;
        if (course.getSpcourselobby() != null) setCount++;
        if (course.getSpmainlobby() != null) setCount++;
        
        String completionColor = setCount == totalCount ? "§a" : (setCount > 0 ? "§e" : "§c");
        sender.sendMessage("§7Setup Progress: " + completionColor + setCount + "/" + totalCount + " complete");
        
        // Show usage statistics
        sender.sendMessage("§6Usage Statistics:");
        sender.sendMessage("§7Times Used: §f" + course.getUsageCount());
        if (course.getLastUsed() != null) {
            sender.sendMessage("§7Last Used: §f" + course.getLastUsed().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            sender.sendMessage("§7Last Used By: §f" + (course.getLastUsedBy() != null ? course.getLastUsedBy() : "Unknown"));
        } else {
            sender.sendMessage("§7Last Used: §cNever");
        }
        
        plugin.getLogger().info("[DEBUG] Course info displayed - Setup progress: " + setCount + "/" + totalCount + " locations set, Usage: " + course.getUsageCount() + " times");
        return true;
    }
    
    private boolean handleSingleplayerSetup(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] Setup command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer setup <coursename> [action]");
            plugin.getLogger().info("[DEBUG] Setup command failed - insufficient arguments");
            return true;
        }
        
        // Check if the third argument is a setup action (not a course name)
        String thirdArg = args[2].toLowerCase();
        if (thirdArg.startsWith("set")) {
            // This is a setup action, not a course name
            sender.sendMessage("§cUsage: /bocrace singleplayer setup <coursename> " + thirdArg);
            sender.sendMessage("§7You need to specify a course name first!");
            plugin.getLogger().info("[DEBUG] Setup command failed - setup action provided without course name: " + thirdArg);
            return true;
        }
        
        String courseName = args[2];
        plugin.getLogger().info("[DEBUG] Looking for course: " + courseName);
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
            plugin.getLogger().info("[DEBUG] Setup command failed - course not found: " + courseName);
            return true;
        }
        
        plugin.getLogger().info("[DEBUG] Course found - Name: " + course.getName() + ", Type: " + course.getType());
        
        if (args.length == 3) {
            // Show setup options
            sender.sendMessage("§6=== Setup Options for '" + courseName + "' ===");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setstartbutton §7- Set the start button location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setboatspawn §7- Set the boat spawn location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setstart <1|2> §7- Set start line points");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setfinish <1|2> §7- Set finish line points");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setreturn §7- Set return/restart location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setcourselobby §7- Set course lobby location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setmainlobby §7- Set main lobby location");
            plugin.getLogger().info("[DEBUG] Setup options displayed for course: " + courseName);
            return true;
        }
        
        String setupAction = args[3].toLowerCase();
        plugin.getLogger().info("[DEBUG] Setup action requested: " + setupAction);
        
        switch (setupAction) {
            case "setstartbutton":
                return handleSetStartButton(sender, course);
            case "setboatspawn":
                return handleSetBoatSpawn(sender, course);
            case "setstart":
                return handleSetStart(sender, args, course);
            case "setfinish":
                return handleSetFinish(sender, args, course);
            case "setreturn":
                return handleSetReturn(sender, course);
            case "setcourselobby":
                return handleSetCourseLobby(sender, course);
            case "setmainlobby":
                return handleSetMainLobby(sender, course);
            default:
                sender.sendMessage("§cUnknown setup action. Use /bocrace singleplayer setup " + courseName + " to see available options.");
                plugin.getLogger().info("[DEBUG] Setup command failed - unknown action: " + setupAction);
                return true;
        }
    }
    
    private boolean handleSetStartButton(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetStartButton called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetStartButton failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getLogger().info("[DEBUG] Player location: " + player.getLocation().getWorld().getName() + 
            " " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ());
        
        // Put player in setup mode
        plugin.setPlayerSetupMode(player, course.getName(), "setstartbutton");
        
        player.sendMessage("§eRight-click the start button for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setstartbutton");
        return true;
    }
    
    private boolean handleSetBoatSpawn(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetBoatSpawn called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetBoatSpawn failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getLogger().info("[DEBUG] Player location: " + player.getLocation().getWorld().getName() + 
            " " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ());
        
        // Put player in setup mode
        plugin.setPlayerSetupMode(player, course.getName(), "setboatspawn");
        
        player.sendMessage("§eRight-click where boats should spawn for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setboatspawn");
        return true;
    }
    
    private boolean handleSingleplayerTp(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] Singleplayer tp command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] Tp command failed - not a player");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer tp <coursename>");
            plugin.getLogger().info("[DEBUG] Tp command failed - insufficient arguments");
            return true;
        }
        
        String courseName = args[2];
        plugin.getLogger().info("[DEBUG] Looking for course to teleport to: " + courseName);
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
            plugin.getLogger().info("[DEBUG] Tp command failed - course not found or wrong type: " + courseName);
            return true;
        }
        
        Player player = (Player) sender;
        
        // Try to teleport to start button first, then boat spawn
        Location teleportLocation = null;
        if (course.getSpstartbutton() != null) {
            teleportLocation = course.getSpstartbutton();
            plugin.getLogger().info("[DEBUG] Teleporting to start button location");
        } else if (course.getSpboatspawn() != null) {
            teleportLocation = course.getSpboatspawn();
            plugin.getLogger().info("[DEBUG] Teleporting to boat spawn location (no start button)");
        } else {
            sender.sendMessage("§cCourse '" + courseName + "' has no setup locations! Please set up the course first.");
            plugin.getLogger().info("[DEBUG] Tp command failed - no setup locations for course: " + courseName);
            return true;
        }
        
        player.teleport(teleportLocation);
        sender.sendMessage("§aTeleported to course '" + courseName + "'!");
        plugin.getLogger().info("[DEBUG] Player teleported to course: " + courseName + " at location: " + teleportLocation.toString());
        return true;
    }
    
    private boolean handleSingleplayerReload(CommandSender sender) {
        plugin.getLogger().info("[DEBUG] Singleplayer reload command called - Player: " + sender.getName());
        
        try {
            plugin.getConfigManager().loadConfigs();
            plugin.getStorageManager().loadCourses();
            sender.sendMessage("§aPlugin configuration and courses reloaded successfully!");
            plugin.getLogger().info("[DEBUG] Reload completed successfully");
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload configuration: " + e.getMessage());
            plugin.getLogger().severe("[DEBUG] Reload failed: " + e.getMessage());
        }
        return true;
    }
    
    
    private boolean handleTestData(CommandSender sender) {
        plugin.debugLog("Test data command called - Player: " + sender.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        String playerName = player.getName();
        
        // Test saving race records
        sender.sendMessage("§6=== Testing New Data Structure ===");
        
        // Test 1: Save some mock race records
        plugin.getRecordManager().saveRaceRecord(playerName, "TestCourse1", 45.2, CourseType.SINGLEPLAYER);
        plugin.getRecordManager().saveRaceRecord(playerName, "TestCourse1", 42.1, CourseType.SINGLEPLAYER);
        plugin.getRecordManager().saveRaceRecord(playerName, "TestCourse2", 38.7, CourseType.SINGLEPLAYER);
        
        sender.sendMessage("§a✓ Saved 3 test race records");
        
        // Test 2: Get top times for TestCourse1
        List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes("TestCourse1", 5);
        sender.sendMessage("§a✓ Retrieved " + topTimes.size() + " top times for TestCourse1");
        
        // Test 3: Get player recent races
        List<RaceRecord> recentRaces = plugin.getRecordManager().getPlayerRecent(playerName, 5);
        sender.sendMessage("§a✓ Retrieved " + recentRaces.size() + " recent races for " + playerName);
        
        // Test 4: Get player stats
        int totalRaces = plugin.getRecordManager().getPlayerTotalRaces(playerName);
        int spRaces = plugin.getRecordManager().getPlayerRacesByType(playerName, CourseType.SINGLEPLAYER);
        sender.sendMessage("§a✓ Player stats - Total: " + totalRaces + ", SP: " + spRaces);
        
        // Test 5: Test course usage tracking
        Course testCourse = plugin.getStorageManager().getCourse("Herewego1");
        if (testCourse != null) {
            testCourse.recordUsage(playerName);
            plugin.getStorageManager().saveCourse(testCourse);
            sender.sendMessage("§a✓ Recorded usage for course: " + testCourse.getName());
            sender.sendMessage("§7  Usage count: " + testCourse.getUsageCount());
            sender.sendMessage("§7  Last used by: " + testCourse.getLastUsedBy());
        } else {
            sender.sendMessage("§c✗ No course found to test usage tracking");
        }
        
        sender.sendMessage("§6=== Test Complete ===");
        sender.sendMessage("§7Check the plugin data folder for the new organized structure:");
        sender.sendMessage("§7  data/singleplayer/courses/");
        sender.sendMessage("§7  data/singleplayer/players/");
        sender.sendMessage("§7  data/multiplayer/ (ready for future)");
        plugin.debugLog("New data structure test completed successfully");
        
        return true;
    }
    
    
    // Additional setup command handlers
    private boolean handleSetStart(CommandSender sender, String[] args, Course course) {
        plugin.debugLog("SetStart called - Player: " + sender.getName() + ", Course: " + course.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.debugLog("SetStart failed - not a player");
            return true;
        }
        
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /bocrace singleplayer setup " + course.getName() + " setstart <1|2>");
            sender.sendMessage("§7Use 1 for first start line point, 2 for second start line point");
            plugin.debugLog("SetStart failed - missing point number");
            return true;
        }
        
        String point = args[4];
        if (!point.equals("1") && !point.equals("2")) {
            sender.sendMessage("§cInvalid point number. Use 1 or 2");
            plugin.debugLog("SetStart failed - invalid point: " + point);
            return true;
        }
        
        Player player = (Player) sender;
        String action = "setstart" + point;
        plugin.setPlayerSetupMode(player, course.getName(), action);
        
        player.sendMessage("§eRight-click start line point " + point + " for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.debugLog("Player " + player.getName() + " entered setup mode for " + action);
        return true;
    }
    
    private boolean handleSetFinish(CommandSender sender, String[] args, Course course) {
        plugin.debugLog("SetFinish called - Player: " + sender.getName() + ", Course: " + course.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.debugLog("SetFinish failed - not a player");
            return true;
        }
        
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /bocrace singleplayer setup " + course.getName() + " setfinish <1|2>");
            sender.sendMessage("§7Use 1 for first finish line point, 2 for second finish line point");
            plugin.debugLog("SetFinish failed - missing point number");
            return true;
        }
        
        String point = args[4];
        if (!point.equals("1") && !point.equals("2")) {
            sender.sendMessage("§cInvalid point number. Use 1 or 2");
            plugin.debugLog("SetFinish failed - invalid point: " + point);
            return true;
        }
        
        Player player = (Player) sender;
        String action = "setfinish" + point;
        plugin.setPlayerSetupMode(player, course.getName(), action);
        
        player.sendMessage("§eRight-click finish line point " + point + " for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.debugLog("Player " + player.getName() + " entered setup mode for " + action);
        return true;
    }
    
    private boolean handleSetReturn(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetReturn called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetReturn failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.setPlayerSetupMode(player, course.getName(), "setreturn");
        
        player.sendMessage("§eRight-click the return/restart location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setreturn");
        return true;
    }
    
    private boolean handleSetCourseLobby(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetCourseLobby called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetCourseLobby failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.setPlayerSetupMode(player, course.getName(), "setcourselobby");
        
        player.sendMessage("§eRight-click the course lobby location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setcourselobby");
        return true;
    }
    
    private boolean handleSetMainLobby(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetMainLobby called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetMainLobby failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.setPlayerSetupMode(player, course.getName(), "setmainlobby");
        
        player.sendMessage("§eRight-click the main lobby location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setmainlobby");
        return true;
    }
    
    
    // Multiplayer command handlers
    private boolean handleMultiplayerCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer create <name>");
            return true;
        }
        
        String courseName = args[2];
        
        // Check if course already exists
        if (plugin.getStorageManager().courseExists(courseName)) {
            sender.sendMessage("§cCourse '" + courseName + "' already exists!");
            return true;
        }
        
        // Create new course
        Course course = new Course(courseName, CourseType.MULTIPLAYER, sender.getName());
        plugin.getStorageManager().addCourse(course);
        plugin.getStorageManager().saveCourse(course);
        
        sender.sendMessage("§aMultiplayer course '" + courseName + "' created successfully!");
        return true;
    }
    
    private boolean handleMultiplayerDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer delete <name>");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.MULTIPLAYER) {
            sender.sendMessage("§cMultiplayer course '" + courseName + "' not found!");
            return true;
        }
        
        plugin.getStorageManager().removeCourse(courseName);
        sender.sendMessage("§aMultiplayer course '" + courseName + "' deleted successfully!");
        return true;
    }
    
    private boolean handleMultiplayerList(CommandSender sender) {
        var courses = plugin.getStorageManager().getCoursesByType(CourseType.MULTIPLAYER);
        
        sender.sendMessage("§6=== Multiplayer Courses ===");
        if (courses.isEmpty()) {
            sender.sendMessage("§7No multiplayer courses found.");
        } else {
            for (Course course : courses) {
                sender.sendMessage("§7- " + course.getName() + " [Prefix: " + course.getPrefixDisplay() + "]");
            }
        }
        return true;
    }
    
    private boolean handleMultiplayerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer info <name>");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.MULTIPLAYER) {
            sender.sendMessage("§cMultiplayer course '" + courseName + "' not found!");
            return true;
        }
        
        sender.sendMessage("§6=== Course Information ===");
        sender.sendMessage("§7Name: §f" + course.getName());
        sender.sendMessage("§7Type: §f" + course.getType());
        sender.sendMessage("§7Prefix: §f" + course.getPrefixDisplay());
        sender.sendMessage("§7Created by: §f" + course.getCreatedBy());
        sender.sendMessage("§7Created on: §f" + course.getCreatedOn());
        sender.sendMessage("§7Last edited: §f" + course.getLastEdited());
        return true;
    }
}
