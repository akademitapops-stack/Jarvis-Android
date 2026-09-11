package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;

public class StatsTracker {

    private final SharedPreferences p;

    public StatsTracker(Context c) {
        p = c.getSharedPreferences("jarvis_stats", Context.MODE_PRIVATE);
    }

    public void incMessage()  { p.edit().putInt("messages", messages() + 1).apply(); }
    public void incResponse() { p.edit().putInt("responses", responses() + 1).apply(); }
    public void incCommand()  { p.edit().putInt("commands", commands() + 1).apply(); }
    public void incSearch()   { p.edit().putInt("searches", searches() + 1).apply(); }
    public void incWeather()  { p.edit().putInt("weather", weatherCalls() + 1).apply(); }
    public void incCall()     { p.edit().putInt("calls", calls() + 1).apply(); }

    public int messages()  { return p.getInt("messages", 0); }
    public int responses() { return p.getInt("responses", 0); }
    public int commands()  { return p.getInt("commands", 0); }
    public int searches()  { return p.getInt("searches", 0); }
    public int weatherCalls() { return p.getInt("weather", 0); }
    public int calls()     { return p.getInt("calls", 0); }

    public void reset() { p.edit().clear().apply(); }

    public String summary() {
        return "Pesan user: " + messages()
                + "\nRespons AI: " + responses()
                + "\nCommand terminal: " + commands()
                + "\nPencarian web: " + searches()
                + "\nCek cuaca: " + weatherCalls()
                + "\nPanggilan: " + calls();
    }
}
