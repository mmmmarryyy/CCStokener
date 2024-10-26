package org.cloud.android;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;

public class WebTest extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final EditText eText = (EditText) findViewById(R.id.address);
        final Button button = (Button) findViewById(R.id.ButtonGo);
        button.setOnClickListener(new Button.OnClickListener() {

            public void onClick(View v) {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost("https://mt0-app.cloud.cm/rpc/json");
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair("m", "login"));
                    nameValuePairs.add(new BasicNameValuePair("c", "User"));
                    nameValuePairs.add(new BasicNameValuePair("password", "cloudisgreat"));
                    nameValuePairs.add(new BasicNameValuePair("alias", "cs588"));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    String result = "";
                    try {
                        HttpResponse response = httpclient.execute(httppost);
                        result = EntityUtils.toString(response.getEntity());
                    } catch (Exception e) {
                        result = e.getMessage();
                    }
                    LayoutInflater inflater = (LayoutInflater) WebTest.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    final View layout = inflater.inflate(R.layout.window1, null);
                    final PopupWindow popup = new PopupWindowTest(layout, 100, 100);
                    Button b = (Button) layout.findViewById(R.id.test_button);
                    b.setOnTouchListener(new OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            Log.d("Debug", "Button activate");
                            popup.dismiss();
                            return false;
                        }
                    });
                    popup.showAtLocation(layout, Gravity.CENTER, 0, 0);
                    View layout2 = inflater.inflate(R.layout.window1, null);
                    final PopupWindow popup2 = new PopupWindowTest(layout2, 100, 100);
                    TextView tview = (TextView) layout2.findViewById(R.id.pagetext);
                    tview.setText(result);
                    popup2.showAtLocation(layout, Gravity.CENTER, 50, -90);
                } catch (Exception e) {
                    Log.d("Debug", e.toString());
                }
            }
        });
    }
}
