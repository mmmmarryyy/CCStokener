package com.xmultra.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import com.xmultra.log.Console;
import com.xmultra.log.Logger;
import com.xmultra.log.MessageLogEntry;

/**
 * Utility for retrieving a Web page.
 *
 * @author      Bob Hucker
 * @version     $Revision: #1 $
 * @since       1.3
 */
public class HttpReader {

    private Logger logger = null;

    private MessageLogEntry msgEntry = null;

    public HttpReader(Logger log) {
        this.logger = log;
        msgEntry = new MessageLogEntry(this, VERSION);
    }

    /**
    * Updated automatically by source control management.
    */
    public static final String VERSION = "@version $Revision: #1 $";

    /**
     * Retrieve a Web page.
     *
     * @param urlString location of the Web page
     * @param maxTries maximum number of times to attempt to get the page
     * @param waitSeconds number of seconds to wait between tries
     *
     * @return String containing Web page contents
     */
    public String getPage(String urlString, int maxTries, int waitSeconds) {
        HttpURLConnection connection = getConnection(urlString, maxTries, waitSeconds);
        StringBuffer buffer = null;
        if (connection != null) {
            try {
                int bufferSize = connection.getContentLength();
                if (bufferSize == -1) {
                    bufferSize = 5120;
                }
                buffer = new StringBuffer(bufferSize);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                int character = 0;
                int END_OF_STREAM = -1;
                while (character != END_OF_STREAM) {
                    character = in.read();
                    buffer.append((char) character);
                }
                in.close();
            } catch (IOException ioe) {
                msgEntry = new MessageLogEntry(this, VERSION);
                msgEntry.setAppContext("getPage()");
                msgEntry.setMessageText("IOException getting " + urlString);
                msgEntry.setError(ioe.getMessage());
                logger.logWarning(msgEntry);
            } catch (Exception e) {
                msgEntry = new MessageLogEntry(this, VERSION);
                msgEntry.setAppContext("getPage()");
                msgEntry.setMessageText("Exception getting " + urlString);
                msgEntry.setError(e.getMessage());
                logger.logWarning(msgEntry);
            }
        }
        return buffer.toString();
    }

    /**
     * Get the size of an HTTP resource
     *
     * @param urlString location of the Web page
     * @param maxTries maximum number of times to attempt to get the page
     * @param waitSeconds number of seconds to wait between tries
     *
     * @return content length if known; -1 otherwise
     */
    public int getContentLength(String urlString, int maxTries, int waitSeconds) {
        HttpURLConnection connection = getConnection(urlString, maxTries, waitSeconds);
        int contentLength = -1;
        if (connection != null) {
            contentLength = connection.getContentLength();
        }
        return contentLength;
    }

    /**
     * Get the content type of an HTTP resource
     *
     * @param urlString location of the Web page
     * @param maxTries maximum number of times to attempt to get the page
     * @param waitSeconds number of seconds to wait between tries
     *
     * @return content type if known; null otherwise
     */
    public String getContentType(String urlString, int maxTries, int waitSeconds) {
        HttpURLConnection connection = getConnection(urlString, maxTries, waitSeconds);
        String contentType = null;
        if (connection != null) {
            contentType = connection.getContentType();
        }
        return contentType;
    }

    /**
     * Get the content type and length of an HTTP resource efficiently with one connection
     *
     * @param urlString location of the Web page
     * @param maxTries maximum number of times to attempt to get the page
     * @param waitSeconds number of seconds to wait between tries
     *
     * @return HashMap containing contentType and contentLength keys and corresponding values
     */
    public HashMap<String, String> getContentLengthAndType(String urlString, int maxTries, int waitSeconds) {
        HttpURLConnection connection = getConnection(urlString, maxTries, waitSeconds);
        HashMap<String, String> contentLengthAndType = new HashMap<String, String>();
        String contentType;
        if (connection != null) {
            int contentLength = -1;
            if (connection != null) {
                contentLength = connection.getContentLength();
            }
            contentLengthAndType.put("contentLength", contentLength >= 0 ? Integer.toString(contentLength) : null);
            contentType = connection.getContentType();
            contentLengthAndType.put("contentType", contentType);
        }
        return contentLengthAndType;
    }

    /**
     * Open an HTTP connection
     *
     * @param urlString location of the Web page
     * @param maxTries maximum number of times to attempt to get the page
     * @param waitSeconds number of seconds to wait between tries
     *
     * @return open connection
     */
    private HttpURLConnection getConnection(String urlString, int maxTries, int waitSeconds) {
        HttpURLConnection connection = null;
        try {
            for (int attempt = 1; attempt <= maxTries; attempt++) {
                URL url = new URL(urlString);
                try {
                    connection = (HttpURLConnection) url.openConnection();
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        break;
                    } else {
                        connection = null;
                    }
                } catch (IOException ioe) {
                    if (attempt == maxTries) {
                        msgEntry = new MessageLogEntry(this, VERSION);
                        msgEntry.setAppContext("getConnection()");
                        msgEntry.setMessageText("IOException getting " + urlString);
                        msgEntry.setError(ioe.getMessage());
                        logger.logWarning(msgEntry);
                        break;
                    }
                }
                try {
                    Thread.sleep(waitSeconds * 1000);
                } catch (InterruptedException ie) {
                }
            }
        } catch (MalformedURLException mue) {
            msgEntry.setAppContext("getConnection()");
            msgEntry.setMessageText("Bad URL: " + urlString);
            msgEntry.setError(mue.getMessage());
            logger.logWarning(msgEntry);
        }
        return connection;
    }
}
