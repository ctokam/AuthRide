package com.example.authride.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routes")
public class Route {

    @PrimaryKey(autoGenerate = true)
    private int id; // Το μοναδικό ID της διαδρομής

    private int driverId; // Το ID του χρήστη που οδηγεί (για να ξέρουμε ποιανού είναι)

    private String startLocation;
    private String intermediateStops;
    private String endLocation;
    private String departureTime;

    private int availableSeats; // Πόσες θέσεις έμειναν

    // Constructor
    public Route(int driverId, String startLocation, String intermediateStops, String endLocation, String departureTime, int availableSeats) {
        this.driverId = driverId;
        this.startLocation = startLocation;
        this.intermediateStops = intermediateStops;
        this.endLocation = endLocation;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDriverId() { return driverId; }
    public void setDriverId(int driverId) { this.driverId = driverId; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getIntermediateStops() { return intermediateStops; }
    public void setIntermediateStops(String intermediateStops) { this.intermediateStops = intermediateStops; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
}