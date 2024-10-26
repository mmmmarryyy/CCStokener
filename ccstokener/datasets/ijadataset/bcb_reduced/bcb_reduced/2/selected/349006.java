package org.xito.boot;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Properties;
import java.security.*;
import org.xito.boot.ui.DownloadProgressFrame;

/**
 * CacheManager is responsible for maintaining downloaded cached files and 
 * making sure the cache is up to date.
 * 
 * @author  Deane Richan
 */
public class CacheManager {

    private static Logger logger = Logger.getLogger(CacheManager.class.getName());

    private static SimpleDateFormat downloadDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    private DownloadProgressFrame downloadFrame;

    private File rootDir;

    private HashMap recentDownloads = new HashMap();

    private HashMap cacheFiles = new HashMap();

    private Properties cacheProperties = new Properties();

    private boolean cache_disabled = false;

    private boolean calculate_progress = true;

    private static int DELAY = 0;

    private static int BUF_SIZE = 1024;

    /** Creates a new instance of CacheManager */
    public CacheManager(File cacheDir, boolean disabled) {
        if (cacheDir.exists()) {
            if (cacheDir.isDirectory() == false) {
                throw new RuntimeException("Cache cannot be created because there is a file:" + cacheDir.toString() + " which already exists.");
            }
        } else {
            if (cacheDir.mkdir() == false) {
                throw new RuntimeException("Cache: " + cacheDir.toString() + " cannot be created.");
            }
        }
        rootDir = cacheDir;
        cache_disabled = disabled;
        System.setProperty("sun.net.client.defaultConnectTimeout", "60000");
        System.setProperty("sun.net.client.defaultReadTimeout", "60000");
        if (!Boot.isHeadless()) {
            downloadFrame = new DownloadProgressFrame(this);
        }
        readCacheProperties();
    }

    /**
    * Read Cache Settings from Props file
    */
    private void readCacheProperties() {
        try {
            File cachePropsFile = new File(rootDir, "cache.properties");
            if (cachePropsFile.exists()) {
                FileInputStream in = new FileInputStream(cachePropsFile);
                cacheProperties.load(in);
                in.close();
            }
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, "Error Reading Cache Settings:" + ioExp.getMessage(), ioExp);
        }
        if (downloadFrame != null) {
            downloadFrame.setPreferredLocationAndSize();
        }
    }

    /**
    * Store Cache Settings to Props file
    */
    public void storeCacheProperties() {
        try {
            File cachePropsFile = new File(rootDir, "cache.properties");
            FileOutputStream out = new FileOutputStream(cachePropsFile);
            cacheProperties.store(out, null);
            out.close();
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, "Error Writing Cache Settings:" + ioExp.getMessage(), ioExp);
        }
    }

    /**
    * Return the Cache Settings that this CacheManager uses
    */
    public Properties getCacheProperties() {
        return cacheProperties;
    }

    /**
    * Convert a URL to a Cache URL. 
    */
    public URL convertToCachedURL(URL u) {
        try {
            return getCachedFileForURL(u).toURL();
        } catch (MalformedURLException badURL) {
            logger.log(Level.SEVERE, badURL.getMessage(), badURL);
        }
        return u;
    }

    /**
    * Convert from a Cache URL. 
    */
    public URL convertFromCachedURL(URL u) {
        if (u == null) return u;
        try {
            String urlStr = u.toString();
            String rootURLStr = this.rootDir.toURL().toString();
            if (!u.getProtocol().equals("file") || !urlStr.startsWith(rootURLStr)) {
                return u;
            }
            Properties infoProps = readInfo(u);
            String nonCacheURL = infoProps.getProperty("resource");
            if (nonCacheURL != null && !nonCacheURL.equals("")) {
                return new URL(nonCacheURL);
            }
            int s = rootURLStr.length();
            int e = urlStr.length();
            urlStr = urlStr.substring(s, e);
            s = 0;
            e = urlStr.indexOf('/', s);
            String protocol = urlStr.substring(s, e);
            s = e + 1;
            e = urlStr.indexOf('/', s);
            String host = urlStr.substring(s, e);
            s = e + 1;
            e = urlStr.indexOf('/', s);
            String port = urlStr.substring(s, e);
            s = e + 1;
            e = urlStr.length();
            String file = urlStr.substring(s, e);
            if (file.endsWith("_root_")) {
                file = file.substring(0, file.lastIndexOf("/") + 1);
            }
            return new URL(protocol, host, Integer.valueOf(port).intValue(), file);
        } catch (Exception badURL) {
            logger.log(Level.WARNING, "Can't convert Cache URL to a regular URL:" + u.toString(), badURL);
        }
        return u;
    }

    /**
    * Check to see if a cached file is up to date with the original resource by
    * Checking content length and size. If the resource's length and size is unknown
    * or it is different from the cached file this method will return false
    */
    public boolean isUptoDate(URL resource, CacheListener listener) {
        if (cache_disabled) return false;
        if (resource.getProtocol().equals("file")) {
            return true;
        }
        String name = null;
        try {
            File cachedFile = getCachedFileForURL(resource);
            Properties infoProps = readInfo(resource);
            if (infoProps.isEmpty() || !cachedFile.exists()) {
                logger.info("UptoDate check failed Cached file doesn't exist:" + cachedFile.toString());
                return false;
            }
            Long lastDownloadTime = (Long) recentDownloads.get(resource);
            if (lastDownloadTime != null) {
                int RECENT = 300000;
                if ((System.currentTimeMillis() - lastDownloadTime.longValue()) < RECENT) {
                    return true;
                }
            }
            CachePolicy policy = CachePolicy.getPolicy(infoProps.getProperty("cache-policy"));
            String completed_str = infoProps.getProperty("completed");
            boolean completed = (completed_str != null && completed_str.equals("true")) ? true : false;
            Date lastDownload = null;
            String lastDownloadStr = infoProps.getProperty("last-downloaded");
            if (lastDownloadStr != null && !lastDownloadStr.equals("")) {
                try {
                    lastDownload = downloadDateFormat.parse(lastDownloadStr);
                } catch (ParseException e) {
                    lastDownload = null;
                } catch (NumberFormatException badNum) {
                    lastDownload = null;
                }
            }
            if (completed && Boot.isOffline()) {
                return true;
            }
            if (completed && lastDownload != null && !policy.equals(CachePolicy.ALWAYS)) {
                long MILLIS_PER_DAY = 1000L * 60L * 60L * 24L;
                long MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;
                long MILLIS_PER_MONTH = MILLIS_PER_DAY * 30;
                long dif = System.currentTimeMillis() - lastDownload.getTime();
                if (policy.equals(CachePolicy.DAILY) && dif < MILLIS_PER_DAY) {
                    return true;
                } else if (policy.equals(CachePolicy.WEEKLY) && dif < MILLIS_PER_WEEK) {
                    return true;
                } else if (policy.equals(CachePolicy.MONTHLY) && dif < MILLIS_PER_MONTH) {
                    return true;
                }
            }
            name = getDownloadName(resource);
            CacheEvent event = new CacheEvent(this, name, resource, -1, -1, -1, -1);
            if (listener != null) {
                listener.gettingInfo(event);
            }
            URLConnection conn = resource.openConnection();
            logger.finer("Getting UptoDate Info");
            long lastModified = conn.getLastModified();
            int size = conn.getContentLength();
            logger.finer("Done Getting UptoDate Info");
            if (cachedFile.lastModified() != lastModified) {
                logger.info("UptoDate check failed Last Modified Dates don't match:" + cachedFile.toString());
                return false;
            }
            if (cachedFile.length() != size) {
                logger.info("UptoDate check failed content sizes don't match:" + cachedFile.toString());
                return false;
            }
            return true;
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, "Error checking up to date:" + ioExp.getMessage(), ioExp);
        } finally {
            CacheEvent event = new CacheEvent(this, name, resource, -1, -1, -1, -1);
            if (listener != null) {
                listener.completeGettingInfo(event);
            }
        }
        return false;
    }

    /**
    * Cache a Resource and return its URL. If the cache is not up to date
    * then the content will be downloaded and cached and then the URL to the
    * cached resource will be returned
    * @param URL of content
    * @return URL of cached content
    * @throws IOException if there is a problem downloading the resource
    */
    public URL getResource(URL resource, CacheListener listener, CachePolicy policy) throws IOException {
        logger.fine("Getting Cached Content for:" + resource.toString());
        downloadResource(resource, listener, policy);
        File cachedFile = getCachedFileForURL(resource);
        return cachedFile.toURL();
    }

    /**
    * Get a local File object for a cached URL
    */
    public File getCachedFileForURL(final URL url) throws MalformedURLException {
        logger.fine("getCachedFileForURL:" + url);
        File f = null;
        f = (File) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    String rootDirStr = rootDir.toURL().toString();
                    String urlStr = url.toString();
                    if (urlStr.startsWith(rootDirStr)) {
                        return new File(url.getFile());
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.toString(), e);
                }
                return null;
            }
        });
        if (f != null) return f;
        f = (File) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    String bootURI = Boot.getBootDir().toURI().toString();
                    if (url.toString().startsWith(bootURI)) {
                        return new File(url.getFile());
                    }
                } catch (Exception e) {
                }
                return null;
            }
        });
        if (f != null) return f;
        if (url.getProtocol().equals("file")) {
            f = new File(url.getFile());
        }
        if (f != null) return f;
        long start = System.currentTimeMillis();
        f = (File) cacheFiles.get(url.toString());
        if (f != null && f.exists()) return f;
        String host = url.getHost();
        String protocol = url.getProtocol();
        int port = url.getPort();
        String path = url.getPath();
        String filePath = url.getFile();
        int lastSlash = path.lastIndexOf("/");
        String fileName = (lastSlash != -1) ? path.substring(lastSlash) : null;
        if (fileName == null || fileName.equals("") || fileName.equals("/")) {
            fileName = "_root_";
        }
        if (lastSlash > -1) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        String dirName = null;
        if (protocol.equals("file")) {
            if (System.getProperty("os.name").startsWith("Windows") && path.indexOf(':') > -1) {
                path = path.replace(':', '_');
            }
            dirName = protocol + "/" + host + "/" + path;
        } else {
            dirName = protocol + "/" + host + "/" + port + "/" + path;
        }
        final String newDirName = dirName;
        File dir = (File) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                File dir = new File(rootDir, newDirName);
                if (dir.exists() == false) {
                    dir.mkdirs();
                }
                return dir;
            }
        });
        f = new File(dir, fileName);
        cacheFiles.put(url.toString(), f);
        logger.fine("Cache File:" + f.toString());
        return f;
    }

    /**
    * The default Listener for the cache manager
    */
    public CacheListener getDefaultListener() {
        return downloadFrame;
    }

    /**
    * Download a single Resource Resource in this Thread. Uses a CachePolicy of ALWAYS
    *
    * @param url to download
    * @param listener to notify
    */
    public void downloadResource(URL url, CacheListener listener) throws IOException {
        downloadResource(url, listener, CachePolicy.ALWAYS);
    }

    /**
    * Download a single Resource Resource in this Thread
    */
    public void downloadResource(final URL url, CacheListener listener, CachePolicy policy) throws IOException {
        Boolean inBootDir = (Boolean) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                String bootURI = Boot.getBootDir().toURI().toString();
                return new Boolean(url.toString().startsWith(bootURI));
            }
        });
        if (inBootDir.booleanValue()) {
            return;
        }
        if (isUptoDate(url, listener)) return;
        download(url, listener, policy);
    }

    /**
    * Download the contents of the URL to the local Cache
    * @param url to download
    * @param listener to notify
    * @param cache policy to use. If null uses CachePolicy.ALWAYS
    */
    private void download(URL url, CacheListener listener, CachePolicy policy) throws IOException {
        long start = System.currentTimeMillis();
        logger.info("Downloading Resource: " + url.toString());
        String name = getDownloadName(url);
        CacheEvent event = new CacheEvent(this, name, url, -1, -1, -1, -1);
        if (listener != null) {
            listener.startDownload(event);
        }
        if (policy == null) {
            policy = CachePolicy.ALWAYS;
        }
        File file = null;
        try {
            URLConnection conn = url.openConnection();
            logger.finer("Getting Download Stream: " + url.toString());
            InputStream in = conn.getInputStream();
            long lastModified = conn.getLastModified();
            int size = conn.getContentLength();
            logger.finer("Done Getting Download Stream: " + url.toString());
            if (!calculate_progress) size = -1;
            int progressSize = 0;
            long progressTime = 0;
            long estimateTime = 0;
            long startTime = System.currentTimeMillis();
            file = getCachedFileForURL(url);
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[BUF_SIZE];
            int count = in.read(buf);
            while (count > 0) {
                out.write(buf, 0, count);
                progressSize += count;
                progressTime = System.currentTimeMillis() - startTime;
                if (size != -1 && progressSize < size) {
                    estimateTime = Math.round(((1.0 / (progressSize / (double) size)) * (double) progressTime) - progressTime);
                } else {
                    estimateTime = -1;
                }
                event = new CacheEvent(this, name, url, size, progressSize, progressTime, estimateTime);
                if (listener != null) {
                    listener.updateDownload(event);
                }
                if (DELAY > 0) {
                    try {
                        Thread.sleep(DELAY);
                    } catch (Exception e) {
                        logger.severe(e.getMessage());
                    }
                }
                count = in.read(buf);
            }
            in.close();
            out.close();
            recentDownloads.put(url, new Long(System.currentTimeMillis()));
            writeInfo(url, file, policy, true);
            if (lastModified != 0) {
                file.setLastModified(lastModified);
            }
            estimateTime = 0;
            event = new CacheEvent(this, name, url, size, progressSize, progressTime, estimateTime);
            if (listener != null) {
                listener.completeDownload(event);
            }
            logger.info("Downloading Complete:" + url.toString());
            logger.fine("Download Time:" + (System.currentTimeMillis() - start));
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, ioExp.getMessage(), ioExp);
            writeInfo(url, file, policy, false);
            if (listener != null) {
                listener.downloadException(name, url, ioExp.getMessage(), ioExp);
            }
            throw ioExp;
        }
    }

    /**
    * Write out the information file about this cached Resource
    */
    protected void writeInfo(URL url, File file, CachePolicy policy, boolean completed) {
        if (file == null) return;
        File infoFile = new File(file.getAbsolutePath() + ".info");
        Properties props = new Properties();
        props.setProperty("resource", url.toString());
        props.setProperty("cache-file", file.toString());
        props.setProperty("last-downloaded", downloadDateFormat.format(new Date()));
        props.setProperty("cache-policy", policy.toString());
        props.setProperty("completed", Boolean.toString(completed));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(infoFile);
            props.store(out, null);
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, ioExp.getMessage(), ioExp);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioExp) {
                    ioExp.printStackTrace();
                }
            }
        }
    }

    protected Properties readInfo(URL url) {
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            File cacheFile = this.getCachedFileForURL(url);
            if (cacheFile.exists() == false) return props;
            File infoFile = new File(cacheFile.getAbsolutePath() + ".info");
            if (infoFile.exists() == false) return props;
            in = new FileInputStream(infoFile);
            props.load(in);
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, ioExp.getMessage(), ioExp);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioExp) {
                    ioExp.printStackTrace();
                }
            }
        }
        return props;
    }

    /**
    * Get a descriptive name for this download URL
    */
    protected String getDownloadName(URL u) {
        String path = u.getPath();
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        return fileName;
    }

    /**
    * Download a single resource in a background Thread
    */
    public Thread downloadResourceInBackground(final URL url, final CacheListener listener, final CachePolicy policy, ThreadGroup g) {
        Thread t = new Thread(g, new Runnable() {

            public void run() {
                try {
                    downloadResource(url, listener, policy);
                } catch (Exception exp) {
                    logger.log(Level.WARNING, exp.getMessage() + ":" + url.toString(), exp);
                    if (listener != null) {
                        listener.downloadException(getDownloadName(url), url, exp.getMessage(), exp);
                    }
                }
            }
        });
        t.start();
        return t;
    }

    /** 
    * Get a specific policy from an Array of Policies. The policy returned will
    * default to ALWAYS if policies are null or index is out of range
    */
    private CachePolicy getPolicy(CachePolicy policies[], int index) {
        if (policies == null || policies.length == 0) return CachePolicy.ALWAYS;
        CachePolicy policy = policies[0];
        try {
            policy = policies[index];
        } catch (IndexOutOfBoundsException badIndex) {
            policy = policies[policies.length - 1];
        }
        if (policy == null) {
            policy = CachePolicy.ALWAYS;
        }
        return policy;
    }

    /**
    * Download multiple resources
    *
    * @param urls to download
    * @param listener to be notified
    * @param CachePolicies to use for each item
    * @param atSameTime true if all URLs are to be fetched at the same time or false if one at a time
    */
    public void downloadResources(final URL urls[], final CacheListener listener, final CachePolicy policies[], boolean atSameTime) {
        ThreadGroup g = (ThreadGroup) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return new ThreadGroup("CacheManager: downloadResources");
            }
        });
        if (atSameTime) {
            for (int i = 0; i < urls.length; i++) {
                downloadResourceInBackground(urls[i], listener, getPolicy(policies, i), g);
            }
            try {
                while (g.activeCount() > 0) {
                    Thread.sleep(500);
                    Thread.yield();
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            Thread t = new Thread(g, new Runnable() {

                public void run() {
                    for (int i = 0; i < urls.length; i++) {
                        try {
                            downloadResource(urls[i], listener, getPolicy(policies, i));
                        } catch (Exception exp) {
                            logger.log(Level.WARNING, exp.getMessage(), exp);
                            if (listener != null) {
                                listener.downloadException(getDownloadName(urls[i]), urls[i], exp.getMessage(), exp);
                            }
                        }
                    }
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException exp) {
                logger.log(Level.SEVERE, exp.getMessage(), exp);
            }
        }
    }

    /**
    * Download multiple resources in background Thread or Threads
    *
    * @param urls to download
    * @param listener to be notified
    * @param cache policy
    * @param atSameTime true if all URLs are to be fetched at the same time or false if one at a time
    */
    public Thread downloadResourcesInBackground(final URL urls[], final CacheListener listener, final CachePolicy policies[], final boolean atSameTime) {
        Thread t = new Thread(new Runnable() {

            public void run() {
                downloadResources(urls, listener, policies, atSameTime);
            }
        });
        t.start();
        return t;
    }

    /**
    * Clears the Entire Local Cache
    * @return true if the whole cache could be cleared false other wise
    */
    public boolean clearCache() {
        return clearCacheDir(rootDir, false);
    }

    /**
    * Clears the Cache of a list of Resources
    * @param url of resources to clear from cache
    * @return true if the resources were cleared from the cache
    */
    public boolean clearCache(URL[] resources) {
        if (resources == null) return true;
        boolean failed = false;
        for (int i = 0; i < resources.length; i++) {
            if (clearCache(resources[i]) == false && failed == false) {
                failed = true;
            }
        }
        return failed;
    }

    /**
    * Clears the Cache of a single Resource
    * @param url of resource to clear from cache
    * @return true if the resource was cleared from the cache
    */
    public boolean clearCache(URL resource) {
        try {
            File f = getCachedFileForURL(resource);
            File infoF = new File(f.getAbsoluteFile() + ".info");
            if (f.exists() == false) {
                return true;
            }
            if (f.isFile()) {
                infoF.delete();
                return f.delete();
            } else {
                return clearCacheDir(f, true);
            }
        } catch (IOException ioExp) {
            logger.log(Level.WARNING, ioExp.getMessage(), ioExp);
            return false;
        }
    }

    /**
    * Clears an Entire directory in the Cache
    */
    private boolean clearCacheDir(File dir, boolean deleteDir) {
        if (dir == null) return true;
        if (dir.exists() == false) return true;
        if (dir.isFile()) {
            return dir.delete();
        }
        File[] children = dir.listFiles();
        boolean failed = false;
        for (int i = 0; i < children.length; i++) {
            if (children[i].isFile()) {
                if (children[i].delete() == false && failed == false) {
                    failed = true;
                }
            } else {
                if (clearCacheDir(children[i], true) == false && failed == false) {
                    failed = true;
                }
            }
        }
        if (failed) {
            return failed;
        }
        if (deleteDir) return dir.delete(); else return true;
    }
}
