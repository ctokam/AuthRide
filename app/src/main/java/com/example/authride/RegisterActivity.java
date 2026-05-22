package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.authride.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Οθόνη εγγραφής. Δημιουργεί χρήστη στο Firebase Authentication (email/password)
 * και αποθηκεύει το προφίλ του στο Firestore (users/{uid}).
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        TextView tvGoToLogin = findViewById(R.id.tv_go_to_login);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Συμπλήρωσε το ονοματεπώνυμό σου");
            return;
        }
        if (!isAuthEmail(email)) {
            etEmail.setError("Χρησιμοποίησε πανεπιστημιακό email (.auth.gr)");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες");
            return;
        }

        btnRegister.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        saveUserProfile(auth.getCurrentUser().getUid(), name, email);
                    } else {
                        btnRegister.setEnabled(true);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Η εγγραφή απέτυχε";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserProfile(String uid, String name, String email) {
        // Προεπιλεγμένος ρόλος· αλλάζει στην οθόνη επιλογής ρόλου.
        User user = new User(name, email, User.ROLE_PASSENGER);
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(unused -> goToRoleSelection())
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Αποτυχία αποθήκευσης προφίλ: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToRoleSelection() {
        Intent intent = new Intent(this, DriverOrPassengerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Δεκτά μόνο emails του ΑΠΘ: auth.gr ή υποτομέας (π.χ. csd.auth.gr). */
    private boolean isAuthEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1);
        return domain.equals("auth.gr") || domain.endsWith(".auth.gr");
    }
}