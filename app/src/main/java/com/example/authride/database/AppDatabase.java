package com.example.authride.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// leme sth Room poia einai ta entities
@Database(entities = {User.class, Route.class, Reservation.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Δίνουμε πρόσβαση sta DAO
    public abstract UserDao userDao();
    public abstract RouteDao routeDao();
    public abstract ReservationDao reservationDao();

    //μηχανισμός Singleton
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "authride_db") // Το όνομα του αρχείου της βάσης στο κινητό
                            .fallbackToDestructiveMigration() // Αν στο μέλλον προσθέσουμε στήλες, απλά διαγράφει τα παλιά δεδομένα και ξεκινάει από την αρχή
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
