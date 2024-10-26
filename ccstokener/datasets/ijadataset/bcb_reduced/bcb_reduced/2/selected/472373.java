package my.study.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class HttpActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
            StringBuilder sb = new StringBuilder();
            URL url = new URL("https://ajax.googleapis.com/ajax/services/search/news?v=1.0&q=google");
            URLConnection connection = url.openConnection();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) sb.append(line);
            JSONObject json = new JSONObject(sb.toString());
            sb.setLength(0);
            JSONObject responseData = (JSONObject) json.get("responseData");
            JSONArray results = (JSONArray) responseData.get("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = (JSONObject) results.get(i);
                sb.append(result.get("title")).append("\n\n");
            }
            TextView tv = (TextView) findViewById(R.id.textView);
            tv.setText(sb.toString());
        } catch (Exception e) {
        }
    }
}
