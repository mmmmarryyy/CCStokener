package org.ujac.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Name: HttpResourceLoader<br>
 * Description: A resource loader which loads resources from network using the http protocol.
 * 
 * @author lauerc
 */
public class HttpResourceLoader implements ResourceLoader {

    /** The URL root. */
    private String urlRoot = null;

    /**
   * Constructs a HttpResourceLoader instance with no specific attributes.
   */
    public HttpResourceLoader() {
    }

    /**
   * Constructs a HttpResourceLoader instance with specific attributes.
   * @param urlRoot The root location.
   */
    public HttpResourceLoader(String urlRoot) {
        this.urlRoot = urlRoot;
    }

    /**
   * @see org.ujac.util.io.ResourceLoader#loadResource(java.lang.String)
   */
    public byte[] loadResource(String location) throws IOException {
        if ((location == null) || (location.length() == 0)) {
            throw new IOException("The given resource location must not be null and non empty.");
        }
        URL url = buildURL(location);
        URLConnection cxn = url.openConnection();
        InputStream is = null;
        try {
            byte[] byteBuffer = new byte[2048];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
            is = cxn.getInputStream();
            int bytesRead = 0;
            while ((bytesRead = is.read(byteBuffer, 0, 2048)) >= 0) {
                bos.write(byteBuffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
   * Checks whether or not the desired resource exists. 
   * @param location The location of the resource.
   * @return true in case the given resource could be located, else false.
   */
    public boolean resourceExists(String location) {
        if ((location == null) || (location.length() == 0)) {
            return false;
        }
        try {
            URL url = buildURL(location);
            URLConnection cxn = url.openConnection();
            InputStream is = null;
            try {
                byte[] byteBuffer = new byte[2048];
                is = cxn.getInputStream();
                while (is.read(byteBuffer, 0, 2048) >= 0) ;
                return true;
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    /**
   * Builds a URL for the given location.
   * @param location The location to convert into a URL.
   * @return The created URL.
   * @throws MalformedURLException If the url root or the location specifies an unknown protocol.
   */
    private URL buildURL(String location) throws MalformedURLException {
        URL url = null;
        if (urlRoot == null) {
            url = new URL(location);
        } else {
            int firstColonIdx = location.indexOf(':');
            int firstSlashIdx = location.indexOf('/');
            if ((firstColonIdx > 0) && ((firstSlashIdx < 0) || (firstColonIdx < firstSlashIdx))) {
                url = new URL(location);
            } else {
                if (urlRoot.endsWith("/")) {
                    url = new URL(urlRoot + location);
                } else {
                    url = new URL(urlRoot + "/" + location);
                }
            }
        }
        return url;
    }
}
