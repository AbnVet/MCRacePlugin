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
        
        // Check if player is in setup mode FIRST (highest priority)
        BOCRacePlugin.SetupMode setupMode = plugin.getPlayerSetupMode(player);
        if (setupMode != null) {
            handleSetupMode(event, player, setupMode);
            return;
        }
        
        // If not in setup mode, check for button clicks (race buttons)
        handleButtonClick(event, player);
    }
    
    private void handleButtonClick(PlayerInteractEvent event, Player player) {
        // Only handle right-clicks on blocks
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        // Check if it's a button
        if (!isButton(clickedBlock.getType())) return;
        
        plugin.raceDebugLog("🔘 Button clicked - Player: " + player.getName() + 
                           ", Button: " + clickedBlock.getType().name() + 
                           " at " + clickedBlock.getWorld().getName() + " " + 
                           clickedBlock.getX() + "," + clickedBlock.getY() + "," + clickedBlock.getZ());
        
        // Find course by start button
        Course course = findCourseByStartButton(clickedBlock);
        if (course != null) {
            plugin.raceDebugLog("✅ START BUTTON FOUND - Course: " + course.getName());
            event.setCancelled(true);
            
            // Simple boat spawn
            spawnRaceBoat(player, course);
            return;
        }
        
        // Find course by return button  
        Course returnCourse = findCourseByReturnButton(clickedBlock);
        if (returnCourse != null) {
            plugin.raceDebugLog("✅ RETURN BUTTON FOUND - Course: " + returnCourse.getName());
            event.setCancelled(true);
            
            // Simple teleport
            teleportToLobby(player, returnCourse);
            return;
        }
        
        plugin.raceDebugLog("❌ Button ignored - not a race button");
    }
    
    private void spawnRaceBoat(Player player, Course course) {
        plugin.raceDebugLog("🚤 SPAWNING BOAT - Player: " + player.getName() + ", Course: " + course.getName());
        
        if (course.getSpboatspawn() == null) {
            player.sendMessage("§cBoat spawn not set for this course!");
            return;
        }
        
        Location spawnLoc = course.getSpboatspawn().clone();
        spawnLoc.setX(spawnLoc.getBlockX() + 0.5);
        spawnLoc.setZ(spawnLoc.getBlockZ() + 0.5);
        spawnLoc.add(0, 1.0, 0);
        
        plugin.raceDebugLog("🚤 Boat spawn location: " + spawnLoc.getWorld().getName() + " " + 
                           spawnLoc.getX() + "," + spawnLoc.getY() + "," + spawnLoc.getZ() + 
                           " yaw:" + spawnLoc.getYaw() + " pitch:" + spawnLoc.getPitch());
        
        org.bukkit.entity.Boat boat = (org.bukkit.entity.Boat) spawnLoc.getWorld().spawnEntity(spawnLoc, org.bukkit.entity.EntityType.OAK_BOAT);
        boat.addPassenger(player);
        
        player.sendMessage("§a🚤 Boat spawned! You're ready to race!");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        plugin.raceDebugLog("✅ BOAT SPAWNED SUCCESSFULLY - UUID: " + boat.getUniqueId());
    }
    
    private void teleportToLobby(Player player, Course course) {
        plugin.raceDebugLog("🚀 TELEPORTING TO LOBBY - Player: " + player.getName() + ", Course: " + course.getName());
        
        Location destination = null;
        String locationName = "";
        
        if (course.getSpmainlobby() != null) {
            destination = course.getSpmainlobby().clone();
            locationName = "main lobby";
        } else if (course.getSpcourselobby() != null) {
            destination = course.getSpcourselobby().clone();
            locationName = "course lobby";
        } else {
            player.sendMessage("§cNo lobby locations set for this course!");
            return;
        }
        
        destination.setX(destination.getBlockX() + 0.5);
        destination.setZ(destination.getBlockZ() + 0.5);
        destination.add(0, 1.0, 0);
        
        player.teleport(destination);
        player.sendMessage("§aTeleported to " + locationName + "!");
        
        plugin.raceDebugLog("✅ TELEPORT SUCCESS - " + locationName);
    }
    
    private Course findCourseByStartButton(Block block) {
        for (Course course : plugin.getStorageManager().getCoursesByType(com.bocrace.model.CourseType.SINGLEPLAYER)) {
            if (course.getSpstartbutton() != null) {
                if (isSameBlock(course.getSpstartbutton(), block.getLocation())) {
                    return course;
                }
            }
        }
        return null;
    }
    
    private Course findCourseByReturnButton(Block block) {
        for (Course course : plugin.getStorageManager().getCoursesByType(com.bocrace.model.CourseType.SINGLEPLAYER)) {
            if (course.getSpreturn() != null) {
                if (isSameBlock(course.getSpreturn(), block.getLocation())) {
                    return course;
                }
            }
        }
        return null;
    }
    
    private boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               Math.abs(loc1.getBlockY() - loc2.getBlockY()) <= 1 && // Button face tolerance
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    private boolean isButton(org.bukkit.Material material) {
        return material.name().contains("BUTTON");
    }
    
    private void handleSetupMode(PlayerInteractEvent event, Player player, BOCRacePlugin.SetupMode setupMode) {
        
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
        
        // For boat spawn, use player's looking direction
        if (action.equals("setboatspawn")) {
            location.setYaw(player.getLocation().getYaw());
            location.setPitch(player.getLocation().getPitch());
            plugin.debugLog("Captured player direction for boat spawn - Yaw: " + location.getYaw() + ", Pitch: " + location.getPitch());
        }
        
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
