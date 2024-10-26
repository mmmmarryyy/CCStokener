package org.oclc.da.ndiipp.spider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;

/**
 * URLChecker
 *
 * This class handles checking a URL for validity. 
 * 
 * @author JCG
 * @version 1.0, 
 * @created 11/11/2004
 */
public class URLChecker {

    /**
     * Construct an instance of a URL checker.
     */
    public URLChecker() {
    }

    /**
     * Check a URL in string format.
     * <p>
     * @param url   A url in string format.
     * @return  One of the status codes defined in <code>Status</code>.
     *          e.g. <code>Status.INVALID_URL</code>
     */
    public int checkURL(String url) {
        try {
            return checkURL(new URL(url));
        } catch (MalformedURLException e) {
            return SpiderStatus.INVALID_URL;
        }
    }

    /**
     * Check a URL in string format.
     * <p>
     * @param url   A URL object.
     * @return  One of the status codes defined in <code>Status</code>. 
     *          e.g. <code>Status.INVALID_URL</code>
     */
    public int checkURL(URL url) {
        if (url.getProtocol().equalsIgnoreCase("http")) {
            return checkHTTP(url);
        } else {
            return SpiderStatus.UNSUPPORTED_PROTOCOL;
        }
    }

    /**
     * Verify that the following http request is valid.
     * <p>
     * @param url   The URL to verify via a header request.
     * @return  <code>Status.SUCCESS</code> on success.
     *          <code>Status.INACCESSIBLE</code> otherwise.
     */
    private int checkHTTP(URL url) {
        try {
            URLConnection connect = url.openConnection();
            String response = connect.getHeaderField(0);
            if (response != null) {
                StringTokenizer tokens = new StringTokenizer(response);
                if (tokens.countTokens() >= 2) {
                    tokens.nextToken();
                    String codeStr = tokens.nextToken();
                    try {
                        int code = Integer.parseInt(codeStr);
                        if ((code / 100) == 2) {
                            return SpiderStatus.SUCCESS;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        } catch (IOException e) {
        }
        return SpiderStatus.INACCESSIBLE;
    }
}
