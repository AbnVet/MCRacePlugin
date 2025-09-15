package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import com.bocrace.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BOCRacePlugin extends JavaPlugin {
    
    private static BOCRacePlugin instance;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configurations and create folder structure
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
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
}
