package org.eclipse.jface.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;

/**
 * An ImageDescriptor that gets its information from a URL.
 * This class is not public API.  Use ImageDescriptor#createFromURL
 * to create a descriptor that uses a URL.
 */
class URLImageDescriptor extends ImageDescriptor {

    private URL url;

    /**
     * Creates a new URLImageDescriptor.
     * @param url The URL to load the image from.  Must be non-null.
     */
    URLImageDescriptor(URL url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (!(o instanceof URLImageDescriptor)) {
            return false;
        }
        return ((URLImageDescriptor) o).url.equals(this.url);
    }

    public ImageData getImageData() {
        ImageData result = null;
        InputStream in = getStream();
        if (in != null) {
            try {
                result = new ImageData(in);
            } catch (SWTException e) {
                if (e.code != SWT.ERROR_INVALID_IMAGE) throw e;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }

    /**
     * Returns a stream on the image contents.  Returns
     * null if a stream could not be opened.
     * @return the stream for loading the data
     */
    protected InputStream getStream() {
        try {
            return new BufferedInputStream(url.openStream());
        } catch (IOException e) {
            return null;
        }
    }

    public int hashCode() {
        return url.hashCode();
    }

    /**
     * The <code>URLImageDescriptor</code> implementation of this <code>Object</code> method 
     * returns a string representation of this object which is suitable only for debugging.
     */
    public String toString() {
        return "URLImageDescriptor(" + url + ")";
    }
}
