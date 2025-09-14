package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.placeholder.PlaceholderAPIExpansion;
import com.bocrace.storage.StorageManager;
import com.bocrace.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BOCRacePlugin
 */
public class BOCRacePlugin extends JavaPlugin {
    private StorageManager storageManager;
    private SetupManager setupManager;
    private RaceManager raceManager;
    private PlaceholderAPIExpansion placeholderExpansion;
    
    @Override
    public void onEnable() {
        // Initialize configuration
        saveDefaultConfig();
        
        // Initialize message system
        MessageUtil.initialize(this);
        
        // Initialize storage manager
        storageManager = new StorageManager(this);
        storageManager.initialize();
        
        // Initialize managers
        setupManager = new SetupManager(this);
        raceManager = new RaceManager(this);
        
        // Register command
        BOCRaceCommand command = new BOCRaceCommand(this);
        getCommand("bocrace").setExecutor(command);
        getCommand("bocrace").setTabCompleter(command);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        
        // Initialize PlaceholderAPI if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new PlaceholderAPIExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
        
        getLogger().info("BOCRacePlugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        if (storageManager != null) {
            storageManager.shutdown();
        }
        
        // Unregister PlaceholderAPI expansion
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        
        // Cancel all active races
        if (raceManager != null) {
            raceManager.shutdown();
        }
        
        getLogger().info("BOCRacePlugin disabled successfully!");
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        MessageUtil.reload(this);
    }
    
    // Getters for managers
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public SetupManager getSetupManager() {
        return setupManager;
    }
    
    public RaceManager getRaceManager() {
        return raceManager;
    }
    
    public PlaceholderAPIExpansion getPlaceholderExpansion() {
        return placeholderExpansion;
    }
}
