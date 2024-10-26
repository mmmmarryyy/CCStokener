package nhb.webflag.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 * a very simple http client
 *
 * @author hendrik
 */
public class HttpClient {

    private static Logger logger = Logger.getLogger(HttpClient.class);

    private String urlString;

    private HttpURLConnection connection;

    private InputStream is;

    private int timeout = 10000;

    /**
	 * Creates a HTTP-Client which will connect to the specified URL
	 *
	 * @param url URL to connect to
	 */
    public HttpClient(String url) {
        this.urlString = url;
    }

    /**
	 * sets the timeout
	 *
	 * @param timeout timeout
	 */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 * connects to the server and opens a stream.
	 */
    private void openInputStream() {
        try {
            URL url = new URL(urlString);
            try {
                HttpURLConnection.setFollowRedirects(true);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686 (x86_64); en-US; rv:1.7.10) Gecko/20061113 Firefox/1.0.4 (Debian package 1.0.4-2sarge13)");
                connection.setRequestProperty("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
                connection.setRequestProperty("Accept-Language", "en_us");
                connection.setConnectTimeout(timeout);
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    logger.warn("HttpServer returned an error code (" + urlString + "): " + connection.getResponseCode());
                    connection = null;
                }
                if (connection != null) {
                    is = connection.getInputStream();
                }
            } catch (SocketTimeoutException e) {
                logger.warn("Timeout (" + urlString + "): " + " " + e.toString());
            }
        } catch (Exception e) {
            logger.warn("Error connecting to http-Server (" + urlString + "): ", e);
        }
        return;
    }

    /**
	 * Return an InputStream to read the requested file from.
	 * You have to close it using @see close().
	 *
	 * @return InputStream or null on error.
	 */
    public InputStream getInputStream() {
        openInputStream();
        return is;
    }

    /**
	 * Closes the connection and associated streams.
	 */
    public void close() {
        try {
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            logger.error(e, e);
        }
        if (connection != null) {
            connection.disconnect();
        }
    }

    /**
	 * fetches the first line of a file using http and closes the
	 * connection automatically.
	 *
	 * @return the first line
	 */
    public String fetchFirstLine() {
        String line = null;
        try {
            openInputStream();
            if (is == null) {
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            line = br.readLine();
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            logger.error("Error connecting to http-Server: ", e);
        }
        return line;
    }

    /**
	 * Fetches a file from the HTTP-Server and stores it on disk
	 *
	 * @param filename name of the file to write
	 * @return true on success, false otherwise
	 */
    public boolean fetchFile(String filename) {
        boolean res = false;
        openInputStream();
        if (is == null) {
            return res;
        }
        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
            copyStream(is, os);
            connection.disconnect();
            res = true;
        } catch (Exception e) {
            res = false;
            logger.error(e, e);
        }
        return res;
    }

    /**
	 * Copies data from an inputStream to and outputStream and closes both
	 * steams after work
	 *
	 * @param inputStream  stream to read from
	 * @param outputStream stream to write to
	 * @throws IOException on an input/output error
	 */
    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[10240];
        int length = inputStream.read(buffer);
        int byteCounter = length;
        while (length > -1) {
            outputStream.write(buffer, 0, length);
            length = inputStream.read(buffer);
            if (length > 0) {
                byteCounter = byteCounter + length;
            }
        }
        inputStream.close();
        outputStream.close();
    }
}
