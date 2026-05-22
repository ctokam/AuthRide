package com.example.authride.util;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.authride.MyBookingsActivity;
import com.example.authride.MyRidesActivity;
import com.example.authride.PassengerActivity;
import com.example.authride.ProfileActivity;
import com.example.authride.PublishRideActivity;
import com.example.authride.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Κεντρική διαχείριση πλοήγησης του bottom navigation. Καλύπτει και τα δύο menu
 * (οδηγού & επιβάτη) αφού τα item ids είναι μοναδικά μεταξύ τους.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {
    }

    public static void setup(AppCompatActivity activity, BottomNavigationView nav, int currentItemId) {
        nav.setSelectedItemId(currentItemId);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) {
                return true; // ήδη εδώ
            }

            Intent intent = null;
            if (id == R.id.nav_publish) {
                intent = new Intent(activity, PublishRideActivity.class);
            } else if (id == R.id.nav_my_rides) {
                intent = new Intent(activity, MyRidesActivity.class);
            } else if (id == R.id.nav_available) {
                intent = new Intent(activity, PassengerActivity.class);
            } else if (id == R.id.nav_my_bookings) {
                intent = new Intent(activity, MyBookingsActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(activity, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}