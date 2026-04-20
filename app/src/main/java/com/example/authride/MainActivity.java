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

public class MainActivity extends AppCompatActivity {

    // 1. Δηλώνουμε στοιχεία της οθόνης
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister;

    // 2. Δηλώνουμε τη Βάση Δεδομένων και το Background Thread
    private AppDatabase db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Φορτώνει το XML του Login

        // 3. Βρίσκουμε τα στοιχεία από XML
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // Αρχικοποιούμε τη Βάση και το Thread
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // 4. Τι γίνεται όταν πατάμε το κουμπί "Σύνδεση"
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // 5. Τι γίνεται όταν πατάμε το "Δεν έχετε λογαριασμό; Εγγραφή"
        tvGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Πάμε στην οθόνη Εγγραφής
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    // Η ΛΟΓΙΚΗ ΤΟΥ LOGIN
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        //Misuse Case: Αν άφησε κάτι κενό
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Στέλνουμε τον "εργάτη" στο παρασκήνιο να ρωτήσει τη βάση
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Ψάχνουμε τον χρήστη μέσω DAO
                User user = db.userDao().login(email, password);

                // Επιστρέφουμε στην Main Thread για να δείξουμε το αποτέλεσμα
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (user != null) {
                            //Βρέθηκε ο χρήστης
                            Toast.makeText(MainActivity.this, "Καλώς ήρθες " + user.getName() + "!", Toast.LENGTH_SHORT).show();

                            // Πάμε στην οθόνη Επιλογής Ρόλου
                            Intent intent = new Intent(MainActivity.this, DriverOrPassengerActivity.class);

                            //Περνάμε το ID του χρήστη στην επόμενη οθόνη για να ξέρουμε ποιος είναι
                            intent.putExtra("USER_ID", user.getId());

                            startActivity(intent);
                            finish(); //Κλείνουμε την οθόνη του Login για να μην μπορεί να γυρίσει πίσω με το back button
                        } else {
                            //Λάθος email ή κωδικός
                            Toast.makeText(MainActivity.this, "Λάθος email ή κωδικός!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}