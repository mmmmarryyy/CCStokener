package ssgpp.conn;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import ssgpp.sozluk.EksiSozlukUtilities;

/**
 *
 * @author Onur Derin <oderin at users.sourceforge.net>
 */
public class ConnectionFactory {

    public static URLConnection createURLConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        HttpURLConnection httpconn = (HttpURLConnection) connection;
        httpconn.setInstanceFollowRedirects(true);
        if (EksiSozlukUtilities.applicationProps.getProperty("User-Agent") != null) connection.setRequestProperty("User-Agent", EksiSozlukUtilities.applicationProps.getProperty("User-Agent")); else connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.8.0.6) Gecko/20060818 Firefox/1.5.0.6");
        return connection;
    }
}
