package chrriis.udoc.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ContentHandlerFactory;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class URLConnectionEx {

    protected static String defaultUserAgent = "Mozilla/5.0";

    public static void setDefaultUserAgent(String defaultUserAgent) {
        URLConnectionEx.defaultUserAgent = defaultUserAgent;
    }

    protected URLConnection urlConnection;

    protected boolean isCompressionActive;

    protected URLConnectionEx(URLConnection urlConnection, boolean isCompressionActive) {
        this.urlConnection = urlConnection;
        this.isCompressionActive = isCompressionActive;
        if (isCompressionActive) {
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        }
        urlConnection.setRequestProperty("User-Agent", defaultUserAgent);
    }

    public static URLConnectionEx openConnection(URL url) throws IOException {
        boolean isCompressionActive = false;
        if (url != null) {
            String protocol = url.getProtocol();
            if ("http".equals(protocol) || "https".equals(protocol)) {
                isCompressionActive = true;
            }
        }
        return new URLConnectionEx(url.openConnection(), isCompressionActive);
    }

    public void connect() throws java.io.IOException {
        urlConnection.connect();
    }

    public boolean getAllowUserInteraction() {
        return urlConnection.getAllowUserInteraction();
    }

    public Object getContent() throws IOException {
        return urlConnection.getContent();
    }

    public String getContentEncoding() {
        return urlConnection.getContentEncoding();
    }

    public int getContentLength() {
        return urlConnection.getContentLength();
    }

    public String getContentType() {
        return urlConnection.getContentType();
    }

    public long getDate() {
        return urlConnection.getDate();
    }

    public static boolean getDefaultAllowUserInteraction() {
        return URLConnection.getDefaultAllowUserInteraction();
    }

    public static String getDefaultRequestProperty(String key) {
        return URLConnection.getDefaultRequestProperty(key);
    }

    public boolean getDefaultUseCaches() {
        return urlConnection.getDefaultUseCaches();
    }

    public boolean getDoInput() {
        return urlConnection.getDoInput();
    }

    public boolean getDoOutput() {
        return urlConnection.getDoOutput();
    }

    public long getExpiration() {
        return urlConnection.getExpiration();
    }

    public static FileNameMap getFileNameMap() {
        return URLConnection.getFileNameMap();
    }

    public String getHeaderField(int n) {
        return urlConnection.getHeaderField(n);
    }

    public String getHeaderField(String name) {
        return urlConnection.getHeaderField(name);
    }

    public long getHeaderFieldDate(String name, long default_) {
        return urlConnection.getHeaderFieldDate(name, default_);
    }

    public int getHeaderFieldInt(String name, int default_) {
        return urlConnection.getHeaderFieldInt(name, default_);
    }

    public String getHeaderFieldKey(int n) {
        return urlConnection.getHeaderFieldKey(n);
    }

    public long getIfModifiedSince() {
        return urlConnection.getIfModifiedSince();
    }

    public InputStream getInputStream() throws IOException {
        InputStream in = urlConnection.getInputStream();
        if (isCompressionActive) {
            String encoding = urlConnection.getContentEncoding();
            if (encoding != null) {
                encoding = encoding.toLowerCase(Locale.ENGLISH);
                if ("gzip".equals(encoding)) {
                    in = new GZIPInputStream(in);
                } else if ("deflate".equals(encoding)) {
                    in = new InflaterInputStream(in, new Inflater(true));
                }
            }
        }
        return in;
    }

    public long getLastModified() {
        return urlConnection.getLastModified();
    }

    public OutputStream getOutputStream() throws IOException {
        return urlConnection.getOutputStream();
    }

    public String getRequestProperty(String key) {
        return urlConnection.getRequestProperty(key);
    }

    public URL getURL() {
        return urlConnection.getURL();
    }

    public boolean getUseCaches() {
        return urlConnection.getUseCaches();
    }

    public static String guessContentTypeFromName(String fname) {
        return URLConnection.guessContentTypeFromName(fname);
    }

    public static String guessContentTypeFromStream(InputStream is) throws IOException {
        return URLConnection.guessContentTypeFromStream(is);
    }

    public void setAllowUserInteraction(boolean allowUserInteraction) {
        urlConnection.setAllowUserInteraction(allowUserInteraction);
    }

    public static void setContentHandlerFactory(ContentHandlerFactory fac) {
        URLConnection.setContentHandlerFactory(fac);
    }

    public static void setDefaultAllowUserInteraction(boolean defaultAllowUserInteraction) {
        URLConnection.setDefaultAllowUserInteraction(defaultAllowUserInteraction);
    }

    public static void setDefaultRequestProperty(String key, String value) {
        URLConnection.setDefaultRequestProperty(key, value);
    }

    public void setDefaultUseCaches(boolean defaultUseCaches) {
        urlConnection.setDefaultUseCaches(defaultUseCaches);
    }

    public void setDoInput(boolean doInput) {
        urlConnection.setDoInput(doInput);
    }

    public void setDoOutput(boolean doOutput) {
        urlConnection.setDoOutput(doOutput);
    }

    public static void setFileNameMap(FileNameMap map) {
        URLConnection.setFileNameMap(map);
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        urlConnection.setIfModifiedSince(ifModifiedSince);
    }

    public void setRequestProperty(String key, String value) {
        urlConnection.setRequestProperty(key, value);
    }

    public void setUseCaches(boolean useCaches) {
        urlConnection.setUseCaches(useCaches);
    }

    public String toString() {
        return urlConnection.toString();
    }

    public URLConnection getWrappedURLConnection() {
        return urlConnection;
    }
}
