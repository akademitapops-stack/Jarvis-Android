package com.hermes.jarvis;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.*;
import com.hermes.jarvis.ai.UniversalProvider;
import com.hermes.jarvis.utils.PrefsManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class ImageGenerationActivity extends AppCompatActivity {
    private EditText prompt, model;
    private ImageView image;
    private TextView status;
    private Button generate;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).build();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_image_generation);
        prompt=findViewById(R.id.etImagePrompt);
        model=findViewById(R.id.etImageModel);
        image=findViewById(R.id.ivGenerated);
        status=findViewById(R.id.tvImageStatus);
        generate=findViewById(R.id.btnGenerate);
        PrefsManager p=new PrefsManager(this);
        String current=p.model();
        if(current!=null && !current.isEmpty()) model.setText(current);
        generate.setOnClickListener(v->generate(p));
    }

    private void generate(PrefsManager p) {
        String q=prompt.getText().toString().trim();
        String imageModel=model.getText().toString().trim();
        if(q.isEmpty()){ status.setText("❌ Prompt kosong"); return; }
        if(imageModel.isEmpty()){ status.setText("❌ Isi model image terlebih dahulu"); return; }
        if(!p.isConfigured()){ status.setText("❌ Provider AI belum dikonfigurasi"); return; }
        generate.setEnabled(false);
        status.setText("⏳ Generating…");

        JsonObject body=new JsonObject();
        body.addProperty("model",imageModel);
        body.addProperty("prompt",q);
        body.addProperty("n",1);
        body.addProperty("size","1024x1024");
        String endpoint=UniversalProvider.normalizeBaseUrl(p.baseUrl())+"/images/generations";
        Request.Builder rb=new Request.Builder().url(endpoint)
                .header("Content-Type","application/json")
                .post(RequestBody.create(body.toString(),MediaType.parse("application/json")));
        String apiKey=UniversalProvider.sanitizeApiKey(p.apiKey());
        if(!apiKey.isEmpty()) rb.header("Authorization","Bearer "+apiKey);
        if(p.baseUrl().toLowerCase().contains("openrouter.ai")) {
            rb.header("X-Title","JARVIS").header("HTTP-Referer","https://openrouter.ai/");
        }
        client.newCall(rb.build()).enqueue(new okhttp3.Callback(){
            @Override public void onFailure(Call c,IOException e){ done("❌ Koneksi gagal: "+safe(e.getMessage())); }
            @Override public void onResponse(Call c,Response x)throws IOException{
                try(x){
                    String raw=x.body()==null?"":x.body().string();
                    if(!x.isSuccessful()){ done("❌ HTTP "+x.code()+": "+compact(raw)); return; }
                    JsonElement root=JsonParser.parseString(raw);
                    if(!root.isJsonObject()) throw new IOException("Respons bukan JSON object");
                    JsonArray data=root.getAsJsonObject().getAsJsonArray("data");
                    if(data==null||data.size()==0) throw new IOException("Respons tidak memiliki data gambar");
                    JsonObject d=data.get(0).isJsonObject()?data.get(0).getAsJsonObject():null;
                    if(d==null) throw new IOException("Data gambar tidak valid");
                    if(d.has("b64_json")&&!d.get("b64_json").isJsonNull()){
                        byte[] bytes=Base64.decode(d.get("b64_json").getAsString(),Base64.DEFAULT);
                        Bitmap bm=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        if(bm==null) throw new IOException("Base64 gambar tidak dapat dibaca");
                        runOnUiThread(()->{ image.setImageBitmap(bm); status.setText("✅ Gambar dibuat"); generate.setEnabled(true); });
                        return;
                    }
                    if(d.has("url")&&!d.get("url").isJsonNull()){
                        String u=d.get("url").getAsString();
                        loadRemoteImage(u);
                        return;
                    }
                    throw new IOException("Respons gambar tidak dikenali");
                }catch(Exception e){ done("❌ "+safe(e.getMessage())); }
            }
        });
    }

    private void loadRemoteImage(String url){
        new Thread(()->{
            try(InputStream in=new URL(url).openStream()){
                Bitmap bm=BitmapFactory.decodeStream(in);
                if(bm==null) throw new IOException("URL tidak mengembalikan gambar");
                runOnUiThread(()->{image.setImageBitmap(bm);status.setText("✅ Gambar dibuat");generate.setEnabled(true);});
            }catch(Exception e){done("❌ Gambar dibuat tetapi gagal ditampilkan: "+safe(e.getMessage()));}
        }).start();
    }

    private void done(String s){runOnUiThread(()->{status.setText(s);generate.setEnabled(true);});}
    private static String safe(String s){return s==null||s.trim().isEmpty()?"unknown error":s;}
    private static String compact(String s){s=s==null?"":s.replace('\n',' ').trim();return s.length()>600?s.substring(0,600)+"…":s;}
}
