package net.siuying.any2rss.loader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class URLLoader implements ContentLoaderIF {

    private static Log log = LogFactory.getLog(URLLoader.class);

    private String encodedProxyLogin;

    /**
     * <p>
     * If "loader.urlloader.proxy.host" and "loader.urlloader.proxy.port"
     * configuration is set, the loader use these settings
     * </p>
     * <p>
     * If "loader.urlloader.proxy.user" and "loader.urlloader.proxy.password"
     * configuration is set, the loader use this to login
     * 
     * @param config The configuration
     * @throws ConfigurationException error in configuration
     */
    public void configure(Configuration config) throws ConfigurationException {
        String proxy = null;
        int proxyPort = -1;
        String proxyUser = null;
        String proxyPass = null;
        try {
            proxy = config.getString("loader.urlloader.proxy.host", "");
            proxyPort = config.getInt("loader.urlloader.proxy.port", -1);
            proxyUser = config.getString("loader.urlloader.proxy.user", "");
            proxyPass = config.getString("loader.urlloader.proxy.password", "");
        } catch (ConversionException ce) {
            throw new ConfigurationException("Error occurs reading parameter", ce);
        }
        if (!proxy.equals("") && proxyPort != -1) {
            this.setProxy(proxy, proxyPort);
            this.setProxyEnabled(true);
        }
        if (proxyUser.equals("") && proxyPass.equals("")) {
            this.setProxyLogin(proxyUser, proxyPass);
        }
        log.trace("done configure URLLoader ... ");
    }

    public String load(String url) throws LoaderException {
        try {
            return load(new URL(url));
        } catch (MalformedURLException e) {
            log.fatal("URL cannot be created: " + e.getMessage());
            throw new LoaderException("URL cannot be created: ", e);
        }
    }

    public String load(URL url) throws LoaderException {
        log.debug("loading content");
        log.trace("opening connection: " + url);
        BufferedReader in = null;
        URLConnection conn = null;
        try {
            conn = url.openConnection();
            in = null;
            if (encodedProxyLogin != null) {
                conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedProxyLogin);
            }
        } catch (IOException ioe) {
            log.warn("Error create connection");
            throw new LoaderException("Error create connection", ioe);
        }
        log.trace("connection opened, reading ... ");
        StringBuilder buffer = new StringBuilder();
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                buffer.append(inputLine);
            }
        } catch (IOException ioe) {
            log.warn("Error loading content");
            throw new LoaderException("Error reading content. ", ioe);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
        log.debug("content loaded");
        return buffer.toString();
    }

    /**
     * set login for proxy that require username and password
     */
    private void setProxyLogin(String user, String pass) {
        String authentication = user + ":" + pass;
        encodedProxyLogin = new sun.misc.BASE64Encoder().encodeBuffer(authentication.getBytes());
    }

    private void setProxyEnabled(boolean enabled) {
        if (enabled) {
            System.getProperties().put("http.proxySet", "true");
        } else {
            System.getProperties().put("http.proxySet", "false");
        }
    }

    private void setProxy(String host, int port) {
        System.getProperties().put("http.proxyHost", host);
        System.getProperties().put("http.proxyPort", new Integer(port));
    }
}
