package com.bocrace.command;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
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
            sender.sendMessage("§cUsage: /bocrace singleplayer <create|setup|delete|list|tp|info|reload|stats|recent>");
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
                sender.sendMessage("§eSingleplayer tp not implemented yet.");
                return true;
            case "info":
                return handleSingleplayerInfo(sender, args);
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
                List<String> spCommands = Arrays.asList("create", "setup", "delete", "list", "tp", "info", "reload", "stats", "recent");
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
                if (secondArg.equals("delete") || secondArg.equals("info") || secondArg.equals("tp") || 
                    secondArg.equals("stats") || secondArg.equals("recent") || secondArg.equals("setup")) {
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
                List<String> setupActions = Arrays.asList("setstartbutton", "setboatspawn");
                for (String action : setupActions) {
                    if (action.toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(action);
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
        plugin.getLogger().info("[DEBUG] Singleplayer create command - Player: " + sender.getName() + ", Course: " + courseName);
        
        // Check if course already exists
        if (plugin.getStorageManager().courseExists(courseName)) {
            sender.sendMessage("§cCourse '" + courseName + "' already exists!");
            plugin.getLogger().info("[DEBUG] Course creation failed - course already exists: " + courseName);
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
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer delete <name>");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
            return true;
        }
        
        plugin.getStorageManager().removeCourse(courseName);
        sender.sendMessage("§aSingleplayer course '" + courseName + "' deleted successfully!");
        return true;
    }
    
    private boolean handleSingleplayerList(CommandSender sender) {
        var courses = plugin.getStorageManager().getCoursesByType(CourseType.SINGLEPLAYER);
        
        sender.sendMessage("§6=== Singleplayer Courses ===");
        if (courses.isEmpty()) {
            sender.sendMessage("§7No singleplayer courses found.");
        } else {
            for (Course course : courses) {
                sender.sendMessage("§7- " + course.getName() + " [Prefix: " + course.getPrefixDisplay() + "]");
            }
        }
        return true;
    }
    
    private boolean handleSingleplayerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer info <name>");
            return true;
        }
        
        String courseName = args[2];
        Course course = plugin.getStorageManager().getCourse(courseName);
        
        if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
            sender.sendMessage("§cSingleplayer course '" + courseName + "' not found!");
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
    
    private boolean handleSingleplayerSetup(CommandSender sender, String[] args) {
        plugin.getLogger().info("[DEBUG] Setup command called - Player: " + sender.getName() + ", Args: " + java.util.Arrays.toString(args));
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /bocrace singleplayer setup <coursename>");
            plugin.getLogger().info("[DEBUG] Setup command failed - insufficient arguments");
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
            default:
                sender.sendMessage("§cUnknown setup action. Available: setstartbutton, setboatspawn");
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
        
        // TODO: Add right-click capture system
        player.sendMessage("§eRight-click the start button for course '" + course.getName() + "'");
        player.sendMessage("§7(This will be implemented with right-click capture)");
        plugin.getLogger().info("[DEBUG] SetStartButton instruction sent to player");
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
        
        // TODO: Add right-click capture system
        player.sendMessage("§eRight-click where boats should spawn for course '" + course.getName() + "'");
        player.sendMessage("§7(This will be implemented with right-click capture)");
        plugin.getLogger().info("[DEBUG] SetBoatSpawn instruction sent to player");
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
