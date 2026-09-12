package com.hermes.jarvis.core;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Connects the Android UI to an optional Baileys bridge. Baileys itself runs in Node.js. */
public class WhatsAppBridge {
    public interface Callback{void ok(String text);void err(String e);}
    private final OkHttpClient client=new OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build();
    public void pair(String bridge,String phone,Callback cb){
        if(bridge==null||bridge.trim().isEmpty()){cb.err("Isi Bridge URL Baileys dulu.");return;}
        try{JSONObject o=new JSONObject();o.put("phone",phone);Request req=new Request.Builder().url(bridge.replaceAll("/$","")+"/pair/code").post(RequestBody.create(o.toString(),MediaType.parse("application/json"))).build();client.newCall(req).enqueue(new okhttp3.Callback(){public void onFailure(Call c,IOException e){cb.err(e.getMessage());}public void onResponse(Call c,Response r)throws IOException{try(r){String s=r.body()==null?"":r.body().string();if(!r.isSuccessful()){cb.err("HTTP "+r.code()+": "+s);return;}cb.ok(s);}}});}catch(Exception e){cb.err(e.getMessage());}
    }
}
