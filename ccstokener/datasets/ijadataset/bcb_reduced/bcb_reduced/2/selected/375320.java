package org.windu2b.jcaddie.tools;

import java.io.*;
import java.net.*;
import org.windu2b.jcaddie.model.*;

/**
 * URL content for files, images...
 * 
 * @author Emmanuel Puybaret, windu.2b
 * 
 */
public class URLContent implements Content {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7866488183277459140L;

    private URL url;

    /**
	 * Constructeur par recopie
	 * 
	 * @param url
	 */
    public URLContent(URL url) {
        try {
            this.url = new URL(url.toString());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    public URLContent(Content content) {
        try {
            this.url = new URL(content.toString());
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Returns the URL of this content.
	 */
    public URL getUrl() {
        return url;
    }

    /**
	 * Returns an InputStream on the URL content.
	 * 
	 * @throws IOException
	 *             if URL stream can't be opened.
	 */
    public InputStream openStream() throws IOException {
        return url.openStream();
    }

    @Override
    public String toString() {
        return url.toString();
    }
}
