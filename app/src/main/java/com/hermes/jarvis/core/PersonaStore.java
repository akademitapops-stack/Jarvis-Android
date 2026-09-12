package com.hermes.jarvis.core;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** User-editable markdown personality/workspace files. */
public class PersonaStore {
    private final File dir;
    public PersonaStore(Context c){dir=new File(c.getFilesDir(),"jarvis_workspace");if(!dir.exists())dir.mkdirs();seed("memory.md","# JARVIS Memory\n\nWrite durable facts here.\n");seed("soul.md","# JARVIS Soul\n\nBe useful, honest, concise and protective of user intent.\n");seed("rules.md","# Rules\n\nUser-defined behavior rules go here.\n");seed("profile.md","# User Profile\n\nAdd preferences and non-sensitive context here.\n");}
    private void seed(String n,String d){File f=new File(dir,n);if(!f.exists())write(n,d);}
    public String read(String n){try{return new String(Files.readAllBytes(new File(dir,safe(n)).toPath()),StandardCharsets.UTF_8);}catch(Exception e){return "";}}
    public void write(String n,String s){try{Files.write(new File(dir,safe(n)).toPath(),(s==null?"":s).getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}
    public File file(String n){return new File(dir,safe(n));}
    private String safe(String n){if(n==null||n.trim().isEmpty())n="notes.md";return n.replaceAll("[^A-Za-z0-9._-]","_");}
    public String prompt(){return "=== memory.md ===\n"+read("memory.md")+"\n=== soul.md ===\n"+read("soul.md")+"\n=== rules.md ===\n"+read("rules.md")+"\n=== profile.md ===\n"+read("profile.md");}
}
