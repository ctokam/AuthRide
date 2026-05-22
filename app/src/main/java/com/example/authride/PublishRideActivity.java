package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.authride.database.AppDatabase;
import com.example.authride.database.Route;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublishRideActivity extends AppCompatActivity {

    //Στοιχεία XML
    private EditText etStartLocation, etEndLocation, etIntermediateStops, etDepartureTime, etAvailableSeats;
    private Button btnPublish;

    // Εργαλεία Βάσης και ID Χρήστη
    private AppDatabase db;
    private ExecutorService executorService;
    private int currentDriverId = -1; //ID οδηγου

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_ride);

        //Σύνδεση με ID XML
        etStartLocation = findViewById(R.id.et_starting_point);
        etEndLocation = findViewById(R.id.et_destination);
        etDepartureTime = findViewById(R.id.et_departure_time);
        etAvailableSeats = findViewById(R.id.et_available_seats);
        btnPublish = findViewById(R.id.btn_publish_ride);

        // Αρχικοποίηση Βάσης
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        //Λήψη ID οδηγού από προηγούμενη οθόνη
        currentDriverId = getIntent().getIntExtra("USER_ID", -1);

        if (currentDriverId == -1) {
            Toast.makeText(this, "Σφάλμα ταυτοποίησης χρήστη.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Λειτουργία κουμπιού "Δημοσίευση"
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishRide();
            }
        });
    }

    //ΛΟΓΙΚΗ ΔΗΜΟΣΙΕΥΣΗΣ
    private void publishRide() {
        String start = etStartLocation.getText().toString().trim();
        String end = etEndLocation.getText().toString().trim();
        String stops = etIntermediateStops.getText().toString().trim();
        String time = etDepartureTime.getText().toString().trim();
        String seatsStr = etAvailableSeats.getText().toString().trim();

        // ΦΙΛΤΡΟ 1: Έλεγχος κενων πεδίων (Οι ενδιάμεσες στάσεις μπορεί να είναι κενές)
        if (start.isEmpty() || end.isEmpty() || time.isEmpty() || seatsStr.isEmpty()) {
            Toast.makeText(this, "Συμπληρώστε όλα τα υποχρεωτικά πεδία!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Μετατροπή των θέσεων από κειμενο (String) σε αριθμό (int)
        int seats;
        try {
            seats = Integer.parseInt(seatsStr);
        } catch (NumberFormatException e) {
            etAvailableSeats.setError("Εισάγετε έναν έγκυρο αριθμό!");
            etAvailableSeats.requestFocus();
            return;
        }

        // ΦΙΛΤΡΟ 2: έλεγχος θέσεων (Misuse Case)
        if (seats <= 0 || seats > 6) {
            etAvailableSeats.setError("Οι θέσεις πρέπει να είναι από 1 έως 6");
            etAvailableSeats.requestFocus();
            return;
        }

        //ΕΠΙΤΥΧΙΑ πάμε στο Background Thread για να γράψουμε στη βάση
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //νέα διαδρομή
                Route newRoute = new Route(currentDriverId, start, stops, end, time, seats);

                // αποθηκευση στη βάση
                db.routeDao().insertRoute(newRoute);

                // Επιστροφη στο UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PublishRideActivity.this, "Η διαδρομή δημοσιεύτηκε!", Toast.LENGTH_LONG).show();

                        //στέλνουμε στην οθόνη "Οι Διαδρομές μου" για να δει τι μόλις ανέβασε
                        Intent intent = new Intent(PublishRideActivity.this, MyRidesActivity.class);
                        intent.putExtra("USER_ID", currentDriverId);
                        startActivity(intent);

                        finish();
                    }
                });
            }
        });
    }
}