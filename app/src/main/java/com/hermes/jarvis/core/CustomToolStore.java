package com.hermes.jarvis.core;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/** User-created tool definitions. Commands still pass through the existing safety validator. */
public class CustomToolStore {
    public static class Tool { public String name="tool"; public String description=""; public String command=""; public boolean requiresRoot=false; }
    private final android.content.SharedPreferences p; private final Gson gson=new Gson(); private List<Tool> tools;
    public CustomToolStore(Context c){p=c.getSharedPreferences("jarvis_custom_tools",Context.MODE_PRIVATE);load();}
    private void load(){try{tools=gson.fromJson(p.getString("tools","[]"),new TypeToken<List<Tool>>(){}.getType());}catch(Exception e){tools=new ArrayList<>();}if(tools==null)tools=new ArrayList<>();}
    private void save(){p.edit().putString("tools",gson.toJson(tools)).apply();}
    public List<Tool> all(){return new ArrayList<>(tools);}
    public void upsert(Tool t){tools.removeIf(x->x.name.equalsIgnoreCase(t.name));tools.add(t);save();}
    public void delete(String name){tools.removeIf(x->x.name.equalsIgnoreCase(name));save();}
    public String prompt(){if(tools.isEmpty())return "(no custom tools)";StringBuilder s=new StringBuilder();for(Tool t:tools)s.append("• ").append(t.name).append(" — ").append(t.description).append(" | command: ").append(t.command).append(" | root=").append(t.requiresRoot).append('\n');return s.toString();}
}
