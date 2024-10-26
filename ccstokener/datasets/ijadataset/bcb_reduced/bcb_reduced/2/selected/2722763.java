package jstockquote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Roch Delsalle <rdelsalle@gmail.com>
 */
public class Get {

    private String str;

    public String url(String url) throws IOException {
        System.getProperties().put("http.proxyHost", "wwwcache.aber.ac.uk");
        System.getProperties().put("http.proxyPort", "8080");
        System.getProperties().put("http.proxyUser", "");
        System.getProperties().put("http.proxyPassword", "");
        System.getProperties().put("http.proxySet", "true");
        try {
            URL url_Stream = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(url_Stream.openStream()));
            str = in.readLine();
            in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return str;
    }
}
