package com.bocrace.util;

import com.bocrace.BOCRacePlugin;
import com.bocrace.model.Course;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Manages sound effects and particles for the race plugin
 * Uses configuration settings for customizable audio/visual feedback
 */
public class SoundEffectManager {
    private final BOCRacePlugin plugin;
    private final Logger logger;
    private final boolean soundsEnabled;
    private final boolean particlesEnabled;
    
    public SoundEffectManager(BOCRacePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.soundsEnabled = plugin.getConfig().getBoolean("sounds.enabled", true);
        this.particlesEnabled = plugin.getConfig().getBoolean("particles.enabled", true);
    }
    
    /**
     * Plays race start effects
     */
    public void playRaceStartEffects(Player player, Location location, Course course) {
        boolean courseSoundsEnabled = course != null ? course.areSoundsEnabled(plugin) : soundsEnabled;
        boolean courseParticlesEnabled = course != null ? course.areParticlesEnabled(plugin) : particlesEnabled;
        
        if (courseSoundsEnabled) {
            // Bell sound for race start (configurable)
            String soundName = plugin.getConfig().getString("sounds.race-start", "BLOCK_NOTE_BLOCK_BELL");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                player.playSound(location, sound, 1.0f, 1.5f);
            }
            // Additional ding for emphasis
            player.playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
        }
        
        if (courseParticlesEnabled) {
            String particleName = plugin.getConfig().getString("particles.race-start", "FIREWORK");
            Particle particle = parseParticle(particleName);
            if (particle != null) {
                player.spawnParticle(particle, location, 20, 1, 1, 1, 0.1);
            }
        }
    }
    
    /**
     * Plays race finish effects
     */
    public void playRaceFinishEffects(Player player, Location location, Course course) {
        boolean courseSoundsEnabled = course != null ? course.areSoundsEnabled(plugin) : soundsEnabled;
        boolean courseParticlesEnabled = course != null ? course.areParticlesEnabled(plugin) : particlesEnabled;
        
        logger.info("🐛 DEBUG: playRaceFinishEffects called - courseSoundsEnabled: " + courseSoundsEnabled);
        if (courseSoundsEnabled) {
            // Primary finish sound
            String soundName = plugin.getConfig().getString("sounds.race-finish", "ENTITY_PLAYER_LEVELUP");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                logger.info("🐛 DEBUG: Playing primary finish sound: " + soundName);
                player.playSound(location, sound, 1.0f, 1.0f);
            }
            
            // Fireworks sound for dramatic effect (LOUDER)
            logger.info("🐛 DEBUG: Playing fireworks sounds...");
            player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 1.0f);
            player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 1.2f);
            player.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2.0f, 1.5f);
            logger.info("🐛 DEBUG: Fireworks sounds played!");
        } else {
            logger.info("🐛 DEBUG: Sounds disabled for this course, skipping finish effects");
        }
        
        if (courseParticlesEnabled) {
            String particleName = plugin.getConfig().getString("particles.race-finish", "VILLAGER_HAPPY");
            Particle particle = parseParticle(particleName);
            if (particle != null) {
                player.spawnParticle(particle, location, 50, 2, 2, 2, 0.1);
            }
        }
    }
    
    /**
     * Plays personal best effects
     */
    public void playPersonalBestEffects(Player player, Location location) {
        if (soundsEnabled) {
            String soundName = plugin.getConfig().getString("sounds.personal-best", "UI_TOAST_CHALLENGE_COMPLETE");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                player.playSound(location, sound, 1.0f, 2.0f);
            }
        }
        
        if (particlesEnabled) {
            // Extra sparkly effects for personal best
            player.spawnParticle(Particle.FIREWORK, location, 30, 2, 2, 2, 0.2);
        }
    }
    
    /**
     * Plays boat spawn effects
     */
    public void playBoatSpawnEffects(Player player, Location location) {
        if (soundsEnabled) {
            player.playSound(location, Sound.ENTITY_BOAT_PADDLE_WATER, 1.0f, 1.0f);
            player.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }
        
        if (particlesEnabled) {
            String particleName = plugin.getConfig().getString("particles.boat-spawn", "SPLASH");
            Particle particle = parseParticle(particleName);
            if (particle != null) {
                player.spawnParticle(particle, location, 20, 1, 0.5, 1, 0.1);
            }
        }
    }
    
    /**
     * Plays setup success effects
     */
    public void playSetupSuccessEffects(Player player, Location location) {
        if (soundsEnabled) {
            String soundName = plugin.getConfig().getString("sounds.setup-success", "ENTITY_EXPERIENCE_ORB_PICKUP");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                player.playSound(location, sound, 1.0f, 1.5f);
            }
        }
        
        if (particlesEnabled) {
            String particleName = plugin.getConfig().getString("particles.setup-success", "VILLAGER_HAPPY");
            Particle particle = parseParticle(particleName);
            if (particle != null) {
                player.spawnParticle(particle, location, 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
    
    /**
     * Plays setup enter effects
     */
    public void playSetupEnterEffects(Player player, Location location) {
        if (soundsEnabled) {
            String soundName = plugin.getConfig().getString("sounds.setup-enter", "BLOCK_NOTE_BLOCK_PLING");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                player.playSound(location, sound, 1.0f, 1.5f);
            }
        }
    }
    
    /**
     * Plays error effects
     */
    public void playErrorEffects(Player player, Location location) {
        if (soundsEnabled) {
            String soundName = plugin.getConfig().getString("sounds.error", "ENTITY_VILLAGER_NO");
            Sound sound = parseSound(soundName);
            if (sound != null) {
                player.playSound(location, sound, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Plays disqualification effects
     */
    public void playDisqualificationEffects(Player player, Location location) {
        if (soundsEnabled) {
            player.playSound(location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            // Additional dramatic sound for DQ
            player.playSound(location, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
        }
        
        if (particlesEnabled) {
            // Red particles for disqualification
            player.spawnParticle(Particle.FLAME, location, 20, 1, 1, 1, 0.1);
        }
    }
    
    /**
     * Parses a sound name from config to Sound enum
     */
    private Sound parseSound(String soundName) {
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid sound name in config: " + soundName);
            return null;
        }
    }
    
    /**
     * Parses a particle name from config to Particle enum
     */
    private Particle parseParticle(String particleName) {
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid particle name in config: " + particleName);
            return null;
        }
    }
    
    /**
     * Checks if sounds are enabled
     */
    public boolean areSoundsEnabled() {
        return soundsEnabled;
    }
    
    /**
     * Checks if particles are enabled
     */
    public boolean areParticlesEnabled() {
        return particlesEnabled;
    }
}
