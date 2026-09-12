package com.hermes.jarvis.model;
public class Message {
    public static final int USER=0,BOT=1,TERM=2,ERROR=3,INFO=4,OK=5,IMAGE=6;
    public final String text; public final int type; public final long time; public final String imageUrl;
    public Message(String text,int type){this(text,type,null);} public Message(String text,int type,String imageUrl){this.text=text;this.type=type;this.imageUrl=imageUrl;this.time=System.currentTimeMillis();}
}
