package com.hermes.jarvis.automation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.hermes.jarvis.utils.PrefsManager;

public class AutomationEngine {

    public static void schedule(Context ctx, int minutes, String message) {
        PrefsManager prefs = new PrefsManager(ctx);
        int id = prefs.nextAlarmId();
        long triggerAt = System.currentTimeMillis() + (minutes * 60_000L);

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("message", message);
        i.putExtra("id", id);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        }
    }
}
