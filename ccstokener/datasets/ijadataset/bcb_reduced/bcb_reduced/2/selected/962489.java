package org.poxd.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.log4j.Logger;
import org.poxd.model.Content;

/**
 * @author pollux
 * 
 */
public class URLContent implements Content {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(URLContent.class);

    private URL url;

    private String absolutepath;

    /**
	 * Should not be used by external code - here only for hibernate
	 */
    public URLContent() {
    }

    /**
	 * @param absolutepath
	 */
    public URLContent(String absolutepath) {
        this.absolutepath = absolutepath;
        url = Thread.currentThread().getContextClassLoader().getResource(absolutepath);
    }

    /**
	 * Returns the URL of this content.
	 */
    public URL getURL() {
        return this.url;
    }

    /**
	 * Returns an InputStream on the URL content.
	 * 
	 * @throws IOException
	 *             if URL stream can't be opened.
	 */
    public InputStream openStream() throws IOException {
        return this.url.openStream();
    }

    public String getAbsolutepath() {
        return absolutepath;
    }

    public void setAbsolutepath(String absolutepath) {
        this.absolutepath = absolutepath;
        try {
            log.debug(absolutepath);
            url = Thread.currentThread().getContextClassLoader().getResource(absolutepath);
        } catch (Exception e) {
            url = null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof URLContent) {
            URLContent ob = (URLContent) obj;
            return absolutepath.equals(ob.getAbsolutepath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        byte bytes[] = absolutepath.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            if (i % 2 == 0) {
                hash += bytes[i];
            }
            if (i % 3 == 0) {
                hash *= bytes[i];
            }
        }
        return hash;
    }
}
