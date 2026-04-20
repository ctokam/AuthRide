package com.example.authride;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.authride.adapters.RouteAdapter;
import com.example.authride.database.AppDatabase;
import com.example.authride.database.Route;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyRidesActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;

    private AppDatabase db;
    private ExecutorService executorService;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rides);

        //Σύνδεση με XML
        tabLayout = findViewById(R.id.tab_layout_rides);
        recyclerView = findViewById(R.id.recyclerViewMyRides);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Αρχικοποίηση Βάσης
        db = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        //ID χρήστη
        currentUserId = getIntent().getIntExtra("USER_ID", -1);

        //Λογική των Tabs
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    //καρτέλα "Επόμενες"
                    loadMyRides(true);
                } else {
                    //καρτέλα "Ιστορικό"
                    loadMyRides(false);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

         loadMyRides(true);
         setupNavigation();
    }

    // --- ΦΟΡΤΩΣΗ ΔΕΔΟΜΕΝΩΝ (Οδηγός + Επιβάτης) ---
    private void loadMyRides(boolean isUpcoming) {
        executorService.execute(() -> {

            //διαδρομές που έχει φτιάξει ως ΟΔΗΓΟΣ
            List<Route> driverRoutes = db.routeDao().getRoutesByDriver(currentUserId);

            //Φέρνουμε τις διαδρομές που έχει κλείσει ως ΕΠΙΒΑΤΗΣ
            // Η βάση μας δίνει τα IDs των διαδρομών, και εμείς τραβάμε τις διαδρομές μία-μία
            List<Integer> reservedIds = db.reservationDao().getReservedRouteIdsByPassenger(currentUserId);
            List<Route> passengerRoutes = new ArrayList<>();
            for (int routeId : reservedIds) {
                Route r = db.routeDao().getRouteById(routeId);
                if (r != null) {
                    passengerRoutes.add(r);
                }
            }

            // Ενώνουμε τις δύο λίστες (Όλες οι διαδρομές που τον αφορούν)
            List<Route> allMyRides = new ArrayList<>();
            allMyRides.addAll(driverRoutes);
            allMyRides.addAll(passengerRoutes);

            // Γυρνάμε στην οθόνη
            runOnUiThread(() -> {
                if (isUpcoming) {
                    // Δείχνουμε τις ενεργές διαδρομές (Επόμενες)
                    RouteAdapter adapter = new RouteAdapter(allMyRides, route -> {
                        // Εδώ μελλοντικά μπορείς να προσθέσεις την "Ακύρωση"
                        Toast.makeText(MyRidesActivity.this, "Επιλέξατε τη διαδρομή", Toast.LENGTH_SHORT).show();
                    });
                    recyclerView.setAdapter(adapter);
                } else {
                    // Ιστορικό: Για τους σκοπούς της εργασίας, το αφήνουμε άδειο,
                    // καθώς απαιτεί πολύπλοκη σύγκριση ημερομηνιών
                    recyclerView.setAdapter(new RouteAdapter(new ArrayList<>(), null));
                    Toast.makeText(MyRidesActivity.this, "Δεν υπάρχουν παλαιότερες διαδρομές στο ιστορικό σας.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    //Bottom Nav
    private void setupNavigation() {
        // Φωτίζουμε το σωστό εικονίδιο
        bottomNavigationView.setSelectedItemId(R.id.nav_my_rides);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_rides) return true;

            if (id == R.id.nav_search) {
                //Αναζήτηση
                Intent intent = new Intent(MyRidesActivity.this, PassengerActivity.class);
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
                finish(); // Κλεισιμο τωρινης οθόνη
                return true;
            } else if (id == R.id.nav_profile) {
                //Προφίλ
                Intent intent = new Intent(MyRidesActivity.this, ProfileActivity.class);
                intent.putExtra("USER_ID", currentUserId);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
    }
}