package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hermes.jarvis.ai.UniversalProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent multi-provider vault. API keys are encrypted with Android Keystore. */
public class ProviderProfileStore {
    public static class Profile {
        public String id = UUID.randomUUID().toString();
        public String name = "New profile";
        public String provider = "Custom OpenAI-compatible";
        public String baseUrl = "";
        public String model = "";
        public String secretRef = "";
    }

    private final SharedPreferences p;
    private final SecretStore secrets;
    private final Gson gson = new Gson();
    private List<Profile> profiles;

    public ProviderProfileStore(Context c) {
        p = c.getSharedPreferences("jarvis_provider_profiles", Context.MODE_PRIVATE);
        secrets = new SecretStore(c);
        load();
        if (profiles.isEmpty()) seedDefaults();
    }

    private void load() {
        try { profiles = gson.fromJson(p.getString("profiles", "[]"), new TypeToken<List<Profile>>(){}.getType()); }
        catch (Exception e) { profiles = new ArrayList<>(); }
        if (profiles == null) profiles = new ArrayList<>();
        for (Profile x : profiles) if (x != null) x.baseUrl = UniversalProvider.normalizeBaseUrl(x.baseUrl);
    }
    private void save() { p.edit().putString("profiles", gson.toJson(profiles)).apply(); }

    private void seedDefaults() {
        add("Groq", "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", "");
        add("Gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash", "");
        add("OpenRouter", "OpenRouter", "https://openrouter.ai/api/v1", "google/gemini-2.5-flash", "");
        add("OpenAI", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "");
        add("DeepSeek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat", "");
        add("Ollama", "Ollama (lokal)", "http://127.0.0.1:11434/v1", "llama3.2", "");
        save();
        if (!profiles.isEmpty()) active(profiles.get(0).id);
    }

    public List<Profile> all() { return new ArrayList<>(profiles); }
    public Profile active() {
        String id = p.getString("active_id", "");
        for (Profile x : profiles) if (x.id.equals(id)) return x;
        return profiles.isEmpty() ? null : profiles.get(0);
    }
    public void active(String id) { p.edit().putString("active_id", id).apply(); }
    public void upsert(Profile x, String apiKey) {
        if (x.id == null || x.id.isEmpty()) x.id = UUID.randomUUID().toString();
        x.secretRef = "key_" + x.id;
        secrets.put(x.secretRef, UniversalProvider.sanitizeApiKey(apiKey));
        boolean replaced = false;
        for (int i=0;i<profiles.size();i++) if (profiles.get(i).id.equals(x.id)) { profiles.set(i,x); replaced=true; break; }
        if (!replaced) profiles.add(x);
        save(); active(x.id);
    }
    public String key(Profile x) { return x == null || x.secretRef == null ? "" : secrets.get(x.secretRef); }
    public void delete(String id) {
        Profile x = null; for (Profile q: profiles) if (q.id.equals(id)) x=q;
        if (x != null && x.secretRef != null) secrets.remove(x.secretRef);
        profiles.removeIf(q -> q.id.equals(id)); save();
        if (profiles.size()>0 && active()==null) active(profiles.get(0).id);
    }
    public void add(String name,String provider,String url,String model,String key) {
        Profile x = new Profile(); x.name=name; x.provider=provider; x.baseUrl=UniversalProvider.normalizeBaseUrl(url); x.model=model; upsert(x,key);
    }
}
