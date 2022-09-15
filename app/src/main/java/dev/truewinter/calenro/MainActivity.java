package dev.truewinter.calenro;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.truewinter.calenro.adapters.EventListAdapter;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, EventListAdapter.ItemClickListener {
    private static final String[] EVENT_DATA = new String[] {
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
    };

    private static final int EVENT_ID = 0;
    private static final int EVENT_TITLE = 1;
    private static final int EVENT_ALL_DAY = 2;
    private static final int EVENT_BEGIN = 3;
    private static final int EVENT_END = 4;

    private SwipeRefreshLayout swipeRefreshLayout = null;
    private static EventListAdapter eventListAdapter = null;
    private static List<Event> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.to_do);
        setSupportActionBar(toolbar);

        if (BuildConfig.DEBUG) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        NotificationManager.registerAllNotificationChannels(getApplicationContext());

        TextView errorHome = findViewById(R.id.error_home);
        errorHome.setVisibility(View.GONE);

        RecyclerView recyclerView = findViewById(R.id.event_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefreshLayout = findViewById(R.id.event_list_container);
        swipeRefreshLayout.setOnRefreshListener(this);
        eventListAdapter = new EventListAdapter(this, events);
        eventListAdapter.setClickListener(this);
        recyclerView.setAdapter(eventListAdapter);

        updateCalendarDataUI();

        String dailyNotificationTime = PreferenceManager.getDefaultSharedPreferences(this).getString("daily_notification_time", null);
        if (dailyNotificationTime == null || dailyNotificationTime.equals("")) {
            Toast.makeText(this, getResources().getText(R.string.toast_init_config),Toast.LENGTH_LONG).show();
            showSettings(true);
        } else {
            ScheduledTaskManager.startAlarmManagerIfNotStarted(getApplicationContext());
            ScheduledTaskManager.showPermanentNotification(events, getApplicationContext());
        }

        try {
            JSONObject j = new JSONObject();
            j.put("test", "testing");
            RemoteLogger.log(j, getApplicationContext());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Permissions.hasCalendarPermissions(this)) {
            updateCalendarDataUI();
        }
    }

    public static synchronized List<Event> getEvents() {
        return events;
    }

    protected synchronized void updateCalendarDataUI() {
        findViewById(R.id.nothing_scheduled).setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);

        if (!Permissions.hasCalendarPermissions(this)) {
            swipeRefreshLayout.setRefreshing(false);
            findViewById(R.id.no_permission).setVisibility(View.VISIBLE);
            Permissions.requestCalendarPermissions(this);
            return;
        }

        updateCalendarData(getApplicationContext());
        eventListAdapter.notifyDataSetChanged();

        swipeRefreshLayout.setRefreshing(false);

        if (events.size() == 0) {
            findViewById(R.id.nothing_scheduled).setVisibility(View.VISIBLE);
        }
    }

    protected static synchronized void updateCalendarData(Context context) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Whole of today, and whole of tomorrow
        LocalDate today = LocalDate.now();
        // Today 00:00
        LocalDateTime todayMidnight = LocalDateTime.of(today, LocalTime.MIDNIGHT);
        // Tomorrow 23:59
        LocalDateTime afterTomorrowMidnight = todayMidnight.plusDays(2).minusMinutes(1);

        ContentResolver crEv = context.getContentResolver();
        Uri.Builder uriEv = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriEv, todayMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        ContentUris.appendId(uriEv, afterTomorrowMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        Cursor curEv = crEv.query(uriEv.build(), EVENT_DATA, null, null);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String persistedEventsString = sharedPreferences.getString("_persistedEvents", "{}");
        JSONObject persistedEvents = new JSONObject();

        try {
            // Check if null just to silence false null warning
            persistedEvents = new JSONObject(persistedEventsString == null ? "{}" : persistedEventsString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        events.clear();

        if (curEv == null) return;
        while (curEv.moveToNext()) {
            long instId;
            String instTitle;
            int instAllDay;
            long instStart = 0;
            long instEnd = 0;

            instId = curEv.getLong(EVENT_ID);
            instTitle = curEv.getString(EVENT_TITLE);
            instAllDay = curEv.getInt(EVENT_ALL_DAY);

            if (instAllDay == 0) {
                instStart = curEv.getLong(EVENT_BEGIN);
                instEnd = curEv.getLong(EVENT_END);
            }

            if (instEnd < System.currentTimeMillis()) {
                continue;
            }

            boolean hasNotified = false;
            if (persistedEvents.has(Long.toString(instId))) {
                try {
                    JSONObject persistedEvent = persistedEvents.getJSONObject(Long.toString(instId));
                    if (persistedEvent.getBoolean("notified")) {
                        hasNotified = true;
                    }


                    // Event moved into the future
                    if (instStart > persistedEvent.getLong("start") && instStart > System.currentTimeMillis()) {
                        hasNotified = false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (instAllDay == 0) {
                events.add(new Event(
                        instId,
                        instTitle,
                        instStart,
                        instEnd
                ).setNotifiedInline(hasNotified));
            } else {
                events.add(new Event(
                        instId,
                        instTitle,
                        true
                ).setNotifiedInline(hasNotified));
            }
        }

        curEv.close();

        Collections.sort(events);
    }

    private void showSettings(boolean hideBackButton) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        settingsIntent.putExtra("hideBackButton", hideBackButton);
        startActivity(settingsIntent);
    }

    private void showAbout() {
        Intent aboutIntent = new Intent(this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Handle crash caused by edge case
        if (grantResults.length == 0) return;
        switch (requestCode) {
            case Permissions.READ_CALENDAR: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findViewById(R.id.no_permission).setVisibility(View.GONE);
                    updateCalendarDataUI();
                } else {
                    findViewById(R.id.no_permission).setVisibility(View.VISIBLE);
                    findViewById(R.id.nothing_scheduled).setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettings(false);
                break;
            case R.id.action_about:
                showAbout();
                break;
        }

        return true;
    }

    @Override
    public void onRefresh() {
        updateCalendarDataUI();
    }

    @Override
    public void onItemClick(View view, int position) {
        Event event = eventListAdapter.getIdFromIndex(position);
        if (event == null) return;
        Event.openEventInCalendarApp(getApplicationContext(), event);
    }

    @Override
    public void onLongClick(View view, int position) {
        // no-op
    }
}
