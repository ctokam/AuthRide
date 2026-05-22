package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * "Οι Διαδρομές μου" (οδηγός). Δύο tabs: Επόμενες (ενεργές & μελλοντικές) και
 * Ιστορικό (περασμένες ή ακυρωμένες). Ακύρωση επιτρέπεται μόνο > 1 ώρα πριν.
 *
 * Πατώντας μια κάρτα διαδρομής, ο οδηγός βλέπει ποιοι επιβάτες έχουν κάνει
 * κράτηση σε αυτήν.
 */
public class MyRidesActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private View emptyState;
    private TabLayout tabs;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid;

    private final List<Ride> allRides = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myUid = auth.getCurrentUser().getUid();

        recycler = findViewById(R.id.recyclerViewMyRides);
        progress = findViewById(R.id.progress_my_rides);
        emptyState = findViewById(R.id.layout_empty_my_rides);
        tabs = findViewById(R.id.tab_layout_rides);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { render(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        BottomNavHelper.setup(this, nav, R.id.nav_my_rides);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRides();
    }

    private void loadRides() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        db.collection("rides")
                .whereEqualTo("driverUid", myUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allRides.clear();
                    allRides.addAll(snapshot.toObjects(Ride.class));
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
        boolean upcomingTab = tabs.getSelectedTabPosition() == 0;
        List<Ride> filtered = new ArrayList<>();
        for (Ride ride : allRides) {
            if (upcomingTab ? ride.isUpcoming() : !ride.isUpcoming()) {
                filtered.add(ride);
            }
        }

        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            RouteAdapter.Mode mode = upcomingTab
                    ? RouteAdapter.Mode.CANCEL_RIDE
                    : RouteAdapter.Mode.NONE;
            RouteAdapter adapter = new RouteAdapter(filtered, mode, this::onCancelRequested);
            // Πάτημα κάρτας -> δες ποιοι έκαναν κράτηση.
            adapter.setOnRideClickListener(this::showRideBookings);
            recycler.setAdapter(adapter);
        }
    }

    /** Εμφανίζει σε διάλογο τους επιβάτες που έχουν κάνει κράτηση στη διαδρομή. */
    private void showRideBookings(Ride ride) {
        db.collection("bookings")
                .whereEqualTo("rideId", ride.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Booking> list = snapshot.toObjects(Booking.class);
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (Booking b : list) {
                        if (b.isCancelled()) continue;
                        count++;
                        sb.append(count).append(". ").append(b.getPassengerName());
                        if (b.getPassengerEmail() != null && !b.getPassengerEmail().isEmpty()) {
                            sb.append("\n    ").append(b.getPassengerEmail());
                        }
                        sb.append("\n\n");
                    }
                    String message = count == 0
                            ? "Κανείς δεν έχει κάνει κράτηση ακόμη."
                            : sb.toString().trim();

                    new AlertDialog.Builder(this)
                            .setTitle("Επιβάτες προς " + ride.getDestination())
                            .setMessage(message)
                            .setPositiveButton("Κλείσιμο", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void onCancelRequested(Ride ride) {
        if (!ride.canBeCancelled()) {
            Toast.makeText(this,
                    "Δεν μπορείς να ακυρώσεις λιγότερο από 1 ώρα πριν την αναχώρηση",
                    Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Ακύρωση διαδρομής")
                .setMessage("Σίγουρα θέλεις να ακυρώσεις τη διαδρομή προς "
                        + ride.getDestination() + ";")
                .setPositiveButton("Ναι", (d, w) -> cancelRide(ride))
                .setNegativeButton("Όχι", null)
                .show();
    }

    private void cancelRide(Ride ride) {
        db.collection("rides").document(ride.getId())
                .update("status", Ride.STATUS_CANCELLED)
                .addOnSuccessListener(unused -> cancelRelatedBookings(ride))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Όταν ακυρώνεται μια διαδρομή, σημαδεύουμε και όλες τις κρατήσεις της ως
     * cancelled, ώστε οι επιβάτες να το δουν στο Ιστορικό τους.
     */
    private void cancelRelatedBookings(Ride ride) {
        db.collection("bookings")
                .whereEqualTo("rideId", ride.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        batch.update(d.getReference(), "status", Booking.STATUS_CANCELLED);
                    }
                    batch.commit().addOnCompleteListener(t -> {
                        Toast.makeText(this, "Η διαδρομή ακυρώθηκε", Toast.LENGTH_SHORT).show();
                        loadRides();
                    });
                })
                .addOnFailureListener(e -> {
                    // Η διαδρομή ακυρώθηκε ούτως ή άλλως· απλώς ανανεώνουμε.
                    Toast.makeText(this, "Η διαδρομή ακυρώθηκε", Toast.LENGTH_SHORT).show();
                    loadRides();
                });
    }
}
