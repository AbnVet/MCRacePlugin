package com.bocrace;

import com.bocrace.model.Course;
import com.bocrace.model.PlayerStats;
import com.bocrace.model.RaceSession;
import com.bocrace.util.ConfigUtil;
import com.bocrace.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

/**
 * Handles all plugin events
 */
public class EventListener implements Listener {
    private final BOCRacePlugin plugin;
    
    public EventListener(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null) return;
        
        // Handle setup mode
        if (plugin.getSetupManager().isInSetupMode(player)) {
            handleSetupInteraction(event);
            return;
        }
        
        // Handle race interactions
        handleRaceInteraction(event);
    }
    
    /**
     * Handle interactions during setup mode
     */
    private void handleSetupInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        
        if (action != Action.RIGHT_CLICK_BLOCK) return;
        
        Location location = block.getLocation();
        
        // Check if it's a button
        if (isButton(block.getType())) {
            plugin.getSetupManager().handleButtonClick(player, location);
        } else {
            plugin.getSetupManager().handleBlockClick(player, location);
        }
        
        event.setCancelled(true);
    }
    
    /**
     * Handle interactions during races
     */
    private void handleRaceInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        
        if (action != Action.RIGHT_CLICK_BLOCK || block == null) return;
        
        Location location = block.getLocation();
        
        // Check if player clicked on a race button
        for (Course course : plugin.getStorageManager().getCourses().values()) {
            if (course.getType() != Course.CourseType.MULTIPLAYER) continue;
            
            // Check create race button
            if (isSameLocation(location, course.getCreateRaceButton())) {
                handleCreateRaceButton(player, course);
                event.setCancelled(true);
                return;
            }
            
            // Check join race button
            if (isSameLocation(location, course.getJoinRaceButton())) {
                handleJoinRaceButton(player, course);
                event.setCancelled(true);
                return;
            }
            
            // Check start race button
            if (isSameLocation(location, course.getStartRaceButton())) {
                handleStartRaceButton(player, course);
                event.setCancelled(true);
                return;
            }
            
            // Check return buttons
            if (isSameLocation(location, course.getReturnToLobbyButton())) {
                handleReturnToLobbyButton(player, course);
                event.setCancelled(true);
                return;
            }
            
            if (isSameLocation(location, course.getReturnToMainButton())) {
                handleReturnToMainButton(player, course);
                event.setCancelled(true);
                return;
            }
        }
        
        // Check singleplayer start button
        for (Course course : plugin.getStorageManager().getCourses().values()) {
            if (course.getType() != Course.CourseType.SINGLEPLAYER) continue;
            
            // For singleplayer, we'll use the return to lobby button as start button
            if (isSameLocation(location, course.getReturnToLobbyButton())) {
                handleSingleplayerStart(player, course);
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * Handle create race button click
     */
    private void handleCreateRaceButton(Player player, Course course) {
        if (course.getType() != Course.CourseType.MULTIPLAYER) return;
        
        // Check if there's already an active race
        RaceSession existingRace = plugin.getRaceManager().getCourseRace(course.getName());
        if (existingRace != null && existingRace.isActive()) {
            MessageUtil.sendMessage(player, "race.race-in-progress");
            return;
        }
        
        // Create new race session
        int maxPlayers = ConfigUtil.getMaxPlayersPerRace(plugin);
        RaceSession race = new RaceSession(course, RaceSession.RaceType.MULTIPLAYER, player.getUniqueId(), maxPlayers);
        plugin.getRaceManager().getActiveRaces().put(course.getName(), race);
        plugin.getRaceManager().getPlayerRaces().put(player.getUniqueId(), race);
        
        // Play button click sound
        Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
        player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
        
        MessageUtil.sendMessage(player, "race.lobby-created");
    }
    
    /**
     * Handle join race button click
     */
    private void handleJoinRaceButton(Player player, Course course) {
        plugin.getRaceManager().joinRace(player, course);
        
        // Play button click sound
        Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
        player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
    }
    
    /**
     * Handle start race button click
     */
    private void handleStartRaceButton(Player player, Course course) {
        RaceSession race = plugin.getRaceManager().getCourseRace(course.getName());
        if (race == null) {
            MessageUtil.sendMessage(player, "race.not-in-lobby");
            return;
        }
        
        if (!race.getLeader().equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "race.not-leader");
            return;
        }
        
        plugin.getRaceManager().startRace(player);
        
        // Play button click sound
        Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
        player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
    }
    
    /**
     * Handle return to lobby button click
     */
    private void handleReturnToLobbyButton(Player player, Course course) {
        Location lobbyLocation = course.getCourseLobby();
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
            
            // Play button click sound
            Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
            player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handle return to main button click
     */
    private void handleReturnToMainButton(Player player, Course course) {
        Location mainLobbyLocation = course.getMainLobby();
        if (mainLobbyLocation != null) {
            player.teleport(mainLobbyLocation);
            
            // Play button click sound
            Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
            player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
        } else {
            // Fallback to course lobby
            handleReturnToLobbyButton(player, course);
        }
    }
    
    /**
     * Handle singleplayer start
     */
    private void handleSingleplayerStart(Player player, Course course) {
        if (course.getType() != Course.CourseType.SINGLEPLAYER) return;
        
        // Check if player is in cooldown
        if (plugin.getRaceManager().isInCooldown(player)) {
            long remaining = plugin.getRaceManager().getCooldownRemaining(player);
            MessageUtil.sendMessage(player, "race.cooldown", java.util.Map.of("time", remaining));
            return;
        }
        
        // Check if course is valid
        if (!course.isComplete()) {
            MessageUtil.sendMessage(player, "error.course-not-valid", 
                java.util.Map.of("course", course.getName()));
            return;
        }
        
        // Start singleplayer race
        startSingleplayerRace(player, course);
        
        // Play button click sound
        Sound clickSound = ConfigUtil.getButtonClickSound(plugin);
        player.playSound(player.getLocation(), clickSound, 1.0f, 1.0f);
    }
    
    /**
     * Start a singleplayer race
     */
    private void startSingleplayerRace(Player player, Course course) {
        // Teleport to boat spawn
        Location spawnLocation = course.getRandomBoatSpawn();
        if (spawnLocation == null) {
            MessageUtil.sendMessage(player, "error.location-invalid");
            return;
        }
        
        player.teleport(spawnLocation);
        
        // Spawn boat and mount player
        Boat boat = spawnLocation.getWorld().spawn(spawnLocation, Boat.class);
        boat.addPassenger(player);
        
        // Create race session
        RaceSession race = new RaceSession(course, RaceSession.RaceType.SINGLEPLAYER, player.getUniqueId(), 1);
        race.setStatus(RaceSession.RaceStatus.ACTIVE);
        
        // Set player data
        RaceSession.PlayerRaceData playerData = race.getPlayerData(player.getUniqueId());
        playerData.setSpawnLocation(spawnLocation);
        playerData.setBoat(boat);
        playerData.setRaceStartTime(System.currentTimeMillis());
        
        // Store race session
        plugin.getRaceManager().getActiveRaces().put(course.getName() + "_" + player.getUniqueId(), race);
        plugin.getRaceManager().getPlayerRaces().put(player.getUniqueId(), race);
        
        // Play start sound
        Sound startSound = ConfigUtil.getRaceStartSound(plugin);
        player.playSound(player.getLocation(), startSound, 1.0f, 1.0f);
        
        MessageUtil.sendMessage(player, "race.singleplayer.started");
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle setup mode
        if (plugin.getSetupManager().isInSetupMode(player)) {
            plugin.getSetupManager().cancelSetup(player);
        }
        
        // Handle race sessions
        RaceSession race = plugin.getRaceManager().getPlayerRace(player);
        if (race != null) {
            if (race.isActive()) {
                // Disqualify player if race is active
                plugin.getRaceManager().disqualifyPlayer(race, player);
            } else {
                // Remove from lobby if race hasn't started
                plugin.getRaceManager().leaveRace(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Initialize player stats if they don't exist
        plugin.getStorageManager().getOrCreatePlayerStats(player.getUniqueId());
    }
    
    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) return;
        
        Player player = (Player) event.getExited();
        RaceSession race = plugin.getRaceManager().getPlayerRace(player);
        
        if (race != null && race.isActive()) {
            // Player exited boat during race - disqualify
            plugin.getRaceManager().disqualifyPlayer(race, player);
        }
    }
    
    
    /**
     * Check if a block type is a button
     */
    private boolean isButton(Material material) {
        return material == Material.STONE_BUTTON || 
               material == Material.OAK_BUTTON || 
               material == Material.SPRUCE_BUTTON || 
               material == Material.BIRCH_BUTTON || 
               material == Material.JUNGLE_BUTTON || 
               material == Material.ACACIA_BUTTON || 
               material == Material.DARK_OAK_BUTTON || 
               material == Material.CRIMSON_BUTTON || 
               material == Material.WARPED_BUTTON || 
               material == Material.POLISHED_BLACKSTONE_BUTTON;
    }
    
    /**
     * Check if two locations are the same
     */
    private boolean isSameLocation(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
}
