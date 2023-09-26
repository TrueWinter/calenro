package dev.truewinter.calenro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

public class Permissions {
    public static final int READ_CALENDAR = 1;
    public static final int POST_NOTIFICATIONS = 2;

    public static boolean hasCalendarPermissions(Activity activity) {
        return activity.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestCalendarPermissions(Activity activity) {
        if (!hasCalendarPermissions(activity)) {
            activity.requestPermissions(new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR}, Permissions.READ_CALENDAR);
        }
    }

    public static boolean hasNotificationPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static void requestNotificationPermissions(Activity activity) {
        if (!hasNotificationPermissions(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, Permissions.POST_NOTIFICATIONS);
        }
    }
}
