package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;

public final class LogStore {
    private static final String PREF = "jarvis_logs";
    private static final String KEY = "entries";
    private static final int MAX = 12000;
    private LogStore() {}
    public static synchronized void add(Context ctx, String level, String message) {
        SharedPreferences p = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String line = System.currentTimeMillis() + " | " + level + " | " + String.valueOf(message);
        String all = p.getString(KEY, "");
        all = (all == null ? "" : all) + line + "\n";
        if (all.length() > MAX) all = all.substring(all.length() - MAX);
        p.edit().putString(KEY, all).apply();
    }
    public static String read(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "(belum ada log)");
    }
    public static void clear(Context ctx) {
        ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply();
    }
}
