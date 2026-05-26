package com.example.authride;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.authride.model.Ride;
import com.example.authride.util.BottomNavHelper;
import com.example.authride.util.TimeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import com.example.authride.util.BookingNotifier;
import com.example.authride.util.NotificationHelper;

/**
 * Οθόνη δημοσίευσης διαδρομής (οδηγός). Συλλέγει αφετηρία, προορισμό,
 * ημερομηνία+ώρα (μέσω pickers) και θέσεις, και γράφει ένα Ride στο Firestore.
 */
public class PublishRideActivity extends AppCompatActivity {

    private EditText etStart, etDestination, etDate, etTime, etSeats;
    private Button btnPublish;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String driverName = "";

    // Κρατάει την επιλεγμένη ημερομηνία/ώρα αναχώρησης.
    private final Calendar departure = Calendar.getInstance();
    private boolean dateSet = false, timeSet = false;
    private final BookingNotifier bookingNotifier = new BookingNotifier();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_ride);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etStart = findViewById(R.id.et_starting_point);
        etDestination = findViewById(R.id.et_destination);
        etDate = findViewById(R.id.et_departure_date);
        etTime = findViewById(R.id.et_departure_time);
        etSeats = findViewById(R.id.et_available_seats);
        btnPublish = findViewById(R.id.btn_publish_ride);

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnPublish.setOnClickListener(v -> publishRide());

        loadDriverName();

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        BottomNavHelper.setup(this, nav, R.id.nav_publish);
        NotificationHelper.ensurePostPermission(this);

    }

    private void loadDriverName() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("fullName") != null) {
                        driverName = doc.getString("fullName");
                    }
                });
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            departure.set(Calendar.YEAR, year);
            departure.set(Calendar.MONTH, month);
            departure.set(Calendar.DAY_OF_MONTH, day);
            dateSet = true;
            etDate.setText(TimeUtils.date(departure.getTimeInMillis()));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        // Δεν επιτρέπουμε ημερομηνίες στο παρελθόν.
        dialog.getDatePicker().setMinDate(now.getTimeInMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            departure.set(Calendar.HOUR_OF_DAY, hour);
            departure.set(Calendar.MINUTE, minute);
            departure.set(Calendar.SECOND, 0);
            timeSet = true;
            etTime.setText(TimeUtils.time(departure.getTimeInMillis()));
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void publishRide() {
        String start = etStart.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String seatsText = etSeats.getText().toString().trim();

        if (TextUtils.isEmpty(start)) {
            etStart.setError("Συμπλήρωσε την αφετηρία");
            return;
        }
        if (TextUtils.isEmpty(destination)) {
            etDestination.setError("Συμπλήρωσε τον προορισμό");
            return;
        }
        if (start.equalsIgnoreCase(destination)) {
            etDestination.setError("Ο προορισμός πρέπει να διαφέρει από την αφετηρία");
            etDestination.requestFocus();
            return;
        }
        if (!dateSet || !timeSet) {
            Toast.makeText(this, "Επίλεξε ημερομηνία και ώρα αναχώρησης", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(seatsText)) {
            etSeats.setError("Συμπλήρωσε τις διαθέσιμες θέσεις");
            return;
        }
        int seats = Integer.parseInt(seatsText);
        if (seats <= 0) {
            etSeats.setError("Οι θέσεις πρέπει να είναι τουλάχιστον 1");
            return;
        }
        if (departure.getTimeInMillis() <= System.currentTimeMillis()) {
            Toast.makeText(this, "Η ώρα αναχώρησης πρέπει να είναι στο μέλλον", Toast.LENGTH_SHORT).show();
            return;
        }
        if (auth.getCurrentUser() == null) {
            return;
        }

        btnPublish.setEnabled(false);
        Ride ride = new Ride(
                auth.getCurrentUser().getUid(),
                driverName,
                start,
                destination,
                departure.getTimeInMillis(),
                seats);

        db.collection("rides").add(ride)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Η διαδρομή δημοσιεύτηκε!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    btnPublish.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    Toast.makeText(this, "Αποτυχία: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearForm() {
        etStart.setText("");
        etDestination.setText("");
        etDate.setText("");
        etTime.setText("");
        etSeats.setText("");
        dateSet = false;
        timeSet = false;
    }

    //EIDOPOIHSH
    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null)
            bookingNotifier.start(this, auth.getCurrentUser().getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        bookingNotifier.stop();
    }
}