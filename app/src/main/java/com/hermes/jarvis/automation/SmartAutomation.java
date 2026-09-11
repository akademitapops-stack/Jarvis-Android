package com.hermes.jarvis.automation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

public class SmartAutomation {

    public static class Rule {
        public int id;
        public String name = "Automasi";
        public String type = "daily";
        public String time = "07:00";
        public int intervalMin = 60;
        public List<String> commands = new ArrayList<>();
        public String message = "Laporan Jarvis";
    }

    private static final Gson G = new Gson();
    private static final String KEY = "auto_rules";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("jarvis_auto", Context.MODE_PRIVATE);
    }

    private static List<Rule> load(Context c) {
        try {
            Rule[] arr = G.fromJson(p(c).getString(KEY, "[]"), Rule[].class);
            return new ArrayList<>(Arrays.asList(arr != null ? arr : new Rule[0]));
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private static void save(Context c, List<Rule> list) {
        p(c).edit().putString(KEY, G.toJson(list)).apply();
    }

    public static List<Rule> list(Context c) { return load(c); }

    public static Rule byId(Context c, int id) {
        for (Rule r : load(c)) if (r.id == id) return r;
        return null;
    }

    public static String add(Context c, Rule r) {
        List<Rule> l = load(c);
        int nid = 1;
        for (Rule x : l) nid = Math.max(nid, x.id + 1);
        r.id = nid;
        l.add(r);
        save(c, l);
        schedule(c, r);
        return "⏰ Automasi \"" + r.name + "\" AKTIF — " + describe(r)
                + "\nPerintah: " + String.join("; ", r.commands);
    }

    public static String remove(Context c, String nameOrId) {
        List<Rule> l = load(c);
        Iterator<Rule> it = l.iterator();
        while (it.hasNext()) {
            Rule r = it.next();
            if (r.name.equalsIgnoreCase(nameOrId.trim())
                    || String.valueOf(r.id).equals(nameOrId.trim())) {
                cancel(c, r);
                it.remove();
                save(c, l);
                return "🗑️ Automasi \"" + r.name + "\" dihapus";
            }
        }
        return "❌ Automasi \"" + nameOrId + "\" tidak ditemukan (buka menu ⏰ untuk daftar)";
    }

    public static String describe(Rule r) {
        return "interval".equals(r.type)
                ? ("setiap " + r.intervalMin + " menit")
                : ("setiap hari jam " + r.time);
    }

    public static void schedule(Context c, Rule r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long interval, first;
        if ("interval".equals(r.type)) {
            interval = Math.max(1, r.intervalMin) * 60_000L;
            first = System.currentTimeMillis() + interval;
        } else {
            int hour = 7, min = 0;
            try {
                String[] hhmm = r.time.split(":");
                hour = Integer.parseInt(hhmm[0].trim());
                if (hhmm.length > 1) min = Integer.parseInt(hhmm[1].trim());
            } catch (Exception ignored) {}
            hour = Math.max(0, Math.min(23, hour));
            min = Math.max(0, Math.min(59, min));
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis())
                cal.add(Calendar.DAY_OF_YEAR, 1);
            interval = AlarmManager.INTERVAL_DAY;
            first = cal.getTimeInMillis();
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, first, interval, pi(c, r.id));
    }

    public static void cancel(Context c, Rule r) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c, r.id));
    }

    public static void rescheduleAll(Context c) {
        for (Rule r : load(c)) schedule(c, r);
    }

    private static PendingIntent pi(Context c, int id) {
        Intent i = new Intent(c, AutomationReceiver.class);
        i.putExtra("auto_id", id);
        return PendingIntent.getBroadcast(c, 10_000 + id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
