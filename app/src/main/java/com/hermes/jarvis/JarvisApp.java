package com.hermes.jarvis;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class JarvisApp extends Application {

    public static final String CHANNEL_ALARMS = "jarvis_alarms";
    public static final String CHANNEL_AUTO = "jarvis_automation";

    @Override
    public void onCreate() {
        super.onCreate();
        com.hermes.jarvis.core.CrashHandler.install(this);
        com.hermes.jarvis.core.LogStore.add(this, "INFO", "JARVIS process started");
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            NotificationChannel a = new NotificationChannel(CHANNEL_ALARMS,
                    "Jarvis Reminders", NotificationManager.IMPORTANCE_HIGH);
            a.setDescription("Pengingat terjadwal");
            nm.createNotificationChannel(a);

            NotificationChannel b = new NotificationChannel(CHANNEL_AUTO,
                    "Jarvis Automations", NotificationManager.IMPORTANCE_DEFAULT);
            b.setDescription("Hasil automasi terjadwal");
            nm.createNotificationChannel(b);
        }
    }
}
