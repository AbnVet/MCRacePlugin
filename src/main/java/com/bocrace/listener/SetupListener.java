package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.util.SoundEffectManager;
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
    private final SoundEffectManager soundEffectManager;
    
    public SetupListener(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.soundEffectManager = plugin.getSoundEffectManager();
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
            spawnRaceBoat(player, course, clickedBlock);
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
    
    private void spawnRaceBoat(Player player, Course course, Block clickedBlock) {
        plugin.raceDebugLog("🚤 SPAWNING RACE BOAT - Player: " + player.getName() + ", Course: " + course.getName());
        
        // Check if course is ready for racing
        if (!plugin.getRaceManager().isRaceReady(course)) {
            player.sendMessage("§cCourse not ready for racing! Missing required components.");
            plugin.raceDebugLog("❌ Course not ready - missing components");
            return;
        }
        
        // Check if player already has active race
        if (plugin.getRaceManager().hasActiveRace(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active race!");
            plugin.raceDebugLog("❌ Player already has active race");
            return;
        }
        
        // Check if course is occupied - behavior depends on which button was clicked
        if (plugin.getRaceManager().isCourseOccupied(course.getName())) {
            // Determine which button was clicked
            boolean isMainLobbyButton = course.getSpmainlobbybutton() != null && 
                isSameBlock(course.getSpmainlobbybutton(), clickedBlock.getLocation());
            
            plugin.raceDebugLog("❌ Course occupied - Button type: " + (isMainLobbyButton ? "mainlobby" : "courselobby"));
            
            if (isMainLobbyButton) {
                // Main lobby button: teleport to course lobby to wait
                player.sendMessage("§eCourse is in use. Please wait in the course lobby.");
                teleportToCourseLobby(player, course);
            } else {
                // Course lobby button: just message (they're already at course)
                player.sendMessage("§eCourse is in use, wait until race is finished.");
            }
            return;
        }
        
        // Determine which button was clicked to track for finish teleport
        boolean isMainLobbyButton = course.getSpmainlobbybutton() != null && 
            isSameBlock(course.getSpmainlobbybutton(), clickedBlock.getLocation());
        String buttonType = isMainLobbyButton ? "mainlobby" : "courselobby";
        
        // Start the race
        com.bocrace.race.ActiveRace race = plugin.getRaceManager().startRace(player, course);
        if (race == null) {
            player.sendMessage("§cFailed to start race!");
            plugin.raceDebugLog("❌ Race start failed");
            return;
        }
        
        // Track which button was used for proper finish/DQ teleport
        race.setStartButtonType(buttonType);
        plugin.raceDebugLog("🔘 Button type recorded: " + buttonType);
        
        // Spawn boat with PDC tracking
        org.bukkit.entity.Boat boat = plugin.getBoatManager().spawnRaceBoat(player, course, race);
        if (boat == null) {
            plugin.getRaceManager().endRace(player.getUniqueId(), com.bocrace.race.ActiveRace.State.DQ, "boat_spawn_failed");
            player.sendMessage("§cFailed to spawn boat!");
            plugin.raceDebugLog("❌ Boat spawn failed");
            return;
        }
        
        // Success messages and sounds
        player.sendMessage("§e⚡ Get ready to race! Cross the start line to begin timing.");
        soundEffectManager.playBoatSpawnEffects(player, player.getLocation());
        
        // Record course usage
        course.recordUsage(player.getName());
        plugin.getStorageManager().saveCourse(course);
        
        plugin.raceDebugLog("✅ RACE STARTED SUCCESSFULLY - Player: " + player.getName() + 
                           ", Course: " + course.getName() + ", Boat: " + boat.getUniqueId());
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
    
    private void teleportToCourseLobby(Player player, Course course) {
        plugin.raceDebugLog("🚀 TELEPORTING TO COURSE LOBBY - Player: " + player.getName() + ", Course: " + course.getName());
        
        if (course.getSpcourselobby() == null) {
            player.sendMessage("§cCourse lobby location not set for this course!");
            return;
        }
        
        Location destination = course.getSpcourselobby().clone();
        destination.setX(destination.getBlockX() + 0.5);
        destination.setZ(destination.getBlockZ() + 0.5);
        destination.add(0, 1.0, 0);
        
        player.teleport(destination);
        plugin.raceDebugLog("✅ TELEPORT SUCCESS - course lobby");
    }
    
    private Course findCourseByStartButton(Block block) {
        for (Course course : plugin.getStorageManager().getCoursesByType(com.bocrace.model.CourseType.SINGLEPLAYER)) {
            // Check main lobby button
            if (course.getSpmainlobbybutton() != null) {
                if (isSameBlock(course.getSpmainlobbybutton(), block.getLocation())) {
                    return course;
                }
            }
            // Check course lobby button
            if (course.getSpcourselobbybutton() != null) {
                if (isSameBlock(course.getSpcourselobbybutton(), block.getLocation())) {
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
        
        // For boat spawn and lobby locations, use player's looking direction
        if (action.equals("setboatspawn") || action.equals("setmainlobby") || action.equals("setcourselobby")) {
            location.setYaw(player.getLocation().getYaw());
            location.setPitch(player.getLocation().getPitch());
            plugin.debugLog("Captured player direction for " + action + " - Yaw: " + location.getYaw() + ", Pitch: " + location.getPitch());
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
            case "setmainlobbybutton":
                course.setSpmainlobbybutton(location);
                success = true;
                player.sendMessage("§aMain lobby start button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Main lobby start button location set: " + location.toString());
                break;
                
            case "setcourselobbybutton":
                course.setSpcourselobbybutton(location);
                success = true;
                player.sendMessage("§aCourse lobby start button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Course lobby start button location set: " + location.toString());
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
                
            case "setreturnmainbutton":
                course.setSpreturn(location);
                success = true;
                player.sendMessage("§aReturn main button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Return main button location set: " + location.toString());
                break;
                
            // MULTIPLAYER SETUP ACTIONS
            case "setmpracelobbyspawn":
                course.setMpraceLobbySpawn(location);
                success = true;
                player.sendMessage("§aMultiplayer race lobby spawn set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] MP race lobby spawn set: " + location.toString());
                break;
                
            case "setmpcreateracebutton":
                course.setMpcreateRaceButton(location);
                success = true;
                player.sendMessage("§aCreate race button location set for course '" + courseName + "'!");
                player.sendMessage("§7This button creates multiplayer race sessions (OP only)");
                plugin.getLogger().info("[DEBUG] MP create race button set: " + location.toString());
                break;
                
            case "setmpstartracebutton":
                course.setMpstartRaceButton(location);
                success = true;
                player.sendMessage("§aStart race button location set for course '" + courseName + "'!");
                player.sendMessage("§7This button starts races AND triggers redstone!");
                plugin.getLogger().info("[DEBUG] MP start race button set: " + location.toString());
                break;
                
            case "setmpjoinracebutton":
                course.setMpjoinRaceButton(location);
                success = true;
                player.sendMessage("§aJoin race button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] MP join race button set: " + location.toString());
                break;
                
            case "setmpcancelracebutton":
                course.setMpcancelRaceButton(location);
                success = true;
                player.sendMessage("§aCancel race button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] MP cancel race button set: " + location.toString());
                break;
                
            case "setmpreturnbutton":
                course.setMpreturnButton(location);
                success = true;
                player.sendMessage("§aReturn to lobby button location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] MP return button set: " + location.toString());
                break;
                
            case "setcourselobbyspawn":
                course.setSpcourselobby(location);
                success = true;
                player.sendMessage("§aCourse lobby spawn location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Course lobby spawn location set: " + location.toString());
                break;
                
            case "setmainlobbyspawn":
                course.setSpmainlobby(location);
                success = true;
                player.sendMessage("§aMain lobby spawn location set for course '" + courseName + "'!");
                plugin.getLogger().info("[DEBUG] Main lobby spawn location set: " + location.toString());
                break;
                
            default:
                // Check if it's a multiplayer boat spawn (setmpboatspawn1, setmpboatspawn2, etc.)
                if (action.startsWith("setmpboatspawn")) {
                    try {
                        String indexStr = action.substring("setmpboatspawn".length());
                        int spawnIndex = Integer.parseInt(indexStr);
                        
                        if (spawnIndex >= 1 && spawnIndex <= 10) {
                            // Ensure list has enough capacity
                            while (course.getMpboatSpawns().size() < spawnIndex) {
                                course.getMpboatSpawns().add(null);
                            }
                            
                            // Set the spawn at the correct index (0-based)
                            course.getMpboatSpawns().set(spawnIndex - 1, location);
                            success = true;
                            player.sendMessage("§aMultiplayer boat spawn #" + spawnIndex + " set for course '" + courseName + "'!");
                            plugin.getLogger().info("[DEBUG] MP boat spawn " + spawnIndex + " set: " + location.toString());
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Fall through to default error
                    }
                }
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
