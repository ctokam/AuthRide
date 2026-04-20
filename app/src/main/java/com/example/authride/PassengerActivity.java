package com.example.authride;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authride.adapters.RouteAdapter;
import com.example.authride.database.AppDatabase;
import com.example.authride.database.Reservation;
import com.example.authride.database.Route;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PassengerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private Button btnRefresh;

    private AppDatabase db;
    private ExecutorService executorService;
    private int currentPassengerId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        //Σύνδεση με XML
        recyclerView = findViewById(R.id.recyclerViewRoutes);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnRefresh = findViewById(R.id.btn_refresh);

        // κάθετη λίστα
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Αρχικοποίηση Βάσης
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        // Λήψη ID επιβάτη
        currentPassengerId = getIntent().getIntExtra("USER_ID", -1);

        // Φορτώνουμε τις διαδρομές μόλις ανοίξει η οθόνη
        loadAvailableRoutes();

        // Το κουμπί ανανέωσης στο Empty State
        btnRefresh.setOnClickListener(v -> loadAvailableRoutes());
    }

    //ΦΟΡΤΩΣΗ ΔΙΑΔΡΟΜΩΝ
    private void loadAvailableRoutes() {
        executorService.execute(() -> {
            //θέσεις > 0
            List<Route> availableRoutes = db.routeDao().getAvailableRoutes();

            runOnUiThread(() -> {
                if (availableRoutes.isEmpty()) {
                    //Empty State-- κρύψε τη λίστα
                    recyclerView.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    // Κρύψε Empty State και εμφάνισε τη λίστα
                    layoutEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    RouteAdapter adapter = new RouteAdapter(availableRoutes, new RouteAdapter.OnRouteClickListener() {
                        @Override
                        public void onBookClick(Route route) {
                            bookRoute(route);
                        }
                    });
                    recyclerView.setAdapter(adapter);
                }
            });
        });
    }

    //ΚΡΑΤΗΣΗ
    private void bookRoute(Route route) {
        executorService.execute(() -> {
            // Έλεγχος: Μήπως την έχει ήδη κλείσει
            Reservation existingRes = db.reservationDao().checkReservation(currentPassengerId, route.getId());

            if (existingRes != null) {
                runOnUiThread(() -> Toast.makeText(PassengerActivity.this, "Έχεις ήδη κλείσει θέση σε αυτή τη διαδρομή!", Toast.LENGTH_SHORT).show());
                return;
            }

            //Δημιουργία νέας κράτησης
            Reservation newReservation = new Reservation(currentPassengerId, route.getId());
            db.reservationDao().insertReservation(newReservation);

            //Μείωση των διαθέσιμων θέσεων του οδηγού κατά 1
            int newSeats = route.getAvailableSeats() - 1;
            db.routeDao().updateSeats(route.getId(), newSeats);

            runOnUiThread(() -> {
                Toast.makeText(PassengerActivity.this, "Η κράτηση ολοκληρώθηκε επιτυχώς!", Toast.LENGTH_SHORT).show();
                // Ξαναφορτώνουμε τη λίστα. Αν οι θέσεις πήγαν στο 0 η διαδρομή θα εξαφανιστεί
                loadAvailableRoutes();
            });
        });
    }
}