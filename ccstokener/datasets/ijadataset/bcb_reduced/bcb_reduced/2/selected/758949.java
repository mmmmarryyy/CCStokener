package uk.co.zenly.jllama.httpLoad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import org.apache.log4j.Logger;
import uk.co.zenly.jllama.LocalProperties;

public class Request extends Thread {

    static Logger logger = Logger.getLogger(Replay.class.getName());

    protected static Properties props = new LocalProperties();

    static int activeThreads = 0;

    static int completedThreads = 0;

    static {
        logger.debug("Initialising request static");
        System.setProperty("sun.net.client.defaultReadTimeout", props.getProperty("page.timeout"));
        System.setProperty("sun.net.client.defaultConnectTimeout", props.getProperty("connection.timeout"));
    }

    URL url;

    public Request(URL url) {
        this.url = url;
    }

    public void makeRequest() {
        activeThreads++;
        int count = 0;
        HttpURLConnection con = null;
        try {
            logger.debug("Making a connection to " + url);
            con = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                count++;
            }
            logger.debug("Got " + count + " lines back from " + url.toString() + " Response code " + con.getResponseCode());
            if (con != null && con.getResponseCode() == 200 && count > 1) {
                completedThreads++;
            }
        } catch (IOException ex) {
            logger.debug(ex, ex);
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception ex) {
                logger.warn(ex, ex);
            }
            activeThreads--;
        }
    }

    /**
	 * Called by Request.start
	 */
    public void run() {
        makeRequest();
    }

    /**
	 * Returns the number of active threads
	 * @return
	 */
    public static int getActiveThreads() {
        return activeThreads;
    }

    public static int getCompletedThreads() {
        return completedThreads;
    }
}
