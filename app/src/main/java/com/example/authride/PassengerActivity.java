package com.example.authride;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authride.adapters.RouteAdapter;
import com.example.authride.model.Booking;
import com.example.authride.model.Ride;
import com.example.authride.util.BottomNavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

/**
 * Οθόνη επιβάτη: εμφανίζει τις διαθέσιμες (ενεργές, μελλοντικές, με ελεύθερες
 * θέσεις) διαδρομές άλλων οδηγών και επιτρέπει κράτηση θέσης.
 *
 * Πατώντας μια κάρτα, ο επιβάτης βλέπει το προφίλ του οδηγού.
 */
public class PassengerActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private View emptyState;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid, myName = "", myEmail = "";

    private final List<Ride> available = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myUid = auth.getCurrentUser().getUid();
        if (auth.getCurrentUser().getEmail() != null) {
            myEmail = auth.getCurrentUser().getEmail();
        }

        recycler = findViewById(R.id.recyclerViewRoutes);
        progress = findViewById(R.id.progress_routes);
        emptyState = findViewById(R.id.layout_empty_state);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> loadAvailableRides());

        loadMyName();

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        BottomNavHelper.setup(this, nav, R.id.nav_available);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailableRides();
    }

    private void loadMyName() {
        db.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.getString("fullName") != null) myName = doc.getString("fullName");
                });
    }

    private void loadAvailableRides() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        db.collection("rides")
                .whereEqualTo("status", Ride.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    available.clear();
                    long now = System.currentTimeMillis();
                    for (Ride ride : snapshot.toObjects(Ride.class)) {
                        boolean future = ride.getDepartureMillis() >= now;
                        boolean notMine = !myUid.equals(ride.getDriverUid());
                        if (future && notMine && ride.hasAvailableSeats()) {
                            available.add(ride);
                        }
                    }
                    progress.setVisibility(View.GONE);
                    render();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Αποτυχία φόρτωσης: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void render() {
        if (available.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            RouteAdapter adapter = new RouteAdapter(available, RouteAdapter.Mode.BOOK,
                    this::confirmBooking);
            // Πάτημα κάρτας -> προβολή προφίλ οδηγού.
            adapter.setOnRideClickListener(this::showDriverProfile);
            recycler.setAdapter(adapter);
        }
    }

    /** Φέρνει και εμφανίζει σε διάλογο τα στοιχεία του οδηγού της διαδρομής. */
    private void showDriverProfile(Ride ride) {
        String driverUid = ride.getDriverUid();
        if (driverUid == null) {
            Toast.makeText(this, "Δεν βρέθηκαν στοιχεία οδηγού", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(driverUid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullName");
                    String email = doc.getString("email");
                    String message = "Όνομα: " + (name != null ? name : "—")
                            + "\nEmail: " + (email != null ? email : "—");
                    new AlertDialog.Builder(this)
                            .setTitle("Στοιχεία οδηγού")
                            .setMessage(message)
                            .setPositiveButton("Κλείσιμο", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void confirmBooking(Ride ride) {
        new AlertDialog.Builder(this)
                .setTitle("Επιβεβαίωση κράτησης")
                .setMessage("Να γίνει κράτηση θέσης στη διαδρομή προς " + ride.getDestination() + ";")
                .setPositiveButton("Ναι", (d, w) -> bookSeat(ride))
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void bookSeat(Ride ride) {
        DocumentReference rideRef = db.collection("rides").document(ride.getId());
        DocumentReference bookingRef = db.collection("bookings").document();

        db.runTransaction(transaction -> {
            Ride fresh = transaction.get(rideRef).toObject(Ride.class);
            if (fresh == null
                    || !Ride.STATUS_ACTIVE.equals(fresh.getStatus())
                    || fresh.getAvailableSeats() <= 0) {
                throw new FirebaseFirestoreException("Δεν υπάρχουν διαθέσιμες θέσεις",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(rideRef, "availableSeats", fresh.getAvailableSeats() - 1);
            transaction.set(bookingRef, new Booking(myUid, myName, myEmail, fresh));
            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Η κράτηση ολοκληρώθηκε!", Toast.LENGTH_SHORT).show();
            loadAvailableRides();
        }).addOnFailureListener(e ->
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
