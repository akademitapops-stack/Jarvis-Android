package com.hermes.jarvis.core;

import android.content.Context;
import android.os.Process;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Context ctx; private final Thread.UncaughtExceptionHandler previous;
    private CrashHandler(Context c) { ctx=c.getApplicationContext(); previous=Thread.getDefaultUncaughtExceptionHandler(); }
    public static void install(Context c) { Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(c)); }
    @Override public void uncaughtException(Thread t, Throwable e) {
        StringWriter sw=new StringWriter(); e.printStackTrace(new PrintWriter(sw));
        LogStore.add(ctx,"FATAL",sw.toString());
        if(previous!=null) previous.uncaughtException(t,e); else Process.killProcess(Process.myPid());
    }
}
