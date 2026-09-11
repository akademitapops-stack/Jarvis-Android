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

        boolean agentMode = prefs.agentMode();
        String sys = SystemPrompt.build(
                ContextEngine.build(appContext),
                memory.dumpForPrompt(),
                agentMode);

        List<String[]> recent = recentHistory();
        provider.send(sys, recent, full, new UniversalProvider.Callback() {
            @Override public void onResponse(AIResponse r) {
                if (!agentMode) {
                    // Safety boundary: normal chat can never execute tool/action fields,
                    // even if a model ignores the chat-only system prompt.
                    r.commands.clear();
                    r.deviceActions.clear();
                    r.webSearches.clear();
                    r.weatherLocation = null;
                    r.autoCreate = null;
                    r.autoDeleteName = null;
                    r.memoryKey = null; r.memoryValue = null;
                    r.scheduleMinutes = -1; r.scheduleMessage = null;
                    r.replyPackage = null; r.replyMessage = null;
                    r.callContact = null; r.contactSearch = null;
                    r.reportHour = -1; r.reportMinute = -1; r.reportDisable = false;
                    r.needsConfirmation = false;
                }
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
