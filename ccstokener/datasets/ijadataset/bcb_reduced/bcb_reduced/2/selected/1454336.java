package Clients;

import java.io.*;
import java.net.*;
import java.lang.Thread;
import java.net.URL;
import Clients.*;
import ProtoSimulator.*;
import org.apache.log4j.Logger;

/**
 *
 * @author  jbrzoz01
 */
public class Http extends Client {

    private static final String CLASS_NAME = Http.class.getName();

    private static int maxRequests = 0;

    private static String requestedUrl = null;

    private static URL url = null;

    private Status status;

    private Notification notification;

    private static String httpResponse = null;

    private static long requestedElapsedTime = 0;

    private static long iterationElapsedTime = 0;

    private static boolean complete = false;

    public static Logger logger = Logger.getLogger(CLASS_NAME);

    /** Creates a new instance of Http */
    public Http(String url, int requests) {
        requestedUrl = url;
        maxRequests = requests;
    }

    public Http(Status s, String url, int requests, Notification n) {
        status = s;
        requestedUrl = url;
        maxRequests = requests;
        notification = n;
    }

    public void run() {
        int requestCount = 0;
        long i0 = System.currentTimeMillis();
        while (requestCount != maxRequests) {
            long r0 = System.currentTimeMillis();
            try {
                url = new URL(requestedUrl);
                logger.debug("Requesting Url, " + url.toString());
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                while ((httpResponse = in.readLine()) != null) {
                    logger.trace("Http Response = " + httpResponse);
                }
            } catch (Exception e) {
                logger.fatal("Exception thrown retrievng Url = " + requestedUrl + ", " + e);
                notification.setNotification(e.toString());
            }
            long r1 = System.currentTimeMillis();
            requestedElapsedTime = r1 - r0;
            logger.debug("Request(" + this.getName() + "/" + this.getId() + ") #" + requestCount + " processed, took " + requestedElapsedTime + "ms");
            requestCount++;
        }
        long i1 = System.currentTimeMillis();
        iterationElapsedTime = i1 - i0;
        logger.trace("Iteration elapsed time is " + iterationElapsedTime + "ms for thread ID " + this.getId());
        status.incrementIterationsComplete();
        logger.info("Iteration for Url = " + requestedUrl + ", (" + this.getName() + "/" + this.getId() + ") took " + iterationElapsedTime + "ms");
        try {
            logger.debug("Joining thread(" + this.getId() + ")");
            this.join(100);
        } catch (Exception e) {
            logger.fatal(e);
            notification.setNotification(e.toString());
        }
    }
}
