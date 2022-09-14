package dev.truewinter.calenro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Optional;

// https://stackoverflow.com/a/20670984
public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public synchronized void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        NotificationManager.NotificationType notificationType = (NotificationManager.NotificationType) extras.get("notificationType");
        if (notificationType == null) return;

        switch (notificationType) {
            case DAILY_NOTIFICATION:
            case PERMANENT_NOTIFICATION:
                Intent intent1 = new Intent(context, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent1);
                break;
            case EVENT_NOTIFICATION:
                long eventId = extras.getLong("eventId");
                Optional<Event> event = MainActivity.getEvents().stream().filter(e -> e.getId() == eventId)
                        .findAny();
                event.ifPresent(e -> Event.openEventInCalendarApp(context, e));
                break;
        }
    }
}
