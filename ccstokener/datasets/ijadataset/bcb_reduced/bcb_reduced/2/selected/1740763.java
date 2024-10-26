package limaCity.Webapp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.CoreProtocolPNames;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "LimaCityPrefs";

    private SharedPreferences settings;

    SharedPreferences.Editor settingsEdit;

    HashMap<String, Cookie> cookiesArray = null;

    private HttpGet httpget = new HttpGet("https://www.lima-city.de/");

    private DefaultHttpClient httpclient = new DefaultHttpClient();

    public BasicCookieStore cookiestore = new BasicCookieStore();

    private List<String> guldenArray = new ArrayList();

    private ArrayAdapter guldenAdapter = null;

    ListView gulden;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        settings = this.getSharedPreferences(PREFS_NAME, MODE_WORLD_WRITEABLE);
        settingsEdit = settings.edit();
        getCookies();
        setUsername(settings.getString("username", "none"));
        gulden = (ListView) findViewById(R.id.guldenView);
        guldenAdapter = new ArrayAdapter(this, R.layout.list_item, guldenArray);
        gulden.setAdapter(guldenAdapter);
        getData();
    }

    private void getCookies() {
        Iterator iterator = settings.getAll().keySet().iterator();
        while (iterator.hasNext()) {
            String key = ((String) iterator.next());
            if (key.startsWith("cookie_")) {
                cookiestore.addCookie(new BasicClientCookie(key.replace("cookie_", ""), settings.getString(key, "")));
            }
            httpclient.setCookieStore(cookiestore);
        }
    }

    public void getData() {
        new Thread(new Runnable() {

            public void run() {
                InputStream content = null;
                boolean loggedin = false;
                try {
                    Log.d("http", "Start get");
                    httpget.getParams().setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);
                    Log.d("http", "get response");
                    HttpResponse response = httpclient.execute(httpget);
                    content = response.getEntity().getContent();
                    Log.d("http", "read out response");
                    BufferedReader rd = new BufferedReader(new InputStreamReader(content), 4096);
                    String line;
                    while ((line = rd.readLine()) != null) {
                        Log.d("getData()", line);
                        if (line.contains("<a href=\"/usercp/page%3Agulden\">\"")) {
                            getGulden(line.toCharArray());
                            loggedin = true;
                            break;
                        }
                    }
                    rd.close();
                    if (!loggedin) {
                        loggedOut(settings.getString("username", "none"));
                    }
                } catch (Exception e) {
                    Log.e("getData()", e.getCause().toString());
                }
                guldenAdapter.notifyDataSetChanged();
            }
        }).start();
    }

    private void getGulden(char[] chars) {
        String guldenString = null;
        Log.d("http", "gulden");
        boolean number = false;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isDigit(chars[i])) {
                number = true;
                guldenString = guldenString + chars[i];
            } else if (number) {
                break;
            }
        }
        if (guldenArray.size() == 1) {
            guldenArray.add(1, guldenString);
        } else {
            guldenArray.set(1, guldenString);
        }
    }

    public void loggedOut(String username) {
        settingsEdit.clear();
        settingsEdit.commit();
        settingsEdit.putString("username", username);
        settingsEdit.putBoolean("isLoggedIn", false);
        settingsEdit.commit();
        Intent intent = new Intent(MainActivity.this, StartActivity.class);
        MainActivity.this.startActivity(intent);
    }

    private void setUsername(String username) {
        TextView usernameView = (TextView) this.findViewById(R.id.userName);
        usernameView.setText(username);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.refresh:
                refresh();
                return true;
            case R.id.logout:
                logout();
                return true;
            case R.id.help:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Wirklich ausloggen?").setCancelable(false).setPositiveButton("Ja", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                MainActivity.this.settingsEdit.clear();
                MainActivity.this.settingsEdit.commit();
                MainActivity.this.finish();
            }
        }).setNegativeButton("Nein", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create();
        builder.show();
    }

    private void showHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name).setMessage(R.string.app_version).setCancelable(true).setNeutralButton("Schlie�en", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    private void refresh() {
        getData();
    }
}
