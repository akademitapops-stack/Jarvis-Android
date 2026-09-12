package com.hermes.jarvis.ai;

import android.content.Context;
import com.hermes.jarvis.core.ContextEngine;
import com.hermes.jarvis.core.CustomToolStore;
import com.hermes.jarvis.core.MemoryBank;
import com.hermes.jarvis.core.PersonaStore;
import com.hermes.jarvis.core.SessionStore;
import com.hermes.jarvis.utils.PrefsManager;
import java.util.ArrayList;
import java.util.List;

public class AIManager {
    public interface Callback { void onResponse(AIResponse r); void onError(String e); }
    private final UniversalProvider provider; private final PrefsManager prefs; private final MemoryBank memory;
    private final Context appContext; private final SessionStore sessions; private final PersonaStore persona; private final CustomToolStore customTools;
    private final List<String[]> history = new ArrayList<>();
    public AIManager(Context ctx, MemoryBank memory) {
        appContext=ctx.getApplicationContext(); prefs=new PrefsManager(ctx); this.memory=memory; provider=new UniversalProvider(prefs);
        sessions=new SessionStore(ctx); persona=new PersonaStore(ctx); customTools=new CustomToolStore(ctx); loadCurrent();
    }
    private void loadCurrent(){history.clear(); SessionStore.Session s=sessions.current(); if(s!=null) for(SessionStore.Chat m:s.messages) history.add(new String[]{m.role,m.text}); trim();}
    public void send(String userMessage,String terminalFeedback,Callback cb){
        if(!provider.isConfigured()){cb.onError("API belum dikonfigurasi! Pilih profile AI dan simpan API key.");return;}
        String full=terminalFeedback==null?userMessage:userMessage+"\n\n"+terminalFeedback;
        String sys=SystemPrompt.build(ContextEngine.build(appContext),memory.dumpForPrompt(),persona.prompt(),customTools.prompt());
        List<String[]> recent=recentHistory();
        provider.send(sys,recent,full,new UniversalProvider.Callback(){
            @Override public void onResponse(AIResponse r){history.add(new String[]{"user",full});history.add(new String[]{"assistant",r.raw});sessions.add("user",full);sessions.add("assistant",r.raw);trim();cb.onResponse(r);}
            @Override public void onError(String e){cb.onError(e);}
        });
    }
    private List<String[]> recentHistory(){int lim=prefs.historyLimit()*2;int start=Math.max(0,history.size()-lim);return new ArrayList<>(history.subList(start,history.size()));}
    private void trim(){while(history.size()>prefs.historyLimit()*2)history.remove(0);}
    public void clearHistory(){history.clear();sessions.clearCurrent();}
    public boolean isConfigured(){return provider.isConfigured();} public String info(){return prefs.model();}
    public SessionStore store(){return sessions;}
    public List<SessionStore.Chat> currentMessages(){SessionStore.Session s=sessions.current();return s==null?new ArrayList<>():new ArrayList<>(s.messages);}
    public void switchSession(String id){sessions.switchTo(id);loadCurrent();}
    public void newSession(String title){sessions.create(title);loadCurrent();}
    public PersonaStore persona(){return persona;}
    public CustomToolStore customTools(){return customTools;}
}
