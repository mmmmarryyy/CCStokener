package net.wotonomy.foundation.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This implementation of URL Resource Reader assumes 2 types
 * of base urls. A base url that ends with / is considered a
 * resource folder, whereas a resource that does not end with
 * / is considered a zip/jar resource folder.
 *
 * If the resource folder happens is a zip/jar archive, the
 * entries are always cached.
 * For non-zip base urls, one could specify whether or not it should
 * be cached.
 *
 * @author Harish Prabandham
 */
public class URLResourceReader {

    private Hashtable resourceCache = new Hashtable();

    private boolean iszip = true;

    private URL url = null;

    private boolean cache = true;

    /**
     * Creates a new URLResourceReader object. You can either give
     * the URL of the zip/jar file or a base url where to
     * look for additional resources. If the url ends with
     * "/" then it is assumed to be  a Base URL. 
     * @param The base url to look for the resources.
     * @param If the base url is not a zip/jar, then true indicates
     * that entries should be cached, false otherwise.
     */
    public URLResourceReader(URL baseurl, boolean cache) throws IOException {
        this.url = baseurl;
        this.cache = cache;
        this.iszip = !url.getFile().endsWith("/");
        if (this.iszip) this.cache = true;
        initialize();
    }

    /**
     * equivalent to URLResourceReader(baseurl, false)
     */
    public URLResourceReader(URL baseurl) throws IOException {
        this(baseurl, false);
    }

    /**
     * Creates a new URLResourceReader object with the given
     * input stream. The stream is assumed to be a zip/jar
     * stream.
     */
    public URLResourceReader(InputStream is) throws IOException {
        init(is);
    }

    private void initialize() throws IOException {
        if (iszip) {
            InputStream is = url.openStream();
            init(is);
            is.close();
        }
    }

    private byte[] readFully(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        int num = 0;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while ((num = is.read(buf)) != -1) {
            bout.write(buf, 0, num);
        }
        return bout.toByteArray();
    }

    private void init(InputStream is) throws IOException {
        ZipInputStream zstream = new ZipInputStream(is);
        ZipEntry entry;
        while ((entry = zstream.getNextEntry()) != null) {
            byte[] entryData = readFully(zstream);
            if (cache) resourceCache.put(entry.getName(), entryData);
            zstream.closeEntry();
        }
        zstream.close();
    }

    /**
     * Returns an Enumeration of all "known" resource names.
     */
    public Enumeration getResourceNames() {
        return resourceCache.keys();
    }

    /**
     * Returns an array of bytes read for this resource if the
     * resource exists. This method blocks until the resource
     * has been fully read. If the resource does not exist,
     * this method returns null.
     */
    public byte[] getResource(String resource) {
        byte[] data = (byte[]) resourceCache.get(resource);
        if (data != null) {
            return data;
        }
        if (iszip) {
            return null;
        }
        try {
            URL realURL = new URL(url.getProtocol(), url.getHost(), url.getFile() + resource);
            data = readFully(realURL.openStream());
            if (cache) resourceCache.put(resource, data);
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        resourceCache.clear();
        resourceCache = null;
    }

    public String toString() {
        return url.toString();
    }
}
