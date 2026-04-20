package com.example.authride.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface RouteDao {

    // Οδηγός: Δημοσίευση νέας διαδρομής
    @Insert
    long insertRoute(Route route);

    // Επιβάτης: Φέρε όλες τις διαδρομές που έχουν διαθέσιμες θέσεις
    @Query("SELECT * FROM routes WHERE availableSeats > 0")
    List<Route> getAvailableRoutes();

    // Οδηγός: Φέρε τις διαδρομές που έχει δημιουργήσει ο ίδιος
    @Query("SELECT * FROM routes WHERE driverId = :driverId")
    List<Route> getRoutesByDriver(int driverId);

    // Σύστημα: Μείωση/Αύξηση θέσεων όταν γίνεται κράτηση ή ακύρωση
    @Query("UPDATE routes SET availableSeats = :newSeats WHERE id = :routeId")
    void updateSeats(int routeId, int newSeats);

    // Σύστημα: Βρες μια συγκεκριμένη διαδρομή από το ID της
    @Query("SELECT * FROM routes WHERE id = :routeId")
    Route getRouteById(int routeId);
}