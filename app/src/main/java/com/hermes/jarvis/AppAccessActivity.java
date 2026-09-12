package com.hermes.jarvis;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.hermes.jarvis.core.AppAccessStore;
import com.hermes.jarvis.service.JarvisAccessibilityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppAccessActivity extends AppCompatActivity {
    private LinearLayout list;
    private AppAccessStore store;
    private PackageManager pm;
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new AppAccessStore(this);
        pm = getPackageManager();
        buildUi();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String s, float size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(0xFFE8EAED);
        t.setPadding(dp(16), dp(8), dp(16), dp(8));
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(24)); root.setBackgroundColor(0xFF0B0F14);

        TextView title = text("J.A.R.V.I.S.  ·  APP ACCESS", 22); title.setTypeface(null, 1);
        root.addView(title);
        TextView sub = text("Kontrol aplikasi yang boleh dioperasikan agent melalui Accessibility. Akses per aplikasi OFF secara default.", 14);
        sub.setTextColor(0xFFAAB2BD); root.addView(sub);

        status = text("", 14); status.setPadding(dp(16),dp(12),dp(16),dp(12)); root.addView(status);
        Button accessibility = new Button(this); accessibility.setText("⚙️ Buka Accessibility Settings");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility, new LinearLayout.LayoutParams(-1, dp(48)));

        Button refresh = new Button(this); refresh.setText("↻ Refresh daftar aplikasi"); refresh.setOnClickListener(v -> populate());
        root.addView(refresh, new LinearLayout.LayoutParams(-1, dp(44)));

        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(0, dp(8), 0, 0);
        root.addView(list, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(root); setContentView(scroll);
        populate();
    }

    private boolean accessibilityEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String target = new ComponentName(this, JarvisAccessibilityService.class).flattenToString();
        for (String s : enabled.split(":")) if (target.equalsIgnoreCase(s)) return true;
        return false;
    }

    private void populate() {
        list.removeAllViews();
        boolean acc = accessibilityEnabled();
        status.setText(acc ? "🟢 UI Control aktif · pilih aplikasi di bawah" : "🟠 UI Control belum aktif · aktifkan Accessibility dulu");
        status.setTextColor(acc ? 0xFF72F1A8 : 0xFFFFC857);

        Intent launcher = new Intent(Intent.ACTION_MAIN); launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> ris = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL);
        Collections.sort(ris, Comparator.comparing(r -> String.valueOf(r.loadLabel(pm)).toLowerCase()));

        String self = getPackageName();
        for (android.content.pm.ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            if (self.equals(pkg)) continue;
            addRow(pkg, ri.loadLabel(pm), ri.loadIcon(pm));
        }
        if (ris.isEmpty()) list.addView(text("Tidak ada aplikasi launcher yang ditemukan.", 14));
    }

    private void addRow(String pkg, CharSequence label, Drawable icon) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(8), dp(8)); row.setBackgroundColor(0xFF131922);
        if (icon != null) { icon.setBounds(0,0,dp(42),dp(42)); TextView spacer = new TextView(this); spacer.setCompoundDrawables(icon,null,null,null); row.addView(spacer, new LinearLayout.LayoutParams(dp(52),dp(52))); }

        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL); labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(String.valueOf(label), 16); name.setPadding(0,0,0,0);
        TextView id = text(pkg, 11); id.setTextColor(0xFF7F8A99); id.setPadding(0,dp(3),0,0);
        labels.addView(name); labels.addView(id);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(62), 1f); row.addView(labels, lp);

        SwitchCompat sw = new SwitchCompat(this); sw.setText("Akses"); sw.setTextColor(0xFFDDE3EA); sw.setChecked(store.isAllowed(pkg));
        sw.setOnCheckedChangeListener((button, checked) -> store.setAllowed(pkg, checked));
        row.addView(sw, new LinearLayout.LayoutParams(dp(92), dp(56)));
        row.setOnClickListener(v -> sw.setChecked(!sw.isChecked()));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(70)); rp.setMargins(0,0,0,dp(6)); list.addView(row,rp);
    }

    @Override protected void onResume() { super.onResume(); if (list != null) populate(); }
}
