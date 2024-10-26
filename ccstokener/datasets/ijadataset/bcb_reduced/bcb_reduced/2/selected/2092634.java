package ch.jester.common.web;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * Hilfsklasse um einen simplen HTTP Proxy zu erzeugen.
 *
 */
public class HTTPFactory {

    private static Proxy mProxy;

    /**
	 * Erzeugen des Proxies
	 * @param pProxyAdress
	 * @param pProxyPort
	 */
    public static void createHTTPProxy(String pProxyAdress, int pProxyPort) {
        mProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(pProxyAdress, pProxyPort));
    }

    public static Proxy getHTTPProxy() {
        return mProxy;
    }

    /**
	 * Proxy löschen
	 */
    public static void reset() {
        mProxy = null;
    }

    /**
	 * zur Site connecten
	 * @param pUrl
	 * @param open öffnen oder nicht.
	 * @return
	 * @throws IOException
	 */
    public static HttpURLConnection connect(String pUrl, boolean open) throws IOException {
        URL url = new URL(pUrl);
        HttpURLConnection uc = null;
        if (HTTPFactory.getHTTPProxy() == null) {
            uc = (HttpURLConnection) url.openConnection();
        } else {
            uc = (HttpURLConnection) url.openConnection(HTTPFactory.getHTTPProxy());
        }
        if (open) {
            uc.connect();
        }
        return uc;
    }

    /**
	 * zur Site connecten
	 * @param pUrl
	 * @return
	 * @throws IOException
	 */
    public static HttpURLConnection connect(String pUrl) throws IOException {
        return connect(pUrl, true);
    }
}
