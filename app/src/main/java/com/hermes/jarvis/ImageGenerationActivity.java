package com.hermes.jarvis;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.*;
import com.hermes.jarvis.utils.PrefsManager;
import java.io.IOException;
import okhttp3.*;

public class ImageGenerationActivity extends AppCompatActivity {
    private EditText prompt,model; private ImageView image; private TextView status; private Button generate;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_image_generation);prompt=findViewById(R.id.etImagePrompt);model=findViewById(R.id.etImageModel);image=findViewById(R.id.ivGenerated);status=findViewById(R.id.tvImageStatus);generate=findViewById(R.id.btnGenerate);PrefsManager p=new PrefsManager(this);model.setText(p.model());generate.setOnClickListener(v->generate(p));}
    private void generate(PrefsManager p){String q=prompt.getText().toString().trim();if(q.isEmpty())return;generate.setEnabled(false);status.setText("Generating…");JsonObject body=new JsonObject();body.addProperty("model",model.getText().toString().trim());body.addProperty("prompt",q);body.addProperty("n",1);body.addProperty("size","1024x1024");Request r=new Request.Builder().url(p.baseUrl().replaceAll("/chat/completions/?$","/images/generations")).header("Authorization","Bearer "+p.apiKey()).post(RequestBody.create(body.toString(),MediaType.parse("application/json"))).build();new OkHttpClient().newCall(r).enqueue(new okhttp3.Callback(){public void onFailure(Call c,IOException e){runOnUiThread(()->done("❌ "+e.getMessage()));}public void onResponse(Call c,Response x)throws IOException{try(x){String raw=x.body()==null?"":x.body().string();if(!x.isSuccessful()){done("HTTP "+x.code()+": "+raw);return;}JsonObject o=JsonParser.parseString(raw).getAsJsonObject();JsonObject d=o.getAsJsonArray("data").get(0).getAsJsonObject();if(d.has("b64_json")){byte[] b=Base64.decode(d.get("b64_json").getAsString(),Base64.DEFAULT);runOnUiThread(()->{image.setImageBitmap(BitmapFactory.decodeByteArray(b,0,b.length));done("✅ Gambar dibuat");});}else if(d.has("url")){String u=d.get("url").getAsString();runOnUiThread(()->done("✅ URL gambar diterima: "+u));}else done("❌ Respons gambar tidak dikenali");}catch(Exception e){done("❌ "+e.getMessage());}}});}
    private void done(String s){runOnUiThread(()->{status.setText(s);generate.setEnabled(true);});}
}
