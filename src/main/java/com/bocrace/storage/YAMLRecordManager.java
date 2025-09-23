package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.RaceRecord;
import com.bocrace.model.CourseType;
import com.bocrace.model.Period;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Clean YAML implementation of RecordManager
 * NEW STRUCTURE: All records saved + period verification files + 30-day retention
 */
public class YAMLRecordManager implements RecordManager {
    
    private final BOCRacePlugin plugin;
    
    // NEW CLEAN DIRECTORY STRUCTURE
    private final File dataDir;
    private final File coursesDir;
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
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.coursesDir = new File(dataDir, "courses");
        this.playersDir = new File(dataDir, "players");
        this.cacheDir = new File(dataDir, "cache");
        this.playerStatsFile = new File(playersDir, "stats.yml");
        this.playerRecentFile = new File(playersDir, "recent.yml");
        
        createDirectoryStructure();
    }
    
    private void createDirectoryStructure() {
        plugin.debugLog("Creating NEW clean data directory structure...");
        
        // Create main directories
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            plugin.debugLog("Created data directory: " + dataDir.getAbsolutePath());
        }
        if (!coursesDir.exists()) {
            coursesDir.mkdirs();
            plugin.debugLog("Created courses directory: " + coursesDir.getAbsolutePath());
        }
        if (!playersDir.exists()) {
            playersDir.mkdirs();
            plugin.debugLog("Created players directory: " + playersDir.getAbsolutePath());
        }
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
            plugin.debugLog("Created cache directory: " + cacheDir.getAbsolutePath());
        }
        
        plugin.debugLog("NEW directory structure created successfully");
    }
    
    @Override
    public void saveRaceRecord(String player, String course, double time, CourseType type) {
        saveRaceRecord(player, course, time, type, LocalDateTime.now());
    }
    
    @Override
    public void saveRaceRecord(String player, String course, double time, CourseType type, LocalDateTime date) {
        plugin.debugLog("Saving race record: " + player + " - " + course + " - " + time + "s - " + type + " - " + date);
        
        try {
            // Round time to 2 decimal places (fix crazy precision)
            double cleanTime = Math.round(time * 100.0) / 100.0;
            
            // Save to ALL records file
            saveToAllRecords(player, course, cleanTime, type, date);
            
            // Save to period files for verification
            saveToPeriodFiles(player, course, cleanTime, type, date);
            
            // Update player data
            updatePlayerData(player, type);
            
            // Update cache for real-time hologram updates
            updateCache(course);
            
            plugin.debugLog("Race record saved successfully to new structure");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save race record: " + e.getMessage());
        }
    }
    
    /**
     * Save to all_records.yml - EVERY race record (no data loss)
     */
    private void saveToAllRecords(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = new File(coursesDir, course);
        if (!courseDir.exists()) {
            courseDir.mkdirs();
        }
        
        File allRecordsFile = new File(courseDir, "all_records.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(allRecordsFile);
        
        // Get existing records
        List<Map<String, Object>> records = new ArrayList<>();
        if (config.contains("records")) {
            for (String key : config.getConfigurationSection("records").getKeys(false)) {
                Map<String, Object> record = new HashMap<>();
                record.put("player", config.getString("records." + key + ".player"));
                record.put("time", config.getDouble("records." + key + ".time"));
                record.put("date", config.getString("records." + key + ".date"));
                record.put("type", config.getString("records." + key + ".type"));
                records.add(record);
            }
        }
        
        // Add new record
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put("player", player);
        newRecord.put("time", time);
        newRecord.put("date", date.format(dateFormatter));
        newRecord.put("type", type.toString());
        records.add(newRecord);
        
        // Sort by date (most recent first) - keep ALL records
        records.sort((a, b) -> {
            String dateA = (String) a.get("date");
            String dateB = (String) b.get("date");
            return dateB.compareTo(dateA); // Most recent first
        });
        
        // Save back to file
        config.set("records", null);
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            String key = "record" + (i + 1);
            config.set("records." + key + ".player", record.get("player"));
            config.set("records." + key + ".time", record.get("time"));
            config.set("records." + key + ".date", record.get("date"));
            config.set("records." + key + ".type", record.get("type"));
        }
        
        config.save(allRecordsFile);
        plugin.debugLog("Saved to all_records.yml - Total records: " + records.size());
    }
    
    /**
     * Save to period files for easy verification
     */
    private void saveToPeriodFiles(String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        File courseDir = new File(coursesDir, course);
        
        // Daily file
        String dailyFileName = "daily_" + date.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".yml";
        saveToPeriodFile(new File(courseDir, dailyFileName), player, course, time, type, date);
        
        // Weekly file
        String weeklyFileName = "weekly_" + getWeekString(date) + ".yml";
        saveToPeriodFile(new File(courseDir, weeklyFileName), player, course, time, type, date);
        
        // Monthly file
        String monthlyFileName = "monthly_" + date.format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".yml";
        saveToPeriodFile(new File(courseDir, monthlyFileName), player, course, time, type, date);
        
        plugin.debugLog("Saved to period files - Daily: " + dailyFileName + ", Weekly: " + weeklyFileName + ", Monthly: " + monthlyFileName);
    }
    
    private void saveToPeriodFile(File periodFile, String player, String course, double time, CourseType type, LocalDateTime date) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(periodFile);
        
        List<Map<String, Object>> records = new ArrayList<>();
        if (config.contains("records")) {
            for (String key : config.getConfigurationSection("records").getKeys(false)) {
                Map<String, Object> record = new HashMap<>();
                record.put("player", config.getString("records." + key + ".player"));
                record.put("time", config.getDouble("records." + key + ".time"));
                record.put("date", config.getString("records." + key + ".date"));
                record.put("type", config.getString("records." + key + ".type"));
                records.add(record);
            }
        }
        
        // Add new record
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put("player", player);
        newRecord.put("course", course);
        newRecord.put("time", time);
        newRecord.put("date", date.format(dateFormatter));
        newRecord.put("type", type.toString());
        records.add(newRecord);
        
        // Sort by time (best first)
        records.sort((a, b) -> Double.compare((Double) a.get("time"), (Double) b.get("time")));
        
        // Save back to file
        config.set("records", null);
        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            String key = "record" + (i + 1);
            config.set("records." + key + ".player", record.get("player"));
            config.set("records." + key + ".course", record.get("course"));
            config.set("records." + key + ".time", record.get("time"));
            config.set("records." + key + ".date", record.get("date"));
            config.set("records." + key + ".type", record.get("type"));
        }
        
        config.save(periodFile);
    }
    
    /**
     * Update player statistics and recent races
     */
    private void updatePlayerData(String player, CourseType type) throws IOException {
        // Update player stats
        FileConfiguration statsConfig = YamlConfiguration.loadConfiguration(playerStatsFile);
        
        int totalRaces = statsConfig.getInt("players." + player + ".totalRaces", 0);
        int singleplayerRaces = statsConfig.getInt("players." + player + ".singleplayerRaces", 0);
        int multiplayerRaces = statsConfig.getInt("players." + player + ".multiplayerRaces", 0);
        
        statsConfig.set("players." + player + ".totalRaces", totalRaces + 1);
        statsConfig.set("players." + player + ".lastRaceDate", LocalDateTime.now().format(dateFormatter));
        
        if (type == CourseType.SINGLEPLAYER) {
            statsConfig.set("players." + player + ".singleplayerRaces", singleplayerRaces + 1);
        } else {
            statsConfig.set("players." + player + ".multiplayerRaces", multiplayerRaces + 1);
        }
        
        statsConfig.save(playerStatsFile);
        
        // Update recent races (keep last 10)
        FileConfiguration recentConfig = YamlConfiguration.loadConfiguration(playerRecentFile);
        
        List<Map<String, Object>> recent = new ArrayList<>();
        if (recentConfig.contains("players." + player + ".recent")) {
            for (String key : recentConfig.getConfigurationSection("players." + player + ".recent").getKeys(false)) {
                Map<String, Object> record = new HashMap<>();
                record.put("course", recentConfig.getString("players." + player + ".recent." + key + ".course"));
                record.put("time", recentConfig.getDouble("players." + player + ".recent." + key + ".time"));
                record.put("date", recentConfig.getString("players." + player + ".recent." + key + ".date"));
                record.put("type", recentConfig.getString("players." + player + ".recent." + key + ".type"));
                recent.add(record);
            }
        }
        
        // Add new recent record at beginning
        Map<String, Object> newRecent = new HashMap<>();
        newRecent.put("course", "Unknown"); // Will be updated by caller
        newRecent.put("time", 0.0); // Will be updated by caller
        newRecent.put("date", LocalDateTime.now().format(dateFormatter));
        newRecent.put("type", type.toString());
        recent.add(0, newRecent);
        
        // Keep only last 10
        if (recent.size() > 10) {
            recent = recent.subList(0, 10);
        }
        
        // Save back
        recentConfig.set("players." + player + ".recent", null);
        for (int i = 0; i < recent.size(); i++) {
            Map<String, Object> record = recent.get(i);
            String key = "race" + (i + 1);
            recentConfig.set("players." + player + ".recent." + key + ".course", record.get("course"));
            recentConfig.set("players." + player + ".recent." + key + ".time", record.get("time"));
            recentConfig.set("players." + player + ".recent." + key + ".date", record.get("date"));
            recentConfig.set("players." + player + ".recent." + key + ".type", record.get("type"));
        }
        
        recentConfig.save(playerRecentFile);
    }
    
    /**
     * Update cache for real-time hologram updates
     */
    private void updateCache(String course) {
        // Invalidate cache for this course
        leaderboardCache.remove(course + "_all");
        leaderboardCache.remove(course + "_daily");
        leaderboardCache.remove(course + "_weekly");
        leaderboardCache.remove(course + "_monthly");
        
        plugin.debugLog("Cache invalidated for course: " + course);
    }
    
    @Override
    public List<RaceRecord> getTopTimes(String course, int limit) {
        // Check cache first
        String cacheKey = course + "_all";
        if (leaderboardCache.containsKey(cacheKey) && !isCacheExpired(cacheKey)) {
            List<RaceRecord> cached = leaderboardCache.get(cacheKey);
            return cached.size() > limit ? cached.subList(0, limit) : cached;
        }
        
        // Load from file
        List<RaceRecord> records = loadAllRecords(course);
        
        // Apply ONE RECORD PER PLAYER rule
        Map<String, RaceRecord> bestPerPlayer = new HashMap<>();
        for (RaceRecord record : records) {
            String player = record.getPlayer();
            if (!bestPerPlayer.containsKey(player) || record.getTime() < bestPerPlayer.get(player).getTime()) {
                bestPerPlayer.put(player, record);
            }
        }
        
        // Sort by time (best first) and limit
        List<RaceRecord> result = new ArrayList<>(bestPerPlayer.values());
        result.sort(Comparator.comparingDouble(RaceRecord::getTime));
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        // Update cache
        leaderboardCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, LocalDateTime.now());
        
        return result;
    }
    
    @Override
    public List<RaceRecord> getTopTimesForPeriod(String course, Period period, int limit) {
        String cacheKey = course + "_" + period.name().toLowerCase();
        
        // Check cache first
        if (leaderboardCache.containsKey(cacheKey) && !isCacheExpired(cacheKey)) {
            List<RaceRecord> cached = leaderboardCache.get(cacheKey);
            return cached.size() > limit ? cached.subList(0, limit) : cached;
        }
        
        // Load from period file
        List<RaceRecord> records = loadPeriodRecords(course, period);
        
        // Apply ONE RECORD PER PLAYER rule
        Map<String, RaceRecord> bestPerPlayer = new HashMap<>();
        for (RaceRecord record : records) {
            String player = record.getPlayer();
            if (!bestPerPlayer.containsKey(player) || record.getTime() < bestPerPlayer.get(player).getTime()) {
                bestPerPlayer.put(player, record);
            }
        }
        
        // Sort by time (best first) and limit
        List<RaceRecord> result = new ArrayList<>(bestPerPlayer.values());
        result.sort(Comparator.comparingDouble(RaceRecord::getTime));
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        // Update cache
        leaderboardCache.put(cacheKey, result);
        cacheTimestamps.put(cacheKey, LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Load all records from all_records.yml
     */
    private List<RaceRecord> loadAllRecords(String course) {
        List<RaceRecord> records = new ArrayList<>();
        
        try {
            File courseDir = new File(coursesDir, course);
            File allRecordsFile = new File(courseDir, "all_records.yml");
            
            if (!allRecordsFile.exists()) {
                return records;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(allRecordsFile);
            if (config.contains("records")) {
                for (String key : config.getConfigurationSection("records").getKeys(false)) {
                    String player = config.getString("records." + key + ".player");
                    double time = config.getDouble("records." + key + ".time");
                    String dateStr = config.getString("records." + key + ".date");
                    String typeStr = config.getString("records." + key + ".type");
                    
                    LocalDateTime date = LocalDateTime.parse(dateStr, dateFormatter);
                    CourseType type = CourseType.valueOf(typeStr);
                    
                    records.add(new RaceRecord(player, course, time, date, type));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load all records for " + course + ": " + e.getMessage());
        }
        
        return records;
    }
    
    /**
     * Load records from period file
     */
    private List<RaceRecord> loadPeriodRecords(String course, Period period) {
        List<RaceRecord> records = new ArrayList<>();
        
        try {
            File courseDir = new File(coursesDir, course);
            File periodFile = getPeriodFile(courseDir, period);
            
            if (!periodFile.exists()) {
                return records;
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(periodFile);
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
            plugin.getLogger().warning("Failed to load period records for " + course + " period " + period + ": " + e.getMessage());
        }
        
        return records;
    }
    
    private File getPeriodFile(File courseDir, Period period) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (period) {
            case DAILY:
                String dailyFileName = "daily_" + now.format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".yml";
                return new File(courseDir, dailyFileName);
            case WEEKLY:
                String weeklyFileName = "weekly_" + getWeekString(now) + ".yml";
                return new File(courseDir, weeklyFileName);
            case MONTHLY:
                String monthlyFileName = "monthly_" + now.format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".yml";
                return new File(courseDir, monthlyFileName);
            default:
                return null;
        }
    }
    
    private String getWeekString(LocalDateTime date) {
        int year = date.getYear();
        int week = date.get(WeekFields.ISO.weekOfYear());
        return year + "_week_" + week;
    }
    
    private boolean isCacheExpired(String cacheKey) {
        if (!cacheTimestamps.containsKey(cacheKey)) {
            return true;
        }
        
        LocalDateTime cacheTime = cacheTimestamps.get(cacheKey);
        return LocalDateTime.now().isAfter(cacheTime.plusSeconds(CACHE_DURATION / 1000));
    }
    
    // Implement other RecordManager methods...
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
            plugin.getLogger().warning("Failed to load recent records for " + player + ": " + e.getMessage());
        }
        
        return records.size() > limit ? records.subList(0, limit) : records;
    }
    
    @Override
    public List<RaceRecord> getPlayerCourseTimes(String player, String course) {
        List<RaceRecord> allRecords = loadAllRecords(course);
        return allRecords.stream()
                .filter(record -> record.getPlayer().equals(player))
                .sorted(Comparator.comparingDouble(RaceRecord::getTime))
                .collect(Collectors.toList());
    }
    
    @Override
    public RaceRecord getPlayerBestTime(String player, String course) {
        List<RaceRecord> playerTimes = getPlayerCourseTimes(player, course);
        return playerTimes.isEmpty() ? null : playerTimes.get(0);
    }
    
    @Override
    public int getPlayerTotalRaces(String player) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
            return config.getInt("players." + player + ".totalRaces", 0);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get total races for " + player + ": " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int getPlayerRacesByType(String player, CourseType type) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
            if (type == CourseType.SINGLEPLAYER) {
                return config.getInt("players." + player + ".singleplayerRaces", 0);
            } else {
                return config.getInt("players." + player + ".multiplayerRaces", 0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get races by type for " + player + ": " + e.getMessage());
            return 0;
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
            File courseDir = new File(coursesDir, courseName);
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
            
            // Clear cache
            leaderboardCache.entrySet().removeIf(entry -> entry.getKey().startsWith(courseName + "_"));
            
            plugin.debugLog("Reset all records for course: " + courseName);
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
            // Delete all course directories
            if (coursesDir.exists()) {
                File[] courseDirs = coursesDir.listFiles();
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
