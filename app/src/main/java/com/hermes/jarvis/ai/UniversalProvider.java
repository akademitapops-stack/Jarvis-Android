package com.hermes.jarvis.ai;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hermes.jarvis.utils.PrefsManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public interface ModelsCallback {
        void onResult(List<String> models, String provider);
        void onError(String error);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();
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

    /**
     * Accepts either a true base URL or the old UI's full /chat/completions URL.
     * The request layer always resolves exactly one chat endpoint.
     */
    public static String normalizeBaseUrl(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        String[] suffixes = {"/chat/completions", "/completions"};
        for (String suffix : suffixes) {
            if (s.endsWith(suffix)) {
                s = s.substring(0, s.length() - suffix.length());
                while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
                break;
            }
        }
        return s;
    }

    public static String chatEndpoint(String raw) {
        String base = normalizeBaseUrl(raw);
        if (base.isEmpty()) return "";
        return base + "/chat/completions";
    }

    private Request buildRequest(JsonObject body) {
        String base = normalizeBaseUrl(prefs.baseUrl());
        String endpoint = chatEndpoint(base);

        Request.Builder rb = new Request.Builder()
                .url(endpoint)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(gson.toJson(body), JSON));

        String key = sanitizeApiKey(prefs.apiKey());
        if (!key.isEmpty()) rb.header("Authorization", "Bearer " + key);

        if (base.contains("openrouter.ai"))
            rb.header("X-Title", "J.A.R.V.I.S. Titan");

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
                    String bodyText = resp.body() != null ? resp.body().string() : "";
                    String contentType = resp.header("Content-Type", "");

                    if (!resp.isSuccessful()) {
                        cb.onError(formatHttpError(resp.code(), bodyText, contentType));
                        return;
                    }

                    if (bodyText.trim().isEmpty()) {
                        cb.onError("Provider mengirim respons kosong.");
                        return;
                    }

                    JsonObject json = gson.fromJson(bodyText, JsonObject.class);
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices == null || choices.size() == 0) {
                        cb.onError("Respons API valid tetapi tidak berisi choices.");
                        return;
                    }

                    JsonObject message = choices.get(0).getAsJsonObject()
                            .getAsJsonObject("message");
                    if (message == null || !message.has("content")) {
                        cb.onError("Respons model tidak berisi message.content.");
                        return;
                    }

                    JsonElement contentElement = message.get("content");
                    String content = contentElement.isJsonPrimitive()
                            ? contentElement.getAsString()
                            : contentElement.toString();

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

    private String formatHttpError(int code, String body, String contentType) {
        String text = body == null ? "" : body.trim();
        String lower = text.toLowerCase();

        if (code == 401) return "❌ API key ditolak (HTTP 401). Periksa key/provider.";
        if (code == 403 && lower.contains("only available on agentic harnesses"))
            return "⚠️ Model ini hanya tersedia untuk Agentic Harness. Pilih model Chat biasa.";
        if (code == 404 && (lower.contains("<!doctype html") || lower.contains("<html")))
            return "❌ Endpoint API 404. Base URL tidak tepat atau provider tidak menyediakan endpoint ini.";
        if (code == 429) return "⏳ Rate limit/kuota provider tercapai (HTTP 429).";
        if (code >= 500) return "🔴 Provider sedang bermasalah (HTTP " + code + ").";

        String clean = text.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return "HTTP " + code + ": " + clean.substring(0, Math.min(clean.length(), 420));
    }

    /**
     * Loads the model catalog supported by the selected API key.
     * For OpenAI-compatible providers the /models endpoint is used.
     * Gemini uses Google's native models catalog.
     */
    public void discoverModels(String providerId, String baseUrlRaw,
                               String rawKey, ModelsCallback cb) {
        final String key = sanitizeApiKey(rawKey);
        if (key.isEmpty()) {
            cb.onError("API key kosong setelah sanitasi. Paste ulang key lalu coba lagi.");
            return;
        }

        final String provider = providerId == null ? "custom" : providerId;
        final String base = normalizeBaseUrl(baseUrlRaw);
        String url;

        if ("gemini".equals(provider)) {
            try {
                url = "https://generativelanguage.googleapis.com/v1beta/models?key="
                        + URLEncoder.encode(key, "UTF-8");
            } catch (Exception e) {
                cb.onError("API key tidak dapat diproses.");
                return;
            }
        } else {
            url = modelsEndpoint(base);
        }

        Request.Builder rb = new Request.Builder().url(url)
                .header("Accept", "application/json");
        if (!"gemini".equals(provider)) rb.header("Authorization", "Bearer " + key);

        client.newCall(rb.get().build()).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError("Gagal mengambil daftar model: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response resp)
                    throws IOException {
                try (resp) {
                    String text = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) {
                        cb.onError(formatHttpError(resp.code(), text,
                                resp.header("Content-Type", "")));
                        return;
                    }
                    try {
                        JsonObject json = gson.fromJson(text, JsonObject.class);
                        List<String> models = new ArrayList<>();
                        if ("gemini".equals(provider)) {
                            JsonArray arr = json.getAsJsonArray("models");
                            if (arr != null) {
                                for (JsonElement e : arr) {
                                    JsonObject m = e.getAsJsonObject();
                                    JsonArray methods = m.getAsJsonArray("supportedGenerationMethods");
                                    boolean usable = methods == null || contains(methods, "generateContent");
                                    if (usable && m.has("name")) {
                                        String name = m.get("name").getAsString();
                                        if (name.startsWith("models/")) name = name.substring(7);
                                        if (!name.isEmpty()) models.add(name);
                                    }
                                }
                            }
                        } else {
                            JsonArray arr = json.getAsJsonArray("data");
                            if (arr != null) {
                                for (JsonElement e : arr) {
                                    JsonObject m = e.getAsJsonObject();
                                    if (m.has("id")) {
                                        String id = m.get("id").getAsString();
                                        if (!id.isEmpty()) models.add(id);
                                    }
                                }
                            }
                        }

                        Set<String> unique = new HashSet<>(models);
                        models = new ArrayList<>(unique);
                        Collections.sort(models, Comparator.comparing(String::toLowerCase));

                        if (models.isEmpty()) {
                            cb.onError("API key valid, tetapi katalog model kosong/tidak didukung provider.");
                        } else {
                            cb.onResult(models, provider);
                        }
                    } catch (Exception e) {
                        cb.onError("Respons katalog model tidak valid: " + e.getMessage());
                    }
                }
            }
        });
    }

    private static boolean contains(JsonArray arr, String value) {
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive() && value.equals(e.getAsString())) return true;
        }
        return false;
    }

    private static String modelsEndpoint(String base) {
        if (base == null || base.isEmpty()) return "";
        if (base.contains("/chat/completions")) return base.replace("/chat/completions", "/models");
        if (base.endsWith("/chat")) return base.substring(0, base.length() - 5) + "/models";
        return base + "/models";
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
                .replace("\u00A0", "")
                .replaceAll("[\\r\\n\\t]", "")
                .trim();
    }

    public boolean isConfigured() {
        String url = normalizeBaseUrl(prefs.baseUrl());
        boolean local = url.contains("localhost") || url.contains("127.0.0.1");
        return local || !sanitizeApiKey(prefs.apiKey()).isEmpty();
    }
}
