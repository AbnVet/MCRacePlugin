package com.bocrace.integration;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.RaceRecord;
import com.bocrace.model.Period;
import com.bocrace.race.ActiveRace;
import com.bocrace.race.MultiplayerRace;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for BOCRacePlugin
 * Provides race-related placeholders for use in other plugins like DecentHolograms
 */
public class BOCRacePlaceholderExpansion extends PlaceholderExpansion {
    
    private final BOCRacePlugin plugin;
    
    public BOCRacePlaceholderExpansion(BOCRacePlugin plugin) {
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
        return true; // Keep expansion loaded even if plugin reloads
    }
    
    @Override
    public boolean canRegister() {
        return true; // Always allow registration
    }
    
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return handleNonPlayerPlaceholders(params);
        }
        
        // Handle player-specific placeholders
        return handlePlayerPlaceholders(player, params);
    }
    
    /**
     * Handle placeholders that don't require a specific player context
     */
    private String handleNonPlayerPlaceholders(String params) {
        // Course status placeholders: course_<name>_status
        if (params.startsWith("course_") && params.endsWith("_status")) {
            String courseName = extractCourseName(params, "course_", "_status");
            return getCourseStatus(courseName);
        }
        
        // Course record placeholders: course_<name>_record
        if (params.startsWith("course_") && params.endsWith("_record")) {
            String courseName = extractCourseName(params, "course_", "_record");
            return getCourseRecord(courseName);
        }
        
        // Course record time placeholders: course_<name>_record_time
        if (params.startsWith("course_") && params.endsWith("_record_time")) {
            String courseName = extractCourseName(params, "course_", "_record_time");
            return getCourseRecordTime(courseName);
        }
        
        // Course usage placeholders: course_<name>_usage
        if (params.startsWith("course_") && params.endsWith("_usage")) {
            String courseName = extractCourseName(params, "course_", "_usage");
            return getCourseUsage(courseName);
        }
        
        // Period-based leaderboard placeholders: leaderboard_<course>_<period>_name_<position> or leaderboard_<course>_<period>_time_<position>
        if (params.startsWith("leaderboard_") && (params.contains("_daily_") || params.contains("_weekly_") || params.contains("_monthly_"))) {
            return handlePeriodLeaderboardPlaceholder(params);
        }
        
        // Leaderboard placeholders: leaderboard_<course>_<position>
        if (params.startsWith("leaderboard_")) {
            return handleLeaderboardPlaceholder(params);
        }
        
        // Global statistics
        switch (params.toLowerCase()) {
            case "total_courses":
                return String.valueOf(plugin.getStorageManager().getAllCourses().size());
            case "active_races":
                return String.valueOf(plugin.getRaceManager().getAllActiveRaces().size());
            case "active_mp_races":
                return String.valueOf(plugin.getMultiplayerRaceManager().getActiveRaceCount());
            default:
                return null; // Placeholder not found
        }
    }
    
    /**
     * Handle placeholders that require a player context
     */
    private String handlePlayerPlaceholders(Player player, String params) {
        UUID playerUuid = player.getUniqueId();
        
        // Player status placeholders
        switch (params.toLowerCase()) {
            case "player_status":
                return getPlayerStatus(playerUuid);
            case "player_current_time":
                return getPlayerCurrentTime(playerUuid);
            case "player_course":
                return getPlayerCurrentCourse(playerUuid);
            case "player_position":
                return getPlayerPosition(playerUuid);
            case "player_races_completed":
                return getPlayerRacesCompleted(playerUuid);
        }
        
        // Personal best placeholders: player_pb_<course>
        if (params.startsWith("player_pb_")) {
            String courseName = params.substring("player_pb_".length());
            return getPlayerPersonalBest(playerUuid, courseName);
        }
        
        // Multiplayer race placeholders
        if (params.startsWith("mp_")) {
            return handleMultiplayerPlaceholders(playerUuid, params);
        }
        
        // Course status placeholders: course_<name>_status (also handle in player context)
        if (params.startsWith("course_") && params.endsWith("_status")) {
            String courseName = extractCourseName(params, "course_", "_status");
            return getCourseStatus(courseName);
        }
        
        // Course record placeholders: course_<name>_record (also handle in player context)
        if (params.startsWith("course_") && params.endsWith("_record")) {
            String courseName = extractCourseName(params, "course_", "_record");
            return getCourseRecord(courseName);
        }
        
        // Course record time placeholders: course_<name>_record_time (also handle in player context)
        if (params.startsWith("course_") && params.endsWith("_record_time")) {
            String courseName = extractCourseName(params, "course_", "_record_time");
            return getCourseRecordTime(courseName);
        }
        
        // Course usage placeholders: course_<name>_usage (also handle in player context)
        if (params.startsWith("course_") && params.endsWith("_usage")) {
            String courseName = extractCourseName(params, "course_", "_usage");
            return getCourseUsage(courseName);
        }
        
        // Period-based leaderboard placeholders: leaderboard_<course>_<period>_name_<position> or leaderboard_<course>_<period>_time_<position> (also handle in player context)
        if (params.startsWith("leaderboard_") && (params.contains("_daily_") || params.contains("_weekly_") || params.contains("_monthly_"))) {
            return handlePeriodLeaderboardPlaceholder(params);
        }
        
        // Leaderboard placeholders: leaderboard_<course>_<position> (also handle in player context)
        if (params.startsWith("leaderboard_")) {
            return handleLeaderboardPlaceholder(params);
        }
        
        // DQ-related placeholders
        switch (params.toLowerCase()) {
            case "player_last_race_status":
                return getPlayerLastRaceStatus(playerUuid);
            case "player_dq_count":
                return getPlayerDQCount(playerUuid);
            case "player_completion_rate":
                return getPlayerCompletionRate(playerUuid);
            case "player_last_dq_reason":
                return getPlayerLastDQReason(playerUuid);
        }
        
        // Course DQ rate placeholders: course_<name>_dq_rate
        if (params.startsWith("course_") && params.endsWith("_dq_rate")) {
            String courseName = extractCourseName(params, "course_", "_dq_rate");
            return getCourseDQRate(courseName);
        }
        
        // Global statistics (also handle in player context)
        switch (params.toLowerCase()) {
            case "total_courses":
                return String.valueOf(plugin.getStorageManager().getAllCourses().size());
            case "active_races":
                return String.valueOf(plugin.getRaceManager().getAllActiveRaces().size());
            case "active_mp_races":
                return String.valueOf(plugin.getMultiplayerRaceManager().getActiveRaceCount());
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Handle multiplayer-specific placeholders
     */
    private String handleMultiplayerPlaceholders(UUID playerUuid, String params) {
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        
        switch (params.toLowerCase()) {
            case "mp_players_joined":
                return mpRace != null ? String.valueOf(mpRace.getPlayerCount()) : "0";
            case "mp_race_status":
                return mpRace != null ? mpRace.getState().toString() : "Not in race";
            case "mp_time_remaining":
                // Calculate time remaining until race timeout
                if (mpRace != null) {
                    long elapsed = System.currentTimeMillis() - mpRace.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long remaining = mpRace.getTimeoutMs() - elapsed;
                    return remaining > 0 ? formatTimeRemaining(remaining) : "Expired";
                }
                return "N/A";
            case "mp_leader":
                // Find current leader (player with best time so far)
                if (mpRace != null && mpRace.getState() == MultiplayerRace.State.RUNNING) {
                    String leaderName = findCurrentLeader(mpRace);
                    return leaderName != null ? leaderName : "N/A";
                }
                return "N/A";
            case "mp_leader_time":
                // Get leader's current best time
                if (mpRace != null && mpRace.getState() == MultiplayerRace.State.RUNNING) {
                    long leaderTime = findCurrentLeaderTime(mpRace);
                    return leaderTime > 0 ? formatTime(leaderTime) : "N/A";
                }
                return "N/A";
            default:
                return null;
        }
    }
    
    // Helper methods for extracting course names from placeholders
    private String extractCourseName(String params, String prefix, String suffix) {
        if (params.startsWith(prefix) && params.endsWith(suffix)) {
            return params.substring(prefix.length(), params.length() - suffix.length());
        }
        return null;
    }
    
    // Course status methods
    private String getCourseStatus(String courseName) {
        Course course = plugin.getStorageManager().getCourse(courseName);
        if (course == null) return "Not Found";
        
        // Priority 1: Manual override - course marked as closed
        if (course.isManuallyClosed()) {
            return "§4Closed";
        }
        
        // Priority 2: Course setup is incomplete
        if (!plugin.getRaceManager().isRaceReady(course)) {
            return "§4Closed";
        }
        
        // Priority 3: Someone is actively racing
        if (plugin.getRaceManager().isCourseOccupied(courseName)) {
            return "§5In Use";
        }
        
        // Priority 4: Someone is in setup mode for this course
        boolean inSetup = plugin.getPlayerSetupModes().values().stream()
            .anyMatch(mode -> mode.getCourseName().equals(courseName));
        
        if (inSetup) {
            return "§4Setup";
        }
        
        // Priority 5: Course is ready and available
        return "§2Open";
    }
    
    private String getCourseRecord(String courseName) {
        try {
            List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes(courseName, 1);
            return !topTimes.isEmpty() ? topTimes.get(0).getPlayer() : "No Record";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    private String getCourseRecordTime(String courseName) {
        try {
            List<RaceRecord> topTimes = plugin.getRecordManager().getTopTimes(courseName, 1);
            return !topTimes.isEmpty() ? formatTime((long)(topTimes.get(0).getTime() * 1000)) : "No Record";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    private String getCourseUsage(String courseName) {
        Course course = plugin.getStorageManager().getCourse(courseName);
        return course != null ? String.valueOf(course.getUsageCount()) : "0";
    }
    
    // Player status methods
    private String getPlayerStatus(UUID playerUuid) {
        // Check singleplayer race first
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null) {
            return "Racing (" + race.getState() + ")";
        }
        
        // Check multiplayer race
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        if (mpRace != null) {
            return "MP Racing (" + mpRace.getState() + ")";
        }
        
        return "Not Racing";
    }
    
    private String getPlayerCurrentTime(UUID playerUuid) {
        // Check singleplayer race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null && race.getState() == ActiveRace.State.RUNNING) {
            return formatTime(race.getCurrentDurationMs());
        }
        
        // Check multiplayer race
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        if (mpRace != null && mpRace.getState() == MultiplayerRace.State.RUNNING) {
            // Get player result and calculate current time if timer started
            MultiplayerRace.PlayerResult result = mpRace.getPlayers().get(playerUuid);
            if (result != null && result.isTimerStarted() && !result.isFinished()) {
                long currentTime = System.currentTimeMillis() - result.getStartTimeMs();
                return formatTime(currentTime);
            }
        }
        
        return "N/A";
    }
    
    private String getPlayerCurrentCourse(UUID playerUuid) {
        // Check singleplayer race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null) {
            return race.getCourseName();
        }
        
        // Check multiplayer race
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        if (mpRace != null) {
            return mpRace.getCourse().getName();
        }
        
        return "None";
    }
    
    private String getPlayerPosition(UUID playerUuid) {
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        if (mpRace != null && mpRace.getState() == MultiplayerRace.State.RUNNING) {
            MultiplayerRace.PlayerResult result = mpRace.getPlayers().get(playerUuid);
            if (result != null && result.getPlacement() > 0) {
                return formatPosition(result.getPlacement());
            }
        }
        return "N/A";
    }
    
    private String getPlayerRacesCompleted(UUID playerUuid) {
        try {
            // Get player name from UUID
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player != null) {
                return String.valueOf(plugin.getRecordManager().getPlayerTotalRaces(player.getName()));
            }
            return "0";
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getPlayerPersonalBest(UUID playerUuid, String courseName) {
        try {
            // Get player name from UUID
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player != null) {
                RaceRecord pb = plugin.getRecordManager().getPlayerBestTime(player.getName(), courseName);
                return pb != null ? formatTime((long)(pb.getTime() * 1000)) : "No PB";
            }
            return "No PB";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    // Leaderboard handling
    private String handleLeaderboardPlaceholder(String params) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Leaderboard placeholder requested: " + params);
        }
        
        // Check for new format: leaderboard_<course>_name_<position> or leaderboard_<course>_time_<position>
        if (params.contains("_name_") || params.contains("_time_")) {
            return handleNewLeaderboardFormat(params);
        }
        
        // Original format: leaderboard_<course>_<position> (backward compatibility)
        return handleOriginalLeaderboardFormat(params);
    }
    
    private String handleNewLeaderboardFormat(String params) {
        // Format: leaderboard_<course>_name_<position> or leaderboard_<course>_time_<position>
        String[] parts = params.split("_");
        if (parts.length >= 4) {
            String position = parts[parts.length - 1];
            String type = parts[parts.length - 2]; // "name" or "time"
            String courseName = String.join("_", java.util.Arrays.copyOfRange(parts, 1, parts.length - 2));
            
            try {
                int pos = Integer.parseInt(position);
                List<RaceRecord> leaderboard = plugin.getRecordManager().getTopTimes(courseName, 10);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Parsed leaderboard request - Course: " + courseName + ", Type: " + type + ", Position: " + pos);
                }
                
                if (pos > 0 && pos <= leaderboard.size()) {
                    RaceRecord record = leaderboard.get(pos - 1);
                    
                    if ("name".equals(type)) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[DEBUG] Returning player name: " + record.getPlayer());
                        }
                        return record.getPlayer();
                    } else if ("time".equals(type)) {
                        String formattedTime = formatTime((long)(record.getTime() * 1000));
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[DEBUG] Returning formatted time: " + formattedTime);
                        }
                        return formattedTime;
                    }
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] No entry found for position " + pos);
                }
                return "N/A";
            } catch (NumberFormatException e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("[DEBUG] Invalid position in leaderboard placeholder: " + position);
                }
                return "Invalid Position";
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("[DEBUG] Error processing leaderboard placeholder: " + e.getMessage());
                }
                return "Error";
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[DEBUG] Invalid leaderboard placeholder format: " + params);
        }
        return "Invalid Format";
    }
    
    private String handleOriginalLeaderboardFormat(String params) {
        // Format: leaderboard_<course>_<position> (backward compatibility)
        String[] parts = params.split("_");
        if (parts.length >= 3) {
            String position = parts[parts.length - 1];
            String courseName = String.join("_", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
            
            try {
                int pos = Integer.parseInt(position);
                List<RaceRecord> leaderboard = plugin.getRecordManager().getTopTimes(courseName, 10);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Original format leaderboard request - Course: " + courseName + ", Position: " + pos);
                }
                
                if (pos > 0 && pos <= leaderboard.size()) {
                    RaceRecord record = leaderboard.get(pos - 1);
                    String result = record.getPlayer() + " - " + formatTime((long)(record.getTime() * 1000));
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] Returning combined result: " + result);
                    }
                    return result;
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] No entry found for position " + pos);
                }
                return "N/A";
            } catch (NumberFormatException e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("[DEBUG] Invalid position in original leaderboard placeholder: " + position);
                }
                return "Invalid Position";
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("[DEBUG] Error processing original leaderboard placeholder: " + e.getMessage());
                }
                return "Error";
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[DEBUG] Invalid original leaderboard placeholder format: " + params);
        }
        return "Invalid Format";
    }
    
    /**
     * Handle period-based leaderboard placeholders (daily/weekly/monthly)
     * Format: leaderboard_<course>_<period>_name_<position> or leaderboard_<course>_<period>_time_<position>
     */
    private String handlePeriodLeaderboardPlaceholder(String params) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Period leaderboard placeholder requested: " + params);
        }
        
        String[] parts = params.split("_");
        
        // Handle two formats:
        // Format 1: leaderboard_<course>_<period>_<position> (returns combined name - time)
        // Format 2: leaderboard_<course>_<period>_<type>_<position> (returns name or time)
        
        String position, type, periodStr, courseName;
        
        if (parts.length >= 4) {
            // Try format 1 first: leaderboard_<course>_<period>_<position>
            try {
                position = parts[parts.length - 1];
                periodStr = parts[parts.length - 2];
                courseName = String.join("_", java.util.Arrays.copyOfRange(parts, 1, parts.length - 2));
                
                // Validate period
                Period period = Period.valueOf(periodStr.toUpperCase());
                int pos = Integer.parseInt(position);
                
                List<RaceRecord> leaderboard = plugin.getRecordManager().getTopTimesForPeriod(courseName, period, 10);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Parsed period leaderboard request (format 1) - Course: " + courseName + 
                        ", Period: " + period + ", Position: " + pos);
                }
                
                if (pos > 0 && pos <= leaderboard.size()) {
                    RaceRecord record = leaderboard.get(pos - 1);
                    String result = record.getPlayer() + " - " + formatTime((long)(record.getTime() * 1000));
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] Returning combined result: " + result);
                    }
                    return result;
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] No entry found for position " + pos + " in period " + period);
                }
                return "N/A";
                
            } catch (IllegalArgumentException e) {
                // Not a valid period, try format 2
                if (parts.length >= 5) {
                    position = parts[parts.length - 1];
                    type = parts[parts.length - 2]; // "name" or "time"
                    periodStr = parts[parts.length - 3]; // "daily", "weekly", or "monthly"
                    courseName = String.join("_", java.util.Arrays.copyOfRange(parts, 1, parts.length - 3));
                    
                    try {
                        int pos = Integer.parseInt(position);
                        Period period = Period.valueOf(periodStr.toUpperCase());
                        
                        List<RaceRecord> leaderboard = plugin.getRecordManager().getTopTimesForPeriod(courseName, period, 10);
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[DEBUG] Parsed period leaderboard request (format 2) - Course: " + courseName + 
                                ", Period: " + period + ", Type: " + type + ", Position: " + pos);
                        }
                        
                        if (pos > 0 && pos <= leaderboard.size()) {
                            RaceRecord record = leaderboard.get(pos - 1);
                            
                            if ("name".equals(type)) {
                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("[DEBUG] Returning player name: " + record.getPlayer());
                                }
                                return record.getPlayer();
                            } else if ("time".equals(type)) {
                                String formattedTime = formatTime((long)(record.getTime() * 1000));
                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("[DEBUG] Returning formatted time: " + formattedTime);
                                }
                                return formattedTime;
                            }
                        }
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[DEBUG] No entry found for position " + pos + " in period " + period);
                        }
                        return "N/A";
                    } catch (NumberFormatException e2) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("[DEBUG] Invalid position in period leaderboard placeholder: " + position);
                        }
                        return "Invalid Position";
                    } catch (IllegalArgumentException e2) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("[DEBUG] Invalid period in leaderboard placeholder: " + periodStr);
                        }
                        return "Invalid Period";
                    } catch (Exception e2) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("[DEBUG] Error processing period leaderboard placeholder (format 2): " + e2.getMessage());
                        }
                        return "Error";
                    }
                } else {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().warning("[DEBUG] Invalid period leaderboard placeholder format: " + params);
                    }
                    return "Invalid Format";
                }
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[DEBUG] Invalid period leaderboard placeholder format: " + params);
        }
        return "Invalid Format";
    }
    
    // DQ-related helper methods
    private String getPlayerLastRaceStatus(UUID playerUuid) {
        try {
            // Get player name from UUID
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player == null) return "Unknown";
            
            // Get player's recent races (most recent first)
            List<RaceRecord> recentRaces = plugin.getRecordManager().getPlayerRecent(player.getName(), 1);
            if (recentRaces.isEmpty()) {
                return "No Races";
            }
            
            RaceRecord lastRace = recentRaces.get(0);
            
            // Check if it was a DQ (negative time or "(DQ)" in player name)
            if (lastRace.getTime() < 0 || lastRace.getPlayer().contains("(DQ)")) {
                // Extract DQ reason from player name
                String playerName = lastRace.getPlayer();
                if (playerName.contains("(DQ - ")) {
                    // New format: "PlayerName (DQ - reason)"
                    String reason = extractDQReason(playerName);
                    return "DQ - " + reason;
                } else if (playerName.contains("(DQ)")) {
                    // Old format: "PlayerName (DQ)" - guess reason from time
                    return "DQ - " + getDQReasonFromTime(Math.abs(lastRace.getTime()));
                } else {
                    return "DQ - Unknown reason";
                }
            } else {
                return "Completed - " + formatTime((long)(lastRace.getTime() * 1000));
            }
        } catch (Exception e) {
            return "Error";
        }
    }
    
    private String getPlayerDQCount(UUID playerUuid) {
        try {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player == null) return "0";
            
            // Get all player races and count DQs
            List<RaceRecord> allRaces = plugin.getRecordManager().getPlayerRecent(player.getName(), Integer.MAX_VALUE);
            int dqCount = 0;
            
            for (RaceRecord race : allRaces) {
                if (race.getTime() < 0 || race.getPlayer().contains("(DQ)")) {
                    dqCount++;
                }
            }
            
            return String.valueOf(dqCount);
        } catch (Exception e) {
            return "0";
        }
    }
    
    private String getPlayerCompletionRate(UUID playerUuid) {
        try {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player == null) return "0%";
            
            // Get all player races
            List<RaceRecord> allRaces = plugin.getRecordManager().getPlayerRecent(player.getName(), Integer.MAX_VALUE);
            if (allRaces.isEmpty()) return "0%";
            
            int totalRaces = allRaces.size();
            int completedRaces = 0;
            
            for (RaceRecord race : allRaces) {
                if (race.getTime() >= 0 && !race.getPlayer().contains("(DQ)")) {
                    completedRaces++;
                }
            }
            
            int completionRate = (int) Math.round((double) completedRaces / totalRaces * 100);
            return completionRate + "%";
        } catch (Exception e) {
            return "0%";
        }
    }
    
    private String getPlayerLastDQReason(UUID playerUuid) {
        try {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerUuid);
            if (player == null) return "N/A";
            
            // Get recent races and find the most recent DQ
            List<RaceRecord> recentRaces = plugin.getRecordManager().getPlayerRecent(player.getName(), 10);
            
            for (RaceRecord race : recentRaces) {
                if (race.getTime() < 0 || race.getPlayer().contains("(DQ)")) {
                    // Extract reason from player name
                    String playerName = race.getPlayer();
                    if (playerName.contains("(DQ - ")) {
                        return extractDQReason(playerName);
                    } else {
                        return getDQReasonFromTime(Math.abs(race.getTime()));
                    }
                }
            }
            
            return "No Recent DQs";
        } catch (Exception e) {
            return "Error";
        }
    }
    
    private String getCourseDQRate(String courseName) {
        try {
            // Get all records for this course
            List<RaceRecord> courseRecords = plugin.getRecordManager().getTopTimes(courseName, Integer.MAX_VALUE);
            if (courseRecords.isEmpty()) return "0%";
            
            int totalRaces = courseRecords.size();
            int dqRaces = 0;
            
            for (RaceRecord race : courseRecords) {
                if (race.getTime() < 0 || race.getPlayer().contains("(DQ)")) {
                    dqRaces++;
                }
            }
            
            int dqRate = (int) Math.round((double) dqRaces / totalRaces * 100);
            return dqRate + "%";
        } catch (Exception e) {
            return "0%";
        }
    }
    
    private String extractDQReason(String playerNameWithDQ) {
        // Extract reason from format: "PlayerName (DQ - reason)"
        int startIndex = playerNameWithDQ.indexOf("(DQ - ");
        if (startIndex != -1) {
            int reasonStart = startIndex + "(DQ - ".length();
            int reasonEnd = playerNameWithDQ.lastIndexOf(")");
            if (reasonEnd > reasonStart) {
                return playerNameWithDQ.substring(reasonStart, reasonEnd);
            }
        }
        return "Unknown";
    }
    
    private String getDQReasonFromTime(double timeSeconds) {
        // Fallback for old DQ records without explicit reasons
        // Make educated guesses based on common DQ times
        if (timeSeconds < 5.0) {
            return "Early exit";
        } else if (timeSeconds > 50.0) {
            return "Timeout/Disconnect";
        } else {
            return "Exited boat";
        }
    }
    
    // Helper methods for multiplayer race leaders
    private String findCurrentLeader(MultiplayerRace mpRace) {
        MultiplayerRace.PlayerResult leader = null;
        int bestPlacement = Integer.MAX_VALUE;
        
        for (MultiplayerRace.PlayerResult result : mpRace.getPlayers().values()) {
            if (!result.isDisqualified() && result.getPlacement() > 0 && result.getPlacement() < bestPlacement) {
                bestPlacement = result.getPlacement();
                leader = result;
            }
        }
        
        return leader != null ? leader.getPlayerName() : null;
    }
    
    private long findCurrentLeaderTime(MultiplayerRace mpRace) {
        MultiplayerRace.PlayerResult leader = null;
        int bestPlacement = Integer.MAX_VALUE;
        
        for (MultiplayerRace.PlayerResult result : mpRace.getPlayers().values()) {
            if (!result.isDisqualified() && result.getPlacement() > 0 && result.getPlacement() < bestPlacement) {
                bestPlacement = result.getPlacement();
                leader = result;
            }
        }
        
        if (leader != null && leader.getRaceTimeMs() != null) {
            return leader.getRaceTimeMs();
        }
        
        return 0;
    }
    
    // Utility methods for formatting
    private String formatTime(long timeMs) {
        if (timeMs <= 0) return "0.00";
        
        long minutes = timeMs / 60000;
        long seconds = (timeMs % 60000) / 1000;
        long milliseconds = timeMs % 1000;
        
        if (minutes > 0) {
            // For times over 1 minute, show MM:SS format (no decimals)
            return String.format("%d:%02d", minutes, seconds);
        } else {
            // For times under 1 minute, show 2 decimal places
            return String.format("%d.%02d", seconds, milliseconds / 10);
        }
    }
    
    private String formatTimeRemaining(long timeMs) {
        if (timeMs <= 0) return "0:00";
        
        long minutes = timeMs / 60000;
        long seconds = (timeMs % 60000) / 1000;
        
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private String formatPosition(int position) {
        switch (position) {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            default: return position + "th";
        }
    }
}
