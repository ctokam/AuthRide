package com.example.authride.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.authride.model.Booking;

/**
 * Προγραμματίζει (και ακυρώνει) μια τοπική ειδοποίηση-υπενθύμιση για τον επιβάτη,
 * λίγο πριν την ώρα αναχώρησης ("ώρα να συναντήσεις τον οδηγό").
 *
 * Χρησιμοποιεί AlarmManager + BroadcastReceiver — δεν χρειάζεται server.
 */
public final class MeetReminderScheduler {

    /** Πόσα λεπτά πριν την αναχώρηση χτυπά η υπενθύμιση. */
    public static final long LEAD_MILLIS = 15L * 60L * 1000L;

    public static final String ACTION_MEET = "com.example.authride.ACTION_MEET_REMINDER";
    public static final String EXTRA_BOOKING_ID = "bookingId";
    public static final String EXTRA_DESTINATION = "destination";
    public static final String EXTRA_START = "startLocation";
    public static final String EXTRA_DEPARTURE = "departureMillis";

    private MeetReminderScheduler() { }

    /** Προγραμματίζει την υπενθύμιση για μια κράτηση (αν η ώρα είναι ακόμη στο μέλλον). */
    public static void schedule(Context context, Booking booking) {
        if (booking.getId() == null) return;
        long triggerAt = booking.getDepartureMillis() - LEAD_MILLIS;
        if (triggerAt <= System.currentTimeMillis()) return; // πολύ αργά, δεν προγραμματίζουμε

        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context, booking);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // Χωρίς άδεια exact alarm -> inexact (μικρή απόκλιση, αποδεκτή για υπενθύμιση).
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /** Ακυρώνει μια προγραμματισμένη υπενθύμιση (π.χ. όταν ακυρώνεται η κράτηση). */
    public static void cancel(Context context, String bookingId) {
        if (bookingId == null) return;
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;

        Intent intent = new Intent(context, MeetReminderReceiver.class).setAction(ACTION_MEET);
        PendingIntent pi = PendingIntent.getBroadcast(context, bookingId.hashCode(), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    private static PendingIntent buildPendingIntent(Context context, Booking booking) {
        Intent intent = new Intent(context, MeetReminderReceiver.class)
                .setAction(ACTION_MEET)
                .putExtra(EXTRA_BOOKING_ID, booking.getId())
                .putExtra(EXTRA_DESTINATION, booking.getDestination())
                .putExtra(EXTRA_START, booking.getStartLocation())
                .putExtra(EXTRA_DEPARTURE, booking.getDepartureMillis());

        return PendingIntent.getBroadcast(context, booking.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}