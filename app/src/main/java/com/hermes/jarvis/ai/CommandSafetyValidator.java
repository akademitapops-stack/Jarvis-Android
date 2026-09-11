package com.hermes.jarvis.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandSafetyValidator {

    public interface Callback {
        void onApproved(List<String> commands);
        void onConfirmation(String warning, List<String> commands);
        void onBlocked(String reason);
    }

    private static final List<String> BLOCKED = Arrays.asList(
            "rm -rf /", "rm -rf /*", "mkfs", ":(){", "dd if=/dev/zero of=/dev/block",
            "dd if=/dev/random of=/dev/block", "chmod -r 777 /system", "flash_image");

    private static final List<String> DANGEROUS = Arrays.asList(
            "rm -rf", "rm -r ", "reboot", "shutdown", "pm uninstall", "pm clear",
            "chmod 777", "dd ", "mount -o remount", "rm /system", "mv /system");

    public static void validate(List<String> commands, Callback cb) {
        boolean needsConfirm = false;
        List<String> flagged = new ArrayList<>();

        for (String c : commands) {
            String low = c.toLowerCase();
            for (String b : BLOCKED) {
                if (low.contains(b)) {
                    cb.onBlocked("🚫 DIBLOKIR: command mengandung pola merusak (\"" + b
                            + "\"). Jarvis menolak melakukannya demi keselamatan device.");
                    return;
                }
            }
            for (String d : DANGEROUS) {
                if (low.startsWith(d) || low.contains(" " + d)) {
                    needsConfirm = true;
                    flagged.add(c);
                    break;
                }
            }
        }

        if (needsConfirm) {
            cb.onConfirmation("⚠️ COMMAND BERISIKO TERDETEKSI:\n\n"
                    + String.join("\n", flagged)
                    + "\n\nJalankan tetap?", commands);
        } else {
            cb.onApproved(commands);
        }
    }
}
