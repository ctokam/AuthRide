package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.authride.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Οθόνη επιλογής ρόλου μετά τη σύνδεση. Ο ίδιος λογαριασμός μπορεί να γίνει
 * οδηγός ή επιβάτης· η επιλογή αποθηκεύεται στο πεδίο role και ανοίγει την
 * αντίστοιχη κύρια οθόνη.
 */
public class DriverOrPassengerActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_or_passenger);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Αν δεν υπάρχει συνδεδεμένος χρήστης, επιστροφή στο login.
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Button btnDriver = findViewById(R.id.btn_driver);
        Button btnPassenger = findViewById(R.id.btn_passenger);

        btnDriver.setOnClickListener(v -> chooseRole(User.ROLE_DRIVER));
        btnPassenger.setOnClickListener(v -> chooseRole(User.ROLE_PASSENGER));
    }

    /**
     * Αποθηκεύει τον επιλεγμένο ρόλο στο users/{uid}.
     *
     * Χρησιμοποιούμε set(..., SetOptions.merge()) αντί για update():
     *  - merge() ΔΗΜΙΟΥΡΓΕΙ το έγγραφο αν δεν υπάρχει, ή ενημερώνει μόνο το
     *    πεδίο "role" αν υπάρχει — χωρίς να σβήνει τα υπόλοιπα πεδία του προφίλ.
     *  - update() αντίθετα πετάει NOT_FOUND όταν το έγγραφο δεν προϋπάρχει,
     *    που είναι ακριβώς το σφάλμα που βλέπαμε στο Logcat.
     */
    private void chooseRole(String role) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("role", role);

        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> openArea(role))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    /**
     * Ανοίγει την κύρια οθόνη ανάλογα με τον ρόλο και καθαρίζει το back stack,
     * ώστε το back να μην επιστρέφει στην οθόνη επιλογής ρόλου.
     */
    private void openArea(String role) {
        Intent intent = User.ROLE_DRIVER.equals(role)
                ? new Intent(this, PublishRideActivity.class)
                : new Intent(this, PassengerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}