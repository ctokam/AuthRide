package com.example.authride.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.authride.MyBookingsActivity;
import com.example.authride.MyRidesActivity;
import com.example.authride.R;
import com.example.authride.model.Booking;
import com.example.authride.model.Ride;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Real-time ειδοποιήσεις (χωρίς server). Δύο listeners ανά χρήστη:
 *  - Οδηγός: νέα κράτηση (ADDED, active) + ακύρωση από επιβάτη (active -> cancelled,
 *    ΕΝΩ η διαδρομή παραμένει ενεργή).
 *  - Επιβάτης: η κράτησή του ακυρώθηκε ΕΠΕΙΔΗ ακυρώθηκε η διαδρομή (από οδηγό ή
 *    λόγω διαγραφής λογαριασμού οδηγού). Αν η διαδρομή είναι ακόμη ενεργή, σημαίνει
 *    ότι ακύρωσε ο ίδιος ο επιβάτης -> δεν ειδοποιούμε.
 *
 * Παρακολουθούμε ΜΕΤΑΒΑΣΕΙΣ κατάστασης (όχι απλώς ADDED) κρατώντας την τελευταία
 * γνωστή κατάσταση κάθε κράτησης. Στο πρώτο snapshot απλώς "σπέρνουμε" (δεν
 * ειδοποιούμε για παλιά). Σετ anti-duplicate αποτρέπουν διπλές ειδοποιήσεις.
 */
public class BookingNotifier {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration driverReg;
    private ListenerRegistration passengerReg;

    private final Map<String, String> driverStatus = new HashMap<>();
    private final Map<String, String> passengerStatus = new HashMap<>();
    private boolean driverSeeded = false;
    private boolean passengerSeeded = false;

    private final Set<String> notifiedDriverCancel = new HashSet<>();
    private final Set<String> notifiedRideCancel = new HashSet<>();

    public void start(Context context, String uid) {
        if (uid == null) return;
        Context app = context.getApplicationContext();
        NotificationHelper.createChannels(app);
        startDriverListener(app, uid);
        startPassengerListener(app, uid);
    }

    public void stop() {
        if (driverReg != null) { driverReg.remove(); driverReg = null; }
        if (passengerReg != null) { passengerReg.remove(); passengerReg = null; }
    }

    // ---------- Πλευρά οδηγού ----------
    private void startDriverListener(Context app, String uid) {
        if (driverReg != null) return;
        driverReg = db.collection("bookings")
                .whereEqualTo("driverUid", uid)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        String id = dc.getDocument().getId();
                        Booking b = dc.getDocument().toObject(Booking.class);
                        String now = b.getStatus();
                        String prev = driverStatus.get(id);

                        if (driverSeeded) {
                            boolean isNew = dc.getType() == DocumentChange.Type.ADDED
                                    && prev == null
                                    && Booking.STATUS_ACTIVE.equals(now);
                            boolean becameCancelled =
                                    dc.getType() != DocumentChange.Type.REMOVED
                                            && Booking.STATUS_CANCELLED.equals(now)
                                            && !Booking.STATUS_CANCELLED.equals(prev);

                            if (isNew) {
                                notifyNewBooking(app, b, id);
                            } else if (becameCancelled) {
                                maybeNotifyDriverOfCancellation(app, b, id);
                            }
                        }
                        driverStatus.put(id, now);
                    }
                    driverSeeded = true;
                });
    }

    // ---------- Πλευρά επιβάτη ----------
    private void startPassengerListener(Context app, String uid) {
        if (passengerReg != null) return;
        passengerReg = db.collection("bookings")
                .whereEqualTo("passengerUid", uid)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        String id = dc.getDocument().getId();
                        Booking b = dc.getDocument().toObject(Booking.class);
                        String now = b.getStatus();
                        String prev = passengerStatus.get(id);

                        if (passengerSeeded) {
                            boolean becameCancelled =
                                    dc.getType() != DocumentChange.Type.REMOVED
                                            && Booking.STATUS_CANCELLED.equals(now)
                                            && !Booking.STATUS_CANCELLED.equals(prev);
                            if (becameCancelled) {
                                maybeNotifyPassengerOfRideCancellation(app, b, id);
                            }
                        }
                        passengerStatus.put(id, now);
                    }
                    passengerSeeded = true;
                });
    }

    /** Ειδοποίησε τον οδηγό ΜΟΝΟ αν την κράτηση την ακύρωσε ο επιβάτης (διαδρομή ενεργή). */
    private void maybeNotifyDriverOfCancellation(Context app, Booking b, String id) {
        if (b.getRideId() == null || notifiedDriverCancel.contains(id)) return;
        db.collection("rides").document(b.getRideId()).get()
                .addOnSuccessListener(doc -> {
                    Ride ride = doc.exists() ? doc.toObject(Ride.class) : null;
                    if (ride != null && !ride.isCancelled()) { // ο επιβάτης ακύρωσε
                        notifiedDriverCancel.add(id);
                        String passenger = b.getPassengerName() != null
                                ? b.getPassengerName() : "Ένας επιβάτης";
                        String title = app.getString(R.string.notif_booking_cancelled_title);
                        String text = app.getString(R.string.notif_booking_cancelled_text,
                                passenger, b.getDestination() != null ? b.getDestination() : "");
                        show(app, NotificationHelper.CHANNEL_BOOKINGS, id.hashCode() ^ 0x11,
                                title, text, MyRidesActivity.class);
                    }
                });
    }

    /** Ειδοποίησε τον επιβάτη ΜΟΝΟ αν ακυρώθηκε η ίδια η διαδρομή (από τον οδηγό). */
    private void maybeNotifyPassengerOfRideCancellation(Context app, Booking b, String id) {
        if (b.getRideId() == null || notifiedRideCancel.contains(id)) return;
        db.collection("rides").document(b.getRideId()).get()
                .addOnSuccessListener(doc -> {
                    Ride ride = doc.exists() ? doc.toObject(Ride.class) : null;
                    boolean driverCancelled = (ride == null) || ride.isCancelled();
                    if (driverCancelled) {
                        notifiedRideCancel.add(id);
                        String title = app.getString(R.string.notif_ride_cancelled_title);
                        String text = app.getString(R.string.notif_ride_cancelled_text,
                                b.getDestination() != null ? b.getDestination() : "",
                                TimeUtils.dateTime(b.getDepartureMillis()));
                        show(app, NotificationHelper.CHANNEL_REMINDERS, id.hashCode() ^ 0x22,
                                title, text, MyBookingsActivity.class);
                    }
                });
    }

    private void notifyNewBooking(Context app, Booking b, String id) {
        String passenger = b.getPassengerName() != null ? b.getPassengerName() : "Κάποιος";
        String title = app.getString(R.string.notif_new_booking_title);
        String text = app.getString(R.string.notif_new_booking_text,
                passenger, b.getDestination() != null ? b.getDestination() : "");
        show(app, NotificationHelper.CHANNEL_BOOKINGS, id.hashCode(), title, text,
                MyRidesActivity.class);
    }

    private void show(Context app, String channel, int notifId,
                      String title, String text, Class<?> target) {
        Intent open = new Intent(app, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(app, notifId, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationHelper.show(app, channel, notifId, title, text, pi);
    }
}