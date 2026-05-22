package com.example.authride.models;

public class Reservation {
    private String id;
    private String passengerId;
    private String routeId;

    // ΑΠΑΡΑΙΤΗΤΟ για το Firebase
    public Reservation() {}

    public Reservation(String passengerId, String routeId) {
        this.passengerId = passengerId;
        this.routeId = routeId;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }
}