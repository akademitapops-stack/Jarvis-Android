package com.hermes.jarvis.automation;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.hermes.jarvis.JarvisApp;
import com.hermes.jarvis.MainActivity;
import com.hermes.jarvis.ai.WeatherTool;
import com.hermes.jarvis.core.ContextEngine;
import com.hermes.jarvis.service.NotificationReader;

public class ReportReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                notifyUser(ctx, buildReport(ctx));
            } catch (Exception ignored) {
            } finally {
                pending.finish();
            }
        }).start();
    }

    private String buildReport(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌅 LAPORAN JARVIS\n");
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append(ContextEngine.build(ctx)).append('\n');
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append(WeatherTool.fetchSync(ctx, "")).append('\n');
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append("💾 Storage:\n")
          .append(com.hermes.jarvis.core.TerminalExecutor.sync("df -h /sdcard | tail -1"))
          .append('\n');
        sb.append("━━━━━━━━━━━━━━━━━━\n");
        sb.append(NotificationReader.snapshotText());
        return sb.toString();
    }

    private void notifyUser(Context ctx, String text) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent open = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 9000, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        nm.notify(9000, new NotificationCompat.Builder(ctx, JarvisApp.CHANNEL_AUTO)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🌅 Laporan Jarvis")
                .setContentText(text.length() > 80 ? text.substring(0, 80) + "…" : text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build());
    }
}
