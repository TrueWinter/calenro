package dev.truewinter.calenro;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduledTaskManager {
    private static final int REQUEST_CODE = 621621415;
    private static AlarmManager alarmManager;

    // https://stackoverflow.com/a/24198314
    private static void startAlarmManager(Context context) {
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags);

        Calendar calendar = Calendar.getInstance();
        // 15 minutes is the minimum allowed while the phone is idle
        calendar.add(Calendar.MINUTE, 15);

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }

    public synchronized static void showPermanentNotification(List<Event> events, Context context) {
        android.app.NotificationManager notificationManager = context.getSystemService(android.app.NotificationManager.class);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = sharedPreferences.getBoolean("permanent_notification", false);
        if (!enabled) {
            if (isPermanentNotificationShown(context)) {
                notificationManager.cancel(NotificationManager.PERMANENT_NOTIFICATION_ID);
            }

            return;
        }

        Intent openIntent = new Intent(context, NotificationReceiver.class);
        openIntent.putExtra("notificationType", NotificationManager.NotificationType.PERMANENT_NOTIFICATION);
        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                NotificationManager.PERMANENT_NOTIFICATION_ID, openIntent, flags);

        StringBuilder notificationText = new StringBuilder();

        for (Event e : events) {
            TimeFormat.ReadableTimeFormat readableTimeFormat = TimeFormat.getInstance()
                    .createReadableTime(e.getStart(), e.getEnd());
            notificationText.append(e.getTitle());
            notificationText.append(": ");
            notificationText.append(readableTimeFormat.getStart());
            notificationText.append(" - ");
            notificationText.append(readableTimeFormat.getEnd());
            notificationText.append("\n");
        }

        if (events.size() == 0) {
            notificationText.append(context.getString(R.string.notification_relax_48h));
        }

        // TODO: Fix the setContentIntent error
        @SuppressLint("NotificationTrampoline")
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                NotificationManager.NotificationChannel.PERMANENT.getId())
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle("Schedule until 23:59 tomorrow")
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true);

        NotificationManager.getInstance(context).sendNotification(notification.build(), NotificationManager.PERMANENT_NOTIFICATION_ID, context);
    }

    private static boolean isPermanentNotificationShown(Context context) {
        android.app.NotificationManager notificationManager = context.getSystemService(android.app.NotificationManager.class);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == NotificationManager.PERMANENT_NOTIFICATION_ID) {
                return true;
            }
        }

        return false;
    }

    public static void restartAlarmManager(Context context) {
        if (!hasRunningAlarmManager(context)) {
            PendingIntent oldIntent = getRunningAlarmManager(context);
            alarmManager.cancel(oldIntent);
            oldIntent.cancel();
        }

        startAlarmManager(context);
    }

    public static void startAlarmManagerIfNotStarted(Context context) {
        if (!hasRunningAlarmManager(context)) {
            startAlarmManager(context);
        }
    }

    // https://stackoverflow.com/a/9575569
    public static boolean hasRunningAlarmManager(Context context) {
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return (PendingIntent.getBroadcast(context, REQUEST_CODE,
                new Intent(context, AlarmReceiver.class), flags) != null);
    }

    public static PendingIntent getRunningAlarmManager(Context context) {
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE,
                new Intent(context, AlarmReceiver.class), flags);

    }

    public static boolean hasRunYetToday(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        int calendarYear = calendar.get(Calendar.YEAR);
        // January is 0
        int calendarMonth = calendar.get(Calendar.MONTH) + 1;
        int calendarDay = calendar.get(Calendar.DAY_OF_MONTH);
        int calendarHour = calendar.get(Calendar.HOUR_OF_DAY);

        // YYYY MM DD HH
        // HH will be used for checking against daily notifications that
        // run before the configured time
        String lastRun = sharedPreferences.getString("_lastRun", null);
        if (lastRun == null) return false;
        String[] lastRunArr = lastRun.split(" ");

        int lastRunYear = Integer.parseInt(lastRunArr[0]);
        int lastRunMonth = Integer.parseInt(lastRunArr[1]);
        int lastRunDay = Integer.parseInt(lastRunArr[2]);

        // For future use
        int lastRunHour = Integer.parseInt(lastRunArr[3]);

        if ((calendarYear == lastRunYear) &&
                (calendarMonth == lastRunMonth) &&
                (calendarDay == lastRunDay)) {
            return true;
        }

        return false;
    }

    public static void setHasRunToday(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        int calendarYear = calendar.get(Calendar.YEAR);
        // January is 0
        int calendarMonth = calendar.get(Calendar.MONTH) + 1;
        int calendarDay = calendar.get(Calendar.DAY_OF_MONTH);
        int calendarHour = calendar.get(Calendar.HOUR_OF_DAY);

        String runTime = String.format(Locale.ENGLISH, "%d %d %d %d", calendarYear, calendarMonth, calendarDay, calendarHour);

        // YYYY MM DD HH
        sharedPreferences.edit().putString("_lastRun", runTime).apply();
    }
}
