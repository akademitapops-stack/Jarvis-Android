package com.hermes.jarvis;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import com.hermes.jarvis.core.LogStore;

public class LogsActivity extends androidx.appcompat.app.AppCompatActivity {
    private TextView log;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setTitle("System Logs");
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(16,16,16,16); box.setBackgroundColor(0xFF0D1117);
        log = new TextView(this); log.setTextColor(0xFFB9D7FF); log.setTextSize(12); log.setTypeface(android.graphics.Typeface.MONOSPACE); log.setMovementMethod(new ScrollingMovementMethod());
        box.addView(log, new LinearLayout.LayoutParams(-1,0,1));
        Button clear = new Button(this); clear.setText("HAPUS LOG"); clear.setOnClickListener(v -> { LogStore.clear(this); refresh(); }); box.addView(clear);
        setContentView(box); refresh();
    }
    private void refresh(){ log.setText(LogStore.read(this)); log.post(() -> log.scrollTo(0, log.getBottom())); }
}
