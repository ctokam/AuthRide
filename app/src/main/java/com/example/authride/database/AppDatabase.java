package com.example.authride.database;

import android.util.Log;
import com.example.authride.models.Reservation;
import com.example.authride.models.Route;
import com.example.authride.models.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

class FirestoreManager {

    private final FirebaseFirestore db;
    private static final String TAG = "FirestoreManager";

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // ==========================================
    // ΛΕΙΤΟΥΡΓΙΕΣ ΧΡΗΣΤΗ (USERS)
    // ==========================================

    // Αποθήκευση νέου χρήστη (καλείται μετά το Register στο FirebaseAuth)
    public void saveUser(User user, OnSuccessListener listener) {
        db.collection("users").document(user.getId())
                .set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    // ==========================================
    // ΛΕΙΤΟΥΡΓΙΕΣ ΔΙΑΔΡΟΜΩΝ (ROUTES)
    // ==========================================

    // Δημοσίευση νέας διαδρομής
    public void createRoute(Route route, OnSuccessListener listener) {
        // Δημιουργούμε ένα άδειο έγγραφο για να πάρουμε το τυχαίο ID
        DocumentReference ref = db.collection("routes").document();
        route.setId(ref.getId()); // Αποθηκεύουμε το ID μέσα στο αντικείμενο

        ref.set(route)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> Log.e(TAG, "Error creating route", e));
    }

    // Φόρτωση διαδρομών με διαθέσιμες θέσεις
    public void getAvailableRoutes(OnRoutesLoadedListener listener) {
        db.collection("routes")
                .whereGreaterThan("availableSeats", 0)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Route> routes = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Route route = document.toObject(Route.class);
                            routes.add(route);
                        }
                        listener.onLoaded(routes);
                    } else {
                        Log.e(TAG, "Error getting routes", task.getException());
                    }
                });
    }

    // ==========================================
    // ΛΕΙΤΟΥΡΓΙΕΣ ΚΡΑΤΗΣΕΩΝ (RESERVATIONS)
    // ==========================================

    // Κράτηση θέσης (Μειώνει τις θέσεις ΚΑΙ δημιουργεί κράτηση ταυτόχρονα)
    public void bookSeat(String passengerId, String routeId, OnSuccessListener listener) {
        DocumentReference routeRef = db.collection("routes").document(routeId);
        DocumentReference reservationRef = db.collection("reservations").document();

        Reservation reservation = new Reservation(passengerId, routeId);
        reservation.setId(reservationRef.getId());

        // Χρησιμοποιούμε Transaction για να είμαστε σίγουροι ότι 2 άτομα
        // δεν θα κλείσουν την τελευταία θέση ταυτόχρονα.
        db.runTransaction(transaction -> {
                    Route route = transaction.get(routeRef).toObject(Route.class);

                    if (route != null && route.getAvailableSeats() > 0) {
                        // 1. Μείωσε τις θέσεις
                        transaction.update(routeRef, "availableSeats", route.getAvailableSeats() - 1);
                        // 2. Αποθήκευσε την κράτηση
                        transaction.set(reservationRef, reservation);
                        return true;
                    } else {
                        throw new Exception("Δεν υπάρχουν διαθέσιμες θέσεις.");
                    }
                }).addOnSuccessListener(result -> listener.onSuccess())
                .addOnFailureListener(e -> Log.e(TAG, "Transaction failed", e));
    }

    // ==========================================
    // INTERFACES ΓΙΑ ΤΑ CALLBACKS (Ασύγχρονη απάντηση)
    // ==========================================
    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnRoutesLoadedListener {
        void onLoaded(List<Route> routes);
    }
}