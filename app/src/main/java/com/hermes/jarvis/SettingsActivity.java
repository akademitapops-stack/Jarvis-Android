package com.hermes.jarvis;

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
import com.hermes.jarvis.service.JarvisAccessibilityService;
import com.hermes.jarvis.utils.PrefsManager;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] PROVIDERS={"Groq","Google Gemini","OpenAI","DeepSeek","OpenRouter","Ollama (lokal)","Custom OpenAI-compatible"};
    private static final String[][] MODELS={{"llama-3.3-70b-versatile","llama-3.1-8b-instant"},{"gemini-2.5-flash","gemini-2.5-pro"},{"gpt-4o-mini","gpt-4.1-mini"},{"deepseek-chat","deepseek-reasoner"},{"google/gemini-2.5-flash","anthropic/claude-3.7-sonnet","openai/gpt-4o-mini","deepseek/deepseek-chat"},{"llama3.2","qwen2.5"},{"custom-model"}};
    private static final String[] URLS={"https://api.groq.com/openai/v1/chat/completions","https://generativelanguage.googleapis.com/v1beta/openai/chat/completions","https://api.openai.com/v1/chat/completions","https://api.deepseek.com/chat/completions","https://openrouter.ai/api/v1/chat/completions","http://127.0.0.1:11434/v1/chat/completions",""};
    private PrefsManager prefs; private ProviderProfileStore store; private AutoCompleteTextView profile,provider,model; private EditText name,key,url,tgToken,tgAllow,waBridge,waPhone,ghToken,ghRepo,ghBranch; private SwitchMaterial speak,fallback,bio,calendar,root,telegramOn;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_settings);prefs=new PrefsManager(this);store=prefs.profileStore();
        profile=findViewById(R.id.spProfile);name=findViewById(R.id.etProfileName);provider=findViewById(R.id.spPreset);model=findViewById(R.id.etModel);key=findViewById(R.id.etKey);url=findViewById(R.id.etUrl);
        speak=findViewById(R.id.swSpeak);fallback=findViewById(R.id.swFallback);bio=findViewById(R.id.swBio);calendar=findViewById(R.id.swCalendar);root=findViewById(R.id.swRoot);telegramOn=findViewById(R.id.swTelegram);
        tgToken=findViewById(R.id.etTelegramToken);tgAllow=findViewById(R.id.etTelegramAllow);waBridge=findViewById(R.id.etWhatsappBridge);waPhone=findViewById(R.id.etWhatsappPhone);ghToken=findViewById(R.id.etGithubToken);ghRepo=findViewById(R.id.etGithubRepo);ghBranch=findViewById(R.id.etGithubBranch);
        ArrayAdapter<String> pa=new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,profileNames());profile.setAdapter(pa);profile.setOnItemClickListener((a,v,pos,id)->loadProfile(store.all().get(pos)));
        provider.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,PROVIDERS));provider.setOnItemClickListener((a,v,pos,id)->{url.setText(URLS[pos]);model.setText(MODELS[pos][0]);model.setTag(MODELS[pos]);});
        model.setOnClickListener(v->showModels());findViewById(R.id.btnSaveProfile).setOnClickListener(v->saveProfile());findViewById(R.id.btnNewProfile).setOnClickListener(v->{name.setText("New AI Profile");profile.setTag(null);provider.setText(PROVIDERS[0],false);url.setText(URLS[0]);model.setText(MODELS[0][0]);key.setText("");});findViewById(R.id.btnDeleteProfile).setOnClickListener(v->deleteProfile());findViewById(R.id.btnTest).setOnClickListener(v->Toast.makeText(this,prefs.apiKey().isEmpty()?"❌ API key kosong":"✅ Credential profile siap dipakai",Toast.LENGTH_LONG).show());
findViewById(R.id.btnAppAccess).setOnClickListener(v->startActivity(new Intent(this,AppAccessActivity.class)));        findViewById(R.id.btnSave).setOnClickListener(v->saveAll()); telegramOn.setOnCheckedChangeListener((b,checked)->{Intent i=new Intent(this,com.hermes.jarvis.service.TelegramAgentService.class);if(checked){startForegroundService(i);}else{stopService(i);}});findViewById(R.id.btnWebTools).setOnClickListener(v->startActivity(new Intent(this,WebToolsActivity.class)));findViewById(R.id.btnPersona).setOnClickListener(v->startActivity(new Intent(this,PersonaActivity.class)));findViewById(R.id.btnTools).setOnClickListener(v->startActivity(new Intent(this,ToolsActivity.class)));findViewById(R.id.btnGithub).setOnClickListener(v->startActivity(new Intent(this,GitHubActivity.class)));findViewById(R.id.btnAccessibility).setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));findViewById(R.id.btnPairWa).setOnClickListener(v->pairWhatsApp());
        load();
    }
    private List<String> profileNames(){List<String> x=new ArrayList<>();for(ProviderProfileStore.Profile p:store.all())x.add(p.name+" · "+p.model);return x;}
    private void load(){ProviderProfileStore.Profile x=store.active();if(x!=null)loadProfile(x);speak.setChecked(prefs.autoSpeak());fallback.setChecked(prefs.fallbackOffline());bio.setChecked(prefs.biometricLock());calendar.setChecked(prefs.calendarEnabled());root.setChecked(prefs.rootAgent());telegramOn.setChecked(!prefs.telegramToken().isEmpty() && !prefs.telegramAllowlist().isEmpty());tgToken.setText(prefs.telegramToken());tgAllow.setText(prefs.telegramAllowlist());waBridge.setText(prefs.whatsappBridgeUrl());waPhone.setText(prefs.whatsappPhoneId());ghToken.setText(prefs.githubToken());ghRepo.setText(prefs.githubRepo());ghBranch.setText(prefs.githubBranch());}
    private void loadProfile(ProviderProfileStore.Profile x){if(x==null)return;profile.setTag(x.id);name.setText(x.name);provider.setText(x.provider,false);url.setText(x.baseUrl);model.setText(x.model);key.setText(store.key(x));model.setTag(modelsFor(x.provider));store.active(x.id);}
    private String[] modelsFor(String p){for(int i=0;i<PROVIDERS.length;i++)if(PROVIDERS[i].equalsIgnoreCase(p))return MODELS[i];return new String[]{model.getText().toString()};}
    private void showModels(){Object t=model.getTag();String[] ms=t instanceof String[]?(String[])t:new String[]{model.getText().toString()};new AlertDialog.Builder(this).setTitle("Model — "+provider.getText()).setItems(ms,(d,w)->model.setText(ms[w])).show();}
    private void saveProfile(){ProviderProfileStore.Profile x=null; Object selected=profile.getTag(); if(selected!=null){for(ProviderProfileStore.Profile q:store.all())if(q.id.equals(String.valueOf(selected)))x=q;} if(x==null)x=new ProviderProfileStore.Profile();x.name=name.getText().toString().trim();if(x.name.isEmpty())x.name="AI Profile";x.provider=provider.getText().toString().trim();x.baseUrl=url.getText().toString().trim();x.model=model.getText().toString().trim();store.upsert(x,UniversalProvider.sanitizeApiKey(key.getText().toString()));Toast.makeText(this,"✅ Profile tersimpan & aktif: "+x.name,Toast.LENGTH_SHORT).show();recreate();}
    private void deleteProfile(){ProviderProfileStore.Profile x=store.active();if(x==null)return;new AlertDialog.Builder(this).setTitle("Hapus profile?").setMessage(x.name).setPositiveButton("Hapus",(d,w)->{store.delete(x.id);recreate();}).setNegativeButton("Batal",null).show();}
    private void saveAll(){prefs.autoSpeak(speak.isChecked());prefs.fallbackOffline(fallback.isChecked());prefs.biometricLock(bio.isChecked());prefs.calendarEnabled(calendar.isChecked());prefs.rootAgent(root.isChecked());prefs.telegramToken(tgToken.getText().toString());prefs.telegramAllowlist(tgAllow.getText().toString());prefs.whatsappBridgeUrl(waBridge.getText().toString());prefs.whatsappPhoneId(waPhone.getText().toString());prefs.githubToken(ghToken.getText().toString());prefs.githubRepo(ghRepo.getText().toString());prefs.githubBranch(ghBranch.getText().toString());Toast.makeText(this,"✅ Settings tersimpan",Toast.LENGTH_SHORT).show();}
    private void pairWhatsApp(){new WhatsAppBridge().pair(waBridge.getText().toString(),waPhone.getText().toString(),new WhatsAppBridge.Callback(){public void ok(String t){runOnUiThread(()->new AlertDialog.Builder(SettingsActivity.this).setTitle("🟢 WhatsApp Pairing Code").setMessage(t).setPositiveButton("OK",null).show());}public void err(String e){runOnUiThread(()->Toast.makeText(SettingsActivity.this,"❌ "+e,Toast.LENGTH_LONG).show());}});}
}
