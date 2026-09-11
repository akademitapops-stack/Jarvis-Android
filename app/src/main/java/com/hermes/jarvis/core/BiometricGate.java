package com.hermes.jarvis.core;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricGate {

    public interface Result {
        void onSuccess();
        void onFailed(String reason);
    }

    public static boolean available(Context ctx) {
        return BiometricManager.from(ctx)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(AppCompatActivity activity, String title, Result cb) {
        try {
            Executor executor = ContextCompat.getMainExecutor(activity);
            BiometricPrompt prompt = new BiometricPrompt(activity, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(
                                BiometricPrompt.AuthenticationResult result) {
                            cb.onSuccess();
                        }
                        @Override
                        public void onAuthenticationError(int code, CharSequence err) {
                            cb.onFailed(err.toString());
                        }
                    });

            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle("Verifikasi identitas untuk melanjutkan")
                    .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();
            prompt.authenticate(info);
        } catch (Exception e) {
            cb.onSuccess();
        }
    }
}
