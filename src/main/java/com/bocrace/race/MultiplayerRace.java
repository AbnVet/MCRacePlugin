package com.bocrace.race;

import com.bocrace.model.Course;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active multiplayer race session
 * Manages race state, players, timers, and results
 */
public class MultiplayerRace {
    
    public enum State {
        LOBBY,      // Race created, players can join
        STARTING,   // Race starting countdown (future feature)
        RUNNING,    // Race in progress
        FINISHED,   // Race completed
        CANCELLED   // Race cancelled by leader
    }
    
    public static class PlayerResult {
        private final UUID playerId;
        private final String playerName;
        private final long startTimeMs;
        private Long finishTimeMs;
        private Long raceTimeMs;
        private int placement;
        private boolean disqualified;
        private String disqualifyReason;
        private boolean timerStarted; // Flag for when timer actually starts (crossing start line)
        
        public PlayerResult(UUID playerId, String playerName, long startTimeMs) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.startTimeMs = startTimeMs;
            this.disqualified = false;
            this.timerStarted = false; // Timer starts when crossing start line
        }
        
        public void finish(long finishTimeMs) {
            this.finishTimeMs = finishTimeMs;
            this.raceTimeMs = finishTimeMs - startTimeMs;
        }
        
        public void disqualify(String reason) {
            this.disqualified = true;
            this.disqualifyReason = reason;
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public long getStartTimeMs() { return startTimeMs; }
        public Long getFinishTimeMs() { return finishTimeMs; }
        public Long getRaceTimeMs() { return raceTimeMs; }
        public int getPlacement() { return placement; }
        public void setPlacement(int placement) { this.placement = placement; }
        public boolean isDisqualified() { return disqualified; }
        public String getDisqualifyReason() { return disqualifyReason; }
        public boolean isFinished() { return finishTimeMs != null; }
        public boolean isTimerStarted() { return timerStarted; }
        public void startTimer() { this.timerStarted = true; }
    }
    
    private final String raceId;
    private final Course course;
    private final UUID leaderId;
    private final String leaderName;
    private final LocalDateTime createdAt;
    private final long timeoutMs;
    
    private State state;
    private final Map<UUID, PlayerResult> players;
    private final List<Location> assignedBoatSpawns;
    private final Map<UUID, Integer> playerBoatSpawns; // playerId -> spawn index
    private BukkitTask timeoutTask;
    
    private long raceStartTimeMs;
    private int nextPlacement;
    
    public MultiplayerRace(String raceId, Course course, Player leader, long timeoutMs) {
        this.raceId = raceId;
        this.course = course;
        this.leaderId = leader.getUniqueId();
        this.leaderName = leader.getName();
        this.createdAt = LocalDateTime.now();
        this.timeoutMs = timeoutMs;
        
        this.state = State.LOBBY;
        this.players = new ConcurrentHashMap<>();
        this.assignedBoatSpawns = new ArrayList<>();
        this.playerBoatSpawns = new ConcurrentHashMap<>();
        this.nextPlacement = 1;
        
        // Initialize available boat spawns (copy from course)
        if (course.getMpboatSpawns() != null) {
            this.assignedBoatSpawns.addAll(course.getMpboatSpawns());
        }
    }
    
    /**
     * Add a player to the race lobby
     * Returns the assigned boat spawn location, or null if race is full/invalid
     */
    public Location addPlayer(Player player) {
        if (state != State.LOBBY) {
            return null; // Race not in lobby state
        }
        
        if (players.containsKey(player.getUniqueId())) {
            return null; // Player already joined
        }
        
        // Find available boat spawn
        List<Integer> availableSpawns = new ArrayList<>();
        for (int i = 0; i < assignedBoatSpawns.size(); i++) {
            if (!playerBoatSpawns.containsValue(i)) {
                availableSpawns.add(i);
            }
        }
        
        if (availableSpawns.isEmpty()) {
            return null; // No available spawns
        }
        
        // Randomly assign a spawn
        int spawnIndex = availableSpawns.get(new Random().nextInt(availableSpawns.size()));
        playerBoatSpawns.put(player.getUniqueId(), spawnIndex);
        
        // Create player result (start time will be set when race starts)
        players.put(player.getUniqueId(), new PlayerResult(player.getUniqueId(), player.getName(), 0));
        
        return assignedBoatSpawns.get(spawnIndex);
    }
    
    /**
     * Remove a player from the race
     */
    public void removePlayer(UUID playerId) {
        players.remove(playerId);
        playerBoatSpawns.remove(playerId);
    }
    
    /**
     * Start the race - leader joins and race begins
     */
    public Location startRace(Player leader) {
        if (state != State.LOBBY || !leaderId.equals(leader.getUniqueId())) {
            return null;
        }
        
        // Assign leader to a boat spawn if available
        Location leaderSpawn = addPlayer(leader);
        if (leaderSpawn == null) {
            return null; // No spawn available for leader
        }
        
        // Start the race
        state = State.RUNNING;
        raceStartTimeMs = System.currentTimeMillis();
        
        // Set start times for all players
        for (PlayerResult result : players.values()) {
            // Update with actual start time (constructor sets 0 as placeholder)
            PlayerResult updatedResult = new PlayerResult(result.getPlayerId(), result.getPlayerName(), raceStartTimeMs);
            players.put(result.getPlayerId(), updatedResult);
        }
        
        return leaderSpawn;
    }
    
    /**
     * Finish a player's race
     */
    public void finishPlayer(UUID playerId) {
        PlayerResult result = players.get(playerId);
        if (result != null && !result.isFinished() && !result.isDisqualified()) {
            result.finish(System.currentTimeMillis());
            result.setPlacement(nextPlacement++);
        }
    }
    
    /**
     * Disqualify a player
     */
    public void disqualifyPlayer(UUID playerId, String reason) {
        PlayerResult result = players.get(playerId);
        if (result != null) {
            result.disqualify(reason);
        }
    }
    
    /**
     * Check if race should end (all players finished/DQ'd or timeout)
     */
    public boolean shouldEnd() {
        if (state != State.RUNNING) {
            return false;
        }
        
        // Check if all players are finished or disqualified
        for (PlayerResult result : players.values()) {
            if (!result.isFinished() && !result.isDisqualified()) {
                return false; // At least one player still racing
            }
        }
        
        return true; // All players done
    }
    
    /**
     * End the race and calculate final results
     */
    public List<PlayerResult> endRace() {
        state = State.FINISHED;
        
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        
        // Return sorted results (finished players by placement, then DQ'd players)
        List<PlayerResult> results = new ArrayList<>(players.values());
        results.sort((a, b) -> {
            if (a.isDisqualified() && !b.isDisqualified()) return 1;
            if (!a.isDisqualified() && b.isDisqualified()) return -1;
            if (a.isDisqualified() && b.isDisqualified()) return 0;
            return Integer.compare(a.getPlacement(), b.getPlacement());
        });
        
        return results;
    }
    
    /**
     * Cancel the race
     */
    public void cancelRace() {
        state = State.CANCELLED;
        
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }
    
    /**
     * Get boat spawn location for a player
     */
    public Location getPlayerBoatSpawn(UUID playerId) {
        Integer spawnIndex = playerBoatSpawns.get(playerId);
        if (spawnIndex == null || spawnIndex >= assignedBoatSpawns.size()) {
            return null;
        }
        return assignedBoatSpawns.get(spawnIndex);
    }
    
    /**
     * Check if player can join (not full, in lobby state)
     */
    public boolean canJoin() {
        return state == State.LOBBY && players.size() < assignedBoatSpawns.size();
    }
    
    /**
     * Get race duration in milliseconds (for running races)
     */
    public long getRaceDurationMs() {
        if (state == State.RUNNING) {
            return System.currentTimeMillis() - raceStartTimeMs;
        }
        return 0;
    }
    
    // Getters
    public String getRaceId() { return raceId; }
    public Course getCourse() { return course; }
    public UUID getLeaderId() { return leaderId; }
    public String getLeaderName() { return leaderName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public State getState() { return state; }
    public Map<UUID, PlayerResult> getPlayers() { return players; }
    public int getPlayerCount() { return players.size(); }
    public long getTimeoutMs() { return timeoutMs; }
    public long getRaceStartTimeMs() { return raceStartTimeMs; }
    
    public void setTimeoutTask(BukkitTask timeoutTask) { this.timeoutTask = timeoutTask; }
    public BukkitTask getTimeoutTask() { return timeoutTask; }
}
