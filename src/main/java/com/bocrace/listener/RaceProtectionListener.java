package com.bocrace.listener;

import com.bocrace.BOCRacePlugin;
import com.bocrace.race.MultiplayerRace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles player protection and effects during multiplayer races
 * Includes damage prevention, death handling, and night vision
 */
public class RaceProtectionListener implements Listener {
    
    private final BOCRacePlugin plugin;
    
    public RaceProtectionListener(BOCRacePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent damage during multiplayer races
     */
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if player is in a multiplayer race
        if (!plugin.getMultiplayerRaceManager().isPlayerInRace(player.getUniqueId())) {
            return;
        }
        
        // Check config for protection settings
        if (!plugin.getConfig().getBoolean("multiplayer.player-protection.enabled", true)) {
            return; // Protection disabled
        }
        
        // Determine damage type and handle accordingly
        EntityDamageEvent.DamageCause cause = event.getCause();
        
        switch (cause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                // Mob damage
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-mob-damage", true)) {
                    event.setCancelled(true);
                    // Only log occasionally to prevent spam
                    if (Math.random() < 0.1) { // 10% chance to log
                        plugin.multiplayerDebugLog("Prevented mob damage to " + player.getName() + " during race");
                    }
                }
                break;
                
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
                // Explosion damage
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-explosion-damage", true)) {
                    event.setCancelled(true);
                    player.sendMessage("§e⚡ Protected from explosions during race!");
                    plugin.multiplayerDebugLog("Prevented explosion damage to " + player.getName() + " during race");
                }
                break;
                
            case FALL:
                // Fall damage (might want to keep for course obstacles)
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-fall-damage", false)) {
                    event.setCancelled(true);
                    plugin.multiplayerDebugLog("Prevented fall damage to " + player.getName() + " during race");
                }
                break;
                
            case DROWNING:
                // Drowning (important for water courses)
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-drowning", true)) {
                    event.setCancelled(true);
                    plugin.multiplayerDebugLog("Prevented drowning damage to " + player.getName() + " during race");
                }
                break;
                
            case PROJECTILE:
                // PvP projectiles
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-pvp", true)) {
                    event.setCancelled(true);
                    player.sendMessage("§e⚡ PvP disabled during races!");
                    plugin.multiplayerDebugLog("Prevented PvP damage to " + player.getName() + " during race");
                }
                break;
                
            default:
                // Other damage types - check general protection
                if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-all-damage", false)) {
                    event.setCancelled(true);
                    plugin.multiplayerDebugLog("Prevented " + cause + " damage to " + player.getName() + " during race");
                }
                break;
        }
    }
    
    /**
     * Handle player death during races (backup protection)
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if player is in a multiplayer race
        MultiplayerRace race = plugin.getMultiplayerRaceManager().getRaceByPlayer(player.getUniqueId());
        if (race == null) {
            return;
        }
        
        plugin.multiplayerDebugLog("Player " + player.getName() + " died during race - handling...");
        
        // Prevent item drops during races
        if (plugin.getConfig().getBoolean("multiplayer.player-protection.prevent-item-drops", true)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            plugin.multiplayerDebugLog("Prevented item drops for " + player.getName());
        }
        
        // Handle death based on config
        if (plugin.getConfig().getBoolean("multiplayer.player-protection.death-disqualifies", true)) {
            // DQ the player
            plugin.getMultiplayerRaceManager().disqualifyPlayer(player.getUniqueId(), "Died during race");
            
            // Set respawn location to race lobby
            if (race.getCourse().getMpraceLobbySpawn() != null) {
                event.getEntity().setBedSpawnLocation(race.getCourse().getMpraceLobbySpawn(), true);
            }
        }
    }
    
    /**
     * Apply night vision when player joins a race
     */
    public void applyRaceEffects(Player player) {
        if (!plugin.getConfig().getBoolean("multiplayer.effects.night-vision.enabled", true)) {
            return;
        }
        
        // Apply night vision (amplifier 0 = lowest level, duration in ticks)
        int durationSeconds = plugin.getConfig().getInt("multiplayer.effects.night-vision.duration", 600); // 10 minutes default
        int durationTicks = durationSeconds * 20; // Convert to ticks
        
        PotionEffect nightVision = new PotionEffect(
            PotionEffectType.NIGHT_VISION,
            durationTicks,
            0, // Amplifier 0 = lowest level
            false, // Not ambient
            false  // Hide particles for cleaner racing
        );
        
        player.addPotionEffect(nightVision);
        player.sendMessage("§e✨ Night vision applied for better racing visibility!");
        
        plugin.multiplayerDebugLog("Applied night vision to " + player.getName() + 
                                 " for " + durationSeconds + " seconds");
    }
    
    /**
     * Remove race effects when player leaves race
     */
    public void removeRaceEffects(Player player) {
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            plugin.multiplayerDebugLog("Removed night vision from " + player.getName());
        }
    }
    
    /**
     * Prevent boat damage during multiplayer races
     */
    @EventHandler
    public void onBoatDamage(VehicleDamageEvent event) {
        if (!(event.getVehicle() instanceof Boat)) {
            return;
        }
        
        Boat boat = (Boat) event.getVehicle();
        
        // Check if it's a race boat
        if (!plugin.getBoatManager().isRaceBoat(boat)) {
            return;
        }
        
        // Get the player in the boat
        if (boat.getPassengers().isEmpty()) {
            return;
        }
        
        if (!(boat.getPassengers().get(0) instanceof Player)) {
            return;
        }
        
        Player player = (Player) boat.getPassengers().get(0);
        
        // Check if player is in a multiplayer race
        if (!plugin.getMultiplayerRaceManager().isPlayerInRace(player.getUniqueId())) {
            return;
        }
        
        // Check config for boat protection
        if (plugin.getConfig().getBoolean("multiplayer.player-protection.protect-boats", true)) {
            event.setCancelled(true);
            plugin.multiplayerDebugLog("Prevented boat damage for " + player.getName() + " during race");
            
            // Notify attacker if it's a player
            if (event.getAttacker() instanceof Player) {
                Player attacker = (Player) event.getAttacker();
                attacker.sendMessage("§c❌ Cannot damage boats during races!");
            }
        }
    }
    
    /**
     * Prevent boat destruction during multiplayer races
     */
    @EventHandler
    public void onBoatDestroy(VehicleDestroyEvent event) {
        if (!(event.getVehicle() instanceof Boat)) {
            return;
        }
        
        Boat boat = (Boat) event.getVehicle();
        
        // Check if it's a race boat
        if (!plugin.getBoatManager().isRaceBoat(boat)) {
            return;
        }
        
        // Get the player in the boat
        if (boat.getPassengers().isEmpty()) {
            return;
        }
        
        if (!(boat.getPassengers().get(0) instanceof Player)) {
            return;
        }
        
        Player player = (Player) boat.getPassengers().get(0);
        
        // Check if player is in a multiplayer race
        if (!plugin.getMultiplayerRaceManager().isPlayerInRace(player.getUniqueId())) {
            return;
        }
        
        // Check config for boat protection
        if (plugin.getConfig().getBoolean("multiplayer.player-protection.protect-boats", true)) {
            event.setCancelled(true);
            plugin.multiplayerDebugLog("Prevented boat destruction for " + player.getName() + " during race");
            
            // Notify attacker if it's a player
            if (event.getAttacker() instanceof Player) {
                Player attacker = (Player) event.getAttacker();
                attacker.sendMessage("§c❌ Cannot destroy boats during races!");
            }
        }
    }
}
