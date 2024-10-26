package partyplanner.app.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.util.Log;

public class URLShortener {

    public static String shortenURI(String toShorten) {
        String shortURI = "ERROR";
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://is.gd/api.php?longurl=" + toShorten);
        try {
            HttpResponse response = httpClient.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                response.getEntity().writeTo(ostream);
                Log.e("HTTP CLIENT", ostream.toString());
            } else {
                InputStream content = response.getEntity().getContent();
                shortURI = convertStreamToString(content);
                content.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shortURI;
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
