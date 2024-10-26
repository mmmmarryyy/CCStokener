package net.sourceforge.autofeed;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a Web Document, which can be a html file, an image
 * or a sound file among others.
 * 
 * Please pay a visit to this class creator's website, www.nadeausoftware.com.
 * Copyright notice from the site owner:
 * All of the text, images, data, marks, logomarks and other content of this 
 * site are the property of the site owner, unless noted otherwise. No material 
 * from this site may be copied, reproduced, republished, uploaded, posted, 
 * transmitted, or distributed in any way, without explicit permission, except 
 * that you may download one copy of the materials on any single computer for 
 * your personal, non-commercial home use only, provided you keep intact all 
 * copyright and other proprietary notices. Modification of the materials or use 
 * of the materials for any other purpose is a violation of copyright and other 
 * proprietary rights. For purposes of these terms, the use of any such material
 * on any other web site or networked computer environment is prohibited.
 * 
 * (The author has granted me permission to use his code as part of this project)
 * @author David Robert Nadeau, Ph.D. (www.nadeausoftware.com)
 */
public class WebFile implements Serializable {

    private java.util.Map<String, java.util.List<String>> responseHeader;

    private java.net.URL responseURL;

    private int responseCode = -1;

    private String MIMEtype;

    private String charset;

    private byte[] content;

    private static int defaultTimeoutMillis = 10000;

    private static String defaultUserAgent = "WebFile";

    /**
     * Open a Web File
     * @param urlString the file's URL
     * @throws java.net.MalformedURLException if the url provided wasn't a valid one
     * or didn't use the http/https protocol
     * @throws java.io.IOException if the document couldn't be opened
     */
    public WebFile(String urlString) throws java.net.MalformedURLException, java.io.IOException {
        this(new URL(urlString), defaultUserAgent, defaultTimeoutMillis);
    }

    /**
     * Open a Web File
     * @param url the file's URL
     * @throws java.io.IOException if the document couldn't be opened
     */
    public WebFile(URL url) throws IOException, java.net.MalformedURLException {
        this(url, defaultUserAgent, defaultTimeoutMillis);
    }

    /**
     * Open a Web File
     * @param url the file's URL
     * @param userAgentString the string sent as UserAgent request header field
     * @param timeoutMillis timeout in milliseconds
     * @throws java.io.IOException if file could not be fetched
     * @throws java.net.MalformedURLException if url was not valid (maybe not http/https?)
     */
    public WebFile(URL url, String userAgentString, int timeoutMillis) throws IOException, java.net.MalformedURLException {
        try {
            final java.net.URLConnection uconn = url.openConnection();
            final java.net.HttpURLConnection conn = (java.net.HttpURLConnection) uconn;
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-agent", userAgentString);
            conn.connect();
            responseHeader = conn.getHeaderFields();
            responseCode = conn.getResponseCode();
            responseURL = conn.getURL();
            final int length = conn.getContentLength();
            final String type = conn.getContentType();
            if (type != null) {
                final String[] parts = type.split(";");
                MIMEtype = parts[0].trim();
                for (int i = 1; i < parts.length && charset == null; i++) {
                    final String t = parts[i].trim();
                    final int index = t.toLowerCase().indexOf("charset=");
                    if (index != -1) {
                        charset = t.substring(index + 8);
                    }
                }
            }
            java.io.InputStream stream = conn.getErrorStream();
            if (stream == null) {
                stream = conn.getInputStream();
            }
            content = readStream(length, stream);
            conn.disconnect();
        } catch (ClassCastException ex) {
            throw new MalformedURLException("Url provided '" + url + "' didn't use " + "http/https protocol");
        }
    }

    /** Read stream bytes and transcode. */
    private byte[] readStream(int length, java.io.InputStream stream) throws java.io.IOException {
        final int buflen = Math.max(1024, Math.max(length, stream.available()));
        byte[] buf = new byte[buflen];
        byte[] bytes = null;
        for (int nRead = stream.read(buf); nRead != -1; nRead = stream.read(buf)) {
            if (bytes == null) {
                bytes = buf;
                buf = new byte[buflen];
                continue;
            }
            final byte[] newBytes = new byte[bytes.length + nRead];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            System.arraycopy(buf, 0, newBytes, bytes.length, nRead);
            bytes = newBytes;
        }
        return bytes;
    }

    /**
     * @return true if the content is (recognized as) an image
     */
    public boolean isContentImage() {
        if (content != null && MIMEtype.startsWith("image")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true if the content has only text
     */
    public boolean isContentText() {
        if (content != null && MIMEtype.startsWith("text")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the content formatted as a String, or null if the content wasn't text
     */
    public String getContentAsText() {
        if (!isContentText()) {
            return null;
        } else {
            if (charset != null) {
                try {
                    return new String(content, charset);
                } catch (UnsupportedEncodingException ex) {
                    return null;
                }
            } else {
                return new String(content);
            }
        }
    }

    /** 
     * @return the content without any formatting. 
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Get the HTTP request response code. 200 means OK.
     * More codes @ http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
     * @return the response code. 
     */
    public int getResponseCode() {
        return responseCode;
    }

    /** 
     * @return the response header. 
     */
    public java.util.Map<String, java.util.List<String>> getHeaderFields() {
        return responseHeader;
    }

    /** 
     * @return the URL of the received page. 
     */
    public java.net.URL getURL() {
        return responseURL;
    }

    /** 
     * @return the MIME type of the content fetched
     */
    public String getMIMEType() {
        return MIMEtype;
    }

    public String getCharset() {
        return charset;
    }

    /**
     * Test this class to ensure correct functionality
     * @param args command line args
     */
    public static void main(String[] args) {
        try {
            WebFile wf = new WebFile("http://www.google.com");
            String s = wf.getContentAsText();
            if (s == null) {
                System.out.println("It wasn't text");
            } else {
                System.out.println(s);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(WebFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
