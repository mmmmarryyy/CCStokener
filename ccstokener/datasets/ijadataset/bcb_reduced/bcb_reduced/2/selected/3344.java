package org.statefive.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.statefive.util.AbstractGatherer;

/**
 * Gathers a stream of data from a given URL.
 * 
 * @author rmeeking
 */
public class UrlStreamGatherer extends AbstractGatherer {

    /** The default amount of characters to read in. */
    public static final int DEFAULT_DATA_SIZE = 1024;

    /** How much data to read from the stream before notifying listeners. */
    protected int dataSize = DEFAULT_DATA_SIZE;

    /** The data buffer to fill up with new data. */
    protected char[] data;

    /** The stream that is being monitored. */
    protected InputStream is;

    /** The URL to gather data from. */
    protected URL url;

    /** If <tt>true</tt>, re-read the URLs input stream; else, continue
   reading from the last position in the stream. */
    private boolean reReadOnUpdate = true;

    /**
   * Creates a new URL stream gatherer with no underlying URL.
   */
    public UrlStreamGatherer() {
        super();
    }

    /**
   * Determines if this gatherer should re-read the stream every time an update
   * (via {@link #gather(java.lang.Object)}) happens.
   *
   * @return <tt>true</tt> if this gatherer re-reads the stream on an update;
   * <tt>false</tt> otherwise.
   */
    public boolean isReReadOnUpdate() {
        return reReadOnUpdate;
    }

    /**
   * Sets if this gatherer should re-read the input stream everytime
   * {@link #gather(java.lang.Object)} is called.
   *
   * @param reReadOnUpdate <tt>true</tt> if this gatherer should re-read
   * the stream on an update; <tt>false</tt> otherwise.
   */
    public void setReReadOnUpdate(boolean reReadOnUpdate) {
        this.reReadOnUpdate = reReadOnUpdate;
    }

    /**
   * Sets the URL that this gatherer will gather from.
   *
   * @param url the URL; must not be <tt>null</tt>.
   *
   * @throws NullPointerException if the URL is <tt>null</tt>.
   */
    public void setUrl(final URL url) throws IOException {
        this.url = url;
        this.is = url.openStream();
    }

    /**
   * Gets the URL for this gatherer, if there is one.
   *
   * @param url the URL, if it has been set; <tt>null</tt> otherwise.
   */
    public URL getUrl() throws IOException {
        return this.url;
    }

    /**
   * Sets the size of data to be read each read.
   * 
   * @param size
   *          how many bytes to read before returning
   */
    public void setDataSize(int size) {
        this.dataSize = size;
    }

    /**
   * Gets the size of data to be read (as in red) each read (as in reed).
   * 
   * @return how many bytes to read before returning
   */
    public int getDataSize() {
        return this.dataSize;
    }

    /**
   * Checks to see if there have been any changes to the stream. If any
   * change has occured the new data is read in and all
   * listeners are notified.
   * 
   * @throws IOException
   *           if there are any problems checking the stream.
   */
    @Override
    public void gather(Object arg) throws IOException {
        if (this.isReReadOnUpdate()) {
            is.close();
            is = null;
            is = url.openStream();
        }
        data = new char[getDataSize()];
        int read = -1;
        int currentPos = 0;
        while (is.available() > 0 && (read = is.read()) != -1) {
            char c = (char) read;
            if (currentPos == getDataSize()) {
                notifyListeners(new String(data, 0, currentPos));
                currentPos = 0;
                data = null;
                data = new char[getDataSize()];
                data[currentPos++] = c;
            } else {
                data[currentPos++] = c;
            }
        }
        if (data != null && data.length > 0) {
            notifyListeners(new String(data, 0, currentPos));
        }
    }
}
