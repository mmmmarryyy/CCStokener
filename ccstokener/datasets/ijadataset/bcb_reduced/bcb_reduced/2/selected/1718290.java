package com.esp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;

public class ESP_ClientActivity extends Activity {

    private EditText editorId, editorPwd, editorUrl;

    private EditText editorLatitude, editorLongtitude;

    private Button btnSubmit;

    private WebView wv;

    private GPSSender sender = new GPSSender();

    Thread t;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnSubmit = (Button) findViewById(R.id.submit);
        editorId = (EditText) findViewById(R.id.editorId);
        editorPwd = (EditText) findViewById(R.id.editorPwd);
        editorUrl = (EditText) findViewById(R.id.editorUrl);
        editorLatitude = (EditText) findViewById(R.id.editorlatitdue);
        editorLongtitude = (EditText) findViewById(R.id.editorlongtitude);
        wv = (WebView) findViewById(R.id.myWebView1);
        wv.setWebViewClient(new WebViewClient() {
        });
        btnSubmit.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                String username = editorId.getText().toString();
                String password = editorPwd.getText().toString();
                String host = editorUrl.getText().toString();
                String hostURL = "http://" + host + ((host.endsWith("/")) ? "" : "/");
                String upGPSURL = hostURL + "postGPS";
                Integer deviceSN = 0;
                sender.init(username, password, deviceSN, upGPSURL);
                t = new Thread(sender);
                t.start();
                wv.loadUrl(hostURL);
            }
        });
    }

    class GPSSender implements Runnable {

        private String userId;

        private String password;

        private Integer deviceSN;

        private String serverURL;

        private Double latitude;

        private Double longtitude;

        private LocationManager mLocationManager01;

        private String strLocationPrivider = "";

        private Location mLocation01 = null;

        private GPSSender() {
            super();
        }

        public void init(String userId, String password, Integer deviceSN, String serverURL) {
            this.userId = userId;
            this.password = password;
            this.deviceSN = deviceSN;
            this.serverURL = serverURL;
            mLocationManager01 = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLocation01 = getLocationPrivider(mLocationManager01);
            if (mLocation01 != null) {
                processLocationUpdated(mLocation01);
            } else {
                System.err.println(getResources().getText(R.string.str_err_location).toString());
            }
        }

        @Override
        public void run() {
            while (true) {
                if (mLocation01 != null) {
                    processLocationUpdated(mLocation01);
                } else {
                    System.err.println(getResources().getText(R.string.str_err_location).toString());
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public Location getLocationPrivider(LocationManager lm) {
            Location retLocation = null;
            try {
                Criteria mCriteria01 = new Criteria();
                while (strLocationPrivider == null || strLocationPrivider.equals("")) {
                    strLocationPrivider = lm.getBestProvider(mCriteria01, true);
                    Thread.sleep(1000);
                }
                while (retLocation == null) {
                    retLocation = lm.getLastKnownLocation(strLocationPrivider);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retLocation;
        }

        private void processLocationUpdated(Location location) {
            String la = String.valueOf(location.getLatitude());
            String lo = String.valueOf(location.getLongitude());
            HttpPost req = new HttpPost(serverURL);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("userId", userId));
            params.add(new BasicNameValuePair("password", password));
            params.add(new BasicNameValuePair("deviceSN", deviceSN.toString()));
            params.add(new BasicNameValuePair("latitude", la));
            params.add(new BasicNameValuePair("longtitude", lo));
            try {
                req.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
                HttpResponse httpResponse = new DefaultHttpClient().execute(req);
                System.err.println(httpResponse.toString());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
