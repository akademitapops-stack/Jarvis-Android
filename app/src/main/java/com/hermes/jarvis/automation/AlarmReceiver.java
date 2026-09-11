package com.hermes.jarvis.automation;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.hermes.jarvis.JarvisApp;
import com.hermes.jarvis.MainActivity;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String msg = intent.getStringExtra("message");
        if (msg == null) msg = "Pengingat Jarvis";

        Intent open = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx,
                intent.getIntExtra("id", 0), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(
                ctx, JarvisApp.CHANNEL_ALARMS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🤖 Jarvis Reminder")
                .setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(intent.getIntExtra("id", 0), b.build());
    }
}
