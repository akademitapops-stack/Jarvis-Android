package com.hermes.jarvis;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.utils.PrefsManager;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] PROVIDERS={"Groq","Google Gemini","OpenAI","DeepSeek","OpenRouter","Ollama (lokal)","Custom OpenAI-compatible"};
    private static final String[] URLS={
            "https://api.groq.com/openai/v1/chat/completions",
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            "https://api.openai.com/v1/chat/completions",
            "https://api.deepseek.com/chat/completions",
            "https://openrouter.ai/api/v1/chat/completions",
            "http://127.0.0.1:11434/v1/chat/completions", ""};
    private static final String[][] MODELS={
            {"llama-3.3-70b-versatile","llama-3.1-8b-instant"},
            {"gemini-2.5-flash","gemini-2.5-pro"},
            {"gpt-4o-mini","gpt-4.1-mini"},
            {"deepseek-chat","deepseek-reasoner"},
            {"openai/gpt-4o-mini","google/gemini-2.5-flash","deepseek/deepseek-chat"},
            {"llama3.2","qwen2.5"}, {"custom-model"}};
    private PrefsManager prefs; private AutoCompleteTextView provider, model; private EditText key,url,tgToken,tgAllow,waToken,waPhone,waAllow;
    private SwitchMaterial speak,fallback,bio;
    @Override protected void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_settings);
        prefs=new PrefsManager(this); provider=findViewById(R.id.spPreset); model=findViewById(R.id.etModel); key=findViewById(R.id.etKey); url=findViewById(R.id.etUrl);
        speak=findViewById(R.id.swSpeak); fallback=findViewById(R.id.swFallback); bio=findViewById(R.id.swBio);
        tgToken=findViewById(R.id.etTelegramToken); tgAllow=findViewById(R.id.etTelegramAllow); waToken=findViewById(R.id.etWhatsappToken); waPhone=findViewById(R.id.etWhatsappPhone); waAllow=findViewById(R.id.etWhatsappAllow);
        provider.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,PROVIDERS));
        provider.setOnItemClickListener((p,v,pos,id)->applyProvider(pos));
        model.setOnClickListener(v->showModels()); load(); findViewById(R.id.btnSave).setOnClickListener(v->save()); findViewById(R.id.btnTest).setOnClickListener(v->Toast.makeText(this, prefs.apiKey().isEmpty()?"❌ API key kosong":"✅ Format key bersih — kirim pesan untuk test request",Toast.LENGTH_LONG).show());
    }
    private void applyProvider(int pos){ url.setText(URLS[pos]); model.setText(MODELS[pos][0]); model.setTag(MODELS[pos]); }
    private void showModels(){ Object tag=model.getTag(); String[] ms=tag instanceof String[]?(String[])tag:new String[]{prefs.model()}; new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Pilih model").setItems(ms,(d,w)->model.setText(ms[w])).show(); }
    private void load(){ key.setText(prefs.apiKey()); url.setText(prefs.baseUrl()); model.setText(prefs.model()); speak.setChecked(prefs.autoSpeak()); fallback.setChecked(prefs.fallbackOffline()); bio.setChecked(prefs.biometricLock()); tgToken.setText(prefs.telegramToken()); tgAllow.setText(prefs.telegramAllowlist()); waToken.setText(prefs.whatsappToken()); waPhone.setText(prefs.whatsappPhoneId()); waAllow.setText(prefs.whatsappAllowlist()); }
    private void save(){ prefs.apiKey(UniversalProvider.sanitizeApiKey(key.getText().toString())); prefs.baseUrl(url.getText().toString().trim()); prefs.model(model.getText().toString().trim()); prefs.autoSpeak(speak.isChecked()); prefs.fallbackOffline(fallback.isChecked()); prefs.biometricLock(bio.isChecked()); prefs.telegramToken(tgToken.getText().toString()); prefs.telegramAllowlist(tgAllow.getText().toString()); prefs.whatsappToken(waToken.getText().toString()); prefs.whatsappPhoneId(waPhone.getText().toString()); prefs.whatsappAllowlist(waAllow.getText().toString()); Toast.makeText(this,"✅ Konfigurasi tersimpan",Toast.LENGTH_SHORT).show(); finish(); }
}
