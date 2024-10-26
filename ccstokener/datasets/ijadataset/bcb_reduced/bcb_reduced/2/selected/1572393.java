package mf.mfrpc.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mf.mfrpc.InvokeException;
import mf.mfrpc.serialize.DataSource;
import mf.mfrpc.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http请求端, 通过application/octet-stream类型发送POST请求。
 * 支持参数和流数据的交互, 默认支持Cookie机制。
 */
public class HttpClient<T> {

    /** log */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** 默认的流缓冲字节大小 */
    public static final int BUFFER_SIZE = 4 * 1024;

    /** application/octet-stream类型POST请求的分隔符 */
    public static final String BOUNDARY = "---------------------------185444985";

    /** http请求流数据的参数名称 */
    public static final String DATASOURCE_NAME = "_upload_";

    /** part of length of the request content */
    private static final int STREAM_SIZE = startStream(DATASOURCE_NAME, "").length + 0 + "\r\n".getBytes().length;

    /** http请求结束符 */
    private static byte[] END = ("--" + BOUNDARY + "--\r\n").getBytes();

    /** http请求地址 */
    protected String url;

    /** default global static CookieManager */
    protected CookieHandler cookieManager = HttpUtil.getCookieManager();

    /** 流数据监听器 */
    protected DataSourceObserver observer;

    /** 流数据 */
    protected List<DataSource> dataSource;

    /** use a ByteArrayOutputStream to write request content */
    protected ByteArrayOutputStream bOutput = new ByteArrayOutputStream(1024);

    public HttpClient() {
        this.dataSource = new ArrayList<DataSource>(3);
    }

    public HttpClient(String url) {
        this.url = url;
        this.dataSource = new ArrayList<DataSource>(3);
    }

    /**
     * http请求。
     */
    public T invoke() throws InvokeException {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            setHeader(con);
            setCookies(con);
            int streamSize = 0;
            for (DataSource ds : dataSource) {
                streamSize += (STREAM_SIZE + ds.getName().getBytes().length + ds.getSize());
            }
            int streamLength = streamSize + bOutput.size() + END.length;
            if (streamLength < 0) {
                con.setChunkedStreamingMode(BUFFER_SIZE);
            } else {
                con.setFixedLengthStreamingMode(streamLength);
            }
            con.connect();
            OutputStream out = con.getOutputStream();
            bOutput.writeTo(out);
            byte[] b = new byte[BUFFER_SIZE];
            int len = 0;
            for (DataSource ds : dataSource) {
                out.write(startStream(DATASOURCE_NAME, ds.getName()));
                if (observer != null) {
                    observer.reset();
                    observer.setDataSource(ds);
                }
                InputStream in = ds.getInputStream();
                while ((len = in.read(b)) != -1) {
                    if (len > 0) {
                        out.write(b, 0, len);
                        out.flush();
                        if (observer != null) observer.upload(len);
                    }
                }
                in.close();
                out.write("\r\n".getBytes());
            }
            out.write(END);
            out.close();
            log.debug("{} : {} : {}", new Object[] { con.getResponseCode(), con.getResponseMessage(), con.getContentType() });
            if (HttpURLConnection.HTTP_OK == con.getResponseCode()) {
                return handleInputStream(con);
            } else {
                log.error(con.getResponseCode() + " : " + System.getProperty("line.separator"));
                if (HttpURLConnection.HTTP_INTERNAL_ERROR == con.getResponseCode()) {
                    log.error(new String(Base64.decode(con.getResponseMessage())));
                } else {
                    log.error(con.getResponseMessage());
                }
                return null;
            }
        } catch (IOException e) {
            throw new InvokeException(e);
        } finally {
            if (con != null) {
                storeCookies(con);
            }
            bOutput.reset();
            dataSource.clear();
        }
    }

    /**
     * a hook to handle http input stream。
     */
    protected T handleInputStream(HttpURLConnection con) throws IOException {
        return (T) con.getInputStream();
    }

    private static byte[] startStream(String paramName, String filename) {
        return new StringBuilder("--").append(BOUNDARY).append("\r\n").append("Content-Disposition: form-data; name=\"").append(paramName).append("\"; filename=\"").append(filename).append("\"\r\n").append("Content-Type: application/octet-stream\r\n\r\n").toString().getBytes();
    }

    /**
     * 添加请求参数。
     *
     * @param paramName 参数名称。
     * @param value 参数值。
     */
    public void addParameter(String paramName, String value) throws InvokeException {
        try {
            bOutput.write(new StringBuilder("--").append(BOUNDARY).append("\r\n").append("Content-Disposition: form-data; name=\"").append(paramName).append("\"\r\n").append("\r\n").append(value).append("\r\n").toString().getBytes());
        } catch (IOException e) {
            throw new InvokeException(e);
        }
    }

    /**
     * 添加请求参数。
     * 
     * @param paramName 参数名称。
     * @param value 参数byte数组类型的值。
     */
    public void addParameter(String paramName, byte[] value) throws InvokeException {
        try {
            bOutput.write(new StringBuilder("--").append(BOUNDARY).append("\r\n").append("Content-Disposition: form-data; name=\"").append(paramName).append("\"\r\n").append("Content-Type: application/octet-stream\r\n\r\n").toString().getBytes());
            bOutput.write(value);
            bOutput.write("\r\n".getBytes());
        } catch (IOException e) {
            throw new InvokeException(e);
        }
    }

    /**
     * 设置HttpURLConnection的请求属性。
     */
    public static void setHeader(HttpURLConnection con) {
        con.setDoOutput(true);
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.7) Gecko/20100713 Firefox/3.6.7");
        con.setRequestProperty("Accept-Language", "	zh-cn,zh;q=0.5");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        con.setRequestProperty("Accept-Encoding", "gzip, deflate");
        con.setRequestProperty("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Referer", "http://www.google.com");
    }

    /**
     * 存储http请求的cookie。
     */
    protected void storeCookies(HttpURLConnection con) throws CookieException {
        try {
            cookieManager.put(con.getURL().toURI(), con.getHeaderFields());
        } catch (IOException e) {
            throw new CookieException(e);
        } catch (URISyntaxException e) {
            throw new CookieException(e);
        }
    }

    /**
     * 从cookie存储中设置http请求的cookie。
     */
    protected void setCookies(HttpURLConnection con) throws CookieException {
        List<String> cookies = null;
        try {
            cookies = cookieManager.get(con.getURL().toURI(), Collections.EMPTY_MAP).get(HttpConstants.REQUEST_COOKIE_HEADER);
        } catch (IOException e) {
            throw new CookieException(e);
        } catch (URISyntaxException e) {
            throw new CookieException(e);
        }
        StringBuilder str = new StringBuilder();
        if (cookies != null && !cookies.isEmpty()) {
            int length = cookies.size();
            for (int i = 0; i < length - 1; i++) {
                str.append(cookies.get(i)).append(';');
            }
            str.append(cookies.get(length - 1));
            log.debug("set {} cookies : [{}]", con.getURL(), str.toString());
            con.setRequestProperty(HttpConstants.REQUEST_COOKIE_HEADER, str.toString());
        }
    }

    public List<DataSource> getDataSource() {
        return dataSource;
    }

    public void addDataSource(DataSource dataSource) {
        this.dataSource.add(dataSource);
    }

    public DataSourceObserver getObserver() {
        return observer;
    }

    public void setObserver(DataSourceObserver observer) {
        this.observer = observer;
    }

    public String getUrl() {
        return url;
    }

    public void resetUrl(String url) {
        this.url = url;
        this.dataSource.clear();
        this.bOutput.reset();
        this.observer = null;
    }

    public CookieHandler getCookieManager() {
        return cookieManager;
    }

    public void setCookieManager(CookieHandler cookieManager) {
        this.cookieManager = cookieManager;
    }
}
