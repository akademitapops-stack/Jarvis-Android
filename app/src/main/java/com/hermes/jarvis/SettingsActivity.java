package com.hermes.jarvis;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
    private final List<String> availableModels = new ArrayList<>();

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

        url.setKeyListener(null);
        url.setFocusable(false);
        url.setClickable(false);

        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        model.setAdapter(modelAdapter);
        model.setThreshold(0);
        // Model is selected from the provider catalog, not typed manually.
        model.setKeyListener(null);
        model.setOnClickListener(v -> showModelPicker());
        model.setOnItemClickListener((a,v,pos,id) -> {
            if (pos >= 0 && pos < modelAdapter.getCount()) {
                model.setText(modelAdapter.getItem(pos), false);
                persistSelectedModel();
            }
        });

        ArrayAdapter<String> pa = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, profileNames());
        profile.setAdapter(pa);
        profile.setKeyListener(null);
        profile.setOnClickListener(v -> profile.showDropDown());
        profile.setOnItemClickListener((a,v,pos,id)->{
            List<ProviderProfileStore.Profile> all=store.all();
            if(pos>=0 && pos<all.size()) loadProfile(all.get(pos));
        });

        provider.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, PROVIDERS));
        provider.setKeyListener(null);
        provider.setOnItemClickListener((a,v,pos,id)->selectProvider(pos));
        provider.setOnClickListener(v->provider.showDropDown());

        findViewById(R.id.btnPasteKey).setOnClickListener(v->pasteKey());
        findViewById(R.id.btnToggleKey).setOnClickListener(v->{
            boolean hidden = key.getTransformationMethod() != null;
            key.setTransformationMethod(hidden ? null : android.text.method.PasswordTransformationMethod.getInstance());
            key.setSelection(key.length());
        });
        findViewById(R.id.btnLoadModels).setOnClickListener(v->discoverModels());
        findViewById(R.id.btnChooseModel).setOnClickListener(v->showModelPicker());
        findViewById(R.id.btnSaveProfile).setOnClickListener(v->{saveProfile();});
        findViewById(R.id.btnNewProfile).setOnClickListener(v->newProfile());
        findViewById(R.id.btnDeleteProfile).setOnClickListener(v->deleteProfile());
        findViewById(R.id.btnTest).setOnClickListener(v->testActiveProfile());
        findViewById(R.id.btnCustomApi).setOnClickListener(v->selectProvider(PROVIDERS.length-1));

        findViewById(R.id.btnAppAccess).setOnClickListener(v->startActivity(new Intent(this,AppAccessActivity.class)));
        findViewById(R.id.btnSave).setOnClickListener(v->saveAll());
        telegramOn.setOnCheckedChangeListener((isChecked,checked)->{
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
        // Repair old/corrupt profiles where the profile name said OpenRouter but
        // the provider field still pointed at Gemini (the bug visible in v2.8).
        String fixedProvider = canonicalProvider(x.provider, x.name);
        x.provider = fixedProvider;
        profile.setTag(x.id); name.setText(x.name); provider.setText(fixedProvider,false);
        String fixedBase = providerBaseUrl(fixedProvider);
        String savedBase = UniversalProvider.normalizeBaseUrl(x.baseUrl);
        if (!fixedBase.isEmpty() && !"Custom API (OpenAI-compatible)".equals(fixedProvider)) {
            x.baseUrl = fixedBase;
            url.setText(fixedBase);
        } else {
            url.setText(savedBase);
        }
        boolean custom = "Custom API (OpenAI-compatible)".equals(fixedProvider);
        url.setKeyListener(custom ? android.text.method.TextKeyListener.getInstance() : null);
        url.setFocusable(custom);
        url.setClickable(custom);
        model.setText(x.model, false); key.setText(store.key(x));
        store.active(x.id);
        // Persist the repaired provider/base URL immediately so MainActivity and
        // the API client cannot keep using the stale Gemini profile.
        store.upsert(x, store.key(x));
        availableModels.clear();
        if(!x.model.isEmpty()) availableModels.add(x.model);
        refreshModelAdapter();
        refreshStatus();
    }

    private void selectProvider(int pos){
        if(pos<0||pos>=PROVIDERS.length)return;
        String selected = PROVIDERS[pos];
        provider.setText(selected,false);
        String base=BASE_URLS[pos];
        boolean custom = "Custom API (OpenAI-compatible)".equals(selected);
        if(!base.isEmpty()) { url.setText(base); }
        else if (url.getText().toString().trim().isEmpty()) { url.setText(""); }
        url.setKeyListener(custom ? android.text.method.TextKeyListener.getInstance() : null);
        url.setFocusable(custom);
        url.setClickable(custom);

        // Never keep a model from another provider. It is the main reason
        // profiles appeared to "jump back" to Gemini.
        model.setText("", false);
        availableModels.clear();
        refreshModelAdapter();
        modelStatus.setText("○ Provider berubah · tekan MUAT MODEL");
        refreshStatus();

        // If a key is already present, immediately refresh the catalog.
        if(!UniversalProvider.sanitizeApiKey(key.getText().toString()).isEmpty()) {
            discoverModels();
        }
    }

    private String canonicalProvider(String raw, String profileName) {
        String s = raw == null ? "" : raw.trim().toLowerCase();
        String n = profileName == null ? "" : profileName.trim().toLowerCase();
        if (s.contains("openrouter") || n.equals("openrouter")) return "OpenRouter";
        if (s.contains("gemini") || n.equals("gemini") || n.contains("google gemini")) return "Google Gemini";
        if (s.equals("groq") || n.equals("groq")) return "Groq";
        if (s.contains("openai") || n.equals("openai")) return "OpenAI";
        if (s.contains("deepseek") || n.equals("deepseek")) return "DeepSeek";
        if (s.contains("ollama") || n.equals("ollama")) return "Ollama (lokal)";
        if (s.contains("custom")) return "Custom API (OpenAI-compatible)";
        return raw == null || raw.trim().isEmpty() ? "Custom API (OpenAI-compatible)" : raw.trim();
    }

    private String providerBaseUrl(String providerName) {
        for (int i=0;i<PROVIDERS.length;i++) if(PROVIDERS[i].equals(providerName)) return BASE_URLS[i];
        return "";
    }

    private void newProfile(){
        profile.setTag(null); name.setText("New AI Profile"); provider.setText(PROVIDERS[0],false);
        url.setText(BASE_URLS[0]); model.setText(DEFAULT_MODELS[0]); key.setText("");
        availableModels.clear();
        if(!DEFAULT_MODELS[0].isEmpty()) availableModels.add(DEFAULT_MODELS[0]);
        refreshModelAdapter();
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
        x.provider=canonicalProvider(provider.getText().toString(), x.name);
        String automaticBase = providerBaseUrl(x.provider);
        x.baseUrl = !automaticBase.isEmpty() ? automaticBase : UniversalProvider.normalizeBaseUrl(url.getText().toString());
        if (!automaticBase.isEmpty()) url.setText(automaticBase);
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
        // Save the CURRENT visible fields first. Do not reuse the old active profile.
        saveProfile();
        UniversalProvider p=new UniversalProvider(new PrefsManager(this));
        modelStatus.setText("⏳ Memuat model yang kompatibel...");
        p.discoverModels(new UniversalProvider.ModelsCallback(){
            @Override public void onModels(List<String> models){
                runOnUiThread(()->{
                    availableModels.clear();
                    availableModels.addAll(models);
                    refreshModelAdapter();
                    modelStatus.setText("✓ "+models.size()+" model kompatibel · tekan PILIH MODEL");
                    if (models.size() == 1) {
                        model.setText(models.get(0), false);
                        persistSelectedModel();
                    }
                });
            }
            @Override public void onError(String e){
                runOnUiThread(()->modelStatus.setText("❌ "+e));
            }
        });
    }

    private void refreshModelAdapter(){
        modelAdapter.clear();
        modelAdapter.addAll(availableModels);
        modelAdapter.notifyDataSetChanged();
        model.setAdapter(modelAdapter);
    }

    private void persistSelectedModel(){
        String selected = model.getText().toString().trim();
        if(selected.isEmpty()) return;
        ProviderProfileStore.Profile x = store.active();
        if(x != null){
            x.model = selected;
            store.upsert(x, store.key(x));
        }
        modelStatus.setText("✓ Model aktif: " + selected);
    }

    private void showModelPicker(){
        if(availableModels.isEmpty()){
            discoverModels();
            Toast.makeText(this,"Memuat katalog model dulu...",Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText search = new EditText(this);
        search.setHint("Cari model…");
        search.setSingleLine(true);
        search.setPadding(28,8,28,8);
        final ListView list = new ListView(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(availableModels));
        list.setAdapter(adapter);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18,0,18,0);
        box.addView(search, new LinearLayout.LayoutParams(-1,56));
        box.addView(list, new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("PILIH MODEL · " + availableModels.size())
                .setView(box).setNegativeButton("BATAL",null).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            String selected = adapter.getItem(position);
            if(selected != null){ model.setText(selected,false); persistSelectedModel(); }
            dialog.dismiss();
        });
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                adapter.getFilter().filter(s);
            }
            public void afterTextChanged(android.text.Editable e){}
        });
        dialog.show();
        search.requestFocus();
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
