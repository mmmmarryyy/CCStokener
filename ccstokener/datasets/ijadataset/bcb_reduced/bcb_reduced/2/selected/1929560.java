package org.cloud.android.activity;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.cloud.android.Constant;
import org.cloud.android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

    /** Called when the activity is first created. */
    EditText login = null;

    EditText psw = null;

    TextView welcome = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        login = (EditText) this.findViewById(R.id.login_id);
        psw = (EditText) this.findViewById(R.id.login_psw);
        welcome = (TextView) this.findViewById(R.id.infotext);
        addButtonListener();
    }

    private void addButtonListener() {
        Button confirm = (Button) this.findViewById(R.id.login_confirm);
        confirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("https://mt0-app.cloud.cm/rpc/json");
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("m", "login"));
                nameValuePairs.add(new BasicNameValuePair("c", "User"));
                nameValuePairs.add(new BasicNameValuePair("password", "cloudisgreat"));
                nameValuePairs.add(new BasicNameValuePair("alias", "cs588"));
                String result = "";
                try {
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    HttpResponse response = httpclient.execute(httppost);
                    result = EntityUtils.toString(response.getEntity());
                    String info[] = result.split(",");
                    String info1 = info[0].trim();
                    String loginResult[] = info1.split(":");
                    String lResult = loginResult[1];
                    Header[] acturalHeaders = response.getAllHeaders();
                    String sessionId = substractSessionId(acturalHeaders);
                    Log.d("SessionId in the Header:", sessionId);
                    if (lResult.equals("0")) {
                        Intent i = new Intent(LoginActivity.this, DestopActivity.class);
                        i.putExtra(Constant.PHP_SESSION_ID, sessionId);
                        startActivity(i);
                    } else if (lResult.equals("-1")) {
                        welcome.setText(Constant.LOGIN_ERROR_MESSAGE);
                    }
                    Log.d("Cloud Debug", lResult);
                } catch (Exception e) {
                    e.printStackTrace();
                    result = e.getMessage();
                }
                Log.d("MSG", result);
            }
        });
        Button clear = (Button) this.findViewById(R.id.login_clear);
        clear.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                login.setText("");
                psw.setText("");
            }
        });
        Button reg = (Button) this.findViewById(R.id.register);
        reg.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });
    }

    private String substractSessionId(Header headers[]) {
        String result = "";
        for (Header h : headers) {
            Log.d("Header", h.getName() + " " + h.getValue());
            String name = h.getName();
            String value = h.getValue();
            if (name.trim().toLowerCase().contains("set-cookie")) {
                String[] pairs = value.split(";");
                for (String pair : pairs) {
                    String[] p = pair.split("=");
                    if (p.length != 2) continue;
                    if (p[0].trim().toLowerCase().equals("phpsessid")) {
                        result = p[1];
                    }
                }
            }
        }
        return result;
    }
}
