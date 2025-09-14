package com.bocrace.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an interactive setup session for course creation/editing
 */
public class SetupSession {
    private final UUID playerId;
    private final String courseName;
    private final Course.CourseType courseType;
    private final List<SetupElement> remainingElements;
    private final List<SetupElement> completedElements;
    private final long startTime;
    private boolean active;
    
    public SetupSession(Player player, String courseName, Course.CourseType courseType) {
        this.playerId = player.getUniqueId();
        this.courseName = courseName;
        this.courseType = courseType;
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.completedElements = new ArrayList<>();
        this.remainingElements = new ArrayList<>();
        
        // Initialize remaining elements based on course type
        initializeElements();
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public String getCourseName() { return courseName; }
    public Course.CourseType getCourseType() { return courseType; }
    public List<SetupElement> getRemainingElements() { return remainingElements; }
    public List<SetupElement> getCompletedElements() { return completedElements; }
    public long getStartTime() { return startTime; }
    public boolean isActive() { return active; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    
    /**
     * Initialize the list of elements that need to be set up
     */
    private void initializeElements() {
        // Common elements for both types
        remainingElements.add(SetupElement.COURSE_LOBBY);
        remainingElements.add(SetupElement.BOAT_SPAWN);
        remainingElements.add(SetupElement.START_LINE);
        remainingElements.add(SetupElement.FINISH_LINE);
        remainingElements.add(SetupElement.RETURN_TO_LOBBY_BUTTON);
        
        if (courseType == Course.CourseType.MULTIPLAYER) {
            // Multiplayer-specific elements
            remainingElements.add(SetupElement.CREATE_RACE_BUTTON);
            remainingElements.add(SetupElement.JOIN_RACE_BUTTON);
            remainingElements.add(SetupElement.START_RACE_BUTTON);
            remainingElements.add(SetupElement.RETURN_TO_MAIN_BUTTON);
            
            // Add 9 more boat spawns (total 10)
            for (int i = 0; i < 9; i++) {
                remainingElements.add(SetupElement.BOAT_SPAWN);
            }
        }
        
        // Optional elements
        remainingElements.add(SetupElement.MAIN_LOBBY);
    }
    
    /**
     * Complete an element setup
     */
    public boolean completeElement(SetupElement element, Location location) {
        if (!remainingElements.contains(element)) {
            return false;
        }
        
        remainingElements.remove(element);
        completedElements.add(element);
        return true;
    }
    
    /**
     * Check if setup is complete
     */
    public boolean isComplete() {
        // Check if all required elements are completed
        List<SetupElement> requiredElements = getRequiredElements();
        return completedElements.containsAll(requiredElements);
    }
    
    /**
     * Get list of required elements for the course type
     */
    public List<SetupElement> getRequiredElements() {
        List<SetupElement> required = new ArrayList<>();
        required.add(SetupElement.COURSE_LOBBY);
        required.add(SetupElement.BOAT_SPAWN);
        required.add(SetupElement.START_LINE);
        required.add(SetupElement.FINISH_LINE);
        required.add(SetupElement.RETURN_TO_LOBBY_BUTTON);
        
        if (courseType == Course.CourseType.MULTIPLAYER) {
            required.add(SetupElement.CREATE_RACE_BUTTON);
            required.add(SetupElement.JOIN_RACE_BUTTON);
            required.add(SetupElement.START_RACE_BUTTON);
            required.add(SetupElement.RETURN_TO_MAIN_BUTTON);
        }
        
        return required;
    }
    
    /**
     * Get list of missing required elements
     */
    public List<SetupElement> getMissingRequiredElements() {
        List<SetupElement> required = getRequiredElements();
        required.removeAll(completedElements);
        return required;
    }
    
    /**
     * Get the next element to set up
     */
    public SetupElement getNextElement() {
        if (remainingElements.isEmpty()) {
            return null;
        }
        return remainingElements.get(0);
    }
    
    /**
     * Get the duration of the setup session in seconds
     */
    public long getDuration() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    /**
     * Cancel the setup session
     */
    public void cancel() {
        this.active = false;
    }
    
    /**
     * Complete the setup session
     */
    public void complete() {
        this.active = false;
    }
    
    /**
     * Enum representing different setup elements
     */
    public enum SetupElement {
        MAIN_LOBBY("Main Lobby", "Optional global lobby location", false),
        COURSE_LOBBY("Course Lobby", "Required lobby for this course", true),
        BOAT_SPAWN("Boat Spawn", "Required boat spawn location", true),
        START_LINE("Start Line", "Required start line cuboid", true),
        FINISH_LINE("Finish Line", "Required finish line cuboid", true),
        CREATE_RACE_BUTTON("Create Race Button", "Required button to create race lobby", true),
        JOIN_RACE_BUTTON("Join Race Button", "Required button to join race lobby", true),
        START_RACE_BUTTON("Start Race Button", "Required button to start the race", true),
        RETURN_TO_LOBBY_BUTTON("Return to Lobby Button", "Required button to return to course lobby", true),
        RETURN_TO_MAIN_BUTTON("Return to Main Button", "Required button to return to main lobby", true);
        
        private final String displayName;
        private final String description;
        private final boolean required;
        
        SetupElement(String displayName, String description, boolean required) {
            this.displayName = displayName;
            this.description = description;
            this.required = required;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public boolean isRequired() { return required; }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}
