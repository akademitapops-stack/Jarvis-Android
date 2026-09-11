package com.hermes.jarvis.ai;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class OfflineBrain {

    public static AIResponse handle(Context ctx, String input) {
        String q = input.toLowerCase().trim();
        AIResponse r = new AIResponse();

        if (q.contains("jam") || q.contains("tanggal") || q.contains("hari")) {
            r.message = com.hermes.jarvis.core.ContextEngine.build(ctx);
            r.speakText = "Memeriksa waktu dan status perangkat.";
            return r;
        }
        if (q.contains("baterai") || q.contains("battery")) {
            r.deviceActions.add("battery");
            r.message = "Melaporkan status baterai.";
            return r;
        }
        if (q.contains("senter") || q.contains("flashlight")) {
            boolean on = !com.hermes.jarvis.core.DeviceController.isTorchOn();
            r.deviceActions.add(on ? "flashlight_on" : "flashlight_off");
            r.message = on ? "Menyalakan senter." : "Mematikan senter.";
            return r;
        }
        if (q.startsWith("buka ") || q.startsWith("open ")) {
            String app = q.replaceFirst("^(buka|open)\\s+", "");
            r.deviceActions.add("open_app_" + app);
            r.message = "Mencoba membuka " + app + "...";
            return r;
        }
        if (q.contains("volume naik")) { r.deviceActions.add("volume_up"); r.message = "Volume naik."; return r; }
        if (q.contains("volume turun")) { r.deviceActions.add("volume_down"); r.message = "Volume turun."; return r; }
        if (q.contains("bisukan") || q.contains("mute")) { r.deviceActions.add("mute"); r.message = "Dibisukan."; return r; }
        if (q.contains("getar")) { r.deviceActions.add("vibrate"); r.message = "Bergetar."; return r; }

        List<String> cmds = new ArrayList<>();
        if (q.startsWith("ls") || q.startsWith("pwd") || q.startsWith("cat ")
                || q.startsWith("df") || q.startsWith("ps") || q.startsWith("getprop")
                || q.startsWith("dumpsys") || q.startsWith("free") || q.startsWith("top")) {
            cmds.add(input.trim());
            r.commands = cmds;
            r.message = "Mode offline: mengeksekusi langsung.";
            return r;
        }
        return null;
    }
}
