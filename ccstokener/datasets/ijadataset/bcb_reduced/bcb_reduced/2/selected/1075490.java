package yaddur.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 
 * @author Viktoras Agejevas
 * @version $Id: HttpConnection.java 10 2007-12-07 16:57:39Z inversion $
 */
public class HttpConnection {

    public HttpURLConnection getConnection(String uri) throws IOException {
        HttpURLConnection connection = null;
        uri = uri.replaceAll("^(https://|http://)", "");
        URL url = new URL("http://" + uri);
        connection = (HttpURLConnection) url.openConnection();
        return connection;
    }
}
