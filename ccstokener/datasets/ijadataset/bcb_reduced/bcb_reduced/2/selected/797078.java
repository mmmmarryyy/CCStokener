package net.sylvek.taiyaki;

import java.io.IOException;
import net.sylvek.taiyaki.widget.JSONArrayAdapter;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * 
 * @author Sylvain Maucourt (smaucourt@gmail.com)
 * 
 */
public class Taiyaki extends ListActivity {

    public static final String TAIYAKI_URL = "net.sylvek.taiyaki.url";

    private static final String TAIYAKI_DEVICES = "devices.json";

    private static final String TAIYAKI_REST = "rest/";

    public static final int FROM_PREF = 0;

    public static final int FROM_LIST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getDevices();
        startX10Listener();
        initPreference();
    }

    private void initPreference() {
        final Intent intent = new Intent(this, Preference.class);
        Button pref = (Button) findViewById(R.id.pref);
        pref.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                startActivityForResult(intent, FROM_PREF);
            }
        });
    }

    private void startX10Listener() {
        Intent service = new Intent(this, X10Listener.class);
        startService(service);
    }

    private void getDevices() {
        SharedPreferences pref = getSharedPreferences(Preference.TAIYAKI_PREF, MODE_PRIVATE);
        String taiyaki = pref.getString(TAIYAKI_URL, null);
        if (taiyaki == null) {
            return;
        }
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(taiyaki + TAIYAKI_DEVICES);
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                String res = EntityUtils.toString(response.getEntity());
                setListAdapter(new JSONArrayAdapter(this, android.R.layout.simple_list_item_1, new JSONArray(res), "name"));
            }
        } catch (ClientProtocolException e) {
            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
        } catch (IOException e) {
            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
        } catch (JSONException e) {
            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case FROM_PREF:
                if (resultCode == RESULT_OK) {
                    getDevices();
                }
                break;
            case FROM_LIST:
            default:
                switch(resultCode) {
                    case RESULT_CANCELED:
                        break;
                    case RESULT_OK:
                    default:
                        SharedPreferences pref = getSharedPreferences(Preference.TAIYAKI_PREF, MODE_PRIVATE);
                        String taiyaki = pref.getString(TAIYAKI_URL, null);
                        if (taiyaki == null) {
                            return;
                        }
                        HttpClient client = new DefaultHttpClient();
                        HttpGet get = new HttpGet(taiyaki + TAIYAKI_REST + data.getDataString());
                        try {
                            HttpResponse response = client.execute(get);
                            if (response.getStatusLine().getStatusCode() == 200) {
                                String res = EntityUtils.toString(response.getEntity());
                                JSONObject result = new JSONObject(res);
                                if (!result.getBoolean("success")) {
                                    new AlertDialog.Builder(this).setMessage(result.getString("result")).show();
                                    return;
                                }
                                Intent sended = new Intent(X10Receiver.ACTION_DIRECT);
                                sended.putExtra(X10Receiver.RESULT, res);
                                sendBroadcast(sended);
                            }
                        } catch (ClientProtocolException e) {
                            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
                        } catch (IOException e) {
                            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
                        } catch (JSONException e) {
                            new AlertDialog.Builder(this).setMessage(e.getMessage()).show();
                        }
                }
        }
    }
}
