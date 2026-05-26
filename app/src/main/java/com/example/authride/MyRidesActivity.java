package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import com.example.authride.util.BookingNotifier;

public class MyRidesActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private View emptyState;
    private TabLayout tabs;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid;
    private final BookingNotifier bookingNotifier = new BookingNotifier();


    private final List<Ride> allRides = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myUid = auth.getCurrentUser().getUid();

        recycler   = findViewById(R.id.recyclerViewMyRides);
        progress   = findViewById(R.id.progress_my_rides);
        emptyState = findViewById(R.id.layout_empty_my_rides);
        tabs       = findViewById(R.id.tab_layout_rides);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab)   { render(); }
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
            if (upcomingTab ? ride.isUpcoming() : !ride.isUpcoming())
                filtered.add(ride);
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
            if (upcomingTab) {
                adapter.setOnViewPassengersListener(this::showRideBookings);
                adapter.setOnEditSeatsListener(this::showEditSeatsDialog);
            }
            recycler.setAdapter(adapter);
        }
    }

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
                        if (b.getPassengerEmail() != null && !b.getPassengerEmail().isEmpty())
                            sb.append("\n    ").append(b.getPassengerEmail());
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

    private void showEditSeatsDialog(Ride ride) {
        int booked = ride.getTotalSeats() - ride.getAvailableSeats();

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.edit_seats_hint);
        input.setText(String.valueOf(ride.getTotalSeats()));

        int pad = getResources().getDimensionPixelSize(R.dimen.space_m);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_seats_title)
                .setMessage(getString(R.string.edit_seats_booked, booked))
                .setView(container)
                .setPositiveButton("Αποθήκευση", (d, w) -> {
                    String txt = input.getText().toString().trim();
                    if (txt.isEmpty()) {
                        Toast.makeText(this, R.string.edit_seats_invalid,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int newTotal;
                    try {
                        newTotal = Integer.parseInt(txt);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.edit_seats_invalid,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newTotal < 1 || newTotal < booked) {
                        Toast.makeText(this, getString(R.string.edit_seats_min, booked),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    applySeatChange(ride, newTotal);
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void applySeatChange(Ride ride, int newTotal) {
        DocumentReference rideRef = db.collection("rides").document(ride.getId());
        db.runTransaction(transaction -> {
            Ride fresh = transaction.get(rideRef).toObject(Ride.class);
            if (fresh == null || fresh.isCancelled()) {
                throw new FirebaseFirestoreException("Η διαδρομή δεν είναι διαθέσιμη",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            int booked = fresh.getTotalSeats() - fresh.getAvailableSeats();
            if (newTotal < booked) {
                throw new FirebaseFirestoreException(
                        getString(R.string.edit_seats_min, booked),
                        FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(rideRef,
                    "totalSeats", newTotal,
                    "availableSeats", newTotal - booked);
            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, R.string.edit_seats_done, Toast.LENGTH_SHORT).show();
            loadRides();
        }).addOnFailureListener(e ->
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
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

    private void cancelRelatedBookings(Ride ride) {
        db.collection("bookings")
                .whereEqualTo("rideId", ride.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snapshot.getDocuments())
                        batch.update(d.getReference(), "status", Booking.STATUS_CANCELLED);
                    batch.commit().addOnCompleteListener(t -> {
                        Toast.makeText(this, "Η διαδρομή ακυρώθηκε",
                                Toast.LENGTH_SHORT).show();
                        loadRides();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Η διαδρομή ακυρώθηκε", Toast.LENGTH_SHORT).show();
                    loadRides();
                });
    }
    //EIDOPOIHSH
    @Override
    protected void onStart() {
        super.onStart();
        if (myUid != null) bookingNotifier.start(this, myUid);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bookingNotifier.stop();
    }
}