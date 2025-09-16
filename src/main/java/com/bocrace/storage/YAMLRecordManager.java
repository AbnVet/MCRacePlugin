package com.bocrace.storage;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.RaceRecord;
import com.bocrace.model.CourseType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML implementation of RecordManager
 * Stores race records in separate YAML files for easy MySQL migration later
 */
public class YAMLRecordManager implements RecordManager {
    
    private final BOCRacePlugin plugin;
    private final File recordsDir;
    private final File playerStatsFile;
    private final File playerRecentFile;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public YAMLRecordManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.recordsDir = new File(plugin.getDataFolder(), "records");
        this.playerStatsFile = new File(plugin.getDataFolder(), "player_stats.yml");
        this.playerRecentFile = new File(plugin.getDataFolder(), "player_recent.yml");
        
        // Create directories if they don't exist
        if (!recordsDir.exists()) {
            recordsDir.mkdirs();
        }
    }
    
    @Override
    public void saveRaceRecord(String player, String course, double time, CourseType type) {
        plugin.debugLog("Saving race record: " + player + " - " + course + " - " + time + "s - " + type);
        
        try {
            // Save to course-specific records file
            saveToCourseRecords(player, course, time, type);
            
            // Update player recent races
            updatePlayerRecent(player, course, time, type);
            
            // Update player stats
            updatePlayerStats(player, type);
            
            plugin.debugLog("Race record saved successfully");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save race record: " + e.getMessage());
        }
    }
    
    private void saveToCourseRecords(String player, String course, double time, CourseType type) throws IOException {
        File courseFile = new File(recordsDir, course + "_records.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(courseFile);
        
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
        newRecord.put("date", LocalDateTime.now().format(dateFormatter));
        newRecord.put("type", type.toString());
        records.add(newRecord);
        
        // Sort by time (best first) and keep only top 5
        records.sort((a, b) -> Double.compare((Double) a.get("time"), (Double) b.get("time")));
        if (records.size() > 5) {
            records = records.subList(0, 5);
        }
        
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
        
        config.save(courseFile);
        plugin.debugLog("Course records updated: " + course + " (" + records.size() + " records)");
    }
    
    private void updatePlayerRecent(String player, String course, double time, CourseType type) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerRecentFile);
        
        // Get existing recent races
        List<Map<String, Object>> recent = new ArrayList<>();
        if (config.contains("players." + player + ".recent")) {
            for (String key : config.getConfigurationSection("players." + player + ".recent").getKeys(false)) {
                Map<String, Object> record = new HashMap<>();
                record.put("course", config.getString("players." + player + ".recent." + key + ".course"));
                record.put("time", config.getDouble("players." + player + ".recent." + key + ".time"));
                record.put("date", config.getString("players." + player + ".recent." + key + ".date"));
                record.put("type", config.getString("players." + player + ".recent." + key + ".type"));
                recent.add(record);
            }
        }
        
        // Add new record at the beginning
        Map<String, Object> newRecord = new HashMap<>();
        newRecord.put("course", course);
        newRecord.put("time", time);
        newRecord.put("date", LocalDateTime.now().format(dateFormatter));
        newRecord.put("type", type.toString());
        recent.add(0, newRecord);
        
        // Keep only most recent 5
        if (recent.size() > 5) {
            recent = recent.subList(0, 5);
        }
        
        // Save back to file
        config.set("players." + player + ".recent", null);
        for (int i = 0; i < recent.size(); i++) {
            Map<String, Object> record = recent.get(i);
            String key = "race" + (i + 1);
            config.set("players." + player + ".recent." + key + ".course", record.get("course"));
            config.set("players." + player + ".recent." + key + ".time", record.get("time"));
            config.set("players." + player + ".recent." + key + ".date", record.get("date"));
            config.set("players." + player + ".recent." + key + ".type", record.get("type"));
        }
        
        config.save(playerRecentFile);
        plugin.debugLog("Player recent races updated: " + player + " (" + recent.size() + " races)");
    }
    
    private void updatePlayerStats(String player, CourseType type) throws IOException {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
        
        // Get current stats
        int totalRaces = config.getInt("players." + player + ".totalRaces", 0);
        int singleplayerRaces = config.getInt("players." + player + ".singleplayerRaces", 0);
        int multiplayerRaces = config.getInt("players." + player + ".multiplayerRaces", 0);
        
        // Update stats
        totalRaces++;
        if (type == CourseType.SINGLEPLAYER) {
            singleplayerRaces++;
        } else if (type == CourseType.MULTIPLAYER) {
            multiplayerRaces++;
        }
        
        // Save updated stats
        config.set("players." + player + ".totalRaces", totalRaces);
        config.set("players." + player + ".singleplayerRaces", singleplayerRaces);
        config.set("players." + player + ".multiplayerRaces", multiplayerRaces);
        config.set("players." + player + ".lastRaceDate", LocalDateTime.now().format(dateFormatter));
        
        config.save(playerStatsFile);
        plugin.debugLog("Player stats updated: " + player + " (Total: " + totalRaces + ", SP: " + singleplayerRaces + ", MP: " + multiplayerRaces + ")");
    }
    
    @Override
    public List<RaceRecord> getTopTimes(String course, int limit) {
        File courseFile = new File(recordsDir, course + "_records.yml");
        if (!courseFile.exists()) {
            return new ArrayList<>();
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(courseFile);
        List<RaceRecord> records = new ArrayList<>();
        
        if (config.contains("records")) {
            for (String key : config.getConfigurationSection("records").getKeys(false)) {
                RaceRecord record = new RaceRecord();
                record.setPlayer(config.getString("records." + key + ".player"));
                record.setCourse(course);
                record.setTime(config.getDouble("records." + key + ".time"));
                record.setDate(LocalDateTime.parse(config.getString("records." + key + ".date"), dateFormatter));
                record.setType(CourseType.valueOf(config.getString("records." + key + ".type")));
                records.add(record);
            }
        }
        
        // Sort by time (best first) and limit results
        records.sort(Comparator.comparingDouble(RaceRecord::getTime));
        if (records.size() > limit) {
            records = records.subList(0, limit);
        }
        
        return records;
    }
    
    @Override
    public List<RaceRecord> getPlayerRecent(String player, int limit) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerRecentFile);
        List<RaceRecord> records = new ArrayList<>();
        
        if (config.contains("players." + player + ".recent")) {
            for (String key : config.getConfigurationSection("players." + player + ".recent").getKeys(false)) {
                RaceRecord record = new RaceRecord();
                record.setPlayer(player);
                record.setCourse(config.getString("players." + player + ".recent." + key + ".course"));
                record.setTime(config.getDouble("players." + player + ".recent." + key + ".time"));
                record.setDate(LocalDateTime.parse(config.getString("players." + player + ".recent." + key + ".date"), dateFormatter));
                record.setType(CourseType.valueOf(config.getString("players." + player + ".recent." + key + ".type")));
                records.add(record);
            }
        }
        
        // Sort by date (most recent first) and limit results
        records.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        if (records.size() > limit) {
            records = records.subList(0, limit);
        }
        
        return records;
    }
    
    @Override
    public List<RaceRecord> getPlayerCourseTimes(String player, String course) {
        // This would require a more complex query across all course files
        // For now, return empty list - can be implemented later if needed
        return new ArrayList<>();
    }
    
    @Override
    public RaceRecord getPlayerBestTime(String player, String course) {
        List<RaceRecord> topTimes = getTopTimes(course, 5);
        return topTimes.stream()
                .filter(record -> record.getPlayer().equals(player))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public int getPlayerTotalRaces(String player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
        return config.getInt("players." + player + ".totalRaces", 0);
    }
    
    @Override
    public int getPlayerRacesByType(String player, CourseType type) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerStatsFile);
        if (type == CourseType.SINGLEPLAYER) {
            return config.getInt("players." + player + ".singleplayerRaces", 0);
        } else if (type == CourseType.MULTIPLAYER) {
            return config.getInt("players." + player + ".multiplayerRaces", 0);
        }
        return 0;
    }
    
    @Override
    public String getPlayerFavoriteCourse(String player) {
        // This would require analyzing all course files
        // For now, return null - can be implemented later if needed
        return null;
    }
}
