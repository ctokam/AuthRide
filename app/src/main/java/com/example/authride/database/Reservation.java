package com.example.authride.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reservations")
public class Reservation {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int passengerId; // Ποιος φοιτητής έκλεισε τη θέση
    private int routeId;     // Σε ποια διαδρομή την έκλεισε

    public Reservation(int passengerId, int routeId) {
        this.passengerId = passengerId;
        this.routeId = routeId;
    }

    // Getters Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPassengerId() { return passengerId; }
    public void setPassengerId(int passengerId) { this.passengerId = passengerId; }

    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }
}