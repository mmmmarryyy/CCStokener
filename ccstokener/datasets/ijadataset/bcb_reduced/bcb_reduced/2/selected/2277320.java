package dpdesktop;

import java.net.URL;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Providing some basic things needed to create a service module. 
 * @author Heiner Reinhardt
 */
public class DPDesktopDataService {

    /**
     * Specifies the encoding of xml data to be decoded and encoded. It is UTF-8 
     * by default and strictly recommended. 
     */
    protected static final String encoding = "UTF-8";

    /**
     * Will create an HttpURLConnection to the service url. A module name 
     * is required.
     * @param module Name of the module, to get data from. 
     * @return Object representing the current http connection to the 
     * service url.
     * @throws java.lang.Exception
     */
    protected static HttpURLConnection createURLConnection(String module) throws Exception {
        String url = DPDesktopDataLocal.getServiceURL();
        HttpURLConnection u = (HttpURLConnection) new URL(url + getAuthString() + "&module=" + module).openConnection();
        return u;
    }

    /**
     * Simple function that returns an string needed for authentication at the 
     * service. 
     * @return String like ?user=USERNAME&pass=PASSWORD
     * @throws java.io.UnsupportedEncodingException If encoding is not 
     * supported. As by default the encoding is UTF-8, like the standard 
     * encoding for java, UTF-8 should not lead to any 
     * UnsupportedEncodingException. 
     * @throws java.lang.Exception If password and username cannot be fetched, 
     * an Exception will be thrown.
     */
    private static String getAuthString() throws UnsupportedEncodingException, Exception {
        String myUser = URLEncoder.encode(DPDesktopDataLocal.getUsername(), encoding);
        String myPass = URLEncoder.encode(DPDesktopDataLocal.getPassword(), encoding);
        return "?user=" + myUser + "&pass=" + myPass;
    }
}
