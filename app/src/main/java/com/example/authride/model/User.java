package com.example.authride.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Χρήστης της εφαρμογής. Αποθηκεύεται στο Firestore στο users/{uid}.
 * Ο ίδιος λογαριασμός λειτουργεί και ως οδηγός και ως επιβάτης — ο τρέχων
 * ρόλος κρατιέται στο πεδίο {@code role} και αλλάζει από το κουμπί του προφίλ.
 */
public class User {

    public static final String ROLE_DRIVER = "driver";
    public static final String ROLE_PASSENGER = "passenger";

    /** Συμπληρώνεται αυτόματα από το id του εγγράφου (= uid του Firebase Auth). */
    @DocumentId
    private String uid;
    private String fullName;
    private String email;
    private String role;

    /** Κενός constructor — απαιτείται από το Firestore για το deserialization. */
    public User() {
    }

    public User(String fullName, String email, String role) {
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** Βοηθητικό: true αν ο τρέχων ρόλος είναι "οδηγός". */
    @Exclude
    public boolean isDriver() {
        return ROLE_DRIVER.equals(role);
    }
}