package com.bocrace.util;

import org.bukkit.Location;

/**
 * Utility for detecting when boats cross start/finish lines
 */
public class LineDetection {
    
    /**
     * Check if a boat crossed a thin plane (start line)
     * Uses the two points to create a line, checks if movement crossed it
     */
    public static boolean crossedStartLine(Location from, Location to, Location point1, Location point2) {
        if (from == null || to == null || point1 == null || point2 == null) return false;
        if (!from.getWorld().equals(to.getWorld()) || !from.getWorld().equals(point1.getWorld())) return false;
        
        // Determine if the line is primarily X or Z oriented
        double deltaX = Math.abs(point2.getX() - point1.getX());
        double deltaZ = Math.abs(point2.getZ() - point1.getZ());
        
        if (deltaX > deltaZ) {
            // Line is primarily X-oriented (horizontal), check Z crossing
            return crossedHorizontalLine(from, to, point1, point2);
        } else {
            // Line is primarily Z-oriented (vertical), check X crossing
            return crossedVerticalLine(from, to, point1, point2);
        }
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
     * Check crossing of a horizontal line (X-oriented)
     */
    private static boolean crossedHorizontalLine(Location from, Location to, Location point1, Location point2) {
        // Line runs along X-axis, we check Z crossing
        double lineZ = (point1.getZ() + point2.getZ()) / 2.0; // Average Z of the line
        double lineY = (point1.getY() + point2.getY()) / 2.0; // Average Y of the line
        
        // Check if movement crossed the Z plane
        boolean fromBefore = from.getZ() < lineZ;
        boolean toAfter = to.getZ() >= lineZ;
        boolean crossed = fromBefore && toAfter;
        
        if (!crossed) return false;
        
        // Check if crossing happened within the X bounds of the line
        double minX = Math.min(point1.getX(), point2.getX()) - 0.5;
        double maxX = Math.max(point1.getX(), point2.getX()) + 0.5;
        
        // Check if crossing happened at the right height (within 2 blocks)
        double heightDiff = Math.abs(to.getY() - lineY);
        
        return (to.getX() >= minX && to.getX() <= maxX) && heightDiff <= 2.0;
    }
    
    /**
     * Check crossing of a vertical line (Z-oriented)
     */
    private static boolean crossedVerticalLine(Location from, Location to, Location point1, Location point2) {
        // Line runs along Z-axis, we check X crossing
        double lineX = (point1.getX() + point2.getX()) / 2.0; // Average X of the line
        double lineY = (point1.getY() + point2.getY()) / 2.0; // Average Y of the line
        
        // Check if movement crossed the X plane
        boolean fromBefore = from.getX() < lineX;
        boolean toAfter = to.getX() >= lineX;
        boolean crossed = fromBefore && toAfter;
        
        if (!crossed) return false;
        
        // Check if crossing happened within the Z bounds of the line
        double minZ = Math.min(point1.getZ(), point2.getZ()) - 0.5;
        double maxZ = Math.max(point1.getZ(), point2.getZ()) + 0.5;
        
        // Check if crossing happened at the right height (within 2 blocks)
        double heightDiff = Math.abs(to.getY() - lineY);
        
        return (to.getZ() >= minZ && to.getZ() <= maxZ) && heightDiff <= 2.0;
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
