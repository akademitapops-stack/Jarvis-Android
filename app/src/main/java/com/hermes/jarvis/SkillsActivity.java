package com.hermes.jarvis;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SkillsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("JARVIS Skills");
        TextView v = new TextView(this);
        v.setTextColor(0xFFFFFFFF); v.setTextSize(15); v.setPadding(28, 28, 28, 28);
        v.setBackgroundColor(0xFF0D1117);
        v.setText("SKILL MATRIX\n\n"
                + "● Chat / Agent loop       READY\n"
                + "● Memory                  READY\n"
                + "● Web search              READY\n"
                + "● Weather                 READY\n"
                + "● Voice / TTS             READY\n"
                + "● Vision camera           READY\n"
                + "● Contacts / Dialer       READY\n"
                + "● Notifications           READY*\n"
                + "● Device controls         READY\n"
                + "● Terminal / root         READY*\n"
                + "● Automation / reminders  READY\n"
                + "● Dashboard / HUD         READY\n"
                + "● Telegram bridge         CONFIG-ONLY\n"
                + "● WhatsApp Cloud API      CONFIG-ONLY\n\n"
                + "* membutuhkan izin Android / root sesuai fitur.\n\n"
                + "JARVIS v2.4 TITAN");
        setContentView(v);
    }
}
