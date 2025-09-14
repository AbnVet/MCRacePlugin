package com.bocrace.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a race course with all its components
 */
public class Course {
    private final String name;
    private final CourseType type;
    private final World world;
    
    // Lobby locations
    private Location mainLobby;
    private Location courseLobby;
    
    // Race elements
    private List<Location> boatSpawns;
    private BoundingBox startLine;
    private BoundingBox finishLine;
    
    // Interactive elements
    private Location createRaceButton;
    private Location joinRaceButton;
    private Location startRaceButton;
    private List<Location> boatSpawnButtons;
    private Location returnToLobbyButton;
    private Location returnToMainButton;
    
    // Metadata
    private final long createdAt;
    private long lastModified;
    private UUID createdBy;
    
    public Course(String name, CourseType type, World world, UUID createdBy) {
        this.name = name;
        this.type = type;
        this.world = world;
        this.createdBy = createdBy;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = createdAt;
        this.boatSpawns = new ArrayList<>();
        this.boatSpawnButtons = new ArrayList<>();
    }
    
    // Getters
    public String getName() { return name; }
    public CourseType getType() { return type; }
    public World getWorld() { return world; }
    public Location getMainLobby() { return mainLobby; }
    public Location getCourseLobby() { return courseLobby; }
    public List<Location> getBoatSpawns() { return boatSpawns; }
    public BoundingBox getStartLine() { return startLine; }
    public BoundingBox getFinishLine() { return finishLine; }
    public Location getCreateRaceButton() { return createRaceButton; }
    public Location getJoinRaceButton() { return joinRaceButton; }
    public Location getStartRaceButton() { return startRaceButton; }
    public List<Location> getBoatSpawnButtons() { return boatSpawnButtons; }
    public Location getReturnToLobbyButton() { return returnToLobbyButton; }
    public Location getReturnToMainButton() { return returnToMainButton; }
    public long getCreatedAt() { return createdAt; }
    public long getLastModified() { return lastModified; }
    public UUID getCreatedBy() { return createdBy; }
    
    // Setters
    public void setMainLobby(Location mainLobby) { 
        this.mainLobby = mainLobby; 
        updateLastModified();
    }
    
    public void setCourseLobby(Location courseLobby) { 
        this.courseLobby = courseLobby; 
        updateLastModified();
    }
    
    public void setBoatSpawns(List<Location> boatSpawns) { 
        this.boatSpawns = boatSpawns; 
        updateLastModified();
    }
    
    public void setStartLine(BoundingBox startLine) { 
        this.startLine = startLine; 
        updateLastModified();
    }
    
    public void setFinishLine(BoundingBox finishLine) { 
        this.finishLine = finishLine; 
        updateLastModified();
    }
    
    public void setCreateRaceButton(Location createRaceButton) { 
        this.createRaceButton = createRaceButton; 
        updateLastModified();
    }
    
    public void setJoinRaceButton(Location joinRaceButton) { 
        this.joinRaceButton = joinRaceButton; 
        updateLastModified();
    }
    
    public void setStartRaceButton(Location startRaceButton) { 
        this.startRaceButton = startRaceButton; 
        updateLastModified();
    }
    
    public void setBoatSpawnButtons(List<Location> boatSpawnButtons) { 
        this.boatSpawnButtons = boatSpawnButtons; 
        updateLastModified();
    }
    
    public void setReturnToLobbyButton(Location returnToLobbyButton) { 
        this.returnToLobbyButton = returnToLobbyButton; 
        updateLastModified();
    }
    
    public void setReturnToMainButton(Location returnToMainButton) { 
        this.returnToMainButton = returnToMainButton; 
        updateLastModified();
    }
    
    private void updateLastModified() {
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Check if the course is complete and ready for use
     */
    public boolean isComplete() {
        if (type == CourseType.SINGLEPLAYER) {
            return courseLobby != null && 
                   !boatSpawns.isEmpty() && 
                   startLine != null && 
                   finishLine != null && 
                   returnToLobbyButton != null;
        } else {
            return courseLobby != null && 
                   boatSpawns.size() == 10 && 
                   startLine != null && 
                   finishLine != null && 
                   createRaceButton != null && 
                   joinRaceButton != null && 
                   startRaceButton != null && 
                   boatSpawnButtons.size() == 10 && 
                   returnToLobbyButton != null && 
                   returnToMainButton != null;
        }
    }
    
    /**
     * Get list of missing required elements
     */
    public List<String> getMissingElements() {
        List<String> missing = new ArrayList<>();
        
        if (courseLobby == null) missing.add("Course Lobby");
        if (boatSpawns.isEmpty()) missing.add("Boat Spawn");
        if (startLine == null) missing.add("Start Line");
        if (finishLine == null) missing.add("Finish Line");
        if (returnToLobbyButton == null) missing.add("Return to Lobby Button");
        
        if (type == CourseType.MULTIPLAYER) {
            if (boatSpawns.size() < 10) missing.add("Boat Spawns (" + boatSpawns.size() + "/10)");
            if (createRaceButton == null) missing.add("Create Race Button");
            if (joinRaceButton == null) missing.add("Join Race Button");
            if (startRaceButton == null) missing.add("Start Race Button");
            if (boatSpawnButtons.size() < 10) missing.add("Boat Spawn Buttons (" + boatSpawnButtons.size() + "/10)");
            if (returnToMainButton == null) missing.add("Return to Main Button");
        }
        
        return missing;
    }
    
    /**
     * Get a random boat spawn location
     */
    public Location getRandomBoatSpawn() {
        if (boatSpawns.isEmpty()) return null;
        return boatSpawns.get((int) (Math.random() * boatSpawns.size()));
    }
    
    /**
     * Check if a location is within the start line
     */
    public boolean isInStartLine(Location location) {
        if (startLine == null) return false;
        return startLine.contains(location.getX(), location.getY(), location.getZ());
    }
    
    /**
     * Check if a location is within the finish line
     */
    public boolean isInFinishLine(Location location) {
        if (finishLine == null) return false;
        return finishLine.contains(location.getX(), location.getY(), location.getZ());
    }
    
    public enum CourseType {
        SINGLEPLAYER,
        MULTIPLAYER
    }
}
