package com.hermes.jarvis.core;

import android.content.Context;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Small Android Keystore backed secret store for API credentials. */
public class SecretStore {
    private static final String KS = "AndroidKeyStore";
    private static final String ALIAS = "jarvis_master_v1";
    private final android.content.SharedPreferences p;
    private SecretKey key;

    public SecretStore(Context c) {
        p = c.getSharedPreferences("jarvis_secrets", Context.MODE_PRIVATE);
        key = loadKey();
    }

    private SecretKey loadKey() {
        try {
            KeyStore ks = KeyStore.getInstance(KS);
            ks.load(null);
            if (ks.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
            KeyGenerator kg = KeyGenerator.getInstance("AES", KS);
            kg.init(256);
            return kg.generateKey();
        } catch (Exception e) {
            // Fallback keeps the app functional on unusual OEM keystore implementations.
            byte[] seed = (p.getString("fallback_seed", "jarvis-local-secret-v1")).getBytes(StandardCharsets.UTF_8);
            return new SecretKeySpec(java.util.Arrays.copyOf(seed, 16), "AES");
        }
    }

    public void put(String name, String value) {
        try {
            if (value == null) value = "";
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipher = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[c.getIV().length + cipher.length];
            System.arraycopy(c.getIV(), 0, all, 0, c.getIV().length);
            System.arraycopy(cipher, 0, all, c.getIV().length, cipher.length);
            p.edit().putString(name, Base64.encodeToString(all, Base64.NO_WRAP)).apply();
        } catch (Exception e) { p.edit().putString(name, value).apply(); }
    }

    public String get(String name) {
        String raw = p.getString(name, "");
        if (raw.isEmpty()) return "";
        try {
            byte[] all = Base64.decode(raw, Base64.NO_WRAP);
            byte[] iv = java.util.Arrays.copyOfRange(all, 0, 12);
            byte[] data = java.util.Arrays.copyOfRange(all, 12, all.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception ignored) { return raw; }
    }

    public void remove(String name) { p.edit().remove(name).apply(); }
}
