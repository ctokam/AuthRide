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
 *
 * Σημαντικό για το σωστό highlight: ο listener επιστρέφει false όταν πλοηγούμαστε
 * σε άλλη οθόνη, ώστε να ΜΗΝ "κολλήσει" σκιασμένο το item του προορισμού πάνω
 * στην τρέχουσα οθόνη. Κάθε Activity ορίζει το δικό της επιλεγμένο item εδώ, στο
 * onCreate της — και επειδή η επιστροφή γίνεται με REORDER_TO_FRONT (χωρίς νέο
 * onCreate), το highlight κάθε οθόνης παραμένει αυτό που όρισε η ίδια.
 */
public final class BottomNavHelper {

    private BottomNavHelper() {
    }

    public static void setup(AppCompatActivity activity, BottomNavigationView nav, int currentItemId) {
        // Όρισε το τρέχον item ΧΩΡΙΣ να πυροδοτηθεί πλοήγηση (listener προσωρινά null).
        nav.setOnItemSelectedListener(null);
        nav.setSelectedItemId(currentItemId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) {
                return true; // ήδη εδώ -> κράτα το highlighted
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
            // false -> μην αλλάξεις το highlight της οθόνης που φεύγει.
            return false;
        });
    }
}