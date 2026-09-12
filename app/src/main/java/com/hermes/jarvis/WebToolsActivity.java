package com.hermes.jarvis;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.hermes.jarvis.web.WebSearchManager;

public class WebToolsActivity extends AppCompatActivity {
    private EditText brave, customUrl, customKey;
    private WebSearchManager web;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_web_tools);web=new WebSearchManager(this);brave=findViewById(R.id.etBraveKey);customUrl=findViewById(R.id.etCustomUrl);customKey=findViewById(R.id.etCustomKey);
        findViewById(R.id.btnSaveBrave).setOnClickListener(v->{web.saveBrave(brave.getText().toString());Toast.makeText(this,"✅ Brave Search aktif",Toast.LENGTH_SHORT).show();});
        findViewById(R.id.btnSaveCustom).setOnClickListener(v->{web.saveCustom(customUrl.getText().toString(),customKey.getText().toString());Toast.makeText(this,"✅ Custom provider aktif",Toast.LENGTH_SHORT).show();});
        findViewById(R.id.btnTestWeb).setOnClickListener(v->new Thread(()->{try{String r=web.search("latest technology",3);runOnUiThread(()->new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("🌐 Web test OK").setMessage(r).setPositiveButton("OK",null).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"❌ "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start());
    }
}
