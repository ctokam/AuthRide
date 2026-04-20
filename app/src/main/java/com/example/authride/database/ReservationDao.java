package com.example.authride.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ReservationDao {

    // Επιβάτης: Κάνει κράτηση (προσθήκη)
    @Insert
    long insertReservation(Reservation reservation);

    // Επιβάτης: Ακυρώνει την κράτηση (διαγραφή)
    @Query("DELETE FROM reservations WHERE passengerId = :passengerId AND routeId = :routeId")
    void deleteReservation(int passengerId, int routeId);

    // Επιβάτης: Φέρε όλα τα ID των διαδρομών που έχει κλείσει (για το MyRides)
    @Query("SELECT routeId FROM reservations WHERE passengerId = :passengerId")
    List<Integer> getReservedRouteIdsByPassenger(int passengerId);

    // Σύστημα: Έλεγχος αν ο επιβάτης έχει ήδη κλείσει αυτή τη διαδρομή
    @Query("SELECT * FROM reservations WHERE passengerId = :passengerId AND routeId = :routeId")
    Reservation checkReservation(int passengerId, int routeId);
}
