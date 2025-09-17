package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import org.bukkit.NamespacedKey;

/**
 * Persistent Data Container keys for tracking race boats
 */
public class PDCKeys {
    
    private final BOCRacePlugin plugin;
    
    // Keys for boat metadata
    public final NamespacedKey raceBoat;
    public final NamespacedKey playerUuid;
    public final NamespacedKey courseName;
    public final NamespacedKey raceState;
    public final NamespacedKey startTime;
    
    public PDCKeys(BOCRacePlugin plugin) {
        this.plugin = plugin;
        
        // Initialize all PDC keys
        this.raceBoat = new NamespacedKey(plugin, "race_boat");
        this.playerUuid = new NamespacedKey(plugin, "player_uuid");
        this.courseName = new NamespacedKey(plugin, "course_name");
        this.raceState = new NamespacedKey(plugin, "race_state");
        this.startTime = new NamespacedKey(plugin, "start_time");
    }
    
    /**
     * Get all PDC keys as an array for debugging
     */
    public NamespacedKey[] getAllKeys() {
        return new NamespacedKey[]{raceBoat, playerUuid, courseName, raceState, startTime};
    }
}
