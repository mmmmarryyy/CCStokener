package org.statefive.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Class to monitor a given URL and notify any observers if the URLs timestamp
 * is changed.
 * 
 * @author rmeeking
 */
public class UrlMonitor extends AbstractThreadedMonitor {

    /** The time the file was last modified. */
    protected long editTime;

    /** The file that we're monitoring. */
    protected URL url;

    /**
   * Creates a new file monitor, with no file to watch.
   */
    public UrlMonitor() {
    }

    /**
   * Checks to see if the monitored file has changed.
   */
    @Override
    public void checkData() {
        try {
            URLConnection conn = url.openConnection();
            if (editTime < conn.getLastModified()) {
                editTime = conn.getLastModified();
                currentSize = conn.getContentLength();
                System.out.println(currentSize);
                notifyListeners(this);
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    /**
   * 
   * For debugging/reporting.
   * 
   * @return debug information.
   */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("URL: " + url);
        buffer.append("; ");
        buffer.append(super.toString());
        return buffer.toString();
    }

    /**
   * Gets the URL that this monitor is checking.
   * 
   * @return the URL being monitored.
   */
    public URL getUrl() {
        return url;
    }

    /**
   * Sets the URL for this monitor.
   *
   * @param url the URL; may be <code>null</code>.
   */
    public void setUrl(final URL url) {
        this.url = url;
        try {
            URLConnection conn = url.openConnection();
            editTime = conn.getLastModified();
        } catch (final IOException ioex) {
            ioex.printStackTrace();
        }
    }
}
