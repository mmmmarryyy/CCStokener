package jd.client.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import jd.client.util.IOUtil;

/**
 * @author Denis Migol
 * 
 */
public class DefaultJDClient extends JDClientBase {

    public DefaultJDClient() {
        super();
    }

    public DefaultJDClient(final int port) {
        super(port);
    }

    public DefaultJDClient(final String host, final int port) {
        super(host, port);
    }

    public DefaultJDClient(final String host) {
        super(host);
    }

    @Override
    protected String requestToString(final String url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            return IOUtil.toString(connection.getInputStream());
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
    }
}
