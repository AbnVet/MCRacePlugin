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
        
        // Auto-migrate config for updates (add missing sections)
        migrateConfig();
    }
    
    /**
     * Add missing config sections for plugin updates
     */
    private void migrateConfig() {
        boolean configUpdated = false;
        
        // Add multiplayer section if missing
        if (!config.contains("multiplayer")) {
            plugin.getLogger().info("Adding missing multiplayer configuration section...");
            config.set("multiplayer.race-timeout", 300);
            config.set("multiplayer.max-join-players", 9);
            config.set("multiplayer.announcements.race-created", "&6{player} &eis starting a multiplayer race on &a{course}&e! Join at the race lobby for prizes!");
            config.set("multiplayer.announcements.race-started", "&aRace started on &6{course}&a! &7({players} racers)");
            config.set("multiplayer.announcements.race-finished", "&6Race completed on &a{course}&6! Results posted.");
            config.set("multiplayer.announcements.race-timeout", "&cRace on &6{course} &chas timed out! Unfinished players disqualified.");
            
            // Player protection settings
            config.set("multiplayer.player-protection.enabled", true);
            config.set("multiplayer.player-protection.prevent-mob-damage", true);
            config.set("multiplayer.player-protection.prevent-pvp", true);
            config.set("multiplayer.player-protection.prevent-explosion-damage", true);
            config.set("multiplayer.player-protection.prevent-drowning", true);
            config.set("multiplayer.player-protection.prevent-fall-damage", false); // Allow for course obstacles
            config.set("multiplayer.player-protection.prevent-item-drops", true);
            config.set("multiplayer.player-protection.death-disqualifies", true);
            config.set("multiplayer.player-protection.protect-boats", true); // Prevent boat damage/destruction
            
            // Race effects
            config.set("multiplayer.effects.night-vision.enabled", true);
            config.set("multiplayer.effects.night-vision.duration", 600); // 10 minutes in seconds
            
            configUpdated = true;
        }
        
        // Add debug-multiplayer if missing
        if (!config.contains("debug-multiplayer")) {
            plugin.getLogger().info("Adding missing debug-multiplayer option...");
            config.set("debug-multiplayer", false);
            configUpdated = true;
        }
        
        // Save updated config
        if (configUpdated) {
            try {
                config.save(new File(plugin.getDataFolder(), "config.yml"));
                plugin.getLogger().info("✅ Config updated with new features!");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save updated config: " + e.getMessage());
            }
        }
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
