package com.bocrace.race;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active races and course occupancy
 */
public class RaceManager {
    
    private final BOCRacePlugin plugin;
    
    // Track active races by player UUID
    private final Map<UUID, ActiveRace> activeRaces = new ConcurrentHashMap<>();
    
    // Track course occupancy by course name
    private final Map<String, UUID> courseOccupancy = new ConcurrentHashMap<>();
    
    public RaceManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if a course is currently occupied
     */
    public boolean isCourseOccupied(String courseName) {
        UUID occupant = courseOccupancy.get(courseName);
        if (occupant == null) return false;
        
        // Check if the occupying player still has an active race
        ActiveRace race = activeRaces.get(occupant);
        if (race == null || race.getState() == ActiveRace.State.FINISHED || race.getState() == ActiveRace.State.DQ) {
            // Clean up stale occupancy
            courseOccupancy.remove(courseName);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the player currently occupying a course
     */
    public UUID getCourseOccupant(String courseName) {
        if (!isCourseOccupied(courseName)) return null;
        return courseOccupancy.get(courseName);
    }
    
    /**
     * Check if a player currently has an active race
     */
    public boolean hasActiveRace(UUID playerUuid) {
        ActiveRace race = activeRaces.get(playerUuid);
        return race != null && (race.getState() == ActiveRace.State.ARMED || race.getState() == ActiveRace.State.RUNNING);
    }
    
    /**
     * Get a player's current active race
     */
    public ActiveRace getActiveRace(UUID playerUuid) {
        return activeRaces.get(playerUuid);
    }
    
    /**
     * Start a new race for a player
     */
    public ActiveRace startRace(Player player, Course course) {
        plugin.debugLog("Starting race for " + player.getName() + " on course " + course.getName());
        
        // Check if player already has an active race
        if (hasActiveRace(player.getUniqueId())) {
            plugin.debugLog("Player " + player.getName() + " already has an active race");
            return null;
        }
        
        // Check if course is occupied
        if (isCourseOccupied(course.getName())) {
            plugin.debugLog("Course " + course.getName() + " is already occupied");
            return null;
        }
        
        // Validate course has required components
        if (!isRaceReady(course)) {
            plugin.debugLog("Course " + course.getName() + " is not ready for racing");
            return null;
        }
        
        // Create new active race with safety location
        Location safeLocation = player.getLocation().clone();
        ActiveRace race = new ActiveRace(player.getUniqueId(), player.getName(), course.getName(), course.getType(), safeLocation);
        
        // Register the race
        activeRaces.put(player.getUniqueId(), race);
        courseOccupancy.put(course.getName(), player.getUniqueId());
        
        plugin.debugLog("Race started successfully: " + race);
        return race;
    }
    
    /**
     * End a race (either finished or DQ)
     */
    public void endRace(UUID playerUuid, ActiveRace.State endState, String reason) {
        ActiveRace race = activeRaces.get(playerUuid);
        if (race == null) return;
        
        plugin.debugLog("Ending race for " + race.getPlayerName() + " with state " + endState + 
                       (reason != null ? " - " + reason : ""));
        
        // Update race state
        race.setState(endState);
        race.setEndNanoTime(System.nanoTime());
        
        if (endState == ActiveRace.State.DQ && reason != null) {
            race.setDqReason(reason);
        }
        
        // Free up the course
        courseOccupancy.remove(race.getCourseName());
        
        // Keep the race record for a short time for stats/cleanup, but don't remove immediately
        // It will be cleaned up by the cleanup task or when player starts a new race
        
        plugin.debugLog("Race ended: " + race);
    }
    
    /**
     * Clean up finished/DQ races older than specified time
     */
    public void cleanupOldRaces(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        
        activeRaces.entrySet().removeIf(entry -> {
            ActiveRace race = entry.getValue();
            if (race.getState() == ActiveRace.State.ARMED || race.getState() == ActiveRace.State.RUNNING) {
                return false; // Keep active races
            }
            
            long raceAge = currentTime - race.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            return raceAge > maxAgeMs;
        });
    }
    
    /**
     * Force cleanup a player's race (for disconnects, etc.)
     */
    public void forceCleanupRace(UUID playerUuid, String reason) {
        ActiveRace race = activeRaces.get(playerUuid);
        if (race == null) return;
        
        if (race.getState() == ActiveRace.State.ARMED || race.getState() == ActiveRace.State.RUNNING) {
            endRace(playerUuid, ActiveRace.State.DQ, reason);
        }
        
        // Remove immediately for force cleanup
        activeRaces.remove(playerUuid);
    }
    
    /**
     * Check if a course is ready for racing (has all required components)
     */
    public boolean isRaceReady(Course course) {
        if (course.getType() != CourseType.SINGLEPLAYER) {
            return false; // Only singleplayer supported for now
        }
        
        // Required components for singleplayer racing
        boolean hasStartButton = course.getSpstartbutton() != null;
        boolean hasBoatSpawn = course.getSpboatspawn() != null;
        boolean hasStartLine = course.getSpstart1() != null && course.getSpstart2() != null;
        boolean hasFinishLine = course.getSpfinish1() != null && course.getSpfinish2() != null;
        boolean hasCourseLobby = course.getSpcourselobby() != null; // Required per spec
        
        return hasStartButton && hasBoatSpawn && hasStartLine && hasFinishLine && hasCourseLobby;
    }
    
    /**
     * Get all active races (for debugging/admin purposes)
     */
    public Map<UUID, ActiveRace> getAllActiveRaces() {
        return new ConcurrentHashMap<>(activeRaces);
    }
    
    /**
     * Get all occupied courses (for debugging/admin purposes)
     */
    public Map<String, UUID> getOccupiedCourses() {
        return new ConcurrentHashMap<>(courseOccupancy);
    }
    
    /**
     * Get race statistics
     */
    public String getStats() {
        int activeCount = (int) activeRaces.values().stream()
                .filter(race -> race.getState() == ActiveRace.State.ARMED || race.getState() == ActiveRace.State.RUNNING)
                .count();
        
        return String.format("Active races: %d, Occupied courses: %d, Total race records: %d", 
                activeCount, courseOccupancy.size(), activeRaces.size());
    }
}
