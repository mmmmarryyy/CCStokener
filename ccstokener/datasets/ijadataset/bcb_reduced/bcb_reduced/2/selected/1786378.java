package com.android.hello;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;
import de.fhkl.helloWorld.implementation.actions.*;
import de.fhkl.helloWorld.implementation.model.parser.AccountParser;
import de.fhkl.helloWorld.interfaces.model.account.Account;
import de.fhkl.helloWorld.interfaces.model.attribute.profile.SingleProfileAttribute;

public class HelloAndroid extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("start");
        setContentView(tv);
        AccountManager ac = new AccountManager();
        String text = "not parsed";
        Account a = null;
        URL url;
        try {
            tv.setText("try to download");
            HttpClient client = new DefaultHttpClient();
            HttpGet getMethod = new HttpGet("http://www.helloworld-network.org/markusAckermann/account.xml");
            tv.setText("start download");
            HttpResponse response = client.execute(getMethod);
            tv.setText("download executed");
            InputStream in = response.getEntity().getContent();
            tv.setText("opened");
            tv.setText("try to encrypt");
            a = ac.decryptAndParseAccount(in, "asdf");
            tv.setText("encryption done");
            if (a != null) tv.setText("encrypted, fn: " + a.getPrivateProfile().getHCard().getFn().getValue());
            tv.setText("encrypted, hcard: " + a.getPrivateProfile().getHCard().toString());
        } catch (MalformedURLException e) {
            tv.setText(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            tv.setText(e.toString());
            e.printStackTrace();
        }
    }
}
