package sfplayer.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michele Dell'Ambrogio, m@nextcode.ch
 */
public class HttpGrabber {

    private URL url = null;

    private String httpString = null;

    public HttpGrabber() {
    }

    public String getHttpString() {
        return httpString;
    }

    public void openConnection(String urlString) {
        try {
            url = new URL(urlString);
            URLConnection con = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) con;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println(this.getClass().getName() + ": HTTP Connection OK");
            }
            grabHttp();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void grabHttp() {
        InputStream is = null;
        try {
            is = url.openStream();
            httpString = (new Scanner(is).useDelimiter("\\Z").next());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
