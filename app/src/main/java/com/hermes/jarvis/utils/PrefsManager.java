package com.hermes.jarvis.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.core.ProviderProfileStore;

public class PrefsManager {
    private final SharedPreferences p;
    private final ProviderProfileStore profiles;
    private final com.hermes.jarvis.core.SecretStore secrets;
    public PrefsManager(Context c) { p=c.getSharedPreferences("jarvis_prefs",Context.MODE_PRIVATE); profiles=new ProviderProfileStore(c); secrets=new com.hermes.jarvis.core.SecretStore(c); migrateLegacy(); }
    private void migrateLegacy(){String old=p.getString("api_key",""); if(!old.isEmpty()){ProviderProfileStore.Profile x=profiles.active(); if(x!=null && profiles.key(x).isEmpty()){x.baseUrl=UniversalProvider.normalizeBaseUrl(p.getString("base_url",x.baseUrl));x.model=p.getString("model",x.model);profiles.upsert(x,UniversalProvider.sanitizeApiKey(old));}}}
    private ProviderProfileStore.Profile active(){ return profiles.active(); }
    public String apiKey(){ ProviderProfileStore.Profile x=active(); return x==null?"":profiles.key(x); }
    public void apiKey(String v){ ProviderProfileStore.Profile x=active(); if(x!=null){ profiles.upsert(x, UniversalProvider.sanitizeApiKey(v)); } else p.edit().putString("api_key",UniversalProvider.sanitizeApiKey(v)).apply(); }
    public String baseUrl(){
        ProviderProfileStore.Profile x=active();
        if(x==null) return UniversalProvider.normalizeBaseUrl(p.getString("base_url","https://api.groq.com/openai/v1"));
        String automatic=providerBaseUrl(x.provider);
        return automatic.isEmpty() ? UniversalProvider.normalizeBaseUrl(x.baseUrl) : automatic;
    }
    public void baseUrl(String v){
        ProviderProfileStore.Profile x=active();
        if(x!=null){
            String automatic=providerBaseUrl(x.provider);
            x.baseUrl=automatic.isEmpty()?UniversalProvider.normalizeBaseUrl(v):automatic;
            profiles.upsert(x,apiKey());
        }else p.edit().putString("base_url",UniversalProvider.normalizeBaseUrl(v)).apply();
    }
    private String providerBaseUrl(String provider){
        if(provider==null) return "";
        String s=provider.toLowerCase();
        if(s.contains("groq")) return "https://api.groq.com/openai/v1";
        if(s.contains("gemini")||s.contains("google")) return "https://generativelanguage.googleapis.com/v1beta/openai";
        if(s.equals("openai")||s.contains("openai")) return "https://api.openai.com/v1";
        if(s.contains("deepseek")) return "https://api.deepseek.com";
        if(s.contains("openrouter")) return "https://openrouter.ai/api/v1";
        if(s.contains("ollama")) return "http://127.0.0.1:11434/v1";
        return "";
    }
    public String model(){ ProviderProfileStore.Profile x=active(); return x!=null?x.model:p.getString("model","llama-3.3-70b-versatile"); }
    public void model(String v){ ProviderProfileStore.Profile x=active(); if(x!=null){x.model=v;profiles.upsert(x,apiKey());}else p.edit().putString("model",v).apply(); }
    public String providerName(){ProviderProfileStore.Profile x=active();return x==null?"Custom":(x.provider==null||x.provider.trim().isEmpty()?x.name:x.provider);}
    public ProviderProfileStore profileStore(){return profiles;}
    public boolean autoSpeak(){return p.getBoolean("auto_speak",true);} public void autoSpeak(boolean v){p.edit().putBoolean("auto_speak",v).apply();}
    public boolean fallbackOffline(){return p.getBoolean("fallback",true);} public void fallbackOffline(boolean v){p.edit().putBoolean("fallback",v).apply();}
    public boolean agentMode(){return p.getBoolean("agent_mode",true);} public void agentMode(boolean v){p.edit().putBoolean("agent_mode",v).apply();}
    public boolean biometricLock(){return p.getBoolean("bio_lock",false);} public void biometricLock(boolean v){p.edit().putBoolean("bio_lock",v).apply();}
    public int historyLimit(){return p.getInt("hist_limit",8);}
    public void telegramToken(String v){secrets.put("tg_token",v==null?"":v.trim());} public String telegramToken(){return secrets.get("tg_token");}
    public void telegramAllowlist(String v){p.edit().putString("tg_allow",v==null?"":v.trim()).apply();} public String telegramAllowlist(){return p.getString("tg_allow","");}
    public void whatsappToken(String v){secrets.put("wa_token",v==null?"":v.trim());} public String whatsappToken(){return secrets.get("wa_token");}
    public void whatsappPhoneId(String v){p.edit().putString("wa_phone",v==null?"":v.trim()).apply();} public String whatsappPhoneId(){return p.getString("wa_phone","");}
    public void whatsappAllowlist(String v){p.edit().putString("wa_allow",v==null?"":v.trim()).apply();} public String whatsappAllowlist(){return p.getString("wa_allow","");}
    public void whatsappBridgeUrl(String v){p.edit().putString("wa_bridge",v==null?"":v.trim()).apply();} public String whatsappBridgeUrl(){return p.getString("wa_bridge","");}
    public void githubToken(String v){secrets.put("gh_token",v==null?"":v.trim());} public String githubToken(){return secrets.get("gh_token");}
    public void githubRepo(String v){p.edit().putString("gh_repo",v==null?"":v.trim()).apply();} public String githubRepo(){return p.getString("gh_repo","");}
    public void githubBranch(String v){p.edit().putString("gh_branch",v==null||v.trim().isEmpty()?"main":v.trim()).apply();} public String githubBranch(){return p.getString("gh_branch","main");}
    public void calendarEnabled(boolean v){p.edit().putBoolean("calendar",v).apply();} public boolean calendarEnabled(){return p.getBoolean("calendar",false);}
    public void rootAgent(boolean v){p.edit().putBoolean("root_agent",v).apply();} public boolean rootAgent(){return p.getBoolean("root_agent",false);}
    public int nextAlarmId(){int i=p.getInt("alarm_id",1);p.edit().putInt("alarm_id",i+1).apply();return i;}
}
