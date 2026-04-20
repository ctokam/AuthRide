package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DriverOrPassengerActivity extends AppCompatActivity {

    private Button btnDriver, btnPassenger;
    private int currentUserId = -1; //αποθηκευση ID προσωρινά

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_or_passenger);

        // κουμπιά από το XML
        btnDriver = findViewById(R.id.btn_driver);
        btnPassenger = findViewById(R.id.btn_passenger);

         currentUserId = getIntent().getIntExtra("USER_ID", -1);

        // Ασφάλεια: Αν χαθεί το ID τον διώχνουμε πίσω
        if (currentUserId == -1) {
            Toast.makeText(this, "Προέκυψε σφάλμα σύνδεσης. Παρακαλώ δοκιμάστε ξανά.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ΕΠΙΛΟΓΗ ΟΔΗΓΟΣ
        btnDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //πάμε στη φόρμα δημοσίευσης διαδρομής
                Intent intent = new Intent(DriverOrPassengerActivity.this, PublishRideActivity.class);
                // Περνάμε το ID στην επόμενη οθόνη για να ξέρουμε τον οδηγο
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
            }
        });

        // ΕΠΙΛΟΓΗ ΕΠΙΒΑΤΗΣ
        btnPassenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // πάμε στη λίστα με διαθέσιμες διαδρομές
                Intent intent = new Intent(DriverOrPassengerActivity.this, PassengerActivity.class);
                // Περνάμε το ID στην επόμενη οθόνη για να ξέρουμε ποιος κλείνει θέση
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
            }
        });
    }
}