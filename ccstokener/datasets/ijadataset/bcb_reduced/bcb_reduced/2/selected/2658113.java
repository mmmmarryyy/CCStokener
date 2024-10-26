package net.learn2develop.AndroidViews;

import android.app.Activity;
import android.os.Bundle;
import android.app.ListActivity;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;
import android.app.Activity;
import android.os.Bundle;
import android.util.Xml;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import net.learn2develop.R;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import org.json.JSONStringer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.InputStream;
import org.json.JSONArray;
import java.io.BufferedReader;

public class GetPersons01 extends ListActivity {

    private static final String SERVICE_URI = "http://192.168.61.3/RestServicePost/RestServiceImpl.svc";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);
        HttpGet request = new HttpGet(SERVICE_URI + "/json/getallpersons");
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String theString = new String("");
        try {
            HttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream stream = responseEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            stream.close();
            theString = builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, theString + "\n", Toast.LENGTH_LONG).show();
    }
}
