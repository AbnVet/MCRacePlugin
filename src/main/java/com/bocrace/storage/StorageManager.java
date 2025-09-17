package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    
    private final BOCRacePlugin plugin;
    private final Map<String, Course> courses;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public StorageManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.courses = new ConcurrentHashMap<>();
    }
    
    public void loadCourses() {
        plugin.debugLog("Starting course reload - clearing " + courses.size() + " courses from memory");
        courses.clear();
        
        // Create courses directory structure
        createCoursesDirectoryStructure();
        
        // Load singleplayer courses
        loadCoursesFromFolder("courses/singleplayer", CourseType.SINGLEPLAYER);
        
        // Load multiplayer courses
        loadCoursesFromFolder("courses/multiplayer", CourseType.MULTIPLAYER);
        
        if (courses.isEmpty()) {
            plugin.getLogger().info("No courses found in courses/singleplayer/ or courses/multiplayer/");
            plugin.debugLog("Course reload complete - 0 courses loaded");
        } else {
            plugin.getLogger().info("Loaded " + courses.size() + " courses");
            plugin.debugLog("Course reload complete - " + courses.size() + " courses loaded: " + String.join(", ", courses.keySet()));
        }
    }
    
    private void createCoursesDirectoryStructure() {
        plugin.debugLog("Creating courses directory structure...");
        
        // Create main courses directory
        File coursesDir = new File(plugin.getDataFolder(), "courses");
        if (!coursesDir.exists()) {
            coursesDir.mkdirs();
            plugin.debugLog("Created courses directory: " + coursesDir.getAbsolutePath());
        }
        
        // Create singleplayer courses directory
        File singleplayerCoursesDir = new File(coursesDir, "singleplayer");
        if (!singleplayerCoursesDir.exists()) {
            singleplayerCoursesDir.mkdirs();
            plugin.debugLog("Created singleplayer courses directory: " + singleplayerCoursesDir.getAbsolutePath());
        }
        
        // Create multiplayer courses directory
        File multiplayerCoursesDir = new File(coursesDir, "multiplayer");
        if (!multiplayerCoursesDir.exists()) {
            multiplayerCoursesDir.mkdirs();
            plugin.debugLog("Created multiplayer courses directory: " + multiplayerCoursesDir.getAbsolutePath());
        }
        
        plugin.debugLog("Courses directory structure created successfully");
    }
    
    private void loadCoursesFromFolder(String folderName, CourseType type) {
        File folder = new File(plugin.getDataFolder(), folderName);
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }
        
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                Course course = loadCourseFromFile(file, type);
                if (course != null) {
                    courses.put(course.getName(), course);
                    plugin.getLogger().info("Loaded course: " + course.getDisplayName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load course from " + file.getName() + ": " + e.getMessage());
            }
        }
    }
    
    private Course loadCourseFromFile(File file, CourseType type) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        String name = config.getString("name");
        if (name == null || name.trim().isEmpty()) {
            plugin.getLogger().warning("Course file " + file.getName() + " has no name, skipping");
            return null;
        }
        
        Course course = new Course();
        course.setName(name);
        course.setType(type);
        
        // Load prefix with fallback
        String prefix = config.getString("prefix");
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "[BOCRace]";
            plugin.getLogger().warning("No prefix set for " + name + ", using [BOCRace]");
        }
        course.setPrefix(prefix);
        
        // Load createdBy with fallback
        String createdBy = config.getString("createdBy");
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "Unknown";
            plugin.getLogger().warning("No creator set for " + name + ", using Unknown");
        }
        course.setCreatedBy(createdBy);
        
        // Load timestamps with fallbacks
        String createdOnStr = config.getString("createdOn");
        if (createdOnStr != null && !createdOnStr.trim().isEmpty()) {
            try {
                course.setCreatedOn(LocalDateTime.parse(createdOnStr, dateTimeFormatter));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid createdOn date for " + name + ", using current time");
                course.setCreatedOn(LocalDateTime.now());
            }
        } else {
            course.setCreatedOn(LocalDateTime.now());
            plugin.getLogger().warning("No createdOn date for " + name + ", using current time");
        }
        
        String lastEditedStr = config.getString("lastEdited");
        if (lastEditedStr != null && !lastEditedStr.trim().isEmpty()) {
            try {
                course.setLastEdited(LocalDateTime.parse(lastEditedStr, dateTimeFormatter));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid lastEdited date for " + name + ", using createdOn");
                course.setLastEdited(course.getCreatedOn());
            }
        } else {
            course.setLastEdited(course.getCreatedOn());
            plugin.getLogger().warning("No lastEdited date for " + name + ", using createdOn");
        }
        
        // Load course-specific Location fields
        World world = Bukkit.getWorld(config.getString("world", "world"));
        if (world == null) {
            plugin.getLogger().warning("Course '" + name + "' references unknown world, using default world");
            world = Bukkit.getWorlds().get(0);
        }
        
        if (type == CourseType.SINGLEPLAYER) {
            course.setSpstartbutton(readLocation(config.getConfigurationSection("spstartbutton"), world)); // Legacy
            course.setSpmainlobbybutton(readLocation(config.getConfigurationSection("spmainlobbybutton"), world));
            course.setSpcourselobbybutton(readLocation(config.getConfigurationSection("spcourselobbybutton"), world));
            course.setSpboatspawn(readLocation(config.getConfigurationSection("spboatspawn"), world));
            
            // Load race line locations
            course.setSpstart1(readLocation(config.getConfigurationSection("spstart1"), world));
            course.setSpstart2(readLocation(config.getConfigurationSection("spstart2"), world));
            course.setSpfinish1(readLocation(config.getConfigurationSection("spfinish1"), world));
            course.setSpfinish2(readLocation(config.getConfigurationSection("spfinish2"), world));
            course.setSpreturn(readLocation(config.getConfigurationSection("spreturn"), world));
            
            // Load lobby locations
            course.setSpcourselobby(readLocation(config.getConfigurationSection("spcourselobby"), world));
            course.setSpmainlobby(readLocation(config.getConfigurationSection("spmainlobby"), world));
            
        } else if (type == CourseType.MULTIPLAYER) {
            // Load multiplayer button locations
            course.setMpraceLobbySpawn(readLocation(config.getConfigurationSection("mpraceLobbySpawn"), world));
            course.setMpcreateRaceButton(readLocation(config.getConfigurationSection("mpcreateRaceButton"), world));
            course.setMpstartRaceButton(readLocation(config.getConfigurationSection("mpstartRaceButton"), world));
            course.setMpjoinRaceButton(readLocation(config.getConfigurationSection("mpjoinRaceButton"), world));
            course.setMpcancelRaceButton(readLocation(config.getConfigurationSection("mpcancelRaceButton"), world));
            course.setMpreturnButton(readLocation(config.getConfigurationSection("mpreturnButton"), world));
            
            // Load multiplayer boat spawns (up to 10)
            List<Location> boatSpawns = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Location spawn = readLocation(config.getConfigurationSection("mpboatSpawn" + i), world);
                if (spawn != null) {
                    boatSpawns.add(spawn);
                }
            }
            course.setMpboatSpawns(boatSpawns);
            
            // Load race line locations (shared with singleplayer)
            course.setSpstart1(readLocation(config.getConfigurationSection("spstart1"), world));
            course.setSpstart2(readLocation(config.getConfigurationSection("spstart2"), world));
            course.setSpfinish1(readLocation(config.getConfigurationSection("spfinish1"), world));
            course.setSpfinish2(readLocation(config.getConfigurationSection("spfinish2"), world));
        }
        
        // Load usage tracking
        course.setUsageCount(config.getInt("usageCount", 0));
        if (config.contains("lastUsed")) {
            course.setLastUsed(LocalDateTime.parse(config.getString("lastUsed")));
        }
        course.setLastUsedBy(config.getString("lastUsedBy"));
        
        // Load per-course settings (optional)
        if (config.contains("sounds-enabled")) {
            course.setSoundsEnabled(config.getBoolean("sounds-enabled"));
        }
        if (config.contains("particles-enabled")) {
            course.setParticlesEnabled(config.getBoolean("particles-enabled"));
        }
        if (config.contains("custom-messages") && config.isConfigurationSection("custom-messages")) {
            Map<String, String> customMessages = new HashMap<>();
            ConfigurationSection messagesSection = config.getConfigurationSection("custom-messages");
            for (String key : messagesSection.getKeys(false)) {
                customMessages.put(key, messagesSection.getString(key));
            }
            course.setCustomMessages(customMessages);
        }
        
        // Load data map
        Map<String, Object> data = new HashMap<>();
        if (config.contains("data") && config.isConfigurationSection("data")) {
            for (String key : config.getConfigurationSection("data").getKeys(false)) {
                data.put(key, config.get("data." + key));
            }
        }
        course.setData(data);
        
        return course;
    }
    
    public void saveCourse(Course course) {
        try {
            String folderName = course.getType() == CourseType.SINGLEPLAYER ? "courses/singleplayer" : "courses/multiplayer";
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            
            File file = new File(folder, course.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            // DEBUG: Log course saving
            plugin.getLogger().info("[DEBUG] Saving course: " + course.getName() + " (Type: " + course.getType() + ")");
            
            config.set("name", course.getName());
            config.set("type", course.getType().toString());
            config.set("prefix", course.getPrefix());
            config.set("createdBy", course.getCreatedBy());
            config.set("createdOn", course.getCreatedOn().format(dateTimeFormatter));
            config.set("lastEdited", course.getLastEdited().format(dateTimeFormatter));
            
            // Save course-specific Location fields
            if (course.getType() == CourseType.SINGLEPLAYER) {
                plugin.getLogger().info("[DEBUG] Saving singleplayer course - spstartbutton: " + 
                    (course.getSpstartbutton() != null ? "SET" : "NULL") + 
                    ", spboatspawn: " + (course.getSpboatspawn() != null ? "SET" : "NULL"));
                
                // Save button locations
                writeLocation(config, "spstartbutton", course.getSpstartbutton()); // Legacy
                writeLocation(config, "spmainlobbybutton", course.getSpmainlobbybutton());
                writeLocation(config, "spcourselobbybutton", course.getSpcourselobbybutton());
                writeLocation(config, "spboatspawn", course.getSpboatspawn());
                
                // Save race line locations
                writeLocation(config, "spstart1", course.getSpstart1());
                writeLocation(config, "spstart2", course.getSpstart2());
                writeLocation(config, "spfinish1", course.getSpfinish1());
                writeLocation(config, "spfinish2", course.getSpfinish2());
                writeLocation(config, "spreturn", course.getSpreturn());
                
                // Save lobby locations
                writeLocation(config, "spcourselobby", course.getSpcourselobby());
                writeLocation(config, "spmainlobby", course.getSpmainlobby());
                
            } else if (course.getType() == CourseType.MULTIPLAYER) {
                plugin.getLogger().info("[DEBUG] Saving multiplayer course - buttons: " + 
                    (course.getMpcreateRaceButton() != null ? "CREATE" : "") + 
                    (course.getMpstartRaceButton() != null ? " START" : "") + 
                    (course.getMpjoinRaceButton() != null ? " JOIN" : "") + 
                    ", boat spawns: " + (course.getMpboatSpawns() != null ? course.getMpboatSpawns().size() : 0));
                
                // Save multiplayer button locations
                writeLocation(config, "mpraceLobbySpawn", course.getMpraceLobbySpawn());
                writeLocation(config, "mpcreateRaceButton", course.getMpcreateRaceButton());
                writeLocation(config, "mpstartRaceButton", course.getMpstartRaceButton());
                writeLocation(config, "mpjoinRaceButton", course.getMpjoinRaceButton());
                writeLocation(config, "mpcancelRaceButton", course.getMpcancelRaceButton());
                writeLocation(config, "mpreturnButton", course.getMpreturnButton());
                
                // Save multiplayer boat spawns (up to 10)
                if (course.getMpboatSpawns() != null) {
                    for (int i = 0; i < course.getMpboatSpawns().size() && i < 10; i++) {
                        writeLocation(config, "mpboatSpawn" + (i + 1), course.getMpboatSpawns().get(i));
                    }
                }
                
                // Save race line locations (shared with singleplayer)
                writeLocation(config, "spstart1", course.getSpstart1());
                writeLocation(config, "spstart2", course.getSpstart2());
                writeLocation(config, "spfinish1", course.getSpfinish1());
                writeLocation(config, "spfinish2", course.getSpfinish2());
            }
            
            // Save usage tracking
            config.set("usageCount", course.getUsageCount());
            if (course.getLastUsed() != null) {
                config.set("lastUsed", course.getLastUsed().toString());
            }
            config.set("lastUsedBy", course.getLastUsedBy());
            
            // Save per-course settings (optional)
            if (course.getSoundsEnabled() != null) {
                config.set("sounds-enabled", course.getSoundsEnabled());
            }
            if (course.getParticlesEnabled() != null) {
                config.set("particles-enabled", course.getParticlesEnabled());
            }
            if (course.getCustomMessages() != null && !course.getCustomMessages().isEmpty()) {
                for (Map.Entry<String, String> entry : course.getCustomMessages().entrySet()) {
                    config.set("custom-messages." + entry.getKey(), entry.getValue());
                }
            }
            
            // Save data map
            for (Map.Entry<String, Object> entry : course.getData().entrySet()) {
                config.set("data." + entry.getKey(), entry.getValue());
            }
            
            config.save(file);
            plugin.getLogger().info("Saved course: " + course.getDisplayName());
            plugin.getLogger().info("[DEBUG] Course saved to: " + file.getAbsolutePath());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save course " + course.getName() + ": " + e.getMessage());
        }
    }
    
    public List<Course> getAllCourses() {
        return new ArrayList<>(courses.values());
    }
    
    public List<Course> getCoursesByType(CourseType type) {
        List<Course> result = new ArrayList<>();
        for (Course course : courses.values()) {
            if (course.getType() == type) {
                result.add(course);
            }
        }
        return result;
    }
    
    public Course getCourse(String name) {
        return courses.get(name);
    }
    
    public boolean courseExists(String name) {
        return courses.containsKey(name);
    }
    
    public List<String> getCourseNames() {
        return new ArrayList<>(courses.keySet());
    }
    
    public void addCourse(Course course) {
        courses.put(course.getName(), course);
    }
    
    public void removeCourse(String name) {
        Course course = courses.get(name);
        if (course != null) {
            // Delete the YAML file from disk
            String folderName = course.getType() == CourseType.SINGLEPLAYER ? "courses/singleplayer" : "courses/multiplayer";
            File folder = new File(plugin.getDataFolder(), folderName);
            File file = new File(folder, course.getName() + ".yml");
            
            if (file.exists()) {
                boolean deleted = file.delete();
                plugin.getLogger().info("[DEBUG] Course file deletion: " + file.getName() + " - " + (deleted ? "SUCCESS" : "FAILED"));
            } else {
                plugin.getLogger().warning("[DEBUG] Course file not found for deletion: " + file.getAbsolutePath());
            }
        }
        
        // Remove from memory
        courses.remove(name);
        plugin.getLogger().info("[DEBUG] Course removed from memory: " + name);
    }
    
    // Helper methods for Location serialization
    private Location readLocation(ConfigurationSection section, World defaultWorld) {
        if (section == null) return null;
        
        World world = defaultWorld;
        if (section.isString("world")) {
            World maybe = Bukkit.getWorld(section.getString("world"));
            if (maybe != null) world = maybe;
        }
        
        int x = section.getInt("x", 0);
        int y = section.getInt("y", 0);
        int z = section.getInt("z", 0);
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        
        return new Location(world, x + 0.5, y, z + 0.5, yaw, pitch);
    }
    
    private void writeLocation(FileConfiguration config, String path, Location location) {
        if (location == null) {
            plugin.getLogger().info("[DEBUG] Writing NULL location for: " + path + " - field will not appear in YAML");
            config.set(path, null);
            return;
        }
        
        plugin.getLogger().info("[DEBUG] Writing location for: " + path + " - World: " + 
            (location.getWorld() != null ? location.getWorld().getName() : "NULL") + 
            ", X: " + location.getBlockX() + ", Y: " + location.getBlockY() + 
            ", Z: " + location.getBlockZ() + ", Yaw: " + location.getYaw());
        
        config.set(path + ".world", location.getWorld() != null ? location.getWorld().getName() : null);
        config.set(path + ".x", location.getBlockX());
        config.set(path + ".y", location.getBlockY());
        config.set(path + ".z", location.getBlockZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }
}
