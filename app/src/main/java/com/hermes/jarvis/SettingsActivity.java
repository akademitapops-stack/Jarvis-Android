package com.hermes.jarvis;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.core.ProviderProfileStore;
import com.hermes.jarvis.core.WhatsAppBridge;
import com.hermes.jarvis.utils.PrefsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] PROVIDERS = {
            "Groq", "Google Gemini", "OpenAI", "DeepSeek", "OpenRouter",
            "Ollama (lokal)", "Custom API (OpenAI-compatible)"
    };
    private static final String[] BASE_URLS = {
            "https://api.groq.com/openai/v1",
            "https://generativelanguage.googleapis.com/v1beta/openai",
            "https://api.openai.com/v1",
            "https://api.deepseek.com",
            "https://openrouter.ai/api/v1",
            "http://127.0.0.1:11434/v1",
            ""
    };
    private static final String[] DEFAULT_MODELS = {
            "llama-3.3-70b-versatile", "gemini-2.5-flash", "gpt-4o-mini",
            "deepseek-chat", "google/gemini-2.5-flash", "llama3.2", ""
    };

    private PrefsManager prefs;
    private ProviderProfileStore store;
    private AutoCompleteTextView profile, provider, model;
    private EditText name, key, url, tgToken, tgAllow, waBridge, waPhone, ghToken, ghRepo, ghBranch;
    private TextView apiStatus, modelStatus;
    private SwitchMaterial speak, fallback, bio, calendar, root, telegramOn;
    private ArrayAdapter<String> modelAdapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        prefs = new PrefsManager(this);
        store = prefs.profileStore();

        profile=findViewById(R.id.spProfile); name=findViewById(R.id.etProfileName);
        provider=findViewById(R.id.spPreset); model=findViewById(R.id.etModel);
        key=findViewById(R.id.etKey); url=findViewById(R.id.etUrl);
        apiStatus=findViewById(R.id.tvApiStatus); modelStatus=findViewById(R.id.tvModelStatus);
        speak=findViewById(R.id.swSpeak); fallback=findViewById(R.id.swFallback);
        bio=findViewById(R.id.swBio); calendar=findViewById(R.id.swCalendar);
        root=findViewById(R.id.swRoot); telegramOn=findViewById(R.id.swTelegram);
        tgToken=findViewById(R.id.etTelegramToken); tgAllow=findViewById(R.id.etTelegramAllow);
        waBridge=findViewById(R.id.etWhatsappBridge); waPhone=findViewById(R.id.etWhatsappPhone);
        ghToken=findViewById(R.id.etGithubToken); ghRepo=findViewById(R.id.etGithubRepo);
        ghBranch=findViewById(R.id.etGithubBranch);

        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        model.setAdapter(modelAdapter);
        model.setThreshold(0);

        ArrayAdapter<String> pa = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, profileNames());
        profile.setAdapter(pa);
        profile.setOnItemClickListener((a,v,pos,id)->{
            List<ProviderProfileStore.Profile> all=store.all();
            if(pos>=0 && pos<all.size()) loadProfile(all.get(pos));
        });

        provider.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, PROVIDERS));
        provider.setOnItemClickListener((a,v,pos,id)->selectProvider(pos));
        provider.setOnClickListener(v->provider.showDropDown());

        findViewById(R.id.btnPasteKey).setOnClickListener(v->pasteKey());
        findViewById(R.id.btnToggleKey).setOnClickListener(v->{
            boolean hidden = key.getTransformationMethod() != null;
            key.setTransformationMethod(hidden ? null : android.text.method.PasswordTransformationMethod.getInstance());
            key.setSelection(key.length());
        });
        findViewById(R.id.btnLoadModels).setOnClickListener(v->discoverModels());
        findViewById(R.id.btnSaveProfile).setOnClickListener(v->{saveProfile();});
        findViewById(R.id.btnNewProfile).setOnClickListener(v->newProfile());
        findViewById(R.id.btnDeleteProfile).setOnClickListener(v->deleteProfile());
        findViewById(R.id.btnTest).setOnClickListener(v->testActiveProfile());
        findViewById(R.id.btnCustomApi).setOnClickListener(v->selectProvider(PROVIDERS.length-1));

        findViewById(R.id.btnAppAccess).setOnClickListener(v->startActivity(new Intent(this,AppAccessActivity.class)));
        findViewById(R.id.btnSave).setOnClickListener(v->saveAll());
        telegramOn.setOnCheckedChangeListener((buttonView,checked)->{
            Intent i=new Intent(this,com.hermes.jarvis.service.TelegramAgentService.class);
            if(checked) startForegroundService(i); else stopService(i);
        });
        findViewById(R.id.btnWebTools).setOnClickListener(v->startActivity(new Intent(this,WebToolsActivity.class)));
        findViewById(R.id.btnPersona).setOnClickListener(v->startActivity(new Intent(this,PersonaActivity.class)));
        findViewById(R.id.btnTools).setOnClickListener(v->startActivity(new Intent(this,ToolsActivity.class)));
        findViewById(R.id.btnGithub).setOnClickListener(v->startActivity(new Intent(this,GitHubActivity.class)));
        findViewById(R.id.btnAccessibility).setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.btnPairWa).setOnClickListener(v->pairWhatsApp());

        load();
    }

    private List<String> profileNames(){
        List<String> x=new ArrayList<>();
        for(ProviderProfileStore.Profile p:store.all()) x.add(p.name+"  ·  "+p.provider+"  ·  "+p.model);
        return x;
    }

    private void load(){
        ProviderProfileStore.Profile x=store.active();
        if(x!=null) loadProfile(x);
        speak.setChecked(prefs.autoSpeak()); fallback.setChecked(prefs.fallbackOffline());
        bio.setChecked(prefs.biometricLock()); calendar.setChecked(prefs.calendarEnabled());
        root.setChecked(prefs.rootAgent());
        telegramOn.setChecked(!prefs.telegramToken().isEmpty() && !prefs.telegramAllowlist().isEmpty());
        tgToken.setText(prefs.telegramToken()); tgAllow.setText(prefs.telegramAllowlist());
        waBridge.setText(prefs.whatsappBridgeUrl()); waPhone.setText(prefs.whatsappPhoneId());
        ghToken.setText(prefs.githubToken()); ghRepo.setText(prefs.githubRepo()); ghBranch.setText(prefs.githubBranch());
        refreshStatus();
    }

    private void loadProfile(ProviderProfileStore.Profile x){
        if(x==null)return;
        profile.setTag(x.id); name.setText(x.name); provider.setText(x.provider,false);
        url.setText(UniversalProvider.normalizeBaseUrl(x.baseUrl));
        model.setText(x.model); key.setText(store.key(x));
        store.active(x.id);
        modelAdapter.clear();
        if(!x.model.isEmpty()) modelAdapter.add(x.model);
        modelAdapter.notifyDataSetChanged();
        refreshStatus();
    }

    private void selectProvider(int pos){
        if(pos<0||pos>=PROVIDERS.length)return;
        provider.setText(PROVIDERS[pos],false);
        String base=BASE_URLS[pos];
        if(!base.isEmpty()) url.setText(base);
        if(DEFAULT_MODELS[pos].isEmpty()) model.setText("");
        else model.setText(DEFAULT_MODELS[pos]);
        modelAdapter.clear();
        if(!DEFAULT_MODELS[pos].isEmpty()) modelAdapter.add(DEFAULT_MODELS[pos]);
        modelAdapter.notifyDataSetChanged();
        if("Custom API (OpenAI-compatible)".equals(PROVIDERS[pos]))
            Toast.makeText(this,"🔧 Custom API aktif. Isi Base URL + API key.",Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void newProfile(){
        profile.setTag(null); name.setText("New AI Profile"); provider.setText(PROVIDERS[0],false);
        url.setText(BASE_URLS[0]); model.setText(DEFAULT_MODELS[0]); key.setText("");
        modelAdapter.clear(); modelAdapter.add(DEFAULT_MODELS[0]); modelAdapter.notifyDataSetChanged();
        apiStatus.setText("● Profil baru — belum diuji"); apiStatus.setTextColor(getColor(R.color.text_dim));
    }

    private void pasteKey(){
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if(cm!=null && cm.hasPrimaryClip()){
            ClipData d=cm.getPrimaryClip();
            if(d!=null&&d.getItemCount()>0){
                String raw=d.getItemAt(0).coerceToText(this).toString();
                String clean=UniversalProvider.sanitizeApiKey(raw);
                key.setText(clean); key.setSelection(key.length());
                apiStatus.setText("✓ API key ditempel & dibersihkan");
                Toast.makeText(this,"API key terbaca dari clipboard.",Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(this,"Clipboard kosong.",Toast.LENGTH_SHORT).show();
    }

    private void saveProfile(){
        ProviderProfileStore.Profile x=null;
        Object selected=profile.getTag();
        if(selected!=null) for(ProviderProfileStore.Profile q:store.all())
            if(q.id.equals(String.valueOf(selected))) x=q;
        if(x==null)x=new ProviderProfileStore.Profile();
        x.name=name.getText().toString().trim();
        if(x.name.isEmpty())x.name="AI Profile";
        x.provider=provider.getText().toString().trim();
        x.baseUrl=UniversalProvider.normalizeBaseUrl(url.getText().toString());
        x.model=model.getText().toString().trim();
        String clean=UniversalProvider.sanitizeApiKey(key.getText().toString());
        if(x.baseUrl.isEmpty()){apiStatus.setText("❌ Base URL wajib diisi");return;}
        if(!clean.isEmpty() || !"Ollama (lokal)".equalsIgnoreCase(x.provider)) {
            if(clean.isEmpty()){apiStatus.setText("❌ API key kosong");return;}
        }
        store.upsert(x,clean); profile.setTag(x.id);
        apiStatus.setText("✓ Tersimpan & aktif · "+x.provider);
        apiStatus.setTextColor(getColor(R.color.green));
        refreshStatus();
    }

    private void testActiveProfile(){
        saveProfile();
        UniversalProvider p=new UniversalProvider(new PrefsManager(this));
        apiStatus.setText("⏳ Menguji API & membaca katalog model...");
        p.test((ok,msg)->runOnUiThread(()->{
            apiStatus.setText((ok?"✓ ":"❌ ")+msg);
            apiStatus.setTextColor(getColor(ok?R.color.green:R.color.error));
            if(ok) discoverModels();
        }));
    }

    private void discoverModels(){
        saveProfile();
        UniversalProvider p=new UniversalProvider(new PrefsManager(this));
        modelStatus.setText("⏳ Memuat model dari provider...");
        p.discoverModels(new UniversalProvider.ModelsCallback(){
            @Override public void onModels(List<String> models){
                runOnUiThread(()->{
                    modelAdapter.clear(); modelAdapter.addAll(models); modelAdapter.notifyDataSetChanged();
                    modelStatus.setText("✓ "+models.size()+" model tersedia · ketuk field untuk memilih");
                    model.setAdapter(modelAdapter);
                    model.showDropDown();
                });
            }
            @Override public void onError(String e){
                runOnUiThread(()->modelStatus.setText("❌ "+e));
            }
        });
    }

    private void refreshStatus(){
        String keyClean=UniversalProvider.sanitizeApiKey(key.getText().toString());
        apiStatus.setText(keyClean.isEmpty() ? "● API key belum diisi" : "● API key siap · "+mask(keyClean));
        apiStatus.setTextColor(getColor(keyClean.isEmpty()?R.color.text_dim:R.color.green));
        modelStatus.setText(model.getText().toString().trim().isEmpty()
                ? "○ Belum ada model · tekan MUAT MODEL" : "✓ Model: "+model.getText());
    }

    private String mask(String s){
        if(s.length()<8)return "••••";
        return s.substring(0,4)+"••••••••"+s.substring(s.length()-4);
    }

    private void deleteProfile(){
        ProviderProfileStore.Profile x=store.active(); if(x==null)return;
        new AlertDialog.Builder(this).setTitle("Hapus profile?")
                .setMessage(x.name).setPositiveButton("Hapus",(d,w)->{store.delete(x.id);load();})
                .setNegativeButton("Batal",null).show();
    }

    private void saveAll(){
        prefs.autoSpeak(speak.isChecked()); prefs.fallbackOffline(fallback.isChecked());
        prefs.biometricLock(bio.isChecked()); prefs.calendarEnabled(calendar.isChecked());
        prefs.rootAgent(root.isChecked()); prefs.telegramToken(tgToken.getText().toString());
        prefs.telegramAllowlist(tgAllow.getText().toString()); prefs.whatsappBridgeUrl(waBridge.getText().toString());
        prefs.whatsappPhoneId(waPhone.getText().toString()); prefs.githubToken(ghToken.getText().toString());
        prefs.githubRepo(ghRepo.getText().toString()); prefs.githubBranch(ghBranch.getText().toString());
        Toast.makeText(this,"✓ Semua settings tersimpan",Toast.LENGTH_SHORT).show();
    }

    private void pairWhatsApp(){
        new WhatsAppBridge().pair(waBridge.getText().toString(),waPhone.getText().toString(),
            new WhatsAppBridge.Callback(){
                public void ok(String t){runOnUiThread(()->new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("🟢 WhatsApp Pairing Code").setMessage(t).setPositiveButton("OK",null).show());}
                public void err(String e){runOnUiThread(()->Toast.makeText(SettingsActivity.this,"❌ "+e,Toast.LENGTH_LONG).show());}
            });
    }
}
