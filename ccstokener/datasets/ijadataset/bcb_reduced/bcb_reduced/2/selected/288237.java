package net.firefly.client.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class HTTPTools {

    public static String encodeURLPart(String part) throws FireflyClientException {
        try {
            return URLEncoder.encode(part, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FireflyClientException(e, "Unsupported URL encoding (UTF-8)");
        }
    }

    public String getResponseEncoding(HttpURLConnection con) throws IOException {
        String encoding = System.getProperty("file.encoding");
        String contentType = con.getContentEncoding();
        if (contentType != null) {
            int index = contentType.indexOf("charset=");
            if (index != -1) {
                encoding = contentType.substring(index + 8).trim();
            }
        }
        return encoding;
    }

    public static HttpURLConnection getUrlConnection(URL url, String proxyHost, String proxyPort, String aclUsername, String aclPassword, Map additionnalHeaders) throws IOException {
        Properties systemSettings = System.getProperties();
        String oldHttpProxyHost = systemSettings.getProperty("http.proxyHost");
        String oldHttpProxyPort = systemSettings.getProperty("http.proxyPort");
        String oldHttpsProxyHost = systemSettings.getProperty("https.proxyHost");
        String oldHttpsProxyPort = systemSettings.getProperty("https.proxyPort");
        String oldHandler = systemSettings.getProperty("java.protocol.handler.pkgs");
        systemSettings.remove("http.proxyHost");
        systemSettings.remove("http.proxyPort");
        systemSettings.remove("https.proxyHost");
        systemSettings.remove("https.proxyPort");
        systemSettings.remove("java.protocol.handler.pkgs");
        System.setProperties(systemSettings);
        String protocol = url.getProtocol();
        if (proxyHost != null && proxyPort != null) {
            systemSettings.put("proxySet", "true");
            if ("https".equals(protocol)) {
                systemSettings.put("https.proxyHost", proxyHost);
                systemSettings.put("https.proxyPort", proxyPort);
            } else if ("http".equals(protocol)) {
                systemSettings.put("http.proxyHost", proxyHost);
                systemSettings.put("http.proxyPort", proxyPort);
            }
        }
        if ("https".equals(protocol)) {
            systemSettings.put("java.protocol.handler.pkgs", "sun.net.www.protocol.https");
        } else if ("http".equals(protocol)) {
            systemSettings.put("java.protocol.handler.pkgs", "sun.net.www.protocol.http");
        }
        systemSettings.put("sun.net.client.defaultConnectTimeout", "5000");
        systemSettings.put("sun.net.client.defaultReadTimeout", "30000");
        System.setProperties(systemSettings);
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setUseCaches(false);
            if (aclUsername != null || aclPassword != null) {
                String authorization = ((aclUsername != null) ? aclUsername : "") + ":" + ((aclPassword != null) ? aclPassword : "");
                BASE64Encoder encoder = new BASE64Encoder();
                String encodedAuthorization = encoder.encode(authorization.getBytes());
                con.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
            }
            if (additionnalHeaders != null) {
                Iterator it = additionnalHeaders.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String value = (String) additionnalHeaders.get(key);
                    con.setRequestProperty(key, value);
                }
            }
            return con;
        } catch (IOException e) {
            throw e;
        } finally {
            if (oldHttpProxyHost != null) {
                systemSettings.put("http.proxyHost", oldHttpProxyHost);
            }
            if (oldHttpProxyPort != null) {
                systemSettings.put("http.proxyPort", oldHttpProxyPort);
            }
            if (oldHttpsProxyHost != null) {
                systemSettings.put("https.proxyHost", oldHttpsProxyHost);
            }
            if (oldHttpsProxyPort != null) {
                systemSettings.put("https.proxyPort", oldHttpsProxyPort);
            }
            if (oldHandler != null) {
                systemSettings.put("java.protocol.handler.pkgs", oldHandler);
            }
            System.setProperties(systemSettings);
        }
    }

    /**
	 * Dumps url connection related system properties. Used for debug only.
	 * 
	 */
    protected static void dumpSystemProperties() {
        Properties systemSettings = System.getProperties();
        System.out.println("-------------------------------- HTTP(S) SETTINGS --------------------------------");
        System.out.println("proxySet" + " : " + systemSettings.get("proxySet"));
        System.out.println("http.proxyHost" + " : " + systemSettings.get("http.proxyHost"));
        System.out.println("http.proxyPort" + " : " + systemSettings.get("http.proxyPort"));
        System.out.println("https.proxyHost" + " : " + systemSettings.get("https.proxyHost"));
        System.out.println("https.proxyPort" + " : " + systemSettings.get("https.proxyPort"));
        System.out.println("java.protocol.handler.pkgs" + " : " + systemSettings.get("java.protocol.handler.pkgs"));
        System.out.println("----------------------------------------------------------------------------------");
    }
}
