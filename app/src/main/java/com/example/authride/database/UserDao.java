package com.example.authride.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    // Εγγραφή νέου χρήστη
    @Insert
    long insertUser(User user);

    // Login: Ψάχνει αν υπάρχει χρήστης με αυτό το email και κωδικό
    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    User login(String email, String password);

    // Έλεγχος αν υπάρχει ήδη το email (για να μην κάνουν διπλή εγγραφή)
    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);
}