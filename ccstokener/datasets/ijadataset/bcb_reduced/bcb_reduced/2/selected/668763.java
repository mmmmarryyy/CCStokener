package net.learn2develop.AndroidViews;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import net.learn2develop.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONStringer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class SavePerson extends Activity {

    private static final String SERVICE_URI = "http://192.168.61.3/RestServicePost/RestServiceImpl.svc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        HttpPost request = new HttpPost(SERVICE_URI + "/json/adduser");
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        String not = new String(" ");
        try {
            JSONStringer vehicle = new JSONStringer().object().key("rData").object().key("details").value("bar|bob|b@h.us|why").endObject().endObject();
            StringEntity entity = new StringEntity(vehicle.toString());
            Toast.makeText(this, vehicle.toString() + "\n", Toast.LENGTH_LONG).show();
            request.setEntity(entity);
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(request);
            Toast.makeText(this, response.getStatusLine().getStatusCode() + "\n", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            not = "NOT ";
        }
        Toast.makeText(this, not + " OK ! " + "\n", Toast.LENGTH_LONG).show();
    }
}
