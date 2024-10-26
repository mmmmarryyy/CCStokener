package com.volantis.mcs.runtime;

import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.synergetics.log.LogDispatcher;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * Simple class that performs an asynchronous check for updates to the product.
 */
public class UpdateChecker implements Runnable {

    /**
     * Used for logging.
     */
    private static final LogDispatcher LOGGER = LocalizationFactory.createLogger(UpdateChecker.class);

    /**
     * The URL to check
     */
    private String checkUrl;

    /**
     * Hide the constructor
     *
     * @param checkUrl the url to connect to.
     */
    private UpdateChecker(String checkUrl) {
        this.checkUrl = checkUrl;
    }

    /**
     * Static helper method to check for updates
     *
     * @param checkUrl the url to connection to. This should have the version
     * string as the first path fragment available to
     * HttpServletRequest.getPathInfo()
     */
    public static void checkForUpdates(String checkUrl) {
        new Thread(new UpdateChecker(checkUrl)).start();
    }

    /**
     * Run asynch so as not to slow anything down while waiting for connection
     */
    public void run() {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking for updates at " + checkUrl);
            }
            URL url = new URL(checkUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer content = new StringBuffer();
                String s = reader.readLine();
                while (s != null) {
                    content.append(s);
                    s = reader.readLine();
                }
                LOGGER.info("update-available", content.toString());
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No update available (Response code " + connection.getResponseCode() + ")");
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Update check failed", e);
            }
        }
    }
}
