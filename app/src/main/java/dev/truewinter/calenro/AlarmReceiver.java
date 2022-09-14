package dev.truewinter.calenro;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
        Log.d(getClass().getSimpleName(), "receive: " + LocalDateTime.ofInstant(
                Instant.ofEpochMilli(System.currentTimeMillis()),
                ZoneId.systemDefault()).format(dateTimeFormatter)
        );

        ScheduledTaskManager.restartAlarmManager(context);

        MainActivity.updateCalendarData(context);
        List<Event> events = MainActivity.getEvents();

        dailyNotification(events, context);
        eventNotifications(events, context);
        ScheduledTaskManager.showPermanentNotification(events, context);

        // Persist the events to avoid multiple notifications
        try {
            JSONObject eventsToPersist = new JSONObject();

            for (Event e : events) {
                eventsToPersist.put(Long.toString(e.getId()), e.toJSON());
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            sharedPreferences.edit().putString("_persistedEvents", eventsToPersist.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized void dailyNotification(List<Event> events, Context context) {
        if (ScheduledTaskManager.hasRunYetToday(context)) return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String dailyNotificationTime = sharedPreferences.getString("daily_notification_time", null);
        if (dailyNotificationTime == null) return;

        int dailyNotificationHour = Integer.parseInt(dailyNotificationTime.split(" ")[0]);
        int dailyNotificationMinute = Integer.parseInt(dailyNotificationTime.split(" ")[1]);

        Calendar dailyNotificationCalendar = Calendar.getInstance();
        dailyNotificationCalendar.set(Calendar.HOUR_OF_DAY, dailyNotificationHour);
        dailyNotificationCalendar.set(Calendar.MINUTE, dailyNotificationMinute);
        long dailyNotificationTimeMS = dailyNotificationCalendar.getTimeInMillis();

        Calendar next16Hours = Calendar.getInstance();
        next16Hours.add(Calendar.HOUR_OF_DAY, 16);

        List<Event> next16HoursEvents = new ArrayList<>();
        for (Event e : events) {
            if (e.getStart() <= next16Hours.getTimeInMillis()) {
                next16HoursEvents.add(e);
            }
        }

        // If the notification is less than 15 minutes in the future, show it now
        if (dailyNotificationTimeMS - System.currentTimeMillis() <= 15 * 60 * 1000 ||
                // If the notification time is in the present or past, show it too
                System.currentTimeMillis() >= dailyNotificationTimeMS) {
            showDailyNotification(next16HoursEvents, context);
        }
    }

    private void showDailyNotification(List<Event> eventsList, Context context) {
        int reservedNotificationId = NotificationManager.getInstance(context).getNotificationId(context);

        Intent openIntent = new Intent(context, NotificationReceiver.class);
        openIntent.putExtra("notificationType", NotificationManager.NotificationType.DAILY_NOTIFICATION);
        PendingIntent openPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                getRandom(), openIntent, 0);

        StringBuilder notificationText = new StringBuilder();

        if (eventsList.size() >= 4) {
            notificationText.append(context.getString(R.string.notification_busy_day));
        } else if (eventsList.size() >= 2) {
            notificationText.append(context.getString(R.string.notification_few_events));
        } else if (eventsList.size() == 1) {
            notificationText.append(context.getString(R.string.notification_one_event));
        } else {
            notificationText.append(context.getString(R.string.notification_relax_today));
        }

        notificationText.append("\n");

        for (Event e : eventsList) {
            String startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getStart()),
                    ZoneId.systemDefault()).format(TimeFormat.formatterWithoutDay);
            String endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getEnd()),
                    ZoneId.systemDefault()).format(TimeFormat.formatterWithoutDay);

            notificationText.append(e.getTitle());
            notificationText.append(": ");
            notificationText.append(startTime);
            notificationText.append(" - ");
            notificationText.append(endTime);
            notificationText.append("\n");
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                NotificationManager.NotificationChannel.DAILY.getId())
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle("Schedule for next 16 hours")
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openPendingIntent)
                .setAutoCancel(true);

        NotificationManager.getInstance(context).sendNotification(notification.build(), reservedNotificationId, context);
        ScheduledTaskManager.setHasRunToday(context);
    }

    private synchronized void eventNotifications(List<Event> events, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean shouldNotifyAtEventStart = sharedPreferences.getBoolean("event_start_notification", false);
        if (!shouldNotifyAtEventStart) return;

        for (Event e : events) {
            if (e.hasNotified()) continue;
            // If the event is less than 15 minutes in the future, show it now
            if (e.getStart() - System.currentTimeMillis() <= 15 * 60 * 1000 ||
                    // If the event start time is in the present or past, but the event has not yet ended, show it too
                    (System.currentTimeMillis() >= e.getStart() && System.currentTimeMillis() < e.getEnd())) {
                int reservedNotificationId = NotificationManager.getInstance(context).getNotificationId(context);

                Intent openIntent = new Intent(context, NotificationReceiver.class);
                openIntent.putExtra("notificationType", NotificationManager.NotificationType.EVENT_NOTIFICATION);
                openIntent.putExtra("eventId", e.getId());
                PendingIntent openPendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                        getRandom(), openIntent, 0);

                TimeFormat.ReadableTimeFormat readableTimeFormat = TimeFormat.getInstance()
                        .createReadableTime(e.getStart(), e.getEnd());

                String notificationText = readableTimeFormat.getStart() + " - " + readableTimeFormat.getEnd();

                NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                        NotificationManager.NotificationChannel.EVENT.getId())
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentTitle(e.getTitle())
                        .setContentText(notificationText)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationText))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(openPendingIntent)
                        .setAutoCancel(true);

                NotificationManager.getInstance(context).sendNotification(notification.build(), reservedNotificationId, context);
                e.setNotified(true);
            }
        }
    }

    private int getRandom() {
        return (int) Math.floor(Math.random() * Integer.MAX_VALUE);
    }
}
