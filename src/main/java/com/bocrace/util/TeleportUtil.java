package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Utility for consistent teleportation with lobby priority system
 */
public class TeleportUtil {
    
    private final BOCRacePlugin plugin;
    
    public TeleportUtil(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Emergency teleport with multiple fallbacks
     * Priority: preRaceLocation > mainLobby > courseLobby > world spawn
     */
    public boolean emergencyTeleport(Player player, Course course, Location preRaceLocation, String reason) {
        plugin.raceDebugLog("🚨 EMERGENCY TELEPORT - Player: " + player.getName() + ", Reason: " + reason);
        
        // Try pre-race location first (safest)
        if (preRaceLocation != null && isSafeLocation(preRaceLocation)) {
            plugin.raceDebugLog("🛡️ Using pre-race location for emergency teleport");
            boolean success = player.teleport(preRaceLocation);
            if (success) {
                player.sendMessage("§a✅ Returned to safety.");
                return true;
            }
        }
        
        // Fallback to normal lobby system
        return teleportToLobby(player, course, reason + " (emergency)");
    }
    
    /**
     * Teleport player using lobby priority system
     * Priority: mainLobby first, then courseLobby (required)
     */
    public boolean teleportToLobby(Player player, Course course, String reason) {
        Location destination = null;
        String locationName = "";
        
        // Priority system: mainLobby first, then courseLobby
        if (course.getSpmainlobby() != null) {
            destination = course.getSpmainlobby().clone();
            locationName = "main lobby";
            plugin.debugLog("Teleporting " + player.getName() + " to main lobby for " + reason);
        } else if (course.getSpcourselobby() != null) {
            destination = course.getSpcourselobby().clone();
            locationName = "course lobby";
            plugin.debugLog("Teleporting " + player.getName() + " to course lobby for " + reason + " (no main lobby set)");
        } else {
            plugin.debugLog("Cannot teleport " + player.getName() + " - no lobby locations set for course " + course.getName());
            
            // EMERGENCY: Teleport to world spawn as absolute last resort
            plugin.raceDebugLog("🚨 EMERGENCY: No lobby configured, using world spawn");
            Location worldSpawn = player.getWorld().getSpawnLocation();
            boolean success = player.teleport(worldSpawn);
            
            if (success) {
                player.sendMessage("§c⚠️ Course lobby not configured! Teleported to world spawn. Please contact an admin.");
                return true;
            } else {
                player.sendMessage("§4🚨 CRITICAL ERROR: Cannot teleport! Please relog and contact an admin immediately!");
                return false;
            }
        }
        
        // Center the location and add slight Y offset for safety
        destination = centerLocation(destination);
        destination.add(0, 1.0, 0); // Add 1 block up to prevent suffocation
        
        // Perform teleportation
        boolean success = player.teleport(destination);
        
        if (success) {
            plugin.debugLog("Successfully teleported " + player.getName() + " to " + locationName + 
                           " at " + destination.getWorld().getName() + " " + 
                           destination.getBlockX() + "," + destination.getBlockY() + "," + destination.getBlockZ());
            
            // Send appropriate message based on reason
            switch (reason.toLowerCase()) {
                case "race_complete":
                    player.sendMessage("§a🏁 Race completed! Returned to " + locationName + ".");
                    break;
                case "course_busy":
                    player.sendMessage("§eCourse is in use. Please wait in the " + locationName + ".");
                    break;
                case "race_dq":
                    player.sendMessage("§c⚠️ Race disqualified - exited boat. Returned to " + locationName + ".");
                    break;
                default:
                    player.sendMessage("§aTeleported to " + locationName + " for course '" + course.getName() + "'.");
                    break;
            }
        } else {
            plugin.debugLog("Failed to teleport " + player.getName() + " to " + locationName);
            player.sendMessage("§cTeleportation failed! Please contact an admin.");
        }
        
        return success;
    }
    
    /**
     * Center a location on the block (x.5, y, z.5)
     */
    public static Location centerLocation(Location location) {
        if (location == null) return null;
        
        Location centered = location.clone();
        centered.setX(location.getBlockX() + 0.5);
        centered.setZ(location.getBlockZ() + 0.5);
        // Keep original Y coordinate (don't center Y)
        
        return centered;
    }
    
    /**
     * Center location with custom Y offset
     */
    public static Location centerLocationWithYOffset(Location location, double yOffset) {
        if (location == null) return null;
        
        Location centered = centerLocation(location);
        centered.add(0, yOffset, 0);
        
        return centered;
    }
    
    /**
     * Check if a location is safe for teleportation
     */
    public static boolean isSafeLocation(Location location) {
        if (location == null || location.getWorld() == null) return false;
        
        // Check if the location and the block above are not solid
        Location above = location.clone().add(0, 1, 0);
        
        return !location.getBlock().getType().isSolid() && 
               !above.getBlock().getType().isSolid();
    }
}
