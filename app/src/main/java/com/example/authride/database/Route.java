package com.example.authride.models;

public class Route {
    private String id;
    private String driverId;
    private String startLocation;
    private String endLocation;
    private String departureTime;
    private int availableSeats;
    private String meetingInstructions; // π.χ. "Στη στάση απέναντι από τη Λέσχη"
    private String contribution; // π.χ. "2€" ή "Δωρεάν"

    // ΑΠΑΡΑΙΤΗΤΟ για το Firebase
    public Route() {}

    public Route(String driverId, String startLocation, String endLocation,
                 String departureTime, int availableSeats, String meetingInstructions, String contribution) {
        this.driverId = driverId;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.meetingInstructions = meetingInstructions;
        this.contribution = contribution;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public String getMeetingInstructions() { return meetingInstructions; }
    public void setMeetingInstructions(String meetingInstructions) { this.meetingInstructions = meetingInstructions; }

    public String getContribution() { return contribution; }
    public void setContribution(String contribution) { this.contribution = contribution; }
}