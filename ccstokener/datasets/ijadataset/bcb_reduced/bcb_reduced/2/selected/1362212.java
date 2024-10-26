package org.freehold.jukebox.conf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * The configuration watcher.
 *
 * <p>
 *
 * Strictly speaking, this object is a singleton, but given all the troubles
 * with the singletons in the application server environment, I'd rather
 * leave it an instance entity - little overhead, great flexibility.
 *
 * <p>
 *
 * The only drawback for not making this object a singleton is a thread
 * consumption - one extra thread per watcher, but I don't believe it is a
 * major issue. Just excercise caution.
 *
 * <p>
 *
 * <strong>NOTE:</strong> It is strongly recommended to {@link #stop stop()}
 * the watcher when you're done, otherwise your application will not
 * terminate nicely because of the extra thread run by the watcher.
 *
 * @author Copyright &copy; <a href="mailto:vt@freehold.crocodile.org">Vadim Tkachenko</a> 1998-2001
 * @version $Id: ConfigurationWatcher.java,v 1.4 2001-04-12 05:22:44 vtt Exp $
 */
public class ConfigurationWatcher {

    /**
     * Mapping of URLs to listener sets.
     *
     * <p>
     *
     * In most cases, the set will consist of just one listener, but anyway,
     * since it is possible for multiple listeners to watch the same
     * configuration objects, and this code is not time-critical, it's OK to
     * have the set of listeners as a value.
     *
     * @see #listener2set
     */
    private HashMap url2listener = new HashMap();

    /**
     * Mapping of listeners to the URL sets they want to get the
     * notification about.
     *
     * <p>
     *
     * Likewise, the values are sets of URL sets. It is possible for the
     * same listener to listen to multiple URL sets.
     *
     * @see #url2listener
     */
    private HashMap listener2set = new HashMap();

    /**
     * URL metadata.
     *
     * <p>
     *
     * The last modified date is stored here. The metadata record doesn't
     * get deleted when the listener is removed, 'cause they may want to
     * come back.
     */
    private HashMap url2meta = new HashMap();

    /**
     * Polling interval, in milliseconds.
     *
     * <p>
     *
     * The polls are <strong>not</strong> guaranteed to happen for all the
     * URLs in exactly this many milliseconds, rather, each poll will start
     * this many milliseconds after the previous one is complete. This is a
     * good approximation - the exactness doesn't really matter here.
     */
    private long interval;

    /**
     * The watcher thread.
     */
    private Thread watcher;

    /**
     * Flag for the watcher to finish working if there are no listeners
     * present.
     */
    private boolean watcherEnabled = false;

    /**
     * Create the instance with a default polling interval of one minute.
     */
    public ConfigurationWatcher() {
        this(60 * 1000);
    }

    /**
     * Create the instance with a specified polling interval.
     *
     * @param interval Polling interval, in milliseconds.
     *
     * @exception IllegalArgumentException if the polling interval is less
     * than half a second - this is just unreasonable.
     */
    public ConfigurationWatcher(long interval) {
        setInterval(interval);
    }

    /**
     * Set the polling interval.
     *
     * @param interval Polling interval, in milliseconds.
     *
     * @exception IllegalArgumentException if the polling interval is less
     * than half a second - this is just unreasonable.
     */
    public void setInterval(long interval) {
        if (interval < 500) {
            throw new IllegalArgumentException("Polling interval of " + interval + " milliseconds? You're nuts");
        }
        this.interval = interval;
    }

    /**
     * Add the configuration change listener.
     *
     * <p>
     *
     * Remember that registering with the configuration watcher will prevent
     * your listener from being garbage collected.
     *
     * @param ccl The consumer to be notified when one of the URLs in the
     * set is modified.
     *
     * @param conf Configuration to retrieve the array of URLs to watch
     * from. This configuration object will not be referenced by the
     * watcher.
     *
     * @exception IllegalArgumentException if either parameter is
     * <code>null</code> or empty.
     *
     * @see #remove
     */
    public synchronized void add(ConfigurationChangeListener ccl, Configuration conf) {
        add(ccl, conf.getUrlChain());
    }

    /**
     * Add the configuration change listener.
     *
     * <p>
     *
     * Remember that registering with the configuration watcher will prevent
     * your listener from being garbage collected.
     *
     * @param ccl The consumer to be notified when one of the URLs in the
     * set is modified.
     *
     * @param urlSet Array of URLs to watch.
     *
     * @exception IllegalArgumentException if either parameter is
     * <code>null</code> or empty.
     *
     * @see #remove
     */
    public synchronized void add(ConfigurationChangeListener ccl, URL urlSet[]) {
        checkAddArguments(ccl, urlSet);
        if (listener2set.isEmpty()) {
            start();
        }
        addListener2Set(ccl, urlSet);
        addUrl2Listener(ccl, urlSet);
        init(urlSet);
    }

    /**
     * @see #add
     *
     * @exception if either parameter is either <code>null</code> or empty.
     */
    private void checkAddArguments(ConfigurationChangeListener ccl, URL urlSet[]) {
        if (urlSet == null || urlSet.length == 0) {
            throw new IllegalArgumentException("urlSet is either null or empty");
        }
        if (ccl == null) {
            throw new IllegalArgumentException("listener is null, who are you trying to notify?");
        }
    }

    /**
     * Look up the entry in the {@link #listener2set listener2set}, if it
     * doesn't exist, create one.
     */
    private void addListener2Set(ConfigurationChangeListener ccl, URL urlSet[]) {
        HashSet set = (HashSet) listener2set.get(ccl);
        if (set == null) {
            set = new HashSet();
            listener2set.put(ccl, set);
        }
        set.add(urlSet);
    }

    /**
     * Look up the entry in the {@link #url2listener url2listener}, if it
     * doesn't exist, create one.
     */
    private void addUrl2Listener(ConfigurationChangeListener ccl, URL urlSet[]) {
        for (int idx = 0; idx < urlSet.length; idx++) {
            HashSet set = (HashSet) url2listener.get(urlSet[idx]);
            if (set == null) {
                set = new HashSet();
                url2listener.put(urlSet[idx], set);
            }
            set.add(ccl);
        }
    }

    /**
     * Remove the configuration change listener.
     *
     * @param ccl Listener to remove.
     *
     * @exception IllegalArgumentException if the listener is
     * <code>null</code> or wasn't previously added.
     */
    public synchronized void remove(ConfigurationChangeListener ccl) {
        if (ccl == null) {
            throw new IllegalArgumentException("listener is null");
        }
        removeListener2Set(ccl);
        removeUrl2Listener(ccl);
        if (listener2set.isEmpty()) {
            watcherEnabled = false;
        }
    }

    /**
     * Remove the listener from the {@link #listener2set listener to URL
     * set} mapping.
     *
     * @param ccl Listener to remove.
     *
     * @exception IllegalArgumentException if the listener wasn't there.
     */
    private void removeListener2Set(ConfigurationChangeListener ccl) {
        if (listener2set.remove(ccl) == null) {
            throw new IllegalArgumentException("listener wasn't present: " + ccl.getClass().getName() + "@" + ccl.hashCode());
        }
    }

    /**
     * Remove all the URLs and listeners from {@link #url2listener
     * url2listener}.
     *
     * @param ccl Listener to find the URLs for.
     */
    private void removeUrl2Listener(ConfigurationChangeListener ccl) {
        HashSet result = new HashSet();
        for (Iterator i = url2listener.keySet().iterator(); i.hasNext(); ) {
            Object key = i.next();
            HashSet cclSet = (HashSet) url2listener.get(key);
            if (cclSet.contains(ccl)) {
                result.add(key);
                cclSet.remove(ccl);
                if (cclSet.isEmpty()) {
                    i.remove();
                }
            }
        }
    }

    /**
     * Start the watcher thread.
     */
    private void start() {
        watcherEnabled = true;
        Runnable r = new Runnable() {

            public void run() {
                while (watcherEnabled) {
                    try {
                        Thread.sleep(interval);
                        HashSet keys = new HashSet();
                        keys.addAll(url2listener.keySet());
                        for (Iterator i = keys.iterator(); i.hasNext(); ) {
                            URL url = (URL) i.next();
                            check(url);
                        }
                    } catch (Throwable t) {
                        if (t instanceof InterruptedException) {
                            return;
                        }
                        System.err.println("Unhandled exception, cycle may not be complete:");
                        t.printStackTrace();
                    }
                }
            }
        };
        watcher = new Thread(r);
        watcher.start();
    }

    /**
     * Retrieve the metadata for the given set of URLs.
     *
     * @param urlSet URL array to retrieve the metadata for.
     */
    private synchronized void init(URL urlSet[]) {
        for (int idx = 0; idx < urlSet.length; idx++) {
            final URL url = urlSet[idx];
            if (url2meta.containsKey(url)) {
                continue;
            }
            url2meta.put(url, null);
            Runnable r = new Runnable() {

                public void run() {
                    update(url);
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
    }

    /**
     * Retrieve the URL metadata and store it.
     *
     * <p>
     *
     * This method is synchronous.
     *
     * @param url The URL to retrieve the metadata for.
     *
     * @exception IllegalArgumentException if the URL protocol is neither
     * <code>http</code> nor <code>file</code>. That leaves
     * <code>ftp</code>, which will be handled if necessity arises.
     */
    private void update(URL url) {
        try {
            String protocol = url.getProtocol();
            if ("http".equalsIgnoreCase(protocol)) {
                updateHTTP(url);
            } else if ("file".equalsIgnoreCase(protocol)) {
                updateFile(url);
            } else {
                throw new IllegalArgumentException("Don't know how to handle protocol '" + protocol + "'");
            }
        } catch (Throwable t) {
            System.err.println("Trying to update " + url);
            t.printStackTrace();
        }
    }

    /**
     * Retrieve the last modified date for a given HTTP URL.
     *
     * @param url HTTP URL to retrieve the last modified date for.
     *
     * @see #url2meta
     */
    private void updateHTTP(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        long lastModified = conn.getHeaderFieldDate("last-modified", -1L);
        if (lastModified == -1L) {
            throw new IOException("Last-Modified: header missing");
        }
        url2meta.put(url, new Long(lastModified));
    }

    /**
     * Retrieve the last modified date for a given file URL.
     *
     * @param url file URL to retrieve the last modified date for.
     *
     * @see #url2meta
     */
    private void updateFile(URL url) {
        File target = new File(url.getFile());
        url2meta.put(url, new Long(target.lastModified()));
    }

    /**
     * Check if the URL last modified date has changed and if so, notify the
     * listener for this URL.
     *
     * @param url URL to check.
     */
    private void check(URL url) {
        Long oldMeta = (Long) url2meta.get(url);
        update(url);
        Long newMeta = (Long) url2meta.get(url);
        if (oldMeta == null || newMeta == null) {
            return;
        }
        if (newMeta.longValue() > oldMeta.longValue()) {
            HashSet set = (HashSet) url2listener.get(url);
            for (Iterator i = set.iterator(); i.hasNext(); ) {
                ConfigurationChangeListener ccl = (ConfigurationChangeListener) i.next();
                try {
                    ccl.configurationChanged(url);
                } catch (Throwable t) {
                    System.err.println("Listener failed to process notification: " + ccl.getClass().getName() + "@" + Integer.toHexString(ccl.hashCode()));
                    t.printStackTrace();
                }
            }
        }
    }

    /**
     * Get the string representation.
     *
     * This shouldn't be neede in normal circumstances.
     *
     * @return The string representation of internal data structures.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("url2listener: " + url2listener);
        result.append("\n");
        result.append("listener2set: [");
        for (Iterator i = listener2set.keySet().iterator(); i.hasNext(); ) {
            Object key = i.next();
            result.append(key.toString()).append("={");
            HashSet set = (HashSet) listener2set.get(key);
            int idx = 0;
            for (Iterator j = set.iterator(); j.hasNext(); ) {
                if (idx++ > 0) {
                    result.append(",");
                }
                Object oUrl = j.next();
                try {
                    URL url[] = (URL[]) oUrl;
                    for (int ui = 0; ui < url.length; ui++) {
                        if (ui > 0) {
                            result.append(",");
                        }
                        result.append(url[ui].toString());
                    }
                } catch (ClassCastException ccex) {
                    System.err.println("Got: " + oUrl.getClass().getName());
                    ccex.printStackTrace();
                }
            }
            result.append("}");
        }
        result.append("]\n");
        result.append("url2meta:     " + url2meta);
        result.append("\n");
        return result.toString();
    }

    /**
     * Shut down the watcher.
     *
     * Stop the {@link #watcher watcher thread}, remove all listeners.
     */
    public synchronized void stop() {
        if (watcherEnabled) {
            watcherEnabled = false;
            watcher.interrupt();
            HashSet set = new HashSet(listener2set.keySet());
            for (Iterator i = set.iterator(); i.hasNext(); ) {
                remove((ConfigurationChangeListener) i.next());
            }
        }
        if (!listener2set.isEmpty() || !url2listener.isEmpty()) {
            throw new IllegalStateException("Inconsistent state: maps are not empty:\n" + toString());
        }
    }

    public void finalize() throws Throwable {
        stop();
    }
}
