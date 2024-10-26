package osdep;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import osdep.proxy.ProxyAuthenticationHandler;

/**
 * Connects to an HTTP site, authenticating the user to the proxy if needed
 * @author SHZ Mar 6, 2008
 */
public class ConnectionHandler {

    /**
	 * Data to authenticate the user to the proxy
	 */
    private ProxyAuthenticationHandler authenticationHandler;

    /**
	 * @param authenticationHandler the object that applies the proxy authentication to the connection 
	 */
    public ConnectionHandler(ProxyAuthenticationHandler authenticationHandler) {
        this.authenticationHandler = authenticationHandler;
    }

    /**
	 * Authenticate the user to the proxy, connects to the http site, and
	 * calls a callback function to do somehing with the connection
	 * @param httpAddress
	 * @param toDo
	 * @throws E 
	 * @throws ConnectionException 
	 * @throws Exception
	 */
    public <E extends Exception> void doWithConnection(String httpAddress, ICallableWithParameter<Void, URLConnection, E> toDo) throws E, ConnectionException {
        URLConnection connection;
        try {
            URL url = new URL(httpAddress);
            connection = url.openConnection();
        } catch (MalformedURLException e) {
            throw new ConnectionException("Connecting to " + httpAddress + " got", e);
        } catch (IOException e) {
            throw new ConnectionException("Connecting to " + httpAddress + " got", e);
        }
        authenticationHandler.doWithProxyAuthentication(connection, toDo);
    }
}
