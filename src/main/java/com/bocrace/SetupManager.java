package com.bocrace;

import com.bocrace.model.Course;
import com.bocrace.model.SetupSession;
import com.bocrace.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages interactive course setup sessions
 */
public class SetupManager {
    private final BOCRacePlugin plugin;
    private final Map<UUID, SetupSession> activeSetups;
    
    public SetupManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.activeSetups = new ConcurrentHashMap<>();
    }
    
    /**
     * Start a setup session for a player
     */
    public void startSetup(Player player, SetupSession setupSession) {
        // Cancel any existing setup for this player
        cancelSetup(player);
        
        activeSetups.put(player.getUniqueId(), setupSession);
        
        // Send initial instruction
        SetupSession.SetupElement nextElement = setupSession.getNextElement();
        if (nextElement != null) {
            MessageUtil.sendMessage(player, "setup.instruction", 
                Map.of("element", nextElement.getDisplayName()));
        }
    }
    
    /**
     * Cancel a setup session for a player
     */
    public void cancelSetup(Player player) {
        SetupSession session = activeSetups.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
            MessageUtil.sendMessage(player, "setup.cancelled", 
                Map.of("course", session.getCourseName()));
        }
    }
    
    /**
     * Complete a setup session for a player
     */
    public void completeSetup(Player player) {
        SetupSession session = activeSetups.remove(player.getUniqueId());
        if (session != null) {
            session.complete();
            MessageUtil.sendMessage(player, "setup.ended", 
                Map.of("course", session.getCourseName()));
        }
    }
    
    /**
     * Handle a player clicking on a block during setup
     */
    public void handleBlockClick(Player player, Location location) {
        SetupSession session = activeSetups.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }
        
        SetupSession.SetupElement currentElement = session.getNextElement();
        if (currentElement == null) {
            return;
        }
        
        // Set the element based on the current setup element
        Course course = plugin.getStorageManager().getCourse(session.getCourseName());
        if (course == null) {
            MessageUtil.sendMessage(player, "error.course-not-valid", 
                Map.of("course", session.getCourseName()));
            return;
        }
        
        boolean success = false;
        
        switch (currentElement) {
            case MAIN_LOBBY:
                course.setMainLobby(location);
                success = true;
                break;
                
            case COURSE_LOBBY:
                course.setCourseLobby(location);
                success = true;
                break;
                
            case BOAT_SPAWN:
                course.getBoatSpawns().add(location);
                success = true;
                break;
                
            case CREATE_RACE_BUTTON:
                course.setCreateRaceButton(location);
                success = true;
                break;
                
            case JOIN_RACE_BUTTON:
                course.setJoinRaceButton(location);
                success = true;
                break;
                
            case START_RACE_BUTTON:
                course.setStartRaceButton(location);
                success = true;
                break;
                
            case RETURN_TO_LOBBY_BUTTON:
                course.setReturnToLobbyButton(location);
                success = true;
                break;
                
            case RETURN_TO_MAIN_BUTTON:
                course.setReturnToMainButton(location);
                success = true;
                break;
                
            case START_LINE:
            case FINISH_LINE:
                // These require two clicks to define a cuboid
                // For now, we'll use a simple 1x1x1 cuboid at the clicked location
                BoundingBox box = BoundingBox.of(location, location);
                if (currentElement == SetupSession.SetupElement.START_LINE) {
                    course.setStartLine(box);
                } else {
                    course.setFinishLine(box);
                }
                success = true;
                break;
        }
        
        if (success) {
            session.completeElement(currentElement, location);
            MessageUtil.sendMessage(player, "setup.element-set", 
                Map.of("element", currentElement.getDisplayName()));
            
            // Check if setup is complete
            if (session.isComplete()) {
                completeSetup(player);
                MessageUtil.sendMessage(player, "setup.course-complete", 
                    Map.of("course", session.getCourseName()));
                
                // Save the course
                plugin.getStorageManager().saveCourses();
            } else {
                // Send next instruction
                SetupSession.SetupElement nextElement = session.getNextElement();
                if (nextElement != null) {
                    MessageUtil.sendMessage(player, "setup.instruction", 
                        Map.of("element", nextElement.getDisplayName()));
                }
            }
        }
    }
    
    /**
     * Handle a player clicking on a button during setup
     */
    public void handleButtonClick(Player player, Location location) {
        SetupSession session = activeSetups.get(player.getUniqueId());
        if (session == null || !session.isActive()) {
            return;
        }
        
        SetupSession.SetupElement currentElement = session.getNextElement();
        if (currentElement == null) {
            return;
        }
        
        // Handle button-specific elements
        if (currentElement == SetupSession.SetupElement.BOAT_SPAWN) {
            Course course = plugin.getStorageManager().getCourse(session.getCourseName());
            if (course != null) {
                course.getBoatSpawnButtons().add(location);
                session.completeElement(currentElement, location);
                MessageUtil.sendMessage(player, "setup.element-set", 
                    Map.of("element", currentElement.getDisplayName()));
                
                // Check if setup is complete
                if (session.isComplete()) {
                    completeSetup(player);
                    MessageUtil.sendMessage(player, "setup.course-complete", 
                        Map.of("course", session.getCourseName()));
                    plugin.getStorageManager().saveCourses();
                } else {
                    SetupSession.SetupElement nextElement = session.getNextElement();
                    if (nextElement != null) {
                        MessageUtil.sendMessage(player, "setup.instruction", 
                            Map.of("element", nextElement.getDisplayName()));
                    }
                }
            }
        }
    }
    
    /**
     * Check if a player is in setup mode
     */
    public boolean isInSetupMode(Player player) {
        SetupSession session = activeSetups.get(player.getUniqueId());
        return session != null && session.isActive();
    }
    
    /**
     * Get the active setup session for a player
     */
    public SetupSession getSetupSession(Player player) {
        return activeSetups.get(player.getUniqueId());
    }
    
    /**
     * Get all active setup sessions
     */
    public Map<UUID, SetupSession> getActiveSetups() {
        return activeSetups;
    }
    
    /**
     * Cancel all setup sessions
     */
    public void cancelAllSetups() {
        for (SetupSession session : activeSetups.values()) {
            session.cancel();
        }
        activeSetups.clear();
    }
}
