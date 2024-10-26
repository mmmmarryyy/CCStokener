package org.apache.jetspeed.cache.disk;

import org.apache.jetspeed.util.URIEncoder;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.urlmanager.URLFetcher;
import org.apache.jetspeed.services.resources.JetspeedResources;
import java.io.*;
import java.net.*;

/**
 *<p>A cache entry represents a data source that can be stored locally for
 *efficiency.
 *
 *<p>It can deliver a string with its contents, but the preferred 
 *way to access to the entry contents is through a Reader 
 *that will get characters from it.
 *
 *<p>There are two kinds of entries:
 *
 *<ul>
 *  <li>Local: It is not cached.
 *  <li>Remote: It can be cached.
 *</ul>
 *
 *<p>Remote entries can be in the following states:
 *
 *<ul>
 *  <li>Invalid: It has no local reference, and the source is transiently or
 *        permanently delivering errors
 *  <li>Stale: It has no local reference, and it is quiet
 *  <li>Loading: It has no local ref yet, and it is loading
 *  <li>Refreshing: It has a local ref (current or expired),
 *         and it is refreshing it
 *  <li>Expired: It has a local ref, but its content is no longer valid
 *  <li>Current: It has a valid local ref
 *</ul>
 *
 *<p>TODO: Some data sources need to be written (i. e., are writable). For those,
 * a mechanism need to be provided to write back the resource. We currently think
 * about HTTP PUT as a mechanism.
 *
 *@author <a href="mailto:burton@apache.org">Kevin A. Burton</a>
 *@author <a href="mailto:sgala@hisitech.com">Santiago Gala</a>
 *@version $Id: JetspeedDiskCacheEntry.java,v 1.33 2004/02/23 02:45:29 jford Exp $
 **/
public class JetspeedDiskCacheEntry implements DiskCacheEntry {

    /**
     * <p>Expiration interval that will be used it the remote URL does not
     * specify one. The contract here is:
     * <ul>
     * <li>If we have no hits, we will hit our entry every time DiskCacheDaemon is run to revalidate.</li>
     * <li>No matter how many hits we get, we will reach our entry at most once per defaultExpirationInterval.</li>
     * </ul>
     */
    private static long defaultExpirationInterval = 1000 * JetspeedResources.getInt(JetspeedResources.DEFAULT_DCE_EXPIRATION_TIME_KEY, 15 * 60);

    static String encoding = JetspeedResources.getString(JetspeedResources.CONTENT_ENCODING_KEY, "iso-8859-1");

    private File file = null;

    private String url = null;

    private String sourceURL = null;

    /**
     Date (ms since epoch) it was last Modified
     */
    private long lastModified = 0;

    /**
     Date (ms since epoch) it expires
     */
    private long expires = 0;

    /**
     * Static initialization of the logger for this class
     */
    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(JetspeedDiskCacheEntry.class.getName());

    /**
     *<p>Build a DiskCacheEntry that is based on a cached filesystem entry
     *
     *<p>This is used to reconstruct the entries from the cache at boot time
     *
     *
     */
    protected JetspeedDiskCacheEntry(File file) {
        this.setFile(file);
        this.setURL(this.getSourceURL());
    }

    /**
    *Build a DiskCacheEntry that is based on a remote URL and may be (or not)
    *backed on disk.
    *
    */
    public JetspeedDiskCacheEntry(String url) {
        this.setURL(url);
        this.init();
    }

    /**
     * Initialize the file variable, if it is null & the url
     * is not local
     *
    */
    public void init() {
        URL url = null;
        try {
            url = new URL(this.getURL());
        } catch (MalformedURLException e) {
            logger.error("Error in URL", e);
            return;
        }
        if ((this.file == null || this.file.exists() == false) && "file".equals(url.getProtocol())) {
            try {
                File newfile = new File(url.getFile());
                this.setFile(newfile);
                if (newfile.exists() == false) {
                    JetspeedDiskCache.getInstance().add(this.getURL(), true);
                }
            } catch (IOException e) {
                logger.error("Error building from file", e);
                return;
            }
        }
    }

    /**
    */
    public String getURL() {
        return this.url;
    }

    /**
    Reconstruct the original URL based on this URL.
    */
    public String getSourceURL() {
        if (this.getFile() == null) {
            return this.getURL();
        } else {
            return URIEncoder.decode(this.getFile().getName());
        }
    }

    /**
    Get the File that this URL obtains within the cache.
    */
    public File getFile() {
        return this.file;
    }

    /**
    Set the file.
    */
    public void setFile(File file) {
        if (file.exists() == false) {
            String message = "The following file does not exist: " + file.getAbsolutePath();
            logger.error(message);
            try {
                JetspeedDiskCache.getInstance().add(this.url, true);
            } catch (Throwable e) {
                logger.error("Error setting file", e);
            }
        }
        this.file = file;
        this.lastModified = file.lastModified();
        this.expires = System.currentTimeMillis() + defaultExpirationInterval;
    }

    /**
    Open this URL and read its data, then return it as a string
    */
    public String getData() throws IOException {
        Reader is = this.getReader();
        StringWriter bos = new StringWriter();
        char chars[] = new char[200];
        int readCount = 0;
        while ((readCount = is.read(chars)) > 0) {
            bos.write(chars, 0, readCount);
        }
        is.close();
        return bos.toString();
    }

    /**
    Get an input stream  from this entry 
    */
    public InputStream getInputStream() throws IOException {
        logger.info("CacheEntry getInputStream() called: " + this.getURL());
        if (this.getFile() != null) {
            return new FileInputStream(this.getFile());
        }
        if (DiskCacheUtils.isLocal(this.getURL())) {
            return new URL(this.getURL()).openConnection().getInputStream();
        }
        this.lastModified = 0;
        this.expires = 0;
        URLFetcher.refresh(this.getURL());
        if (this.getFile() != null) return new FileInputStream(this.getFile());
        throw new IOException(this.getURL() + ": is not in cache after forcing");
    }

    /**
    Get a Reader  from this entry.
        ( Patch for handling character encoding sent by 
          Yoshihiro KANNA  <y-kanna@bl.jp.nec.com> )
      For local entries, we assume that the URL coming
       from the WEB server is allright WRT encoding
    For remote entries, we assume that the cache saved them in the local store
        using UTF8 encoding
    */
    public Reader getReader() throws IOException {
        if (DiskCacheUtils.isLocal(this.getURL())) {
            URLConnection conn = new URL(this.getURL()).openConnection();
            String encoding = conn.getContentEncoding();
            if (encoding == null) {
                encoding = JetspeedResources.getString(JetspeedResources.CONTENT_ENCODING_KEY, "iso-8859-1");
            }
            return new InputStreamReader(conn.getInputStream(), encoding);
        }
        if (this.getFile() != null) {
            InputStreamReader reader = null;
            try {
                reader = new InputStreamReader(new FileInputStream(this.getFile()), "UTF8");
            } catch (UnsupportedEncodingException e) {
                logger.error("Encoding error", e);
                reader = new FileReader(this.getFile());
            }
            return reader;
        }
        this.lastModified = 0;
        this.expires = 0;
        URLFetcher.refresh(this.getURL());
        if (this.getFile() != null) return this.getReader();
        throw new IOException(this.getURL() + ": is not in cache after forcing");
    }

    /**
    Get a Writer  to update this entry.
      For local entries, we assume that the URL coming
       from the WEB server allows PUT
    For remote entries, we throws a IOException

    */
    public Writer getWriter() throws IOException {
        if (DiskCacheUtils.isRemote(this.getURL())) {
            throw new IOException("Cannot write to remote URLs!");
        }
        if (DiskCacheUtils.isLocal(this.getURL())) {
            URL url = new URL(this.getURL());
            if (url.getProtocol().equalsIgnoreCase("http")) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("PUT");
                return new HttpURLWriter(conn);
            } else {
                File file = new File(url.getFile());
                file.getParentFile().mkdirs();
                return new FileURLWriter(file);
            }
        }
        throw new IOException(this.getURL() + ": is not local or remote");
    }

    /**
       Return the last modified date of this entry.
    */
    public long getLastModified() {
        if (isLocal()) {
            try {
                String localfile = this.getURL().substring(5);
                this.lastModified = new File(localfile).lastModified();
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error getLastModified ", e);
                }
                e.printStackTrace();
            }
        }
        return this.lastModified;
    }

    /**
    Set the last modified date of this entry.
    */
    public void setLastModified(long time) {
        this.lastModified = time;
    }

    /**
       Set the url on which this is based.
    */
    public void setURL(String url) {
        if (DiskCacheUtils.isVirtual(url)) {
            url = DiskCacheUtils.getLocalURL(url);
        }
        this.url = url;
    }

    /**
    Set the expiration  date of this entry.
    */
    public long getExpirationTime() {
        return this.expires;
    }

    /**
    Set the expiration  date of this entry.
    */
    public void setExpirationTime(long time) {
        this.expires = time;
        if (this.expires < System.currentTimeMillis()) {
            this.expires = System.currentTimeMillis() + defaultExpirationInterval;
        }
    }

    /**
    */
    public boolean hasExpired() {
        return this.expires <= 0 || this.expires < System.currentTimeMillis();
    }

    /**
    */
    public boolean isLocal() {
        return DiskCacheUtils.isLocal(this.getSourceURL());
    }

    class HttpURLWriter extends OutputStreamWriter {

        private HttpURLConnection conn;

        public HttpURLWriter(HttpURLConnection conn) throws UnsupportedEncodingException, IOException {
            super(conn.getOutputStream(), encoding);
            this.conn = conn;
            logger.info("HttpURLWriter encoding -> " + encoding + " method -> " + this.conn.getRequestMethod());
        }

        public void close() throws IOException {
            super.close();
            this.conn.getResponseCode();
            logger.info("HttpURLWriter close encoding -> " + encoding + " method -> " + this.conn.getRequestMethod() + " Status -> " + this.conn.getResponseCode());
        }
    }

    class FileURLWriter extends FileWriter {

        private String filename;

        public FileURLWriter(File file) throws UnsupportedEncodingException, IOException {
            super(file);
            this.filename = file.getPath();
            logger.info("FileURLWriter opening file -> " + filename);
        }

        public void close() throws IOException {
            super.close();
            logger.info("FileURLWriter closing file -> " + filename);
        }
    }
}
