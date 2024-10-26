package etracks.app;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class mixquery extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mixquery);
        LinearLayout mainl = (LinearLayout) findViewById(R.id.layout_mixquery);
        Bundle bun = getIntent().getExtras();
        String info = bun.getString("param_string");
        boolean tag = bun.getBoolean("param_bool");
        String Req = "";
        if (tag) {
            Req = "http://8tracks.com/mixes.json?api_key=44c6da02712ba6681580ae23a035fea62635a970&tag=" + info + "&per_page=10";
        } else Req = "http://8tracks.com/mixes.json?api_key=44c6da02712ba6681580ae23a035fea62635a970&q=" + info + "&per_page=10";
        try {
            URL ReqURL = new URL(Req);
            HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
            conn.setRequestProperty("User-Agent", "Agent");
            BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String source = buff.readLine();
            JSONObject js = new JSONObject(source);
            JSONArray mixesA = js.getJSONArray("mixes");
            for (int i = 0; i < mixesA.length(); ++i) {
                JSONObject mix = mixesA.getJSONObject(i);
                final JSONObject cover = mix.getJSONObject("cover_urls");
                LinearLayout l = new LinearLayout(this);
                l.setOrientation(LinearLayout.HORIZONTAL);
                final ImageView im = new ImageView(this);
                TextView txt = new TextView(this);
                txt.setGravity(Gravity.CENTER_VERTICAL);
                txt.setPadding(8, 0, 0, 0);
                im.setImageBitmap(fetchImage(cover.getString("sq100")));
                im.setClickable(true);
                final String mixid = mix.getString("id");
                im.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        Intent intent = new Intent();
                        Bundle bun = new Bundle();
                        bun.putString("param_string", mixid);
                        intent.setClass(mixquery.this, mixinfo.class);
                        intent.putExtras(bun);
                        startActivity(intent);
                    }
                });
                txt.setText(mix.getString("name"));
                l.addView(im);
                l.addView(txt);
                mainl.addView(l);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
}
