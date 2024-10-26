package net.walkingtools.android;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import android.app.Activity;
import android.util.Log;

/**
 * URLFetch asynchronously retrieves data from a url (String). It starts when the constructor is called,
 * and calls back to either dataReturned with the content of the transaction, or to failed with the Throwable
 * that caused the failure. Both the constructor and methods pass a unique code value so that many 
 * instances can be spawned and the specific request calling back can be identified.
 * @author Brett Stalbaum
 * @version 0.1.1
 * @since 0.1.1
 *
 */
public class URLFetch implements Runnable {

    private Thread t = null;

    private URL url = null;

    private byte[] data = null;

    private URLResponseReceiver callback = null;

    private int uniqueCode;

    /**
	 * 
	 * @param url the url to retrieve
	 * @param callback a URLResponseReceiver that will get the callback
	 * @param uniqueCode a unique code that disambiguates this URLFecth from others
	 * @throws MalformedURLException
	 */
    public URLFetch(String url, URLResponseReceiver callback, int uniqueCode) throws MalformedURLException {
        this.url = new URL(url);
        this.callback = callback;
        this.uniqueCode = uniqueCode;
        t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        URLConnection urlConnection;
        Vector<Byte> buildit = new Vector<Byte>();
        try {
            urlConnection = url.openConnection();
            BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
            byte temp = 0;
            while ((temp = (byte) in.read()) != -1) {
                buildit.add(temp);
            }
        } catch (IOException e) {
            callback.failed(e, url.toExternalForm(), uniqueCode);
        }
        data = new byte[buildit.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = buildit.elementAt(i);
        }
        String serverMessage = new String(data);
        callback.dataReturned(data, url.toExternalForm(), uniqueCode);
    }
}
