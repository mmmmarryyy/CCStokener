package rjws.client;

import java.applet.Applet;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class AppletHTTPClient extends HTTPClientBase {

    public AppletHTTPClient(Applet context) throws MalformedURLException {
        String host = context.getDocumentBase().getHost();
        int port = context.getDocumentBase().getPort();
        baseUrl = new URL("http", host, port, "");
    }

    /**
	 * Requests Data
	 * @param doc
	 * @param getData
	 * @return
	 * @throws IOException
	 */
    public String get(String doc, Map<String, String> getData) throws IOException {
        String urlParams = doc + "?";
        for (Map.Entry<String, String> entry : getData.entrySet()) {
            urlParams += entry.getKey() + "=" + entry.getValue() + "&";
        }
        URL url = new URL(baseUrl, urlParams);
        URLConnection conn = url.openConnection();
        conn.connect();
        return getStringFromInputStream(conn.getInputStream());
    }

    /**
	 * TODO!
	 * Posts Data
	 * @param doc
	 * @param postData
	 * @return
	 * @throws IOException 
	 */
    public String post(String doc, Map<String, String> postData) throws IOException {
        return null;
    }

    private final URL baseUrl;
}
