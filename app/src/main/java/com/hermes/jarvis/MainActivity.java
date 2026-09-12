package com.hermes.jarvis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hermes.jarvis.adapter.ChatAdapter;
import com.hermes.jarvis.ai.AIManager;
import com.hermes.jarvis.ai.AIResponse;
import com.hermes.jarvis.ai.CommandSafetyValidator;
import com.hermes.jarvis.ai.OfflineBrain;
import com.hermes.jarvis.ai.SystemPrompt;
import com.hermes.jarvis.ai.WeatherTool;
import com.hermes.jarvis.ai.WebSearchTool;
import com.hermes.jarvis.web.WebSearchManager;
import com.hermes.jarvis.automation.AutomationEngine;
import com.hermes.jarvis.automation.DailyReport;
import com.hermes.jarvis.automation.SmartAutomation;
import com.hermes.jarvis.core.BiometricGate;
import com.hermes.jarvis.core.ContactHelper;
import com.hermes.jarvis.core.CalendarTool;
import com.hermes.jarvis.core.CustomToolStore;
import com.hermes.jarvis.core.GitHubManager;
import com.hermes.jarvis.service.JarvisAccessibilityService;
import com.hermes.jarvis.core.DeviceController;
import com.hermes.jarvis.core.MemoryBank;
import com.hermes.jarvis.core.StatsTracker;
import com.hermes.jarvis.core.TerminalExecutor;
import com.hermes.jarvis.core.SessionStore;
import com.hermes.jarvis.model.Message;
import com.hermes.jarvis.service.NotificationReader;
import com.hermes.jarvis.service.WakeWordService;
import com.hermes.jarvis.ui.CameraVisionActivity;
import com.hermes.jarvis.ui.DashboardActivity;
import com.hermes.jarvis.ui.HudOverlayService;
import com.hermes.jarvis.ui.SoundFX;
import com.hermes.jarvis.utils.PrefsManager;
import com.hermes.jarvis.voice.VoiceEngine;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_DEPTH = 3;

    private ChatAdapter adapter;
    private EditText etInput;
    private ImageButton btnSend, btnMic;
    private TextView tvStatus;
    private RecyclerView rv;

    private AIManager ai;
    private MemoryBank memory;
    private VoiceEngine voice;
    private PrefsManager prefs;
    private TerminalExecutor terminal;
    private StatsTracker stats;

    private boolean processing = false;
    private boolean voiceMode = true;
    private boolean wakeOn = false, hudOn = false;
    private String pendingCallPhone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("J.A.R.V.I.S.");
            getSupportActionBar().setSubtitle("Hermes Agent Core v2.8 TITAN");
        }

        rv = findViewById(R.id.rvChat);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatus.setOnClickListener(v -> showProfiles());

        findViewById(R.id.quickDashboard).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.quickVision).setOnClickListener(v -> startActivity(new Intent(this, CameraVisionActivity.class)));
        findViewById(R.id.quickSkills).setOnClickListener(v -> startActivity(new Intent(this, SkillsActivity.class)));
        findViewById(R.id.quickLogs).setOnClickListener(v -> startActivity(new Intent(this, LogsActivity.class)));
        findViewById(R.id.quickSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.quickNew).setOnClickListener(v -> newChat());
        findViewById(R.id.quickSessions).setOnClickListener(v -> showSessions());
        findViewById(R.id.quickImage).setOnClickListener(v -> startActivity(new Intent(this, ImageGenerationActivity.class)));
        findViewById(R.id.quickWeb).setOnClickListener(v -> startActivity(new Intent(this, WebToolsActivity.class)));
        findViewById(R.id.btnMode).setOnClickListener(v -> {
            prefs.agentMode(!prefs.agentMode());
            updateModeButton();
            Toast.makeText(this, prefs.agentMode() ? "⚡ Agent mode aktif" : "💬 Chat mode aktif", Toast.LENGTH_SHORT).show();
        });

        prefs = new PrefsManager(this);
        terminal = TerminalExecutor.get();
        memory = new MemoryBank(this);
        ai = new AIManager(this, memory);
        voice = new VoiceEngine(this);
        stats = new StatsTracker(this);

        adapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);

        btnSend.setOnClickListener(v -> send());
        etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                boolean ok = s.toString().trim().length() > 0 && !processing;
                btnSend.setEnabled(ok);
                btnSend.setAlpha(ok ? 1f : 0.4f);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnMic.setOnClickListener(v -> {
            if (processing) return;
            voice.listen(new VoiceEngine.SpeechListener() {
                @Override public void onSpeech(String text) {
                    runOnUiThread(() -> {
                        etInput.setText(text);
                        send();
                    });
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "🎙️ " + msg, Toast.LENGTH_SHORT).show());
                }
            });
        });

        requestPerms();
        restoreOrWelcome();
        updateStatus();
        updateModeButton();
        handleTrigger(getIntent());

        if (prefs.biometricLock() && BiometricGate.available(this)) {
            BiometricGate.authenticate(this, "🔓 Buka JARVIS",
                    new BiometricGate.Result() {
                        @Override public void onSuccess() { }
                        @Override public void onFailed(String reason) {
                            Toast.makeText(MainActivity.this,
                                    "🔒 Terkunci: " + reason, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleTrigger(intent);
    }

    private void handleTrigger(Intent intent) {
        if (intent != null && intent.getBooleanExtra("voice_trigger", false)) {
            etInput.postDelayed(() -> {
                if (!processing) btnMic.performClick();
            }, 500);
        }
    }

    private void requestPerms() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS
        }, 1);
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
    }

    private void restoreOrWelcome() {
        List<SessionStore.Chat> saved = ai.currentMessages();
        if (!saved.isEmpty()) {
            for (SessionStore.Chat m : saved) {
                int type = "user".equals(m.role) ? Message.USER : Message.BOT;
                adapter.add(new Message(m.text, type));
            }
            return;
        }
        welcome();
    }

    private void welcome() {
        SoundFX.boot();
        adapter.add(new Message(
                "╔═══════════════════════════════╗\n"
                + "║   🤖 J.A.R.V.I.S. ONLINE      ║\n"
                + "║   Hermes Agent Core v2.8 TITAN      ║\n"
                + "╚═══════════════════════════════╝\n\n"
                + "Root: " + (terminal.hasRoot() ? "✅ YA" : "❌ TIDAK") + "\n"
                + "Provider: " + prefs.providerName() + "\n" +
                + "Memori: " + memory.size() + " fakta\n\n"
                + "Coba:\n"
                + "  \"telpon Budi\" (kontak)\n"
                + "  \"laporan pagi jam 7\" (auto-report)\n"
                + "  \"cuaca gimana?\" · \"nomor Andi apa?\"\n"
                + "  Menu 📊 Dashboard · 📷 Vision",
                Message.INFO));

        if (!ai.isConfigured()) {
            adapter.add(new Message(
                    "⚠️ API belum siap. ⚙️ AI → pilih provider → paste API key → TEST API + LOAD MODELS.",
                    Message.ERROR));
        }
    }

    private void updateStatus() {
        String s = (terminal.hasRoot() ? "● ROOT" : "● USER")
                + " | " + prefs.providerName()
                + " | " + prefs.model()
                + " | " + (prefs.agentMode() ? "⚡ AGENT" : "💬 CHAT")
                + (ai.isConfigured() ? "" : " | ⚠️ NO KEY");
        tvStatus.setText(s);
        tvStatus.setTextColor(ContextCompat.getColor(this,
                terminal.hasRoot() ? R.color.green : R.color.orange));
    }

    private void updateModeButton() {
        android.view.View v=findViewById(R.id.btnMode);
        if(v instanceof com.google.android.material.button.MaterialButton)
            ((com.google.android.material.button.MaterialButton)v).setText(prefs.agentMode() ? "⚡ Agent" : "💬 Chat");
    }

    private void send() {
        String input = etInput.getText().toString().trim();
        if (input.isEmpty() || processing) return;
        etInput.setText("");
        SoundFX.send();
        stats.incMessage();
        adapter.add(new Message(input, Message.USER));
        dispatch(input, 0, null);
    }

    private void dispatch(String userText, int depth, String feedback) {
        processing = true;
        refreshSendBtn();
        if (depth == 0 && feedback == null) {
            adapter.add(new Message("🧠 " + prefs.model() + " berpikir...", Message.INFO));
        }

        ai.send(userText, feedback, new AIManager.Callback() {
            @Override public void onResponse(AIResponse resp) {
                stats.incResponse();
                runOnUiThread(() -> handleAIResponse(resp, userText, depth));
            }
            @Override public void onError(String err) {
                runOnUiThread(() -> {
                    SoundFX.alert();
                    adapter.add(new Message("❌ " + err, Message.ERROR));
                    if (prefs.fallbackOffline()) {
                        AIResponse local = OfflineBrain.handle(MainActivity.this, userText);
                        if (local != null) {
                            adapter.add(new Message(
                                    "🔄 Mode offline aktif (rule-based).", Message.INFO));
                            handleAIResponse(local, userText, MAX_DEPTH);
                            return;
                        }
                    }
                    finishTurn();
                });
            }
        });
    }

    private void handleAIResponse(AIResponse resp, String userText, int depth) {
        if (!prefs.agentMode() && resp.hasActions()) {
            adapter.add(new Message("💬 Chat mode: aksi/tool dinonaktifkan. Aktifkan ⚡ Agent jika ingin JARVIS menjalankan tugas.", Message.INFO));
            finishTurn();
            return;
        }
        if (resp.message != null && !resp.message.isEmpty()) {
            adapter.add(new Message(resp.message, Message.BOT));
        }
        if (resp.memoryKey != null && resp.memoryValue != null) {
            memory.remember(resp.memoryKey, resp.memoryValue);
            adapter.add(new Message("🧠 Memori: " + resp.memoryKey
                    + " = " + resp.memoryValue, Message.OK));
        }
        if (resp.createToolName != null && !resp.createToolName.trim().isEmpty() && resp.createToolCommand != null && !resp.createToolCommand.trim().isEmpty()) {
            CustomToolStore.Tool t = new CustomToolStore.Tool(); t.name=resp.createToolName.trim(); t.description=resp.createToolDescription==null?"":resp.createToolDescription.trim(); t.command=resp.createToolCommand.trim(); t.requiresRoot=resp.createToolRoot; ai.customTools().upsert(t); adapter.add(new Message("🧩 Tool tersimpan: "+t.name, Message.OK));
        }
        if (resp.useTool != null && !resp.useTool.trim().isEmpty()) { runCustomTool(resp.useTool.trim(), userText, depth); return; }
        if (resp.githubList != null && !resp.githubList.trim().isEmpty()) {
            adapter.add(new Message("🐙 Mengakses GitHub: "+resp.githubList, Message.INFO));
            new GitHubManager().listRepo(prefs.githubToken(), prefs.githubRepo(), prefs.githubBranch(), new GitHubManager.Callback(){ public void ok(String t){runOnUiThread(()->{adapter.add(new Message(t,Message.TERM)); if(depth<MAX_DEPTH)dispatch(userText,depth+1,"[GITHUB]\n"+t);else finishTurn();});} public void err(String e){runOnUiThread(()->{adapter.add(new Message("❌ GitHub: "+e,Message.ERROR));finishTurn();});}}); return;
        }
        if (resp.calendarTitle != null && !resp.calendarTitle.trim().isEmpty() && resp.calendarStartMs > 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_CALENDAR,Manifest.permission.READ_CALENDAR},88); adapter.add(new Message("📅 Izinkan akses Kalender lalu ulangi perintah.",Message.INFO)); finishTurn(); return; }
            long end=resp.calendarEndMs>resp.calendarStartMs?resp.calendarEndMs:resp.calendarStartMs+3600000L; String result=CalendarTool.addEvent(this,resp.calendarTitle,resp.calendarStartMs,end,resp.calendarNote); adapter.add(new Message(result, result.startsWith("✅")?Message.OK:Message.ERROR)); if(depth<MAX_DEPTH&&result.startsWith("❌")){} finishTurn(); return;
        }
        if (resp.scheduleMessage != null && resp.scheduleMinutes > 0) {
            AutomationEngine.schedule(this, resp.scheduleMinutes, resp.scheduleMessage);
            adapter.add(new Message("⏰ Reminder " + resp.scheduleMinutes
                    + " menit: " + resp.scheduleMessage, Message.OK));
        }

        if (resp.autoCreate != null) {
            adapter.add(new Message(SmartAutomation.add(this, resp.autoCreate),
                    Message.OK));
            SoundFX.reply();
        }
        if (resp.autoDeleteName != null) {
            adapter.add(new Message(
                    SmartAutomation.remove(this, resp.autoDeleteName), Message.INFO));
        }

        if (resp.reportDisable) {
            DailyReport.cancel(this);
            adapter.add(new Message("🌅 Laporan harian DIMATIKAN", Message.INFO));
        } else if (resp.reportHour >= 0) {
            DailyReport.schedule(this, resp.reportHour, resp.reportMinute);
            adapter.add(new Message(String.format(Locale.US,
                    "🌅 Laporan harian AKTIF setiap jam %02d:%02d\n"
                    + "Isi: baterai, cuaca, storage, notifikasi terbaru.",
                    resp.reportHour, resp.reportMinute), Message.OK));
            SoundFX.reply();
        }

        if (resp.contactSearch != null) {
            String res = ContactHelper.searchText(this, resp.contactSearch);
            adapter.add(new Message(res, Message.TERM));
            if (depth < MAX_DEPTH) {
                dispatch(userText, depth + 1, "[CONTACTS]\n" + res + "\n\n"
                        + SystemPrompt.feedbackInstruction(MAX_DEPTH));
            } else finishTurn();
            return;
        }

        if (resp.callContact != null) {
            handleCall(resp.callContact);
            return;
        }

        if (resp.weatherLocation != null) {
            stats.incWeather();
            adapter.add(new Message("🌤️ Cek cuaca"
                    + (resp.weatherLocation.isEmpty() ? " (GPS)..." : ": "
                    + resp.weatherLocation), Message.INFO));
            WeatherTool.fetch(this, resp.weatherLocation,
                    new WeatherTool.WeatherCallback() {
                        @Override public void onResult(String formatted) {
                            runOnUiThread(() -> {
                                adapter.add(new Message(formatted, Message.TERM));
                                if (depth < MAX_DEPTH) {
                                    dispatch(userText, depth + 1, "[WEATHER]\n"
                                            + formatted + "\n\n"
                                            + SystemPrompt.feedbackInstruction(MAX_DEPTH));
                                } else finishTurn();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                adapter.add(new Message("❌ Cuaca: " + e, Message.ERROR));
                                finishTurn();
                            });
                        }
                    });
            return;
        }

        if (!resp.webSearches.isEmpty() || !resp.newsSearches.isEmpty() || !resp.imageSearches.isEmpty() || !resp.webOpens.isEmpty()) {
            runWebTools(resp, userText, depth);
            return;
        }

        if (resp.replyPackage != null && resp.replyMessage != null) {
            String app = NotificationReader.appName(this, resp.replyPackage);
            new AlertDialog.Builder(this)
                    .setTitle("Kirim balasan?")
                    .setMessage("Ke: " + app + "\n\n\"" + resp.replyMessage + "\"")
                    .setPositiveButton("✅ Kirim", (d, w) -> {
                        String r = NotificationReader.reply(this,
                                resp.replyPackage, resp.replyMessage);
                        adapter.add(new Message(r,
                                r.startsWith("✅") ? Message.OK : Message.ERROR));
                        voice.speak(r.startsWith("✅") ? "Terkirim." : "Gagal.");
                        finishTurn();
                    })
                    .setNegativeButton("❌ Batal", (d, w) -> finishTurn())
                    .setCancelable(false).show();
            return;
        }

        for (String action : resp.deviceActions) {
            if (action.equals("notif_list")) {
                adapter.add(new Message(NotificationReader.snapshotText(), Message.TERM));
                continue;
            }
            String result = executeUiAction(action);
            if (result == null) result = DeviceController.execute(this, action);
            adapter.add(new Message("🔧 " + action + " → " + result, Message.INFO));
        }

        boolean hasCommands = resp.commands != null && !resp.commands.isEmpty();
        if (!hasCommands) {
            if (voiceMode) {
                String sp = resp.speakText != null ? resp.speakText : resp.message;
                if (sp != null && !sp.isEmpty()) voice.speak(sp);
            }
            SoundFX.reply();
            finishTurn();
            return;
        }

        CommandSafetyValidator.validate(resp.commands,
                new CommandSafetyValidator.Callback() {
                    @Override public void onApproved(List<String> cmds) {
                        runSequence(cmds, 0, new StringBuilder(), output ->
                                runOnUiThread(() -> {
                                    if (depth < MAX_DEPTH) {
                                        dispatch(userText, depth + 1,
                                                buildFeedback(output));
                                    } else finishTurn();
                                }));
                    }
                    @Override public void onConfirmation(String warn, List<String> cmds) {
                        runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Konfirmasi")
                                .setMessage(warn)
                                .setPositiveButton("✅ Jalankan", (d, w) ->
                                        runSequence(cmds, 0, new StringBuilder(), out ->
                                                runOnUiThread(() -> {
                                                    if (depth < MAX_DEPTH) {
                                                        dispatch(userText, depth + 1,
                                                                buildFeedback(out));
                                                    } else finishTurn();
                                                })))
                                .setNegativeButton("❌ Batal", (d, w) -> finishTurn())
                                .setCancelable(false).show());
                    }
                    @Override public void onBlocked(String reason) {
                        runOnUiThread(() -> {
                            adapter.add(new Message(reason, Message.ERROR));
                            finishTurn();
                        });
                    }
                });
    }

    private String executeUiAction(String action) {
        if (action == null || !action.startsWith("ui_")) return null;
        if (action.startsWith("ui_tap_")) return JarvisAccessibilityService.tapText(action.substring(7));
        if (action.startsWith("ui_type_")) return JarvisAccessibilityService.typeText(action.substring(8));
        if (action.equals("ui_back")) return JarvisAccessibilityService.back();
        if (action.equals("ui_home")) return JarvisAccessibilityService.home();
        if (action.equals("ui_recents")) return JarvisAccessibilityService.recents();
        if (action.equals("ui_scroll_forward")) return JarvisAccessibilityService.scroll(true);
        if (action.equals("ui_scroll_backward")) return JarvisAccessibilityService.scroll(false);
        return "❌ UI action tidak dikenal";
    }

    private void runCustomTool(String name, String userText, int depth) {
        for (CustomToolStore.Tool t : ai.customTools().all()) if (t.name.equalsIgnoreCase(name)) {
            CommandSafetyValidator.validate(java.util.Collections.singletonList(t.command), new CommandSafetyValidator.Callback(){
                public void onApproved(List<String> cmds){ terminal.run(cmds.get(0),t.requiresRoot,new TerminalExecutor.Callback(){ public void onOutput(String line){} public void onError(String line){} public void onComplete(int code,String out){runOnUiThread(()->{adapter.add(new Message("🧩 "+t.name+" → "+out,Message.TERM));if(depth<MAX_DEPTH)dispatch(userText,depth+1,buildFeedback(out));else finishTurn();}); }}); }
                public void onConfirmation(String warn,List<String> cmds){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Jalankan tool "+t.name+"?").setMessage(warn).setPositiveButton("Jalankan",(d,w)->terminal.run(cmds.get(0),t.requiresRoot,new TerminalExecutor.Callback(){ public void onOutput(String line){} public void onError(String line){} public void onComplete(int code,String out){runOnUiThread(()->{adapter.add(new Message(out,Message.TERM));finishTurn();}); }} )).setNegativeButton("Batal",(d,w)->finishTurn()).show());}
                public void onBlocked(String reason){runOnUiThread(()->{adapter.add(new Message(reason,Message.ERROR));finishTurn();});}
            }); return;
        }
        adapter.add(new Message("❌ Tool tidak ditemukan: "+name,Message.ERROR)); finishTurn();
    }

    private void handleCall(String name) {
        if (!ContactHelper.hasPermission(this)) {
            adapter.add(new Message("❌ Izin kontak belum diberikan. "
                    + "Buka ulang app dan izinkan kontak.", Message.ERROR));
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, 3);
            finishTurn();
            return;
        }
        List<ContactHelper.Contact> list = ContactHelper.search(this, name);
        if (list.isEmpty()) {
            adapter.add(new Message("❌ Kontak \"" + name + "\" tidak ditemukan di HP.",
                    Message.ERROR));
            finishTurn();
            return;
        }
        if (list.size() == 1) {
            confirmCall(list.get(0));
        } else {
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++)
                items[i] = list.get(i).name + " — " + list.get(i).phone;
            new AlertDialog.Builder(this)
                    .setTitle("Pilih kontak \"" + name + "\"")
                    .setItems(items, (d, w) -> confirmCall(list.get(w)))
                    .setNegativeButton("Batal", (d, w) -> finishTurn())
                    .setCancelable(false)
                    .show();
        }
    }

    private void confirmCall(ContactHelper.Contact c) {
        new AlertDialog.Builder(this)
                .setTitle("📞 Hubungi " + c.name + "?")
                .setMessage("Nomor: " + c.phone)
                .setPositiveButton("📞 Panggil", (d, w) -> doCall(c.phone))
                .setNegativeButton("Batal", (d, w) -> finishTurn())
                .setCancelable(false)
                .show();
    }

    private void doCall(String phone) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone)));
                stats.incCall();
                adapter.add(new Message("📞 Memanggil " + phone, Message.OK));
            } else {
                pendingCallPhone = phone;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE}, 77);
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                adapter.add(new Message("📱 Dialer dibuka. Izin panggil diminta — "
                        + "setelah diberikan, panggilan berikutnya langsung otomatis.",
                        Message.INFO));
            }
        } catch (Exception e) {
            adapter.add(new Message("❌ " + e.getMessage(), Message.ERROR));
        }
        finishTurn();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        if (requestCode == 77 && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED
                && pendingCallPhone != null) {
            Toast.makeText(this, "✅ Izin telepon diberikan! Coba ulangi perintah tadi.",
                    Toast.LENGTH_LONG).show();
            pendingCallPhone = null;
        }
    }

    private void runWebTools(AIResponse resp, String userText, int depth) {
        WebSearchManager web = new WebSearchManager(this);
        new Thread(() -> {
            StringBuilder feedback = new StringBuilder("[WEB TOOLS RESULT]\n");
            try {
                for (String q : resp.webSearches) {
                    stats.incSearch();
                    runOnUiThread(() -> adapter.add(new Message("🌐 Web search: " + q, Message.INFO)));
                    List<WebSearchManager.Result> rs = web.searchResults(q, 6, "web");
                    feedback.append("\n[SEARCH: ").append(q).append("]\n");
                    for (WebSearchManager.Result r : rs) {
                        feedback.append(r.title).append("\n").append(r.url).append("\n").append(r.description).append("\n\n");
                    }
                    final String formatted = web.search(q, 6);
                    runOnUiThread(() -> adapter.add(new Message(formatted, Message.TERM)));
                }
                for (String q : resp.newsSearches) {
                    stats.incSearch();
                    runOnUiThread(() -> adapter.add(new Message("📰 News search: " + q, Message.INFO)));
                    List<WebSearchManager.Result> rs = web.searchResults(q, 6, "news");
                    feedback.append("\n[NEWS: ").append(q).append("]\n");
                    for (WebSearchManager.Result r : rs) feedback.append("• ").append(r.title).append("\n").append(r.url).append("\n").append(r.description).append("\n\n");
                    final String formatted = web.news(q, 6);
                    runOnUiThread(() -> adapter.add(new Message(formatted, Message.TERM)));
                }
                for (String url : resp.webOpens) {
                    runOnUiThread(() -> adapter.add(new Message("📖 Membaca: " + url, Message.INFO)));
                    String text = web.scrape(url);
                    feedback.append("\n[OPEN/SCRAPE: ").append(url).append("]\n").append(text).append("\n");
                    String shown = text.length() > 7000 ? text.substring(0, 7000) + "\n[…dipotong…]" : text;
                    runOnUiThread(() -> adapter.add(new Message(shown, Message.TERM)));
                }
                for (String q : resp.imageSearches) {
                    stats.incSearch();
                    runOnUiThread(() -> adapter.add(new Message("🖼️ Mencari gambar: " + q, Message.INFO)));
                    List<WebSearchManager.ImageResult> imgs = web.images(q, 6);
                    feedback.append("\n[IMAGES: ").append(q).append("]\n");
                    for (WebSearchManager.ImageResult im : imgs) {
                        if (im.url == null || im.url.isEmpty()) continue;
                        String title = im.title == null || im.title.isEmpty() ? "Hasil gambar" : im.title;
                        String line = "🖼️ " + title + "\n" + im.url;
                        runOnUiThread(() -> adapter.add(new Message(line, Message.IMAGE, im.url)));
                        feedback.append(title).append("\n").append(im.url).append("\n");
                    }
                }
                String result = feedback.append("\n").append(SystemPrompt.feedbackInstruction(MAX_DEPTH)).toString();
                runOnUiThread(() -> { if (depth < MAX_DEPTH) dispatch(userText, depth + 1, result); else finishTurn(); });
            } catch (Exception e) {
                runOnUiThread(() -> { adapter.add(new Message("❌ Web: " + e.getMessage(), Message.ERROR)); finishTurn(); });
            }
        }).start();
    }

    private interface SeqDone { void onDone(String allOutput); }

    private void runSequence(List<String> cmds, int idx, StringBuilder sb, SeqDone done) {
        if (idx >= cmds.size()) { done.onDone(sb.toString()); return; }
        stats.incCommand();
        String cmd = cmds.get(idx);
        adapter.add(new Message("$ " + cmd, Message.TERM));

        terminal.run(cmd, new TerminalExecutor.Callback() {
            @Override public void onOutput(String line) {
                runOnUiThread(() -> sb.append(line).append('\n'));
            }
            @Override public void onError(String line) {
                runOnUiThread(() -> {
                    sb.append("[ERR] ").append(line).append('\n');
                    adapter.add(new Message("  ⚠ " + line, Message.ERROR));
                });
            }
            @Override public void onComplete(int code, String full) {
                runOnUiThread(() -> {
                    adapter.add(new Message(full + "\n[exit " + code + "]"
                            + (code == 0 ? " ✅" : " ⚠️"), Message.TERM));
                    if (full.equals("__CLEAR__")) adapter.clear();
                    runSequence(cmds, idx + 1, sb, done);
                });
            }
        });
    }

    private String buildFeedback(String output) {
        String tail = output.length() > 1800
                ? "...\n" + output.substring(output.length() - 1800) : output;
        if (tail.trim().isEmpty()) tail = "(tidak ada output — exit 0)";
        return "[TERMINAL FEEDBACK]\n" + tail + "\n\n"
                + SystemPrompt.feedbackInstruction(MAX_DEPTH);
    }

    private void finishTurn() {
        processing = false;
        refreshSendBtn();
    }

    private void refreshSendBtn() {
        boolean ok = etInput.getText().toString().trim().length() > 0 && !processing;
        btnSend.setEnabled(ok);
        btnSend.setAlpha(ok ? 1f : 0.4f);
        etInput.setEnabled(!processing);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
        } else if (id == R.id.action_vision) {
            startActivity(new Intent(this, CameraVisionActivity.class));
        } else if (id == R.id.action_auto) {
            showAutomations();
        } else if (id == R.id.action_wake) {
            toggleWakeWord();
        } else if (id == R.id.action_hud) {
            toggleHud();
        } else if (id == R.id.action_notif_perm) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } else if (id == R.id.action_voice) {
            voiceMode = !voiceMode;
            if (!voiceMode) voice.stopSpeaking();
            Toast.makeText(this, voiceMode ? "🔊 Suara ON" : "🔇 OFF",
                    Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_clear) {
            newChat();
        } else if (id == R.id.action_new_chat) {
            newChat();
        } else if (id == R.id.action_sessions) {
            showSessions();
        } else if (id == R.id.action_image) {
            startActivity(new Intent(this, ImageGenerationActivity.class));
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle("J.A.R.V.I.S. — v2.5 TITAN+")
                    .setMessage("Modul lengkap:\n"
                            + "• Agent loop + memori persisten + live context\n"
                            + "• Terminal root + safety + offline fallback\n"
                            + "• Voice + wake word + notifikasi + web + cuaca\n"
                            + "• Vision + automasi + Arc HUD\n"
                            + "• Kontak & telepon + laporan harian\n"
                            + "• Dashboard + kunci biometrik\n\n"
                            + "Model: " + prefs.model()
                            + "\nRoot: " + (terminal.hasRoot() ? "Ya" : "Tidak"))
                    .setPositiveButton("OK", null).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProfiles() {
        List<com.hermes.jarvis.core.ProviderProfileStore.Profile> ps = prefs.profileStore().all();
        String[] items = new String[ps.size()];
        String active = prefs.profileStore().active() == null ? "" : prefs.profileStore().active().id;
        for (int i=0;i<ps.size();i++) items[i]=(ps.get(i).id.equals(active)?"● ":"")+ps.get(i).name+" — "+ps.get(i).model;
        new AlertDialog.Builder(this).setTitle("AI Profiles").setItems(items,(d,w)->{prefs.profileStore().active(ps.get(w).id);updateStatus();Toast.makeText(this,"⚡ "+ps.get(w).name+" aktif",Toast.LENGTH_SHORT).show();}).setPositiveButton("Settings",(d,w)->startActivity(new Intent(this,SettingsActivity.class))).show();
    }

    private void newChat() {
        ai.newSession("New chat");
        adapter.clear();
        welcome();
        Toast.makeText(this, "✨ Chat baru", Toast.LENGTH_SHORT).show();
    }

    private void showSessions() {
        List<SessionStore.Session> ss = ai.store().all();
        String[] items = new String[ss.size()];
        SessionStore.Session cur = ai.store().current();
        for (int i=0;i<ss.size();i++) items[i] = (ss.get(i).id.equals(cur==null?"":cur.id)?"● ":"") + ss.get(i).title;
        new AlertDialog.Builder(this).setTitle("Chats / Sessions").setItems(items,(d,w)->{
            ai.switchSession(ss.get(w).id); adapter.clear(); restoreOrWelcome();
        }).setNeutralButton("+ New chat",(d,w)->newChat()).show();
    }

    private void showAutomations() {
        List<SmartAutomation.Rule> rules = SmartAutomation.list(this);
        if (rules.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("⏰ Automasi")
                    .setMessage("Belum ada automasi.\n\n"
                            + "• \"buat automasi tiap hari jam 7 cek baterai\"\n"
                            + "• \"laporan pagi jam 6:30\" (laporan harian)")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] items = new String[rules.size()];
        for (int i = 0; i < rules.size(); i++)
            items[i] = rules.get(i).name + " — " + SmartAutomation.describe(rules.get(i));
        new AlertDialog.Builder(this)
                .setTitle("⏰ Automasi (" + rules.size() + ") — tap untuk hapus")
                .setItems(items, (d, w) -> new AlertDialog.Builder(this)
                        .setMessage("Hapus \"" + rules.get(w).name + "\"?")
                        .setPositiveButton("Hapus", (d2, w2) -> {
                            SmartAutomation.remove(this, rules.get(w).name);
                            Toast.makeText(this, "🗑️ Dihapus", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Batal", null).show())
                .setNeutralButton("Tutup", null).show();
    }

    private void toggleWakeWord() {
        Intent i = new Intent(this, WakeWordService.class);
        if (wakeOn) {
            i.setAction("STOP");
            startService(i);
            wakeOn = false;
            Toast.makeText(this, "🎯 Wake Word OFF", Toast.LENGTH_SHORT).show();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin mic diperlukan", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
            startForegroundService(i);
            wakeOn = true;
            Toast.makeText(this, "🎯 ON — bilang \"Jarvis!\"", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleHud() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Izin Overlay")
                    .setMessage("Aktifkan 'Tampil di atas aplikasi lain' lalu ulangi.")
                    .setPositiveButton("Buka Pengaturan", (d, w) ->
                            startActivity(new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("Batal", null).show();
            return;
        }
        Intent i = new Intent(this, HudOverlayService.class);
        if (hudOn) {
            i.setAction("STOP");
            startService(i);
            hudOn = false;
        } else {
            startService(i);
            hudOn = true;
        }
        Toast.makeText(this, hudOn ? "⚛️ Arc HUD ON" : "⚛️ OFF",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        SmartAutomation.rescheduleAll(this);
        DailyReport.rescheduleIfEnabled(this);
    }

    @Override protected void onDestroy() { voice.release(); super.onDestroy(); }
}
