package com.hermes.jarvis.core;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;

import java.util.List;

public class DeviceController {

    private static boolean torchOn = false;

    public static String execute(Context ctx, String action) {
        try {
            if (action == null) return "action null";

            if (action.equals("flashlight_on"))  return setTorch(ctx, true);
            if (action.equals("flashlight_off")) return setTorch(ctx, false);

            if (action.startsWith("volume_")) {
                return setVolume(ctx, action.substring(7));
            }
            if (action.equals("mute")) return setVolume(ctx, "0");

            if (action.startsWith("brightness_")) {
                return setBrightness(ctx,
                        Integer.parseInt(action.substring(11)));
            }
            if (action.startsWith("open_app_")) {
                return openApp(ctx, action.substring(9));
            }
            if (action.startsWith("open_url_")) {
                String url = action.substring(9);
                if (!url.startsWith("http")) url = "https://" + url;
                ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "✅ Membuka " + url;
            }
            if (action.startsWith("dial_")) {
                ctx.startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + action.substring(5)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "✅ Membuka dialer";
            }
            if (action.equals("vibrate")) { vibrate(ctx); return "✅ Bergetar"; }
            if (action.equals("battery")) { return batteryReport(ctx); }
            if (action.equals("sensors")) { return sensorReport(ctx); }
            if (action.equals("wifi_panel")) {
                ctx.startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "✅ Panel jaringan dibuka";
            }

            return "❓ Action tidak dikenal: " + action;
        } catch (Exception e) {
            return "❌ " + e.getMessage();
        }
    }

    private static String setTorch(Context ctx, boolean on) {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cm.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash)
                        && Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(facing)) {
                    cm.setTorchMode(id, on);
                    torchOn = on;
                    return on ? "🔦 Senter ON" : "🔦 Senter OFF";
                }
            }
            return "❌ Flash tidak ditemukan";
        } catch (Exception e) { return "❌ Senter: " + e.getMessage(); }
    }

    public static boolean isTorchOn() { return torchOn; }

    private static String setVolume(Context ctx, String val) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int target;
        switch (val) {
            case "up":
                target = Math.min(am.getStreamVolume(AudioManager.STREAM_MUSIC) + 2, max);
                break;
            case "down":
                target = Math.max(am.getStreamVolume(AudioManager.STREAM_MUSIC) - 2, 0);
                break;
            default:
                try { target = (Integer.parseInt(val) * max) / 100; }
                catch (NumberFormatException e) { return "❌ volume tidak valid"; }
        }
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
        return "🔊 Volume: " + (target * 100 / max) + "%";
    }

    private static String setBrightness(Context ctx, int val) {
        try {
            if (!Settings.System.canWrite(ctx)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + ctx.getPackageName()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return "⚠️ Izinkan 'Modify system settings' dulu, lalu ulangi";
            }
            val = Math.max(1, Math.min(val, 255));
            Settings.System.putInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, val);
            return "☀️ Kecerahan: " + (val * 100 / 255) + "%";
        } catch (Exception e) { return "❌ Brightness: " + e.getMessage(); }
    }

    private static String openApp(Context ctx, String name) {
        PackageManager pm = ctx.getPackageManager();
        String q = name.toLowerCase().trim();
        for (android.content.pm.ApplicationInfo app : pm.getInstalledApplications(0)) {
            String label = pm.getApplicationLabel(app).toString().toLowerCase();
            if (label.contains(q) || app.packageName.contains(q)) {
                Intent launch = pm.getLaunchIntentForPackage(app.packageName);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(launch);
                    return "✅ Membuka " + pm.getApplicationLabel(app);
                }
            }
        }
        return "❌ Aplikasi \"" + name + "\" tidak ditemukan";
    }

    private static void vibrate(Context ctx) {
        Vibrator v;
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager)
                    ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            v = vm.getDefaultVibrator();
        } else {
            v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(400,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    public static String batteryReport(Context ctx) {
        Intent bat = ctx.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (bat == null) return "❌ Tidak bisa membaca baterai";
        int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = bat.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int health = bat.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        String h;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: h = "Baik"; break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: h = "Overheat ⚠️"; break;
            case BatteryManager.BATTERY_HEALTH_DEAD: h = "Rusak"; break;
            default: h = "Unknown";
        }
        return String.format("🔋 Baterai: %d%% | %s | Kesehatan: %s",
                level * 100 / Math.max(scale, 1),
                status == BatteryManager.BATTERY_STATUS_CHARGING ? "Charging" : "Discharge",
                h);
    }

    private static String sensorReport(Context ctx) {
        SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder("📡 Sensor (").append(sensors.size()).append("):\n");
        int i = 0;
        for (Sensor s : sensors) {
            sb.append("• ").append(s.getName()).append('\n');
            if (++i >= 15) { sb.append("... (dan lainnya)"); break; }
        }
        return sb.toString();
    }
}
