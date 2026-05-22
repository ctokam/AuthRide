package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.authride.model.User;
import com.example.authride.util.BottomNavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Οθόνη προφίλ (κοινή για οδηγό/επιβάτη). Δείχνει στοιχεία χρήστη, επιτρέπει
 * αλλαγή ρόλου, αποσύνδεση και διαγραφή λογαριασμού. Το bottom navigation
 * προσαρμόζεται στον ρόλο.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvAvatar;
    private Button btnSwitchRole, btnLogout, btnDeleteAccount;
    private BottomNavigationView nav;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String myUid;
    private String currentRole = User.ROLE_PASSENGER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }
        myUid = auth.getCurrentUser().getUid();

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvAvatar = findViewById(R.id.tv_avatar_initial);
        btnSwitchRole = findViewById(R.id.btn_switch_role);
        btnLogout = findViewById(R.id.btn_logout);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
        nav = findViewById(R.id.bottom_navigation);

        btnSwitchRole.setOnClickListener(v -> switchRole());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) return;

                    String name = user.getFullName() != null ? user.getFullName() : "";
                    tvName.setText(name);
                    tvEmail.setText(user.getEmail());
                    tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());

                    currentRole = user.getRole() != null ? user.getRole() : User.ROLE_PASSENGER;
                    applyRoleUi();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία φόρτωσης προφίλ", Toast.LENGTH_LONG).show());
    }

    /** Ρυθμίζει το κουμπί αλλαγής ρόλου και το σωστό bottom navigation menu. */
    private void applyRoleUi() {
        boolean isDriver = User.ROLE_DRIVER.equals(currentRole);
        btnSwitchRole.setText(isDriver
                ? R.string.btn_switch_to_passenger
                : R.string.btn_switch_to_driver);

        nav.getMenu().clear();
        nav.inflateMenu(isDriver ? R.menu.bottom_nav_driver : R.menu.bottom_nav_passenger);
        BottomNavHelper.setup(this, nav, R.id.nav_profile);
    }

    private void switchRole() {
        String newRole = User.ROLE_DRIVER.equals(currentRole)
                ? User.ROLE_PASSENGER
                : User.ROLE_DRIVER;

        Map<String, Object> data = new HashMap<>();
        data.put("role", newRole);

        db.collection("users").document(myUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Intent intent = User.ROLE_DRIVER.equals(newRole)
                            ? new Intent(this, PublishRideActivity.class)
                            : new Intent(this, PassengerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Διαγραφή λογαριασμού")
                .setMessage("Σίγουρα θέλεις να διαγράψεις τον λογαριασμό σου; "
                        + "Η ενέργεια είναι μη αναστρέψιμη.")
                .setPositiveButton("Διαγραφή", (d, w) -> deleteAccount())
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    /**
     * Διαγράφει πρώτα το έγγραφο προφίλ (users/{uid}) και μετά τον ίδιο τον
     * λογαριασμό από το Firebase Authentication. Αν το Firebase ζητήσει
     * πρόσφατη σύνδεση, ενημερώνουμε τον χρήστη να ξανασυνδεθεί.
     */
    private void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }
        db.collection("users").document(myUid).delete()
                .addOnCompleteListener(t -> user.delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Ο λογαριασμός διαγράφηκε",
                                    Toast.LENGTH_LONG).show();
                            goToLogin();
                        })
                        .addOnFailureListener(e -> {
                            if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(this,
                                        "Για ασφάλεια, αποσυνδέσου και ξανασυνδέσου, "
                                                + "και μετά δοκίμασε ξανά τη διαγραφή.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Αποτυχία: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }));
    }

    private void logout() {
        auth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
