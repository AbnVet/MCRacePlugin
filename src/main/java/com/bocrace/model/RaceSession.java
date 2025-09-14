package com.bocrace.model;

import org.bukkit.Location;
import org.bukkit.entity.Boat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active race session
 */
public class RaceSession {
    private final String sessionId;
    private final Course course;
    private final RaceType raceType;
    private final UUID leader;
    private final Set<UUID> participants;
    private final Map<UUID, PlayerRaceData> playerData;
    private final long startTime;
    private long endTime;
    private RaceStatus status;
    private final int maxPlayers;
    
    public RaceSession(Course course, RaceType raceType, UUID leader, int maxPlayers) {
        this.sessionId = UUID.randomUUID().toString();
        this.course = course;
        this.raceType = raceType;
        this.leader = leader;
        this.maxPlayers = maxPlayers;
        this.participants = ConcurrentHashMap.newKeySet();
        this.playerData = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
        this.status = RaceStatus.WAITING;
        
        // Add leader as first participant
        this.participants.add(leader);
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public Course getCourse() { return course; }
    public RaceType getRaceType() { return raceType; }
    public UUID getLeader() { return leader; }
    public Set<UUID> getParticipants() { return participants; }
    public Map<UUID, PlayerRaceData> getPlayerData() { return playerData; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public RaceStatus getStatus() { return status; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCurrentPlayerCount() { return participants.size(); }
    
    // Setters
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setStatus(RaceStatus status) { this.status = status; }
    
    /**
     * Add a player to the race session
     */
    public boolean addParticipant(UUID playerId) {
        if (participants.size() >= maxPlayers) {
            return false;
        }
        participants.add(playerId);
        playerData.put(playerId, new PlayerRaceData(playerId));
        return true;
    }
    
    /**
     * Remove a player from the race session
     */
    public boolean removeParticipant(UUID playerId) {
        boolean removed = participants.remove(playerId);
        if (removed) {
            playerData.remove(playerId);
        }
        return removed;
    }
    
    /**
     * Check if a player is in this race session
     */
    public boolean hasParticipant(UUID playerId) {
        return participants.contains(playerId);
    }
    
    /**
     * Get player race data
     */
    public PlayerRaceData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }
    
    /**
     * Check if the race is full
     */
    public boolean isFull() {
        return participants.size() >= maxPlayers;
    }
    
    /**
     * Check if the race is active (started but not finished)
     */
    public boolean isActive() {
        return status == RaceStatus.ACTIVE;
    }
    
    /**
     * Check if the race is finished
     */
    public boolean isFinished() {
        return status == RaceStatus.FINISHED || status == RaceStatus.CANCELLED;
    }
    
    /**
     * Get the duration of the race in milliseconds
     */
    public long getDuration() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }
    
    /**
     * Get the duration of the race in seconds
     */
    public double getDurationSeconds() {
        return getDuration() / 1000.0;
    }
    
    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        double seconds = getDurationSeconds();
        int minutes = (int) (seconds / 60);
        seconds = seconds % 60;
        return String.format("%d:%05.2f", minutes, seconds);
    }
    
    /**
     * Get race results sorted by finish time
     */
    public List<PlayerRaceData> getResults() {
        return playerData.values().stream()
                .filter(data -> data.getFinishTime() > 0)
                .sorted(Comparator.comparingLong(PlayerRaceData::getFinishTime))
                .toList();
    }
    
    /**
     * Get race results sorted by finish position
     */
    public List<PlayerRaceData> getResultsByPosition() {
        return playerData.values().stream()
                .filter(data -> data.getFinishPosition() > 0)
                .sorted(Comparator.comparingInt(PlayerRaceData::getFinishPosition))
                .toList();
    }
    
    public enum RaceType {
        SINGLEPLAYER,
        MULTIPLAYER
    }
    
    public enum RaceStatus {
        WAITING,    // Waiting for players to join
        STARTING,   // Race is about to start
        ACTIVE,     // Race is in progress
        FINISHED,   // Race completed normally
        CANCELLED,  // Race was cancelled
        TIMEOUT     // Race timed out
    }
    
    /**
     * Data for a single player in a race
     */
    public static class PlayerRaceData {
        private final UUID playerId;
        private Location spawnLocation;
        private Boat boat;
        private long raceStartTime;
        private long finishTime;
        private int finishPosition;
        private boolean disqualified;
        private String disqualificationReason;
        
        public PlayerRaceData(UUID playerId) {
            this.playerId = playerId;
            this.finishPosition = -1;
            this.disqualified = false;
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public Location getSpawnLocation() { return spawnLocation; }
        public Boat getBoat() { return boat; }
        public long getRaceStartTime() { return raceStartTime; }
        public long getFinishTime() { return finishTime; }
        public int getFinishPosition() { return finishPosition; }
        public boolean isDisqualified() { return disqualified; }
        public String getDisqualificationReason() { return disqualificationReason; }
        
        // Setters
        public void setSpawnLocation(Location spawnLocation) { this.spawnLocation = spawnLocation; }
        public void setBoat(Boat boat) { this.boat = boat; }
        public void setRaceStartTime(long raceStartTime) { this.raceStartTime = raceStartTime; }
        public void setFinishTime(long finishTime) { this.finishTime = finishTime; }
        public void setFinishPosition(int finishPosition) { this.finishPosition = finishPosition; }
        public void setDisqualified(boolean disqualified) { this.disqualified = disqualified; }
        public void setDisqualificationReason(String disqualificationReason) { this.disqualificationReason = disqualificationReason; }
        
        /**
         * Get the race time in milliseconds
         */
        public long getRaceTime() {
            if (raceStartTime == 0 || finishTime == 0) return 0;
            return finishTime - raceStartTime;
        }
        
        /**
         * Get the race time in seconds
         */
        public double getRaceTimeSeconds() {
            return getRaceTime() / 1000.0;
        }
        
        /**
         * Get formatted race time string
         */
        public String getFormattedRaceTime() {
            double seconds = getRaceTimeSeconds();
            int minutes = (int) (seconds / 60);
            seconds = seconds % 60;
            return String.format("%d:%05.2f", minutes, seconds);
        }
        
        /**
         * Check if the player has finished the race
         */
        public boolean hasFinished() {
            return finishTime > 0 && !disqualified;
        }
    }
}
