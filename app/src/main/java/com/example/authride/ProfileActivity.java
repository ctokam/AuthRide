package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.authride.database.AppDatabase;
import com.example.authride.database.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail;
    private Button btnSwitchRole, btnLogout;
    private BottomNavigationView bottomNavigationView;

    private AppDatabase db;
    private ExecutorService executorService;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Σύνδεση με το XML
        tvUserName = findViewById(R.id.tv_profile_name);
        tvUserEmail = findViewById(R.id.tv_profile_email);
        btnSwitchRole = findViewById(R.id.btn_switch_role);
        btnLogout = findViewById(R.id.btn_logout);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Αρχικοποίηση Βάσης
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        //Λήψη του ID χρήστη
        currentUserId = getIntent().getIntExtra("USER_ID", -1);

        if (currentUserId != -1) {
            loadUserProfile();
        }

        //----------Κουμπια------------------

        // ΑΛΛΑΓΗ ΡΟΛΟΥ: στέλνουμε πίσω στην οθόνη επιλογής
        btnSwitchRole.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, DriverOrPassengerActivity.class);
            intent.putExtra("USER_ID", currentUserId);
            startActivity(intent);
            finish();
        });

        // ΑΠΟΣΥΝΔΕΣΗ: καθαρίζουμε το stack και πάμε στο Login
        btnLogout.setOnClickListener(v -> {
            Toast.makeText(ProfileActivity.this, "Αποσυνδεθήκατε επιτυχώς", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        //Bottom Navigation
        setupNavigation();
    }

    private void loadUserProfile() {
        executorService.execute(() -> {
             User user = db.userDao().getUserByEmail(getIntent().getStringExtra("USER_EMAIL"));
        });
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;

            if (id == R.id.nav_search || id == R.id.nav_my_rides) {
                finish();
                return true;
            }
            return false;
        });
    }
}