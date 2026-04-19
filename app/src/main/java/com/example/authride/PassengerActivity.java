package com.example.authride;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.authride.adapters.RouteAdapter;
import java.util.ArrayList;
import java.util.List;

public class PassengerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RouteAdapter adapter;
    private List<Routes> routeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Συνδση Activity με το XML αρχείο οθόνης
        setContentView(R.layout.activity_passenger);

        //Βρίσκουμε το RecyclerView μέσω του ID που του XML
        recyclerView = findViewById(R.id.recyclerViewRoutes);

        //Οριζουμε ότι η λίστα μας θα είναι κάθετη (από πάνω προς τα κάτω)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Dummy data
        routeList = new ArrayList<>();

        // Προσθέτουμε 3 εικονικές διαδρομές χρησιμοποιώντας την κλάση Routes
        routeList.add(new Routes("Γιώργος Παπαδόπουλος", "08:30 ΠΜ", "Καλαμαριά (Κέντρο)", "ΑΠΘ (ΣΘΕ)"));
        routeList.add(new Routes("Μαρία Κώστα", "09:15 ΠΜ", "Τούμπα (Γήπεδο)", "ΑΠΘ (Νομική)"));
        routeList.add(new Routes("Νίκος Ανδρέου", "10:00 ΠΜ", "Εύοσμος (Πλατεία)", "ΑΠΘ (Βιβλιοθήκη)"));

        //Ενώνουμε τα δεδομένα με τον Adapter και τον δίνουμε στο RecyclerView
        adapter = new RouteAdapter(routeList);
        recyclerView.setAdapter(adapter);
    }
}