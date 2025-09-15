package com.bocrace;

import com.bocrace.command.BOCRaceCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BOCRacePlugin extends JavaPlugin {
    
    private static BOCRacePlugin instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Register the main command
        getCommand("bocrace").setExecutor(new BOCRaceCommand());
        getCommand("bocrace").setTabCompleter(new BOCRaceCommand());
        
        getLogger().info("BOCRacePlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("BOCRacePlugin has been disabled!");
    }
    
    public static BOCRacePlugin getInstance() {
        return instance;
    }
}
