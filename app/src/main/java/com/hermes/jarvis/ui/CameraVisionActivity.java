package com.hermes.jarvis.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.hermes.jarvis.R;
import com.hermes.jarvis.ai.SystemPrompt;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.utils.PrefsManager;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraVisionActivity extends AppCompatActivity {

    private ImageView ivPhoto;
    private EditText etQuestion;
    private TextView tvResult;
    private Button btnShoot, btnAnalyze;
    private Bitmap photo;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> camLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getExtras() != null) {
                            photo = (Bitmap) result.getData()
                                    .getExtras().get("data");
                            if (photo != null) {
                                ivPhoto.setImageBitmap(photo);
                                tvResult.setText("✅ Foto siap. Ketuk ANALISA.");
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_vision);

        ivPhoto = findViewById(R.id.ivPhoto);
        etQuestion = findViewById(R.id.etQuestion);
        tvResult = findViewById(R.id.tvResult);
        btnShoot = findViewById(R.id.btnShoot);
        btnAnalyze = findViewById(R.id.btnAnalyze);

        etQuestion.setText("Jelaskan apa yang kamu lihat di foto ini.");

        btnShoot.setOnClickListener(v -> {
            try {
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                camLauncher.launch(i);
            } catch (Exception e) {
                Toast.makeText(this, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show();
            }
        });

        btnAnalyze.setOnClickListener(v -> {
            if (photo == null) {
                Toast.makeText(this, "Ambil foto dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            analyze();
        });
    }

    private void analyze() {
        btnAnalyze.setEnabled(false);
        tvResult.setText("🔍 JARVIS sedang melihat...");

        final String question = etQuestion.getText().toString().trim().isEmpty()
                ? "Jelaskan apa yang kamu lihat." : etQuestion.getText().toString().trim();

        exec.execute(() -> {
            try {
                Bitmap scaled = scale(photo, 720);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);

                UniversalProvider provider =
                        new UniversalProvider(new PrefsManager(this));
                if (!provider.isConfigured()) {
                    runOnUiThread(() -> {
                        tvResult.setText("❌ API belum dikonfigurasi (⚙️ Settings).\n"
                                + "Tips: pakai Gemini 1.5 Flash (gratis, dukung vision).");
                        btnAnalyze.setEnabled(true);
                    });
                    return;
                }

                provider.sendVision(SystemPrompt.visionPrompt(), b64, question,
                        new UniversalProvider.Callback() {
                            @Override public void onResponse(
                                    com.hermes.jarvis.ai.AIResponse r) {
                                runOnUiThread(() -> {
                                    tvResult.setText(r.message);
                                    SoundFX.reply();
                                    btnAnalyze.setEnabled(true);
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    tvResult.setText("❌ " + e);
                                    btnAnalyze.setEnabled(true);
                                });
                            }
                        });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvResult.setText("❌ " + e.getMessage());
                    btnAnalyze.setEnabled(true);
                });
            }
        });
    }

    private static Bitmap scale(Bitmap src, int max) {
        int w = src.getWidth(), h = src.getHeight();
        float f = Math.max(1f, Math.max(w, h) / (float) max);
        return Bitmap.createScaledBitmap(src,
                Math.max(1, (int) (w / f)), Math.max(1, (int) (h / f)), true);
    }

    @Override
    protected void onDestroy() {
        exec.shutdown();
        super.onDestroy();
    }
}
