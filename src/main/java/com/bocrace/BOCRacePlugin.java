package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.config.ConfigManager;
import com.bocrace.storage.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BOCRacePlugin extends JavaPlugin {
    
    private static BOCRacePlugin instance;
    private ConfigManager configManager;
    private StorageManager storageManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configurations and create folder structure
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Load courses
        storageManager = new StorageManager(this);
        storageManager.loadCourses();
        
        // Register the main command
        getCommand("bocrace").setExecutor(new BOCRaceCommand(this));
        getCommand("bocrace").setTabCompleter(new BOCRaceCommand(this));
        
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
}
