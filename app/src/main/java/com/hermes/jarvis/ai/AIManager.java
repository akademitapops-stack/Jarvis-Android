package com.hermes.jarvis.ai;

import android.content.Context;

import com.hermes.jarvis.core.ContextEngine;
import com.hermes.jarvis.core.MemoryBank;
import com.hermes.jarvis.utils.PrefsManager;

import java.util.ArrayList;
import java.util.List;

public class AIManager {

    public interface Callback {
        void onResponse(AIResponse r);
        void onError(String e);
    }

    private final UniversalProvider provider;
    private final PrefsManager prefs;
    private final MemoryBank memory;
    private final Context appContext;
    private final List<String[]> history = new ArrayList<>();

    public AIManager(Context ctx, MemoryBank memory) {
        this.appContext = ctx.getApplicationContext();
        this.prefs = new PrefsManager(ctx);
        this.memory = memory;
        this.provider = new UniversalProvider(prefs);
    }

    public void send(String userMessage, String terminalFeedback, Callback cb) {
        if (!provider.isConfigured()) {
            cb.onError("API belum dikonfigurasi!\n\nBuka ⚙️ Settings → pilih preset "
                    + "(Groq/Gemini gratis) → isi API key.\n\nAtau coba mode offline "
                    + "dengan perintah sederhana.");
            return;
        }

        String full = terminalFeedback == null ? userMessage
                : userMessage + "\n\n" + terminalFeedback;

        String sys = SystemPrompt.build(
                ContextEngine.build(appContext),
                memory.dumpForPrompt());

        List<String[]> recent = recentHistory();
        provider.send(sys, recent, full, new UniversalProvider.Callback() {
            @Override public void onResponse(AIResponse r) {
                history.add(new String[]{"user", full});
                history.add(new String[]{"assistant", r.raw});
                trim();
                cb.onResponse(r);
            }
            @Override public void onError(String e) { cb.onError(e); }
        });
    }

    private List<String[]> recentHistory() {
        int lim = prefs.historyLimit() * 2;
        int start = Math.max(0, history.size() - lim);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    private void trim() {
        while (history.size() > prefs.historyLimit() * 2) history.remove(0);
    }

    public void clearHistory() { history.clear(); }
    public boolean isConfigured() { return provider.isConfigured(); }
    public String info() { return prefs.model(); }
}
