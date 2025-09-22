package com.bocrace.command;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import java.util.HashMap;
import java.util.Map;
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
                plugin.getStorageManager().loadCourses(); // Also reload course settings
                sender.sendMessage("§aConfigs and courses reloaded successfully.");
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
            sender.sendMessage("§cUsage: /bocrace singleplayer <create|setup|delete|list|tp|info|reload|testdata|racedebug>");
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
            case "racedebug":
                return handleRaceDebug(sender);
            default:
                sender.sendMessage("§cUnknown singleplayer command. Use /bocrace help for available commands.");
                return true;
        }
    }
    
    private boolean handleMultiplayerCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bocrace multiplayer <create|setup|delete|list|tp|info|reload|stats|recent>");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "create":
                return handleMultiplayerCreate(sender, args);
            case "setup":
                return handleMultiplayerSetup(sender, args);
            case "edit":
                sender.sendMessage("§eMultiplayer edit not implemented yet.");
                return true;
            case "delete":
                return handleMultiplayerDelete(sender, args);
            case "list":
                return handleMultiplayerList(sender);
            case "tp":
                return handleMultiplayerTp(sender, args);
            case "info":
                return handleMultiplayerInfo(sender, args);
            case "reload":
                sender.sendMessage("§eMultiplayer reload not implemented yet.");
                return true;
            case "stats":
                return handleMultiplayerStats(sender, args);
            case "recent":
                return handleMultiplayerRecent(sender, args);
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
        sender.sendMessage("§b/bocrace multiplayer setup <name> §7- Setup course components (buttons, spawns, etc.)");
        sender.sendMessage("§b/bocrace multiplayer edit <name> §7- Edit an existing multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer delete <name> §7- Delete a multiplayer course");
        sender.sendMessage("§b/bocrace multiplayer list §7- List all multiplayer courses");
        sender.sendMessage("§b/bocrace multiplayer tp <name> §7- Teleport to a multiplayer course race lobby");
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
                List<String> spCommands = Arrays.asList("create", "setup", "delete", "list", "tp", "info", "reload", "testdata", "racedebug");
                for (String spCommand : spCommands) {
                    if (spCommand.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(spCommand);
                    }
                }
            } else if (firstArg.equals("multiplayer")) {
                // Multiplayer subcommands
                List<String> mpCommands = Arrays.asList("create", "setup", "edit", "delete", "list", "tp", "info", "reload", "stats", "recent");
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
                    secondArg.equals("stats") || secondArg.equals("recent") || secondArg.equals("setup")) {
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
            // Fourth argument - setup actions
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            
            if (firstArg.equals("singleplayer") && secondArg.equals("setup")) {
                List<String> setupActions = Arrays.asList(
                    "setmainlobbybutton", "setcourselobbybutton", "setboatspawn", "setboattype", "setstartlinepoints", "setfinishlinepoints", 
                    "setreturnmainbutton", "setcourselobbyspawn", "setmainlobbyspawn"
                );
                for (String action : setupActions) {
                    if (action.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(action);
                    }
                }
            } else if (firstArg.equals("multiplayer") && secondArg.equals("setup")) {
                List<String> mpSetupActions = Arrays.asList(
                    "setmpracelobbyspawn", "setmpcreateracebutton", "setmpstartracebutton", "setmpjoinracebutton", 
                    "setmpcancelracebutton", "setmpreturnbutton", "setmpboatspawn", "setstartlinepoints", "setfinishlinepoints"
                );
                for (String action : mpSetupActions) {
                    if (action.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(action);
                    }
                }
            }
        } else if (args.length == 5) {
            // Fifth argument - point numbers for setstart/setfinish or boat spawn numbers
            String firstArg = args[0].toLowerCase();
            String secondArg = args[1].toLowerCase();
            String fourthArg = args[3].toLowerCase();
            
            if (secondArg.equals("setup")) {
                if (fourthArg.equals("setstartlinepoints") || fourthArg.equals("setfinishlinepoints")) {
                    // Point numbers for start/finish lines
                    List<String> pointNumbers = Arrays.asList("1", "2");
                    for (String point : pointNumbers) {
                        if (point.startsWith(args[4].toLowerCase())) {
                            completions.add(point);
                        }
                    }
                } else if (fourthArg.equals("setboattype")) {
                    // Boat type options
                    List<String> boatTypes = Arrays.asList("oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "pale_oak");
                    for (String boatType : boatTypes) {
                        if (boatType.startsWith(args[4].toLowerCase())) {
                            completions.add(boatType);
                        }
                    }
                } else if (firstArg.equals("multiplayer") && fourthArg.equals("setmpboatspawn")) {
                    // Boat spawn numbers for multiplayer
                    List<String> spawnNumbers = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
                    for (String spawn : spawnNumbers) {
                        if (spawn.startsWith(args[4].toLowerCase())) {
                            completions.add(spawn);
                        }
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
        
        // Set default per-course settings (so admins can easily customize)
        course.setSoundsEnabled(true);      // Default: sounds enabled
        course.setParticlesEnabled(true);   // Default: particles enabled
        
        // Set default custom messages (so admins can see what can be customized)
        Map<String, String> defaultMessages = new HashMap<>();
        defaultMessages.put("race-start", "§a§l🏁 RACE STARTED! §a§lGO GO GO!");
        defaultMessages.put("race-finish", "§6§l🏆 RACE FINISHED! §6§lTime: {time}");
        defaultMessages.put("personal-best", "§a§l⭐ NEW PERSONAL BEST! §a§l⭐");
        course.setCustomMessages(defaultMessages);
        
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
        
        // Show setup status with correct logic
        sender.sendMessage("§6Setup Status:");
        
        // Show button setup based on lobby configuration
        if (course.getSpmainlobby() != null) {
            // Main lobby is set, so main lobby button is required
            sender.sendMessage("§7- Main Lobby Spawn: " + (course.getSpmainlobby() != null ? "§aSET" : "§cNOT SET"));
            sender.sendMessage("§7- Main Lobby Button: " + (course.getSpmainlobbybutton() != null ? "§aSET" : "§cNOT SET §c(REQUIRED)"));
            sender.sendMessage("§7- Course Lobby Button: " + (course.getSpcourselobbybutton() != null ? "§aSET" : "§7NOT SET §8(optional)"));
        } else {
            // No main lobby, so course lobby button is required
            sender.sendMessage("§7- Main Lobby Spawn: §7NOT SET §8(optional)");
            sender.sendMessage("§7- Main Lobby Button: " + (course.getSpmainlobbybutton() != null ? "§aSET" : "§7NOT SET §8(optional)"));
            sender.sendMessage("§7- Course Lobby Button: " + (course.getSpcourselobbybutton() != null ? "§aSET" : "§cNOT SET §c(REQUIRED)"));
        }
        
        sender.sendMessage("§7- Boat Spawn: " + (course.getSpboatspawn() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Boat Type: " + (course.getBoatType() != null ? "§e" + course.getBoatType().toLowerCase() : "§7oak (default)"));
        sender.sendMessage("§7- Start Line Point 1: " + (course.getSpstart1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Start Line Point 2: " + (course.getSpstart2() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 1: " + (course.getSpfinish1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 2: " + (course.getSpfinish2() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Return Button: " + (course.getSpreturn() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Course Lobby Spawn: " + (course.getSpcourselobby() != null ? "§aSET" : "§cNOT SET"));
        
        // Calculate completion status with correct logic
        int setCount = 0;
        int totalCount = 8; // Fixed total (not 9)
        
        // Count required components
        if (course.getSpboatspawn() != null) setCount++;
        if (course.getSpstart1() != null) setCount++;
        if (course.getSpstart2() != null) setCount++;
        if (course.getSpfinish1() != null) setCount++;
        if (course.getSpfinish2() != null) setCount++;
        if (course.getSpreturn() != null) setCount++;
        if (course.getSpcourselobby() != null) setCount++;
        
        // Count button setup based on lobby configuration
        if (course.getSpmainlobby() != null) {
            // Main lobby is set, main lobby button is required
            if (course.getSpmainlobbybutton() != null) setCount++;
        } else {
            // No main lobby, course lobby button is required
            if (course.getSpcourselobbybutton() != null) setCount++;
        }
        
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
            sender.sendMessage("§e§lStart Buttons:");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setmainlobbybutton §7- Main lobby button (teleports busy players to course lobby)");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setcourselobbybutton §7- Course lobby button (shows busy message only)");
            sender.sendMessage("§e§lCourse Components:");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setboatspawn §7- Set the boat spawn location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setboattype <type> §7- Set boat type (oak, cherry, bamboo, etc.)");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setstartlinepoints <1|2> §7- Set start line points");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setfinishlinepoints <1|2> §7- Set finish line points");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setreturnmainbutton §7- Set return/restart button location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setcourselobbyspawn §7- Set course lobby spawn location");
            sender.sendMessage("§a/bocrace singleplayer setup " + courseName + " setmainlobbyspawn §7- Set main lobby spawn location");
            plugin.getLogger().info("[DEBUG] Setup options displayed for course: " + courseName);
            return true;
        }
        
        String setupAction = args[3].toLowerCase();
        plugin.getLogger().info("[DEBUG] Setup action requested: " + setupAction);
        
        switch (setupAction) {
            case "setmainlobbybutton":
                return handleSetMainLobbyButton(sender, course);
            case "setcourselobbybutton":
                return handleSetCourseLobbyButton(sender, course);
            case "setboatspawn":
                return handleSetBoatSpawn(sender, course);
            case "setboattype":
                return handleSetBoatType(sender, args, course);
            case "setstartlinepoints":
                return handleSetStart(sender, args, course);
            case "setfinishlinepoints":
                return handleSetFinish(sender, args, course);
            case "setreturnmainbutton":
                return handleSetReturn(sender, course);
            case "setcourselobbyspawn":
                return handleSetCourseLobby(sender, course);
            case "setmainlobbyspawn":
                return handleSetMainLobby(sender, course);
            default:
                sender.sendMessage("§cUnknown setup action. Use /bocrace singleplayer setup " + courseName + " to see available options.");
                plugin.getLogger().info("[DEBUG] Setup command failed - unknown action: " + setupAction);
                return true;
        }
    }
    
    private boolean handleSetMainLobbyButton(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetMainLobbyButton called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetMainLobbyButton failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getLogger().info("[DEBUG] Player location: " + player.getLocation().getWorld().getName() + 
            " " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ());
        
        // Put player in setup mode
        plugin.setPlayerSetupMode(player, course.getName(), "setmainlobbybutton");
        
        player.sendMessage("§eRight-click the main lobby start button for course '" + course.getName() + "'");
        player.sendMessage("§7This button will teleport busy players to course lobby to wait");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setmainlobbybutton");
        return true;
    }
    
    private boolean handleSetCourseLobbyButton(CommandSender sender, Course course) {
        plugin.getLogger().info("[DEBUG] SetCourseLobbyButton called - Player: " + sender.getName() + ", Course: " + course.getName());
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.getLogger().info("[DEBUG] SetCourseLobbyButton failed - not a player");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getLogger().info("[DEBUG] Player location: " + player.getLocation().getWorld().getName() + 
            " " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ());
        
        // Put player in setup mode
        plugin.setPlayerSetupMode(player, course.getName(), "setcourselobbybutton");
        
        player.sendMessage("§eRight-click the course lobby start button for course '" + course.getName() + "'");
        player.sendMessage("§7This button will show busy message only (no teleport)");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setcourselobbybutton");
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
    
    private boolean handleSetBoatType(CommandSender sender, String[] args, Course course) {
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /bocrace singleplayer setup " + course.getName() + " setboattype <type>");
            sender.sendMessage("§7Available types: oak, birch, spruce, jungle, acacia, dark_oak, mangrove, cherry, bamboo, pale_oak");
            return true;
        }
        
        String boatTypeName = args[4].toLowerCase();
        
        // Validate boat type
        String[] validTypes = {"oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "pale_oak"};
        boolean isValid = false;
        for (String validType : validTypes) {
            if (validType.equals(boatTypeName)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            sender.sendMessage("§cInvalid boat type: " + boatTypeName);
            sender.sendMessage("§7Available types: oak, birch, spruce, jungle, acacia, dark_oak, mangrove, cherry, bamboo, pale_oak");
            return true;
        }
        
        // Set the boat type
        course.setBoatType(boatTypeName.toUpperCase());
        plugin.getStorageManager().saveCourse(course);
        
        sender.sendMessage("§aBoat type set to " + boatTypeName + " for course '" + course.getName() + "'!");
        sender.sendMessage("§7New races will spawn " + boatTypeName + " boats.");
        
        plugin.debugLog("Boat type set for course " + course.getName() + ": " + boatTypeName);
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
        
        // Use lobby priority system: mainLobby first, then courseLobby (required)
        Location teleportLocation = null;
        String locationName = "";
        
        if (course.getSpmainlobby() != null) {
            teleportLocation = course.getSpmainlobby();
            locationName = "main lobby";
            plugin.debugLog("Teleporting to main lobby location");
        } else if (course.getSpcourselobby() != null) {
            teleportLocation = course.getSpcourselobby();
            locationName = "course lobby";
            plugin.debugLog("Teleporting to course lobby location (no main lobby set)");
        } else {
            sender.sendMessage("§cCourse '" + courseName + "' has no lobby locations set! Please set up courseLobby (required) or mainLobby.");
            plugin.debugLog("Tp command failed - no lobby locations for course: " + courseName);
            return true;
        }
        
        player.teleport(teleportLocation);
        sender.sendMessage("§aTeleported to " + locationName + " for course '" + courseName + "'!");
        plugin.debugLog("Player teleported to " + locationName + " for course: " + courseName + " at " + teleportLocation.getWorld().getName() + " " + teleportLocation.getBlockX() + "," + teleportLocation.getBlockY() + "," + teleportLocation.getBlockZ());
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
    
    private boolean handleRaceDebug(CommandSender sender) {
        if (!sender.hasPermission("bocrace.admin")) {
            sender.sendMessage("§cYou don't have permission to use debug commands!");
            return true;
        }
        
        sender.sendMessage("§6=== Race Manager Debug Info ===");
        
        // Test RaceManager initialization
        if (plugin.getRaceManager() == null) {
            sender.sendMessage("§c✗ RaceManager is NULL - CRITICAL ERROR!");
            return true;
        }
        
        sender.sendMessage("§a✓ RaceManager initialized successfully");
        
        // Show race statistics
        String stats = plugin.getRaceManager().getStats();
        sender.sendMessage("§7" + stats);
        
        // Test course validation
        var courses = plugin.getStorageManager().getCoursesByType(com.bocrace.model.CourseType.SINGLEPLAYER);
        sender.sendMessage("§7Testing course validation for " + courses.size() + " singleplayer courses:");
        
        for (var course : courses) {
            boolean isReady = plugin.getRaceManager().isRaceReady(course);
            String status = isReady ? "§a✓ READY" : "§c✗ NOT READY";
            sender.sendMessage("§7- " + course.getName() + ": " + status);
            
            if (!isReady) {
                // Show what's missing
                StringBuilder missing = new StringBuilder("§7  Missing: ");
                if (course.getSpstartbutton() == null) missing.append("startbutton ");
                if (course.getSpboatspawn() == null) missing.append("boatspawn ");
                if (course.getSpstart1() == null || course.getSpstart2() == null) missing.append("startline ");
                if (course.getSpfinish1() == null || course.getSpfinish2() == null) missing.append("finishline ");
                if (course.getSpcourselobby() == null) missing.append("courselobby ");
                sender.sendMessage(missing.toString());
            }
        }
        
        sender.sendMessage("§6=== Debug Complete ===");
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
        plugin.setPlayerSetupMode(player, course.getName(), "setreturnmainbutton");
        
        player.sendMessage("§eRight-click the return/restart location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setreturnmainbutton");
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
        plugin.setPlayerSetupMode(player, course.getName(), "setcourselobbyspawn");
        
        player.sendMessage("§eRight-click the course lobby location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setcourselobbyspawn");
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
        plugin.setPlayerSetupMode(player, course.getName(), "setmainlobbyspawn");
        
        player.sendMessage("§eRight-click the main lobby location for course '" + course.getName() + "'");
        player.sendMessage("§7You have 30 seconds to right-click a block!");
        plugin.getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode for setmainlobbyspawn");
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
    
    private boolean handleMultiplayerSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer setup <name> [action]");
            sender.sendMessage("§7Available actions:");
            sender.sendMessage("§7  setmpraceLobbySpawn - Set race lobby spawn point");
            sender.sendMessage("§7  setmpcreateRaceButton - Set create race button");
            sender.sendMessage("§7  setmpstartRaceButton - Set start race button (triggers redstone)");
            sender.sendMessage("§7  setmpjoinRaceButton - Set join race button");
            sender.sendMessage("§7  setmpcancelRaceButton - Set cancel race button (optional)");
            sender.sendMessage("§7  setmpreturnButton - Set return to lobby button");
            sender.sendMessage("§7  setmpboatspawn <1-10> - Set boat spawn point (1-10)");
            sender.sendMessage("§7  setstartlinepoints - Set start line detection points");
            sender.sendMessage("§7  setfinishlinepoints - Set finish line detection points");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.MULTIPLAYER) {
            sender.sendMessage("§cMultiplayer course '" + courseName + "' not found!");
            return true;
        }
        
        if (args.length < 4) {
            // Show current setup status
            showMultiplayerSetupStatus(sender, course);
            return true;
        }
        
        String action = args[3].toLowerCase();
        Player player = (Player) sender;
        
        return handleMultiplayerSetupAction(player, course, action, args);
    }
    
    private void showMultiplayerSetupStatus(CommandSender sender, Course course) {
        sender.sendMessage("§6=== Multiplayer Course Setup: " + course.getName() + " ===");
        sender.sendMessage("§7Race Lobby Spawn: " + (course.getMpraceLobbySpawn() != null ? "§aSet" : "§cNot Set"));
        sender.sendMessage("§7Create Race Button: " + (course.getMpcreateRaceButton() != null ? "§aSet" : "§cNot Set"));
        sender.sendMessage("§7Start Race Button: " + (course.getMpstartRaceButton() != null ? "§aSet" : "§cNot Set"));
        sender.sendMessage("§7Join Race Button: " + (course.getMpjoinRaceButton() != null ? "§aSet" : "§cNot Set"));
        sender.sendMessage("§7Cancel Race Button: " + (course.getMpcancelRaceButton() != null ? "§aSet" : "§7Optional"));
        sender.sendMessage("§7Return Button: " + (course.getMpreturnButton() != null ? "§aSet" : "§cNot Set"));
        
        int boatSpawns = course.getMpboatSpawns() != null ? course.getMpboatSpawns().size() : 0;
        sender.sendMessage("§7Boat Spawns: §e" + boatSpawns + "/10 " + (boatSpawns >= 2 ? "§a✓" : "§c(Need at least 2)"));
        
        sender.sendMessage("§7Start Line: " + (course.getSpstart1() != null && course.getSpstart2() != null ? "§aSet" : "§cNot Set"));
        sender.sendMessage("§7Finish Line: " + (course.getSpfinish1() != null && course.getSpfinish2() != null ? "§aSet" : "§cNot Set"));
    }
    
    private boolean handleMultiplayerSetupAction(Player player, Course course, String action, String[] args) {
        
        switch (action) {
            case "setmpracelobbyspawn":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpracelobbyspawn");
                player.sendMessage("§eRight-click where players should spawn in the race lobby for course '" + course.getName() + "'");
                player.sendMessage("§7You have 30 seconds to right-click a block!");
                break;
                
            case "setmpcreateracebutton":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpcreateracebutton");
                player.sendMessage("§eRight-click the CREATE RACE button for course '" + course.getName() + "'");
                player.sendMessage("§7This button creates multiplayer race sessions (OP only)");
                player.sendMessage("§7You have 30 seconds to right-click a button!");
                break;
                
            case "setmpstartracebutton":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpstartracebutton");
                player.sendMessage("§eRight-click the START RACE button for course '" + course.getName() + "'");
                player.sendMessage("§7This button starts the race AND triggers redstone!");
                player.sendMessage("§7You have 30 seconds to right-click a button!");
                break;
                
            case "setmpjoinracebutton":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpjoinracebutton");
                player.sendMessage("§eRight-click the JOIN RACE button for course '" + course.getName() + "'");
                player.sendMessage("§7This button lets players join active races");
                player.sendMessage("§7You have 30 seconds to right-click a button!");
                break;
                
            case "setmpcancelracebutton":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpcancelracebutton");
                player.sendMessage("§eRight-click the CANCEL RACE button for course '" + course.getName() + "'");
                player.sendMessage("§7This button cancels races (leader only, optional)");
                player.sendMessage("§7You have 30 seconds to right-click a button!");
                break;
                
            case "setmpreturnbutton":
                plugin.setPlayerSetupMode(player, course.getName(), "setmpreturnbutton");
                player.sendMessage("§eRight-click the RETURN TO LOBBY button for course '" + course.getName() + "'");
                player.sendMessage("§7This button teleports players back to race lobby");
                player.sendMessage("§7You have 30 seconds to right-click a button!");
                break;
                
            case "setmpboatspawn":
                if (args.length < 5) {
                    player.sendMessage("§cUsage: /bocrace multiplayer setup " + course.getName() + " setmpboatspawn <1-10>");
                    return true;
                }
                
                String spawnIndexStr = args[4];
                try {
                    int spawnIndex = Integer.parseInt(spawnIndexStr);
                    if (spawnIndex < 1 || spawnIndex > 10) {
                        player.sendMessage("§cBoat spawn index must be between 1 and 10!");
                        return true;
                    }
                    
                    plugin.setPlayerSetupMode(player, course.getName(), "setmpboatspawn" + spawnIndex);
                    player.sendMessage("§eRight-click where boat #" + spawnIndex + " should spawn for course '" + course.getName() + "'");
                    player.sendMessage("§7Boat will face the direction you're looking when you click");
                    player.sendMessage("§7You have 30 seconds to right-click a block!");
                    
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid spawn index. Use a number between 1 and 10.");
                    return true;
                }
                break;
                
            case "setstartlinepoints":
                // Reuse singleplayer logic for start line
                return handleSetStart(player, args, course);
                
            case "setfinishlinepoints":
                // Reuse singleplayer logic for finish line
                return handleSetFinish(player, args, course);
                
            default:
                player.sendMessage("§cUnknown setup action: " + action);
                return true;
        }
        
        // Setup mode initiated - no immediate saving (SetupListener handles the actual setting)
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
    
    private boolean handleMultiplayerTp(CommandSender sender, String[] args) {
        plugin.debugLog("Multiplayer tp command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            plugin.debugLog("Multiplayer tp command failed - not a player");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer tp <coursename>");
            plugin.debugLog("Multiplayer tp command failed - insufficient arguments");
            return true;
        }
        
        String courseName = args[2];
        plugin.debugLog("Looking for multiplayer course to teleport to: " + courseName);
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.MULTIPLAYER) {
            sender.sendMessage("§cMultiplayer course '" + courseName + "' not found!");
            plugin.debugLog("Multiplayer tp command failed - course not found or wrong type: " + courseName);
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if mpraceLobbySpawn is set (this is the designated spawn location for multiplayer)
        Location teleportLocation = course.getMpraceLobbySpawn();
        
        if (teleportLocation == null) {
            sender.sendMessage("§cCourse '" + courseName + "' has no race lobby spawn set! Please set up the race lobby spawn location first.");
            sender.sendMessage("§7Use: /bocrace multiplayer setup " + courseName + " setraceLobbySpawn");
            plugin.debugLog("Multiplayer tp command failed - no race lobby spawn for course: " + courseName);
            return true;
        }
        
        // Teleport to the race lobby spawn
        player.teleport(teleportLocation);
        sender.sendMessage("§aTeleported to race lobby for course '" + courseName + "'!");
        sender.sendMessage("§7Use the buttons here to create/join multiplayer races!");
        plugin.debugLog("Player teleported to race lobby for course: " + courseName + " at " + 
                       teleportLocation.getWorld().getName() + " " + 
                       teleportLocation.getBlockX() + "," + teleportLocation.getBlockY() + "," + teleportLocation.getBlockZ());
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
        
        sender.sendMessage("§6=== Multiplayer Course Information ===");
        sender.sendMessage("§7Name: §f" + course.getName());
        sender.sendMessage("§7Type: §f" + course.getType());
        sender.sendMessage("§7Prefix: §f" + course.getPrefixDisplay());
        sender.sendMessage("§7Created by: §f" + course.getCreatedBy());
        sender.sendMessage("§7Created on: §f" + course.getCreatedOn());
        sender.sendMessage("§7Last edited: §f" + course.getLastEdited());
        
        // Show multiplayer setup status (like singleplayer does)
        sender.sendMessage("§6Multiplayer Setup Status:");
        sender.sendMessage("§7- Race Lobby Spawn: " + (course.getMpraceLobbySpawn() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Create Race Button: " + (course.getMpcreateRaceButton() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Start Race Button: " + (course.getMpstartRaceButton() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Join Race Button: " + (course.getMpjoinRaceButton() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Cancel Race Button: " + (course.getMpcancelRaceButton() != null ? "§aSET" : "§7OPTIONAL"));
        sender.sendMessage("§7- Return Button: " + (course.getMpreturnButton() != null ? "§aSET" : "§cNOT SET"));
        
        // Show boat spawns status
        int boatSpawns = course.getMpboatSpawns() != null ? course.getMpboatSpawns().size() : 0;
        sender.sendMessage("§7- Boat Spawns: §e" + boatSpawns + "/10 " + (boatSpawns >= 2 ? "§a✓" : "§c(Need at least 2)"));
        
        // Show race lines status (shared with singleplayer)
        sender.sendMessage("§7- Start Line Point 1: " + (course.getSpstart1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Start Line Point 2: " + (course.getSpstart2() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 1: " + (course.getSpfinish1() != null ? "§aSET" : "§cNOT SET"));
        sender.sendMessage("§7- Finish Line Point 2: " + (course.getSpfinish2() != null ? "§aSET" : "§cNOT SET"));
        
        // Show completion status
        int setCount = 0;
        int totalRequired = 8; // 6 required components + 2 race lines
        if (course.getMpraceLobbySpawn() != null) setCount++;
        if (course.getMpcreateRaceButton() != null) setCount++;
        if (course.getMpstartRaceButton() != null) setCount++;
        if (course.getMpjoinRaceButton() != null) setCount++;
        if (course.getMpreturnButton() != null) setCount++;
        if (boatSpawns >= 2) setCount++; // Count as 1 if at least 2 boat spawns
        if (course.getSpstart1() != null && course.getSpstart2() != null) setCount++; // Both start points
        if (course.getSpfinish1() != null && course.getSpfinish2() != null) setCount++; // Both finish points
        
        sender.sendMessage("§6Completion: §e" + setCount + "/" + totalRequired + " " + 
                          (setCount >= totalRequired ? "§a✅ READY FOR RACING" : "§c❌ INCOMPLETE"));
        
        return true;
    }
    
    /**
     * Handle multiplayer stats command
     */
    private boolean handleMultiplayerStats(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace multiplayer stats <course>");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null) {
            sender.sendMessage("§cCourse '" + courseName + "' not found!");
            return true;
        }
        
        if (course.getType() != com.bocrace.model.CourseType.MULTIPLAYER) {
            sender.sendMessage("§cCourse '" + courseName + "' is not a multiplayer course!");
            return true;
        }
        
        // Get top 10 times for this course
        java.util.List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes(courseName, 10);
        
        sender.sendMessage("§6§l🏆 MULTIPLAYER LEADERBOARD - " + courseName.toUpperCase());
        sender.sendMessage("§8" + "=".repeat(50));
        
        if (topTimes.isEmpty()) {
            sender.sendMessage("§7No races completed on this course yet!");
        } else {
            for (int i = 0; i < topTimes.size(); i++) {
                RaceRecord record = topTimes.get(i);
                String medal = i == 0 ? "§6🥇" : i == 1 ? "§7🥈" : i == 2 ? "§c🥉" : "§e" + (i + 1) + ".";
                
                sender.sendMessage(medal + " §f" + record.getPlayer() + " §7- §a" + 
                                 String.format("%.2fs", record.getTime()) + " §8(" + 
                                 record.getFormattedDate() + ")");
            }
        }
        
        sender.sendMessage("§8" + "=".repeat(50));
        // Get total races for this course by counting all records
        java.util.List<RaceRecord> allCourseRecords = plugin.getRecordManager().getTopTimes(courseName, Integer.MAX_VALUE);
        sender.sendMessage("§7Total races: §e" + allCourseRecords.size());
        
        return true;
    }
    
    /**
     * Handle multiplayer recent command
     */
    private boolean handleMultiplayerRecent(CommandSender sender, String[] args) {
        String targetPlayer;
        
        if (args.length >= 3) {
            // Admin checking another player: /bocrace multiplayer recent <player>
            if (!sender.hasPermission("bocrace.admin")) {
                sender.sendMessage("§cYou don't have permission to view other players' recent races!");
                return true;
            }
            targetPlayer = args[2];
        } else {
            // Player checking their own: /bocrace multiplayer recent
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("§cConsole must specify a player: /bocrace multiplayer recent <player>");
                return true;
            }
            targetPlayer = sender.getName();
        }
        
        // Get recent multiplayer races for the player
        java.util.List<RaceRecord> recentRaces = plugin.getRecordManager().getPlayerRecent(targetPlayer, 10);
        
        // Filter for multiplayer races only
        java.util.List<RaceRecord> mpRaces = recentRaces.stream()
            .filter(record -> record.getType() == com.bocrace.model.CourseType.MULTIPLAYER)
            .collect(java.util.stream.Collectors.toList());
        
        sender.sendMessage("§6§l🏁 RECENT MULTIPLAYER RACES - " + targetPlayer.toUpperCase());
        sender.sendMessage("§8" + "=".repeat(50));
        
        if (mpRaces.isEmpty()) {
            sender.sendMessage("§7No multiplayer races found for " + targetPlayer + "!");
        } else {
            for (RaceRecord record : mpRaces) {
                sender.sendMessage("§e" + record.getCourse() + " §7- §a" + 
                                 String.format("%.2fs", record.getTime()) + " §8(" + 
                                 record.getFormattedDate() + ")");
            }
        }
        
        sender.sendMessage("§8" + "=".repeat(50));
        
        // Show player stats
        int totalMpRaces = plugin.getRecordManager().getPlayerRacesByType(targetPlayer, com.bocrace.model.CourseType.MULTIPLAYER);
        sender.sendMessage("§7Total multiplayer races: §e" + totalMpRaces);
        
        return true;
    }
    
    /**
     * Format timestamp for display
     */
    private String formatTimestamp(long timestamp) {
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), 
            java.time.ZoneId.systemDefault()
        );
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }
}
