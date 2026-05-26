package com.example.authride.util;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.authride.R;

/**
 * Κεντρικό σημείο για ό,τι αφορά ειδοποιήσεις (notifications):
 *  - δημιουργία των καναλιών (απαιτείται από Android 8+)
 *  - έλεγχος/αίτημα της άδειας POST_NOTIFICATIONS (απαιτείται από Android 13+)
 *  - εμφάνιση μιας ειδοποίησης με ένα μόνο call.
 */
public final class NotificationHelper {

    /** Κανάλι για "νέα κράτηση στη διαδρομή σου" (οδηγός). */
    public static final String CHANNEL_BOOKINGS = "channel_bookings";
    /** Κανάλι για "ώρα να συναντήσεις τον οδηγό" (επιβάτης). */
    public static final String CHANNEL_REMINDERS = "channel_reminders";

    public static final int REQ_POST_NOTIFICATIONS = 1001;

    private NotificationHelper() { }

    /** Δημιουργεί τα κανάλια ειδοποιήσεων. Ασφαλές να κληθεί πολλές φορές. */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel bookings = new NotificationChannel(
                CHANNEL_BOOKINGS,
                context.getString(R.string.notif_channel_bookings),
                NotificationManager.IMPORTANCE_HIGH);
        bookings.setDescription(context.getString(R.string.notif_channel_bookings_desc));

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH);
        reminders.setDescription(context.getString(R.string.notif_channel_reminders_desc));

        nm.createNotificationChannel(bookings);
        nm.createNotificationChannel(reminders);
    }

    /**
     * Ζητά (αν χρειάζεται) την άδεια POST_NOTIFICATIONS. Από Android 13 (TIRAMISU)
     * και πάνω είναι runtime permission· σε παλαιότερες εκδόσεις δεν χρειάζεται.
     */
    public static void ensurePostPermission(Activity activity) {
        createChannels(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted = ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                        REQ_POST_NOTIFICATIONS);
            }
        }
    }

    /** true αν επιτρέπεται να εμφανίσουμε ειδοποιήσεις. */
    public static boolean canNotify(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Εμφανίζει μία ειδοποίηση. Το {@code contentIntent} (προαιρετικό) ορίζει
     * ποια οθόνη ανοίγει με το πάτημα της ειδοποίησης.
     */
    public static void show(Context context, String channelId, int notificationId,
                            String title, String text, PendingIntent contentIntent) {
        createChannels(context);
        if (!canNotify(context)) return;

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_directions_car)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (contentIntent != null) b.setContentIntent(contentIntent);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, b.build());
        } catch (SecurityException ignored) {
            // Η άδεια ανακλήθηκε στο μεταξύ· αγνοούμε σιωπηλά.
        }
    }
}