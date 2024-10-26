package cz.papezzde.talkingplaces.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Basic connector that connects to URL and provides the input stream to read from.  
 * @author Zdenek Papez
 *
 */
public class URLConnector {

    private InputStream inputStream;

    private URLConnection conn;

    private URL url;

    private boolean isOnline;

    /**
	 * Default construcor
	 */
    public URLConnector() {
    }

    /**
	 * Constructs the instance and connects to the specified link. 
	 * @param link link to connect to
	 */
    public URLConnector(String link) {
        this();
        connect(link);
    }

    /**
	 * Opens a connection to the specified link.
	 * @param link link to connect to
	 */
    public void connect(String link) {
        try {
            url = new URL(link);
            conn = url.openConnection();
            inputStream = conn.getInputStream();
            isOnline = true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            isOnline = false;
        }
    }

    /**
	 * Closes the current connection
	 */
    public void close() {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Getter for the input stream of the open connection
	 * @return input stream to the URL
	 */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
	 * Getter for the isOnline
	 * @return if connection successful
	 */
    public boolean isOnline() {
        return isOnline;
    }
}
