package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.race.ActiveRace;
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
        
        // Check if player has an active race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race == null) return;
        
        plugin.debugLog("Player disconnected during race - Player: " + player.getName() + 
                       ", Course: " + race.getCourseName() + 
                       ", State: " + race.getState() + 
                       ", Duration: " + race.getCurrentDurationMs() + "ms");
        
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
                race.getPlayerName() + " (DQ)", 
                race.getCourseName(), 
                -timeSeconds, // Negative indicates DQ
                race.getCourseType()
            );
            
            plugin.debugLog("DQ record saved - Player: " + race.getPlayerName() + 
                           ", Time: " + race.getFormattedDqMessage());
        }
        
        plugin.debugLog("Race cleanup completed for disconnected player: " + player.getName());
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
        
        // Check if player has an active race
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race == null) return;
        
        plugin.debugLog("Player exited race boat - Player: " + player.getName() + 
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
                race.getPlayerName() + " (DQ)", 
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
}
