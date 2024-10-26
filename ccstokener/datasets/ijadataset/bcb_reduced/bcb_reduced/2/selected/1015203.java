package com.hp.hpl.jena.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import org.apache.commons.logging.*;

/** Location files named by a URL
 * 
 * @author Andy Seaborne
 * @version $Id: LocatorURL.java,v 1.8 2006/03/22 13:52:49 andy_seaborne Exp $
 */
public class LocatorURL implements Locator {

    static Log log = LogFactory.getLog(LocatorURL.class);

    static final String acceptHeader = "application/rdf+xml,application/xml;q=0.9,*/*;q=0.5";

    public InputStream open(String filenameOrURI) {
        if (!hasScheme(filenameOrURI, "http:")) {
            if (FileManager.logAllLookups && log.isTraceEnabled()) log.trace("Not found: " + filenameOrURI);
            return null;
        }
        try {
            URL url = new URL(filenameOrURI);
            URLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", acceptHeader);
            conn.setRequestProperty("Accept-Charset", "utf-8,*");
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.connect();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            if (in == null) {
                if (FileManager.logAllLookups && log.isTraceEnabled()) log.trace("Not found: " + filenameOrURI);
                return null;
            }
            if (FileManager.logAllLookups && log.isTraceEnabled()) log.trace("Found: " + filenameOrURI);
            return in;
        } catch (java.io.FileNotFoundException ex) {
            if (FileManager.logAllLookups && log.isTraceEnabled()) log.trace("LocatorURL: not found: " + filenameOrURI);
            return null;
        } catch (MalformedURLException ex) {
            log.warn("Malformed URL: " + filenameOrURI);
            return null;
        } catch (IOException ex) {
            if (ex instanceof ConnectException) {
                if (FileManager.logAllLookups && log.isTraceEnabled()) log.trace("LocatorURL: not found: " + filenameOrURI);
            } else log.warn("IO Exception opening URL: " + filenameOrURI + "  " + ex.getMessage());
            return null;
        }
    }

    public String getName() {
        return "LocatorURL";
    }

    private boolean hasScheme(String uri, String scheme) {
        String actualScheme = getScheme(uri);
        if (actualScheme == null) return false;
        return actualScheme.equalsIgnoreCase(scheme);
    }

    private String getScheme(String uri) {
        int ch = uri.indexOf(':');
        if (ch < 0) return null;
        return uri.substring(0, ch + 1);
    }
}
