package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.command.RaceStatsCommand;
import com.bocrace.config.ConfigManager;
import com.bocrace.listener.SetupListener;
import com.bocrace.listener.StartButtonListener;
import com.bocrace.listener.RaceLineListener;
import com.bocrace.listener.RaceCleanupListener;
import com.bocrace.listener.MultiplayerButtonListener;
import com.bocrace.listener.RaceProtectionListener;
import com.bocrace.storage.StorageManager;
import com.bocrace.race.RaceManager;
import com.bocrace.race.MultiplayerRaceManager;
import com.bocrace.storage.RecordManager;
import com.bocrace.storage.YAMLRecordManager;
import com.bocrace.util.PDCKeys;
import com.bocrace.util.BoatManager;
import com.bocrace.util.TeleportUtil;
import com.bocrace.util.SoundEffectManager;
import com.bocrace.race.ActiveRace;
import com.bocrace.model.Course;
import com.bocrace.integration.BOCRacePlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.io.File;

public class BOCRacePlugin extends JavaPlugin {

    private static BOCRacePlugin instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    private RecordManager recordManager;
    private RaceManager raceManager;
    private MultiplayerRaceManager multiplayerRaceManager;
    
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
        
        // Initialize race managers
        raceManager = new RaceManager(this);
        multiplayerRaceManager = new MultiplayerRaceManager(this);
        debugLog("Race managers initialized successfully");
        
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
        getServer().getPluginManager().registerEvents(new StartButtonListener(this, boatManager, teleportUtil), this);
        getServer().getPluginManager().registerEvents(new RaceLineListener(this, boatManager, teleportUtil), this);
        getServer().getPluginManager().registerEvents(new RaceCleanupListener(this, boatManager, teleportUtil), this);
        getServer().getPluginManager().registerEvents(new MultiplayerButtonListener(this), this);
        getServer().getPluginManager().registerEvents(new RaceProtectionListener(this), this);
        debugLog("All event listeners registered successfully");

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BOCRacePlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
            
            // Generate placeholder reference file
            generatePlaceholderReferenceFile();
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholder support disabled.");
        }

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
        
        // Cleanup multiplayer races
        if (multiplayerRaceManager != null) {
            multiplayerRaceManager.shutdown();
            getLogger().info("🏁 Cleaned up all multiplayer races");
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
    
    public MultiplayerRaceManager getMultiplayerRaceManager() {
        return multiplayerRaceManager;
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
    
    public void multiplayerDebugLog(String message) {
        if (getConfig().getBoolean("debug-multiplayer", false)) {
            getLogger().info("[MP-DEBUG] " + message);
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
    
    /**
     * Generate the placeholders_api.yml reference file
     */
    private void generatePlaceholderReferenceFile() {
        try {
            File placeholderFile = new File(getDataFolder(), "placeholders_api.yml");
            
            StringBuilder content = new StringBuilder();
            content.append("# BOCRacePlugin PlaceholderAPI Reference\n");
            content.append("# This file is auto-generated on server start\n");
            content.append("# Copy and paste these placeholders into your holograms/plugins\n\n");
            
            // Get actual course names for examples
            List<String> courseNames = storageManager.getAllCourses().stream()
                .map(Course::getName)
                .collect(java.util.stream.Collectors.toList());
            
            String exampleCourse = courseNames.isEmpty() ? "YourCourseName" : courseNames.get(0);
            
            content.append("# ==========================================\n");
            content.append("# COURSE STATUS PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("course_status: \"").append("%bocrace_course_").append(exampleCourse).append("_status%\"\n");
            content.append("# Returns: §4Closed, §5In Use, §4Setup, or §2Open\n\n");
            
            content.append("# ==========================================\n");
            content.append("# COURSE RECORD PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("course_record_holder: \"").append("%bocrace_course_").append(exampleCourse).append("_record%\"\n");
            content.append("course_record_time: \"").append("%bocrace_course_").append(exampleCourse).append("_record_time%\"\n");
            content.append("course_usage_count: \"").append("%bocrace_course_").append(exampleCourse).append("_usage%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# STANDARD LEADERBOARD PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("leaderboard_name_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_name_1%\"\n");
            content.append("leaderboard_time_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_time_1%\"\n");
            content.append("leaderboard_name_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_name_2%\"\n");
            content.append("leaderboard_time_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_time_2%\"\n");
            content.append("leaderboard_name_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_name_3%\"\n");
            content.append("leaderboard_time_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_time_3%\"\n");
            content.append("leaderboard_name_4: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_name_4%\"\n");
            content.append("leaderboard_time_4: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_time_4%\"\n");
            content.append("leaderboard_name_5: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_name_5%\"\n");
            content.append("leaderboard_time_5: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_time_5%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# DAILY LEADERBOARD PLACEHOLDERS (Today's Records)\n");
            content.append("# ==========================================\n");
            content.append("daily_name_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_name_1%\"\n");
            content.append("daily_time_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_time_1%\"\n");
            content.append("daily_name_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_name_2%\"\n");
            content.append("daily_time_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_time_2%\"\n");
            content.append("daily_name_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_name_3%\"\n");
            content.append("daily_time_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_daily_time_3%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# WEEKLY LEADERBOARD PLACEHOLDERS (Last 7 Days)\n");
            content.append("# ==========================================\n");
            content.append("weekly_name_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_name_1%\"\n");
            content.append("weekly_time_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_time_1%\"\n");
            content.append("weekly_name_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_name_2%\"\n");
            content.append("weekly_time_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_time_2%\"\n");
            content.append("weekly_name_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_name_3%\"\n");
            content.append("weekly_time_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_weekly_time_3%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# MONTHLY LEADERBOARD PLACEHOLDERS (Current Month)\n");
            content.append("# ==========================================\n");
            content.append("monthly_name_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_name_1%\"\n");
            content.append("monthly_time_1: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_time_1%\"\n");
            content.append("monthly_name_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_name_2%\"\n");
            content.append("monthly_time_2: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_time_2%\"\n");
            content.append("monthly_name_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_name_3%\"\n");
            content.append("monthly_time_3: \"").append("%bocrace_leaderboard_").append(exampleCourse).append("_monthly_time_3%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# PLAYER STATUS PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("player_status: \"%bocrace_player_status%\"\n");
            content.append("player_current_time: \"%bocrace_player_current_time%\"\n");
            content.append("player_current_course: \"%bocrace_player_course%\"\n");
            content.append("player_races_completed: \"%bocrace_player_races_completed%\"\n");
            content.append("player_pb: \"").append("%bocrace_player_pb_").append(exampleCourse).append("%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# MULTIPLAYER RACE PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("mp_players_joined: \"%bocrace_mp_players_joined%\"\n");
            content.append("mp_race_status: \"%bocrace_mp_race_status%\"\n");
            content.append("mp_leader: \"%bocrace_mp_leader%\"\n");
            content.append("mp_leader_time: \"%bocrace_mp_leader_time%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# GLOBAL STATISTICS PLACEHOLDERS\n");
            content.append("# ==========================================\n");
            content.append("total_courses: \"%bocrace_total_courses%\"\n");
            content.append("active_races: \"%bocrace_active_races%\"\n");
            content.append("active_mp_races: \"%bocrace_active_mp_races%\"\n\n");
            
            content.append("# ==========================================\n");
            content.append("# COLOR CODES REFERENCE\n");
            content.append("# ==========================================\n");
            content.append("# §2 = Green (Open status)\n");
            content.append("# §5 = Purple (In Use status)\n");
            content.append("# §4 = Red (Setup/Closed status)\n");
            content.append("# §f = White (default text)\n");
            content.append("# §7 = Gray (secondary text)\n\n");
            
            content.append("# ==========================================\n");
            content.append("# TIME FORMATTING\n");
            content.append("# ==========================================\n");
            content.append("# Under 1 minute: 12.80 (2 decimal places)\n");
            content.append("# Over 1 minute: 1:35 (MM:SS format, no decimals)\n");
            content.append("# No data: N/A\n\n");
            
            content.append("# ==========================================\n");
            content.append("# INSTRUCTIONS\n");
            content.append("# ==========================================\n");
            content.append("# 1. Replace ").append(exampleCourse).append(" with your actual course names\n");
            content.append("# 2. Copy the placeholder you need (including the % symbols)\n");
            content.append("# 3. Paste into your hologram/plugin configuration\n");
            content.append("# 4. Test with: /bocrace testpapi test <placeholder>\n");
            content.append("# 5. Generate test data with: /bocrace test-leaderboards <course>\n\n");
            
            if (!courseNames.isEmpty()) {
                content.append("# ==========================================\n");
                content.append("# YOUR ACTUAL COURSE NAMES\n");
                content.append("# ==========================================\n");
                for (String courseName : courseNames) {
                    content.append("# ").append(courseName).append("\n");
                }
                content.append("\n");
            }
            
            // Write the file
            java.nio.file.Files.write(placeholderFile.toPath(), content.toString().getBytes());
            getLogger().info("PlaceholderAPI reference file generated: placeholders_api.yml");
            
        } catch (Exception e) {
            getLogger().warning("Failed to generate placeholder reference file: " + e.getMessage());
        }
    }
}
