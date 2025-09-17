package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import com.bocrace.race.MultiplayerRace;
import com.bocrace.race.MultiplayerRaceManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

/**
 * Handles multiplayer race button interactions
 * Supports: Create Race, Join Race, Start Race, Cancel Race, Return to Lobby
 * Includes redstone integration for start button
 */
public class MultiplayerButtonListener implements Listener {
    
    private final BOCRacePlugin plugin;
    private final MultiplayerRaceManager raceManager;
    
    public MultiplayerButtonListener(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.raceManager = plugin.getMultiplayerRaceManager();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Player player = event.getPlayer();
        Location blockLocation = clickedBlock.getLocation();
        
        // Find course with matching button location
        Course course = findCourseByButtonLocation(blockLocation);
        if (course == null || course.getType() != CourseType.MULTIPLAYER) {
            return;
        }
        
        // Determine button type and handle accordingly
        ButtonType buttonType = getButtonType(course, blockLocation);
        if (buttonType == null) {
            return;
        }
        
        // Cancel the event to prevent other interactions
        event.setCancelled(true);
        
        // Handle button press based on type
        switch (buttonType) {
            case CREATE_RACE:
                handleCreateRaceButton(player, course);
                break;
            case JOIN_RACE:
                handleJoinRaceButton(player, course);
                break;
            case START_RACE:
                handleStartRaceButton(player, course, clickedBlock);
                break;
            case CANCEL_RACE:
                handleCancelRaceButton(player, course);
                break;
            case RETURN_LOBBY:
                handleReturnLobbyButton(player, course);
                break;
        }
        
        plugin.debugLog("Player " + player.getName() + " pressed " + buttonType + 
                       " button on course " + course.getName());
    }
    
    /**
     * Handle Create Race button press
     */
    private void handleCreateRaceButton(Player player, Course course) {
        // Check permissions
        if (!player.isOp()) {
            player.sendMessage("§c§l❌ Only OPs can create multiplayer races!");
            return;
        }
        
        // Check if course already has an active race
        if (raceManager.isCourseOccupied(course.getName())) {
            player.sendMessage("§c§l❌ Race already in progress on this course!");
            return;
        }
        
        // Check if player is already in a race
        if (raceManager.isPlayerInRace(player.getUniqueId())) {
            player.sendMessage("§c§l❌ You are already in a multiplayer race!");
            return;
        }
        
        // Validate course setup
        if (!isValidMultiplayerCourse(course)) {
            player.sendMessage("§c§l❌ Course is not properly set up for multiplayer!");
            player.sendMessage("§7Use /bocrace multiplayer setup " + course.getName() + " to configure.");
            return;
        }
        
        // Create the race
        MultiplayerRace race = raceManager.createRace(course, player);
        if (race == null) {
            player.sendMessage("§c§l❌ Failed to create race! Please try again.");
            return;
        }
        
        // Success message
        player.sendMessage("§a§l🏁 MULTIPLAYER RACE CREATED!");
        player.sendMessage("§7Course: §a" + course.getName());
        player.sendMessage("§7Players can now join using the Join Race button.");
        player.sendMessage("§7Press the Start Race button when ready to begin!");
        
        // Play success effects
        plugin.getSoundEffectManager().playSetupSuccessEffects(player, player.getLocation());
    }
    
    /**
     * Handle Join Race button press
     */
    private void handleJoinRaceButton(Player player, Course course) {
        // Check if there's an active race
        MultiplayerRace race = raceManager.getRaceByCourse(course.getName());
        if (race == null) {
            player.sendMessage("§c§l❌ No active race on this course!");
            player.sendMessage("§7Wait for someone to create a race first.");
            return;
        }
        
        // Check if race is in lobby state
        if (race.getState() != MultiplayerRace.State.LOBBY) {
            player.sendMessage("§c§l❌ Race has already started!");
            return;
        }
        
        // Check if player is already in a race
        if (raceManager.isPlayerInRace(player.getUniqueId())) {
            player.sendMessage("§c§l❌ You are already in a multiplayer race!");
            return;
        }
        
        // Check if race is full
        if (!race.canJoin()) {
            int maxPlayers = plugin.getConfig().getInt("multiplayer.max-join-players", 9);
            player.sendMessage("§c§l❌ Race is full! (" + maxPlayers + " players maximum)");
            return;
        }
        
        // Join the race
        boolean joined = raceManager.joinRace(course, player);
        if (!joined) {
            player.sendMessage("§c§l❌ Failed to join race! It may be full or starting.");
            return;
        }
        
        // Success - player is teleported and boat spawned by race manager
        player.sendMessage("§7Race Leader: §a" + race.getLeaderName());
        player.sendMessage("§7Players: §e" + race.getPlayerCount() + " §7joined");
    }
    
    /**
     * Handle Start Race button press (includes redstone trigger)
     */
    private void handleStartRaceButton(Player player, Course course, Block clickedBlock) {
        // Check if there's an active race
        MultiplayerRace race = raceManager.getRaceByCourse(course.getName());
        if (race == null) {
            player.sendMessage("§c§l❌ No active race on this course!");
            player.sendMessage("§7Create a race first using the Create Race button.");
            return;
        }
        
        // Check if player is the race leader
        if (!race.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c§l❌ Only the race leader can start the race!");
            player.sendMessage("§7Race Leader: §a" + race.getLeaderName());
            return;
        }
        
        // Check if race is in lobby state
        if (race.getState() != MultiplayerRace.State.LOBBY) {
            player.sendMessage("§c§l❌ Race has already started or ended!");
            return;
        }
        
        // Check if there are enough players (at least 1 other + leader = 2 minimum)
        if (race.getPlayerCount() < 1) {
            player.sendMessage("§c§l❌ Need at least 1 other player to start the race!");
            player.sendMessage("§7Current players: §e" + race.getPlayerCount());
            return;
        }
        
        // TRIGGER REDSTONE FIRST (before starting race logic)
        triggerRedstone(clickedBlock);
        
        // Start the race
        boolean started = raceManager.startRace(course, player);
        if (!started) {
            player.sendMessage("§c§l❌ Failed to start race! No available boat spawn.");
            return;
        }
        
        // Success - race manager handles player teleportation and announcements
        player.sendMessage("§a§l🚀 RACE STARTED! §a§lCross the start line to begin timing!");
    }
    
    /**
     * Handle Cancel Race button press
     */
    private void handleCancelRaceButton(Player player, Course course) {
        // Check if there's an active race
        MultiplayerRace race = raceManager.getRaceByCourse(course.getName());
        if (race == null) {
            player.sendMessage("§c§l❌ No active race on this course!");
            return;
        }
        
        // Check if player is the race leader
        if (!race.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage("§c§l❌ Only the race leader can cancel the race!");
            player.sendMessage("§7Race Leader: §a" + race.getLeaderName());
            return;
        }
        
        // Check if race can be cancelled
        if (race.getState() == MultiplayerRace.State.FINISHED || 
            race.getState() == MultiplayerRace.State.CANCELLED) {
            player.sendMessage("§c§l❌ Race has already ended!");
            return;
        }
        
        // Cancel the race
        boolean cancelled = raceManager.cancelRace(course, player);
        if (!cancelled) {
            player.sendMessage("§c§l❌ Failed to cancel race!");
            return;
        }
        
        // Success message
        player.sendMessage("§c§l❌ RACE CANCELLED!");
        player.sendMessage("§7All players have been returned to the race lobby.");
        
        // Play error sound
        plugin.getSoundEffectManager().playErrorEffects(player, player.getLocation());
    }
    
    /**
     * Handle Return to Lobby button press
     */
    private void handleReturnLobbyButton(Player player, Course course) {
        // Check if player is in a race on this course
        MultiplayerRace race = raceManager.getRaceByPlayer(player.getUniqueId());
        if (race != null && !race.getCourse().getName().equals(course.getName())) {
            player.sendMessage("§c§l❌ You are in a race on a different course!");
            return;
        }
        
        // Teleport to race lobby spawn
        Location lobbySpawn = course.getMpraceLobbySpawn();
        if (lobbySpawn == null) {
            player.sendMessage("§c§l❌ Race lobby spawn not set for this course!");
            return;
        }
        
        // If player is in an active race, disqualify them
        if (race != null && race.getState() == MultiplayerRace.State.RUNNING) {
            raceManager.disqualifyPlayer(player.getUniqueId(), "Player returned to lobby");
        }
        
        // Teleport player
        player.teleport(lobbySpawn);
        player.sendMessage("§a§l🏠 Returned to race lobby!");
        
        // Play teleport sound
        plugin.getSoundEffectManager().playSetupEnterEffects(player, player.getLocation());
    }
    
    /**
     * Trigger redstone signal at the button location
     */
    private void triggerRedstone(Block buttonBlock) {
        // Set block to powered state briefly
        Material originalMaterial = buttonBlock.getType();
        
        // Power the block for redstone signal
        buttonBlock.setType(Material.REDSTONE_BLOCK);
        
        // Schedule to revert back after 1 tick (immediate power pulse)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (buttonBlock.getType() == Material.REDSTONE_BLOCK) {
                buttonBlock.setType(originalMaterial);
            }
        }, 1L);
        
        plugin.debugLog("Triggered redstone at " + buttonBlock.getLocation() + 
                       " (was " + originalMaterial + ")");
    }
    
    /**
     * Find course by button location
     */
    private Course findCourseByButtonLocation(Location location) {
        for (Course course : plugin.getStorageManager().getCoursesByType(CourseType.MULTIPLAYER)) {
            if (isLocationMatch(course.getMpcreateRaceButton(), location) ||
                isLocationMatch(course.getMpjoinRaceButton(), location) ||
                isLocationMatch(course.getMpstartRaceButton(), location) ||
                isLocationMatch(course.getMpcancelRaceButton(), location) ||
                isLocationMatch(course.getMpreturnButton(), location)) {
                return course;
            }
        }
        return null;
    }
    
    /**
     * Get button type for the clicked location
     */
    private ButtonType getButtonType(Course course, Location location) {
        if (isLocationMatch(course.getMpcreateRaceButton(), location)) {
            return ButtonType.CREATE_RACE;
        }
        if (isLocationMatch(course.getMpjoinRaceButton(), location)) {
            return ButtonType.JOIN_RACE;
        }
        if (isLocationMatch(course.getMpstartRaceButton(), location)) {
            return ButtonType.START_RACE;
        }
        if (isLocationMatch(course.getMpcancelRaceButton(), location)) {
            return ButtonType.CANCEL_RACE;
        }
        if (isLocationMatch(course.getMpreturnButton(), location)) {
            return ButtonType.RETURN_LOBBY;
        }
        return null;
    }
    
    /**
     * Check if two locations match (same world and coordinates)
     */
    private boolean isLocationMatch(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return false;
        }
        
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
    
    /**
     * Validate course has required multiplayer components
     */
    private boolean isValidMultiplayerCourse(Course course) {
        return course.getMpraceLobbySpawn() != null &&
               course.getMpcreateRaceButton() != null &&
               course.getMpstartRaceButton() != null &&
               course.getMpjoinRaceButton() != null &&
               course.getSpstart1() != null &&
               course.getSpstart2() != null &&
               course.getSpfinish1() != null &&
               course.getSpfinish2() != null &&
               course.getMpboatSpawns() != null &&
               course.getMpboatSpawns().size() >= 2; // At least 2 boat spawns
    }
    
    /**
     * Button types for multiplayer races
     */
    private enum ButtonType {
        CREATE_RACE,    // Leader creates race session
        JOIN_RACE,      // Players join race lobby
        START_RACE,     // Leader starts race + triggers redstone
        CANCEL_RACE,    // Leader cancels race (optional)
        RETURN_LOBBY    // Return to race lobby spawn
    }
}
