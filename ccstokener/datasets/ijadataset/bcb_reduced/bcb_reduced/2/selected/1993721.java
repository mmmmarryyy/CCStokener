package jwu2.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import jwu2.log.Logger;
import jwu2.net.cache.SiteCache;

/**
 *
 * @author Rolf
 */
public class ConnectionFactory {

    /**
     * Creates an http connection to the given url, and returns a reader to the
     * data contained. This will try looking into the cache of sites first to try
     * and lower bandwidth usage.
     * @param url The url to open connection to
     * @return a reader to the data at url
     */
    public static BufferedReader getReaderFromURL(String url) throws SiteNotFoundException {
        BufferedReader reader = null;
        reader = createReader(url);
        return reader;
    }

    /**
     * This does the same as getReaderFromURL(String url) except it bypasses the
     * cache.
     * @param url The url to open connection to
     * @return a reader to the data
     */
    public static BufferedReader getReaderFromURLNoCache(String url) throws SiteNotFoundException {
        return createReaderConnection(url);
    }

    /**
     * This does the same as getReaderFromURL(String url) except it forces the
     * cache to be updated
     * @param url The url to open connection to
     * @return a reader to the data
     */
    public static BufferedReader getReaderFromURLForceCacheUpdate(String url) throws SiteNotFoundException {
        if (fetchConnectionToCache(url)) {
            return createReader(url);
        }
        return createReaderConnection(url);
    }

    private static BufferedReader createReader(String url) throws SiteNotFoundException {
        try {
            String cacheString = SiteCache.singleton().getSite(url);
            BufferedReader reader = new BufferedReader(new StringReader(cacheString));
            Logger.logln("Using cache..");
            return reader;
        } catch (Exception e) {
            Logger.logln("Cache to old, fetching...");
            if (fetchConnectionToCache(url)) {
                return createReader(url);
            }
            return null;
        }
    }

    private static boolean fetchConnectionToCache(String url) throws SiteNotFoundException {
        BufferedReader in = createReaderConnection(url);
        StringBuilder sb = new StringBuilder();
        try {
            String line = in.readLine();
            while (line != null) {
                sb.append(line);
                line = in.readLine();
            }
            SiteCache.singleton().putSite(url, sb.toString());
            return true;
        } catch (Exception ex) {
            Logger.logln("Exception reading site [" + url + "] - " + ex);
        }
        return false;
    }

    private static BufferedReader createReaderConnection(String urlString) throws SiteNotFoundException {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-agent", "Mozilla/4.5");
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Logger.logln("Response code for url [" + urlString + "] was " + conn.getResponseCode() + " [" + conn.getResponseMessage() + "]");
                throw new SiteNotFoundException(urlString);
            }
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException ex) {
            Logger.logln("" + ex);
        }
        return reader;
    }
}
