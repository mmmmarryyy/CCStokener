package net.assimilator.examples.sca.web.tomcat.balancer.utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @version $Id: URLReader.java 140 2007-04-25 01:19:37Z khartig $
 */
public class URLReader {

    private static String strURL = "http://127.0.0.1:8081/clusterapp/test.jsp";

    private static Logger logger = Logger.getLogger("net.assimilator.examples.sca.web.tomcat.utilities");

    public URLReader() {
    }

    public boolean isServerAlive(String pStrURL) {
        boolean isAlive;
        long t1 = System.currentTimeMillis();
        try {
            URL url = new URL(pStrURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.fine(inputLine);
            }
            logger.info("**  Connection successful..  **");
            in.close();
            isAlive = true;
        } catch (Exception e) {
            logger.info("**  Connection failed..  **");
            isAlive = false;
        }
        long t2 = System.currentTimeMillis();
        logger.info("Time taken to check connection: " + (t2 - t1) + " ms.");
        return isAlive;
    }

    public static void main(String[] args) throws Exception {
        URLReader test = new URLReader();
        if (test.isServerAlive(strURL)) {
            logger.info("**  IT'S ALIVE  **");
        } else {
            logger.info("**  THE SERVER IS NOT AVAILABLE  **");
        }
    }
}
