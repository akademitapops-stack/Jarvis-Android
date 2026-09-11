package com.hermes.jarvis.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ContactHelper {

    public static class Contact {
        public String name;
        public String phone;
    }

    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static List<Contact> search(Context ctx, String query) {
        List<Contact> results = new ArrayList<>();
        if (!hasPermission(ctx)) return results;
        try {
            String q = query == null ? "" : query.trim();
            String selection = q.isEmpty() ? null
                    : ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            String[] args = q.isEmpty() ? null : new String[]{"%" + q + "%"};

            Cursor c = ctx.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    selection, args,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (c != null) {
                while (c.moveToNext() && results.size() < 20) {
                    Contact ct = new Contact();
                    ct.name = c.getString(0);
                    ct.phone = c.getString(1);
                    if (ct.name != null && ct.phone != null) results.add(ct);
                }
                c.close();
            }
        } catch (Exception ignored) {}
        return results;
    }

    public static String searchText(Context ctx, String query) {
        if (!hasPermission(ctx))
            return "❌ Izin kontak belum diberikan. Buka app → berikan izin kontak.";
        List<Contact> list = search(ctx, query);
        if (list.isEmpty())
            return "❌ Kontak \"" + query + "\" tidak ditemukan";
        StringBuilder sb = new StringBuilder("📇 Kontak \"").append(query).append("\":\n");
        for (Contact c : list)
            sb.append("• ").append(c.name).append(": ").append(c.phone).append('\n');
        return sb.toString();
    }
}
