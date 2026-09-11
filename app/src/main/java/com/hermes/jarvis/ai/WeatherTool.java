package com.hermes.jarvis.ai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherTool {

    public interface WeatherCallback {
        void onResult(String formatted);
        void onError(String error);
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).build();

    public static void fetch(Context ctx, String location, WeatherCallback cb) {
        if (location == null || location.trim().isEmpty()) fetchByGPS(ctx, cb);
        else geocodeThenFetch(location.trim(), cb);
    }

    /** Versi sinkron untuk laporan harian. Jangan panggil di main thread. */
    public static String fetchSync(Context ctx, String location) {
        final java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(1);
        final String[] out = {"❌ Cuaca tidak tersedia"};
        fetch(ctx, location, new WeatherCallback() {
            @Override public void onResult(String s) { out[0] = s; latch.countDown(); }
            @Override public void onError(String e) {
                out[0] = "❌ Cuaca: " + e; latch.countDown();
            }
        });
        try {
            latch.await(20, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        return out[0];
    }

    private static void fetchByGPS(Context ctx, WeatherCallback cb) {
        try {
            boolean ok = ContextCompat.checkSelfPermission(ctx,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(ctx,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            if (!ok) { cb.onError("Izin lokasi belum diberikan"); return; }

            LocationManager lm = (LocationManager)
                    ctx.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            if (lm != null) {
                for (String p : lm.getProviders(true)) {
                    Location l = lm.getLastKnownLocation(p);
                    if (l != null && (best == null || l.getAccuracy() < best.getAccuracy()))
                        best = l;
                }
            }
            if (best == null) {
                cb.onError("Lokasi GPS belum tersedia (aktifkan GPS / buka Maps "
                        + "sejenak), atau sebutkan nama kota: \"cuaca di Bandung\"");
                return;
            }
            fetchWeather(best.getLatitude(), best.getLongitude(), "📍 Lokasi saat ini", cb);
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    private static void geocodeThenFetch(String city, WeatherCallback cb) {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + enc(city) + "&count=1&language=id&format=json";
        try {
            Response r = client.newCall(new Request.Builder().url(url)
                    .header("User-Agent", "HermesJarvis/2.1").build()).execute();
            try (r) {
                if (!r.isSuccessful() || r.body() == null) {
                    cb.onError("Kota tidak ditemukan"); return;
                }
                JsonObject o = JsonParser.parseString(r.body().string()).getAsJsonObject();
                if (!o.has("results") || o.getAsJsonArray("results").size() == 0) {
                    cb.onError("Kota \"" + city + "\" tidak ditemukan"); return;
                }
                JsonObject res = o.getAsJsonArray("results").get(0).getAsJsonObject();
                double lat = res.get("latitude").getAsDouble();
                double lon = res.get("longitude").getAsDouble();
                String name = res.has("name") ? res.get("name").getAsString() : city;
                String country = res.has("country") ? ", " + res.get("country").getAsString() : "";
                fetchWeather(lat, lon, name + country, cb);
            }
        } catch (Exception e) {
            cb.onError("Geocoding gagal: " + e.getMessage());
        }
    }

    private static void fetchWeather(double lat, double lon, String label, WeatherCallback cb) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                + "&longitude=" + lon
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,"
                + "weather_code,wind_speed_10m,precipitation"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,"
                + "precipitation_probability_max&forecast_days=2&timezone=auto";
        try {
            Response r = client.newCall(new Request.Builder().url(url)
                    .header("User-Agent", "HermesJarvis/2.1").build()).execute();
            try (r) {
                if (!r.isSuccessful() || r.body() == null) {
                    cb.onError("HTTP " + r.code()); return;
                }
                JsonObject o = JsonParser.parseString(r.body().string()).getAsJsonObject();

                JsonObject cur = o.getAsJsonObject("current");
                double t = g(cur, "temperature_2m");
                double feels = g(cur, "apparent_temperature");
                double hum = g(cur, "relative_humidity_2m");
                double wind = g(cur, "wind_speed_10m");
                int wcode = (int) g(cur, "weather_code");

                JsonObject daily = o.getAsJsonObject("daily");
                JsonArray times = daily.getAsJsonArray("time");
                JsonArray dmax = daily.getAsJsonArray("temperature_2m_max");
                JsonArray dmin = daily.getAsJsonArray("temperature_2m_min");
                JsonArray dcodes = daily.getAsJsonArray("weather_code");
                JsonArray dprob = daily.getAsJsonArray("precipitation_probability_max");

                StringBuilder sb = new StringBuilder();
                sb.append("🌤️ CUACA — ").append(label).append('\n');
                sb.append("Sekarang: ").append(fmt(t)).append("°C (terasa ")
                  .append(fmt(feels)).append("°C), ").append(wmo(wcode)).append('\n');
                sb.append("Kelembapan: ").append(fmt(hum)).append("% | Angin: ")
                  .append(fmt(wind)).append(" km/jam\n");

                if (times.size() > 0 && dmax.size() > 0 && dmin.size() > 0) {
                    sb.append("Hari ini: ").append(fmt(dmin.get(0).getAsDouble()))
                      .append("–").append(fmt(dmax.get(0).getAsDouble()))
                      .append("°C, ").append(wmo(dcodes.get(0).getAsInt()));
                    if (dprob.size() > 0)
                        sb.append(" (hujan ").append(dprob.get(0).getAsInt()).append("%)");
                    sb.append('\n');
                }
                if (times.size() > 1 && dmax.size() > 1 && dmin.size() > 1) {
                    sb.append("Besok: ").append(fmt(dmin.get(1).getAsDouble()))
                      .append("–").append(fmt(dmax.get(1).getAsDouble()))
                      .append("°C, ").append(wmo(dcodes.get(1).getAsInt()));
                    if (dprob.size() > 1)
                        sb.append(" (hujan ").append(dprob.get(1).getAsInt()).append("%)");
                }
                cb.onResult(sb.toString());
            }
        } catch (Exception e) {
            cb.onError("Cuaca gagal: " + e.getMessage());
        }
    }

    private static double g(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
    }
    private static String fmt(double v) { return String.format(java.util.Locale.US, "%.0f", v); }

    private static String wmo(int c) {
        if (c == 0) return "Cerah ☀️";
        if (c <= 3) return "Cerah berawan 🌤️";
        if (c == 45 || c == 48) return "Berkabut 🌫️";
        if (c >= 51 && c <= 57) return "Gerimis 🌦️";
        if (c >= 61 && c <= 67) return "Hujan 🌧️";
        if (c >= 71 && c <= 77) return "Salju ❄️";
        if (c >= 80 && c <= 82) return "Hujan deras ⛈️";
        if (c == 85 || c == 86) return "Hujan salju 🌨️";
        if (c >= 95) return "Badai petir ⛈️";
        return "Cuaca (" + c + ")";
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s.replace(' ', '+'); }
    }
}
