package org.hfbk.vis;

import java.net.URL;
import java.net.URLEncoder;

/**
 * a simple server logger, sends given messages to vis server.
 *  
 */
public class Logger {

    public static boolean enabled = true;

    public static void log(final String user, final String msg) {
        if (!Prefs.current.log || !enabled) return;
        Thread logger = new Thread() {

            public void run() {
                this.setPriority(Thread.MIN_PRIORITY);
                try {
                    URL url = new URL(Prefs.current.baseURL + "log.php?user=" + URLEncoder.encode(user, "utf8") + "&message=" + URLEncoder.encode(msg, "utf8"));
                    url.openStream().close();
                } catch (Exception e) {
                    System.out.println("Logger: " + e);
                }
            }
        };
        logger.start();
    }
}
