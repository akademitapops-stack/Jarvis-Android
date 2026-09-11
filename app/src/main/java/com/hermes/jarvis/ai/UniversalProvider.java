package com.hermes.jarvis.ai;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hermes.jarvis.utils.PrefsManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UniversalProvider {

    public interface Callback {
        void onResponse(AIResponse response);
        void onError(String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS).build();
    private final Gson gson = new Gson();
    private final PrefsManager prefs;

    public UniversalProvider(PrefsManager prefs) { this.prefs = prefs; }

    public void send(String systemPrompt, List<String[]> history,
                     String userMessage, Callback cb) {
        JsonObject body = baseBody();
        JsonArray msgs = new JsonArray();
        msgs.add(msg("system", systemPrompt));
        if (history != null)
            for (String[] h : history) msgs.add(msg(h[0], h[1]));
        msgs.add(msg("user", userMessage));
        body.add("messages", msgs);
        exec(buildRequest(body), cb);
    }

    public void sendVision(String systemPrompt, String base64Jpeg,
                           String question, Callback cb) {
        JsonObject body = baseBody();
        JsonArray msgs = new JsonArray();
        msgs.add(msg("system", systemPrompt));

        JsonObject um = new JsonObject();
        um.addProperty("role", "user");
        JsonArray content = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("type", "text");
        textPart.addProperty("text", question);
        JsonObject imgPart = new JsonObject();
        imgPart.addProperty("type", "image_url");
        JsonObject urlObj = new JsonObject();
        urlObj.addProperty("url", "data:image/jpeg;base64," + base64Jpeg);
        imgPart.add("image_url", urlObj);
        content.add(textPart);
        content.add(imgPart);
        um.add("content", content);
        msgs.add(um);

        body.add("messages", msgs);
        exec(buildRequest(body), cb);
    }

    private JsonObject baseBody() {
        JsonObject body = new JsonObject();
        body.addProperty("model", prefs.model());
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 1500);
        return body;
    }

    private JsonObject msg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private Request buildRequest(JsonObject body) {
        Request.Builder rb = new Request.Builder()
                .url(prefs.baseUrl())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON));
        String key = sanitizeApiKey(prefs.apiKey());
        if (!key.isEmpty()) rb.header("Authorization", "Bearer " + key);
        if (prefs.baseUrl().contains("openrouter"))
            rb.header("X-Title", "Hermes Jarvis");
        return rb.build();
    }

    private void exec(Request request, Callback cb) {
        long t0 = System.currentTimeMillis();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError("Koneksi API gagal: " + e.getMessage());
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response resp)
                    throws IOException {
                try (resp) {
                    if (!resp.isSuccessful()) {
                        String err = resp.body() != null ? resp.body().string() : "?";
                        cb.onError("HTTP " + resp.code() + ": "
                                + err.substring(0, Math.min(err.length(), 300)));
                        return;
                    }
                    JsonObject json = gson.fromJson(resp.body().string(), JsonObject.class);
                    String content = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    AIResponse r = AIResponseParser.parse(content, prefs.model());
                    r.timeMs = System.currentTimeMillis() - t0;
                    cb.onResponse(r);
                } catch (Exception e) {
                    cb.onError("Parse error: " + e.getMessage()
                            + "\n(Catatan: untuk foto, model harus support vision)");
                }
            }
        });
    }


    /** Removes invisible Unicode markers commonly introduced by copy/paste. */
    public static String sanitizeApiKey(String raw) {
        if (raw == null) return "";
        return raw
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u200E", "")
                .replace("\u200F", "")
                .replace("\u202A", "")
                .replace("\u202B", "")
                .replace("\u202C", "")
                .replace("\u202D", "")
                .replace("\u202E", "")
                .replace("\u2060", "")
                .replace("\uFEFF", "")
                .trim();
    }

    public boolean isConfigured() {
        String url = prefs.baseUrl();
        boolean local = url.contains("localhost") || url.contains("127.0.0.1");
        return local || !prefs.apiKey().isEmpty();
    }
}
