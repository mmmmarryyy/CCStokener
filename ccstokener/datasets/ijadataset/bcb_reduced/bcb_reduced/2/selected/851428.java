package etracks.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class mixinfo extends Activity {

    String mixId = "";

    String cover_url = "";

    String mixTitle = "";

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mixinfo);
        Bundle bun = getIntent().getExtras();
        String info = bun.getString("param_string");
        String Req = "http://8tracks.com/mixes/" + info + ".json?api_key=44c6da02712ba6681580ae23a035fea62635a970";
        mixId = info;
        URL ReqURL;
        try {
            ReqURL = new URL(Req);
            HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
            conn.setRequestProperty("User-Agent", "Agent");
            BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String source = buff.readLine();
            JSONObject js = new JSONObject(source);
            JSONObject mix = js.getJSONObject("mix");
            JSONObject covers = mix.getJSONObject("cover_urls");
            JSONObject userInfo = mix.getJSONObject("user");
            cover_url = covers.getString("max200");
            ImageView im = (ImageView) findViewById(R.id.MixInfoMainImage);
            im.setImageBitmap(fetchImage(cover_url));
            String mixNameStr = mix.getString("name");
            mixTitle = mixNameStr;
            TextView MixName = (TextView) findViewById(R.id.MixInfoNameTv);
            MixName.setText(mixNameStr);
            String mixDescStr = mix.getString("description").replace("\r", "");
            TextView MixDesc = (TextView) findViewById(R.id.MixInfoDescTv);
            MixDesc.setText(mixDescStr);
            String mixTagsStr = mix.getString("tag_list_cache");
            TextView MixTags = (TextView) findViewById(R.id.MixInfoTagsTv);
            MixTags.setText(mixTagsStr);
            String mixCreatorStr = userInfo.getString("login");
            TextView MixCreator = (TextView) findViewById(R.id.MixInfoCreatorTv);
            MixCreator.setText(mixCreatorStr);
        } catch (Exception e) {
        }
    }

    private Bitmap fetchImage(String urlstr) throws Exception {
        URL url;
        url = new URL(urlstr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setDoInput(true);
        c.setRequestProperty("User-Agent", "Agent");
        c.connect();
        InputStream is = c.getInputStream();
        Bitmap img;
        img = BitmapFactory.decodeStream(is);
        return img;
    }

    public void PlayMixHandler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        String Req = "http://8tracks.com/sets/new.json?api_key=44c6da02712ba6681580ae23a035fea62635a970";
        String token = "";
        URL ReqURL;
        try {
            ReqURL = new URL(Req);
            HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
            conn.setRequestProperty("User-Agent", "Agent");
            BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String source = buff.readLine();
            JSONObject js = new JSONObject(source);
            token = js.getString("play_token");
        } catch (Exception e) {
            e.printStackTrace();
        }
        bun.putString("param_string", mixId);
        bun.putString("param_string1", token);
        bun.putString("param_string2", cover_url);
        bun.putString("param_string3", mixTitle);
        intent.setClass(this, playmix.class);
        intent.putExtras(bun);
        startActivity(intent);
    }
}
