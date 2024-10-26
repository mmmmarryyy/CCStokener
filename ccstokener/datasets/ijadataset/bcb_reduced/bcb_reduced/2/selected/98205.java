package com.example.android.apis;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class NewsActivity extends Activity {

    private TextView text;

    private ImageView image;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newsactivity);
        text = (TextView) findViewById(R.id.NewsActivityText);
        image = (ImageView) findViewById(R.id.NewsActivityImage);
        Intent intent = this.getIntent();
        String u = intent.getStringExtra("newsurl");
        URL url = null;
        try {
            url = new URL(u);
            String jsonText = getJSONScoreStringFromNet(url);
            JSONObject j1 = new JSONObject(jsonText);
            URL urlText = new URL(j1.getString("actualpicurl"));
            String actualText = j1.getString("actualtext");
            text.setText(actualText);
            InputStream ins = null;
            try {
                ins = urlText.openStream();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap b = BitmapFactory.decodeStream(new FlushedInputStream(ins));
            image.setImageBitmap(b);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getJSONScoreStringFromNet(URL urladdress) {
        InputStream instream = null;
        BufferedReader read = null;
        try {
            instream = urladdress.openStream();
            read = new BufferedReader(new InputStreamReader(instream));
            String s = new String("");
            String line = null;
            while ((line = read.readLine()) != null) {
                s = s + line;
            }
            return s;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                read.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

class FlushedInputStream extends FilterInputStream {

    public FlushedInputStream(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    public long skip(long n) throws IOException {
        long totalBytesSkipped = 0L;
        while (totalBytesSkipped < n) {
            long bytesSkipped = in.skip(n - totalBytesSkipped);
            if (bytesSkipped == 0L) {
                int b = read();
                if (b < 0) {
                    break;
                } else {
                    bytesSkipped = 1;
                }
            }
            totalBytesSkipped += bytesSkipped;
        }
        return totalBytesSkipped;
    }
}
