package jnewsgate.http;

import jnewsgate.util.*;
import jnewsgate.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.logging.*;

/**
 * Caches sites retrieved via HTTP.
 */
public class WebCache {

    private static WebCache instance;

    private static Logger l = Log.get();

    public static final String USER_AGENT = "jNewsGate/" + Main.VERSION;

    public static synchronized WebCache getInstance() {
        if (instance == null) instance = new WebCache();
        return instance;
    }

    private Map cachedData = new CacheMap(1024);

    private int flushInterval = -1;

    private long lastFlush = 0;

    private WebCache() {
    }

    public void setFlushInterval(int fi) {
        l.finest("Flush Interval set to " + fi);
        flushInterval = fi;
    }

    public void flushMaybe() {
        l.finest("Maybe flushing");
        if (flushInterval == -1) return;
        long now = System.currentTimeMillis();
        if (lastFlush + flushInterval < now) {
            l.fine("Flushing WebCache");
            cachedData.clear();
            lastFlush = now;
        }
    }

    public String getSite(String url) {
        try {
            return getSite(new URL(url));
        } catch (MalformedURLException ex) {
            l.log(Level.SEVERE, "Cannot convert String to URL", ex);
            return "";
        }
    }

    public String getSite(URL url) {
        return getSite(url, USER_AGENT);
    }

    public String getSite(URL url, String userAgent) {
        String urlS = url.toString();
        String data = (String) cachedData.get(urlS);
        if (data != null) return data;
        int pos = urlS.indexOf("&password=");
        if (pos == -1) pos = urlS.length();
        l.fine("Requesting " + urlS.substring(0, pos));
        try {
            URLConnection uc = url.openConnection();
            uc.setRequestProperty("User-Agent", userAgent);
            InputStream in = uc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
            StringBuffer sb = new StringBuffer();
            char[] buf = new char[4096];
            int len;
            while ((len = br.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
            String res = sb.toString();
            cachedData.put(urlS, res);
            l.finest("Done requesting.");
            return res;
        } catch (UnknownHostException ex) {
            l.warning(ex.toString());
            return "";
        } catch (IOException ex) {
            l.log(Level.SEVERE, "IOException", ex);
            return "";
        }
    }

    /**
     * Test code to fill something into the cache.
     */
    public void putCache(URL url, InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
            StringBuffer data = new StringBuffer();
            char[] buf = new char[4096];
            int len;
            while ((len = br.read(buf)) != -1) {
                data.append(buf, 0, len);
            }
            String res = data.toString();
            cachedData.put(url.toString(), res);
        } catch (IOException ex) {
            l.log(Level.SEVERE, "Error writing into cache", ex);
        }
    }
}
