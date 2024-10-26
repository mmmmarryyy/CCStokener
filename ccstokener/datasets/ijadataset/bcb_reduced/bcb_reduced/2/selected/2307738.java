package org.terukusu.android.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.terukusu.android.util.StringUtils;
import android.util.Log;

/**
 * Webからデータをダウンロードするためのクラスです。
 *
 * @author Teruhiko Kusunoki&lt;<a href="teru.kusu@gmail.com">teru.kusu@gmail.com</a>&gt;
 *
 */
public class HttpDownloader {

    /**
     * TAG for {@link android.util.Log}
     */
    private static final String TAG = HttpDownloader.class.getSimpleName();

    public static enum Method {

        POST, GET
    }

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    private static final int DEFAULT_SO_TIMEOUT = 10000;

    /** URLエンコーディングに指定する文字コード */
    private String encoding = StringUtils.getDefaultEncoding();

    private Method method = Method.GET;

    private String url;

    private Map<String, String> queries;

    private Map<String, String> cookies;

    private Map<String, String> headers;

    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    private int soTimeout = DEFAULT_SO_TIMEOUT;

    /** サーバーが返してきたContent-Lengthヘッダの値 */
    private long contentLength;

    /** ダウンロード開始からの経過時間 */
    private long elapsed;

    /** ダウンロード済みの容量 */
    private long downloaded;

    /** 平均通信速度(bps) */
    private long avarageSpeed;

    /** キャンセルが要求されたかどうか */
    private boolean isCanceled = false;

    /**
     * 新しいオブジェクトを生成します。
     *
     */
    public HttpDownloader() {
    }

    /**
     * 新しいオブジェクトを生成します。
     *
     * @param url URLです。
     */
    public HttpDownloader(String url) {
        setUrl(url);
    }

    /**
     * 新しいオブジェクトを生成します。
     *
     * @param url URL
     * @param queries クエリ
     */
    public HttpDownloader(String url, Map<String, String> queries) {
        this.url = url;
        this.queries = queries;
    }

    public HttpDownloader(String url, Map<String, String> queries, Map<String, String> cookies) {
        this.url = url;
        this.queries = queries;
        this.cookies = cookies;
    }

    /**
     * ダウンロードを実行します
     *
     * @throws SocketTimeoutException 接続時、または通信時にタイムアウタが発生した場合
     * @throws ClientProtocolException プロトコルに関する問題が有った場合
     * @throws IOException 入出力に問題が発生した場合
     */
    public HttpResponse execute() throws SocketTimeoutException, ClientProtocolException, IOException {
        long start = System.currentTimeMillis();
        HttpUriRequest request = null;
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        if (Method.POST == getMethod()) {
            HttpPost post = new HttpPost(getUrl());
            if (getQueries() != null) {
                for (Map.Entry<String, String> entry : getQueries().entrySet()) {
                    postParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
            post.setEntity(new UrlEncodedFormEntity(postParams, getEncoding()));
            request = post;
        } else {
            StringBuilder sb = new StringBuilder(getUrl());
            if (getQueries() != null) {
                if (sb.indexOf("?") >= 0) {
                    sb.append('&');
                } else {
                    sb.append('?');
                }
                sb.append(makeQueryString(getQueries()));
            }
            String fullUrl = sb.toString();
            HttpGet get = new HttpGet(fullUrl);
            request = get;
        }
        if (getCookies() != null) {
            String cookieStr = makeCookieString(getCookies());
            request.addHeader("Cookie", cookieStr);
        }
        if (getHeaders() != null) {
            for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
                request.addHeader(entry.getKey(), URLEncoder.encode(entry.getValue(), getEncoding()));
            }
        }
        HttpClient client = new DefaultHttpClient();
        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, getConnectionTimeout());
        client.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, getSoTimeout());
        org.apache.http.HttpResponse hr = null;
        HttpResponse res = new HttpResponse();
        hr = client.execute(request);
        res.setStatusCode(hr.getStatusLine().getStatusCode());
        for (Header header : hr.getAllHeaders()) {
            res.setHeader(header.getName(), header.getValue());
        }
        for (Header header : hr.getHeaders("Set-Cookie")) {
            String cookieEntry = header.getValue();
            Cookie cookie = Cookie.parseCookie(cookieEntry, null);
            res.setCookie(cookie.getKey(), cookie);
        }
        if (hr.getEntity() == null) {
            return res;
        }
        Header contentTypeHeader = hr.getEntity().getContentType();
        if (contentTypeHeader != null) {
            String contentType = contentTypeHeader.getValue();
            if (contentType != null) {
                Pattern p = Pattern.compile("text/[^;]+(\\s*;\\s*charset\\s*=\\s*(.+))?", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(contentType);
                if (m.find() && !StringUtils.isBlank(m.group(2))) {
                    res.setCharacterEncoding(m.group(2));
                }
            }
        }
        setContentLength(hr.getEntity().getContentLength());
        res.setContentLength(hr.getEntity().getContentLength());
        ByteArrayOutputStream baos = null;
        InputStream is = null;
        try {
            baos = new ByteArrayOutputStream();
            is = hr.getEntity().getContent();
            byte[] buff = new byte[getBufferSize()];
            int readSize = 0;
            while (!isCanceled()) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    synchronized (this) {
                        isCanceled = true;
                    }
                    break;
                }
                if ((readSize = is.read(buff)) < 0) {
                    break;
                }
                synchronized (this) {
                    setDownloaded(getDownloaded() + readSize);
                    setElapsed(System.currentTimeMillis() - start);
                    long elapsedSec = getElapsed() / 1000;
                    if (elapsedSec == 0) {
                        setAvarageSpeed(getDownloaded());
                    } else {
                        setAvarageSpeed(getDownloaded() / elapsedSec);
                    }
                }
                baos.write(buff, 0, readSize);
            }
            Log.v(TAG, "downloaded: avarage speed = " + getAvarageSpeed() + " bps, downloaded = " + getDownloaded() + " bytes");
            if (isCanceled()) {
                request.abort();
            }
        } finally {
            try {
                baos.close();
            } catch (Exception ignore) {
            }
            try {
                is.close();
            } catch (Exception ignore) {
            }
        }
        res.setBody(baos.toByteArray());
        return res;
    }

    /**
     * マップからURLのクエリ文字列を生成します。
     *
     * @param params マップ
     * @return クエリ文字列
     */
    private String makeQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            try {
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), getEncoding()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return sb.toString();
    }

    /**
     * Cookieヘッダとして送信するための文字列を生成します
     *
     * @param cookies クッキーとして送信するキーと値
     * @return Cookieヘッダ用の文字列です
     */
    private String makeCookieString(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * ダウンロードを中止します
     */
    public synchronized void cancel() {
        isCanceled = true;
    }

    /**
     * 現在の通信速度(bps)を取得
     * @return 現在の通信速度
     */
    public synchronized long getSpeed() {
        return 0;
    }

    /**
     * encoding を取得します。
     * @return encoding
     */
    public synchronized String getEncoding() {
        return encoding;
    }

    /**
     * encoding を設定します。
     * @param encoding セットする encoding
     */
    public synchronized void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * method を取得します。
     * @return method
     */
    public synchronized Method getMethod() {
        return method;
    }

    /**
     * method を設定します。
     * @param method セットする method
     */
    public synchronized void setMethod(Method method) {
        this.method = method;
    }

    /**
     * url を取得します。
     * @return url
     */
    public synchronized String getUrl() {
        return url;
    }

    /**
     * url を設定します。
     * @param url セットする url
     */
    public synchronized void setUrl(String url) {
        this.url = url;
    }

    /**
     * queries を取得します。
     * @return queries
     */
    public synchronized Map<String, String> getQueries() {
        return queries;
    }

    /**
     * queries を設定します。
     * @param queries セットする queries
     */
    public synchronized void setQueries(Map<String, String> queries) {
        this.queries = queries;
    }

    /**
     * elapsed を取得します。
     * @return elapsed
     */
    public synchronized long getElapsed() {
        return elapsed;
    }

    /**
     * elapsed を設定します。
     * @param elapsed セットする elapsed
     */
    public synchronized void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    /**
     * bufferSize を取得します。
     * @return bufferSize
     */
    public synchronized int getBufferSize() {
        return bufferSize;
    }

    /**
     * bufferSize を設定します。
     * @param bufferSize セットする bufferSize
     */
    public synchronized void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * cookies を取得します。
     * @return cookies
     */
    public synchronized Map<String, String> getCookies() {
        return cookies;
    }

    /**
     * cookies を設定します。
     * @param cookies セットする cookies
     */
    public synchronized void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    /**
     * contentLength を取得します。
     * @return contentLength
     */
    public synchronized long getContentLength() {
        return contentLength;
    }

    /**
     * contentLength を設定します。
     * @param contentLength セットする contentLength
     */
    protected synchronized void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * downloaded を取得します。
     * @return downloaded
     */
    public synchronized long getDownloaded() {
        return downloaded;
    }

    /**
     * downloaded を設定します。
     * @param downloaded セットする downloaded
     */
    protected synchronized void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    /**
     * isCanceled を取得します。
     * @return isCanceled
     */
    public synchronized boolean isCanceled() {
        return isCanceled;
    }

    /**
     * isCanceled を設定します。
     * @param isCanceled セットする isCanceled
     */
    protected synchronized void setCanceled(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    /**
     * avarageSpeed を取得します。
     * @return avarageSpeed
     */
    public synchronized long getAvarageSpeed() {
        return avarageSpeed;
    }

    /**
     * avarageSpeed を設定します。
     * @param avarageSpeed セットする avarageSpeed
     */
    protected synchronized void setAvarageSpeed(long avarageSpeed) {
        this.avarageSpeed = avarageSpeed;
    }

    /**
     * connectionTimeout を取得します。
     * @return connectionTimeout
     */
    public synchronized int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * connectionTimeout を設定します。
     * @param connectionTimeout セットする connectionTimeout
     */
    public synchronized void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * soTimeout を取得します。
     * @return soTimeout
     */
    public synchronized int getSoTimeout() {
        return soTimeout;
    }

    /**
     * soTimeout を設定します。
     * @param soTimeout セットする soTimeout
     */
    public synchronized void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    /**
     * headers を取得します。
     * @return headers
     */
    public synchronized Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * headers を設定します。
     * @param headers セットする headers
     */
    public synchronized void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
