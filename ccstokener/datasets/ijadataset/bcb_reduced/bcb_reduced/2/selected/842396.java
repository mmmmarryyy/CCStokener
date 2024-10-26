package etracks.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class etracks extends Activity {

    private static boolean logged = false;

    private static String userToken = null;

    public static void setUserToken(String token) {
        userToken = token;
    }

    public static void setLogged(boolean l) {
        logged = l;
    }

    LinkedList<String> musicIds = new LinkedList<String>();

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button lb = (Button) findViewById(R.id.login);
        if (logged) {
            userToken = null;
            lb.setText("Logout");
        } else lb.setText("Login");
        String Req = "http://8tracks.com/mixes.json?api_key=44c6da02712ba6681580ae23a035fea62635a970&per_page=9";
        URL ReqURL;
        try {
            ReqURL = new URL(Req);
            HttpURLConnection conn = (HttpURLConnection) ReqURL.openConnection();
            conn.setRequestProperty("User-Agent", "Agent");
            BufferedReader buff = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String source = buff.readLine();
            JSONObject js = new JSONObject(source);
            JSONArray mixesA = js.getJSONArray("mixes");
            for (int i = 0; i < mixesA.length(); ++i) {
                JSONObject mix = mixesA.getJSONObject(i);
                musicIds.add(mix.getString("id"));
                JSONObject cover = mix.getJSONObject("cover_urls");
                switch(i) {
                    case 0:
                        ImageView miximg1 = (ImageView) findViewById(R.id.Mainmixes1);
                        miximg1.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 1:
                        ImageView miximg2 = (ImageView) findViewById(R.id.Mainmixes2);
                        miximg2.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 2:
                        ImageView miximg3 = (ImageView) findViewById(R.id.Mainmixes3);
                        miximg3.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 3:
                        ImageView miximg4 = (ImageView) findViewById(R.id.Mainmixes4);
                        miximg4.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 4:
                        ImageView miximg5 = (ImageView) findViewById(R.id.Mainmixes5);
                        miximg5.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 5:
                        ImageView miximg6 = (ImageView) findViewById(R.id.Mainmixes6);
                        miximg6.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 6:
                        ImageView miximg7 = (ImageView) findViewById(R.id.Mainmixes7);
                        miximg7.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 7:
                        ImageView miximg8 = (ImageView) findViewById(R.id.Mainmixes8);
                        miximg8.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    case 8:
                        ImageView miximg9 = (ImageView) findViewById(R.id.Mainmixes9);
                        miximg9.setImageBitmap(fetchImage(cover.getString("sq100")));
                        break;
                    default:
                        break;
                }
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

    public void myMixesMainHandler(View v) throws IOException {
        Intent it = new Intent(Intent.ACTION_VIEW);
        it.setClassName(this, mixes.class.getName());
        startActivity(it);
    }

    public void myLoginHandler(View v) {
        if (!logged) {
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setClassName(this, login.class.getName());
            startActivity(it);
        } else {
            Button lb = (Button) findViewById(R.id.login);
            lb.setText("Login");
            logged = false;
        }
    }

    public void searchMixHandler(View v) {
        EditText toSearch = (EditText) findViewById(R.id.searchMPT);
        String s = toSearch.getText().toString();
        String[] splited = s.split(" ");
        s = "";
        for (int i = 0; i < splited.length; ++i) {
            if (i == (splited.length - 1)) s += splited[i]; else s += splited[i] + "+";
        }
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", s);
        bun.putBoolean("param_bool", false);
        intent.setClass(this, mixquery.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic1Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(0));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic2Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(1));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic3Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(2));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic4Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(3));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic5Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(4));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic6Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(5));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic7Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(6));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic8Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(7));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }

    public void getMusic9Handler(View v) {
        Intent intent = new Intent();
        Bundle bun = new Bundle();
        bun.putString("param_string", musicIds.get(8));
        intent.setClass(this, mixinfo.class);
        intent.putExtras(bun);
        startActivity(intent);
    }
}
