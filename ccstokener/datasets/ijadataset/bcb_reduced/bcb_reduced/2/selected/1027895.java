package com.sleepsocial.client.SleepSocial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {

    private Facebook facebook = new Facebook("192876737424509");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (!facebook.isSessionValid()) {
            facebook.authorize(this, new String[] { "email" }, new DialogListener() {

                @Override
                public void onComplete(Bundle values) {
                    ((TextView) findViewById(R.id.txt)).setText(facebook.getAccessToken());
                    try {
                        sendData(values);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFacebookError(FacebookError error) {
                }

                @Override
                public void onError(DialogError e) {
                }

                @Override
                public void onCancel() {
                }
            });
        }
    }

    private void sendData(Bundle values) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://sleepsocial.appspot.com/connections/facebook/connect.htm");
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(Facebook.TOKEN, values.getString(Facebook.TOKEN)));
            nameValuePairs.add(new BasicNameValuePair("expires", values.getString(Facebook.EXPIRES)));
            nameValuePairs.add(new BasicNameValuePair("secret", "secret"));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            byte[] data = new byte[1024];
            response.getEntity().getContent().read(data);
            ((TextView) findViewById(R.id.txt)).setText(new String(data));
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }
    }
}
