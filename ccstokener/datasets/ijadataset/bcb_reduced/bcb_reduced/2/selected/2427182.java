package com.totsp.networking;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class JavaNetHttpGet extends Activity {

    private EditText getInput;

    private TextView getOutput;

    private Button getButton;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.java_net_http_simple);
        getInput = (EditText) findViewById(R.id.get_input);
        getOutput = (TextView) findViewById(R.id.get_output);
        getButton = (Button) findViewById(R.id.get_button);
        getButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                getOutput.setText("");
                String output = getHttpResponse(getInput.getText().toString());
                if (output != null) {
                    getOutput.setText(output);
                }
            }
        });
    }

    ;

    /**
    * Perform an HTTP GET with HttpUrlConnection.
    * 
    * @param location
    * @return
    */
    private String getHttpResponse(String location) {
        String result = null;
        URL url = null;
        try {
            url = new URL(location);
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
        }
        if (url != null) {
            try {
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                String inputLine;
                int lineCount = 0;
                while ((lineCount < 10) && ((inputLine = in.readLine()) != null)) {
                    lineCount++;
                    result += "\n" + inputLine;
                }
                in.close();
                urlConn.disconnect();
            } catch (IOException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
            }
        } else {
            Log.e(Constants.LOG_TAG, "url NULL");
        }
        return result;
    }
}
