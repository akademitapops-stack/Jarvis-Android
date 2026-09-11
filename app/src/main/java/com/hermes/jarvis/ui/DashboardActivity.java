package com.hermes.jarvis.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hermes.jarvis.R;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.automation.DailyReport;
import com.hermes.jarvis.automation.SmartAutomation;
import com.hermes.jarvis.core.BiometricGate;
import com.hermes.jarvis.core.MemoryBank;
import com.hermes.jarvis.core.StatsTracker;
import com.hermes.jarvis.core.TerminalExecutor;
import com.hermes.jarvis.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvContent;
    private MemoryBank memory;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = new PrefsManager(this);
        memory = new MemoryBank(this);
        tvContent = findViewById(R.id.tvContent);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnManageMem).setOnClickListener(v -> manageMemory());

        findViewById(R.id.btnStats).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage("Reset semua statistik?")
                        .setPositiveButton("Reset", (d, w) -> {
                            new StatsTracker(this).reset();
                            refresh();
                        })
                        .setNegativeButton("Batal", null).show());

        if (prefs.biometricLock() && BiometricGate.available(this)) {
            BiometricGate.authenticate(this, "📊 Buka Dashboard",
                    new BiometricGate.Result() {
                        @Override public void onSuccess() { refresh(); }
                        @Override public void onFailed(String r) {
                            Toast.makeText(DashboardActivity.this,
                                    "Terkunci: " + r, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            refresh();
        }
    }

    private void manageMemory() {
        Map<String, String> entries = memory.entries();
        if (entries.isEmpty()) {
            Toast.makeText(this, "🧠 Memori kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> keys = new ArrayList<>(entries.keySet());
        Collections.sort(keys);
        String[] items = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++)
            items[i] = keys.get(i) + " = " + entries.get(keys.get(i));

        new AlertDialog.Builder(this)
                .setTitle("🧠 Memori — tap untuk hapus")
                .setItems(items, (d, w) -> {
                    memory.forget(keys.get(w));
                    Toast.makeText(this, "🗑️ \"" + keys.get(w) + "\" dihapus",
                            Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton("🗑️ Hapus Semua", (d, w) -> {
                    memory.clearAll();
                    refresh();
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();

        sb.append("🤖 SYSTEM\n");
        sb.append("Model: ").append(prefs.model()).append('\n');
        sb.append("API: ").append(
                new UniversalProvider(prefs).isConfigured()
                        ? "✅ terkonfigurasi" : "❌ belum diisi").append('\n');
        sb.append("Root: ").append(
                TerminalExecutor.get().hasRoot() ? "✅ tersedia" : "❌ tidak").append('\n');
        sb.append("Kunci biometrik: ").append(
                prefs.biometricLock() ? "🔐 AKTIF" : "nonaktif").append("\n\n");

        sb.append("📊 STATISTIK\n").append(new StatsTracker(this).summary()).append("\n\n");

        sb.append("🌅 LAPORAN HARIAN\nStatus: ");
        if (DailyReport.enabled(this))
            sb.append("✅ Aktif setiap jam ").append(DailyReport.timeString(this))
              .append("\n(dapat dimatikan: \"matikan laporan\")");
        else
            sb.append("❌ Nonaktif (aktifkan: \"laporan pagi jam 7\")");
        sb.append("\n\n");

        sb.append("⏰ AUTOMASI (").append(SmartAutomation.list(this).size()).append(")\n");
        List<SmartAutomation.Rule> autos = SmartAutomation.list(this);
        if (autos.isEmpty()) sb.append("(kosong)\n");
        for (SmartAutomation.Rule r : autos)
            sb.append("• ").append(r.name).append(" — ")
              .append(SmartAutomation.describe(r)).append('\n');
        sb.append('\n');

        Map<String, String> mem = memory.entries();
        List<String> keys = new ArrayList<>(mem.keySet());
        Collections.sort(keys);
        sb.append("🧠 MEMORI (").append(keys.size()).append(")\n");
        if (keys.isEmpty()) sb.append("(kosong)\n");
        for (String k : keys)
            sb.append("• ").append(k).append(" = ").append(mem.get(k)).append('\n');

        tvContent.setText(sb.toString());
    }
}
