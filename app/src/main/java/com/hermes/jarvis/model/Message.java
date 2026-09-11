package com.hermes.jarvis.model;

public class Message {
    public static final int USER = 0, BOT = 1, TERM = 2, ERROR = 3, INFO = 4, OK = 5;

    public final String text;
    public final int type;
    public final long time;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
        this.time = System.currentTimeMillis();
    }
}
