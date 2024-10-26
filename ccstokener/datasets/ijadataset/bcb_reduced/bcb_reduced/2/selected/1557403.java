package com.loribel.commons.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import com.loribel.commons.util.FTools;
import com.loribel.commons.util.GB_NumberTools;
import com.loribel.commons.util.STools;

/**
 * Tools to use HttpURLConnection.
 * Cette version utilise HttpURLConnection
 *
 * @deprecated Ancienne version de GB_HttpTools
 * @author Gregory Borelli
 */
final class GB_HttpTools2 {

    public static boolean canAccess(URL a_url, boolean a_useProxyCache) {
        try {
            InputStream in = getInputStream(a_url, a_useProxyCache);
            in.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static InputStream getInputStream(URL a_url, boolean a_useProxyCache) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) a_url.openConnection();
        if (!a_useProxyCache) {
            connection.setRequestProperty("Cache-Control", "no-cache");
        }
        return connection.getInputStream();
    }

    /**
     * Load url into dir.
     * replace {1} into a_url by index.
     */
    public static void loadUrlsToDir(String a_url, File a_destDir, boolean a_useProxyCache, int a_indexStart, int a_indexEnd, String a_pattern) throws IOException {
        for (int i = a_indexStart; i <= a_indexEnd; i++) {
            String l_index = GB_NumberTools.applyPattern(i, a_pattern);
            String l_url = STools.replace(a_url, l_index);
            File l_destFile = new File(a_destDir, l_index + ".html");
            loadUrlToFile(l_url, l_destFile, a_useProxyCache);
        }
    }

    /**
     * Load the content of a file from url to a local file.
     */
    public static void loadUrlToFile(String a_url, File a_destinationFile, boolean a_useProxyCache) throws IOException {
        URL l_url = new URL(a_url);
        loadUrlToFile(l_url, a_destinationFile, a_useProxyCache);
    }

    /**
     * Load the content of a file from url to a local file.
     *
     * @param a_url URL - a_url the url of the page or file to download
     * @param a_destinationFile File - a_destinationFile the destination file
     * @param a_useProxyCache boolean - a_useProxyCache true, if use the cache of proxy
     */
    public static void loadUrlToFile(URL a_url, File a_destinationFile, boolean a_useProxyCache) throws IOException {
        a_destinationFile.getParentFile().mkdirs();
        OutputStream l_out = new FileOutputStream(a_destinationFile);
        loadUrlToOutputStream(a_url, l_out, a_useProxyCache);
        FTools.closeSafe(l_out);
    }

    /**
     * Load the content of a file from url to a local file.
     *
     * @param a_string URL - a_url the url of the page or file to download
     * @param a_destinationFile File - a_destinationFile the destination file
     * @param a_useProxyCache boolean - a_useProxyCache true, if use the cache of proxy
     */
    public static void loadUrlToOutputStream(URL a_url, OutputStream a_out, boolean a_useProxyCache) throws IOException {
        InputStream in = null;
        try {
            in = getInputStream(a_url, a_useProxyCache);
            int count = 0;
            byte buffer[] = new byte[4096];
            while ((count = in.read(buffer)) > 0) {
                a_out.write(buffer, 0, count);
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static String loadUrlToString(URL a_url, boolean a_useProxyCache) throws IOException {
        OutputStream l_out = new ByteArrayOutputStream();
        GB_HttpTools2.loadUrlToOutputStream(a_url, l_out, false);
        String retour = l_out.toString();
        FTools.closeSafe(l_out);
        return retour;
    }

    /**
     * Remove the settings of the proxy.
     * Update the System Properties.
     */
    public static void removeProxy() {
        Properties prop = System.getProperties();
        prop.put("proxySet", "false");
        prop.put("proxyHost", null);
        prop.put("proxyPort", null);
    }

    /**
     * Set a proxy configuration.
     * Update the System Properties.
     *
     * @param a_proxyHost String - a_proxyHost the host of the proxy
     * @param a_proxyPort int - a_proxyPort the port of the proxy
     */
    public static void setProxy(String a_proxyHost, int a_proxyPort) {
        Properties prop = System.getProperties();
        prop.put("proxySet", "true");
        prop.put("proxyHost", a_proxyHost);
        prop.put("proxyPort", "" + a_proxyPort);
    }
}
