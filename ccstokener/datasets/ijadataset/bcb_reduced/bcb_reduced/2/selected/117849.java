package com.wei.weiAndroidTest1;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class CopyOfweiAndroidTest1Backup extends Activity {

    /** Called when the activity is first created. */
    Handler h;

    String imageUrl = "";

    Bitmap bmImg;

    ImageView imView;

    Button button1;

    public Bitmap returnBitMap(String url, String PHPSessionID, String currentUsedPHPSESSIDString) {
        URL myFileUrl = null;
        URL myUploadFileUrl = null;
        OutputStream os = null;
        Bitmap bitmap = null;
        try {
            myFileUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
            conn.setRequestProperty("Cookie", PHPSessionID);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final EditText eText = (EditText) findViewById(R.id.address);
        final TextView tView = (TextView) findViewById(R.id.pagetext);
        final Button button = (Button) findViewById(R.id.ButtonGo);
        button.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {
                try {
                    tView.setText("test2");
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost("https://mt1-app.cloud.cm:443/rpc/json");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("m", "login"));
                    nameValuePairs.add(new BasicNameValuePair("c", "User"));
                    nameValuePairs.add(new BasicNameValuePair("password", "cloudisgreat"));
                    nameValuePairs.add(new BasicNameValuePair("alias", "cs588"));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    HttpResponse response = httpclient.execute(httppost);
                    String acturalData = "";
                    Header[] acturalHeaders = null;
                    String acturalHeaderString = "";
                    String currentUsedPHPSESSIDName = "";
                    String currentUsedPHPSESSIDValue = "";
                    String currentUsedPHPSESSIDString = "";
                    acturalData = EntityUtils.toString(response.getEntity());
                    acturalHeaders = response.getAllHeaders();
                    int counter = 0;
                    currentUsedPHPSESSIDName = "HeaderName:" + acturalHeaders[7].getName();
                    currentUsedPHPSESSIDString = acturalHeaders[7].getValue().split(";")[0].split("=")[1];
                    currentUsedPHPSESSIDValue = "PHPSESSID=" + currentUsedPHPSESSIDString;
                    acturalHeaderString += currentUsedPHPSESSIDName;
                    acturalHeaderString += "\n";
                    acturalHeaderString += currentUsedPHPSESSIDValue;
                    acturalHeaderString += "\n";
                    for (counter = 0; counter < acturalHeaders.length; counter++) {
                        acturalHeaderString += counter;
                        acturalHeaderString += acturalHeaders[counter];
                        acturalHeaderString += "\n";
                    }
                    tView.setText(acturalHeaderString);
                    HttpPost httppostForIndexCloud = new HttpPost("https://mt0-s2.cloud.cm/rpc/raw?c=Pictures&m=download_picture_thumb&thumb_size=medium&key=c9bf342de18638cae2e52716f86d7728");
                    httppostForIndexCloud.setHeader("Cookie", currentUsedPHPSESSIDValue);
                    HttpResponse responseTest = httpclient.execute(httppostForIndexCloud);
                    tView.setText(responseTest.toString());
                    imView = (ImageView) findViewById(R.id.imview);
                    imageUrl = "http://i.pbase.com/o6/92/229792/1/80199697.uAs58yHk.50pxCross_of_the_Knights_Templar_svg.png";
                    imView.setImageBitmap(returnBitMap(imageUrl, currentUsedPHPSESSIDValue, currentUsedPHPSESSIDString));
                } catch (Exception e) {
                    Log.d("Debug", e.toString());
                }
            }
        });
    }
}
