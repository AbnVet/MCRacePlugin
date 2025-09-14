package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/**
 * Utility class for handling messages with MiniMessage formatting
 */
public class MessageUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static FileConfiguration messagesConfig;
    
    /**
     * Initialize the message system
     */
    public static void initialize(BOCRacePlugin plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Send a message to a command sender
     */
    public static void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Map.of());
    }
    
    /**
     * Send a message to a command sender with placeholders
     */
    public static void sendMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        String message = getMessage(key);
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // Replace placeholders
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        // Parse and send the message
        Component component = MINI_MESSAGE.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Get a message from the configuration
     */
    public static String getMessage(String key) {
        if (messagesConfig == null) {
            return "Message system not initialized";
        }
        
        String message = messagesConfig.getString(key);
        if (message == null) {
            return "Message not found: " + key;
        }
        
        // Replace prefix placeholder
        String prefix = messagesConfig.getString("general.prefix", "<gradient:#00ff00:#00ffff>BOCRace</gradient>");
        message = message.replace("{prefix}", prefix);
        
        return message;
    }
    
    /**
     * Get a message with placeholders replaced
     */
    public static String getMessage(String key, Map<String, Object> placeholders) {
        String message = getMessage(key);
        
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        return message;
    }
    
    /**
     * Parse a MiniMessage string to a Component
     */
    public static Component parseMessage(String message) {
        return MINI_MESSAGE.deserialize(message);
    }
    
    /**
     * Parse a MiniMessage string with placeholders to a Component
     */
    public static Component parseMessage(String message, Map<String, Object> placeholders) {
        // Replace placeholders
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        
        return MINI_MESSAGE.deserialize(message);
    }
    
    /**
     * Format a time in milliseconds to a readable string
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds == 0) return "N/A";
        
        double seconds = milliseconds / 1000.0;
        int minutes = (int) (seconds / 60);
        seconds = seconds % 60;
        
        return String.format("%d:%05.2f", minutes, seconds);
    }
    
    /**
     * Format a time in seconds to a readable string
     */
    public static String formatTime(double seconds) {
        if (seconds == 0) return "N/A";
        
        int minutes = (int) (seconds / 60);
        seconds = seconds % 60;
        
        return String.format("%d:%05.2f", minutes, seconds);
    }
    
    /**
     * Get the prefix from configuration
     */
    public static String getPrefix() {
        return messagesConfig != null ? 
            messagesConfig.getString("general.prefix", "<gradient:#00ff00:#00ffff>BOCRace</gradient>") : 
            "<gradient:#00ff00:#00ffff>BOCRace</gradient>";
    }
    
    /**
     * Reload the messages configuration
     */
    public static void reload(BOCRacePlugin plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
