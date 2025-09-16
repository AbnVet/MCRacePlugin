package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SetupListener implements Listener {
    
    private final BOCRacePlugin plugin;
    
    public SetupListener(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in setup mode
        BOCRacePlugin.SetupMode setupMode = plugin.getPlayerSetupMode(player);
        if (setupMode == null) {
            return; // Player not in setup mode
        }
        
        // Check if setup mode has expired
        if (setupMode.isExpired()) {
            plugin.clearPlayerSetupMode(player);
            player.sendMessage("§cSetup mode expired. Please run the setup command again.");
            plugin.getLogger().info("[DEBUG] Setup mode expired for player: " + player.getName());
            return;
        }
        
        // Only handle right-click events
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Location location = clickedBlock.getLocation();
        String courseName = setupMode.getCourseName();
        String action = setupMode.getAction();
        
        plugin.getLogger().info("[DEBUG] Right-click captured - Player: " + player.getName() + 
            ", Action: " + action + ", Course: " + courseName + 
            ", Location: " + location.getWorld().getName() + " " + 
            location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        
        // Get the course
        Course course = plugin.getStorageManager().getCourse(courseName);
        if (course == null) {
            player.sendMessage("§cCourse '" + courseName + "' not found!");
            plugin.clearPlayerSetupMode(player);
            plugin.getLogger().info("[DEBUG] Course not found during setup: " + courseName);
            return;
        }
        
        // Handle different setup actions
        boolean success = false;
        switch (action) {
            case "setstartbutton":
                course.setSpstartbutton(location);
                success = true;
                player.sendMessage("§aStart button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Start button location set: " + location.toString());
                break;
                
            case "setboatspawn":
                course.setSpboatspawn(location);
                success = true;
                player.sendMessage("§aBoat spawn location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Boat spawn location set: " + location.toString());
                break;
                
            case "setstart1":
                course.setSpstart1(location);
                success = true;
                player.sendMessage("§aStart line point 1 set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Start line point 1 set: " + location.toString());
                break;
                
            case "setstart2":
                course.setSpstart2(location);
                success = true;
                player.sendMessage("§aStart line point 2 set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Start line point 2 set: " + location.toString());
                break;
                
            case "setfinish1":
                course.setSpfinish1(location);
                success = true;
                player.sendMessage("§aFinish line point 1 set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Finish line point 1 set: " + location.toString());
                break;
                
            case "setfinish2":
                course.setSpfinish2(location);
                success = true;
                player.sendMessage("§aFinish line point 2 set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Finish line point 2 set: " + location.toString());
                break;
                
            case "setreturn":
                course.setSpreturn(location);
                success = true;
                player.sendMessage("§aReturn location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Return location set: " + location.toString());
                break;
                
            case "setcourselobby":
                course.setSpcourselobby(location);
                success = true;
                player.sendMessage("§aCourse lobby location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Course lobby location set: " + location.toString());
                break;
                
            case "setmainlobby":
                course.setSpmainlobby(location);
                success = true;
                player.sendMessage("§aMain lobby location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Main lobby location set: " + location.toString());
                break;
                
                
            default:
                player.sendMessage("§cUnknown setup action: " + action);
                plugin.getLogger().info("[DEBUG] Unknown setup action: " + action);
                return;
        }
        
        if (success) {
            // Save the course
            plugin.getStorageManager().saveCourse(course);
            plugin.getLogger().info("[DEBUG] Course saved after location update: " + courseName);
            
            // Visual feedback - particles and sound
            Location particleLocation = location.add(0.5, 1, 0.5); // Center above block
            player.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                particleLocation,
                10, 0.3, 0.3, 0.3, 0.01
            );
            
            // Sound effect
            player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            
            plugin.getLogger().info("[DEBUG] Visual feedback sent - particles and sound");
            
            // Clear setup mode
            plugin.clearPlayerSetupMode(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear setup mode when player leaves
        plugin.clearPlayerSetupMode(event.getPlayer());
        plugin.getLogger().info("[DEBUG] Cleared setup mode for disconnected player: " + event.getPlayer().getName());
    }
}
