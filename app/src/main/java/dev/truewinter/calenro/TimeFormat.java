package dev.truewinter.calenro;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class TimeFormat {
    public static DateTimeFormatter formatterWithDay = DateTimeFormatter.ofPattern("(dd-MMM-yyyy) HH:mm");
    public static DateTimeFormatter formatterWithDayNoParen = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    public static DateTimeFormatter formatterWithoutDay = DateTimeFormatter.ofPattern("HH:mm");

    private static TimeFormat instance = null;
    private TimeFormat() {}

    public static TimeFormat getInstance() {
        if (instance == null) {
            instance = new TimeFormat();
        }

        return instance;
    }

    public ReadableTimeFormat createReadableTime(long start, long end) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(System.currentTimeMillis());

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTimeInMillis(start);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(end);

        // Always show the start date
        String startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(start),
                ZoneId.systemDefault()).format(TimeFormat.formatterWithDay);
        String endTime;
        LocalDateTime endTimeLocal = LocalDateTime.ofInstant(Instant.ofEpochMilli(end),
                ZoneId.systemDefault());

        if (startCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH) + 1) {
            // Tomorrow
            startTime = "Tomorrow " + startTime;
        } else if (startCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH)) {
            // Today
            startTime = "Today " + startTime;
        } else {
            startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(start),
                    ZoneId.systemDefault()).format(TimeFormat.formatterWithDayNoParen);
        }

        if (endCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH) + 1) {
            // Tomorrow
            endTime = "Tomorrow " + endTimeLocal.format(TimeFormat.formatterWithoutDay);
        } else if (endCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH)) {
            // Today
            endTime = endTimeLocal.format(TimeFormat.formatterWithoutDay);
        } else {
            endTime = endTimeLocal.format(TimeFormat.formatterWithDayNoParen);
        }

        return new ReadableTimeFormat(startTime, endTime);
    }

    public class ReadableTimeFormat {
        private String start;
        private String end;

        private ReadableTimeFormat(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }
    }
}
