package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.command.RaceStatsCommand;
import com.bocrace.config.ConfigManager;
import com.bocrace.listener.SetupListener;
import com.bocrace.listener.StartButtonListener;
import com.bocrace.listener.RaceLineListener;
import com.bocrace.listener.RaceCleanupListener;
import com.bocrace.storage.StorageManager;
import com.bocrace.race.RaceManager;
import com.bocrace.storage.RecordManager;
import com.bocrace.storage.YAMLRecordManager;
import com.bocrace.util.PDCKeys;
import com.bocrace.util.BoatManager;
import com.bocrace.util.TeleportUtil;
import com.bocrace.util.SoundEffectManager;
import com.bocrace.race.ActiveRace;
import com.bocrace.model.Course;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BOCRacePlugin extends JavaPlugin {

    private static BOCRacePlugin instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    private RecordManager recordManager;
    private RaceManager raceManager;
    
    // Race utilities
    private PDCKeys pdcKeys;
    private BoatManager boatManager;
    private TeleportUtil teleportUtil;
    private SoundEffectManager soundEffectManager;
    
    // Setup mode tracking
    private Map<UUID, SetupMode> playerSetupModes;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize setup mode tracking
        playerSetupModes = new HashMap<>();
        
        // Load configurations and create folder structure
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Load courses
        storageManager = new StorageManager(this);
        storageManager.loadCourses();
        
        // Initialize record manager
        recordManager = new YAMLRecordManager(this);
        
        // Initialize race manager
        raceManager = new RaceManager(this);
        debugLog("Race manager initialized successfully");
        
        // Initialize race utilities
        pdcKeys = new PDCKeys(this);
        boatManager = new BoatManager(this, pdcKeys);
        teleportUtil = new TeleportUtil(this);
        soundEffectManager = new SoundEffectManager(this);
        debugLog("Race utilities initialized successfully");

        // Register the main command
        getCommand("bocrace").setExecutor(new BOCRaceCommand(this));
        getCommand("bocrace").setTabCompleter(new BOCRaceCommand(this));
        
        // Register the race stats command
        getCommand("racestats").setExecutor(new RaceStatsCommand(this));
        getCommand("racestats").setTabCompleter(new RaceStatsCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new SetupListener(this), this);
        getServer().getPluginManager().registerEvents(new RaceLineListener(this, boatManager, teleportUtil), this);
        getServer().getPluginManager().registerEvents(new RaceCleanupListener(this, boatManager, teleportUtil), this);
        debugLog("All event listeners registered successfully");

        getLogger().info("BOCRacePlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("🚨 BOCRacePlugin disabling - performing emergency cleanup...");
        
        // Emergency: Teleport all racing players to safety
        if (raceManager != null && teleportUtil != null) {
            int playersRescued = performEmergencyPlayerRescue();
            if (playersRescued > 0) {
                getLogger().info("🛡️ Emergency rescued " + playersRescued + " racing players");
            }
        }
        
        // Cleanup all race boats
        if (boatManager != null) {
            int cleanedUp = boatManager.cleanupAllRaceBoats();
            if (cleanedUp > 0) {
                getLogger().info("🛥️ Cleaned up " + cleanedUp + " race boats on shutdown");
            }
        }
        
        getLogger().info("BOCRacePlugin has been disabled!");
    }
    
    /**
     * Emergency rescue system for plugin disable
     */
    private int performEmergencyPlayerRescue() {
        int rescued = 0;
        
        for (ActiveRace race : raceManager.getAllActiveRaces().values()) {
            if (race.getState() == ActiveRace.State.ARMED || race.getState() == ActiveRace.State.RUNNING) {
                Player player = getServer().getPlayer(race.getPlayerUuid());
                if (player != null && player.isOnline()) {
                    
                    // Try to get the course for proper teleportation
                    Course course = storageManager.getCourse(race.getCourseName());
                    
                    if (course != null) {
                        // Use emergency teleport with pre-race location fallback
                        teleportUtil.emergencyTeleport(player, course, race.getPreRaceLocation(), "plugin_disable");
                    } else {
                        // Course not found, use pre-race location or world spawn
                        Location safeLocation = race.getPreRaceLocation();
                        if (safeLocation == null || !TeleportUtil.isSafeLocation(safeLocation)) {
                            safeLocation = player.getWorld().getSpawnLocation();
                        }
                        
                        player.teleport(safeLocation);
                        player.sendMessage("§c⚠️ Race plugin disabled! Returned to safe location.");
                    }
                    
                    rescued++;
                    getLogger().info("🛡️ Emergency rescued player: " + race.getPlayerName());
                }
            }
        }
        
        return rescued;
    }
    
    public static BOCRacePlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public RecordManager getRecordManager() {
        return recordManager;
    }
    
    public RaceManager getRaceManager() {
        return raceManager;
    }
    
    public PDCKeys getPdcKeys() {
        return pdcKeys;
    }
    
    public BoatManager getBoatManager() {
        return boatManager;
    }
    
    public TeleportUtil getTeleportUtil() {
        return teleportUtil;
    }
    
    public SoundEffectManager getSoundEffectManager() {
        return soundEffectManager;
    }
    
    // Setup mode management
    public Map<UUID, SetupMode> getPlayerSetupModes() {
        return playerSetupModes;
    }
    
    public void setPlayerSetupMode(Player player, String courseName, String action) {
        SetupMode setupMode = new SetupMode(courseName, action);
        playerSetupModes.put(player.getUniqueId(), setupMode);
        getLogger().info("[DEBUG] Player " + player.getName() + " entered setup mode: " + action + " for course " + courseName);
    }
    
    public void clearPlayerSetupMode(Player player) {
        SetupMode removed = playerSetupModes.remove(player.getUniqueId());
        if (removed != null) {
            getLogger().info("[DEBUG] Player " + player.getName() + " exited setup mode: " + removed.getAction() + " for course " + removed.getCourseName());
        }
    }
    
    public SetupMode getPlayerSetupMode(Player player) {
        return playerSetupModes.get(player.getUniqueId());
    }
    
    // Debug logging helpers
    public void debugLog(String message) {
        if (configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    public void raceDebugLog(String message) {
        if (configManager.isRaceDebugEnabled()) {
            getLogger().info("[RACE-DEBUG] " + message);
        }
    }
    
    public void setupDebugLog(String message) {
        if (configManager.isSetupDebugEnabled()) {
            getLogger().info("[SETUP-DEBUG] " + message);
        }
    }
    
    // Setup mode data class
    public static class SetupMode {
        private final String courseName;
        private final String action;
        private final long timestamp;
        
        public SetupMode(String courseName, String action) {
            this.courseName = courseName;
            this.action = action;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getCourseName() { return courseName; }
        public String getAction() { return action; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isExpired() {
            // 30 second timeout
            return System.currentTimeMillis() - timestamp > 30000;
        }
    }
}
