package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Per-app allowlist for JARVIS UI automation. Disabled by default for safety. */
public final class AppAccessStore {
    private static final String PREF = "jarvis_app_access";
    private static final String KEY = "allowed_packages";
    private final SharedPreferences sp;

    public AppAccessStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean isAllowed(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        return sp.getStringSet(KEY, new HashSet<>()).contains(packageName);
    }

    public void setAllowed(String packageName, boolean allowed) {
        Set<String> set = new HashSet<>(sp.getStringSet(KEY, new HashSet<>()));
        if (allowed) set.add(packageName); else set.remove(packageName);
        sp.edit().putStringSet(KEY, set).apply();
    }

    public Set<String> allAllowed() {
        return new HashSet<>(sp.getStringSet(KEY, new HashSet<>()));
    }
}
