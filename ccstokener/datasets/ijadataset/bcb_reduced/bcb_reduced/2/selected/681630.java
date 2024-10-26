package net.sf.ennea.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:nkzzg@163.com">Zhiguo Zhao</a>
 * @version 1.0
 */
public abstract class AbstractClient implements Client {

    private static final Log log = LogFactory.getLog(AbstractClient.class);

    /**
	 * the remote url
	 */
    protected String endPoint = null;

    /**
	 * remote call type
	 */
    protected String transferType = null;

    /**
	 * need concrete class implement
	 * 
	 * @param request		eg. com.packageName.className.methodName
	 * @param parameters	the parameters of the method
	 */
    public abstract Object invoke(String request, Object parameters[]) throws Throwable;

    /**
	 * initialize a connection
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 */
    private HttpURLConnection initConnection() throws MalformedURLException, IOException, ProtocolException {
        if (log.isDebugEnabled()) {
            log.debug("Initialize the HttpURLConnection Object");
        }
        URL url = new URL(endPoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setAllowUserInteraction(true);
        connection.setDefaultUseCaches(false);
        return connection;
    }

    /**
	 * @param requestId		eg. com.packageName.className.methodName
	 * @return 				can Ennea used HttpURLConnection
	 * @throws 				IOException
	 */
    protected HttpURLConnection getConnection(String requestId) throws IOException {
        HttpURLConnection connection = initConnection();
        connection.setRequestProperty("REMOTE-TYPE", transferType);
        connection.setRequestProperty("REMOTE-REQUEST-ID", requestId);
        return connection;
    }

    /**
	 * @param requestId		the class name
	 * @return 				can Snack used HttpURLConnection
	 * @throws 				IOException	
	 */
    protected HttpURLConnection getSnackConnection(String requestId) throws IOException {
        HttpURLConnection connection = initConnection();
        connection.setRequestProperty("X-SNACK-REQUEST-TYPE", transferType);
        connection.setRequestProperty("X-SNACK-RESPONSE-TYPE", new StringBuffer(transferType).append(":JAVA").toString());
        connection.setRequestProperty("X-SNACK-REQUEST-ID", requestId);
        return connection;
    }
}
