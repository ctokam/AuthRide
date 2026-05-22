package com.example.authride.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Βοηθητικές μέθοδοι μορφοποίησης ημερομηνίας/ώρας από timestamp (millis). */
public final class TimeUtils {

    private static final Locale EL = new Locale("el", "GR");

    private TimeUtils() {
    }

    /** π.χ. "25/05 08:30" — για τις κάρτες διαδρομών. */
    public static String dateTime(long millis) {
        return new SimpleDateFormat("dd/MM HH:mm", EL).format(new Date(millis));
    }

    /** π.χ. "25/05/2026" — για το πεδίο ημερομηνίας. */
    public static String date(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", EL).format(new Date(millis));
    }

    /** π.χ. "08:30" — για το πεδίο ώρας. */
    public static String time(long millis) {
        return new SimpleDateFormat("HH:mm", EL).format(new Date(millis));
    }
}