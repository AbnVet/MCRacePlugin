package com.bocrace.model;

import java.time.LocalDateTime;

/**
 * Represents a race record with time, player, course, and metadata
 */
public class RaceRecord {
    private String player;
    private String course;
    private double time;
    private LocalDateTime date;
    private CourseType type;
    
    // Default constructor
    public RaceRecord() {
        this.date = LocalDateTime.now();
    }
    
    // Full constructor
    public RaceRecord(String player, String course, double time, CourseType type) {
        this.player = player;
        this.course = course;
        this.time = time;
        this.type = type;
        this.date = LocalDateTime.now();
    }
    
    // Full constructor with date
    public RaceRecord(String player, String course, double time, LocalDateTime date, CourseType type) {
        this.player = player;
        this.course = course;
        this.time = time;
        this.date = date;
        this.type = type;
    }
    
    // Getters and setters
    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }
    
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    
    public double getTime() { return time; }
    public void setTime(double time) { this.time = time; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public CourseType getType() { return type; }
    public void setType(CourseType type) { this.type = type; }
    
    // Helper methods
    public String getFormattedTime() {
        return String.format("%.2f", time);
    }
    
    public String getFormattedDate() {
        return date.toString().replace("T", " ");
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s (%s) - %s", 
            player, getFormattedTime(), course, getFormattedDate());
    }
}
