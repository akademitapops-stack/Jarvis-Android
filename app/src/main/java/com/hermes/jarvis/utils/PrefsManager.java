package com.hermes.jarvis.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.hermes.jarvis.ai.UniversalProvider;

public class PrefsManager {
    private final SharedPreferences p;

    public PrefsManager(Context c) {
        p = c.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE);
    }

    public String apiKey() { return UniversalProvider.sanitizeApiKey(p.getString("api_key", "")); }
    public void apiKey(String v) {
        p.edit().putString("api_key", UniversalProvider.sanitizeApiKey(v)).apply();
    }

    public String providerId() {
        String saved = p.getString("provider_id", null);
        if (saved != null && !saved.isEmpty()) return saved;

        // Migrate older v2.x installs which stored only the full endpoint URL.
        String u = p.getString("base_url", "").toLowerCase();
        if (u.contains("openrouter.ai")) return "openrouter";
        if (u.contains("generativelanguage.googleapis.com")) return "gemini";
        if (u.contains("api.openai.com")) return "openai";
        if (u.contains("api.groq.com")) return "groq";
        if (u.contains("api.deepseek.com")) return "deepseek";
        if (u.contains("127.0.0.1:11434") || u.contains("localhost:11434")) return "ollama";
        return "custom";
    }
    public void providerId(String v) {
        p.edit().putString("provider_id", v == null ? "custom" : v).apply();
    }

    public String baseUrl() {
        return p.getString("base_url", "https://api.groq.com/openai/v1");
    }
    public void baseUrl(String v) {
        p.edit().putString("base_url", UniversalProvider.normalizeBaseUrl(v)).apply();
    }

    public String model() {
        return p.getString("model", "llama-3.3-70b-versatile");
    }
    public void model(String v) { p.edit().putString("model", v == null ? "" : v.trim()).apply(); }

    public boolean agentMode() { return p.getBoolean("agent_mode", true); }
    public void agentMode(boolean v) { p.edit().putBoolean("agent_mode", v).apply(); }

    public boolean autoSpeak() { return p.getBoolean("auto_speak", true); }
    public void autoSpeak(boolean v) { p.edit().putBoolean("auto_speak", v).apply(); }

    public boolean fallbackOffline() { return p.getBoolean("fallback", true); }
    public void fallbackOffline(boolean v) { p.edit().putBoolean("fallback", v).apply(); }

    public boolean biometricLock() { return p.getBoolean("bio_lock", false); }
    public void biometricLock(boolean v) { p.edit().putBoolean("bio_lock", v).apply(); }

    public int historyLimit() { return p.getInt("hist_limit", 8); }

    public void telegramToken(String v) { p.edit().putString("tg_token", v == null ? "" : v.trim()).apply(); }
    public String telegramToken() { return p.getString("tg_token", ""); }
    public void telegramAllowlist(String v) { p.edit().putString("tg_allow", v == null ? "" : v.trim()).apply(); }
    public String telegramAllowlist() { return p.getString("tg_allow", ""); }
    public void whatsappToken(String v) { p.edit().putString("wa_token", v == null ? "" : v.trim()).apply(); }
    public String whatsappToken() { return p.getString("wa_token", ""); }
    public void whatsappPhoneId(String v) { p.edit().putString("wa_phone", v == null ? "" : v.trim()).apply(); }
    public String whatsappPhoneId() { return p.getString("wa_phone", ""); }
    public void whatsappAllowlist(String v) { p.edit().putString("wa_allow", v == null ? "" : v.trim()).apply(); }
    public String whatsappAllowlist() { return p.getString("wa_allow", ""); }

    public int nextAlarmId() {
        int i = p.getInt("alarm_id", 1);
        p.edit().putInt("alarm_id", i + 1).apply();
        return i;
    }
}
