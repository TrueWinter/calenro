package dev.truewinter.calenro;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

public class NotificationManager {
    private static NotificationManager instance = null;

    // See comment in getNotificationId()
    public static final int PERMANENT_NOTIFICATION_ID = 1073741825;

    public enum NotificationChannel {
        DAILY("daily", "Daily Notifications", null, false),
        EVENT("event", "Event Notifications", "Receive a notification at the start of an event", false),
        PERMANENT("summary", "Permanent Summary Notification", "Shows a permanent silent notification", true);

        private String id;
        private String name;
        private String description;
        private boolean lowPriority;

        NotificationChannel(String id, String name, String description, boolean lowPriority) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.lowPriority = lowPriority;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isLowPriority() {
            return lowPriority;
        }
    }

    public enum NotificationType {
        DAILY_NOTIFICATION,
        EVENT_NOTIFICATION,
        PERMANENT_NOTIFICATION
    }

    private static int lastNotificationId = 0;

    public static NotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationManager();

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            int _lastNotificationId = sharedPreferences.getInt("_lastNotificationId", -1);

            if (_lastNotificationId != -1) {
                lastNotificationId = _lastNotificationId;
            }
        }

        return instance;
    }

    /**
     * Sends a notification with custom ID from reserved range
     * @param notification The notification to send
     * @param notificationId Notification ID, must be from reserved range
     * @return returns the notification ID
     */
    protected synchronized void sendNotification(Notification notification, int notificationId, Context context) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notificationId, notification);
    }

    protected synchronized int getNotificationId(Context context) {
        // In the highly unlikely event that there are ever more than
        // 1073741823 notifications, start the notification IDs again from 0.
        // IDs 1073741824 and above are reserved for other notifications
        // (such as the permanent summary notification).
        if (lastNotificationId >= (Math.floor(Integer.MAX_VALUE) / 2) - 1) {
            lastNotificationId = 0;
        }

        lastNotificationId++;
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt("_lastNotificationId", lastNotificationId).apply();
        return lastNotificationId;
    }

    // https://developer.android.com/develop/ui/views/notifications/build-notification
    private static void createNotificationChannel(NotificationChannel notificationChannel, Context context) {
        int importance = notificationChannel.isLowPriority() ? android.app.NotificationManager.IMPORTANCE_MIN : android.app.NotificationManager.IMPORTANCE_DEFAULT;
        android.app.NotificationChannel channel = new android.app.NotificationChannel(notificationChannel.getId(), notificationChannel.getName(), importance);
        if (notificationChannel.getDescription() != null) {
            channel.setDescription(notificationChannel.getDescription());
        }

        if (notificationChannel.isLowPriority()) {
            channel.setShowBadge(false);
        }

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        android.app.NotificationManager notificationManager = context.getSystemService(android.app.NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public static void registerAllNotificationChannels(Context context) {
        createNotificationChannel(NotificationChannel.DAILY, context);
        createNotificationChannel(NotificationChannel.EVENT, context);
        createNotificationChannel(NotificationChannel.PERMANENT, context);
    }
}
