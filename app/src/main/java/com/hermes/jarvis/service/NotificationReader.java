package com.hermes.jarvis.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationReader extends NotificationListenerService {

    public static class NotifInfo {
        public String packageName, title, text;
        public long time;
        public Notification.Action replyAction;
        public RemoteInput remoteInput;
    }

    private static final ConcurrentHashMap<String, NotifInfo> recent =
            new ConcurrentHashMap<>();

    @Override
    public void onListenerConnected() {
        StatusBarNotification[] active = getActiveNotifications();
        if (active != null) for (StatusBarNotification n : active) store(n);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        store(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) recent.remove(sbn.getPackageName());
    }

    private void store(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        Notification n = sbn.getNotification();
        String pkg = sbn.getPackageName();
        if (getPackageName().equals(pkg)) return;

        Bundle extras = n.extras;
        NotifInfo info = new NotifInfo();
        info.packageName = pkg;
        info.title = extras.getCharSequence(Notification.EXTRA_TITLE) + "";
        info.text = extras.getCharSequence(Notification.EXTRA_TEXT) + "";
        info.time = sbn.getPostTime();

        Notification.Action[] actions = n.actions;
        if (actions != null) {
            for (Notification.Action a : actions) {
                if (a.getRemoteInputs() != null && a.getRemoteInputs().length > 0) {
                    info.replyAction = a;
                    info.remoteInput = a.getRemoteInputs()[0];
                    break;
                }
            }
        }
        recent.put(pkg, info);
    }

    public static List<NotifInfo> snapshot() {
        return new ArrayList<>(recent.values());
    }

    public static NotifInfo latest(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) {
            NotifInfo best = null;
            for (NotifInfo i : recent.values()) {
                if (best == null || i.time > best.time) best = i;
            }
            return best;
        }
        return recent.get(pkg.trim());
    }

    public static String appName(Context ctx, String pkg) {
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    public static String snapshotText() {
        if (recent.isEmpty())
            return "📭 Tidak ada notifikasi tersimpan.\n(Pastikan izin 'Notification "
                    + "access' aktif di Settings)";
        StringBuilder sb = new StringBuilder("📬 Notifikasi terbaru:\n");
        List<NotifInfo> list = snapshot();
        list.sort((a, b) -> Long.compare(b.time, a.time));
        int i = 0;
        for (NotifInfo n : list) {
            String txt = n.text == null ? "" : n.text;
            sb.append("• [").append(n.packageName).append("] ")
              .append(n.title).append(": ")
              .append(txt, 0, Math.min(80, txt.length()))
              .append(n.replyAction != null ? "  ✉️(bisa dibalas)" : "")
              .append('\n');
            if (++i >= 10) break;
        }
        return sb.toString();
    }

    public static String reply(Context ctx, String packageName, String message) {
        NotifInfo info = latest(packageName);
        if (info == null)
            return "❌ Tidak ada notifikasi aktif dari " + packageName;
        if (info.replyAction == null || info.remoteInput == null)
            return "❌ " + appName(ctx, info.packageName)
                    + " tidak mendukung balasan via notifikasi";
        try {
            Intent i = new Intent();
            Bundle b = new Bundle();
            b.putCharSequence(info.remoteInput.getResultKey(), message);
            RemoteInput.addResultsToIntent(new RemoteInput[]{info.remoteInput}, i, b);
            info.replyAction.actionIntent.send(ctx, 0, i);
            return "✅ Balasan terkirim ke " + appName(ctx, info.packageName)
                    + " (" + info.title + "): \"" + message + "\"";
        } catch (PendingIntent.CanceledException e) {
            return "❌ Gagal mengirim: " + e.getMessage();
        }
    }
}
