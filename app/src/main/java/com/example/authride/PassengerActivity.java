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
import com.example.authride.util.BookingNotifier;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import com.example.authride.util.MeetReminderScheduler;
import com.example.authride.util.NotificationHelper;

public class PassengerActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progress;
    private View emptyState;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid, myName = "", myEmail = "";

    private final List<Ride> available = new ArrayList<>();
    private final Set<String> myBookedRideIds = new HashSet<>();
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
        setContentView(R.layout.activity_passenger);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }
        myUid = auth.getCurrentUser().getUid();
        if (auth.getCurrentUser().getEmail() != null)
            myEmail = auth.getCurrentUser().getEmail();

        recycler   = findViewById(R.id.recyclerViewRoutes);
        progress   = findViewById(R.id.progress_routes);
        emptyState = findViewById(R.id.layout_empty_state);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        Button btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> loadAvailableRides());

        loadMyName();

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        BottomNavHelper.setup(this, nav, R.id.nav_available);
        NotificationHelper.ensurePostPermission(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvailableRides();
    }

    private void loadMyName() {
        db.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.getString("fullName") != null)
                        myName = doc.getString("fullName");
                });
    }

    private void loadAvailableRides() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        // Βήμα 1: ποιες διαδρομές έχω ήδη κρατήσει (ενεργές κρατήσεις).
        db.collection("bookings")
                .whereEqualTo("passengerUid", myUid)
                .whereEqualTo("status", Booking.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snap -> {
                    myBookedRideIds.clear();
                    for (Booking b : snap.toObjects(Booking.class)) {
                        if (b.getRideId() != null) myBookedRideIds.add(b.getRideId());
                    }
                    fetchRides();
                })
                .addOnFailureListener(e -> fetchRides()); // σε αποτυχία, συνέχισε χωρίς το σετ
    }

    private void fetchRides() {
        db.collection("rides")
                .whereEqualTo("status", Ride.STATUS_ACTIVE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    available.clear();
                    long now = System.currentTimeMillis();
                    for (Ride ride : snapshot.toObjects(Ride.class)) {
                        if (ride.getDepartureMillis() < now) continue;
                        boolean mine = myUid.equals(ride.getDriverUid());
                        if (mine) {
                            // Δική σου διαδρομή: φαίνεται αλλά χωρίς κράτηση.
                            available.add(ride);
                        } else if (ride.hasAvailableSeats()
                                || myBookedRideIds.contains(ride.getId())) {
                            // Φαίνεται αν έχει θέσεις Ή αν την έχω ήδη κρατήσει
                            // (ώστε να βλέπω το "Κρατήθηκε ήδη θέση" ακόμη κι αν γέμισε).
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
            adapter.setCurrentUserUid(myUid);
            adapter.setBookedRideIds(myBookedRideIds);
            adapter.setOnRideClickListener(this::showDriverProfile);
            recycler.setAdapter(adapter);
        }
    }

    private void showDriverProfile(Ride ride) {
        String driverUid = ride.getDriverUid();
        if (driverUid == null) {
            Toast.makeText(this, "Δεν βρέθηκαν στοιχεία οδηγού", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(driverUid).get()
                .addOnSuccessListener(doc -> {
                    String name  = doc.getString("fullName");
                    String email = doc.getString("email");
                    String msg   = "Όνομα: " + (name  != null ? name  : "—")
                            + "\nEmail: " + (email != null ? email : "—");
                    new AlertDialog.Builder(this)
                            .setTitle("Στοιχεία οδηγού")
                            .setMessage(msg)
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
                .setMessage("Να γίνει κράτηση θέσης στη διαδρομή προς "
                        + ride.getDestination() + ";")
                .setPositiveButton("Ναι", (d, w) -> bookSeat(ride))
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void bookSeat(Ride ride) {
        // Ασφάλεια: δεν επιτρέπεται κράτηση στη δική σου διαδρομή.
        if (myUid.equals(ride.getDriverUid())) {
            Toast.makeText(this, R.string.cannot_book_own, Toast.LENGTH_LONG).show();
            return;
        }

        DocumentReference rideRef = db.collection("rides").document(ride.getId());
        // Ντετερμινιστικό id => το πολύ ΜΙΑ κράτηση ανά (διαδρομή, επιβάτη).
        DocumentReference bookingRef = db.collection("bookings")
                .document(ride.getId() + "_" + myUid);

        db.runTransaction(transaction -> {
            // ΟΛΑ τα reads ΠΡΙΝ από τα writes (κανόνας των Firestore transactions).
            Ride fresh = transaction.get(rideRef).toObject(Ride.class);
            DocumentSnapshot existing = transaction.get(bookingRef);

            if (fresh == null
                    || !Ride.STATUS_ACTIVE.equals(fresh.getStatus())
                    || fresh.getAvailableSeats() <= 0) {
                throw new FirebaseFirestoreException("Δεν υπάρχουν διαθέσιμες θέσεις",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            if (myUid.equals(fresh.getDriverUid())) {
                throw new FirebaseFirestoreException(
                        "Δεν μπορείς να κάνεις κράτηση στη δική σου διαδρομή",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            // Αποτροπή διπλής κράτησης: μπλόκαρε μόνο αν υπάρχει ΕΝΕΡΓΗ κράτηση.
            if (existing.exists()
                    && !Booking.STATUS_CANCELLED.equals(existing.getString("status"))) {
                throw new FirebaseFirestoreException(
                        "Έχεις ήδη κάνει κράτηση σε αυτή τη διαδρομή",
                        FirebaseFirestoreException.Code.ABORTED);
            }


            // set (όχι add) σε ντετερμινιστικό id: αν υπήρχε παλιά ΑΚΥΡΩΜΕΝΗ κράτηση,
            // γράφεται από πάνω -> καμία διπλοεγγραφή/"φάντασμα" στο ιστορικό.
            transaction.update(rideRef, "availableSeats", fresh.getAvailableSeats() - 1);
            transaction.set(bookingRef, new Booking(myUid, myName, myEmail, fresh));
            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Η κράτηση ολοκληρώθηκε!", Toast.LENGTH_SHORT).show();

            // Προγραμμάτισε την υπενθύμιση με το ίδιο ντετερμινιστικό id.
            Booking justBooked = new Booking(myUid, myName, myEmail, ride);
            justBooked.setId(ride.getId() + "_" + myUid);
            MeetReminderScheduler.schedule(this, justBooked);

            loadAvailableRides();
        }).addOnFailureListener(e ->
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}