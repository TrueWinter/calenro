package dev.truewinter.calenro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    protected static boolean hideBackButton = false;
    protected static ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            hideBackButton = getIntent().getBooleanExtra("hideBackButton", false);
            actionBar.setDisplayHomeAsUpEnabled(!hideBackButton);

            // hideBackButton should only ever be true if the app has not yet been configured
            if (!hideBackButton) {
                ScheduledTaskManager.startAlarmManagerIfNotStarted(getApplicationContext());
            }
        }

        if (!Permissions.hasCalendarPermissions(this)) {
            Permissions.requestCalendarPermissions(this);
        }
    }

    // If the user has not yet configured the app, don't allow them to access anything other than the settings
    @Override
    public void onBackPressed() {
        if (!hideBackButton) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, getResources().getText(R.string.toast_init_config),Toast.LENGTH_LONG).show();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements TimePickerDialog.OnTimeSetListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Activity activity = getActivity();
            if (activity == null) return;

            Preference dailyNotificationTime = findPreference("daily_notification_time");
            if (dailyNotificationTime == null) return;

            updateDailyNotificationSummary();

            dailyNotificationTime.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    showTimePicker();
                    return false;
                }
            });

            Preference betaNotice = findPreference("pref_static_field_key_1");
            if (betaNotice == null) return;

            betaNotice.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Report a bug");
                    builder.setMessage("To report a bug, either send an email to TrueWinter support or file an issue on GitHub");
                    builder.setPositiveButton("Copy Support Email", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("TrueWinter Support Email", "support@truewinter.dev");
                            clipboardManager.setPrimaryClip(clip);

                            // Android 13+ (API level 33) includes visual feedback for copied content
                            if (Build.VERSION.SDK_INT <= 32) {
                                Toast.makeText(getContext(), "Copied TrueWinter support email to clipboard", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    builder.setNeutralButton("Open GitHub", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://github.com/TrueWinter/calenro"));
                            startActivity(intent);
                        }
                    });

                    builder.show();

                    return false;
                }
            });

            Preference permanentNotification = findPreference("permanent_notification");
            if (permanentNotification == null) return;

            permanentNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    ScheduledTaskManager.showPermanentNotification(MainActivity.getEvents(), activity);
                    return false;
                }
            });
        }

        private void showTimePicker() {
            // Default to 8 AM
            int hour = 8;
            int min = 0;

            if (notificationTimeIsSet()) {
                SharedPreferences pref = getPreferenceManager().getSharedPreferences();
                if (pref == null) return;
                String setTime = pref.getString("daily_notification_time", null);

                // notificationTimeIsSet() checks if it is null, no need to check here
                hour = Integer.parseInt(setTime.split(" ")[0]);
                min = Integer.parseInt(setTime.split(" ")[1]);
            }

            new TimePickerDialog(getContext(), this, hour, min, false).show();
        }

        private boolean notificationTimeIsSet() {
            SharedPreferences pref = getPreferenceManager().getSharedPreferences();
            if (pref == null) return false;
            String setTime = pref.getString("daily_notification_time", null);

            if (setTime == null || setTime.equals("")) {
                return false;
            } else {
                return true;
            }
        }

        private void updateDailyNotificationSummary() {
            Preference dailyNotificationTime = findPreference("daily_notification_time");
            if (dailyNotificationTime == null) return;

            String dailyNotificationTimeInfo = getString(R.string.daily_notification_time_summary);
            String dailyNotificationTimeInfoDefault = getString(R.string.daily_notification_time_summary_default_time);

            SharedPreferences pref = dailyNotificationTime.getSharedPreferences();
            if (pref == null) return;
            String setTime = pref.getString("daily_notification_time", null);
            if (!notificationTimeIsSet()) {
                dailyNotificationTimeInfo = dailyNotificationTimeInfo.replace("{{time}}", dailyNotificationTimeInfoDefault);
            } else {
                String parsedTime = "";

                // Need to compare hour, so using int here
                int setTimeHour = Integer.parseInt(setTime.split(" ")[0]);
                String setTimeMin = setTime.split(" ")[1];
                String am_pm = "AM";

                if (setTimeHour >= 12) {
                    am_pm = "PM";

                    // Convert to 12 hour time
                    if (setTimeHour >= 13) {
                        setTimeHour = setTimeHour - 12;
                    }
                }

                if (setTimeMin.length() == 1) {
                    setTimeMin = "0" + setTimeMin;
                }

                parsedTime = setTimeHour + ":" + setTimeMin + " " + am_pm;
                dailyNotificationTimeInfo = dailyNotificationTimeInfo.replace("{{time}}", parsedTime);
            }
            dailyNotificationTime.setSummary(dailyNotificationTimeInfo);
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int i, int i1) {
            String time = timePicker.getHour() + " " + timePicker.getMinute();

            SharedPreferences pref = getPreferenceManager().getSharedPreferences();
            if (pref == null) return;
            pref.edit().putString("daily_notification_time", time).apply();

            updateDailyNotificationSummary();
            hideBackButton = false;
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Make Android Studio happy
            if (getActivity() == null) return;
            ScheduledTaskManager.startAlarmManagerIfNotStarted(getActivity().getApplicationContext());
        }
    }
}