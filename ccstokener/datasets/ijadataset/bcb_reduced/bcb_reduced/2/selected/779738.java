package org.apache.myfaces.trinidadinternal.share.io;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.myfaces.trinidadinternal.util.URLUtils;

/**
 * An InputStreamProvider for opening URLs.
 * <p>
 * @version $Name:  $ ($Revision: adfrt/faces/adf-faces-impl/src/main/java/oracle/adfinternal/view/faces/share/io/URLInputStreamProvider.java#0 $) $Date: 10-nov-2005.19:00:10 $
 */
public class URLInputStreamProvider implements InputStreamProvider {

    /**
   * Create an URLInputStreamProvider.
   */
    public URLInputStreamProvider(URL url) {
        if (url == null) throw new NullPointerException();
        _url = url;
    }

    public InputStream openInputStream() throws IOException {
        URLConnection connection = _url.openConnection();
        _lastModifiedTime = connection.getLastModified();
        InputStream base = connection.getInputStream();
        if (base instanceof BufferedInputStream) return base; else return new BufferedInputStream(base);
    }

    public String getDisplayName() {
        return _url.toExternalForm();
    }

    public Object getIdentifier() {
        return _url;
    }

    /**
   * Returns true if the underlying target has changed
   * since the last call to openInputStream()
   */
    public boolean hasSourceChanged() {
        try {
            long currentModifiedTime = URLUtils.getLastModified(_url);
            return currentModifiedTime != _lastModifiedTime;
        } catch (IOException ioe) {
            return false;
        }
    }

    public Object getCachedResult() {
        return _cached;
    }

    public void setCachedResult(Object value) {
        _cached = value;
    }

    private final URL _url;

    private Object _cached;

    private long _lastModifiedTime = -1;
}
