package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Οθόνη σύνδεσης. Αν ο χρήστης είναι ήδη συνδεδεμένος, παρακάμπτει την οθόνη.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-login: αν υπάρχει ήδη συνδεδεμένος χρήστης, πήγαινε κατευθείαν παρακάτω.
        if (auth.getCurrentUser() != null) {
            goToRoleSelection();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Συμπλήρωσε το email σου");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Συμπλήρωσε τον κωδικό σου");
            return;
        }

        btnLogin.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToRoleSelection();
                    } else {
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Λάθος email ή κωδικός", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToRoleSelection() {
        Intent intent = new Intent(this, DriverOrPassengerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}