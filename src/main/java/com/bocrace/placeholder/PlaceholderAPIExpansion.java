package com.bocrace.placeholder;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.PlayerStats;
import com.bocrace.util.MessageUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for BOCRacePlugin
 */
public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    private final BOCRacePlugin plugin;
    
    public PlaceholderAPIExpansion(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "bocrace";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "AbnVet";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        String[] args = params.split("_");
        if (args.length < 2) return "";
        
        String type = args[0].toLowerCase();
        String courseName = args[1];
        
        Course course = plugin.getStorageManager().getCourse(courseName);
        if (course == null) return "Course not found";
        
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(player.getUniqueId());
        if (stats == null) return "No stats found";
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        
        switch (type) {
            case "besttime":
                return courseStats.getFormattedBestTime();
                
            case "lasttime":
                return courseStats.getFormattedLastRaceTime();
                
            case "attempts":
                return String.valueOf(courseStats.getAttempts());
                
            case "races":
                return String.valueOf(courseStats.getTotalRaces());
                
            case "wins":
                return String.valueOf(courseStats.getWins());
                
            case "winrate":
                return String.format("%.1f%%", courseStats.getWinPercentage());
                
            case "averagetime":
                return MessageUtil.formatTime(courseStats.getAverageTime());
                
            case "position":
                return getPlayerPosition(player.getUniqueId(), courseName);
                
            case "leaderboard":
                return getLeaderboardEntry(args);
                
            case "recent":
                return getRecentRace(player.getUniqueId(), courseName);
                
            default:
                return "Invalid placeholder";
        }
    }
    
    /**
     * Get player's position on leaderboard
     */
    private String getPlayerPosition(UUID playerId, String courseName) {
        List<PlayerStats.CourseStats> leaderboard = getCourseLeaderboard(courseName);
        
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getCourseName().equals(courseName)) {
                return String.valueOf(i + 1);
            }
        }
        
        return "N/A";
    }
    
    /**
     * Get leaderboard entry
     */
    private String getLeaderboardEntry(String[] args) {
        if (args.length < 3) return "Invalid format";
        
        String courseName = args[1];
        int position;
        
        try {
            position = Integer.parseInt(args[2]) - 1; // Convert to 0-based index
        } catch (NumberFormatException e) {
            return "Invalid position";
        }
        
        List<PlayerStats.CourseStats> leaderboard = getCourseLeaderboard(courseName);
        
        if (position < 0 || position >= leaderboard.size()) {
            return "N/A";
        }
        
        PlayerStats.CourseStats stats = leaderboard.get(position);
        return stats.getFormattedBestTime();
    }
    
    /**
     * Get recent race result
     */
    private String getRecentRace(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return "N/A";
        
        return stats.getMostRecentRace(courseName)
                .map(record -> record.getFormattedRaceTime())
                .orElse("N/A");
    }
    
    /**
     * Get course leaderboard
     */
    private List<PlayerStats.CourseStats> getCourseLeaderboard(String courseName) {
        return plugin.getStorageManager().getPlayerStats().values().stream()
                .map(stats -> stats.getCourseStats(courseName))
                .filter(courseStats -> courseStats.getBestTime() > 0)
                .sorted(Comparator.comparingLong(PlayerStats.CourseStats::getBestTime))
                .limit(10)
                .toList();
    }
    
    /**
     * Get top 10 leaderboard for a course
     */
    public List<PlayerStats.CourseStats> getTop10Leaderboard(String courseName) {
        return getCourseLeaderboard(courseName);
    }
    
    /**
     * Get player's best time for a course
     */
    public String getPlayerBestTime(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return "N/A";
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getFormattedBestTime();
    }
    
    /**
     * Get player's last race time for a course
     */
    public String getPlayerLastTime(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return "N/A";
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getFormattedLastRaceTime();
    }
    
    /**
     * Get player's total attempts for a course
     */
    public int getPlayerAttempts(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return 0;
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getAttempts();
    }
    
    /**
     * Get player's total races for a course
     */
    public int getPlayerRaces(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return 0;
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getTotalRaces();
    }
    
    /**
     * Get player's wins for a course
     */
    public int getPlayerWins(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return 0;
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getWins();
    }
    
    /**
     * Get player's win rate for a course
     */
    public double getPlayerWinRate(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return 0.0;
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getWinPercentage();
    }
    
    /**
     * Get player's average time for a course
     */
    public double getPlayerAverageTime(UUID playerId, String courseName) {
        PlayerStats stats = plugin.getStorageManager().getPlayerStats(playerId);
        if (stats == null) return 0.0;
        
        PlayerStats.CourseStats courseStats = stats.getCourseStats(courseName);
        return courseStats.getAverageTime();
    }
    
    /**
     * Get player's position on leaderboard for a course
     */
    public int getPlayerLeaderboardPosition(UUID playerId, String courseName) {
        List<PlayerStats.CourseStats> leaderboard = getCourseLeaderboard(courseName);
        
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getCourseName().equals(courseName)) {
                return i + 1;
            }
        }
        
        return -1; // Not on leaderboard
    }
}
