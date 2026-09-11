package com.hermes.jarvis.ai;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WebSearchTool {

    public interface SearchCallback {
        void onResult(String formatted);
        void onError(String error);
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    public static void search(String query, SearchCallback cb) {
        searchWikipedia(query, new SearchCallback() {
            @Override public void onResult(String s) { cb.onResult(s); }
            @Override public void onError(String e) { searchDDG(query, cb); }
        });
    }

    private static void searchWikipedia(String query, SearchCallback cb) {
        String url = "https://id.wikipedia.org/api/rest_v1/page/summary/" + enc(query);
        Request req = new Request.Builder().url(url)
                .header("User-Agent", "HermesJarvis/2.0 (student project)").build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                cb.onError(e.getMessage());
            }
            @Override public void onResponse(@NonNull Call c, @NonNull Response resp)
                    throws IOException {
                try (resp) {
                    if (!resp.isSuccessful()) { cb.onError("HTTP " + resp.code()); return; }
                    JsonObject o = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    String extract = str(o, "extract");
                    if (extract.length() < 20) { cb.onError("no result"); return; }
                    cb.onResult("🌐 [Wikipedia] " + str(o, "title") + "\n" + extract);
                } catch (Exception e) { cb.onError(e.getMessage()); }
            }
        });
    }

    private static void searchDDG(String query, SearchCallback cb) {
        String url = "https://api.duckduckgo.com/?q=" + enc(query)
                + "&format=json&no_html=1&skip_disambig=1";
        Request req = new Request.Builder().url(url)
                .header("User-Agent", "HermesJarvis/2.0").build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call c, @NonNull IOException e) {
                cb.onError(e.getMessage());
            }
            @Override public void onResponse(@NonNull Call c, @NonNull Response resp)
                    throws IOException {
                try (resp) {
                    if (!resp.isSuccessful()) { cb.onError("HTTP " + resp.code()); return; }
                    JsonObject o = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    String abs = str(o, "AbstractText");
                    if (abs.length() > 10) {
                        cb.onResult("🌐 [DuckDuckGo] " + abs);
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    if (o.has("RelatedTopics") && o.get("RelatedTopics").isJsonArray()) {
                        for (JsonElement t : o.getAsJsonArray("RelatedTopics")) {
                            if (t.isJsonObject()) {
                                JsonObject to = t.getAsJsonObject();
                                if (to.has("Text") && !to.get("Text").isJsonNull()) {
                                    String txt = to.get("Text").getAsString();
                                    sb.append("• ").append(txt, 0,
                                            Math.min(150, txt.length())).append('\n');
                                    if (++count >= 5) break;
                                }
                            }
                        }
                    }
                    if (count == 0) { cb.onError("no result"); return; }
                    cb.onResult("🌐 [DuckDuckGo] " + query + ":\n" + sb);
                } catch (Exception e) { cb.onError(e.getMessage()); }
            }
        });
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s.replace(' ', '+'); }
    }
}
