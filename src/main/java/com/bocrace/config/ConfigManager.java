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
        File singleplayerFolder = new File(plugin.getDataFolder(), "singleplayer");
        File multiplayerFolder = new File(plugin.getDataFolder(), "multiplayer");
        
        if (!singleplayerFolder.exists()) {
            singleplayerFolder.mkdirs();
            plugin.getLogger().info("Created singleplayer course folder");
        }
        
        if (!multiplayerFolder.exists()) {
            multiplayerFolder.mkdirs();
            plugin.getLogger().info("Created multiplayer course folder");
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
    
    public String getPrefixColor() {
        return config.getString("colors.prefix", "GOLD");
    }
    
    public String getBroadcastColor() {
        return config.getString("colors.broadcast", "AQUA");
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
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
