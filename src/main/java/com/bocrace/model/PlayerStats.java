package com.bocrace.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents player statistics for races
 */
public class PlayerStats {
    private final UUID playerId;
    private final Map<String, CourseStats> courseStats;
    private final List<RaceRecord> raceHistory;
    private final long firstPlayed;
    private long lastPlayed;
    
    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.courseStats = new ConcurrentHashMap<>();
        this.raceHistory = new ArrayList<>();
        this.firstPlayed = System.currentTimeMillis();
        this.lastPlayed = firstPlayed;
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public Map<String, CourseStats> getCourseStats() { return courseStats; }
    public List<RaceRecord> getRaceHistory() { return raceHistory; }
    public long getFirstPlayed() { return firstPlayed; }
    public long getLastPlayed() { return lastPlayed; }
    
    // Setters
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }
    
    /**
     * Get or create course stats for a specific course
     */
    public CourseStats getCourseStats(String courseName) {
        return courseStats.computeIfAbsent(courseName, CourseStats::new);
    }
    
    /**
     * Add a race record to history
     */
    public void addRaceRecord(RaceRecord record) {
        raceHistory.add(record);
        setLastPlayed(System.currentTimeMillis());
        
        // Keep only last 100 records to prevent memory issues
        if (raceHistory.size() > 100) {
            raceHistory.remove(0);
        }
    }
    
    /**
     * Get the most recent race record for a course
     */
    public Optional<RaceRecord> getMostRecentRace(String courseName) {
        return raceHistory.stream()
                .filter(record -> record.getCourseName().equals(courseName))
                .max(Comparator.comparingLong(RaceRecord::getTimestamp));
    }
    
    /**
     * Get all race records for a course
     */
    public List<RaceRecord> getRaceRecords(String courseName) {
        return raceHistory.stream()
                .filter(record -> record.getCourseName().equals(courseName))
                .sorted(Comparator.comparingLong(RaceRecord::getTimestamp).reversed())
                .toList();
    }
    
    /**
     * Get total races played across all courses
     */
    public int getTotalRaces() {
        return raceHistory.size();
    }
    
    /**
     * Get total wins across all courses
     */
    public int getTotalWins() {
        return (int) raceHistory.stream()
                .filter(record -> record.getPosition() == 1)
                .count();
    }
    
    /**
     * Course-specific statistics
     */
    public static class CourseStats {
        private final String courseName;
        private long bestTime;
        private int totalRaces;
        private int wins;
        private int attempts;
        private long totalTime;
        private long lastRaceTime;
        
        public CourseStats(String courseName) {
            this.courseName = courseName;
            this.bestTime = 0;
            this.totalRaces = 0;
            this.wins = 0;
            this.attempts = 0;
            this.totalTime = 0;
            this.lastRaceTime = 0;
        }
        
        // Getters
        public String getCourseName() { return courseName; }
        public long getBestTime() { return bestTime; }
        public int getTotalRaces() { return totalRaces; }
        public int getWins() { return wins; }
        public int getAttempts() { return attempts; }
        public long getTotalTime() { return totalTime; }
        public long getLastRaceTime() { return lastRaceTime; }
        
        // Setters
        public void setBestTime(long bestTime) { this.bestTime = bestTime; }
        public void setTotalRaces(int totalRaces) { this.totalRaces = totalRaces; }
        public void setWins(int wins) { this.wins = wins; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        public void setTotalTime(long totalTime) { this.totalTime = totalTime; }
        public void setLastRaceTime(long lastRaceTime) { this.lastRaceTime = lastRaceTime; }
        
        /**
         * Update stats with a new race result
         */
        public void updateStats(long raceTime, int position, boolean finished) {
            attempts++;
            if (finished) {
                totalRaces++;
                totalTime += raceTime;
                lastRaceTime = raceTime;
                
                if (bestTime == 0 || raceTime < bestTime) {
                    bestTime = raceTime;
                }
                
                if (position == 1) {
                    wins++;
                }
            }
        }
        
        /**
         * Get average race time
         */
        public double getAverageTime() {
            return totalRaces > 0 ? (double) totalTime / totalRaces : 0;
        }
        
        /**
         * Get win percentage
         */
        public double getWinPercentage() {
            return totalRaces > 0 ? (double) wins / totalRaces * 100 : 0;
        }
        
        /**
         * Get formatted best time
         */
        public String getFormattedBestTime() {
            if (bestTime == 0) return "N/A";
            double seconds = bestTime / 1000.0;
            int minutes = (int) (seconds / 60);
            seconds = seconds % 60;
            return String.format("%d:%05.2f", minutes, seconds);
        }
        
        /**
         * Get formatted last race time
         */
        public String getFormattedLastRaceTime() {
            if (lastRaceTime == 0) return "N/A";
            double seconds = lastRaceTime / 1000.0;
            int minutes = (int) (seconds / 60);
            seconds = seconds % 60;
            return String.format("%d:%05.2f", minutes, seconds);
        }
    }
    
    /**
     * Represents a single race record
     */
    public static class RaceRecord {
        private final String raceId;
        private final String courseName;
        private final RaceType raceType;
        private final long timestamp;
        private final long raceTime;
        private final int position;
        private final boolean finished;
        private final String sessionId;
        
        public RaceRecord(String raceId, String courseName, RaceType raceType, long raceTime, 
                         int position, boolean finished, String sessionId) {
            this.raceId = raceId;
            this.courseName = courseName;
            this.raceType = raceType;
            this.timestamp = System.currentTimeMillis();
            this.raceTime = raceTime;
            this.position = position;
            this.finished = finished;
            this.sessionId = sessionId;
        }
        
        // Getters
        public String getRaceId() { return raceId; }
        public String getCourseName() { return courseName; }
        public RaceType getRaceType() { return raceType; }
        public long getTimestamp() { return timestamp; }
        public long getRaceTime() { return raceTime; }
        public int getPosition() { return position; }
        public boolean isFinished() { return finished; }
        public String getSessionId() { return sessionId; }
        
        /**
         * Get formatted race time
         */
        public String getFormattedRaceTime() {
            if (raceTime == 0) return "N/A";
            double seconds = raceTime / 1000.0;
            int minutes = (int) (seconds / 60);
            seconds = seconds % 60;
            return String.format("%d:%05.2f", minutes, seconds);
        }
        
        /**
         * Get formatted date
         */
        public String getFormattedDate() {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(timestamp));
        }
        
        public enum RaceType {
            SINGLEPLAYER,
            MULTIPLAYER
        }
    }
}
