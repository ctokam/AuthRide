package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.authride.database.AppDatabase;
import com.example.authride.database.User;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    // 1.Δηλώνουμε τα στοιχεία XML
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;

    // 2.Εργαλεία Βάσης Δεδομένων
    private AppDatabase db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Φορτώνει το XML της Εγγραφής

        // 3. Σύνδεση με τα ID του XML (Βεβαιώσου ότι τα ID ταιριάζουν με αυτά που έβαλες στο XML σου)
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        // Αρχικοποίηση
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // 4. Κουμπί Εγγραφή
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // 5. Κουμπί επιστροφής στο Login ("Έχετε ήδη λογαριασμό;")
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //κλείνουμε αυτή την οθόνη και γυρνάει στο Login
            }
        });
    }

    //ΛΟΓΙΚΗ ΤΗΣ ΕΓΓΡΑΦΗΣ
    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ΦΙΛΤΡΟ 1: Κενά πεδία
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ΦΙΛΤΡΟ 2: Ιδρυματικό Email - Misuse Case
        if (!email.endsWith("@csd.auth.gr")) {
            etEmail.setError("Απαιτείται email του Τμήματος Πληροφορικής (@csd.auth.gr)!");
            etEmail.requestFocus();
            return;
        }

        // ΦΙΛΤΡΟ 3: Ταίριασμα Κωδικών - Misuse Case
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Οι κωδικοί δεν ταιριάζουν!");
            etConfirmPassword.requestFocus();
            return;
        }

        // ρωτάμε τη Βάση στο Background Thread
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // ΦΙΛΤΡΟ 4: Ελέγχουμε αν υπάρχει ήδη αυτό το email στη βάση
                User existingUser = db.userDao().getUserByEmail(email);

                if (existingUser != null) {
                    //περιπτωση υπάρχει ήδη Επιστρέφουμε στην οθόνη για να το πούμε στον χρήστη.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            etEmail.setError("Αυτό το email χρησιμοποιείται ήδη!");
                            etEmail.requestFocus();
                        }
                    });
                } else {
                    //Φτιάχνουμε νέο αντικείμενο User
                    User newUser = new User(name, email, password);

                    // Το αποθηκεύουμε στη βάση
                    long newUserId = db.userDao().insertUser(newUser);
                    //μήνυμα επιτυχίας
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterActivity.this, "Επιτυχής εγγραφή! Καλώς ήρθες, " + name + "!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, DriverOrPassengerActivity.class);
                            intent.putExtra("USER_ID", (int) newUserId); // Περνάμε το ID του για να ξέρει η εφαρμογή ποιος είναι
                            startActivity(intent);
                            finish(); // Κλείνουμε εγγραφή και γυρνάμε στο Login
                        }
                    });
                }
            }
        });
    }
}