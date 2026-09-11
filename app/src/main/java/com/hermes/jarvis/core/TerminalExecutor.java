package com.hermes.jarvis.core;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TerminalExecutor {

    public interface Callback {
        void onOutput(String line);
        void onError(String line);
        void onComplete(int exitCode, String fullOutput);
    }

    private static final String TAG = "Terminal";
    private static TerminalExecutor inst;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String cwd = "/sdcard";
    private Boolean rootCache = null;

    public static synchronized TerminalExecutor get() {
        if (inst == null) inst = new TerminalExecutor();
        return inst;
    }

    public boolean hasRoot() {
        if (rootCache != null) return rootCache;
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su",
                "/su/bin/su", "/data/adb/magisk", "/data/local/xbin/su"};
        for (String p : paths) {
            if (new File(p).exists()) { rootCache = true; return true; }
        }
        try {
            Process pr = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String out = r.readLine();
            pr.waitFor(); r.close(); pr.destroy();
            rootCache = out != null && out.contains("uid=0");
        } catch (Exception e) { rootCache = false; }
        return rootCache;
    }

    public String cwd() { return cwd; }
    public void cwd(String v) { cwd = v; }

    public void run(String command, boolean forceRoot, Callback cb) {
        String cmd = command.trim();
        if (cmd.equals("pwd")) { done(cb, cwd, 0); return; }
        if (cmd.startsWith("cd ")) {
            String target = cmd.substring(3).trim();
            String np = target.startsWith("/")
                    ? target : new File(cwd, target).getAbsolutePath();
            File d = new File(np);
            if (d.isDirectory()) { cwd = d.getAbsolutePath(); done(cb, cwd, 0); }
            else done(cb, "cd: " + target + ": No such directory", 1);
            return;
        }
        if (cmd.equals("clear")) { done(cb, "__CLEAR__", 0); return; }

        final boolean root = forceRoot || cmd.startsWith("su ") || cmd.startsWith("sudo ");
        final String finalCmd = cmd.startsWith("su ") || cmd.startsWith("sudo ")
                ? cmd.replaceFirst("^(su|sudo)\\s+", "") : cmd;
        exec.execute(() -> {
            if (root && !hasRoot()) {
                postErr(cb, "Root tidak tersedia di device ini");
                postDone(cb, "", 1);
                return;
            }
            StringBuilder out = new StringBuilder();
            int code = -1;
            try {
                ProcessBuilder pb = new ProcessBuilder(root
                        ? new String[]{"su", "-c", finalCmd}
                        : new String[]{"sh", "-c", finalCmd});
                pb.directory(new File(cwd).exists() ? new File(cwd) : new File("/"));
                java.util.Map<String, String> env = pb.environment();
                env.put("PATH", "/system/bin:/system/xbin:/sbin:/vendor/bin:/data/local/bin");
                env.put("HOME", "/sdcard");
                env.put("TMPDIR", "/data/local/tmp");
                env.put("TERM", "xterm-256color");
                Process pr = pb.start();
                BufferedReader so = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                BufferedReader se = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                String line;
                while ((line = so.readLine()) != null) {
                    out.append(line).append('\n');
                    postOut(cb, line);
                }
                while ((line = se.readLine()) != null) {
                    out.append("[ERR] ").append(line).append('\n');
                    postErr(cb, line);
                }
                code = pr.waitFor();
                so.close(); se.close(); pr.destroy();
            } catch (Exception e) {
                String msg = "Error: " + e.getMessage();
                out.append(msg);
                postErr(cb, msg);
            }
            Log.d(TAG, "cmd done: " + finalCmd + " -> " + code);
            postDone(cb, out.toString(), code);
        });
    }

    public void run(String command, Callback cb) { run(command, false, cb); }

    /** Eksekusi sinkron (untuk background/laporan). Jangan panggil di main thread. */
    public static String sync(String command) {
        try {
            Process pr = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String l;
            while ((l = r.readLine()) != null) sb.append(l).append('\n');
            pr.waitFor(); r.close(); pr.destroy();
            return sb.toString().trim();
        } catch (Exception e) { return ""; }
    }

    private void done(Callback cb, String out, int code) {
        postOut(cb, out);
        postDone(cb, out, code);
    }
    private void postOut(Callback cb, String s) { main.post(() -> cb.onOutput(s)); }
    private void postErr(Callback cb, String s) { main.post(() -> cb.onError(s)); }
    private void postDone(Callback cb, String s, int c) { main.post(() -> cb.onComplete(c, s)); }
}
