package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Utility class for handling configuration values
 */
public class ConfigUtil {
    
    /**
     * Get a sound from configuration
     */
    public static Sound getSound(BOCRacePlugin plugin, String path, Sound defaultValue) {
        String soundName = plugin.getConfig().getString(path);
        if (soundName == null || soundName.isEmpty()) {
            return defaultValue;
        }
        
        try {
            // Use reflection to avoid deprecated method
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name: " + soundName + " at path: " + path);
            return defaultValue;
        }
    }
    
    /**
     * Get a color from configuration
     */
    public static String getColor(BOCRacePlugin plugin, String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }
    
    /**
     * Get a boolean from configuration
     */
    public static boolean getBoolean(BOCRacePlugin plugin, String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }
    
    /**
     * Get an integer from configuration
     */
    public static int getInt(BOCRacePlugin plugin, String path, int defaultValue) {
        return plugin.getConfig().getInt(path, defaultValue);
    }
    
    /**
     * Get a double from configuration
     */
    public static double getDouble(BOCRacePlugin plugin, String path, double defaultValue) {
        return plugin.getConfig().getDouble(path, defaultValue);
    }
    
    /**
     * Get a string from configuration
     */
    public static String getString(BOCRacePlugin plugin, String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }
    
    /**
     * Get a configuration section
     */
    public static ConfigurationSection getSection(BOCRacePlugin plugin, String path) {
        return plugin.getConfig().getConfigurationSection(path);
    }
    
    /**
     * Check if a path exists in configuration
     */
    public static boolean exists(BOCRacePlugin plugin, String path) {
        return plugin.getConfig().contains(path);
    }
    
    /**
     * Get the maximum players per race
     */
    public static int getMaxPlayersPerRace(BOCRacePlugin plugin) {
        return getInt(plugin, "general.max-players-per-race", 10);
    }
    
    /**
     * Get the race timeout in seconds
     */
    public static int getRaceTimeout(BOCRacePlugin plugin) {
        return getInt(plugin, "general.race-timeout", 300);
    }
    
    /**
     * Check if debug mode is enabled
     */
    public static boolean isDebugEnabled(BOCRacePlugin plugin) {
        return getBoolean(plugin, "general.debug", false);
    }
    
    /**
     * Get the race start sound
     */
    public static Sound getRaceStartSound(BOCRacePlugin plugin) {
        return getSound(plugin, "sounds.race-start", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }
    
    /**
     * Get the race finish sound
     */
    public static Sound getRaceFinishSound(BOCRacePlugin plugin) {
        return getSound(plugin, "sounds.race-finish", Sound.ENTITY_PLAYER_LEVELUP);
    }
    
    /**
     * Get the button click sound
     */
    public static Sound getButtonClickSound(BOCRacePlugin plugin) {
        return getSound(plugin, "sounds.button-click", Sound.UI_BUTTON_CLICK);
    }
    
    /**
     * Get the error sound
     */
    public static Sound getErrorSound(BOCRacePlugin plugin) {
        return getSound(plugin, "sounds.error", Sound.ENTITY_VILLAGER_NO);
    }
    
    /**
     * Check if singleplayer races are enabled
     */
    public static boolean isSingleplayerEnabled(BOCRacePlugin plugin) {
        return getBoolean(plugin, "race.singleplayer-enabled", true);
    }
    
    /**
     * Check if multiplayer races are enabled
     */
    public static boolean isMultiplayerEnabled(BOCRacePlugin plugin) {
        return getBoolean(plugin, "race.multiplayer-enabled", true);
    }
    
    /**
     * Get the cooldown between races in seconds
     */
    public static int getRaceCooldown(BOCRacePlugin plugin) {
        return getInt(plugin, "race.cooldown", 5);
    }
    
    /**
     * Get the boat cleanup delay in seconds
     */
    public static int getBoatCleanupDelay(BOCRacePlugin plugin) {
        return getInt(plugin, "race.boat-cleanup-delay", 30);
    }
    
    /**
     * Get the storage type
     */
    public static String getStorageType(BOCRacePlugin plugin) {
        return getString(plugin, "storage.type", "yaml");
    }
    
    /**
     * Get the courses file path
     */
    public static String getCoursesFilePath(BOCRacePlugin plugin) {
        return getString(plugin, "storage.yaml.courses-file", "courses.yml");
    }
    
    /**
     * Get the stats file path
     */
    public static String getStatsFilePath(BOCRacePlugin plugin) {
        return getString(plugin, "storage.yaml.stats-file", "stats.yml");
    }
    
    /**
     * Check if PlaceholderAPI is enabled
     */
    public static boolean isPlaceholderAPIEnabled(BOCRacePlugin plugin) {
        return getBoolean(plugin, "placeholderapi.enabled", true);
    }
    
    /**
     * Get the PlaceholderAPI cache duration in seconds
     */
    public static int getPlaceholderAPICacheDuration(BOCRacePlugin plugin) {
        return getInt(plugin, "placeholderapi.cache-duration", 30);
    }
    
    /**
     * Get the maximum number of concurrent races
     */
    public static int getMaxConcurrentRaces(BOCRacePlugin plugin) {
        return getInt(plugin, "performance.max-concurrent-races", 5);
    }
    
    /**
     * Get the update tick rate
     */
    public static int getUpdateTickRate(BOCRacePlugin plugin) {
        return getInt(plugin, "performance.update-tick-rate", 5);
    }
    
    /**
     * Check if async operations are enabled
     */
    public static boolean isAsyncOperationsEnabled(BOCRacePlugin plugin) {
        return getBoolean(plugin, "performance.async-operations", true);
    }
}
