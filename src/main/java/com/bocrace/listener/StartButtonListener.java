package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import com.bocrace.race.ActiveRace;
import com.bocrace.util.BoatManager;
import com.bocrace.util.TeleportUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Handles start button clicks and race initiation
 */
public class StartButtonListener implements Listener {
    
    private final BOCRacePlugin plugin;
    private final BoatManager boatManager;
    private final TeleportUtil teleportUtil;
    
    public StartButtonListener(BOCRacePlugin plugin, BoatManager boatManager, TeleportUtil teleportUtil) {
        this.plugin = plugin;
        this.boatManager = boatManager;
        this.teleportUtil = teleportUtil;
    }
    
    @EventHandler
    public void onButtonClick(PlayerInteractEvent event) {
        // Only handle right-clicks on blocks with main hand
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        Player player = event.getPlayer();
        
        // DEBUG: Log all button clicks for troubleshooting
        if (isButton(clickedBlock.getType())) {
            plugin.raceDebugLog("Button clicked - Player: " + player.getName() + 
                               ", Button: " + clickedBlock.getType().name() + 
                               " at " + clickedBlock.getWorld().getName() + " " + 
                               clickedBlock.getX() + "," + clickedBlock.getY() + "," + clickedBlock.getZ());
        } else {
            return; // Not a button, ignore
        }
        
        // Find course by start button location
        Course course = findCourseByStartButton(clickedBlock);
        if (course != null) {
            // This is a start button
            plugin.raceDebugLog("Start button found for course: " + course.getName());
            handleRaceStart(player, course);
            return;
        }
        
        // Check if it's a return button
        Course returnCourse = findCourseByReturnButton(clickedBlock);
        if (returnCourse != null) {
            // This is a return button
            plugin.raceDebugLog("Return button found for course: " + returnCourse.getName());
            handleReturnButton(player, returnCourse);
            return;
        }
        
        plugin.raceDebugLog("Button click ignored - not a race button");
    }
    
    /**
     * Handle return button click
     */
    private void handleReturnButton(Player player, Course course) {
        plugin.raceDebugLog("Return button clicked - Player: " + player.getName() + ", Course: " + course.getName());
        
        // Cancel the button interaction
        // event.setCancelled(true); // Note: Can't cancel here since we don't have event reference
        
        // Teleport using lobby priority system
        teleportUtil.teleportToLobby(player, course, "return_button");
        
        plugin.raceDebugLog("Return button teleportation completed for " + player.getName());
    }
    
    /**
     * Handle a race start attempt
     */
    private void handleRaceStart(Player player, Course course) {
        // Cancel the button interaction to prevent redstone activation
        // event.setCancelled(true); // Note: We'll need to pass event reference
        
        plugin.raceDebugLog("Start button clicked - Player: " + player.getName() + ", Course: " + course.getName());
        // Check if player already has an active race
        if (plugin.getRaceManager().hasActiveRace(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active race!");
            plugin.debugLog("Race start denied - " + player.getName() + " already has active race");
            return;
        }
        
        // Check if course is occupied
        if (plugin.getRaceManager().isCourseOccupied(course.getName())) {
            player.sendMessage("§eTrack in use, please wait.");
            plugin.debugLog("Race start denied - course " + course.getName() + " is occupied");
            
            // Teleport to lobby if course is busy
            teleportUtil.teleportToLobby(player, course, "course_busy");
            return;
        }
        
        // Validate course is ready for racing
        if (!plugin.getRaceManager().isRaceReady(course)) {
            player.sendMessage("§cCourse '" + course.getName() + "' is not ready for racing! Please contact an admin.");
            plugin.debugLog("Race start denied - course " + course.getName() + " not ready for racing");
            return;
        }
        
        // Start the race
        ActiveRace race = plugin.getRaceManager().startRace(player, course);
        if (race == null) {
            player.sendMessage("§cFailed to start race! Please try again.");
            plugin.debugLog("Race start failed - RaceManager.startRace returned null for " + player.getName());
            return;
        }
        
        // Spawn the race boat
        Boat boat = boatManager.spawnRaceBoat(player, course, race);
        if (boat == null) {
            plugin.getRaceManager().endRace(player.getUniqueId(), ActiveRace.State.DQ, "boat_spawn_failed");
            player.sendMessage("§cFailed to spawn race boat! Please contact an admin.");
            plugin.debugLog("Race start failed - boat spawning failed for " + player.getName());
            return;
        }
        
        // Success! Play sound and send message
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        player.sendMessage("§e⚡ Get ready to race! Cross the start line to begin timing.");
        
        // Record course usage
        course.recordUsage(player.getName());
        plugin.getStorageManager().saveCourse(course);
        
        plugin.debugLog("Race started successfully - Player: " + player.getName() + 
                       ", Course: " + course.getName() + 
                       ", Boat: " + boat.getUniqueId() + 
                       ", Race: " + race.toString());
    }
    
    /**
     * Find a course by its start button location
     */
    private Course findCourseByStartButton(Block block) {
        plugin.raceDebugLog("🔍 Looking for course with start button at: " + formatLocation(block.getLocation()));
        
        for (Course course : plugin.getStorageManager().getCoursesByType(CourseType.SINGLEPLAYER)) {
            if (course.getSpstartbutton() != null) {
                plugin.raceDebugLog("🔍 Checking course " + course.getName() + " button at: " + formatLocation(course.getSpstartbutton()));
                
                if (isSameBlock(course.getSpstartbutton(), block.getLocation())) {
                    plugin.raceDebugLog("✅ START BUTTON MATCH FOUND - Course: " + course.getName());
                    return course;
                } else {
                    plugin.raceDebugLog("❌ No match for course " + course.getName());
                }
            } else {
                plugin.raceDebugLog("⚠️ Course " + course.getName() + " has no start button set");
            }
        }
        
        plugin.raceDebugLog("❌ NO COURSE FOUND for button at: " + formatLocation(block.getLocation()));
        return null;
    }
    
    /**
     * Find a course by its return button location
     */
    private Course findCourseByReturnButton(Block block) {
        for (Course course : plugin.getStorageManager().getCoursesByType(CourseType.SINGLEPLAYER)) {
            if (course.getSpreturn() != null) {
                if (isSameBlock(course.getSpreturn(), block.getLocation())) {
                    plugin.raceDebugLog("Return button match found - Course: " + course.getName() + 
                                       ", Button at: " + formatLocation(course.getSpreturn()) + 
                                       ", Clicked: " + formatLocation(block.getLocation()));
                    return course;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if two locations represent the same block (with button face tolerance)
     */
    private boolean isSameBlock(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        // Check if same world
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            plugin.raceDebugLog("Block comparison FAILED - different worlds");
            return false;
        }
        
        // Check X and Z coordinates (must be exact)
        boolean sameXZ = loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockZ() == loc2.getBlockZ();
        
        // Check Y coordinate with tolerance for button faces (±1 block)
        int yDiff = Math.abs(loc1.getBlockY() - loc2.getBlockY());
        boolean sameY = yDiff <= 1; // Allow 1 block difference for button faces
        
        boolean same = sameXZ && sameY;
        
        plugin.raceDebugLog("Block comparison - " + formatLocation(loc1) + " vs " + formatLocation(loc2) + 
                           " | XZ match: " + sameXZ + ", Y diff: " + yDiff + " | Result: " + same);
        return same;
    }
    
    /**
     * Format a location for debug logging
     */
    private String formatLocation(org.bukkit.Location loc) {
        if (loc == null) return "null";
        return String.format("%s %d,%d,%d", 
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    /**
     * Check if a material is a button
     */
    private boolean isButton(Material material) {
        switch (material) {
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case BIRCH_BUTTON:
            case JUNGLE_BUTTON:
            case ACACIA_BUTTON:
            case DARK_OAK_BUTTON:
            case MANGROVE_BUTTON:
            case CHERRY_BUTTON:
            case BAMBOO_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:
            case STONE_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
                return true;
            default:
                return false;
        }
    }
}
