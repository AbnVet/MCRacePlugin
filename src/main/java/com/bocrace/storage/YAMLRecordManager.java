package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.RaceRecord;
import com.bocrace.model.CourseType;
import com.bocrace.model.Period;
import com.bocrace.model.Course;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CLEAN YAML implementation of RecordManager
 * NEW STRUCTURE: Simple leaderboard files + automatic reset logic
 */
public class YAMLRecordManager implements RecordManager {
    
    private final BOCRacePlugin plugin;
    private final StorageManager storageManager;
    
    // CLEAN DIRECTORY STRUCTURE
    private final File dataDir;
    private final File singleplayerDir;
    private final File multiplayerDir;
    private final File playersDir;
    private final File cacheDir;
    private final File playerStatsFile;
    private final File playerRecentFile;
    
    // In-memory cache for real-time hologram updates
    private final Map<String, List<RaceRecord>> leaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 300000; // 5 minutes
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public YAMLRecordManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.singleplayerDir = new File(dataDir, "singleplayer");
        this.multiplayerDir = new File(dataDir, "multiplayer");
        this.playersDir = new File(dataDir, "players");
        this.cacheDir = new File(dataDir, "cache");
        this.playerStatsFile = new File(playersDir, "stats.yml");
        this.playerRecentFile = new File(playersDir, "recent.yml");
        
        createDirectoryStructure();
    }
    
    private void createDirectoryStructure() {
        plugin.debugLog("Creating CLEAN leaderboard directory structure...");
        
        // Create main directories
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            plugin.debugLog("Created data directory: " + dataDir.getAbsolutePath());
        }
        if (!singleplayerDir.exists()) {
            singleplayerDir.mkdirs();
            plugin.debugLog("Created singleplayer directory: " + singleplayerDir.getAbsolutePath());
        }
        if (!multiplayerDir.exists()) {
            multiplayerDir.mkdirs();
            plugin.debugLog("Created multiplayer directory: " + multiplayerDir.getAbsolutePath());
        }
        if (!playersDir.exists()) {
            playersDir.mkdirs();
            plugin.debugLog("Created players directory: " + playersDir.getAbsolutePath());
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            plugin.debugLog("Created cache directory: " + cacheDir.getAbsolutePath());
        }
        
        plugin.debugLog("CLEAN directory structure created successfully");
    }
    
    /**
     * Get the correct course directory based on course type
     */
    private File getCourseDirectory(String courseName, CourseType type) {
        File typeDir = (type == CourseType.SINGLEPLAYER) ? singleplayerDir : multiplayerDir;
        return new File(typeDir, courseName);
    }
    
    /**
     * Get course configuration
     */
    private Course getCourseConfig(String courseName, CourseType type) {
        Course course = storageManager.getCourse(courseName);
        if (course != null && course.getType() == type) {
            return course;
        }
        return null;
    }
    
    @Override
    public void saveRaceRecord(String player, String course, double time, CourseType type) {
        saveRaceRecord(player, course, time, type, LocalDateTime.now());
    }
    
    @Override
    public void saveRaceRecord(String player, String course, double time, CourseType type, LocalDateTime date) {
        try {
            plugin.debugLog("Saving race record: " + player + " - " + String.format("%.2f", time) + "s on " + course);
            
            // 1. Always save to all_records.yml (permanent backup)
            saveToAllRecords(player, course, time, type, date);
            
            // 2. Get course configuration
            Course courseConfig = getCourseConfig(course, type);
            if (courseConfig == null) {
                plugin.getLogger().warning("Course config not found for " + course + " (" + type + ")");
                return;
            }
            
            // 3. Check if reset is needed for enabled leaderboards
            checkAndResetLeaderboards(course, type, courseConfig);
            
            // 4. Update enabled leaderboards
            if (courseConfig.isDailyLeaderboard()) {
                updateDailyLeaderboard(player, course, time, type, date);
            }
            
            if (courseConfig.isWeeklyLeaderboard()) {
                updateWeeklyLeaderboard(player, course, time, type, date);
            }
            
            if (courseConfig.isMonthlyLeaderboard()) {
                updateMonthlyLeaderboard(player, course, time, type, date);
            }
            
            // 5. Update player stats and recent
            updatePlayerRecent(player, course, time, type, date);
            updatePlayerStats(player, type);
            
            // 6. Clear cache for this course
            String cacheKey = course + "_" + type.name();
            leaderboardCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
            
            plugin.debugLog("Race record saved successfully to clean structure");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save race record: " + e.getMessage());
        }
    }
    
    /**
     * Save to all_records.yml - EVERY race record (no data loss)
     */
    private void saveToAllRecords(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = getCourseDirectory(course, type);
        if (!courseDir.exists()) {
            courseDir.mkdirs();
        }
        
        File allRecordsFile = new File(courseDir, "all_records.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(allRecordsFile);
        
        // Generate unique key
        String key = "record_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        
        // Save record
        config.set("records." + key + ".player", player);
        config.set("records." + key + ".course", course);
        config.set("records." + key + ".time", Math.round(time * 100.0) / 100.0); // Round to 2 decimal places
        config.set("records." + key + ".date", date.format(dateFormatter));
        config.set("records." + key + ".type", type.name());
        
        config.save(allRecordsFile);
        plugin.debugLog("Saved to all_records.yml - Record: " + key);
    }
    
    /**
     * Check if leaderboard resets are needed and perform them
     */
    private void checkAndResetLeaderboards(String course, CourseType type, Course courseConfig) {
        
        // Daily reset check
        if (courseConfig.isDailyLeaderboard() && courseConfig.isResetDaily()) {
            if (shouldResetDaily(course, type)) {
                clearDailyLeaderboard(course, type);
                plugin.debugLog("Reset daily leaderboard for " + course);
            }
        }
        
        // Weekly reset check
        if (courseConfig.isWeeklyLeaderboard() && courseConfig.isResetWeekly()) {
            if (shouldResetWeekly(course, type)) {
                clearWeeklyLeaderboard(course, type);
                plugin.debugLog("Reset weekly leaderboard for " + course);
            }
        }
        
        // Monthly reset check
        if (courseConfig.isMonthlyLeaderboard() && courseConfig.isResetMonthly()) {
            if (shouldResetMonthly(course, type)) {
                clearMonthlyLeaderboard(course, type);
                plugin.debugLog("Reset monthly leaderboard for " + course);
            }
        }
    }
    
    /**
     * Check if daily reset is needed
     */
    private boolean shouldResetDaily(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File dailyFile = new File(courseDir, "daily_leaderboard.yml");
        
        if (!dailyFile.exists()) {
            return false; // No file to reset
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dailyFile);
        String lastResetStr = config.getString("last-reset");
        
        if (lastResetStr == null) {
            return true; // No reset timestamp, assume needs reset
        }
        
        try {
            LocalDateTime lastReset = LocalDateTime.parse(lastResetStr, dateFormatter);
            LocalDateTime now = LocalDateTime.now();
            
            // Reset if more than 24 hours have passed
            return now.isAfter(lastReset.plusHours(24));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid last-reset timestamp for daily leaderboard: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Check if weekly reset is needed
     */
    private boolean shouldResetWeekly(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File weeklyFile = new File(courseDir, "weekly_leaderboard.yml");
        
        if (!weeklyFile.exists()) {
            return false; // No file to reset
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(weeklyFile);
        String lastResetStr = config.getString("last-reset");
        
        if (lastResetStr == null) {
            return true; // No reset timestamp, assume needs reset
        }
        
        try {
            LocalDateTime lastReset = LocalDateTime.parse(lastResetStr, dateFormatter);
            LocalDateTime now = LocalDateTime.now();
            
            // Reset if more than 7 days have passed
            return now.isAfter(lastReset.plusDays(7));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid last-reset timestamp for weekly leaderboard: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Check if monthly reset is needed
     */
    private boolean shouldResetMonthly(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File monthlyFile = new File(courseDir, "monthly_leaderboard.yml");
        
        if (!monthlyFile.exists()) {
            return false; // No file to reset
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(monthlyFile);
        String lastResetStr = config.getString("last-reset");
        
        if (lastResetStr == null) {
            return true; // No reset timestamp, assume needs reset
        }
        
        try {
            LocalDateTime lastReset = LocalDateTime.parse(lastResetStr, dateFormatter);
            LocalDateTime now = LocalDateTime.now();
            
            // Reset if month has changed
            return now.getMonth() != lastReset.getMonth() || now.getYear() != lastReset.getYear();
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid last-reset timestamp for monthly leaderboard: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Clear daily leaderboard
     */
    private void clearDailyLeaderboard(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File dailyFile = new File(courseDir, "daily_leaderboard.yml");
        
        if (dailyFile.exists()) {
            dailyFile.delete();
        }
    }
    
    /**
     * Clear weekly leaderboard
     */
    private void clearWeeklyLeaderboard(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File weeklyFile = new File(courseDir, "weekly_leaderboard.yml");
        
        if (weeklyFile.exists()) {
            weeklyFile.delete();
        }
    }
    
    /**
     * Clear monthly leaderboard
     */
    private void clearMonthlyLeaderboard(String course, CourseType type) {
        File courseDir = getCourseDirectory(course, type);
        File monthlyFile = new File(courseDir, "monthly_leaderboard.yml");
        
        if (monthlyFile.exists()) {
            monthlyFile.delete();
        }
    }
    
    /**
     * Update daily leaderboard with one record per player rule
     */
    private void updateDailyLeaderboard(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = getCourseDirectory(course, type);
        if (!courseDir.exists()) {
            courseDir.mkdirs();
        }
        
        File dailyFile = new File(courseDir, "daily_leaderboard.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(dailyFile);
        
        // Load existing records
        List<RaceRecord> records = new ArrayList<>();
        if (config.contains("leaderboard")) {
            for (String key : config.getConfigurationSection("leaderboard").getKeys(false)) {
                String recordPlayer = config.getString("leaderboard." + key + ".player");
                double recordTime = config.getDouble("leaderboard." + key + ".time");
                String recordDateStr = config.getString("leaderboard." + key + ".date");
                String recordTypeStr = config.getString("leaderboard." + key + ".type");
                
                LocalDateTime recordDate = LocalDateTime.parse(recordDateStr, dateFormatter);
                CourseType recordType = CourseType.valueOf(recordTypeStr);
                
                records.add(new RaceRecord(recordPlayer, course, recordTime, recordDate, recordType));
            }
        }
        
        // Remove any existing record for this player (one record per player rule)
        records.removeIf(record -> record.getPlayer().equals(player));
        
        // Add new record
        records.add(new RaceRecord(player, course, time, date, type));
        
        // Sort by time and keep top 5
        records.sort(Comparator.comparing(RaceRecord::getTime));
        records = records.stream().limit(5).collect(Collectors.toList());
        
        // Save back to file
        config.set("leaderboard", null); // Clear existing
        for (int i = 0; i < records.size(); i++) {
            RaceRecord record = records.get(i);
            config.set("leaderboard.position_" + (i + 1) + ".player", record.getPlayer());
            config.set("leaderboard.position_" + (i + 1) + ".time", record.getTime());
            config.set("leaderboard.position_" + (i + 1) + ".date", record.getDate().format(dateFormatter));
            config.set("leaderboard.position_" + (i + 1) + ".type", record.getType().name());
        }
        
        config.set("last-updated", LocalDateTime.now().format(dateFormatter));
        config.set("last-reset", LocalDateTime.now().format(dateFormatter));
        
        config.save(dailyFile);
        plugin.debugLog("Updated daily leaderboard for " + course + " - " + records.size() + " records");
    }
    
    /**
     * Update weekly leaderboard with one record per player rule
     */
    private void updateWeeklyLeaderboard(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = getCourseDirectory(course, type);
        if (!courseDir.exists()) {
            courseDir.mkdirs();
        }
        
        File weeklyFile = new File(courseDir, "weekly_leaderboard.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(weeklyFile);
        
        // Load existing records
        List<RaceRecord> records = new ArrayList<>();
        if (config.contains("leaderboard")) {
            for (String key : config.getConfigurationSection("leaderboard").getKeys(false)) {
                String recordPlayer = config.getString("leaderboard." + key + ".player");
                double recordTime = config.getDouble("leaderboard." + key + ".time");
                String recordDateStr = config.getString("leaderboard." + key + ".date");
                String recordTypeStr = config.getString("leaderboard." + key + ".type");
                
                LocalDateTime recordDate = LocalDateTime.parse(recordDateStr, dateFormatter);
                CourseType recordType = CourseType.valueOf(recordTypeStr);
                
                records.add(new RaceRecord(recordPlayer, course, recordTime, recordDate, recordType));
            }
        }
        
        // Remove any existing record for this player (one record per player rule)
        records.removeIf(record -> record.getPlayer().equals(player));
        
        // Add new record
        records.add(new RaceRecord(player, course, time, date, type));
        
        // Sort by time and keep top 5
        records.sort(Comparator.comparing(RaceRecord::getTime));
        records = records.stream().limit(5).collect(Collectors.toList());
        
        // Save back to file
        config.set("leaderboard", null); // Clear existing
        for (int i = 0; i < records.size(); i++) {
            RaceRecord record = records.get(i);
            config.set("leaderboard.position_" + (i + 1) + ".player", record.getPlayer());
            config.set("leaderboard.position_" + (i + 1) + ".time", record.getTime());
            config.set("leaderboard.position_" + (i + 1) + ".date", record.getDate().format(dateFormatter));
            config.set("leaderboard.position_" + (i + 1) + ".type", record.getType().name());
        }
        
        config.set("last-updated", LocalDateTime.now().format(dateFormatter));
        config.set("last-reset", LocalDateTime.now().format(dateFormatter));
        
        config.save(weeklyFile);
        plugin.debugLog("Updated weekly leaderboard for " + course + " - " + records.size() + " records");
    }
    
    /**
     * Update monthly leaderboard with one record per player rule
     */
    private void updateMonthlyLeaderboard(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = getCourseDirectory(course, type);
        if (!courseDir.exists()) {
            courseDir.mkdirs();
        }
        
        File monthlyFile = new File(courseDir, "monthly_leaderboard.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(monthlyFile);
        
        // Load existing records
        List<RaceRecord> records = new ArrayList<>();
        if (config.contains("leaderboard")) {
            for (String key : config.getConfigurationSection("leaderboard").getKeys(false)) {
                String recordPlayer = config.getString("leaderboard." + key + ".player");
                double recordTime = config.getDouble("leaderboard." + key + ".time");
                String recordDateStr = config.getString("leaderboard." + key + ".date");
                String recordTypeStr = config.getString("leaderboard." + key + ".type");
                
                LocalDateTime recordDate = LocalDateTime.parse(recordDateStr, dateFormatter);
                CourseType recordType = CourseType.valueOf(recordTypeStr);
                
                records.add(new RaceRecord(recordPlayer, course, recordTime, recordDate, recordType));
            }
        }
        
        // Remove any existing record for this player (one record per player rule)
        records.removeIf(record -> record.getPlayer().equals(player));
        
        // Add new record
        records.add(new RaceRecord(player, course, time, date, type));
        
        // Sort by time and keep top 5
        records.sort(Comparator.comparing(RaceRecord::getTime));
        records = records.stream().limit(5).collect(Collectors.toList());
        
        // Save back to file
        config.set("leaderboard", null); // Clear existing
        for (int i = 0; i < records.size(); i++) {
            RaceRecord record = records.get(i);
            config.set("leaderboard.position_" + (i + 1) + ".player", record.getPlayer());
            config.set("leaderboard.position_" + (i + 1) + ".time", record.getTime());
            config.set("leaderboard.position_" + (i + 1) + ".date", record.getDate().format(dateFormatter));
            config.set("leaderboard.position_" + (i + 1) + ".type", record.getType().name());
        }
        
        config.set("last-updated", LocalDateTime.now().format(dateFormatter));
        config.set("last-reset", LocalDateTime.now().format(dateFormatter));
        
        config.save(monthlyFile);
        plugin.debugLog("Updated monthly leaderboard for " + course + " - " + records.size() + " records");
    }
    
    /**
     * Update player recent races
     */
    private void updatePlayerRecent(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerRecentFile);
        
        // Load existing recent races
        List<Map<String, Object>> recentRaces = new ArrayList<>();
        if (config.contains("players." + player + ".recent")) {
            for (String key : config.getConfigurationSection("players." + player + ".recent").getKeys(false)) {
                Map<String, Object> race = new HashMap<>();
                race.put("course", config.getString("players." + player + ".recent." + key + ".course"));
                race.put("time", config.getDouble("players." + player + ".recent." + key + ".time"));
                race.put("date", config.getString("players." + player + ".recent." + key + ".date"));
                race.put("type", config.getString("players." + player + ".recent." + key + ".type"));
                recentRaces.add(race);
            }
        }
        
        // Add new race
        Map<String, Object> newRace = new HashMap<>();
        newRace.put("course", course);
        newRace.put("time", Math.round(time * 100.0) / 100.0);
        newRace.put("date", date.format(dateFormatter));
        newRace.put("type", type.name());
        recentRaces.add(newRace);
        
        // Keep only last 10 races
        if (recentRaces.size() > 10) {
            recentRaces = recentRaces.subList(recentRaces.size() - 10, recentRaces.size());
        }
        
        // Save back to file
        config.set("players." + player + ".recent", null);
        for (int i = 0; i < recentRaces.size(); i++) {
            Map<String, Object> race = recentRaces.get(i);
            config.set("players." + player + ".recent.race" + (i + 1), race);
        }
        
        config.save(playerRecentFile);
    }
    
    /**
     * Update player statistics
     */
    private void updatePlayerStats(String player, CourseType type) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
        
        // Initialize player stats if not exists
        if (!config.contains("players." + player)) {
            config.set("players." + player + ".totalRaces", 0);
            config.set("players." + player + ".singleplayerRaces", 0);
            config.set("players." + player + ".multiplayerRaces", 0);
        }
        
        // Update stats
        int totalRaces = config.getInt("players." + player + ".totalRaces") + 1;
        config.set("players." + player + ".totalRaces", totalRaces);
        
        if (type == CourseType.SINGLEPLAYER) {
            int spRaces = config.getInt("players." + player + ".singleplayerRaces") + 1;
            config.set("players." + player + ".singleplayerRaces", spRaces);
        } else {
            int mpRaces = config.getInt("players." + player + ".multiplayerRaces") + 1;
            config.set("players." + player + ".multiplayerRaces", mpRaces);
        }
        
        config.set("players." + player + ".lastRaceDate", LocalDateTime.now().format(dateFormatter));
        
        config.save(playerStatsFile);
    }
    
    @Override
    public List<RaceRecord> getTopTimes(String course, int limit) {
        // Load from both singleplayer and multiplayer
        List<RaceRecord> allRecords = new ArrayList<>();
        
        // Load singleplayer records
        loadAllRecordsFromDirectory(allRecords, getCourseDirectory(course, CourseType.SINGLEPLAYER));
        
        // Load multiplayer records
        loadAllRecordsFromDirectory(allRecords, getCourseDirectory(course, CourseType.MULTIPLAYER));
        
        // Sort by time and apply one record per player rule
        Map<String, RaceRecord> bestTimes = new HashMap<>();
        for (RaceRecord record : allRecords) {
            String player = record.getPlayer();
            if (!bestTimes.containsKey(player) || record.getTime() < bestTimes.get(player).getTime()) {
                bestTimes.put(player, record);
            }
        }
        
        List<RaceRecord> result = new ArrayList<>(bestTimes.values());
        result.sort(Comparator.comparing(RaceRecord::getTime));
        
        return result.stream().limit(limit).collect(Collectors.toList());
    }
    
    @Override
    public List<RaceRecord> getTopTimesForPeriod(String course, Period period, int limit) {
        List<RaceRecord> records = new ArrayList<>();
        
        // Load from both singleplayer and multiplayer
        loadPeriodRecordsFromDirectory(records, getCourseDirectory(course, CourseType.SINGLEPLAYER), period);
        loadPeriodRecordsFromDirectory(records, getCourseDirectory(course, CourseType.MULTIPLAYER), period);
        
        // Sort by time and apply one record per player rule
        Map<String, RaceRecord> bestTimes = new HashMap<>();
        for (RaceRecord record : records) {
            String player = record.getPlayer();
            if (!bestTimes.containsKey(player) || record.getTime() < bestTimes.get(player).getTime()) {
                bestTimes.put(player, record);
            }
        }
        
        List<RaceRecord> result = new ArrayList<>(bestTimes.values());
        result.sort(Comparator.comparing(RaceRecord::getTime));
        
        return result.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Load all records from a specific course directory
     */
    private void loadAllRecordsFromDirectory(List<RaceRecord> records, File courseDir) {
        try {
            File allRecordsFile = new File(courseDir, "all_records.yml");
            
            if (!allRecordsFile.exists()) {
                return;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(allRecordsFile);
            if (config.contains("records")) {
                for (String key : config.getConfigurationSection("records").getKeys(false)) {
                    String player = config.getString("records." + key + ".player");
                    String courseName = config.getString("records." + key + ".course");
                    double time = config.getDouble("records." + key + ".time");
                    String dateStr = config.getString("records." + key + ".date");
                    String typeStr = config.getString("records." + key + ".type");
                    
                    LocalDateTime date = LocalDateTime.parse(dateStr, dateFormatter);
                    CourseType type = CourseType.valueOf(typeStr);
                    
                    records.add(new RaceRecord(player, courseName, time, date, type));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load all records from " + courseDir.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Load period records from a specific course directory
     */
    private void loadPeriodRecordsFromDirectory(List<RaceRecord> records, File courseDir, Period period) {
        try {
            File periodFile = getPeriodFile(courseDir, period);
            
            if (!periodFile.exists()) {
                return;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(periodFile);
            if (config.contains("leaderboard")) {
                for (String key : config.getConfigurationSection("leaderboard").getKeys(false)) {
                    String player = config.getString("leaderboard." + key + ".player");
                    String courseName = courseDir.getName();
                    double time = config.getDouble("leaderboard." + key + ".time");
                    String dateStr = config.getString("leaderboard." + key + ".date");
                    String typeStr = config.getString("leaderboard." + key + ".type");
                    
                    LocalDateTime date = LocalDateTime.parse(dateStr, dateFormatter);
                    CourseType type = CourseType.valueOf(typeStr);
                    
                    records.add(new RaceRecord(player, courseName, time, date, type));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load period records from " + courseDir.getName() + " period " + period + ": " + e.getMessage());
        }
    }
    
    private File getPeriodFile(File courseDir, Period period) {
        switch (period) {
            case DAILY:
                return new File(courseDir, "daily_leaderboard.yml");
            case WEEKLY:
                return new File(courseDir, "weekly_leaderboard.yml");
            case MONTHLY:
                return new File(courseDir, "monthly_leaderboard.yml");
            default:
                return null;
        }
    }
    
    @Override
    public List<RaceRecord> getPlayerRecent(String player, int limit) {
        List<RaceRecord> records = new ArrayList<>();
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerRecentFile);
            if (config.contains("players." + player + ".recent")) {
                for (String key : config.getConfigurationSection("players." + player + ".recent").getKeys(false)) {
                    String course = config.getString("players." + player + ".recent." + key + ".course");
                    double time = config.getDouble("players." + player + ".recent." + key + ".time");
                    String dateStr = config.getString("players." + player + ".recent." + key + ".date");
                    String typeStr = config.getString("players." + player + ".recent." + key + ".type");
                    
                    LocalDateTime date = LocalDateTime.parse(dateStr, dateFormatter);
                    CourseType type = CourseType.valueOf(typeStr);
                    
                    records.add(new RaceRecord(player, course, time, date, type));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load recent races for " + player + ": " + e.getMessage());
        }
        
        records.sort(Comparator.comparing(RaceRecord::getDate).reversed());
        return records.stream().limit(limit).collect(Collectors.toList());
    }
    
    public Map<String, Object> getPlayerStats(String player) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
            if (config.contains("players." + player)) {
                stats.put("totalRaces", config.getInt("players." + player + ".totalRaces", 0));
                stats.put("singleplayerRaces", config.getInt("players." + player + ".singleplayerRaces", 0));
                stats.put("multiplayerRaces", config.getInt("players." + player + ".multiplayerRaces", 0));
                stats.put("lastRaceDate", config.getString("players." + player + ".lastRaceDate", "Never"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player stats for " + player + ": " + e.getMessage());
        }
        
        return stats;
    }
    
    @Override
    public List<RaceRecord> getPlayerCourseTimes(String player, String course) {
        List<RaceRecord> records = new ArrayList<>();
        
        // Load from both singleplayer and multiplayer
        loadAllRecordsFromDirectory(records, getCourseDirectory(course, CourseType.SINGLEPLAYER));
        loadAllRecordsFromDirectory(records, getCourseDirectory(course, CourseType.MULTIPLAYER));
        
        // Filter for this player and course
        records = records.stream()
            .filter(record -> record.getPlayer().equals(player) && record.getCourse().equals(course))
            .sorted(Comparator.comparing(RaceRecord::getTime))
            .collect(Collectors.toList());
        
        return records;
    }
    
    @Override
    public RaceRecord getPlayerBestTime(String player, String course) {
        List<RaceRecord> courseTimes = getPlayerCourseTimes(player, course);
        return courseTimes.isEmpty() ? null : courseTimes.get(0);
    }
    
    @Override
    public int getPlayerTotalRaces(String player) {
        Map<String, Object> stats = getPlayerStats(player);
        return (Integer) stats.getOrDefault("totalRaces", 0);
    }
    
    @Override
    public int getPlayerRacesByType(String player, CourseType type) {
        Map<String, Object> stats = getPlayerStats(player);
        if (type == CourseType.SINGLEPLAYER) {
            return (Integer) stats.getOrDefault("singleplayerRaces", 0);
        } else {
            return (Integer) stats.getOrDefault("multiplayerRaces", 0);
        }
    }
    
    @Override
    public String getPlayerFavoriteCourse(String player) {
        // Implementation for finding most raced course
        // This would require analyzing all records - could be expensive
        // For now, return null to indicate not implemented
        return null;
    }
    
    @Override
    public boolean resetCourseRecords(String courseName) {
        try {
            // Reset both singleplayer and multiplayer records
            boolean singleplayerReset = resetCourseDirectory(getCourseDirectory(courseName, CourseType.SINGLEPLAYER));
            boolean multiplayerReset = resetCourseDirectory(getCourseDirectory(courseName, CourseType.MULTIPLAYER));
            
            return singleplayerReset && multiplayerReset;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reset course records for " + courseName + ": " + e.getMessage());
            return false;
        }
    }
    
    private boolean resetCourseDirectory(File courseDir) {
        try {
            if (courseDir.exists()) {
                // Delete all files in course directory
                File[] files = courseDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }
                courseDir.delete();
            }
            
            // Clear cache for this course directory
            String courseName = courseDir.getName();
            leaderboardCache.entrySet().removeIf(entry -> entry.getKey().startsWith(courseName + "_"));
            
            plugin.debugLog("Reset all records for course directory: " + courseDir.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reset course records: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean resetPlayerRecords(String playerName) {
        try {
            // Reset player stats
            FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(playerStatsFile);
            statsConfig.set("players." + playerName, null);
            statsConfig.save(playerStatsFile);
            
            // Reset player recent
            FileConfiguration recentConfig = YamlConfiguration.loadConfiguration(playerRecentFile);
            recentConfig.set("players." + playerName, null);
            recentConfig.save(playerRecentFile);
            
            plugin.debugLog("Reset all records for player: " + playerName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reset player records: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean resetAllRecords() {
        try {
            // Delete all singleplayer course directories
            if (singleplayerDir.exists()) {
                File[] courseDirs = singleplayerDir.listFiles();
                if (courseDirs != null) {
                    for (File courseDir : courseDirs) {
                        if (courseDir.isDirectory()) {
                            File[] files = courseDir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    file.delete();
                                }
                            }
                            courseDir.delete();
                        }
                    }
                }
            }
            
            // Delete all multiplayer course directories
            if (multiplayerDir.exists()) {
                File[] courseDirs = multiplayerDir.listFiles();
                if (courseDirs != null) {
                    for (File courseDir : courseDirs) {
                        if (courseDir.isDirectory()) {
                            File[] files = courseDir.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    file.delete();
                                }
                            }
                            courseDir.delete();
                        }
                    }
                }
            }
            
            // Reset player data
            playerStatsFile.delete();
            playerRecentFile.delete();
            
            // Clear cache
            leaderboardCache.clear();
            cacheTimestamps.clear();
            
            plugin.debugLog("Reset ALL race records");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reset all records: " + e.getMessage());
            return false;
        }
    }
}
