package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages data storage for courses and player statistics
 */
public class StorageManager {
    private final BOCRacePlugin plugin;
    private final File coursesFile;
    private final File statsFile;
    private final Map<String, Course> courses;
    private final Map<UUID, PlayerStats> playerStats;
    
    public StorageManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.coursesFile = new File(plugin.getDataFolder(), "courses.yml");
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.courses = new ConcurrentHashMap<>();
        this.playerStats = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the storage system
     */
    public void initialize() {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load courses
        loadCourses();
        
        // Load player stats
        loadPlayerStats();
        
        plugin.getLogger().info("Storage system initialized successfully");
    }
    
    /**
     * Load all courses from YAML file
     */
    public void loadCourses() {
        if (!coursesFile.exists()) {
            plugin.getLogger().info("No courses file found, creating new one");
            saveCourses();
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(coursesFile);
        courses.clear();
        
        ConfigurationSection coursesSection = config.getConfigurationSection("courses");
        if (coursesSection == null) {
            plugin.getLogger().warning("No courses section found in courses.yml");
            return;
        }
        
        for (String courseName : coursesSection.getKeys(false)) {
            try {
                Course course = loadCourseFromConfig(coursesSection.getConfigurationSection(courseName), courseName);
                if (course != null) {
                    courses.put(courseName, course);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load course " + courseName + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + courses.size() + " courses");
    }
    
    /**
     * Load a single course from configuration
     */
    private Course loadCourseFromConfig(ConfigurationSection section, String courseName) {
        if (section == null) return null;
        
        String typeStr = section.getString("type");
        String worldName = section.getString("world");
        String createdByStr = section.getString("createdBy");
        
        if (typeStr == null || worldName == null || createdByStr == null) {
            plugin.getLogger().warning("Invalid course data for " + courseName);
            return null;
        }
        
        Course.CourseType type = Course.CourseType.valueOf(typeStr.toUpperCase());
        World world = Bukkit.getWorld(worldName);
        UUID createdBy = UUID.fromString(createdByStr);
        
        if (world == null) {
            plugin.getLogger().warning("World " + worldName + " not found for course " + courseName);
            return null;
        }
        
        Course course = new Course(courseName, type, world, createdBy);
        
        // Load locations
        if (section.contains("mainLobby")) {
            course.setMainLobby(loadLocation(section.getConfigurationSection("mainLobby")));
        }
        if (section.contains("courseLobby")) {
            course.setCourseLobby(loadLocation(section.getConfigurationSection("courseLobby")));
        }
        
        // Load boat spawns
        List<Location> boatSpawns = new ArrayList<>();
        ConfigurationSection boatSpawnsSection = section.getConfigurationSection("boatSpawns");
        if (boatSpawnsSection != null) {
            for (String key : boatSpawnsSection.getKeys(false)) {
                Location spawn = loadLocation(boatSpawnsSection.getConfigurationSection(key));
                if (spawn != null) {
                    boatSpawns.add(spawn);
                }
            }
        }
        course.setBoatSpawns(boatSpawns);
        
        // Load bounding boxes
        if (section.contains("startLine")) {
            course.setStartLine(loadBoundingBox(section.getConfigurationSection("startLine")));
        }
        if (section.contains("finishLine")) {
            course.setFinishLine(loadBoundingBox(section.getConfigurationSection("finishLine")));
        }
        
        // Load buttons
        if (section.contains("createRaceButton")) {
            course.setCreateRaceButton(loadLocation(section.getConfigurationSection("createRaceButton")));
        }
        if (section.contains("joinRaceButton")) {
            course.setJoinRaceButton(loadLocation(section.getConfigurationSection("joinRaceButton")));
        }
        if (section.contains("startRaceButton")) {
            course.setStartRaceButton(loadLocation(section.getConfigurationSection("startRaceButton")));
        }
        if (section.contains("returnToLobbyButton")) {
            course.setReturnToLobbyButton(loadLocation(section.getConfigurationSection("returnToLobbyButton")));
        }
        if (section.contains("returnToMainButton")) {
            course.setReturnToMainButton(loadLocation(section.getConfigurationSection("returnToMainButton")));
        }
        
        // Load boat spawn buttons
        List<Location> boatSpawnButtons = new ArrayList<>();
        ConfigurationSection boatSpawnButtonsSection = section.getConfigurationSection("boatSpawnButtons");
        if (boatSpawnButtonsSection != null) {
            for (String key : boatSpawnButtonsSection.getKeys(false)) {
                Location button = loadLocation(boatSpawnButtonsSection.getConfigurationSection(key));
                if (button != null) {
                    boatSpawnButtons.add(button);
                }
            }
        }
        course.setBoatSpawnButtons(boatSpawnButtons);
        
        return course;
    }
    
    /**
     * Load a location from configuration
     */
    private Location loadLocation(ConfigurationSection section) {
        if (section == null) return null;
        
        String worldName = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    /**
     * Load a bounding box from configuration
     */
    private BoundingBox loadBoundingBox(ConfigurationSection section) {
        if (section == null) return null;
        
        double minX = section.getDouble("minX");
        double minY = section.getDouble("minY");
        double minZ = section.getDouble("minZ");
        double maxX = section.getDouble("maxX");
        double maxY = section.getDouble("maxY");
        double maxZ = section.getDouble("maxZ");
        
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Save all courses to YAML file
     */
    public CompletableFuture<Void> saveCourses() {
        return CompletableFuture.runAsync(() -> {
            FileConfiguration config = new YamlConfiguration();
            
            ConfigurationSection coursesSection = config.createSection("courses");
            
            for (Course course : courses.values()) {
                saveCourseToConfig(coursesSection, course);
            }
            
            try {
                config.save(coursesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save courses: " + e.getMessage());
            }
        });
    }
    
    /**
     * Save a single course to configuration
     */
    private void saveCourseToConfig(ConfigurationSection coursesSection, Course course) {
        ConfigurationSection courseSection = coursesSection.createSection(course.getName());
        
        courseSection.set("type", course.getType().name());
        courseSection.set("world", course.getWorld().getName());
        courseSection.set("createdBy", course.getCreatedBy().toString());
        courseSection.set("createdAt", course.getCreatedAt());
        courseSection.set("lastModified", course.getLastModified());
        
        // Save locations
        if (course.getMainLobby() != null) {
            saveLocation(courseSection.createSection("mainLobby"), course.getMainLobby());
        }
        if (course.getCourseLobby() != null) {
            saveLocation(courseSection.createSection("courseLobby"), course.getCourseLobby());
        }
        
        // Save boat spawns
        ConfigurationSection boatSpawnsSection = courseSection.createSection("boatSpawns");
        for (int i = 0; i < course.getBoatSpawns().size(); i++) {
            saveLocation(boatSpawnsSection.createSection(String.valueOf(i)), course.getBoatSpawns().get(i));
        }
        
        // Save bounding boxes
        if (course.getStartLine() != null) {
            saveBoundingBox(courseSection.createSection("startLine"), course.getStartLine());
        }
        if (course.getFinishLine() != null) {
            saveBoundingBox(courseSection.createSection("finishLine"), course.getFinishLine());
        }
        
        // Save buttons
        if (course.getCreateRaceButton() != null) {
            saveLocation(courseSection.createSection("createRaceButton"), course.getCreateRaceButton());
        }
        if (course.getJoinRaceButton() != null) {
            saveLocation(courseSection.createSection("joinRaceButton"), course.getJoinRaceButton());
        }
        if (course.getStartRaceButton() != null) {
            saveLocation(courseSection.createSection("startRaceButton"), course.getStartRaceButton());
        }
        if (course.getReturnToLobbyButton() != null) {
            saveLocation(courseSection.createSection("returnToLobbyButton"), course.getReturnToLobbyButton());
        }
        if (course.getReturnToMainButton() != null) {
            saveLocation(courseSection.createSection("returnToMainButton"), course.getReturnToMainButton());
        }
        
        // Save boat spawn buttons
        ConfigurationSection boatSpawnButtonsSection = courseSection.createSection("boatSpawnButtons");
        for (int i = 0; i < course.getBoatSpawnButtons().size(); i++) {
            saveLocation(boatSpawnButtonsSection.createSection(String.valueOf(i)), course.getBoatSpawnButtons().get(i));
        }
    }
    
    /**
     * Save a location to configuration
     */
    private void saveLocation(ConfigurationSection section, Location location) {
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
    
    /**
     * Save a bounding box to configuration
     */
    private void saveBoundingBox(ConfigurationSection section, BoundingBox box) {
        section.set("minX", box.getMinX());
        section.set("minY", box.getMinY());
        section.set("minZ", box.getMinZ());
        section.set("maxX", box.getMaxX());
        section.set("maxY", box.getMaxY());
        section.set("maxZ", box.getMaxZ());
    }
    
    /**
     * Load player statistics from YAML file
     */
    public void loadPlayerStats() {
        if (!statsFile.exists()) {
            plugin.getLogger().info("No stats file found, creating new one");
            savePlayerStats();
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        playerStats.clear();
        
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            plugin.getLogger().warning("No players section found in stats.yml");
            return;
        }
        
        for (String playerIdStr : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                PlayerStats stats = loadPlayerStatsFromConfig(playersSection.getConfigurationSection(playerIdStr), playerId);
                if (stats != null) {
                    playerStats.put(playerId, stats);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load stats for player " + playerIdStr + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players");
    }
    
    /**
     * Load player stats from configuration
     */
    private PlayerStats loadPlayerStatsFromConfig(ConfigurationSection section, UUID playerId) {
        if (section == null) return null;
        
        PlayerStats stats = new PlayerStats(playerId);
        stats.setLastPlayed(section.getLong("lastPlayed", System.currentTimeMillis()));
        
        // Load course stats
        ConfigurationSection courseStatsSection = section.getConfigurationSection("courseStats");
        if (courseStatsSection != null) {
            for (String courseName : courseStatsSection.getKeys(false)) {
                ConfigurationSection courseSection = courseStatsSection.getConfigurationSection(courseName);
                PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
                
                courseStats.setBestTime(courseSection.getLong("bestTime", 0));
                courseStats.setTotalRaces(courseSection.getInt("totalRaces", 0));
                courseStats.setWins(courseSection.getInt("wins", 0));
                courseStats.setAttempts(courseSection.getInt("attempts", 0));
                courseStats.setTotalTime(courseSection.getLong("totalTime", 0));
                courseStats.setLastRaceTime(courseSection.getLong("lastRaceTime", 0));
            }
        }
        
        // Load race history
        ConfigurationSection raceHistorySection = section.getConfigurationSection("raceHistory");
        if (raceHistorySection != null) {
            for (String raceId : raceHistorySection.getKeys(false)) {
                ConfigurationSection raceSection = raceHistorySection.getConfigurationSection(raceId);
                PlayerStats.RaceRecord record = new PlayerStats.RaceRecord(
                    raceId,
                    raceSection.getString("courseName"),
                    PlayerStats.RaceRecord.RaceType.valueOf(raceSection.getString("raceType")),
                    raceSection.getLong("raceTime"),
                    raceSection.getInt("position"),
                    raceSection.getBoolean("finished"),
                    raceSection.getString("sessionId")
                );
                stats.addRaceRecord(record);
            }
        }
        
        return stats;
    }
    
    /**
     * Save player statistics to YAML file
     */
    public CompletableFuture<Void> savePlayerStats() {
        return CompletableFuture.runAsync(() -> {
            FileConfiguration config = new YamlConfiguration();
            
            ConfigurationSection playersSection = config.createSection("players");
            
            for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
                savePlayerStatsToConfig(playersSection, entry.getKey(), entry.getValue());
            }
            
            try {
                config.save(statsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save player stats: " + e.getMessage());
            }
        });
    }
    
    /**
     * Save player stats to configuration
     */
    private void savePlayerStatsToConfig(ConfigurationSection playersSection, UUID playerId, PlayerStats stats) {
        ConfigurationSection playerSection = playersSection.createSection(playerId.toString());
        
        playerSection.set("firstPlayed", stats.getFirstPlayed());
        playerSection.set("lastPlayed", stats.getLastPlayed());
        
        // Save course stats
        ConfigurationSection courseStatsSection = playerSection.createSection("courseStats");
        for (Map.Entry<String, PlayerStats.CourseStats> entry : stats.getCourseStats().entrySet()) {
            ConfigurationSection courseSection = courseStatsSection.createSection(entry.getKey());
            PlayerStats.CourseStats courseStats = entry.getValue();
            
            courseSection.set("bestTime", courseStats.getBestTime());
            courseSection.set("totalRaces", courseStats.getTotalRaces());
            courseSection.set("wins", courseStats.getWins());
            courseSection.set("attempts", courseStats.getAttempts());
            courseSection.set("totalTime", courseStats.getTotalTime());
            courseSection.set("lastRaceTime", courseStats.getLastRaceTime());
        }
        
        // Save race history
        ConfigurationSection raceHistorySection = playerSection.createSection("raceHistory");
        for (PlayerStats.RaceRecord record : stats.getRaceHistory()) {
            ConfigurationSection raceSection = raceHistorySection.createSection(record.getRaceId());
            raceSection.set("courseName", record.getCourseName());
            raceSection.set("raceType", record.getRaceType().name());
            raceSection.set("raceTime", record.getRaceTime());
            raceSection.set("position", record.getPosition());
            raceSection.set("finished", record.isFinished());
            raceSection.set("sessionId", record.getSessionId());
        }
    }
    
    // Course management methods
    public Map<String, Course> getCourses() { return courses; }
    public Course getCourse(String name) { return courses.get(name); }
    public void addCourse(Course course) { courses.put(course.getName(), course); }
    public void removeCourse(String name) { courses.remove(name); }
    public boolean hasCourse(String name) { return courses.containsKey(name); }
    
    // Player stats management methods
    public Map<UUID, PlayerStats> getPlayerStats() { return playerStats; }
    public PlayerStats getPlayerStats(UUID playerId) { return playerStats.get(playerId); }
    public PlayerStats getOrCreatePlayerStats(UUID playerId) { 
        return playerStats.computeIfAbsent(playerId, PlayerStats::new); 
    }
    public void removePlayerStats(UUID playerId) { playerStats.remove(playerId); }
    
    /**
     * Shutdown the storage system
     */
    public void shutdown() {
        // Save all data before shutdown
        saveCourses().join();
        savePlayerStats().join();
        plugin.getLogger().info("Storage system shutdown complete");
    }
}
