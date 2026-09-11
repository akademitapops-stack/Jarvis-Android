package com.hermes.jarvis.automation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

public class DailyReport {

    private static final int REQ = 8900;
    private static final String KEY = "daily_report";
    private static final String PREF = "jarvis_auto";

    public static void schedule(Context c, int hour, int minute) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        hour = clamp(hour, 0, 23);
        minute = clamp(minute, 0, 59);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);

        if (Build.VERSION.SDK_INT >= 23)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi(c));
        else
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi(c));

        prefs(c).edit().putBoolean(KEY, true)
                .putInt(KEY + "_h", hour).putInt(KEY + "_m", minute).apply();
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c));
        prefs(c).edit().putBoolean(KEY, false).apply();
    }

    public static boolean enabled(Context c) {
        return prefs(c).getBoolean(KEY, false);
    }

    public static String timeString(Context c) {
        return String.format(Locale.US, "%02d:%02d",
                prefs(c).getInt(KEY + "_h", 7), prefs(c).getInt(KEY + "_m", 0));
    }

    public static void rescheduleIfEnabled(Context c) {
        if (enabled(c))
            schedule(c, prefs(c).getInt(KEY + "_h", 7), prefs(c).getInt(KEY + "_m", 0));
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static PendingIntent pi(Context c) {
        Intent i = new Intent(c, ReportReceiver.class);
        return PendingIntent.getBroadcast(c, REQ, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
