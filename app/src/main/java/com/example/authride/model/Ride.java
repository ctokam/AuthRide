package com.example.authride.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Μία διαδρομή που δημοσιεύει ένας οδηγός. Αποθηκεύεται στο Firestore στο rides/{id}.
 * Η ημερομηνία και η ώρα αναχώρησης κρατιούνται ενωμένες σε ένα timestamp
 * ({@code departureMillis}) — έτσι γίνονται εύκολα τόσο ο διαχωρισμός
 * "Επόμενες"/"Ιστορικό" όσο και ο έλεγχος του κανόνα ακύρωσης.
 */
public class Ride {

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_CANCELLED = "cancelled";

    /** Όριο κανόνα: ο οδηγός δεν μπορεί να ακυρώσει εντός 1 ώρας από την αναχώρηση. */
    public static final long CANCEL_WINDOW_MILLIS = 60L * 60L * 1000L;

    /** Συμπληρώνεται αυτόματα από το id του εγγράφου του Firestore. */
    @DocumentId
    private String id;
    private String driverUid;
    private String driverName;
    private String startLocation;
    private String destination;
    private long departureMillis;
    private int totalSeats;
    private int availableSeats;
    private String status;

    /** Κενός constructor — απαιτείται από το Firestore. */
    public Ride() {
    }

    public Ride(String driverUid, String driverName, String startLocation,
                String destination, long departureMillis, int totalSeats) {
        this.driverUid = driverUid;
        this.driverName = driverName;
        this.startLocation = startLocation;
        this.destination = destination;
        this.departureMillis = departureMillis;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;   // ξεκινά με όλες τις θέσεις ελεύθερες
        this.status = STATUS_ACTIVE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
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

    /** Ενεργή και με ώρα αναχώρησης στο μέλλον — ανήκει στο tab "Επόμενες". */
    @Exclude
    public boolean isUpcoming() {
        return STATUS_ACTIVE.equals(status)
                && departureMillis >= System.currentTimeMillis();
    }

    @Exclude
    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    /** Κανόνας ακύρωσης: επιτρέπεται μόνο αν απομένει τουλάχιστον 1 ώρα ως την αναχώρηση. */
    @Exclude
    public boolean canBeCancelled() {
        return STATUS_ACTIVE.equals(status)
                && (departureMillis - System.currentTimeMillis()) >= CANCEL_WINDOW_MILLIS;
    }
}