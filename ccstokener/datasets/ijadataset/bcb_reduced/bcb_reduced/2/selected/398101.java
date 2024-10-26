package net.sourceforge.bing.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import net.sourceforge.bing.exception.BingException;
import net.sourceforge.bing.exception.ConnectionException;
import net.sourceforge.bing.model.request.BingRequest;
import net.sourceforge.bing.model.response.BingResponse;
import net.sourceforge.bing.util.ProxyWrapper;
import org.apache.log4j.Logger;

/**
 * @author Christian Ternes
 * <p>
 * This class queries the microsoft search engine bing and converts the results to
 * java objects.
 * <p>
 * Example usage:
 * <code><pre>
 	BingRequest request = new BingRequest(
				"YOUR_API_ID", "bing",
				BingRequest.SourceType.Web);
	BingQuery bingQuery = new BingQuery();
	BingResponse response = bingQuery.query(request);
 * </pre></code>
 * <p>
 * To make a successfull query you need a developer id from the bing site. 
 * 
 */
public class BingQuery {

    private static final Logger logger = Logger.getLogger(BingQuery.class);

    private boolean useProxy = false;

    private String proxyHost = "";

    private int proxyPort = 0;

    private ProxyType proxyType = null;

    public enum ProxyType {

        HTTP, SOCKS
    }

    /**
	 * Creates a new query by using direct connection. 
	 * This should be used normally.  
	 */
    public BingQuery() {
    }

    /** 
	 * Creates a new query by using a proxy.
	 * This should be used if you are behind a proxy.
	 * Please use the setProxy() method to set your proxy settings.
	 * 
	 * @param useProxy true if you want to connect via proxy
	 * 
	 */
    public BingQuery(boolean useProxy) {
        this.useProxy = useProxy;
    }

    /**
	 * Sets a proxy for the connection. 
	 * 
	 * @param type a ProxyType that represents the type of the proxy
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 * @see ProxyType
	 */
    public void setProxy(ProxyType type, String host, int port) {
        this.proxyType = type;
        this.proxyHost = host;
        this.proxyPort = port;
    }

    /**
	 * Queries the bing search engine with the request
	 * 
	 * @param request a BingRequest object that represents the query
	 * @return a BingResponse object that represents the response 
	 * @throws BingException if something went wrong during the request
	 * @see BingRequest
	 * @see BingResponse 
	 */
    public BingResponse query(BingRequest request) throws BingException {
        BingResponse response = queryBing(request);
        return response;
    }

    private BingResponse queryBing(BingRequest request) throws BingException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Searching through bing...");
            }
            String query = request.getQuery();
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("http://api.bing.net/json.aspx?" + "AppId=" + request.getAppId() + "&Query=" + query + "&Sources=" + request.getType().toString());
            URLConnection connection = null;
            if (useProxy) {
                if (proxyType == null) {
                    throw new BingException("Please set a proxy first before trying to connect through a proxy", new Throwable());
                }
                connection = ProxyWrapper.getURLConnection(url.toString(), proxyType.toString(), proxyHost, proxyPort);
            } else {
                connection = new URL(url.toString()).openConnection();
            }
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String response = builder.toString();
            ResponseParser parser = new ResponseParser();
            parser.getError(response);
            return parser.getResults(response);
        } catch (MalformedURLException e) {
            logger.error(e);
            throw new ConnectionException("Could not connect to host", e);
        } catch (IOException e) {
            logger.error(e);
            throw new ConnectionException("Could not connect to host", e);
        }
    }
}
