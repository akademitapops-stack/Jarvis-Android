package com.hermes.jarvis.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundFX {

    private static ToneGenerator tg;

    private static synchronized ToneGenerator tg() {
        if (tg == null) {
            try { tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70); }
            catch (Exception e) { tg = null; }
        }
        return tg;
    }

    private static void play(int tone) {
        ToneGenerator g = tg();
        if (g != null) {
            try { g.startTone(tone, 150); } catch (Exception ignored) {}
        }
    }

    public static void send()  { play(ToneGenerator.TONE_PROP_BEEP); }
    public static void reply() { play(ToneGenerator.TONE_PROP_ACK); }
    public static void alert() { play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD); }
    public static void boot()  { play(ToneGenerator.TONE_CDMA_CONFIRM); }
    public static void tap()   { play(ToneGenerator.TONE_PROP_BEEP2); }
}
