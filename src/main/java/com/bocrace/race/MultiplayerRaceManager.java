package com.bocrace.race;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active multiplayer race sessions
 * Handles race creation, player management, and cleanup
 */
public class MultiplayerRaceManager {
    
    private final BOCRacePlugin plugin;
    private final Map<String, MultiplayerRace> activeRaces;          // raceId -> race
    private final Map<String, MultiplayerRace> courseRaces;         // courseName -> race
    private final Map<UUID, MultiplayerRace> playerRaces;           // playerId -> race
    
    public MultiplayerRaceManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.activeRaces = new ConcurrentHashMap<>();
        this.courseRaces = new ConcurrentHashMap<>();
        this.playerRaces = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new multiplayer race
     */
    public MultiplayerRace createRace(Course course, Player leader) {
        // Check if course already has an active race
        if (courseRaces.containsKey(course.getName())) {
            return null; // Course busy
        }
        
        // Check if leader is already in a race
        if (playerRaces.containsKey(leader.getUniqueId())) {
            return null; // Leader already racing
        }
        
        // Validate course has required components
        if (!isValidMultiplayerCourse(course)) {
            return null; // Course not properly set up
        }
        
        // Generate unique race ID
        String raceId = generateRaceId(course.getName());
        
        // Get timeout from config
        long timeoutMs = plugin.getConfig().getLong("multiplayer.race-timeout", 300) * 1000;
        
        // Create race
        MultiplayerRace race = new MultiplayerRace(raceId, course, leader, timeoutMs);
        
        // Register race
        activeRaces.put(raceId, race);
        courseRaces.put(course.getName(), race);
        // Note: Leader not added to playerRaces until they join
        
        // Schedule timeout task
        race.setTimeoutTask(new BukkitRunnable() {
            @Override
            public void run() {
                timeoutRace(raceId);
            }
        }.runTaskLater(plugin, timeoutMs / 50)); // Convert ms to ticks
        
        // Send server announcement
        sendRaceCreatedAnnouncement(race);
        
        plugin.multiplayerDebugLog("Created race: " + raceId + " on course " + course.getName() + 
                                  " by " + leader.getName());
        
        return race;
    }
    
    /**
     * Player attempts to join a race
     */
    public boolean joinRace(Course course, Player player) {
        MultiplayerRace race = courseRaces.get(course.getName());
        if (race == null) {
            return false; // No active race on this course
        }
        
        // Check if player already in a race
        if (playerRaces.containsKey(player.getUniqueId())) {
            return false; // Player already racing
        }
        
        // Check if race can accept more players
        if (!race.canJoin()) {
            return false; // Race full or not in lobby state
        }
        
        // Add player to race
        Location boatSpawn = race.addPlayer(player);
        if (boatSpawn == null) {
            return false; // Failed to add player
        }
        
        // Register player
        playerRaces.put(player.getUniqueId(), race);
        
        // Teleport player to boat spawn
        player.teleport(boatSpawn);
        
        // Spawn boat with player
        plugin.getBoatManager().spawnRaceBoat(player, boatSpawn);
        
        // Play join effects
        plugin.getSoundEffectManager().playRaceStartEffects(player, boatSpawn, course);
        
        player.sendMessage("§a§l🏁 Joined multiplayer race! §aWaiting for race leader to start...");
        plugin.multiplayerDebugLog("Player " + player.getName() + " joined race " + race.getRaceId());
        
        return true;
    }
    
    /**
     * Leader starts the race
     */
    public boolean startRace(Course course, Player leader) {
        MultiplayerRace race = courseRaces.get(course.getName());
        if (race == null) {
            return false; // No active race
        }
        
        if (!race.getLeaderId().equals(leader.getUniqueId())) {
            return false; // Not the race leader
        }
        
        if (race.getState() != MultiplayerRace.State.LOBBY) {
            return false; // Race not in lobby state
        }
        
        // Start the race (leader gets assigned a boat spawn)
        Location leaderSpawn = race.startRace(leader);
        if (leaderSpawn == null) {
            return false; // Failed to start (no spawn available)
        }
        
        // Register leader as player
        playerRaces.put(leader.getUniqueId(), race);
        
        // Teleport leader to boat spawn
        leader.teleport(leaderSpawn);
        
        // Spawn boat for leader
        plugin.getBoatManager().spawnRaceBoat(leader, leaderSpawn);
        
        // Send race started announcement
        sendRaceStartedAnnouncement(race);
        
        // Notify all players race has started
        for (UUID playerId : race.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§a§l🚀 RACE STARTED! §a§lCross the start line to begin timing!");
                plugin.getSoundEffectManager().playRaceStartEffects(player, player.getLocation(), course);
            }
        }
        
        plugin.multiplayerDebugLog("Started race: " + race.getRaceId() + " with " + 
                                  race.getPlayerCount() + " players");
        
        return true;
    }
    
    /**
     * Cancel a race (leader only)
     */
    public boolean cancelRace(Course course, Player leader) {
        MultiplayerRace race = courseRaces.get(course.getName());
        if (race == null) {
            return false; // No active race
        }
        
        if (!race.getLeaderId().equals(leader.getUniqueId())) {
            return false; // Not the race leader
        }
        
        if (race.getState() == MultiplayerRace.State.FINISHED || 
            race.getState() == MultiplayerRace.State.CANCELLED) {
            return false; // Race already ended
        }
        
        // Cancel the race
        race.cancelRace();
        
        // Cleanup all players
        cleanupRace(race, "Race cancelled by leader");
        
        plugin.debugLog("Cancelled multiplayer race: " + race.getRaceId() + " by " + leader.getName());
        
        return true;
    }
    
    /**
     * Player finishes the race
     */
    public void finishPlayer(UUID playerId) {
        MultiplayerRace race = playerRaces.get(playerId);
        if (race == null || race.getState() != MultiplayerRace.State.RUNNING) {
            return;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        
        // Finish the player
        race.finishPlayer(playerId);
        
        // Get placement
        MultiplayerRace.PlayerResult result = race.getPlayers().get(playerId);
        if (result != null) {
            // Send finish message
            player.sendMessage("§6§l🏆 RACE FINISHED! §6§lPlacement: #" + result.getPlacement() + 
                             " §7(Time: " + formatTime(result.getRaceTimeMs()) + ")");
            
            // Play finish effects
            plugin.getSoundEffectManager().playRaceFinishEffects(player, player.getLocation(), race.getCourse());
            
            // Remove player's boat
            Boat boat = plugin.getBoatManager().findRaceBoatByPlayer(player.getUniqueId());
            if (boat != null) {
                plugin.getBoatManager().removeRaceBoat(boat, "race_finished");
            }
            
            // Teleport player back to race lobby after delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        Location lobbySpawn = race.getCourse().getMpraceLobbySpawn();
                        if (lobbySpawn != null) {
                            player.teleport(lobbySpawn);
                        }
                    }
                }
            }.runTaskLater(plugin, 40L); // 2 second delay
        }
        
        // Check if race should end
        if (race.shouldEnd()) {
            endRace(race);
        }
        
        plugin.debugLog("Player " + player.getName() + " finished race " + race.getRaceId() + 
                       " in placement #" + (result != null ? result.getPlacement() : "?"));
    }
    
    /**
     * Disqualify a player
     */
    public void disqualifyPlayer(UUID playerId, String reason) {
        MultiplayerRace race = playerRaces.get(playerId);
        if (race == null) {
            return;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        
        // Disqualify the player
        race.disqualifyPlayer(playerId, reason);
        
        // Remove from tracking
        playerRaces.remove(playerId);
        
        // Cleanup player
        if (player != null) {
            player.sendMessage("§c§l❌ DISQUALIFIED! §c" + reason);
            
            // Remove boat
            Boat boat = plugin.getBoatManager().findRaceBoatByPlayer(playerId);
            if (boat != null) {
                plugin.getBoatManager().removeRaceBoat(boat, "disqualified");
            }
            
            // Teleport back to race lobby
            Location lobbySpawn = race.getCourse().getMpraceLobbySpawn();
            if (lobbySpawn != null) {
                player.teleport(lobbySpawn);
            }
        }
        
        // Check if race should end
        if (race.shouldEnd()) {
            endRace(race);
        }
        
        plugin.debugLog("Disqualified player " + (player != null ? player.getName() : playerId) + 
                       " from race " + race.getRaceId() + ": " + reason);
    }
    
    /**
     * Handle player disconnect
     */
    public void handlePlayerDisconnect(UUID playerId) {
        MultiplayerRace race = playerRaces.get(playerId);
        if (race == null) {
            return;
        }
        
        if (race.getLeaderId().equals(playerId)) {
            // Leader disconnected
            if (race.getState() == MultiplayerRace.State.LOBBY) {
                // Cancel race if still in lobby
                race.cancelRace();
                cleanupRace(race, "Race leader disconnected");
            } else {
                // DQ leader if race is running
                disqualifyPlayer(playerId, "Leader disconnected");
            }
        } else {
            // Regular player disconnected
            disqualifyPlayer(playerId, "Player disconnected");
        }
    }
    
    /**
     * End a race and show results
     */
    private void endRace(MultiplayerRace race) {
        // Get final results
        var results = race.endRace();
        
        // Send results to all players
        sendRaceResults(race, results);
        
        // Send server announcement
        sendRaceFinishedAnnouncement(race);
        
        // Cleanup race
        cleanupRaceData(race);
        
        plugin.debugLog("Ended multiplayer race: " + race.getRaceId() + " with " + 
                       results.size() + " participants");
    }
    
    /**
     * Timeout a race
     */
    private void timeoutRace(String raceId) {
        MultiplayerRace race = activeRaces.get(raceId);
        if (race == null) {
            return;
        }
        
        // DQ all unfinished players
        for (UUID playerId : race.getPlayers().keySet()) {
            MultiplayerRace.PlayerResult result = race.getPlayers().get(playerId);
            if (result != null && !result.isFinished() && !result.isDisqualified()) {
                disqualifyPlayer(playerId, "Race timed out");
            }
        }
        
        // End the race
        endRace(race);
        
        // Send timeout announcement
        String message = plugin.getConfig().getString("multiplayer.announcements.race-timeout", 
                                                    "&cRace on &6{course} &chas timed out! Unfinished players disqualified.")
                              .replace("{course}", race.getCourse().getName())
                              .replace("&", "§");
        Bukkit.broadcastMessage(message);
        
        plugin.debugLog("Timed out multiplayer race: " + raceId);
    }
    
    /**
     * Cleanup race and all players
     */
    private void cleanupRace(MultiplayerRace race, String reason) {
        // Notify all players
        for (UUID playerId : race.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§c§l❌ " + reason);
                
                // Remove boat
                Boat boat = plugin.getBoatManager().findRaceBoatByPlayer(playerId);
                if (boat != null) {
                    plugin.getBoatManager().removeRaceBoat(boat, "race_cleanup");
                }
                
                // Teleport back to race lobby
                Location lobbySpawn = race.getCourse().getMpraceLobbySpawn();
                if (lobbySpawn != null) {
                    player.teleport(lobbySpawn);
                }
            }
        }
        
        // Cleanup race data
        cleanupRaceData(race);
    }
    
    /**
     * Remove race from all tracking maps
     */
    private void cleanupRaceData(MultiplayerRace race) {
        activeRaces.remove(race.getRaceId());
        courseRaces.remove(race.getCourse().getName());
        
        // Remove all players from tracking
        for (UUID playerId : race.getPlayers().keySet()) {
            playerRaces.remove(playerId);
        }
        
        // Cancel timeout task
        if (race.getTimeoutTask() != null) {
            race.getTimeoutTask().cancel();
        }
    }
    
    /**
     * Send race results to participants
     */
    private void sendRaceResults(MultiplayerRace race, java.util.List<MultiplayerRace.PlayerResult> results) {
        // Send results to each participant
        for (UUID playerId : race.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage("§6§l=== RACE RESULTS ===");
                
                int position = 1;
                for (MultiplayerRace.PlayerResult result : results) {
                    if (result.isDisqualified()) {
                        player.sendMessage("§7" + position + ". §c" + result.getPlayerName() + " §7- DQ (" + result.getDisqualifyReason() + ")");
                    } else {
                        player.sendMessage("§7" + position + ". §a" + result.getPlayerName() + " §7- " + formatTime(result.getRaceTimeMs()));
                    }
                    position++;
                }
            }
        }
    }
    
    /**
     * Send server announcements
     */
    private void sendRaceCreatedAnnouncement(MultiplayerRace race) {
        String message = plugin.getConfig().getString("multiplayer.announcements.race-created",
                                                    "&6{player} &eis starting a multiplayer race on &a{course}&e! Join at the race lobby for prizes!")
                              .replace("{player}", race.getLeaderName())
                              .replace("{course}", race.getCourse().getName())
                              .replace("&", "§");
        Bukkit.broadcastMessage(message);
    }
    
    private void sendRaceStartedAnnouncement(MultiplayerRace race) {
        String message = plugin.getConfig().getString("multiplayer.announcements.race-started",
                                                    "&aRace started on &6{course}&a! &7({players} racers)")
                              .replace("{course}", race.getCourse().getName())
                              .replace("{players}", String.valueOf(race.getPlayerCount()))
                              .replace("&", "§");
        Bukkit.broadcastMessage(message);
    }
    
    private void sendRaceFinishedAnnouncement(MultiplayerRace race) {
        String message = plugin.getConfig().getString("multiplayer.announcements.race-finished",
                                                    "&6Race completed on &a{course}&6! Results posted.")
                              .replace("{course}", race.getCourse().getName())
                              .replace("&", "§");
        Bukkit.broadcastMessage(message);
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
     * Generate unique race ID
     */
    private String generateRaceId(String courseName) {
        return courseName + "_" + System.currentTimeMillis();
    }
    
    /**
     * Format race time for display
     */
    private String formatTime(Long timeMs) {
        if (timeMs == null) {
            return "N/A";
        }
        
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        long milliseconds = timeMs % 1000;
        
        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, milliseconds);
        } else {
            return String.format("%d.%03d", seconds, milliseconds);
        }
    }
    
    // Public getters for external access
    public MultiplayerRace getRaceByPlayer(UUID playerId) {
        return playerRaces.get(playerId);
    }
    
    public MultiplayerRace getRaceByCourse(String courseName) {
        return courseRaces.get(courseName);
    }
    
    public boolean isPlayerInRace(UUID playerId) {
        return playerRaces.containsKey(playerId);
    }
    
    public boolean isCourseOccupied(String courseName) {
        return courseRaces.containsKey(courseName);
    }
    
    public int getActiveRaceCount() {
        return activeRaces.size();
    }
    
    /**
     * Shutdown - cleanup all active races
     */
    public void shutdown() {
        for (MultiplayerRace race : activeRaces.values()) {
            cleanupRace(race, "Server shutting down");
        }
    }
}
