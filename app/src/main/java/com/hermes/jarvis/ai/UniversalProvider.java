package com.hermes.jarvis.ai;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.hermes.jarvis.utils.PrefsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Unified provider client.
 *
 * Stores a BASE URL in preferences and appends /chat/completions only at request time.
 * This prevents the common bug where the settings UI stores an endpoint as a base URL.
 */
public class UniversalProvider {
    public interface Callback {
        void onResponse(AIResponse response);
        void onError(String error);
    }

    public interface ModelsCallback {
        void onModels(List<String> models);
        void onError(String error);
    }

    public interface TestCallback {
        void onResult(boolean ok, String message);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS).build();
    private final Gson gson = new Gson();
    private final PrefsManager prefs;

    public UniversalProvider(PrefsManager prefs) { this.prefs = prefs; }

    public void send(String systemPrompt, List<String[]> history,
                     String userMessage, Callback cb) {
        JsonObject body = baseBody();
        JsonArray msgs = new JsonArray();
        msgs.add(msg("system", systemPrompt));
        if (history != null) for (String[] h : history) msgs.add(msg(h[0], h[1]));
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
        content.add(textPart); content.add(imgPart);
        um.add("content", content); msgs.add(um);
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
        m.addProperty("content", content == null ? "" : content);
        return m;
    }

    /** Normalize any legacy/full endpoint into a provider BASE URL. */
    public static String normalizeBaseUrl(String raw) {
        String s = cleanUrl(raw);
        if (s.isEmpty()) return "";
        // Remove accidental query/fragment from API endpoint settings.
        int q = s.indexOf('?'); if (q >= 0) s = s.substring(0, q);
        int h = s.indexOf('#'); if (h >= 0) s = s.substring(0, h);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        String lower = s.toLowerCase();
        String[] suffixes = {
                "/chat/completions", "/responses", "/messages", "/completions"
        };
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                s = s.substring(0, s.length() - suffix.length());
                break;
            }
        }
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String chatEndpoint(String base) {
        String b = normalizeBaseUrl(base);
        if (b.isEmpty()) return "";
        return b + "/chat/completions";
    }

    private Request buildRequest(JsonObject body) {
        String endpoint = chatEndpoint(prefs.baseUrl());
        Request.Builder rb = new Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON));
        String key = sanitizeApiKey(prefs.apiKey());
        if (!key.isEmpty()) rb.header("Authorization", "Bearer " + key);
        if (isOpenRouter()) {
            rb.header("X-Title", "JARVIS");
            rb.header("HTTP-Referer", "https://openrouter.ai/");
        }
        return rb.build();
    }

    private boolean isOpenRouter() {
        return prefs.baseUrl().toLowerCase().contains("openrouter.ai");
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
                    String body = resp.body() == null ? "" : resp.body().string();
                    if (!resp.isSuccessful()) {
                        cb.onError(formatHttpError(resp.code(), body));
                        return;
                    }
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    JsonArray choices = json == null ? null : json.getAsJsonArray("choices");
                    if (choices == null || choices.size() == 0)
                        throw new IOException("Respons API tidak memiliki choices");
                    JsonObject message = choices.get(0).getAsJsonObject()
                            .getAsJsonObject("message");
                    if (message == null) throw new IOException("Respons API tidak memiliki message");
                    JsonElement contentEl = message.get("content");
                    String content = contentEl == null || contentEl.isJsonNull()
                            ? "" : contentEl.getAsString();
                    AIResponse r = AIResponseParser.parse(content, prefs.model());
                    r.timeMs = System.currentTimeMillis() - t0;
                    cb.onResponse(r);
                } catch (Exception e) {
                    cb.onError("Parse API gagal: " + e.getMessage());
                }
            }
        });
    }

    /** Fetch model IDs from provider catalog. */
    public void discoverModels(ModelsCallback cb) {
        new Thread(() -> {
            try {
                String provider = prefs.providerName().toLowerCase();
                List<String> result;
                if (provider.contains("gemini") || prefs.baseUrl().contains("generativelanguage.googleapis.com")) {
                    result = fetchGeminiModels();
                } else {
                    result = fetchOpenAIModels();
                }
                Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
                cb.onModels(result);
            } catch (Exception e) {
                cb.onError(e.getMessage() == null ? "Gagal memuat model" : e.getMessage());
            }
        }).start();
    }

    private List<String> fetchOpenAIModels() throws Exception {
        String url = normalizeBaseUrl(prefs.baseUrl()) + "/models";
        Request.Builder b = new Request.Builder().url(url)
                .header("Accept", "application/json").get();
        String key = sanitizeApiKey(prefs.apiKey());
        if (!key.isEmpty()) b.header("Authorization", "Bearer " + key);
        try (Response r = client.newCall(b.build()).execute()) {
            String body = r.body() == null ? "" : r.body().string();
            if (!r.isSuccessful()) throw new IOException(formatHttpError(r.code(), body));
            JsonObject root = gson.fromJson(body, JsonObject.class);
            JsonArray data = root == null ? null : root.getAsJsonArray("data");
            List<String> all = new ArrayList<>();
            if (data != null) for (JsonElement e : data) {
                if (!e.isJsonObject()) continue;
                JsonObject m = e.getAsJsonObject();
                String id = m.has("id") ? m.get("id").getAsString() : "";
                if (id.isEmpty()) continue;

                // OpenRouter's /models catalog is intentionally huge (400+ entries).
                // JARVIS is a chat/agent client, so hide media/embedding/reranker
                // entries that cannot be used by the normal chat endpoint.
                if (isOpenRouterBase(url) && !isChatModel(m, id)) continue;
                all.add(id);
            }
            if (all.isEmpty()) throw new IOException("Provider tidak mengembalikan model chat yang kompatibel.");

            if (isOpenRouterBase(url)) {
                // Keep the UI compact: show a useful shortlist instead of hundreds
                // of entries. The picker is searchable and the currently selected
                // model is always re-added by SettingsActivity when necessary.
                Collections.sort(all, (a,b) -> {
                    int af = a.endsWith(":free") ? 0 : 1;
                    int bf = b.endsWith(":free") ? 0 : 1;
                    if (af != bf) return Integer.compare(af,bf);
                    return a.compareToIgnoreCase(b);
                });
                if (all.size() > 60) all = new ArrayList<>(all.subList(0,60));
            }
            return all;
        }
    }

    private boolean isOpenRouterBase(String url) {
        return url != null && url.toLowerCase().contains("openrouter.ai");
    }

    private boolean isChatModel(JsonObject m, String id) {
        String lower = id.toLowerCase();
        String[] blocked = {"embedding", "rerank", "moderation", "whisper", "tts",
                "speech", "audio", "image", "vision-edit", "video", "veo", "sora"};
        for (String token : blocked) if (lower.contains(token)) return false;

        // Prefer the structured modality metadata when OpenRouter supplies it.
        if (m.has("architecture") && m.get("architecture").isJsonObject()) {
            JsonObject a = m.getAsJsonObject("architecture");
            if (a.has("output_modalities") && a.get("output_modalities").isJsonArray()) {
                boolean text = false;
                for (JsonElement x : a.getAsJsonArray("output_modalities"))
                    if ("text".equalsIgnoreCase(x.getAsString())) text = true;
                if (!text) return false;
            }
        }
        return true;
    }

    private List<String> fetchGeminiModels() throws Exception {
        String key = sanitizeApiKey(prefs.apiKey());
        if (key.isEmpty()) throw new IOException("API key kosong");
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key="
                + java.net.URLEncoder.encode(key, "UTF-8");
        Request r = new Request.Builder().url(url).header("Accept", "application/json").get().build();
        try (Response resp = client.newCall(r).execute()) {
            String body = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) throw new IOException(formatHttpError(resp.code(), body));
            JsonObject root = gson.fromJson(body, JsonObject.class);
            JsonArray data = root == null ? null : root.getAsJsonArray("models");
            List<String> out = new ArrayList<>();
            if (data != null) for (JsonElement e : data) {
                if (!e.isJsonObject()) continue;
                JsonObject m=e.getAsJsonObject();
                String name=m.has("name")?m.get("name").getAsString():"";
                String methods="";
                if(m.has("supportedGenerationMethods")) methods=m.get("supportedGenerationMethods").toString();
                if(name.startsWith("models/") && methods.contains("generateContent"))
                    out.add(name.substring("models/".length()));
            }
            if(out.isEmpty()) throw new IOException("Gemini tidak mengembalikan model yang bisa generateContent.");
            return out;
        }
    }

    /** Lightweight live credential test without exposing the key. */
    public void test(TestCallback cb) {
        new Thread(() -> {
            try {
                if (!isConfigured()) { cb.onResult(false, "API key kosong"); return; }
                String provider = prefs.providerName().toLowerCase();
                if (provider.contains("gemini") || prefs.baseUrl().contains("generativelanguage.googleapis.com")) {
                    // Model list call validates the key and also gives useful discovery.
                    List<String> models = fetchGeminiModels();
                    cb.onResult(true, "API valid · " + models.size() + " model tersedia");
                } else {
                    List<String> models = fetchOpenAIModels();
                    cb.onResult(true, "API valid · " + models.size() + " model tersedia");
                }
            } catch (Exception e) {
                cb.onResult(false, e.getMessage() == null ? "Validasi gagal" : e.getMessage());
            }
        }).start();
    }

    public static String sanitizeApiKey(String raw) {
        if (raw == null) return "";
        String s = raw
                .replace("\u200B","").replace("\u200C","").replace("\u200D","")
                .replace("\u200E","").replace("\u200F","").replace("\u202A","")
                .replace("\u202B","").replace("\u202C","").replace("\u202D","")
                .replace("\u202E","").replace("\u2060","").replace("\uFEFF","");
        // Clipboard/notes sometimes wrap a key in quotes or add line breaks.
        s=s.replace("\r","").replace("\n","").replace("\t","").trim();
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))
            s=s.substring(1,s.length()-1).trim();
        return s;
    }

    private static String cleanUrl(String raw) {
        if(raw==null)return "";
        return raw.replace("\u200B","").replace("\u200E","").replace("\u200F","")
                .replace("\uFEFF","").trim();
    }

    private static String formatHttpError(int code, String body) {
        String b = body == null ? "" : body.trim();
        if (b.startsWith("<!DOCTYPE") || b.startsWith("<html") || b.contains("<html"))
            return "HTTP " + code + " · endpoint mengembalikan HTML. Periksa Base URL provider.";
        if (b.length() > 500) b = b.substring(0,500);
        return "HTTP " + code + (b.isEmpty() ? "" : " · " + b);
    }

    public boolean isConfigured() {
        String url = normalizeBaseUrl(prefs.baseUrl());
        boolean local = url.contains("localhost") || url.contains("127.0.0.1");
        return !url.isEmpty() && (local || !sanitizeApiKey(prefs.apiKey()).isEmpty());
    }
}
