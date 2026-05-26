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

import com.example.authride.adapters.BookingAdapter;
import com.example.authride.model.Booking;
import com.example.authride.model.Ride;
import com.example.authride.util.BottomNavHelper;
import com.example.authride.util.MeetReminderScheduler;
import com.example.authride.util.BookingNotifier;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import com.example.authride.util.MeetReminderScheduler;

/**
 * "Οι Κρατήσεις μου" (επιβάτης). Tabs Επόμενες/Ιστορικό. Η ακύρωση κράτησης
 * διαγράφει την κράτηση και επιστρέφει τη θέση στη διαδρομή (transaction).
 *
 * Πατώντας μια κάρτα, ο επιβάτης βλέπει το προφίλ του οδηγού.
 */
public class MyBookingsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private View emptyState;
    private TabLayout tabs;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid;

    private final List<Booking> allBookings = new ArrayList<>();

    private final BookingNotifier bookingNotifier = new BookingNotifier();

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myUid = auth.getCurrentUser().getUid();

        recycler = findViewById(R.id.recyclerViewMyBookings);
        progress = findViewById(R.id.progress_my_bookings);
        emptyState = findViewById(R.id.layout_empty_my_bookings);
        tabs = findViewById(R.id.tab_layout_bookings);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { render(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        BottomNavHelper.setup(this, nav, R.id.nav_my_bookings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }

    private void loadBookings() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        db.collection("bookings")
                .whereEqualTo("passengerUid", myUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allBookings.clear();
                    allBookings.addAll(snapshot.toObjects(Booking.class));
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
        List<Booking> filtered = new ArrayList<>();
        for (Booking booking : allBookings) {
            if (upcomingTab ? booking.isUpcoming() : !booking.isUpcoming()) {
                filtered.add(booking);
            }
        }

        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            BookingAdapter.Mode mode = upcomingTab
                    ? BookingAdapter.Mode.CANCEL_BOOKING
                    : BookingAdapter.Mode.NONE;
            BookingAdapter adapter = new BookingAdapter(filtered, mode, this::confirmCancel);
            // Πάτημα κάρτας -> προβολή προφίλ οδηγού.
            adapter.setOnBookingClickListener(this::showDriverProfile);
            recycler.setAdapter(adapter);
        }
    }

    /** Φέρνει και εμφανίζει σε διάλογο τα στοιχεία του οδηγού της κράτησης. */
    private void showDriverProfile(Booking booking) {
        String driverUid = booking.getDriverUid();
        if (driverUid == null) {
            // Παλιότερες κρατήσεις ίσως δεν έχουν driverUid· δείχνουμε ό,τι ξέρουμε.
            new AlertDialog.Builder(this)
                    .setTitle("Στοιχεία οδηγού")
                    .setMessage("Όνομα: " + (booking.getDriverName() != null
                            ? booking.getDriverName() : "—"))
                    .setPositiveButton("Κλείσιμο", null)
                    .show();
            return;
        }
        db.collection("users").document(driverUid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullName");
                    String email = doc.getString("email");
                    // Fallback: αν ο οδηγός διαγράφηκε, χρησιμοποίησε το αποθηκευμένο όνομα της κράτησης.
                    if (name == null || name.isEmpty()) name = booking.getDriverName();
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

    private void confirmCancel(Booking booking) {
        new AlertDialog.Builder(this)
                .setTitle("Ακύρωση κράτησης")
                .setMessage("Σίγουρα θέλεις να ακυρώσεις την κράτηση προς "
                        + booking.getDestination() + ";")
                .setPositiveButton("Ναι", (d, w) -> cancelBooking(booking))
                .setNegativeButton("Όχι", null)
                .show();
    }

    private void cancelBooking(Booking booking) {
        DocumentReference bookingRef = db.collection("bookings").document(booking.getId());
        DocumentReference rideRef = db.collection("rides").document(booking.getRideId());

        db.runTransaction(transaction -> {
            // ΟΛΑ τα reads πριν τα writes (κανόνας των Firestore transactions).
            DocumentSnapshot bookingSnap = transaction.get(bookingRef);

            // Αν λείπει ή είναι ήδη ακυρωμένη, μην κάνεις τίποτα — αποτρέπει
            // διπλή επιστροφή θέσης σε σπάνια race conditions.
            if (!bookingSnap.exists()
                    || Booking.STATUS_CANCELLED.equals(bookingSnap.getString("status"))) {
                return null;
            }

            Ride ride = transaction.get(rideRef).toObject(Ride.class);

            // Σημάδεψε ως ακυρωμένη (ΔΕΝ τη διαγράφουμε -> μένει στο Ιστορικό ως "Ακυρώθηκε").
            transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED);

            // Επίστρεψε τη θέση, αν η διαδρομή υπάρχει ακόμη και είναι ενεργή.
            if (ride != null && !ride.isCancelled()) {
                transaction.update(rideRef, "availableSeats", ride.getAvailableSeats() + 1);
            }
            return null;
        }).addOnSuccessListener(unused -> {
            MeetReminderScheduler.cancel(this, booking.getId());
            Toast.makeText(this, "Η κράτηση ακυρώθηκε", Toast.LENGTH_SHORT).show();
            loadBookings();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
