package com.hermes.jarvis.core;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GitHubManager {
    public interface Callback{void ok(String text);void err(String e);}
    private final OkHttpClient client=new OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build();
    public void listRepo(String token,String repo,String branch,Callback cb){
        String url="https://api.github.com/repos/"+repo+"/contents?ref="+(branch==null||branch.isEmpty()?"main":branch);
        Request.Builder b=new Request.Builder().url(url).header("Accept","application/vnd.github+json");if(token!=null&&!token.isEmpty())b.header("Authorization","Bearer "+token);
        client.newCall(b.build()).enqueue(new okhttp3.Callback(){public void onFailure(Call c,IOException e){cb.err(e.getMessage());}public void onResponse(Call c,Response r)throws IOException{try(r){String s=r.body()==null?"":r.body().string();if(!r.isSuccessful()){cb.err("HTTP "+r.code()+": "+s);return;}JSONArray a=new JSONArray(s);StringBuilder o=new StringBuilder("📦 "+repo+"/\n");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);o.append(x.optString("type")).append("  ").append(x.optString("name")).append('\n');}cb.ok(o.toString());}catch(Exception e){cb.err(e.getMessage());}}});
    }
}
