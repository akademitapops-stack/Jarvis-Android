package com.hermes.jarvis.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContextEngine {

    public static String build(Context ctx) {
        StringBuilder sb = new StringBuilder();

        String now = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm",
                new Locale("id", "ID")).format(new Date());
        sb.append("Waktu: ").append(now).append("\n");

        Intent bat = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat != null) {
            int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            if (level >= 0) {
                int pct = level * 100 / Math.max(scale, 1);
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
                sb.append("Baterai: ").append(pct).append("%")
                  .append(charging ? " (sedang charging)" : "").append("\n");
            }
        }

        try {
            ConnectivityManager cm = (ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities caps = cm != null
                    ? cm.getNetworkCapabilities(cm.getActiveNetwork()) : null;
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    sb.append("Jaringan: WiFi\n");
                else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    sb.append("Jaringan: Data Seluler\n");
            } else {
                sb.append("Jaringan: OFFLINE\n");
            }
        } catch (Exception ignored) {}

        sb.append("Device: ").append(Build.MANUFACTURER).append(" ")
          .append(Build.MODEL).append(" (Android ").append(Build.VERSION.RELEASE).append(")\n");
        sb.append("Root: ").append(TerminalExecutor.get().hasRoot() ? "YA" : "TIDAK").append("\n");
        sb.append("CWD: ").append(TerminalExecutor.get().cwd());

        return sb.toString();
    }
}
