package com.hermes.jarvis.web;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Patterns;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Brave + generic JSON web tools with bounded scraping and SSRF guards. */
public final class WebSearchManager {
    public static final String PROVIDER_BRAVE = "brave";
    public static final String PROVIDER_CUSTOM = "custom";
    private static final String PREFS = "jarvis_web";
    private final Context context;
    private final OkHttpClient client;

    public static final class Result { public String title="", url="", description=""; }
    public static final class ImageResult { public String title="", url="", source=""; }

    public WebSearchManager(Context context) {
        this.context=context.getApplicationContext();
        client=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS)
                .followRedirects(true).build();
    }
    private SharedPreferences prefs(){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private static String clean(String s){if(s==null)return "";return s.replace("\u200B","").replace("\u200C","").replace("\u200D","").replace("\u200E","").replace("\u200F","").replace("\u202A","").replace("\u202B","").replace("\u202C","").replace("\u202D","").replace("\u202E","").replace("\u2060","").replace("\uFEFF","").trim();}
    public void saveBrave(String apiKey){prefs().edit().putString("provider",PROVIDER_BRAVE).putString("brave_key",clean(apiKey)).apply();}
    public void saveCustom(String url,String apiKey){prefs().edit().putString("provider",PROVIDER_CUSTOM).putString("custom_url",clean(url)).putString("custom_key",clean(apiKey)).apply();}
    public String provider(){return prefs().getString("provider",PROVIDER_BRAVE);}
    public boolean configured(){return PROVIDER_BRAVE.equals(provider())?!prefs().getString("brave_key","").isEmpty():!prefs().getString("custom_url","").isEmpty();}

    public String search(String q,int count)throws IOException{return format(searchResults(q,count,"web"));}
    public String news(String q,int count)throws IOException{return format(searchResults(q,count,"news"));}
    public List<Result> searchResults(String q,int count,String type)throws IOException{
        if(clean(q).isEmpty())throw new IOException("Query kosong"); count=Math.max(1,Math.min(count,10));
        if(PROVIDER_BRAVE.equals(provider())) return braveResults(q,count,type);
        return customResults(q,count);
    }
    private List<Result> braveResults(String q,int count,String type)throws IOException{
        String key=prefs().getString("brave_key",""); if(key.isEmpty())throw new IOException("Brave API key belum diatur");
        String path="web".equals(type)?"web/search":"news/search";
        String url="https://api.search.brave.com/res/v1/"+path+"?q="+ Uri.encode(clean(q))+"&count="+count;
        Request req=new Request.Builder().url(url).header("Accept","application/json").header("X-Subscription-Token",key).get().build();
        try(Response r=client.newCall(req).execute()){String body=body(r);if(!r.isSuccessful())throw new IOException("Brave HTTP "+r.code());return parseBrave(body,type);}
    }
    private List<Result> customResults(String q,int count)throws IOException{
        String endpoint=prefs().getString("custom_url","");if(endpoint.isEmpty())throw new IOException("Custom search URL belum diatur");
        JSONObject p=new JSONObject();try{p.put("q",q);p.put("count",count);}catch(Exception e){throw new IOException(e);}
        Request.Builder b=new Request.Builder().url(endpoint).post(okhttp3.RequestBody.create(p.toString(),okhttp3.MediaType.parse("application/json")));
        String key=prefs().getString("custom_key","");if(!key.isEmpty())b.header("Authorization","Bearer "+clean(key));
        try(Response r=client.newCall(b.build()).execute()){String body=body(r);if(!r.isSuccessful())throw new IOException("Search HTTP "+r.code());return parseGeneric(body);}
    }
    private List<Result> parseBrave(String body,String type)throws IOException{
        try{JSONObject root=new JSONObject(body);JSONArray a;
            if("web".equals(type)){JSONObject web=root.optJSONObject("web");a=web==null?null:web.optJSONArray("results");}
            else {a=root.optJSONArray("results");if(a==null){JSONObject news=root.optJSONObject("news");a=news==null?null:news.optJSONArray("results");}}
            List<Result> out=new ArrayList<>();if(a==null)return out;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;Result z=new Result();z.title=x.optString("title","(tanpa judul)");z.url=x.optString("url","");z.description=x.optString("description","");out.add(z);}return out;
        }catch(Exception e){throw new IOException("Brave response parse error: "+e.getMessage(),e);}
    }
    private List<Result> parseGeneric(String body){List<Result> out=new ArrayList<>();try{JSONObject root=new JSONObject(body);JSONArray a=root.optJSONArray("results");if(a==null&&root.optJSONObject("web")!=null)a=root.optJSONObject("web").optJSONArray("results");if(a==null)return out;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;Result z=new Result();z.title=x.optString("title","");z.url=x.optString("url",x.optString("link",""));z.description=x.optString("description",x.optString("snippet",""));out.add(z);}}catch(Exception ignored){}return out;}
    private String format(List<Result> rs){StringBuilder s=new StringBuilder();for(int i=0;i<rs.size();i++){Result r=rs.get(i);s.append(i+1).append(". ").append(r.title).append("\n").append(r.url).append("\n").append(r.description).append("\n\n");}return s.toString().trim();}

    public List<ImageResult> images(String q,int count)throws IOException{
        String key=prefs().getString("brave_key","");if(key.isEmpty())throw new IOException("Image Search membutuhkan Brave API key");count=Math.max(1,Math.min(count,10));
        String url="https://api.search.brave.com/res/v1/images/search?q="+Uri.encode(clean(q))+"&count="+count;
        Request req=new Request.Builder().url(url).header("Accept","application/json").header("X-Subscription-Token",key).get().build();
        try(Response r=client.newCall(req).execute()){String body=body(r);if(!r.isSuccessful())throw new IOException("Brave Images HTTP "+r.code());JSONObject root=new JSONObject(body);JSONArray a=root.optJSONArray("results");List<ImageResult> out=new ArrayList<>();if(a==null)return out;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;ImageResult z=new ImageResult();z.title=x.optString("title","");JSONObject th=x.optJSONObject("thumbnail");z.url=th==null?"":th.optString("src","");JSONObject props=x.optJSONObject("properties");if(props!=null)z.source=props.optString("url","");if(z.url.isEmpty())z.url=z.source;out.add(z);}return out;}
        catch(Exception e){if(e instanceof IOException)throw (IOException)e;throw new IOException("Image parse error: "+e.getMessage(),e);}
    }
    public String scrape(String url)throws IOException{
        String u=clean(url);if(!isSafeUrl(u))throw new IOException("URL tidak aman/ditolak");
        Request req=new Request.Builder().url(u).header("User-Agent","JARVIS/2.7 Android Web Reader").get().build();
        try(Response r=client.newCall(req).execute()){if(!r.isSuccessful())throw new IOException("Web HTTP "+r.code());if(r.body()==null)return "";byte[] bytes=r.body().bytes();if(bytes.length>2_000_000)throw new IOException("Halaman terlalu besar");String html=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);return extractText(html);}
    }
    private static boolean isSafeUrl(String u){try{URI x=URI.create(u);String scheme=x.getScheme();if(!"http".equalsIgnoreCase(scheme)&&!"https".equalsIgnoreCase(scheme))return false;String host=x.getHost();if(host==null||host.length()==0)return false;InetAddress a=InetAddress.getByName(host);byte[] ip=a.getAddress();if(a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress())return false;return !(ip.length==16 && (ip[0]&0xff)==0xfc || ip.length==16 && (ip[0]&0xfe)==0xfc); }catch(Exception e){return false;}}
    private static String extractText(String html){String s=html.replaceAll("(?is)<(script|style|noscript|svg)[^>]*>.*?</\\1>"," ").replaceAll("(?is)<br\\s*/?>","\\n").replaceAll("(?is)</(p|div|li|h1|h2|h3|article|section)>","\\n").replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">");s=s.replaceAll("[ \\t]+"," ").replaceAll("\\n{3,}","\\n\\n").trim();return s.length()>12000?s.substring(0,12000)+"\\n[…dipotong…]":s;}
    private static String body(Response r)throws IOException{return r.body()==null?"":r.body().string();}
}
"
