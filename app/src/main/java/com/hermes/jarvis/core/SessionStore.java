package com.hermes.jarvis.core;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Persistent chat sessions so switching/new app launches do not erase conversation context. */
public class SessionStore {
    public static class Chat { public String role; public String text; public long time; public Chat(){} public Chat(String r,String t){role=r;text=t;time=System.currentTimeMillis();} }
    public static class Session { public String id=UUID.randomUUID().toString(); public String title="New chat"; public long created=System.currentTimeMillis(), updated=created; public List<Chat> messages=new ArrayList<>(); }
    private final SharedPreferences p; private final Gson gson=new Gson(); private List<Session> sessions;
    public SessionStore(Context c){p=c.getSharedPreferences("jarvis_sessions",Context.MODE_PRIVATE);load();if(sessions.isEmpty())create("New chat");}
    private void load(){try{sessions=gson.fromJson(p.getString("sessions","[]"),new TypeToken<List<Session>>(){}.getType());}catch(Exception e){sessions=new ArrayList<>();}if(sessions==null)sessions=new ArrayList<>();}
    private void save(){p.edit().putString("sessions",gson.toJson(sessions)).apply();}
    public List<Session> all(){List<Session>x=new ArrayList<>(sessions);Collections.sort(x, Comparator.comparingLong((Session s)->s.updated).reversed());return x;}
    public Session current(){String id=p.getString("active","" );for(Session s:sessions)if(s.id.equals(id))return s;return sessions.isEmpty()?null:sessions.get(0);}
    public Session create(String title){Session s=new Session();s.title=(title==null||title.trim().isEmpty())?"New chat":title.trim();sessions.add(s);p.edit().putString("active",s.id).apply();save();return s;}
    public void switchTo(String id){p.edit().putString("active",id).apply();}
    public void add(String role,String text){Session s=current();if(s==null)s=create("New chat");s.messages.add(new Chat(role,text));s.updated=System.currentTimeMillis();if("New chat".equals(s.title)&&"user".equals(role)){s.title=text.length()>34?text.substring(0,34)+"…":text;}save();}
    public void clearCurrent(){Session s=current();if(s!=null){s.messages.clear();s.updated=System.currentTimeMillis();save();}}
    public void delete(String id){sessions.removeIf(s->s.id.equals(id));if(sessions.isEmpty())create("New chat");else if(current()==null)switchTo(sessions.get(0).id);save();}
}
