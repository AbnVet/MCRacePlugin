package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.race.ActiveRace;
import com.bocrace.race.MultiplayerRace;
import com.bocrace.util.BoatManager;
import com.bocrace.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.UUID;

/**
 * Handles race cleanup for disconnects and boat exits
 */
public class RaceCleanupListener implements Listener {
    
    private final BOCRacePlugin plugin;
    private final BoatManager boatManager;
    private final TeleportUtil teleportUtil;
    
    public RaceCleanupListener(BOCRacePlugin plugin, BoatManager boatManager, TeleportUtil teleportUtil) {
        this.plugin = plugin;
        this.boatManager = boatManager;
        this.teleportUtil = teleportUtil;
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Check if player has an active singleplayer race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null) {
            plugin.debugLog("Player disconnected during singleplayer race - Player: " + player.getName() + 
                           ", Course: " + race.getCourseName() + 
                           ", State: " + race.getState() + 
                           ", Duration: " + race.getCurrentDurationMs() + "ms");
            handleSingleplayerDisconnect(player, race);
            return;
        }
        
        // Check if player has an active multiplayer race
        if (plugin.getMultiplayerRaceManager().isPlayerInRace(playerUuid)) {
            plugin.debugLog("Player disconnected during multiplayer race - Player: " + player.getName());
            handleMultiplayerDisconnect(player);
            return;
        }
    }
    
    private void handleSingleplayerDisconnect(Player player, ActiveRace race) {
        UUID playerUuid = player.getUniqueId();
        
        // Find and remove the race boat
        Boat boat = boatManager.findRaceBoatByPlayer(playerUuid);
        if (boat != null) {
            boatManager.removeRaceBoat(boat, "player_disconnected");
        }
        
        // End the race as DQ
        String dqMessage = "Disconnected";
        plugin.getRaceManager().endRace(playerUuid, ActiveRace.State.DQ, dqMessage);
        
        // Save DQ record if they were actually racing
        if (race.getState() == ActiveRace.State.RUNNING) {
            double timeSeconds = race.getCurrentDurationMs() / 1000.0;
            
            // Save as DQ record (negative time indicates DQ)
            plugin.getRecordManager().saveRaceRecord(
                race.getPlayerName() + " (DQ - " + dqMessage + ")", 
                race.getCourseName(), 
                -timeSeconds, // Negative indicates DQ
                race.getCourseType()
            );
            
            plugin.debugLog("DQ record saved - Player: " + race.getPlayerName() + 
                           ", Time: " + race.getFormattedDqMessage());
        }
        
        plugin.debugLog("Race cleanup completed for disconnected player: " + player.getName());
    }
    
    private void handleMultiplayerDisconnect(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Find and remove the race boat
        Boat boat = boatManager.findRaceBoatByPlayer(playerUuid);
        if (boat != null) {
            boatManager.removeRaceBoat(boat, "player_disconnected");
        }
        
        // Handle disconnect through multiplayer race manager
        plugin.getMultiplayerRaceManager().handlePlayerDisconnect(playerUuid);
        
        plugin.debugLog("Multiplayer race cleanup completed for disconnected player: " + player.getName());
    }
    
    @EventHandler
    public void onBoatExit(VehicleExitEvent event) {
        // Only handle boats
        if (!(event.getVehicle() instanceof Boat)) return;
        
        // Only handle players
        if (!(event.getExited() instanceof Player)) return;
        
        Boat boat = (Boat) event.getVehicle();
        Player player = (Player) event.getExited();
        
        // Only handle race boats
        if (!boatManager.isRaceBoat(boat)) return;
        
        UUID playerUuid = player.getUniqueId();
        
        // Check if player has an active singleplayer race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null) {
            handleSingleplayerBoatExit(player, race, boat);
            return;
        }
        
        // Check if player has an active multiplayer race
        if (plugin.getMultiplayerRaceManager().isPlayerInRace(playerUuid)) {
            handleMultiplayerBoatExit(player, boat);
            return;
        }
    }
    
    private void handleSingleplayerBoatExit(Player player, ActiveRace race, Boat boat) {
        UUID playerUuid = player.getUniqueId();
        plugin.debugLog("Player exited singleplayer race boat - Player: " + player.getName() + 
                       ", Course: " + race.getCourseName() + 
                       ", State: " + race.getState() + 
                       ", Duration: " + race.getCurrentDurationMs() + "ms");
        
        // Don't DQ if race is already finished - this is normal boat cleanup
        if (race.getState() == ActiveRace.State.FINISHED) {
            plugin.debugLog("🚫 IGNORING boat exit - race already finished normally");
            return;
        }
        
        // End the race as DQ (only if not already finished)
        String dqMessage = "Exited boat";
        plugin.getRaceManager().endRace(playerUuid, ActiveRace.State.DQ, dqMessage);
        
        // Save DQ record if they were actually racing
        if (race.getState() == ActiveRace.State.RUNNING) {
            double timeSeconds = race.getCurrentDurationMs() / 1000.0;
            
            // Save as DQ record (negative time indicates DQ)
            plugin.getRecordManager().saveRaceRecord(
                race.getPlayerName() + " (DQ - " + dqMessage + ")", 
                race.getCourseName(), 
                -timeSeconds, // Negative indicates DQ
                race.getCourseType()
            );
            
            plugin.debugLog("DQ record saved - Player: " + race.getPlayerName() + 
                           ", Time: " + race.getFormattedDqMessage());
        }
        
        // Remove the boat
        boatManager.removeRaceBoat(boat, "player_exited");
        
        // DQ teleport based on button type used
        Course course = plugin.getStorageManager().getCourse(race.getCourseName());
        if (course != null) {
            plugin.debugLog("🚫 DQ TELEPORT - Player: " + player.getName() + " exited boat, Button type: " + race.getStartButtonType());
            player.sendMessage("§c§l❌ DISQUALIFIED! §cYou exited your boat. Race ended.");
            
            // Teleport based on which button was used to start the race
            if ("mainlobby".equals(race.getStartButtonType())) {
                // Started from main lobby → return to main lobby
                teleportUtil.teleportToLobby(player, course, "dq_silent");
            } else {
                // Started from course lobby → return to course lobby
                teleportUtil.teleportToCourseLobby(player, course, "dq_silent");
            }
        } else {
            // Course not found, use pre-race location or world spawn
            Location safeLocation = race.getPreRaceLocation();
            if (safeLocation == null || !TeleportUtil.isSafeLocation(safeLocation)) {
                safeLocation = player.getWorld().getSpawnLocation();
            }
            player.teleport(safeLocation);
            player.sendMessage("§c§l❌ DISQUALIFIED! §cRace ended - returned to safe location.");
        }
        
        plugin.debugLog("Race cleanup completed for boat exit: " + player.getName());
    }
    
    private void handleMultiplayerBoatExit(Player player, Boat boat) {
        plugin.multiplayerDebugLog("Player exited multiplayer race boat - Player: " + player.getName());
        
        // Get the race
        MultiplayerRace race = plugin.getMultiplayerRaceManager().getRaceByPlayer(player.getUniqueId());
        if (race == null) {
            return;
        }
        
        // Remove the boat
        boatManager.removeRaceBoat(boat, "player_exited");
        
        // Check if this is the leader and if anyone has crossed start line yet
        if (race.getLeaderId().equals(player.getUniqueId())) {
            // Leader exited boat - check if race should be cancelled
            boolean anyoneStarted = false;
            for (MultiplayerRace.PlayerResult result : race.getPlayers().values()) {
                if (result.isTimerStarted()) {
                    anyoneStarted = true;
                    break;
                }
            }
            
            if (!anyoneStarted) {
                // No one crossed start line yet - cancel entire race
                plugin.getMultiplayerRaceManager().cancelRace(race.getCourse(), player);
                plugin.multiplayerDebugLog("Race cancelled - leader exited boat before anyone started");
                return;
            }
        }
        
        // Normal DQ (someone already started or not leader)
        plugin.getMultiplayerRaceManager().disqualifyPlayer(player.getUniqueId(), "You exited your boat during the race");
        
        plugin.multiplayerDebugLog("Multiplayer race cleanup completed for boat exit: " + player.getName());
    }
}
