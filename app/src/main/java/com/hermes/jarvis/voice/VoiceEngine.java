package com.hermes.jarvis.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceEngine {

    public interface SpeechListener {
        void onSpeech(String text);
        void onError(String message);
    }

    private final Context ctx;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    public float pitch = 0.85f;
    public float speed = 1.05f;

    public VoiceEngine(Context ctx) {
        this.ctx = ctx;
        tts = new TextToSpeech(ctx, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setLanguage(Locale.getDefault());
                tts.setPitch(pitch);
                tts.setSpeechRate(speed);
            }
        });
    }

    public void listen(SpeechListener listener) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            listener.onError("Speech recognition tidak tersedia. Install 'Google App'.");
            return;
        }
        SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(ctx);
        sr.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(ctx, "🎙️ Mendengarkan...", Toast.LENGTH_SHORT).show();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        msg = "Tidak terdengar suara"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        msg = "Waktu habis"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        msg = "Izin mic belum diberikan"; break;
                    default:
                        msg = "Error mic (" + error + ")";
                }
                listener.onError(msg);
                sr.destroy();
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> list = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null && !list.isEmpty()) listener.onSpeech(list.get(0));
                sr.destroy();
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        sr.startListening(intent);
    }

    public void speak(String text) {
        if (!ttsReady || tts == null || text == null) return;
        String clean = text.replaceAll("[^\\x20-\\x7E\\n]", " ").trim();
        if (clean.isEmpty()) return;
        Bundle params = new Bundle();
        tts.speak(clean, TextToSpeech.QUEUE_ADD, params,
                "jarvis_" + System.currentTimeMillis());
    }

    public void stopSpeaking() {
        if (tts != null) tts.stop();
    }

    public void release() {
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
    }
}
