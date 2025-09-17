package com.bocrace.util;

import org.bukkit.Location;

/**
 * Utility for detecting when boats cross start/finish lines
 */
public class LineDetection {
    
    /**
     * Check if a boat ENTERED the start line zone (RaceOG style)
     * Much more forgiving than precise plane crossing
     */
    public static boolean crossedStartLine(Location from, Location to, Location point1, Location point2) {
        if (from == null || to == null || point1 == null || point2 == null) return false;
        if (!from.getWorld().equals(to.getWorld()) || !from.getWorld().equals(point1.getWorld())) return false;
        
        // Check if player ENTERED the start zone (wasn't inside, now is inside)
        boolean wasInside = insideStartZone(from, point1, point2);
        boolean nowInside = insideStartZone(to, point1, point2);
        
        return !wasInside && nowInside;
    }
    
    /**
     * Check if a boat entered a forgiving cuboid (finish line)
     * Creates a 3-4 block wide detection zone
     */
    public static boolean enteredFinishZone(Location boatLocation, Location point1, Location point2) {
        if (boatLocation == null || point1 == null || point2 == null) return false;
        if (!boatLocation.getWorld().equals(point1.getWorld())) return false;
        
        // Create expanded cuboid bounds
        double minX = Math.min(point1.getX(), point2.getX()) - 1.5; // 1.5 block expansion
        double maxX = Math.max(point1.getX(), point2.getX()) + 1.5;
        double minZ = Math.min(point1.getZ(), point2.getZ()) - 1.5;
        double maxZ = Math.max(point1.getZ(), point2.getZ()) + 1.5;
        
        // Y range - allow 3 blocks up and 1 block down from the line points
        double minY = Math.min(point1.getY(), point2.getY()) - 1.0;
        double maxY = Math.max(point1.getY(), point2.getY()) + 3.0;
        
        // Check if boat is within the expanded cuboid
        double bx = boatLocation.getX();
        double by = boatLocation.getY();
        double bz = boatLocation.getZ();
        
        boolean inZone = (bx >= minX && bx <= maxX) && 
                        (by >= minY && by <= maxY) && 
                        (bz >= minZ && bz <= maxZ);
        
        return inZone;
    }
    
    /**
     * Check if boat is inside the start line zone (RaceOG style)
     * Forgiving zone detection instead of precise plane crossing
     */
    private static boolean insideStartZone(Location loc, Location point1, Location point2) {
        if (loc == null || point1 == null || point2 == null) return false;
        
        // Get the line bounds
        double minX = Math.min(point1.getX(), point2.getX());
        double maxX = Math.max(point1.getX(), point2.getX()) + 1.0; // Include block width
        double minZ = Math.min(point1.getZ(), point2.getZ());
        double maxZ = Math.max(point1.getZ(), point2.getZ()) + 1.0;
        double minY = Math.min(point1.getY(), point2.getY()) - 1.0; // 1 block below
        double maxY = Math.max(point1.getY(), point2.getY()) + 3.0; // 3 blocks above
        
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        
        // Determine if this is a thin line or rectangle
        double deltaX = Math.abs(point2.getX() - point1.getX());
        double deltaZ = Math.abs(point2.getZ() - point1.getZ());
        
        if (deltaX > deltaZ) {
            // Horizontal line (X-oriented) - check if within X bounds and close to Z center
            double zCenter = (point1.getZ() + point2.getZ()) / 2.0 + 0.5; // Block center
            boolean withinX = x >= minX && x <= maxX;
            boolean withinThinZ = Math.abs(z - zCenter) <= 0.6; // 0.6 block tolerance (RaceOG style)
            boolean withinY = y >= minY && y <= maxY;
            return withinX && withinThinZ && withinY;
        } else {
            // Vertical line (Z-oriented) - check if within Z bounds and close to X center
            double xCenter = (point1.getX() + point2.getX()) / 2.0 + 0.5; // Block center
            boolean withinZ = z >= minZ && z <= maxZ;
            boolean withinThinX = Math.abs(x - xCenter) <= 0.6; // 0.6 block tolerance (RaceOG style)
            boolean withinY = y >= minY && y <= maxY;
            return withinZ && withinThinX && withinY;
        }
    }
    
    /**
     * Get start zone bounds for debugging
     */
    public static String getStartZoneDescription(Location boatLocation, Location point1, Location point2) {
        if (boatLocation == null || point1 == null || point2 == null) return "START ZONE: Invalid locations";
        
        double deltaX = Math.abs(point2.getX() - point1.getX());
        double deltaZ = Math.abs(point2.getZ() - point1.getZ());
        
        if (deltaX > deltaZ) {
            // Horizontal line
            double zCenter = (point1.getZ() + point2.getZ()) / 2.0 + 0.5;
            double minX = Math.min(point1.getX(), point2.getX());
            double maxX = Math.max(point1.getX(), point2.getX()) + 1.0;
            return String.format("START ZONE: Horizontal line X[%.1f to %.1f], Z center %.1f (±0.6) | Boat at: (%.1f,%.1f,%.1f)",
                    minX, maxX, zCenter, boatLocation.getX(), boatLocation.getY(), boatLocation.getZ());
        } else {
            // Vertical line
            double xCenter = (point1.getX() + point2.getX()) / 2.0 + 0.5;
            double minZ = Math.min(point1.getZ(), point2.getZ());
            double maxZ = Math.max(point1.getZ(), point2.getZ()) + 1.0;
            return String.format("START ZONE: Vertical line Z[%.1f to %.1f], X center %.1f (±0.6) | Boat at: (%.1f,%.1f,%.1f)",
                    minZ, maxZ, xCenter, boatLocation.getX(), boatLocation.getY(), boatLocation.getZ());
        }
    }
    
    /**
     * Get finish zone bounds for debugging
     */
    public static String getFinishZoneDescription(Location boatLocation, Location point1, Location point2) {
        if (boatLocation == null || point1 == null || point2 == null) return "FINISH ZONE: Invalid locations";
        
        double minX = Math.min(point1.getX(), point2.getX()) - 1.5;
        double maxX = Math.max(point1.getX(), point2.getX()) + 1.5;
        double minZ = Math.min(point1.getZ(), point2.getZ()) - 1.5;
        double maxZ = Math.max(point1.getZ(), point2.getZ()) + 1.5;
        double minY = Math.min(point1.getY(), point2.getY()) - 1.0;
        double maxY = Math.max(point1.getY(), point2.getY()) + 3.0;
        
        return String.format("FINISH ZONE: X[%.1f to %.1f], Y[%.1f to %.1f], Z[%.1f to %.1f] | Boat at: (%.1f,%.1f,%.1f)",
                minX, maxX, minY, maxY, minZ, maxZ, 
                boatLocation.getX(), boatLocation.getY(), boatLocation.getZ());
    }
    
    /**
     * Get a description of the line for debugging
     */
    public static String getLineDescription(Location point1, Location point2, String lineType) {
        if (point1 == null || point2 == null) return lineType + ": NOT SET";
        
        double deltaX = Math.abs(point2.getX() - point1.getX());
        double deltaZ = Math.abs(point2.getZ() - point1.getZ());
        String orientation = (deltaX > deltaZ) ? "horizontal" : "vertical";
        
        return String.format("%s: %s line from (%d,%d,%d) to (%d,%d,%d)", 
                lineType, orientation,
                point1.getBlockX(), point1.getBlockY(), point1.getBlockZ(),
                point2.getBlockX(), point2.getBlockY(), point2.getBlockZ());
    }
}
