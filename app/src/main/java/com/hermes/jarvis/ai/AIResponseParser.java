package com.hermes.jarvis.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.hermes.jarvis.automation.SmartAutomation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIResponseParser {

    private static final String TAG = "Parser";
    private static final Gson gson = new Gson();

    public static AIResponse parse(String raw, String model) {
        AIResponse r = new AIResponse();
        r.raw = raw;
        r.model = model;
        String cleaned = clean(raw);

        try {
            JsonObject o = gson.fromJson(cleaned, JsonObject.class);
            if (o.has("response") && !o.get("response").isJsonNull())
                r.message = o.get("response").getAsString();
            if (o.has("speak") && !o.get("speak").isJsonNull())
                r.speakText = o.get("speak").getAsString();
            if (o.has("commands") && o.get("commands").isJsonArray())
                o.getAsJsonArray("commands").forEach(e -> r.commands.add(e.getAsString()));
            if (o.has("device_actions") && o.get("device_actions").isJsonArray())
                o.getAsJsonArray("device_actions").forEach(e ->
                        r.deviceActions.add(e.getAsString()));
            if (o.has("web_search") && o.get("web_search").isJsonArray())
                o.getAsJsonArray("web_search").forEach(e ->
                        r.webSearches.add(e.getAsString()));
            if (o.has("news_search") && o.get("news_search").isJsonArray())
                o.getAsJsonArray("news_search").forEach(e -> r.newsSearches.add(e.getAsString()));
            if (o.has("image_search") && o.get("image_search").isJsonArray())
                o.getAsJsonArray("image_search").forEach(e -> r.imageSearches.add(e.getAsString()));
            if (o.has("web_open") && o.get("web_open").isJsonArray())
                o.getAsJsonArray("web_open").forEach(e -> r.webOpens.add(e.getAsString()));

            if (o.has("weather") && o.get("weather").isJsonObject()) {
                JsonObject w = o.getAsJsonObject("weather");
                r.weatherLocation = (w.has("location") && !w.get("location").isJsonNull())
                        ? w.get("location").getAsString() : "";
            }

            if (o.has("create_automation") && o.get("create_automation").isJsonObject()) {
                JsonObject a = o.getAsJsonObject("create_automation");
                SmartAutomation.Rule rule = new SmartAutomation.Rule();
                rule.name = jstr(a, "name", "Automasi Jarvis");
                rule.type = jstr(a, "type", "daily");
                rule.time = jstr(a, "time", "07:00");
                rule.intervalMin = (a.has("interval_minutes")
                        && !a.get("interval_minutes").isJsonNull())
                        ? a.get("interval_minutes").getAsInt() : 60;
                rule.message = jstr(a, "message", "Laporan Jarvis");
                if (a.has("commands") && a.get("commands").isJsonArray())
                    a.getAsJsonArray("commands").forEach(e ->
                            rule.commands.add(e.getAsString()));
                r.autoCreate = rule;
            }
            if (o.has("delete_automation") && !o.get("delete_automation").isJsonNull())
                r.autoDeleteName = o.get("delete_automation").getAsString();

            if (o.has("memory_write") && o.get("memory_write").isJsonObject()) {
                JsonObject m = o.getAsJsonObject("memory_write");
                if (m.has("key") && m.has("value")) {
                    r.memoryKey = m.get("key").getAsString();
                    r.memoryValue = m.get("value").getAsString();
                }
            }
            if (o.has("schedule") && o.get("schedule").isJsonObject()) {
                JsonObject s = o.getAsJsonObject("schedule");
                if (s.has("minutes") && s.has("message")) {
                    r.scheduleMinutes = s.get("minutes").getAsInt();
                    r.scheduleMessage = s.get("message").getAsString();
                }
            }
            if (o.has("notification_reply") && o.get("notification_reply").isJsonObject()) {
                JsonObject nr = o.getAsJsonObject("notification_reply");
                if (nr.has("package") && nr.has("message")) {
                    r.replyPackage = nr.get("package").getAsString();
                    r.replyMessage = nr.get("message").getAsString();
                }
            }

            if (o.has("call_contact") && !o.get("call_contact").isJsonNull())
                r.callContact = o.get("call_contact").getAsString();
            if (o.has("search_contacts") && !o.get("search_contacts").isJsonNull())
                r.contactSearch = o.get("search_contacts").getAsString();

            if (o.has("daily_report") && o.get("daily_report").isJsonObject()) {
                JsonObject dr = o.getAsJsonObject("daily_report");
                if (dr.has("disable") && dr.get("disable").getAsBoolean()) {
                    r.reportDisable = true;
                } else {
                    r.reportHour = dr.has("hour") && !dr.get("hour").isJsonNull()
                            ? dr.get("hour").getAsInt() : 7;
                    r.reportMinute = dr.has("minute") && !dr.get("minute").isJsonNull()
                            ? dr.get("minute").getAsInt() : 0;
                }
            }

            if (o.has("create_tool") && o.get("create_tool").isJsonObject()) {
                JsonObject t=o.getAsJsonObject("create_tool"); r.createToolName=jstr(t,"name",""); r.createToolDescription=jstr(t,"description",""); r.createToolCommand=jstr(t,"command",""); r.createToolRoot=t.has("requires_root")&&t.get("requires_root").getAsBoolean();
            }
            if (o.has("use_tool") && !o.get("use_tool").isJsonNull()) r.useTool=o.get("use_tool").getAsString();
            if (o.has("github_list") && !o.get("github_list").isJsonNull()) r.githubList=o.get("github_list").getAsString();
            if (o.has("calendar_event") && o.get("calendar_event").isJsonObject()) { JsonObject ce=o.getAsJsonObject("calendar_event"); r.calendarTitle=jstr(ce,"title",""); r.calendarNote=jstr(ce,"note",""); if(ce.has("start_ms"))r.calendarStartMs=ce.get("start_ms").getAsLong(); if(ce.has("end_ms"))r.calendarEndMs=ce.get("end_ms").getAsLong(); }

            if (o.has("needs_confirmation"))
                r.needsConfirmation = o.get("needs_confirmation").getAsBoolean();
        } catch (JsonSyntaxException | IllegalStateException e) {
            Log.w(TAG, "JSON gagal, fallback: " + e.getMessage());
            r.message = raw.trim();
            r.commands = fallbackCommands(raw);
        }
        return r;
    }

    private static String jstr(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }

    private static String clean(String raw) {
        String c = raw.trim();
        Matcher m = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?\\s*```",
                Pattern.DOTALL).matcher(c);
        if (m.find()) c = m.group(1).trim();
        int s = c.indexOf('{'), e = c.lastIndexOf('}');
        if (s != -1 && e > s) c = c.substring(s, e + 1);
        return c;
    }

    private static List<String> fallbackCommands(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(
                "(?:^|\\n)\\s*(?:\\$|>)?\\s*((?:ls|cat|ps|df|free|uname|getprop|dumpsys|"
                        + "top|netstat|pm list)\\b[^\\n]+)",
                Pattern.MULTILINE).matcher(text);
        while (m.find()) out.add(m.group(1).trim());
        return out;
    }
}
