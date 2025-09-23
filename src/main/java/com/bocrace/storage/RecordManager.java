package com.bocrace.storage;

import com.bocrace.model.RaceRecord;
import com.bocrace.model.CourseType;
import com.bocrace.model.Period;
import java.util.List;

/**
 * Interface for managing race records and statistics
 * Supports both YAML and future MySQL implementations
 */
public interface RecordManager {
    
    /**
     * Save a race record
     * @param player Player name
     * @param course Course name
     * @param time Race time in seconds
     * @param type Course type (SINGLEPLAYER/MULTIPLAYER)
     */
    void saveRaceRecord(String player, String course, double time, CourseType type);
    
    /**
     * Get top times for a specific course
     * @param course Course name
     * @param limit Maximum number of records to return
     * @return List of race records sorted by time (best first)
     */
    List<RaceRecord> getTopTimes(String course, int limit);
    
    /**
     * Get top times for a specific course within a time period
     * @param course Course name
     * @param period Time period (DAILY, WEEKLY, MONTHLY)
     * @param limit Maximum number of records to return
     * @return List of race records sorted by time (best first) within the period
     */
    List<RaceRecord> getTopTimesForPeriod(String course, Period period, int limit);
    
    /**
     * Get player's recent races across all courses
     * @param player Player name
     * @param limit Maximum number of records to return
     * @return List of race records sorted by date (most recent first)
     */
    List<RaceRecord> getPlayerRecent(String player, int limit);
    
    /**
     * Get player's times for a specific course
     * @param player Player name
     * @param course Course name
     * @return List of race records sorted by time (best first)
     */
    List<RaceRecord> getPlayerCourseTimes(String player, String course);
    
    /**
     * Get player's best time for a specific course
     * @param player Player name
     * @param course Course name
     * @return Best race record or null if no records
     */
    RaceRecord getPlayerBestTime(String player, String course);
    
    /**
     * Get player's total race count
     * @param player Player name
     * @return Total number of races completed
     */
    int getPlayerTotalRaces(String player);
    
    /**
     * Get player's race count by type
     * @param player Player name
     * @param type Course type
     * @return Number of races of specified type
     */
    int getPlayerRacesByType(String player, CourseType type);
    
    /**
     * Get player's favorite course (most races)
     * @param player Player name
     * @return Course name with most races, or null if no races
     */
    String getPlayerFavoriteCourse(String player);
    
    /**
     * Reset all race records for a specific course
     * @param courseName Course name to reset
     * @return true if successful, false otherwise
     */
    boolean resetCourseRecords(String courseName);
    
    /**
     * Reset all race records for a specific player
     * @param playerName Player name to reset
     * @return true if successful, false otherwise
     */
    boolean resetPlayerRecords(String playerName);
    
    /**
     * Reset ALL race records globally
     * @return true if successful, false otherwise
     */
    boolean resetAllRecords();
}
