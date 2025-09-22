package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.race.ActiveRace;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Manages race boat spawning, tracking, and cleanup
 */
public class BoatManager {
    
    private final BOCRacePlugin plugin;
    private final PDCKeys pdcKeys;
    
    public BoatManager(BOCRacePlugin plugin, PDCKeys pdcKeys) {
        this.plugin = plugin;
        this.pdcKeys = pdcKeys;
    }
    
    /**
     * Spawn a race boat for a player at the specified location (multiplayer)
     */
    public Boat spawnRaceBoat(Player player, Location spawnLocation) {
        if (spawnLocation == null) {
            plugin.debugLog("Cannot spawn boat - no spawn location provided");
            return null;
        }
        
        // Use exact spawn location including yaw/pitch for proper direction
        Location boatSpawn = spawnLocation.clone();
        boatSpawn.setX(spawnLocation.getBlockX() + 0.5); // Center X
        boatSpawn.setZ(spawnLocation.getBlockZ() + 0.5); // Center Z
        boatSpawn.add(0, 1.0, 0); // Add Y offset
        // Keep original yaw/pitch from setup (FIXED - was missing for multiplayer)
        
        plugin.debugLog("Spawning multiplayer race boat for " + player.getName() + " at " + 
                       boatSpawn.getWorld().getName() + " " + 
                       boatSpawn.getBlockX() + "," + boatSpawn.getBlockY() + "," + boatSpawn.getBlockZ() +
                       " facing yaw: " + boatSpawn.getYaw());
        
        // Spawn the boat
        Boat boat = (Boat) boatSpawn.getWorld().spawnEntity(boatSpawn, EntityType.OAK_BOAT);
        
        // Mark as race boat
        boat.getPersistentDataContainer().set(pdcKeys.raceBoat, PersistentDataType.BOOLEAN, true);
        boat.getPersistentDataContainer().set(pdcKeys.playerUuid, PersistentDataType.STRING, player.getUniqueId().toString());
        
        // Teleport player into boat
        player.teleport(boatSpawn);
        boat.addPassenger(player);
        
        plugin.debugLog("✅ Multiplayer race boat spawned successfully for " + player.getName());
        return boat;
    }
    
    /**
     * Spawn a race boat for a player at the specified course (singleplayer)
     */
    public Boat spawnRaceBoat(Player player, Course course, ActiveRace race) {
        Location spawnLocation = course.getSpboatspawn();
        if (spawnLocation == null) {
            plugin.debugLog("Cannot spawn boat - no boat spawn location set for course " + course.getName());
            return null;
        }
        
        // Use exact spawn location including yaw/pitch for proper direction
        Location boatSpawn = spawnLocation.clone();
        boatSpawn.setX(spawnLocation.getBlockX() + 0.5); // Center X
        boatSpawn.setZ(spawnLocation.getBlockZ() + 0.5); // Center Z
        boatSpawn.add(0, 1.0, 0); // Add Y offset
        // Keep original yaw/pitch from setup
        
        plugin.debugLog("Spawning race boat for " + player.getName() + " at " + 
                       boatSpawn.getWorld().getName() + " " + 
                       boatSpawn.getBlockX() + "," + boatSpawn.getBlockY() + "," + boatSpawn.getBlockZ());
        
        // Spawn the boat with course-specific type
        EntityType boatType = parseBoatType(course.getBoatType());
        Boat boat = (Boat) boatSpawn.getWorld().spawnEntity(boatSpawn, boatType);
        
        plugin.debugLog("Spawned " + boatType.name() + " for course " + course.getName());
        
        // Tag the boat with PDC data
        boat.getPersistentDataContainer().set(pdcKeys.raceBoat, PersistentDataType.BOOLEAN, true);
        boat.getPersistentDataContainer().set(pdcKeys.playerUuid, PersistentDataType.STRING, player.getUniqueId().toString());
        boat.getPersistentDataContainer().set(pdcKeys.courseName, PersistentDataType.STRING, course.getName());
        boat.getPersistentDataContainer().set(pdcKeys.raceState, PersistentDataType.STRING, race.getState().name());
        boat.getPersistentDataContainer().set(pdcKeys.startTime, PersistentDataType.LONG, System.currentTimeMillis());
        
        // Add player as passenger
        boat.addPassenger(player);
        
        // Update race with boat UUID
        race.setBoatUuid(boat.getUniqueId());
        
        plugin.debugLog("Race boat spawned successfully - UUID: " + boat.getUniqueId() + 
                       ", Player: " + player.getName() + ", Course: " + course.getName());
        
        return boat;
    }
    
    /**
     * Check if an entity is a race boat
     */
    public boolean isRaceBoat(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Boat)) return false;
        
        return entity.getPersistentDataContainer().has(pdcKeys.raceBoat, PersistentDataType.BOOLEAN);
    }
    
    /**
     * Get the player UUID associated with a race boat
     */
    public UUID getRaceBoatPlayer(Boat boat) {
        if (!isRaceBoat(boat)) return null;
        
        String uuidString = boat.getPersistentDataContainer().get(pdcKeys.playerUuid, PersistentDataType.STRING);
        if (uuidString == null) return null;
        
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            plugin.debugLog("Invalid UUID in boat PDC: " + uuidString);
            return null;
        }
    }
    
    /**
     * Get the course name associated with a race boat
     */
    public String getRaceBoatCourse(Boat boat) {
        if (!isRaceBoat(boat)) return null;
        
        return boat.getPersistentDataContainer().get(pdcKeys.courseName, PersistentDataType.STRING);
    }
    
    /**
     * Update boat race state
     */
    public void updateBoatState(Boat boat, ActiveRace.State state) {
        if (!isRaceBoat(boat)) return;
        
        boat.getPersistentDataContainer().set(pdcKeys.raceState, PersistentDataType.STRING, state.name());
        plugin.debugLog("Updated boat state to " + state.name() + " for boat " + boat.getUniqueId());
    }
    
    /**
     * Remove a race boat and cleanup
     */
    public void removeRaceBoat(Boat boat, String reason) {
        if (boat == null || boat.isDead()) return;
        
        UUID playerUuid = getRaceBoatPlayer(boat);
        String courseName = getRaceBoatCourse(boat);
        
        plugin.debugLog("Removing race boat - UUID: " + boat.getUniqueId() + 
                       ", Player: " + (playerUuid != null ? playerUuid.toString() : "unknown") + 
                       ", Course: " + (courseName != null ? courseName : "unknown") + 
                       ", Reason: " + reason);
        
        // Remove all passengers first
        boat.getPassengers().forEach(boat::removePassenger);
        
        // Remove the boat
        boat.remove();
        
        plugin.debugLog("Race boat removed successfully");
    }
    
    /**
     * Find a race boat by player UUID
     */
    public Boat findRaceBoatByPlayer(UUID playerUuid) {
        // This is a simple implementation - in a production environment,
        // you might want to maintain a map of active boats for better performance
        
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Boat && isRaceBoat(entity)) {
                    Boat boat = (Boat) entity;
                    UUID boatPlayer = getRaceBoatPlayer(boat);
                    if (playerUuid.equals(boatPlayer)) {
                        return boat;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Cleanup all race boats (for plugin disable/reload)
     */
    public int cleanupAllRaceBoats() {
        int count = 0;
        
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity instanceof Boat && isRaceBoat(entity)) {
                    removeRaceBoat((Boat) entity, "plugin_cleanup");
                    count++;
                }
            }
        }
        
        plugin.debugLog("Cleaned up " + count + " race boats");
        return count;
    }
    
    /**
     * Parse boat type string to EntityType with fallback to OAK_BOAT
     */
    private EntityType parseBoatType(String boatTypeName) {
        if (boatTypeName == null || boatTypeName.trim().isEmpty()) {
            return EntityType.OAK_BOAT; // Default
        }
        
        try {
            // Try to parse the boat type
            String normalizedName = boatTypeName.toUpperCase().trim();
            
            // Add _BOAT suffix if not present
            if (!normalizedName.endsWith("_BOAT") && !normalizedName.endsWith("_RAFT")) {
                normalizedName = normalizedName + "_BOAT";
            }
            
            // Parse the EntityType
            EntityType boatType = EntityType.valueOf(normalizedName);
            
            // Verify it's actually a boat type
            if (isValidBoatType(boatType)) {
                return boatType;
            } else {
                plugin.getLogger().warning("Invalid boat type: " + boatTypeName + " - using OAK_BOAT");
                return EntityType.OAK_BOAT;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown boat type: " + boatTypeName + " - using OAK_BOAT");
            return EntityType.OAK_BOAT;
        }
    }
    
    /**
     * Check if an EntityType is a valid boat type
     */
    private boolean isValidBoatType(EntityType type) {
        switch (type) {
            case OAK_BOAT:
            case BIRCH_BOAT:
            case SPRUCE_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
            case MANGROVE_BOAT:
            case CHERRY_BOAT:
            case BAMBOO_RAFT:
            case PALE_OAK_BOAT:
                return true;
            default:
                return false;
        }
    }
}
