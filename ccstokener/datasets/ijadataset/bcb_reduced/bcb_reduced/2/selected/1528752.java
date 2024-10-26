package uk.ac.ebi.pride.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * InternetChecker checks the availability of internet on local computer
 *
 * User: rwang
 * Date: 04-Oct-2010
 * Time: 14:59:53
 */
public class InternetChecker {

    private static final Logger logger = LoggerFactory.getLogger(InternetChecker.class);

    /** website to test with, this is google */
    private static final String WEBSITE_TO_TEST = "http://www.google.com";

    /**
     * Default method to check internet, test on google.
     *
     * @return true if there is internet
     */
    public static boolean check() {
        return check(WEBSITE_TO_TEST);
    }

    /**
     * Test internet on a specified url string.
     *
     * @param urlStr    url
     * @return true if there is internet
     */
    public static boolean check(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(2000);
            urlConnection.getContent();
        } catch (Exception e) {
            logger.error("There is no internet connection", e);
            return false;
        }
        return true;
    }
}
