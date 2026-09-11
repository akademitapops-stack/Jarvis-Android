package com.hermes.jarvis.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.core.app.NotificationCompat;

import com.hermes.jarvis.MainActivity;

import java.util.ArrayList;
import java.util.Locale;

public class WakeWordService extends Service {

    private static final String WAKE_WORD = "jarvis";
    private static final String CHANNEL_ID = "wake_word_channel";
    private static final int NOTIF_ID = 2001;

    private SpeechRecognizer recognizer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private long lastTrigger = 0;

    private final Runnable listenRunnable = this::startListening;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForegroundCompat();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setLanguage(Locale.getDefault());
                tts.setPitch(0.85f);
                tts.setSpeechRate(1.05f);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            shutdown();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!running) {
            running = true;
            handler.post(listenRunnable);
        }
        return START_STICKY;
    }

    private void startListening() {
        if (!running) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handler.postDelayed(listenRunnable, 10_000);
            return;
        }
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { }
            @Override public void onBeginningOfSpeech() { }
            @Override public void onRmsChanged(float rmsdB) { }
            @Override public void onBufferReceived(byte[] buffer) { }
            @Override public void onEndOfSpeech() { }
            @Override public void onPartialResults(Bundle partialResults) { }
            @Override public void onEvent(int eventType, Bundle params) { }

            @Override public void onResults(Bundle results) {
                if (!running) return;
                ArrayList<String> list = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null) {
                    for (String s : list) {
                        if (s != null && s.toLowerCase(Locale.getDefault())
                                .contains(WAKE_WORD)) {
                            onWakeWordDetected();
                            break;
                        }
                    }
                }
                scheduleRestart(400);
            }

            @Override public void onError(int error) {
                if (!running) return;
                long delay = (error == SpeechRecognizer.ERROR_NO_MATCH
                        || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) ? 250 : 1500;
                scheduleRestart(delay);
            }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizer.startListening(i);
    }

    private void scheduleRestart(long ms) {
        handler.removeCallbacks(listenRunnable);
        handler.postDelayed(listenRunnable, ms);
    }

    private void onWakeWordDetected() {
        long now = System.currentTimeMillis();
        if (now - lastTrigger < 5000) return;
        lastTrigger = now;
        say("Ya?");
        try {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("voice_trigger", true);
            startActivity(i);
        } catch (Exception ignored) {}
    }

    private void say(String text) {
        if (ttsReady && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null,
                    "wake_" + System.currentTimeMillis());
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Wake Word", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Deteksi kata 'Jarvis'");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void startForegroundCompat() {
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🎯 Jarvis Wake Word aktif")
                .setContentText("Berkata \"Jarvis\" untuk memanggil")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIF_ID, n);
        }
    }

    private void shutdown() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    @Override
    public void onDestroy() {
        shutdown();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
