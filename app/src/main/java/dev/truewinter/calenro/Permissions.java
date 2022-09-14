package dev.truewinter.calenro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

public class Permissions {
    public static final int READ_CALENDAR = 1;

    public static boolean hasCalendarPermissions(Activity activity) {
        return activity.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestCalendarPermissions(Activity activity) {
        if (!hasCalendarPermissions(activity)) {
            activity.requestPermissions(new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, Permissions.READ_CALENDAR);
        }
    }
}
