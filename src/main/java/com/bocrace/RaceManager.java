package com.bocrace;

import com.bocrace.model.Course;
import com.bocrace.model.PlayerStats;
import com.bocrace.model.RaceSession;
import com.bocrace.util.ConfigUtil;
import com.bocrace.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages race sessions and lobby system
 */
public class RaceManager {
    private final BOCRacePlugin plugin;
    private final Map<String, RaceSession> activeRaces;
    private final Map<UUID, RaceSession> playerRaces;
    private final Map<UUID, Long> playerCooldowns;
    private BukkitTask raceUpdateTask;
    
    public RaceManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.activeRaces = new ConcurrentHashMap<>();
        this.playerRaces = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
        
        // Start race update task
        startRaceUpdateTask();
    }
    
    /**
     * Start the race update task
     */
    private void startRaceUpdateTask() {
        int tickRate = ConfigUtil.getUpdateTickRate(plugin);
        raceUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateRaces();
            }
        }.runTaskTimer(plugin, 0, tickRate);
    }
    
    /**
     * Update all active races
     */
    private void updateRaces() {
        for (RaceSession race : activeRaces.values()) {
            if (race.isActive()) {
                checkRaceTimeout(race);
                checkPlayerPositions(race);
            }
        }
    }
    
    /**
     * Check if a race has timed out
     */
    private void checkRaceTimeout(RaceSession race) {
        int timeout = ConfigUtil.getRaceTimeout(plugin);
        if (timeout > 0 && race.getDurationSeconds() > timeout) {
            endRace(race, RaceSession.RaceStatus.TIMEOUT);
        }
    }
    
    /**
     * Check player positions for finish line detection
     */
    private void checkPlayerPositions(RaceSession race) {
        Course course = race.getCourse();
        
        for (UUID playerId : race.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;
            
            RaceSession.PlayerRaceData playerData = race.getPlayerData(playerId);
            if (playerData.hasFinished() || playerData.isDisqualified()) continue;
            
            Location location = player.getLocation();
            
            // Check if player crossed the finish line
            if (course.isInFinishLine(location)) {
                finishPlayer(race, player, playerData);
            }
        }
    }
    
    /**
     * Join a multiplayer race
     */
    public void joinRace(Player player, Course course) {
        // Check if player is already in a race
        if (playerRaces.containsKey(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "race.already-in-race");
            return;
        }
        
        // Check if player is in cooldown
        if (isInCooldown(player)) {
            long remaining = getCooldownRemaining(player);
            MessageUtil.sendMessage(player, "race.cooldown", Map.of("time", remaining));
            return;
        }
        
        // Check if course is valid
        if (!course.isComplete()) {
            MessageUtil.sendMessage(player, "error.course-not-valid", 
                Map.of("course", course.getName()));
            return;
        }
        
        // Find or create race session
        RaceSession race = activeRaces.get(course.getName());
        if (race == null) {
            // Create new race session
            int maxPlayers = ConfigUtil.getMaxPlayersPerRace(plugin);
            race = new RaceSession(course, RaceSession.RaceType.MULTIPLAYER, player.getUniqueId(), maxPlayers);
            activeRaces.put(course.getName(), race);
            playerRaces.put(player.getUniqueId(), race);
            
            MessageUtil.sendMessage(player, "race.lobby-created");
        } else {
            // Join existing race session
            if (race.isFull()) {
                MessageUtil.sendMessage(player, "race.lobby-full", 
                    Map.of("max", race.getMaxPlayers()));
                return;
            }
            
            if (race.isActive()) {
                MessageUtil.sendMessage(player, "race.race-in-progress");
                return;
            }
            
            race.addParticipant(player.getUniqueId());
            playerRaces.put(player.getUniqueId(), race);
            
            MessageUtil.sendMessage(player, "race.lobby-joined", 
                Map.of("course", course.getName()));
        }
    }
    
    /**
     * Leave a race
     */
    public void leaveRace(Player player) {
        RaceSession race = playerRaces.remove(player.getUniqueId());
        if (race == null) {
            MessageUtil.sendMessage(player, "race.not-in-lobby");
            return;
        }
        
        race.removeParticipant(player.getUniqueId());
        
        // If this was the leader and race hasn't started, cancel the race
        if (race.getLeader().equals(player.getUniqueId()) && !race.isActive()) {
            cancelRace(race);
        } else if (race.isActive()) {
            // Disqualify player if race is active
            disqualifyPlayer(race, player);
        }
        
        MessageUtil.sendMessage(player, "race.lobby-left");
    }
    
    /**
     * Start a race
     */
    public void startRace(Player player) {
        RaceSession race = playerRaces.get(player.getUniqueId());
        if (race == null) {
            MessageUtil.sendMessage(player, "race.not-in-lobby");
            return;
        }
        
        if (!race.getLeader().equals(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "race.not-leader");
            return;
        }
        
        if (race.isActive()) {
            MessageUtil.sendMessage(player, "race.race-in-progress");
            return;
        }
        
        // Start the race
        race.setStatus(RaceSession.RaceStatus.ACTIVE);
        
        // Teleport all players to random spawn points
        for (UUID playerId : race.getParticipants()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) continue;
            
            Location spawnLocation = race.getCourse().getRandomBoatSpawn();
            if (spawnLocation == null) continue;
            
            p.teleport(spawnLocation);
            
            // Spawn boat and mount player
            Boat boat = spawnLocation.getWorld().spawn(spawnLocation, Boat.class);
            boat.addPassenger(p);
            
            // Set player data
            RaceSession.PlayerRaceData playerData = race.getPlayerData(playerId);
            playerData.setSpawnLocation(spawnLocation);
            playerData.setBoat(boat);
            playerData.setRaceStartTime(System.currentTimeMillis());
        }
        
        // Play start sound
        Sound startSound = ConfigUtil.getRaceStartSound(plugin);
        for (UUID playerId : race.getParticipants()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null) {
                p.playSound(p.getLocation(), startSound, 1.0f, 1.0f);
            }
        }
        
        MessageUtil.sendMessage(player, "race.race-starting");
    }
    
    /**
     * Finish a player in a race
     */
    private void finishPlayer(RaceSession race, Player player, RaceSession.PlayerRaceData playerData) {
        if (playerData.hasFinished()) return;
        
        long finishTime = System.currentTimeMillis();
        playerData.setFinishTime(finishTime);
        
        // Calculate position
        int position = race.getResults().size() + 1;
        playerData.setFinishPosition(position);
        
        // Play finish sound
        Sound finishSound = ConfigUtil.getRaceFinishSound(plugin);
        player.playSound(player.getLocation(), finishSound, 1.0f, 1.0f);
        
        // Send finish message
        MessageUtil.sendMessage(player, "race.player-finished", 
            Map.of("player", player.getName(), "position", position, "time", playerData.getFormattedRaceTime()));
        
        // Check if race is complete
        if (allPlayersFinished(race)) {
            endRace(race, RaceSession.RaceStatus.FINISHED);
        }
    }
    
    
    /**
     * End a race
     */
    private void endRace(RaceSession race, RaceSession.RaceStatus status) {
        race.setStatus(status);
        race.setEndTime(System.currentTimeMillis());
        
        // Teleport all players back to lobby
        for (UUID playerId : race.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;
            
            // Remove boat
            RaceSession.PlayerRaceData playerData = race.getPlayerData(playerId);
            if (playerData.getBoat() != null) {
                playerData.getBoat().remove();
            }
            
            // Teleport to lobby
            teleportToLobby(player, race.getCourse());
            
            // Update stats
            updatePlayerStats(player, race, playerData);
        }
        
        // Send race results
        sendRaceResults(race);
        
        // Clean up
        activeRaces.remove(race.getCourse().getName());
        for (UUID playerId : race.getParticipants()) {
            playerRaces.remove(playerId);
            setCooldown(playerId);
        }
    }
    
    /**
     * Cancel a race
     */
    private void cancelRace(RaceSession race) {
        race.setStatus(RaceSession.RaceStatus.CANCELLED);
        
        // Teleport all players back to lobby
        for (UUID playerId : race.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                teleportToLobby(player, race.getCourse());
            }
            playerRaces.remove(playerId);
        }
        
        activeRaces.remove(race.getCourse().getName());
        MessageUtil.sendMessage(Bukkit.getConsoleSender(), "race.lobby-cancelled");
    }
    
    /**
     * Check if all players have finished
     */
    private boolean allPlayersFinished(RaceSession race) {
        for (UUID playerId : race.getParticipants()) {
            RaceSession.PlayerRaceData playerData = race.getPlayerData(playerId);
            if (!playerData.hasFinished() && !playerData.isDisqualified()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Send race results to all participants
     */
    private void sendRaceResults(RaceSession race) {
        List<RaceSession.PlayerRaceData> results = race.getResultsByPosition();
        
        for (UUID playerId : race.getParticipants()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) continue;
            
            MessageUtil.sendMessage(player, "race.race-finished");
            
            for (int i = 0; i < results.size(); i++) {
                RaceSession.PlayerRaceData playerData = results.get(i);
                Player p = Bukkit.getPlayer(playerData.getPlayerId());
                if (p != null) {
                    MessageUtil.sendMessage(player, "race.leaderboard-entry", 
                        Map.of("position", i + 1, "player", p.getName(), "time", playerData.getFormattedRaceTime()));
                }
            }
        }
    }
    
    /**
     * Update player statistics
     */
    private void updatePlayerStats(Player player, RaceSession race, RaceSession.PlayerRaceData playerData) {
        PlayerStats stats = plugin.getStorageManager().getOrCreatePlayerStats(player.getUniqueId());
        PlayerStats.CourseStats courseStats = stats.getCourseStats(race.getCourse().getName());
        
        // Update course stats
        courseStats.updateStats(playerData.getRaceTime(), playerData.getFinishPosition(), playerData.hasFinished());
        
        // Add race record
        PlayerStats.RaceRecord record = new PlayerStats.RaceRecord(
            UUID.randomUUID().toString(),
            race.getCourse().getName(),
            PlayerStats.RaceRecord.RaceType.MULTIPLAYER,
            playerData.getRaceTime(),
            playerData.getFinishPosition(),
            playerData.hasFinished(),
            race.getSessionId()
        );
        stats.addRaceRecord(record);
        
        // Save stats
        plugin.getStorageManager().savePlayerStats();
    }
    
    /**
     * Teleport player to appropriate lobby
     */
    private void teleportToLobby(Player player, Course course) {
        Location lobbyLocation = course.getMainLobby();
        if (lobbyLocation == null) {
            lobbyLocation = course.getCourseLobby();
        }
        
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
    }
    
    
    /**
     * Set cooldown for player
     */
    private void setCooldown(UUID playerId) {
        int cooldown = ConfigUtil.getRaceCooldown(plugin);
        if (cooldown > 0) {
            playerCooldowns.put(playerId, System.currentTimeMillis() + (cooldown * 1000L));
        }
    }
    
    /**
     * Get active race for a player
     */
    public RaceSession getPlayerRace(Player player) {
        return playerRaces.get(player.getUniqueId());
    }
    
    /**
     * Get active race for a course
     */
    public RaceSession getCourseRace(String courseName) {
        return activeRaces.get(courseName);
    }
    
    /**
     * Get all active races
     */
    public Map<String, RaceSession> getActiveRaces() {
        return activeRaces;
    }
    
    /**
     * Get player races map
     */
    public Map<UUID, RaceSession> getPlayerRaces() {
        return playerRaces;
    }
    
    /**
     * Check if player is in cooldown
     */
    public boolean isInCooldown(Player player) {
        return isInCooldown(player.getUniqueId());
    }
    
    /**
     * Check if player is in cooldown
     */
    public boolean isInCooldown(UUID playerId) {
        Long cooldownEnd = playerCooldowns.get(playerId);
        if (cooldownEnd == null) return false;
        
        if (System.currentTimeMillis() > cooldownEnd) {
            playerCooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public long getCooldownRemaining(Player player) {
        return getCooldownRemaining(player.getUniqueId());
    }
    
    /**
     * Get remaining cooldown time in seconds
     */
    public long getCooldownRemaining(UUID playerId) {
        Long cooldownEnd = playerCooldowns.get(playerId);
        if (cooldownEnd == null) return 0;
        
        return Math.max(0, (cooldownEnd - System.currentTimeMillis()) / 1000);
    }
    
    /**
     * Disqualify a player from a race
     */
    public void disqualifyPlayer(RaceSession race, Player player) {
        RaceSession.PlayerRaceData playerData = race.getPlayerData(player.getUniqueId());
        if (playerData == null) return;
        
        playerData.setDisqualified(true);
        playerData.setDisqualificationReason("Left the race");
        
        // Remove boat
        if (playerData.getBoat() != null) {
            playerData.getBoat().remove();
        }
        
        // Teleport back to lobby
        teleportToLobby(player, race.getCourse());
        
        MessageUtil.sendMessage(player, "race.player-disqualified", 
            Map.of("player", player.getName()));
    }
    
    /**
     * Shutdown the race manager
     */
    public void shutdown() {
        if (raceUpdateTask != null) {
            raceUpdateTask.cancel();
        }
        
        // End all active races
        for (RaceSession race : activeRaces.values()) {
            endRace(race, RaceSession.RaceStatus.CANCELLED);
        }
        
        activeRaces.clear();
        playerRaces.clear();
        playerCooldowns.clear();
    }
}
