package com.hermes.jarvis;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final class ProviderPreset {
        final String id, name, baseUrl;
        final String[] fallbackModels;

        ProviderPreset(String id, String name, String baseUrl, String... models) {
            this.id = id;
            this.name = name;
            this.baseUrl = baseUrl;
            this.fallbackModels = models;
        }
    }

    private final ProviderPreset[] PRESETS = new ProviderPreset[]{
            new ProviderPreset("openrouter", "🌐 OpenRouter",
                    "https://openrouter.ai/api/v1",
                    "openai/gpt-4o-mini", "google/gemini-2.5-flash",
                    "deepseek/deepseek-chat"),
            new ProviderPreset("gemini", "✨ Google Gemini",
                    "https://generativelanguage.googleapis.com/v1beta/openai",
                    "gemini-2.5-flash", "gemini-2.5-pro"),
            new ProviderPreset("openai", "🟢 OpenAI",
                    "https://api.openai.com/v1",
                    "gpt-4o-mini", "gpt-4.1-mini"),
            new ProviderPreset("groq", "⚡ Groq",
                    "https://api.groq.com/openai/v1",
                    "llama-3.3-70b-versatile", "llama-3.1-8b-instant"),
            new ProviderPreset("deepseek", "🐋 DeepSeek",
                    "https://api.deepseek.com",
                    "deepseek-chat", "deepseek-reasoner"),
            new ProviderPreset("ollama", "🦙 Ollama (lokal)",
                    "http://127.0.0.1:11434/v1",
                    "llama3.2", "qwen2.5"),
            new ProviderPreset("custom", "🔧 Custom OpenAI-compatible",
                    "", "custom-model")
    };

    private PrefsManager prefs;
    private UniversalProvider api;
    private AutoCompleteTextView provider, model;
    private EditText key, url, tgToken, tgAllow, waToken, waPhone, waAllow;
    private SwitchMaterial speak, fallback, bio;
    private TextView status;
    private final List<String> discoveredModels = new ArrayList<>();
    private final Map<String, String> providerNames = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        prefs = new PrefsManager(this);
        api = new UniversalProvider(prefs);

        provider = findViewById(R.id.spPreset);
        model = findViewById(R.id.etModel);
        key = findViewById(R.id.etKey);
        url = findViewById(R.id.etUrl);
        status = findViewById(R.id.tvConfigStatus);

        speak = findViewById(R.id.swSpeak);
        fallback = findViewById(R.id.swFallback);
        bio = findViewById(R.id.swBio);
        tgToken = findViewById(R.id.etTelegramToken);
        tgAllow = findViewById(R.id.etTelegramAllow);
        waToken = findViewById(R.id.etWhatsappToken);
        waPhone = findViewById(R.id.etWhatsappPhone);
        waAllow = findViewById(R.id.etWhatsappAllow);

        List<String> names = new ArrayList<>();
        for (ProviderPreset p : PRESETS) {
            names.add(p.name);
            providerNames.put(p.id, p.name);
        }

        provider.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names));
        provider.setInputType(InputType.TYPE_NULL);
        provider.setKeyListener(null);
        provider.setOnClickListener(v -> provider.showDropDown());
        provider.setOnItemClickListener((p, v, pos, id) -> applyProvider(PRESETS[pos]));

        model.setInputType(InputType.TYPE_NULL);
        model.setKeyListener(null);
        model.setOnClickListener(v -> {
            if (discoveredModels.isEmpty()) {
                discoverModels(false);
            } else {
                showModels(discoveredModels);
            }
        });

        findViewById(R.id.btnTest).setOnClickListener(v -> discoverModels(true));
        findViewById(R.id.btnRefreshModels).setOnClickListener(v -> discoverModels(true));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        load();
    }

    private ProviderPreset findPreset(String id) {
        for (ProviderPreset p : PRESETS) if (p.id.equals(id)) return p;
        return PRESETS[PRESETS.length - 1];
    }

    private void applyProvider(ProviderPreset p) {
        prefs.providerId(p.id);
        url.setText(p.baseUrl);
        url.setEnabled("custom".equals(p.id));
        discoveredModels.clear();

        if (p.fallbackModels.length > 0) model.setText(p.fallbackModels[0]);
        status.setText("Provider dipilih: " + p.name
                + "\nTekan VALIDATE & LOAD MODELS untuk mengambil katalog model dari API key.");
        status.setTextColor(Color.LTGRAY);
    }

    private void load() {
        String pid = prefs.providerId();
        ProviderPreset p = findPreset(pid);
        provider.setText(providerNames.get(pid) != null ? providerNames.get(pid) : p.name, false);

        key.setText(prefs.apiKey());
        url.setText(prefs.baseUrl());
        url.setEnabled("custom".equals(pid));
        model.setText(prefs.model());

        speak.setChecked(prefs.autoSpeak());
        fallback.setChecked(prefs.fallbackOffline());
        bio.setChecked(prefs.biometricLock());
        tgToken.setText(prefs.telegramToken());
        tgAllow.setText(prefs.telegramAllowlist());
        waToken.setText(prefs.whatsappToken());
        waPhone.setText(prefs.whatsappPhoneId());
        waAllow.setText(prefs.whatsappAllowlist());

        if (!prefs.apiKey().isEmpty()) {
            status.setText("🔐 API key tersimpan. Siap mendeteksi model.");
            status.setTextColor(Color.LTGRAY);
        } else {
            status.setText("Masukkan API key. Paste dari Notes juga didukung.");
            status.setTextColor(Color.LTGRAY);
        }
    }

    private void discoverModels(boolean showToast) {
        final String cleanKey = UniversalProvider.sanitizeApiKey(key.getText().toString());
        final String pid = prefs.providerId();
        final String base = UniversalProvider.normalizeBaseUrl(url.getText().toString());

        if (cleanKey.isEmpty()) {
            status.setText("❌ API key kosong setelah sanitasi. Pastikan key benar-benar ter-paste.");
            status.setTextColor(Color.RED);
            if (showToast) Toast.makeText(this,
                    "❌ API key kosong. Paste ulang API key.", Toast.LENGTH_LONG).show();
            return;
        }

        if (base.isEmpty() && !"ollama".equals(pid)) {
            status.setText("❌ Base URL kosong.");
            return;
        }

        // Critical fix: validate the key currently visible in the EditText.
        // Do not read the old SharedPreferences value here.
        prefs.apiKey(cleanKey);
        prefs.baseUrl(base);

        status.setText("⏳ Menghubungkan ke " + providerNames.get(pid)
                + " dan mengambil daftar model...");
        status.setTextColor(Color.LTGRAY);

        api.discoverModels(pid, base, cleanKey, new UniversalProvider.ModelsCallback() {
            @Override public void onResult(List<String> models, String providerId) {
                runOnUiThread(() -> {
                    discoveredModels.clear();
                    discoveredModels.addAll(models);

                    String current = model.getText().toString().trim();
                    if (current.isEmpty() || !discoveredModels.contains(current)) {
                        model.setText(discoveredModels.get(0));
                    }

                    status.setText("✅ API VALID • " + discoveredModels.size()
                            + " model terdeteksi\nTap MODEL untuk memilih.");
                    status.setTextColor(Color.rgb(80, 220, 120));

                    if (showToast) Toast.makeText(SettingsActivity.this,
                            "✅ " + discoveredModels.size() + " model tersedia",
                            Toast.LENGTH_SHORT).show();

                    showModels(discoveredModels);
                });
            }

            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    status.setText(error);
                    status.setTextColor(Color.RED);
                    if (showToast) Toast.makeText(SettingsActivity.this,
                            error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showModels(List<String> models) {
        if (models == null || models.isEmpty()) return;

        final EditText search = new EditText(this);
        search.setHint("🔎 Cari model...");
        search.setSingleLine(true);
        search.setPadding(28, 12, 28, 12);

        final List<String> filtered = new ArrayList<>(models);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, filtered);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(search, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 58));
        ListView list = new ListView(this);
        box.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        list.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🧠 PILIH MODEL (" + models.size() + ")")
                .setView(box)
                .setNegativeButton("Tutup", null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            model.setText(filtered.get(position));
            dialog.dismiss();
        });

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                String q = s.toString().toLowerCase(Locale.ROOT).trim();
                filtered.clear();
                for (String m : models)
                    if (q.isEmpty() || m.toLowerCase(Locale.ROOT).contains(q)) filtered.add(m);
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * .94f),
                    (int)(getResources().getDisplayMetrics().heightPixels * .78f));
    }

    private void save() {
        String cleanKey = UniversalProvider.sanitizeApiKey(key.getText().toString());
        prefs.providerId(resolveProviderId());
        prefs.apiKey(cleanKey);
        prefs.baseUrl(url.getText().toString());
        prefs.model(model.getText().toString());
        prefs.autoSpeak(speak.isChecked());
        prefs.fallbackOffline(fallback.isChecked());
        prefs.biometricLock(bio.isChecked());
        prefs.telegramToken(tgToken.getText().toString());
        prefs.telegramAllowlist(tgAllow.getText().toString());
        prefs.whatsappToken(waToken.getText().toString());
        prefs.whatsappPhoneId(waPhone.getText().toString());
        prefs.whatsappAllowlist(waAllow.getText().toString());

        Toast.makeText(this,
                cleanKey.isEmpty() ? "⚠️ Tersimpan tanpa API key"
                        : "✅ Konfigurasi tersimpan",
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private String resolveProviderId() {
        String current = provider.getText().toString();
        for (ProviderPreset p : PRESETS)
            if (p.name.equals(current)) return p.id;
        return prefs.providerId();
    }
}
