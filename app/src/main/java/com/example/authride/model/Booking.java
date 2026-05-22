package com.example.authride.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Κράτηση θέσης ενός επιβάτη σε μια διαδρομή. Αποθηκεύεται στο bookings/{id}.
 *
 * Αντιγράφουμε σκόπιμα (denormalization) τα βασικά στοιχεία της διαδρομής μέσα
 * στην κράτηση. Έτσι η οθόνη "Οι Κρατήσεις μου" γεμίζει με μία μόνο ανάγνωση,
 * χωρίς να χρειάζεται ξεχωριστό fetch της διαδρομής για κάθε κράτηση.
 *
 * Το πεδίο status επιτρέπει να ξεχωρίζουμε τις ενεργές από τις ακυρωμένες
 * κρατήσεις — όταν ο οδηγός ακυρώνει μια διαδρομή, οι σχετικές κρατήσεις
 * σημαδεύονται ως cancelled και εμφανίζονται στο Ιστορικό του επιβάτη.
 */
public class Booking {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELLED = "cancelled";

    @DocumentId
    private String id;
    private String rideId;
    private String passengerUid;
    private String passengerName;
    private String passengerEmail;

    // --- Denormalized στοιχεία διαδρομής (για γρήγορη εμφάνιση στη λίστα) ---
    private String driverUid;
    private String driverName;
    private String startLocation;
    private String destination;
    private long departureMillis;

    private long createdAt;
    private String status;

    /** Κενός constructor — απαιτείται από το Firestore. */
    public Booking() {
    }

    /** Φτιάχνει κράτηση αντιγράφοντας τα στοιχεία εμφάνισης από τη διαδρομή. */
    public Booking(String passengerUid, String passengerName, String passengerEmail, Ride ride) {
        this.rideId = ride.getId();
        this.passengerUid = passengerUid;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.driverUid = ride.getDriverUid();
        this.driverName = ride.getDriverName();
        this.startLocation = ride.getStartLocation();
        this.destination = ride.getDestination();
        this.departureMillis = ride.getDepartureMillis();
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_ACTIVE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getPassengerUid() {
        return passengerUid;
    }

    public void setPassengerUid(String passengerUid) {
        this.passengerUid = passengerUid;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    public String getDriverUid() {
        return driverUid;
    }

    public void setDriverUid(String driverUid) {
        this.driverUid = driverUid;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public long getDepartureMillis() {
        return departureMillis;
    }

    public void setDepartureMillis(long departureMillis) {
        this.departureMillis = departureMillis;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Exclude
    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    /**
     * Η κράτηση αφορά μελλοντική διαδρομή και ΔΕΝ έχει ακυρωθεί — tab "Επόμενες".
     * Οι ακυρωμένες (ακόμη κι αν είναι στο μέλλον) πέφτουν στο Ιστορικό.
     */
    @Exclude
    public boolean isUpcoming() {
        return !STATUS_CANCELLED.equals(status)
                && departureMillis >= System.currentTimeMillis();
    }
}
