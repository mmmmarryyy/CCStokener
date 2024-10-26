package lokahi.agent.util;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * @author Stephen Toback
 * @version $Id: TMCAgentBrowser.java,v 1.1 2006/03/02 19:19:44 drtobes Exp $
 */
public class TMCAgentBrowser implements Callable<String> {

    static final Logger logger = Logger.getLogger(TMCAgentBrowser.class);

    private URL url;

    public TMCAgentBrowser(URL url) {
        this.url = url;
    }

    public static String get(String url) {
        String ret = "Error, malformed URL";
        try {
            ret = get(new URL(url));
        } catch (MalformedURLException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Exception: " + e.getMessage());
            }
        }
        return ret;
    }

    public static String get(URL url) {
        StringBuilder ret = new StringBuilder();
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            int i = 0;
            while (i != -1) {
                i = in.read();
                if (i != -1) {
                    char c = (char) i;
                    ret.append(c);
                }
            }
        } catch (IOException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Exception: " + e.getMessage());
            }
            ret.append("Error connecting to the URL");
        }
        return ret.toString();
    }

    public String call() throws Exception {
        return get(url);
    }
}
