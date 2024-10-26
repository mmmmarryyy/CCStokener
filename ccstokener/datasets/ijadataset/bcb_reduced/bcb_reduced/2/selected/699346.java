package net.learn2develop.AndroidViews;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import net.learn2develop.R;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONStringer;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class SavePerson02 extends Activity implements OnClickListener {

    private static final String SERVICE_URI = "http://192.168.61.3/RestServicePost/RestServiceImpl.svc";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save);
        View add = findViewById(R.id.Button01add);
        add.setOnClickListener(this);
        View home = findViewById(R.id.Button01home);
        home.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.Button01home:
                break;
            case R.id.Button01add:
                View editText1 = (EditText) findViewById(R.id.name);
                View editText2 = (EditText) findViewById(R.id.number);
                View editText3 = (EditText) findViewById(R.id.skypeId);
                View editText4 = (EditText) findViewById(R.id.address);
                String myEditText1 = ((TextView) editText1).getText().toString();
                String myEditText2 = ((TextView) editText2).getText().toString();
                String myEditText3 = ((TextView) editText3).getText().toString();
                String myEditText4 = ((TextView) editText4).getText().toString();
                String myEditString = new String(myEditText1 + "|" + myEditText2 + "|" + myEditText3 + "|" + myEditText4);
                HttpPost request = new HttpPost(SERVICE_URI + "/json/adduser");
                request.setHeader("Accept", "application/json");
                request.setHeader("Content-type", "application/json");
                String not = new String(" ");
                try {
                    JSONStringer vehicle = new JSONStringer().object().key("rData").object().key("details").value(myEditString).endObject().endObject();
                    StringEntity entity = new StringEntity(vehicle.toString());
                    Toast.makeText(this, vehicle.toString() + "\n", Toast.LENGTH_LONG).show();
                    request.setEntity(entity);
                    DefaultHttpClient httpClient = new DefaultHttpClient();
                    HttpResponse response = httpClient.execute(request);
                } catch (Exception e) {
                    not = "NOT ";
                }
                Toast.makeText(this, not + " OK ! " + "\n", Toast.LENGTH_LONG).show();
                break;
        }
    }
}
