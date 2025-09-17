package com.bocrace.race;

import com.bocrace.model.CourseType;
import org.bukkit.Location;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an active race session for a single player
 */
public class ActiveRace {
    
    public enum State {
        ARMED,      // Player in boat, ready to cross start line
        RUNNING,    // Timer active, race in progress
        FINISHED,   // Race completed successfully
        DQ          // Disqualified (exited boat, disconnected, etc.)
    }
    
    private final UUID playerUuid;
    private final String playerName;
    private final String courseName;
    private final CourseType courseType;
    private final LocalDateTime startTime;
    
    // Safety: Save player's location before race starts
    private final Location preRaceLocation;
    
    private UUID boatUuid;
    private State state;
    private long startNanoTime;
    private long endNanoTime;
    private String dqReason;
    private long lastDisplayedSecond = -1; // For chat timer display
    private String startButtonType; // Track which button was used to start race
    
    public ActiveRace(UUID playerUuid, String playerName, String courseName, CourseType courseType, Location preRaceLocation) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.courseName = courseName;
        this.courseType = courseType;
        this.preRaceLocation = preRaceLocation.clone(); // Clone to prevent modification
        this.startTime = LocalDateTime.now();
        this.state = State.ARMED;
    }
    
    // Getters
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getCourseName() { return courseName; }
    public CourseType getCourseType() { return courseType; }
    public LocalDateTime getStartTime() { return startTime; }
    public Location getPreRaceLocation() { return preRaceLocation; }
    public UUID getBoatUuid() { return boatUuid; }
    public State getState() { return state; }
    public long getStartNanoTime() { return startNanoTime; }
    public long getEndNanoTime() { return endNanoTime; }
    public String getDqReason() { return dqReason; }
    public long getLastDisplayedSecond() { return lastDisplayedSecond; }
    public String getStartButtonType() { return startButtonType; }
    
    // Setters
    public void setBoatUuid(UUID boatUuid) { this.boatUuid = boatUuid; }
    public void setState(State state) { this.state = state; }
    public void setStartNanoTime(long startNanoTime) { this.startNanoTime = startNanoTime; }
    public void setEndNanoTime(long endNanoTime) { this.endNanoTime = endNanoTime; }
    public void setDqReason(String dqReason) { this.dqReason = dqReason; }
    public void setLastDisplayedSecond(long lastDisplayedSecond) { this.lastDisplayedSecond = lastDisplayedSecond; }
    public void setStartButtonType(String startButtonType) { this.startButtonType = startButtonType; }
    
    /**
     * Get the current race duration in milliseconds
     * @return duration in milliseconds, or 0 if race hasn't started
     */
    public long getCurrentDurationMs() {
        if (startNanoTime == 0) return 0;
        long endTime = (state == State.RUNNING) ? System.nanoTime() : endNanoTime;
        return (endTime - startNanoTime) / 1_000_000L;
    }
    
    /**
     * Get the final race time in milliseconds
     * @return final time in milliseconds, or 0 if race not finished
     */
    public long getFinalTimeMs() {
        if (state != State.FINISHED || endNanoTime == 0) return 0;
        return (endNanoTime - startNanoTime) / 1_000_000L;
    }
    
    /**
     * Format the current duration for display
     * @return formatted time string like "Race Time: 00:15.342"
     */
    public String getFormattedCurrentTime() {
        long ms = getCurrentDurationMs();
        return formatTime(ms);
    }
    
    /**
     * Format the final time for display
     * @return formatted time string like "Race Time: 00:15.342"
     */
    public String getFormattedFinalTime() {
        long ms = getFinalTimeMs();
        return formatTime(ms);
    }
    
    /**
     * Format a DQ message with timestamp
     * @return formatted DQ message like "DQ at 00:23.1 - Disconnected"
     */
    public String getFormattedDqMessage() {
        if (state != State.DQ || dqReason == null) return "";
        long ms = getCurrentDurationMs();
        return String.format("DQ at %s - %s", formatTimeShort(ms), dqReason);
    }
    
    private String formatTime(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        long millis = milliseconds % 1000;
        return String.format("Race Time: %02d:%02d.%03d", minutes, seconds, millis);
    }
    
    private String formatTimeShort(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        long millis = milliseconds % 1000;
        return String.format("%02d:%02d.%01d", minutes, seconds, millis / 100);
    }
    
    @Override
    public String toString() {
        return String.format("ActiveRace{player=%s, course=%s, state=%s, duration=%dms}", 
                playerName, courseName, state, getCurrentDurationMs());
    }
}
