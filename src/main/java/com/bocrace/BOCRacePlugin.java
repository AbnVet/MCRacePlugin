package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.config.ConfigManager;
import com.bocrace.listener.SetupListener;
import com.bocrace.storage.StorageManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BOCRacePlugin extends JavaPlugin {
    
    private static BOCRacePlugin instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    
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
        
        // Register the main command
        getCommand("bocrace").setExecutor(new BOCRaceCommand(this));
        getCommand("bocrace").setTabCompleter(new BOCRaceCommand(this));
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new SetupListener(this), this);
        
        getLogger().info("BOCRacePlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("BOCRacePlugin has been disabled!");
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
