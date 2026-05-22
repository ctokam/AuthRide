package com.example.authride.models;

public class User {
    private String id; // Το UID από το Firebase Auth
    private String name;
    private String email;
    private String phoneNumber; // Απαραίτητο για επικοινωνία στο σημείο συνάντησης

    // ΑΠΑΡΑΙΤΗΤΟ για το Firebase
    public User() {}

    public User(String id, String name, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}