package com.iv.flash.url;

import com.iv.flash.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Implementation of regular url
 *
 * @author Dmitry Skavish
 */
public class URLUrl extends IVUrl {

    private URL url;

    private URLConnection conn;

    private long lastModified = 0;

    private long lastConnect = 0;

    /**
     * Creates URLUrl from URL
     *
     * @param url    specified URL
     * @exception IVException
     */
    public URLUrl(URL url) throws IVException {
        this.url = url;
    }

    public String getParameter(String name) {
        if (parms == null) {
            try {
                parse(getName());
            } catch (IVException e) {
                Log.logRB(e);
            }
            if (parms == null) {
                parms = new Hashtable();
            }
        }
        return super.getParameter(name);
    }

    public String getName() {
        return url.toExternalForm();
    }

    public String getRef() {
        return url.getRef();
    }

    public long lastModified() {
        return lastModified;
    }

    public InputStream getInputStream() throws IOException {
        connect();
        Log.logRB(Resource.RETRIEVINGCONTENT, new Object[] { getName() });
        return conn.getInputStream();
    }

    public void refresh() {
        try {
            connect();
        } catch (IOException e) {
            Log.logRB(e);
        }
    }

    private synchronized void connect() throws IOException {
        long now = System.currentTimeMillis();
        if (lastConnect == 0 || lastConnect + 500 < now) {
            Log.logRB(Resource.CONNECTINGTO, new Object[] { getName() });
            String auth = setProxy();
            conn = url.openConnection();
            if (auth != null) conn.setRequestProperty("Proxy-Authorization", auth);
            conn.connect();
            lastModified = conn.getLastModified();
            lastConnect = System.currentTimeMillis();
        }
    }

    private static String setProxy() {
        String auth = null;
        String useProxy = PropertyManager.getProperty("com.iv.flash.http.proxy.enable");
        if ("true".equalsIgnoreCase(useProxy)) {
            String proxy = PropertyManager.getProperty("com.iv.flash.http.proxy.host");
            String proxyport = PropertyManager.getProperty("com.iv.flash.http.proxy.port");
            String myUserName = PropertyManager.getProperty("com.iv.flash.http.proxy.username");
            String myPassword = PropertyManager.getProperty("com.iv.flash.http.proxy.password");
            if (proxy != null) {
                Properties props = System.getProperties();
                props.put("proxySet", "true");
                props.put("proxyHost", proxy);
                if (proxyport != null) props.put("proxyPort", proxyport); else props.put("proxyPort", "80");
                if (myUserName != null && myUserName.trim().length() > 0) {
                    String authString = myUserName + ":" + ((myPassword != null && myPassword.trim().length() > 0) ? myPassword : "");
                    auth = "Basic " + new sun.misc.BASE64Encoder().encode(authString.getBytes());
                }
            }
        }
        return auth;
    }
}
