package com.bocrace.model;

import org.bukkit.Location;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Course {
    
    private String name;
    private CourseType type;
    private String prefix;
    private String createdBy;
    private LocalDateTime createdOn;
    private LocalDateTime lastEdited;
    private Map<String, Object> data;
    
    // Singleplayer race components
    private Location spstartbutton;
    private Location spboatspawn;
    
    // Race line locations
    private Location spstart1;
    private Location spstart2;
    private Location spfinish1;
    private Location spfinish2;
    private Location spreturn;
    
    // Lobby locations
    private Location spcourselobby;
    private Location spmainlobby;
    
    
    // Default constructor
    public Course() {
        this.data = new HashMap<>();
        this.createdOn = LocalDateTime.now();
        this.lastEdited = LocalDateTime.now();
    }
    
    // Constructor with required fields
    public Course(String name, CourseType type, String createdBy) {
        this();
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
        this.prefix = "[BOCRace]"; // Default prefix
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public CourseType getType() {
        return type;
    }
    
    public void setType(CourseType type) {
        this.type = type;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedOn() {
        return createdOn;
    }
    
    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }
    
    public LocalDateTime getLastEdited() {
        return lastEdited;
    }
    
    public void setLastEdited(LocalDateTime lastEdited) {
        this.lastEdited = lastEdited;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    // Singleplayer component getters and setters
    public Location getSpstartbutton() {
        return spstartbutton;
    }
    
    public void setSpstartbutton(Location spstartbutton) {
        this.spstartbutton = spstartbutton;
    }
    
    public Location getSpboatspawn() {
        return spboatspawn;
    }
    
    public void setSpboatspawn(Location spboatspawn) {
        this.spboatspawn = spboatspawn;
    }
    
    // Race line getters and setters
    public Location getSpstart1() { return spstart1; }
    public void setSpstart1(Location spstart1) { this.spstart1 = spstart1; }
    
    public Location getSpstart2() { return spstart2; }
    public void setSpstart2(Location spstart2) { this.spstart2 = spstart2; }
    
    public Location getSpfinish1() { return spfinish1; }
    public void setSpfinish1(Location spfinish1) { this.spfinish1 = spfinish1; }
    
    public Location getSpfinish2() { return spfinish2; }
    public void setSpfinish2(Location spfinish2) { this.spfinish2 = spfinish2; }
    
    public Location getSpreturn() { return spreturn; }
    public void setSpreturn(Location spreturn) { this.spreturn = spreturn; }
    
    // Lobby getters and setters
    public Location getSpcourselobby() { return spcourselobby; }
    public void setSpcourselobby(Location spcourselobby) { this.spcourselobby = spcourselobby; }
    
    public Location getSpmainlobby() { return spmainlobby; }
    public void setSpmainlobby(Location spmainlobby) { this.spmainlobby = spmainlobby; }
    
    
    // Helper methods
    public void updateLastEdited() {
        this.lastEdited = LocalDateTime.now();
    }
    
    public String getDisplayName() {
        return name + " (" + type + ")";
    }
    
    public String getPrefixDisplay() {
        return prefix != null ? prefix : "[BOCRace]";
    }
}
