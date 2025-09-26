package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import com.bocrace.model.CourseType;
import com.bocrace.race.ActiveRace;
import com.bocrace.race.MultiplayerRace;
import com.bocrace.util.BoatManager;
import com.bocrace.util.LineDetection;
import com.bocrace.util.TeleportUtil;
import com.bocrace.util.SoundEffectManager;
import com.bocrace.model.RaceRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Handles start/finish line detection and race timing
 */
public class RaceLineListener implements Listener {
    
    private final BOCRacePlugin plugin;
    private final BoatManager boatManager;
    private final TeleportUtil teleportUtil;
    private final SoundEffectManager soundEffectManager;
    
    public RaceLineListener(BOCRacePlugin plugin, BoatManager boatManager, TeleportUtil teleportUtil) {
        this.plugin = plugin;
        this.boatManager = boatManager;
        this.teleportUtil = teleportUtil;
        this.soundEffectManager = plugin.getSoundEffectManager();
    }
    
    @EventHandler
    public void onBoatMove(VehicleMoveEvent event) {
        // Only handle boats
        if (!(event.getVehicle() instanceof Boat)) return;
        
        Boat boat = (Boat) event.getVehicle();
        
        // Only handle race boats
        if (!boatManager.isRaceBoat(boat)) return;
        
        // Get the player UUID from the boat
        UUID playerUuid = boatManager.getRaceBoatPlayer(boat);
        if (playerUuid == null) {
            plugin.raceDebugLog("Race boat move - no player UUID found");
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Check for singleplayer race first
        ActiveRace race = plugin.getRaceManager().getActiveRace(playerUuid);
        if (race != null) {
            // Handle singleplayer race
            Course course = plugin.getStorageManager().getCourse(race.getCourseName());
            if (course == null || course.getType() != CourseType.SINGLEPLAYER) {
                plugin.raceDebugLog("Race boat move - singleplayer course not found or wrong type: " + race.getCourseName());
                return;
            }
            
            plugin.raceDebugLog("Singleplayer race boat moving - Player: " + race.getPlayerName() + 
                               ", State: " + race.getState() + 
                               ", From: " + formatLocation(from) + 
                               ", To: " + formatLocation(to));
            
            // Handle different race states
            if (race.getState() == ActiveRace.State.ARMED) {
                handleStartLineDetection(boat, race, course, from, to);
            } else if (race.getState() == ActiveRace.State.RUNNING) {
                handleFinishLineDetection(boat, race, course, from, to);
                updateRaceTimer(boat, race);
            }
            return;
        }
        
        // Check for multiplayer race
        MultiplayerRace mpRace = plugin.getMultiplayerRaceManager().getRaceByPlayer(playerUuid);
        if (mpRace != null) {
            // Handle multiplayer race
            Course course = mpRace.getCourse();
            if (course == null || course.getType() != CourseType.MULTIPLAYER) {
                plugin.raceDebugLog("Race boat move - multiplayer course not found or wrong type");
                return;
            }
            
            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null) {
                plugin.raceDebugLog("Race boat move - multiplayer player not found: " + playerUuid);
                return;
            }
            
            plugin.raceDebugLog("Multiplayer race boat moving - Player: " + player.getName() + 
                               ", Race State: " + mpRace.getState() + 
                               ", From: " + formatLocation(from) + 
                               ", To: " + formatLocation(to));
            
            // Handle multiplayer race states
            if (mpRace.getState() == MultiplayerRace.State.RUNNING) {
                handleMultiplayerStartLineDetection(boat, mpRace, course, player, from, to);
                handleMultiplayerFinishLineDetection(boat, mpRace, course, player, from, to);
                updateMultiplayerRaceTimer(boat, mpRace, player);
            }
            return;
        }
        
        plugin.raceDebugLog("Race boat move - no active race found for player " + playerUuid);
    }
    
    /**
     * Handle start line detection (thin plane)
     */
    private void handleStartLineDetection(Boat boat, ActiveRace race, Course course, Location from, Location to) {
        if (course.getSpstart1() == null || course.getSpstart2() == null) {
            plugin.raceDebugLog("Start line detection skipped - start line not configured");
            return;
        }
        
        plugin.raceDebugLog("Checking start line crossing - Start1: " + formatLocation(course.getSpstart1()) + 
                           ", Start2: " + formatLocation(course.getSpstart2()));
        
        // Add detailed zone debug info
        plugin.raceDebugLog(LineDetection.getStartZoneDescription(to, course.getSpstart1(), course.getSpstart2()));
        
        boolean crossedStart = LineDetection.crossedStartLine(from, to, course.getSpstart1(), course.getSpstart2());
        
        plugin.raceDebugLog("Start line check result: " + crossedStart);
        
        if (crossedStart) {
            plugin.raceDebugLog("🏁 START LINE CROSSED! - Player: " + race.getPlayerName() + 
                               ", Course: " + course.getName() + 
                               ", From: " + formatLocation(from) + 
                               ", To: " + formatLocation(to));
            
            // Start the race timer
            race.setState(ActiveRace.State.RUNNING);
            race.setStartNanoTime(System.nanoTime());
            
            // Update boat state
            boatManager.updateBoatState(boat, ActiveRace.State.RUNNING);
            
            // Get player and send feedback
            Player player = getPlayerFromBoat(boat);
            if (player != null) {
                // Play start sound and effects using SoundEffectManager
                soundEffectManager.playRaceStartEffects(player, player.getLocation(), course);
                
                // Send start message
                player.sendMessage("§a🏁 Timer started!");
                
                plugin.raceDebugLog("🎵 Start sound played and timer started for " + player.getName());
            }
        }
    }
    
    /**
     * Handle finish line detection (proper crossing detection)
     */
    private void handleFinishLineDetection(Boat boat, ActiveRace race, Course course, Location from, Location to) {
        if (course.getSpfinish1() == null || course.getSpfinish2() == null) {
            plugin.raceDebugLog("Finish line detection skipped - finish line not configured");
            return;
        }
        
        plugin.raceDebugLog("Checking finish line crossing - Finish1: " + formatLocation(course.getSpfinish1()) + 
                           ", Finish2: " + formatLocation(course.getSpfinish2()) + 
                           ", From: " + formatLocation(from) + 
                           ", To: " + formatLocation(to));
        
        // Add detailed zone debug info
        plugin.raceDebugLog(LineDetection.getFinishZoneDescription(to, course.getSpfinish1(), course.getSpfinish2()));
        
        boolean crossedFinish = LineDetection.crossedFinishLine(from, to, course.getSpfinish1(), course.getSpfinish2());
        
        plugin.raceDebugLog("Finish line crossing check result: " + crossedFinish);
        
        if (crossedFinish) {
            plugin.raceDebugLog("🏆 FINISH LINE CROSSED! - Player: " + race.getPlayerName() + 
                               ", Course: " + course.getName() + 
                               ", From: " + formatLocation(from) + 
                               ", To: " + formatLocation(to));
            
            // Complete the race
            completeRace(boat, race, course);
        }
    }
    
    /**
     * Update the race timer display
     */
    private void updateRaceTimer(Boat boat, ActiveRace race) {
        Player player = getPlayerFromBoat(boat);
        if (player == null) return;
        
        // Always use ActionBar for smooth live stopwatch display
        String timerText = race.getFormattedCurrentTime();
        player.sendActionBar(Component.text(timerText, NamedTextColor.AQUA));
        
        // Debug every 1 second (1000ms) to avoid spam
        long currentTime = System.currentTimeMillis();
        if (currentTime % 1000 < 50) { // Log roughly every second
            plugin.raceDebugLog("⏱️ Timer update - " + timerText + " for " + race.getPlayerName() + " (mode: actionbar)");
        }
    }
    
    /**
     * Complete a race
     */
    private void completeRace(Boat boat, ActiveRace race, Course course) {
        plugin.raceDebugLog("🏆 COMPLETING RACE - Player: " + race.getPlayerName() + ", Course: " + course.getName());
        
        // End the race
        plugin.getRaceManager().endRace(race.getPlayerUuid(), ActiveRace.State.FINISHED, null);
        
        Player player = getPlayerFromBoat(boat);
        if (player != null) {
            // Calculate final time
            long finalTimeMs = race.getFinalTimeMs();
            String finalTimeFormatted = race.getFormattedFinalTime();
            
            plugin.raceDebugLog("🕐 Final time calculated: " + finalTimeMs + "ms (" + finalTimeFormatted + ")");
            
            // Play celebration sound and effects using SoundEffectManager
            plugin.raceDebugLog("🐛 About to play finish effects...");
            soundEffectManager.playRaceFinishEffects(player, player.getLocation(), course);
            plugin.raceDebugLog("🎵 Celebration sound played");
            plugin.raceDebugLog("🎆 Firework particles spawned");
            
            // Send completion message
            String finishMessage = plugin.getConfig().getString("messages.race-finish", "§a🏁 You finished in: {time}!");
            finishMessage = finishMessage.replace("{time}", finalTimeFormatted.replace("Race Time: ", ""));
            player.sendMessage(finishMessage);
            
            // Check if it's a personal best
            try {
                RaceRecord bestRecord = plugin.getRecordManager().getPlayerBestTime(race.getPlayerName(), race.getCourseName());
                if (bestRecord == null || finalTimeMs < (bestRecord.getTime() * 1000)) { // Convert seconds to ms for comparison
                    String pbMessage = plugin.getConfig().getString("messages.personal-best", "§a§l⭐ NEW PERSONAL BEST! §a§l⭐");
                    player.sendMessage(pbMessage);
                    soundEffectManager.playPersonalBestEffects(player, player.getLocation());
                    plugin.raceDebugLog("🌟 NEW PERSONAL BEST! - Player: " + race.getPlayerName() + ", Time: " + finalTimeMs + "ms");
                }
            } catch (Exception e) {
                plugin.raceDebugLog("Error checking personal best: " + e.getMessage());
            }
            
            // Save race record
            plugin.getRecordManager().saveRaceRecord(
                race.getPlayerName(), 
                race.getCourseName(), 
                finalTimeMs / 1000.0, // Convert to seconds
                race.getCourseType()
            );
            
            plugin.raceDebugLog("💾 Race record saved - Player: " + race.getPlayerName() + 
                               ", Course: " + course.getName() + 
                               ", Time: " + finalTimeMs + "ms");
            
            // Teleport back to lobby after brief delay (let sounds/particles finish)
            plugin.raceDebugLog("🚀 Scheduling teleport in 1 second (let effects finish)...");
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.raceDebugLog("🚀 EXECUTING TELEPORT - Player: " + player.getName() + " after race completion, Button type: " + race.getStartButtonType());
                        
                        // Teleport based on which button was used to start the race
                        if ("mainlobby".equals(race.getStartButtonType())) {
                            // Started from main lobby → return to main lobby
                            teleportUtil.teleportToLobby(player, course, "race_complete");
                        } else {
                            // Started from course lobby → return to course lobby
                            teleportUtil.teleportToCourseLobby(player, course, "race_complete");
                        }
                    }
                }
            }.runTaskLater(plugin, 40L); // 2 second delay
        }
        
        // Remove the boat
        plugin.raceDebugLog("🛥️ Removing race boat");
        boatManager.removeRaceBoat(boat, "race_completed");
    }
    
    /**
     * Get the player from a boat
     */
    private Player getPlayerFromBoat(Boat boat) {
        if (boat.getPassengers().isEmpty()) return null;
        
        if (boat.getPassengers().get(0) instanceof Player) {
            return (Player) boat.getPassengers().get(0);
        }
        
        return null;
    }
    
    /**
     * Handle multiplayer start line detection
     */
    private void handleMultiplayerStartLineDetection(Boat boat, MultiplayerRace race, Course course, Player player, Location from, Location to) {
        // Check if player is in race
        MultiplayerRace.PlayerResult result = race.getPlayers().get(player.getUniqueId());
        if (result == null || result.isDisqualified()) {
            return; // Player not in race or already DQ'd
        }
        
        // Check if timer already started
        if (result.isTimerStarted()) {
            return; // Timer already started for this player
        }
        
        // Check if player crossed start line
        boolean crossedStart = LineDetection.crossedStartLine(from, to, course.getSpstart1(), course.getSpstart2());
        if (crossedStart) {
            plugin.raceDebugLog("🏁 Multiplayer start line crossed by " + player.getName() + " - STARTING TIMER");
            
            // Start the timer for this player
            result.startTimer();
            
            // Play start effects
            soundEffectManager.playRaceStartEffects(player, to, course);
            player.sendMessage("§a§l🏁 GO! §aTimer started!");
        }
    }
    
    /**
     * Handle multiplayer finish line detection
     */
    private void handleMultiplayerFinishLineDetection(Boat boat, MultiplayerRace race, Course course, Player player, Location from, Location to) {
        // Check if player has finished already
        MultiplayerRace.PlayerResult result = race.getPlayers().get(player.getUniqueId());
        if (result == null || result.isFinished() || result.isDisqualified()) {
            return;
        }
        
        // Check if crossed finish zone
        boolean crossedFinish = LineDetection.crossedFinishLine(from, to, course.getSpfinish1(), course.getSpfinish2());
        if (crossedFinish) {
            plugin.raceDebugLog("🏆 Multiplayer finish line crossed by " + player.getName());
            
            // Finish the player through race manager
            plugin.getMultiplayerRaceManager().finishPlayer(player.getUniqueId());
        }
    }
    
    /**
     * Update multiplayer race timer display
     */
    private void updateMultiplayerRaceTimer(Boat boat, MultiplayerRace race, Player player) {
        // Check if player's timer has started
        MultiplayerRace.PlayerResult result = race.getPlayers().get(player.getUniqueId());
        if (result == null || !result.isTimerStarted()) {
            return; // Don't show timer until player crosses start line
        }
        
        // Get player's current race time
        long raceTimeMs = System.currentTimeMillis() - race.getRaceStartTimeMs();
        
        // Format time for display
        long seconds = raceTimeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long milliseconds = raceTimeMs % 1000;
        
        String timeDisplay;
        if (minutes > 0) {
            timeDisplay = String.format("§6⏱ %d:%02d.%03d", minutes, seconds, milliseconds);
        } else {
            timeDisplay = String.format("§6⏱ %d.%03d", seconds, milliseconds);
        }
        
        // Send to action bar for smooth updates
        player.sendActionBar(timeDisplay);
    }
    
    /**
     * Format a location for debug logging
     */
    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("%s %.1f,%.1f,%.1f", 
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
