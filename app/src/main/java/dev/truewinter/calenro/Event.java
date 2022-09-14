package dev.truewinter.calenro;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;

import org.json.JSONException;
import org.json.JSONObject;

public class Event implements Comparable<Event> {
    private long id;
    private String title;
    private boolean allDay = false;
    private long start;
    private long end;
    private boolean notified = false;

    public Event(long id, String title, boolean allDay) {
        this.id = id;
        this.title = title;
        this.allDay = allDay;
    }

    public Event(long id, String title, long start, long end) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public boolean hasNotified() {
        return notified;
    }

    public Event setNotifiedInline(boolean notified) {
        this.notified = notified;
        return this;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("title", title);
        j.put("allDay", allDay);
        if (!allDay) {
            j.put("start", start);
            j.put("end", end);
        }
        j.put("notified", notified);
        return j;
    }

    @Override
    public int compareTo(Event otherEvent) {
        if (this.allDay) {
            return 1;
        }

        return Long.compare(start, otherEvent.getEnd());
    }

    public static void openEventInCalendarApp(Context context, Event event) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setData(uri);
        intent.putExtra("beginTime", event.getStart());
        intent.putExtra("endTime", event.getEnd());
        context.startActivity(intent);
    }
}
