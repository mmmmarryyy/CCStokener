package com.totsp.networking;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import com.totsp.networking.util.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.IOException;

/**
 * Android basic HTTP example using Apache HttpClient.
 * 
 * 
 * @author charliecollins
 * 
 */
public class ApacheHttpSimple extends Activity {

    private static final String URL1 = "http://www.comedycentral.com/rss/jokes/index.jhtml";

    private static final String URL2 = "http://feeds.theonion.com/theonion/daily";

    private Spinner urlChooser;

    private Button button;

    private TextView output;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.apache_http_simple);
        urlChooser = (Spinner) findViewById(R.id.apache_url);
        ArrayAdapter<String> urls = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] { ApacheHttpSimple.URL1, ApacheHttpSimple.URL2 });
        urls.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        urlChooser.setAdapter(urls);
        button = (Button) findViewById(R.id.apachego_button);
        output = (TextView) findViewById(R.id.apache_output);
        button.setOnClickListener(new OnClickListener() {

            public void onClick(final View v) {
                ApacheHttpSimple.this.output.setText("");
                new HttpRequestTask().execute(ApacheHttpSimple.this.urlChooser.getSelectedItem().toString());
            }
        });
    }

    ;

    private class HttpRequestTask extends AsyncTask<String, Void, String> {

        private final ProgressDialog dialog = new ProgressDialog(ApacheHttpSimple.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Performing HTTP request...");
            dialog.show();
        }

        @Override
        protected String doInBackground(final String... args) {
            String result = null;
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet httpMethod = new HttpGet(args[0]);
            try {
                HttpResponse response = client.execute(httpMethod);
                HttpEntity entity = response.getEntity();
                result = StringUtils.inputStreamToString(entity.getContent());
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(final String result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            ApacheHttpSimple.this.output.setText(result);
        }
    }
}
