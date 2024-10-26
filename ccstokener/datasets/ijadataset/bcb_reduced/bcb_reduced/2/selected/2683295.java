package com.kaixinff.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class HttpClient {

    private static final Map<String, String> IMAGE_MIME = new HashMap<String, String>() {

        {
            put(".gif", "image/gif");
            put(".ief", "image/ief");
            put(".jpe", "image/jpeg");
            put(".jpeg", "image/jpeg");
            put(".jpg", "image/jpeg");
            put(".pbm", "image/x-portable-bitmap");
            put(".pgm", "image/x-portable-graymap");
            put(".png", "image/png");
            put(".pnm", "image/x-portable-anymap");
            put(".ppm", "image/x-portable-pixmap");
            put(".ras", "image/cmu-raster");
            put(".rgb", "image/x-rgb");
            put(".tif", "image/tiff");
            put(".tiff", "image/tiff");
            put(".xbm", "image/x-xbitmap");
            put(".xpm", "image/x-xpixmap");
            put(".xwd", "image/x-xwindowdump");
        }
    };

    private static MutiCookieHandler defaultCookieHandler = new MutiCookieHandler();

    private static Proxy globalProxy;

    private static int globalTimeout = 30000;

    private MutiCookieHandler cookieHandler;

    private Proxy proxy;

    private int timeout;

    private int retryTime = 5;

    private long timestamp;

    static {
        HttpURLConnection.setFollowRedirects(false);
    }

    public static String getHost(String url) {
        Pattern p = Pattern.compile("http[:][/][/](.+?)[/]");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public HttpClient() {
        cookieHandler = defaultCookieHandler;
    }

    public HttpClient(boolean mutiMode) {
        if (!mutiMode) {
            cookieHandler = defaultCookieHandler;
        } else {
            cookieHandler = new MutiCookieHandler();
        }
    }

    public HttpClient(Proxy proxy) {
        this();
        this.proxy = proxy;
    }

    public HttpClient(Proxy proxy, boolean mutiMode) {
        this(mutiMode);
        this.proxy = proxy;
    }

    public void setCookies(Collection<String> cookies) {
        cookieHandler.setCookies(cookies);
    }

    public Collection<String> getCookies() {
        return cookieHandler.getCookies();
    }

    public String getCookie(String key) {
        return cookieHandler.getCookie(key);
    }

    public static void setGlobalProxy(Proxy proxy) {
        globalProxy = proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Proxy getProxy() {
        if (proxy != null) {
            return proxy;
        }
        return globalProxy;
    }

    public static void setGlobalTimeout(int timeout) {
        globalTimeout = timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        if (timeout > 0) {
            return timeout;
        }
        return globalTimeout;
    }

    public int getRetryTime() {
        return retryTime;
    }

    public void setRetryTime(int retryTime) {
        this.retryTime = retryTime;
    }

    public HttpResponse doPost(String url, String referer, NameValuePair[] params) throws IOException {
        return doPost(url, referer, params, "UTF-8");
    }

    public HttpResponse doPost(String url, String referer, NameValuePair[] params, String charset) throws IOException {
        HttpResponse resp = null;
        IOException ex = null;
        for (int i = 0; i < retryTime; i++) {
            try {
                resp = executeMethod(url, "POST", referer, params, charset);
            } catch (IOException e) {
                ex = e;
            }
            if (resp != null) {
                return resp;
            }
        }
        throw ex;
    }

    public HttpResponse doGet(String url, String referer) throws IOException {
        return doGet(url, referer, "UTF-8");
    }

    public HttpResponse doGet(String url, String referer, String charset) throws IOException {
        HttpResponse resp = null;
        IOException ex = null;
        for (int i = 0; i < retryTime; i++) {
            try {
                resp = executeMethod(url, "GET", referer, null, charset);
            } catch (IOException e) {
                ex = e;
            }
            if (resp != null) {
                return resp;
            }
        }
        throw ex;
    }

    public HttpResponse uploadFile(String url, String referer, String filename, String charset) throws IOException {
        File file = new File(filename);
        FileInputStream in = new FileInputStream(file);
        HttpURLConnection conn = createConnect(url, "POST", referer);
        String boundary = "---------------------------12292603834279";
        conn.addRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream out = conn.getOutputStream();
        out.write("--".getBytes());
        out.write(boundary.getBytes());
        out.write("\r\n".getBytes());
        out.write(("Content-Disposition: form-data; name=\"icon\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        out.write(("Content-Type: " + getMime(filename) + "\r\n\r\n").getBytes());
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.write("\r\n--".getBytes());
        out.write(boundary.getBytes());
        out.write("--\r\n".getBytes());
        out.flush();
        out.close();
        HttpResponse resp = new HttpResponse(conn, charset);
        readContent(resp, charset);
        return resp;
    }

    private HttpResponse executeMethod(String url, String method, String referer, NameValuePair[] params, String charset) throws IOException {
        HttpURLConnection conn = createConnect(url, method, referer);
        if (params != null) {
            for (int i = 0, n = params.length; i < n; i++) {
                if (i != 0) {
                    conn.getOutputStream().write('&');
                }
                byte[] bytes = params[i].encode(charset).getBytes();
                conn.getOutputStream().write(bytes);
            }
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
        }
        conn.connect();
        HttpResponse resp = new HttpResponse(conn, charset);
        return readContent(resp, charset);
    }

    private String getMime(String filename) {
        int index = filename.lastIndexOf('.');
        String extname = filename.substring(index);
        return IMAGE_MIME.get(extname);
    }

    private void waitAMoment() {
        long t = 1000 - (System.currentTimeMillis() - timestamp);
        if (t > 0) {
            try {
                Thread.sleep(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        timestamp = System.currentTimeMillis();
    }

    private HttpURLConnection createConnect(String url, String method, String referer) throws IOException {
        waitAMoment();
        HttpURLConnection conn = null;
        Proxy proxy = getProxy();
        if (proxy != null) {
            conn = (HttpURLConnection) new URL(url).openConnection(proxy);
        } else {
            conn = (HttpURLConnection) new URL(url).openConnection();
        }
        int timeout = getTimeout();
        if (timeout > 0) {
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        String host = getHost(url);
        if (host != null) {
            conn.addRequestProperty("Host", host);
        }
        conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.1.3) Gecko/20090824 Firefox/3.5.3 (.NET CLR 3.5.30729)");
        conn.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.addRequestProperty("Accept-Language", "zh-cn,zh;q=0.5");
        conn.addRequestProperty("Accept-Encoding", "gzip,deflate");
        conn.addRequestProperty("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        conn.addRequestProperty("Keep-Alive", "115");
        if (referer != null) {
            conn.addRequestProperty("Referer", referer);
        }
        cookieHandler.get(conn);
        return conn;
    }

    protected void setCookie(HttpURLConnection conn) throws IOException {
        cookieHandler.put(conn);
    }

    private HttpResponse readContent(HttpResponse resp, String charset) throws IOException {
        String encoding = resp.getHeaderField("Content-Encoding");
        setCookie(resp.getConn());
        if (resp.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || resp.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
            String url = resp.getHeaderField("location");
            if (!url.startsWith("http://")) {
                url = "http://" + resp.getHost() + url;
            }
            timestamp = 0;
            return doGet(url, null);
        }
        byte[] bytes = null;
        InputStream in = resp.getConn().getInputStream();
        if (encoding != null && "gzip".equals(encoding)) {
            GZIPInputStream gin = null;
            ByteArrayOutputStream out = null;
            try {
                gin = new GZIPInputStream(in);
                out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = gin.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                if (gin != null) {
                    gin.close();
                }
                if (out != null) {
                    out.close();
                }
            }
            bytes = out.toByteArray();
        } else {
            ByteArrayOutputStream out = null;
            try {
                out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
            bytes = out.toByteArray();
        }
        resp.setBytes(bytes);
        return resp;
    }
}
