package com.example.authride.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.authride.MyBookingsActivity;
import com.example.authride.R;
import com.example.authride.model.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Δέχεται το "χτύπημα" του AlarmManager και εμφανίζει την υπενθύμιση στον επιβάτη.
 *
 * Πριν δείξει την ειδοποίηση, ΕΛΕΓΧΕΙ στο Firestore ότι η κράτηση είναι ακόμη
 * ενεργή. Έτσι, αν στο μεταξύ ο οδηγός ακύρωσε τη διαδρομή (ή ακυρώθηκε η κράτηση
 * από άλλη συσκευή), δεν στέλνουμε άσκοπη υπενθύμιση.
 *
 * Επειδή η ανάγνωση από Firestore είναι ασύγχρονη και ένας BroadcastReceiver
 * "ζει" ελάχιστα, χρησιμοποιούμε goAsync() ώστε το σύστημα να μας κρατήσει
 * ζωντανούς μέχρι να ολοκληρωθεί ο έλεγχος (καλούμε finish() σε κάθε περίπτωση).
 */
public class MeetReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper.createChannels(context);

        final String bookingId = intent.getStringExtra(MeetReminderScheduler.EXTRA_BOOKING_ID);
        final String dest = intent.getStringExtra(MeetReminderScheduler.EXTRA_DESTINATION);
        final String start = intent.getStringExtra(MeetReminderScheduler.EXTRA_START);
        final long departure = intent.getLongExtra(MeetReminderScheduler.EXTRA_DEPARTURE, 0L);

        // Χωρίς id δεν μπορούμε να ελέγξουμε· δείχνουμε κατευθείαν (fail-open).
        if (bookingId == null) {
            showReminder(context, null, dest, start, departure);
            return;
        }

        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync(); // κράτα ζωντανό τον receiver όσο τρέχει ο έλεγχος

        FirebaseFirestore.getInstance()
                .collection("bookings").document(bookingId)
                .get()
                .addOnSuccessListener(doc -> {
                    Booking b = doc.exists() ? doc.toObject(Booking.class) : null;
                    // Δείξε την υπενθύμιση μόνο αν η κράτηση υπάρχει και ΔΕΝ είναι ακυρωμένη.
                    if (b != null && !b.isCancelled()) {
                        showReminder(app, bookingId, dest, start, departure);
                    }
                    pending.finish();
                })
                .addOnFailureListener(e -> {
                    // Αποτυχία ανάγνωσης (π.χ. χωρίς δίκτυο): δείχνουμε την υπενθύμιση
                    // ούτως ή άλλως, ώστε ο επιβάτης να μη χάσει τη διαδρομή του.
                    showReminder(app, bookingId, dest, start, departure);
                    pending.finish();
                });
    }

    private void showReminder(Context context, String bookingId,
                              String dest, String start, long departure) {
        String title = context.getString(R.string.notif_meet_title);
        String text = context.getString(R.string.notif_meet_text,
                dest != null ? dest : "",
                TimeUtils.time(departure),
                start != null ? start : "");

        Intent open = new Intent(context, MyBookingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = bookingId != null ? bookingId.hashCode() : (int) System.currentTimeMillis();
        PendingIntent pi = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationHelper.show(context, NotificationHelper.CHANNEL_REMINDERS,
                id, title, text, pi);
    }
}