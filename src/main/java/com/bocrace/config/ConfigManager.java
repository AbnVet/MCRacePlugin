package com.bocrace.config;

import com.bocrace.BOCRacePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
    
    private final BOCRacePlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    
    public ConfigManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create course folders
        createCourseFolders();
        
        // Load config.yml
        loadConfig();
        
        // Load messages.yml
        loadMessages();
        
        // Log startup information
        logStartupInfo();
    }
    
    private void createCourseFolders() {
        // Create courses directory structure
        File coursesDir = new File(plugin.getDataFolder(), "courses");
        File singleplayerCoursesDir = new File(coursesDir, "singleplayer");
        File multiplayerCoursesDir = new File(coursesDir, "multiplayer");
        
        if (!singleplayerCoursesDir.exists()) {
            singleplayerCoursesDir.mkdirs();
            plugin.getLogger().info("Created singleplayer courses folder");
        }
        
        if (!multiplayerCoursesDir.exists()) {
            multiplayerCoursesDir.mkdirs();
            plugin.getLogger().info("Created multiplayer courses folder");
        }
        
        // Create data directory structure
        File dataDir = new File(plugin.getDataFolder(), "data");
        File singleplayerDataDir = new File(dataDir, "singleplayer");
        File multiplayerDataDir = new File(dataDir, "multiplayer");
        
        if (!singleplayerDataDir.exists()) {
            singleplayerDataDir.mkdirs();
            plugin.getLogger().info("Created singleplayer data folder");
        }
        
        if (!multiplayerDataDir.exists()) {
            multiplayerDataDir.mkdirs();
            plugin.getLogger().info("Created multiplayer data folder");
        }
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    private void logStartupInfo() {
        int maxPlayers = config.getInt("max-players", 10);
        int raceTimeout = config.getInt("race-timeout", 60);
        
        plugin.getLogger().info("Loaded config: max-players=" + maxPlayers + ", race-timeout=" + raceTimeout + "s");
        plugin.getLogger().info("Messages loaded successfully.");
    }
    
    public void reloadConfigs() {
        loadConfig();
        loadMessages();
        plugin.getLogger().info("Configs reloaded successfully.");
    }
    
    // Getters for config values
    public int getRaceTimeout() {
        return config.getInt("race-timeout", 60);
    }
    
    public int getMaxPlayers() {
        return config.getInt("max-players", 10);
    }
    
    public boolean isStartDingEnabled() {
        return config.getBoolean("sounds.start-ding", true);
    }
    
    public boolean isFinishFireworksEnabled() {
        return config.getBoolean("sounds.finish-fireworks", true);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public boolean isRaceDebugEnabled() {
        return config.getBoolean("debug-race", false);
    }
    
    public boolean isSetupDebugEnabled() {
        return config.getBoolean("debug-setup", false);
    }
    
    public String getPrefixColor() {
        return config.getString("colors.prefix", "GOLD");
    }
    
    public String getBroadcastColor() {
        return config.getString("colors.broadcast", "AQUA");
    }
    
    // Getters for messages
    public String getPrefix() {
        return messages.getString("prefix", "[BOCRace] ");
    }
    
    public String getMessage(String key) {
        return messages.getString("messages." + key, "Message not found: " + key);
    }
    
    public String getMessage(String key, String placeholder, String replacement) {
        String message = getMessage(key);
        return message.replace("%" + placeholder + "%", replacement);
    }
}
