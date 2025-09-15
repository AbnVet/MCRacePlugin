package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
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
        courses.clear();
        
        // Load singleplayer courses
        loadCoursesFromFolder("singleplayer", CourseType.SINGLEPLAYER);
        
        // Load multiplayer courses
        loadCoursesFromFolder("multiplayer", CourseType.MULTIPLAYER);
        
        if (courses.isEmpty()) {
            plugin.getLogger().info("No courses found in singleplayer/ or multiplayer/");
        } else {
            plugin.getLogger().info("Loaded " + courses.size() + " courses");
        }
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
            String folderName = course.getType() == CourseType.SINGLEPLAYER ? "singleplayer" : "multiplayer";
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            
            File file = new File(folder, course.getName() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            
            config.set("name", course.getName());
            config.set("type", course.getType().toString());
            config.set("prefix", course.getPrefix());
            config.set("createdBy", course.getCreatedBy());
            config.set("createdOn", course.getCreatedOn().format(dateTimeFormatter));
            config.set("lastEdited", course.getLastEdited().format(dateTimeFormatter));
            
            // Save data map
            for (Map.Entry<String, Object> entry : course.getData().entrySet()) {
                config.set("data." + entry.getKey(), entry.getValue());
            }
            
            config.save(file);
            plugin.getLogger().info("Saved course: " + course.getDisplayName());
            
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
    
    public void addCourse(Course course) {
        courses.put(course.getName(), course);
    }
    
    public void removeCourse(String name) {
        courses.remove(name);
    }
}
