package com.hermes.jarvis.automation;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.hermes.jarvis.JarvisApp;
import com.hermes.jarvis.MainActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AutomationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int id = intent.getIntExtra("auto_id", -1);
        SmartAutomation.Rule r = SmartAutomation.byId(ctx, id);
        if (r == null) return;

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String cmd : r.commands) {
            if (++count > 3) break;
            try {
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
                pb.redirectErrorStream(true);
                pb.environment().put("PATH",
                        "/system/bin:/system/xbin:/sbin:/vendor/bin:/data/local/bin");
                Process p = pb.start();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String l; int n = 0;
                while ((l = br.readLine()) != null) {
                    sb.append(l).append('\n');
                    if (++n > 25) break;
                }
                br.close();
                p.waitFor();
                p.destroy();
            } catch (Exception e) {
                sb.append("ERR: ").append(e.getMessage()).append('\n');
            }
        }

        String body = sb.toString().trim();
        if (body.isEmpty()) body = "(tidak ada output)";
        notifyUser(ctx, r.name, r.message + "\n\n" + body, id);
    }

    private void notifyUser(Context ctx, String title, String text, int id) {
        try {
            NotificationManager nm = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent open = new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(ctx, id, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder b = new NotificationCompat.Builder(
                    ctx, JarvisApp.CHANNEL_AUTO)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("⏰ " + title)
                    .setContentText(text.length() > 60
                            ? text.substring(0, 60) + "…" : text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            nm.notify(5000 + id, b.build());
        } catch (Exception e) {
            Toast.makeText(ctx, "Automasi error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
