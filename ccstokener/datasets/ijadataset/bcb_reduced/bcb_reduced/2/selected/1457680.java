package org.allesta.wsabi.webservice.axis.yahoouserping;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.axis.AxisFault;

/**
 * DOCUMENT ME!
 *
 * @author Allesta, LLC
 * @version $Revision: 1.4 $ 
 */
public class YahooUserPing {

    private static Log _log = LogFactory.getLog(YahooUserPing.class);

    /**
     * DOCUMENT ME!
     *
     * @param username DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean isUserOnLine(String username) throws AxisFault {
        if (_log.isDebugEnabled()) {
            _log.debug("isUserOnLine(): entered. username = " + username);
        }
        boolean isOnline = false;
        BufferedReader reader = null;
        long startTime = 0;
        try {
            if (_log.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
            }
            URL url = new URL("http://mail.opi.yahoo.com/online?u=" + username + "&m=t&t=0");
            URLConnection connection = url.openConnection();
            connection.connect();
            reader = getReader(connection);
            String result = reader.readLine();
            if (_log.isDebugEnabled()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                _log.debug("isUserOnLine(): time elapsed = " + elapsedTime);
            }
            if ((result != null) && (result.indexOf("NOT ONLINE") < 0)) {
                isOnline = true;
            }
        } catch (MalformedURLException e) {
            _log.error("isUserOnLine(): caught MalformedURLException: " + e.getMessage(), e);
            throw new AxisFault("Service Exception, please try again");
        } catch (IOException e) {
            _log.error("isUserOnLine(): caught IOException: " + e.getMessage(), e);
            throw new AxisFault("Service Exception, please try again");
        } finally {
            close(reader);
            if (_log.isDebugEnabled()) {
                _log.debug("isUserOnLine(): exiting with return value of " + isOnline + " for user " + username);
            }
        }
        return isOnline;
    }

    /**
     * DOCUMENT ME!
     *
     * @param args DOCUMENT ME!
     */
    public static void main(String[] args) throws Exception {
        YahooUserPing ping = new YahooUserPing();
        System.out.println("result = " + ping.isUserOnLine(""));
    }

    /**
     * DOCUMENT ME!
     *
     * @param connection DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private BufferedReader getReader(URLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader breader = new BufferedReader(reader);
        return breader;
    }

    /**
     * DOCUMENT ME!
     *
     * @param reader DOCUMENT ME!
     */
    private void close(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                _log.warn("close(): unable to close reader");
            }
        }
    }
}
