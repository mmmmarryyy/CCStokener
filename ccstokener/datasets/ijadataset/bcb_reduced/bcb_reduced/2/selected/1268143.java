package etracks.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class login extends Activity {

    final String apiKey = "44c6da02712ba6681580ae23a035fea62635a970";

    final String authenticationURL = "https://8tracks.com/sessions.json?api_key=" + apiKey;

    final int statusOK = 200;

    final int statusUnprocessable = 422;

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_NoTitleBar_Fullscreen);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loginpage);
        Log.d("login_section", "entered login activity...");
    }

    public void myCheckCredHandler(View v) {
        Log.d("login_section", "entered handler");
        EditText Username = (EditText) findViewById(R.id.Username);
        EditText Password = (EditText) findViewById(R.id.user_pass);
        String username_S = Username.getText().toString();
        String pass_S = Password.getText().toString();
        TextView ltv = (TextView) findViewById(R.id.LoginPagetv);
        HttpClient httpclient = createHttpClient();
        HttpPost httppost = new HttpPost(authenticationURL);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("login", username_S));
            nameValuePairs.add(new BasicNameValuePair("password", pass_S));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            int status = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            Log.d("login_section", responseBody);
            JSONObject jsonObject = new JSONObject(responseBody);
            if (status == this.statusOK && jsonObject.getBoolean("logged_in")) {
                ltv.setText("You have been logged in. :D");
                etracks.setLogged(true);
                etracks.setUserToken(jsonObject.getString("user_token"));
                Intent it = new Intent(Intent.ACTION_VIEW);
                it.setClassName(this, etracks.class.getName());
                this.finish();
                startActivity(it);
            } else if (status == this.statusUnprocessable && !jsonObject.getBoolean("logged_in")) {
                if (!jsonObject.isNull("errors")) ltv.setText(jsonObject.getString("errors")); else ltv.setText("login unsuccessful");
            } else Log.d("login_section", "what just happened?");
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private HttpClient createHttpClient() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);
        return new DefaultHttpClient(conMgr, params);
    }
}
