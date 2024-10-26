package logic;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Makes connection and takes headers and responses from server.
 * @author Bartek Kowalik
 *
 */
public class Connector {

    /**
	 * Automatically connects to server and gets it's responses
	 * 
	 * @param url - String URL to site like http://www.mySite.myDomain
	 * @throws IOException Only one exception to set it easier
	 */
    public Connector(String url) throws IOException {
        this.url = new URL(url);
        this.connection = this.url.openConnection();
        this.responseMap = this.connection.getHeaderFields();
    }

    /**
	 * Return server's
	 * @return responseMap - server's responses 
	 */
    public Map getResponseMap() {
        return this.responseMap;
    }

    private URL url = null;

    private URLConnection connection = null;

    /**
	 * Takes all returned responses from server
	 */
    private Map responseMap;
}
