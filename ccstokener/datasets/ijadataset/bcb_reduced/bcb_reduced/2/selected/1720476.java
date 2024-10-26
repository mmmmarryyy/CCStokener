package rjws.client;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class HTTPClient extends HTTPClientBase {

    public HTTPClient(String host) {
        this(host, 80);
    }

    public HTTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private final int port;

    private final String host;

    @Override
    public String get(String doc, Map<String, String> getData) throws IOException {
        String urlParams = doc + "?";
        for (Map.Entry<String, String> entry : getData.entrySet()) {
            urlParams += entry.getKey() + "=" + entry.getValue() + "&";
        }
        URL url = new URL("http://" + host + ":" + port + "/" + urlParams);
        URLConnection conn = url.openConnection();
        conn.connect();
        return getStringFromInputStream(conn.getInputStream());
    }

    @Override
    public String post(String doc, Map<String, String> postData) throws IOException {
        return null;
    }
}
