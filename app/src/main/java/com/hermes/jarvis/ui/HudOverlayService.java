package com.hermes.jarvis.ui;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.hermes.jarvis.MainActivity;
import com.hermes.jarvis.R;

public class HudOverlayService extends Service {

    private WindowManager wm;
    private View bubble;
    private WindowManager.LayoutParams params;
    private TextView tvInfo;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            if (bubble == null || tvInfo == null) return;
            String ctx = com.hermes.jarvis.core.ContextEngine
                    .build(HudOverlayService.this);
            String time = line(ctx, "Waktu:");
            String battery = line(ctx, "Baterai:");
            String net = line(ctx, "Jaringan:");
            tvInfo.setText((time != null ? time + "\n" : "")
                    + (battery != null ? battery + "\n" : "")
                    + (net != null ? net : ""));
            handler.postDelayed(this, 15_000);
        }
    };

    private static String line(String src, String prefix) {
        for (String l : src.split("\n")) if (l.startsWith(prefix)) return l;
        return null;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            removeBubble();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble == null) showBubble();
        return START_STICKY;
    }

    private void showBubble() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 24; params.y = 300;

        bubble = LayoutInflater.from(this).inflate(R.layout.hud_arc, null);
        tvInfo = bubble.findViewById(R.id.tvHudInfo);
        ArcReactorView reactor = bubble.findViewById(R.id.arcView);
        if (reactor != null) reactor.start();

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float touchX, touchY; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x; startY = params.y;
                        touchX = event.getRawX(); touchY = event.getRawY();
                        moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchX);
                        int dy = (int) (event.getRawY() - touchY);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true;
                        params.x = startX + dx; params.y = startY + dy;
                        try { wm.updateViewLayout(bubble, params); }
                        catch (Exception ignored) {}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            SoundFX.tap();
                            try {
                                Intent i = new Intent(HudOverlayService.this,
                                        MainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                i.putExtra("voice_trigger", true);
                                startActivity(i);
                            } catch (Exception ignored) {}
                        }
                        return true;
                }
                return false;
            }
        });

        try { wm.addView(bubble, params); }
        catch (Exception e) { stopSelf(); return; }
        handler.post(updater);
    }

    private void removeBubble() {
        handler.removeCallbacksAndMessages(null);
        if (bubble != null && wm != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
    }

    @Override public void onDestroy() { removeBubble(); super.onDestroy(); }
}
