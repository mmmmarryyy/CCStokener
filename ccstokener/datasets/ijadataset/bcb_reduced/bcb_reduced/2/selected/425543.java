package com.fluo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class Fluo extends Activity implements SyncElement {

    static ArrayList<String> desktopServerList = new ArrayList<String>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final String address = "http://www.alexbednarczyk.com/upload/files/1/School/fluointro.txt";
        final TextView content = (TextView) findViewById(R.id.content);
        GetText(content, address);
        final ImageButton button = (ImageButton) findViewById(R.id.next_button);
        button.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Socket socket;
                ObjectOutputStream output;
                FluoMessage msg;
                try {
                    socket = new Socket(SERVER_NAME, SERVER_PORT);
                    output = new ObjectOutputStream(socket.getOutputStream());
                    msg = new FluoMessage("android");
                    output.writeObject(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startActivity(new Intent(Fluo.this, Login.class));
            }
        });
    }

    public void GetText(TextView content, String address) {
        String url = address;
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            content.setText(TextHelper.GetText(response));
        } catch (Exception ex) {
            content.setText("Welcome to Fluo. Failed to connect to intro server.");
        }
    }
}
