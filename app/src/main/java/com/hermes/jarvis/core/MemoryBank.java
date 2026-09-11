package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class MemoryBank {

    private static final String KEY = "memory_map";
    private final SharedPreferences p;
    private final Gson gson = new Gson();
    private Map<String, String> mem;

    public MemoryBank(Context c) {
        p = c.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        String raw = p.getString(KEY, "{}");
        try {
            mem = gson.fromJson(raw, new TypeToken<HashMap<String, String>>(){}.getType());
        } catch (Exception e) { mem = new HashMap<>(); }
        if (mem == null) mem = new HashMap<>();
    }

    private void save() {
        p.edit().putString(KEY, gson.toJson(mem)).apply();
    }

    public void remember(String key, String value) {
        mem.put(key.toLowerCase().trim(), value);
        save();
    }

    public void forget(String key) {
        mem.remove(key.toLowerCase().trim());
        save();
    }

    public String recall(String key) {
        return mem.get(key.toLowerCase().trim());
    }

    public Map<String, String> entries() { return new HashMap<>(mem); }

    public void clearAll() { mem.clear(); save(); }

    public String dumpForPrompt() {
        if (mem.isEmpty()) return "(belum ada memori tersimpan)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : mem.entrySet()) {
            sb.append("• ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        }
        return sb.toString().trim();
    }

    public int size() { return mem.size(); }
}
