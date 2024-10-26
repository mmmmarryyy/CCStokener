package limaCity.Webapp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;

public class StartActivity extends Activity {

    private static final String PREFS_NAME = "LimaCityPrefs";

    private SharedPreferences settings;

    SharedPreferences.Editor settingsEdit;

    HashMap<String, Cookie> cookiesArray = null;

    private HttpPost httpPost = new HttpPost("https://www.lima-city.de/");

    private DefaultHttpClient httpclient;

    public CookieManager cookieManager = CookieManager.getInstance();

    private ProgressDialog progressDialog;

    public backgroundActivity bg = null;

    RequestActionListener requestActionListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        settings = this.getSharedPreferences(PREFS_NAME, MODE_WORLD_WRITEABLE);
        httpclient = new DefaultHttpClient();
        settingsEdit = settings.edit();
        buttonReactionInit();
    }

    public void buttonReactionInit() {
        Button loginButton = (Button) findViewById(R.id.loginButton);
        progressDialog = new ProgressDialog(StartActivity.this);
        final EditText username = (EditText) findViewById(R.id.getUsername);
        final EditText password = (EditText) findViewById(R.id.getPassword);
        loginButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                progressDialog.setMessage("Versuche einzuloggen...");
                progressDialog.show();
                login(username.getText().toString(), password.getText().toString());
            }
        });
        this.setOnLoggedInListener(new RequestActionListener() {

            public void onLoggedIn(Boolean loggedin, String username) {
                progressDialog.dismiss();
                if (loggedin) {
                    loggedIn(username);
                }
            }
        });
    }

    public void loggedIn(String username) {
        settingsEdit.putString("username", username);
        settingsEdit.putBoolean("isLoggedIn", true);
        settingsEdit.commit();
        Intent intent = new Intent(StartActivity.this, MainActivity.class);
        StartActivity.this.startActivity(intent);
    }

    public void login(final String username, final String password) {
        new Thread(new Runnable() {

            public void run() {
                InputStream content = null;
                boolean loggedin = false;
                try {
                    Log.d("http", "Start httpPost");
                    List nameValuePairs = new ArrayList(2);
                    nameValuePairs.add(new BasicNameValuePair("form_username", username));
                    nameValuePairs.add(new BasicNameValuePair("form_password", password));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
                    httpPost.getParams().setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);
                    Log.d("http", "get response");
                    HttpResponse response = httpclient.execute(httpPost);
                    content = response.getEntity().getContent();
                    Log.d("http", "read out response");
                    response.getStatusLine().getStatusCode();
                    cookiesArray = getCookies(response);
                    Iterator iterator = cookiesArray.keySet().iterator();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        settingsEdit.putString("cookie_" + key, "[" + cookiesArray.get(key).getValue() + "]" + "[" + cookiesArray.get(key).getDomain() + "]");
                    }
                    settingsEdit.commit();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(content), 4096);
                    String line;
                    while (((line = rd.readLine()) != null) && loggedin != true) {
                        if (line.contains("href=\"/usercp\"")) {
                            loggedin = true;
                            break;
                        }
                    }
                    rd.close();
                } catch (Exception e) {
                }
                requestActionListener.onLoggedIn(loggedin, username);
            }
        }).start();
    }

    private HashMap<String, Cookie> getCookies(HttpResponse response) {
        HashMap<String, Cookie> sessionCookie = new HashMap<String, Cookie>();
        CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
        Header[] allHeaders = response.getAllHeaders();
        CookieOrigin origin = new CookieOrigin("http://www.lima-city.de/", 8080, "/", false);
        for (Header header : allHeaders) {
            List<Cookie> parse;
            try {
                parse = cookieSpecBase.parse(header, origin);
                for (Cookie cookie : parse) {
                    sessionCookie.put(cookie.getName().toString(), cookie);
                }
            } catch (MalformedCookieException e) {
                e.printStackTrace();
            }
        }
        return sessionCookie;
    }

    public void setOnLoggedInListener(RequestActionListener listener) {
        requestActionListener = listener;
    }

    public interface RequestActionListener {

        public abstract void onLoggedIn(Boolean loggedin, String username);
    }
}
