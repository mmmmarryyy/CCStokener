package org.gocha.inetools.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.gocha.files.FileUtil;

/**
 * Абстрактный класс HTTP Запроса
 * @author gocha
 */
public abstract class AbstractHttpRequest implements HttpRequest {

    private SSLSocketFactory sslSocketFactory = null;

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sslSockFactory) {
        this.sslSocketFactory = sslSockFactory;
    }

    protected HttpHeaders header = null;

    @Override
    public HttpHeaders getHeader() {
        if (header == null) {
            header = new HttpHeaders();
        }
        return header;
    }

    @Override
    public void setHeader(HttpHeaders header) {
        this.header = header;
    }

    protected URL url = null;

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    protected boolean useCaches = false;

    /**
     * Использовать кеш JRE
     * @return true - использовать; false - не использовать
     */
    public boolean isUseCaches() {
        return useCaches;
    }

    /**
     * Использовать кеш JRE
     * @param useCaches true - использовать; false - не использовать
     */
    public void setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
    }

    protected int connectTimeout = 15000;

    /**
     * Время соединения с сервером
     * @return время соединения в милисикундах
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Время соединения (таймаут) с сервером
     * @param connectTimeout время соединения в милисикундах
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    protected int readTimeout = 15000;

    /**
     * Время чтения (таймаут)
     * @return Таймаут чтения в милисикундах
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Время чтения (таймаут)
     * @param readTimeout Таймаут чтения в милисикундах
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    protected Proxy proxy = null;

    @Override
    public Proxy getProxy() {
        return proxy;
    }

    @Override
    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    protected Boolean instanceFollowRedirects = null;

    /**
     * Свойство: Следовать перенаправлениям (ответы сервера со статусом 3xx)
     * @return true - Следовать; false - не следовать; null - по умолчанию
     */
    public Boolean getInstanceFollowRedirects() {
        return instanceFollowRedirects;
    }

    /**
     * Свойство: Следовать перенаправлениям (ответы сервера со статусом 3xx)
     * @param follow true - Следовать; false - не следовать; null - по умолчанию
     */
    public void setInstanceFollowRedirects(Boolean follow) {
        instanceFollowRedirects = follow;
    }

    private boolean disconnect = true;

    /**
     * Свойство: разъединять соединение после выполнения запроса
     * @return true - Разъединять; false - Оставлять (по умолчанию true)
     */
    public boolean isDisconnect() {
        return disconnect;
    }

    /**
     * Свойство: разъединять соединение после выполнения запроса
     * @param disconnect true - Разъединять; false - Оставлять (по умолчанию true)
     */
    public void setDisconnect(boolean disconnect) {
        this.disconnect = disconnect;
    }

    /**
     * Свойство для всех HTTP соединений - Следовать перенаправлениям (ответы сервера со статусом 3xx)
     * @return true - следовать; false - не следовать
     */
    public static boolean isFollowRedirects() {
        return HttpURLConnection.getFollowRedirects();
    }

    /**
     * Свойство для всех HTTP соединений - Следовать перенаправлениям (ответы сервера со статусом 3xx)
     * @param follow true - следовать; false - не следовать
     */
    public static void setFollowRedirects(boolean follow) {
        HttpURLConnection.setFollowRedirects(follow);
    }

    private Long ifModifiedSince = null;

    /**
     * Свойство указывает - с какого времени возвращать измененный документ.<br/>
     * Если документ не был измененн с указанного времени, то тело будет пустое.
     * @return Время условия изм. документа, или null если не указано поле.
     */
    public Long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Свойство указывает - с какого времени возвращать измененный документ.<br/>
     * Если документ не был измененн с указанного времени, то тело будет пустое.
     * @param ifModifiedSince Время условия изм. документа, либо null если не указано значение.
     */
    public void setIfModifiedSince(Long ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    /**
     * Тело запроса, вызывающая сторона закрывает поток
     * @return тело изапроса или null
     */
    protected InputStream getContentInputStream() {
        return null;
    }

    ;

    /**
     * Тип содержимого тела запроса
     * @return Тип содержимого тела или null
     */
    protected String getContentType() {
        return null;
    }

    ;

    /**
     * Длина тела запроса
     * @return длина или -1
     */
    protected int getContentLength() {
        return -1;
    }

    ;

    /**
     * Тип запроса
     * @return тип запроса
     */
    protected abstract String getRequestMethod();

    /**
     * Устанавливает заголовки для указанного соединения
     * @param connection Соединение
     */
    protected void setRequestHeader(URLConnection connection) {
        for (String key : getHeader().keySet()) {
            if (key == null) continue;
            String val = getHeader().get(key);
            if (val == null) continue;
            connection.setRequestProperty(key, val);
        }
    }

    /**
     * Создает ответ на запрос
     * @param connection Соедиение
     * @return HTTP Ответ
     * @throws IOException Ошибка передачи данных
     */
    protected HttpResponse createResponse(URLConnection connection) throws IOException {
        InputStream input = connection.getInputStream();
        Map<String, List<String>> downloadHeaderFields = connection.getHeaderFields();
        BasicHttpResponse httpResp = new BasicHttpResponse(this, downloadHeaderFields, getDownloadPredicate(), getDownloadListener(), input);
        input.close();
        return httpResp;
    }

    /**
	 * Подписчик загруки
	 */
    protected DownloadListener downloadListener = null;

    /**
	 * Подписчик загруки
	 * @return Подписчик загруки
	 */
    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    /**
	 * Подписчик загруки
	 * @param listener Подписчик загруки
	 */
    public void setDownloadListener(DownloadListener listener) {
        this.downloadListener = listener;
    }

    /**
	 * размер блока
	 */
    protected int blockSize = 1024 * 16;

    /**
	 * Размер блока данных
	 * @return размер блока
	 */
    public int getBlockSize() {
        return blockSize;
    }

    /**
	 * Размер блока данных
	 * @param blockSize размер блока
	 */
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public HttpResponse execute() throws IOException {
        URL _url = getUrl();
        if (_url == null) throw new IOException("url not set");
        Proxy _proxy = getProxy();
        URLConnection urlConn = _proxy == null ? _url.openConnection() : _url.openConnection(_proxy);
        HttpURLConnection httpUrlConn = urlConn instanceof HttpURLConnection ? (HttpURLConnection) urlConn : null;
        HttpsURLConnection httpsUrlConn = urlConn instanceof HttpsURLConnection ? (HttpsURLConnection) urlConn : null;
        if (httpUrlConn != null) {
            String reqMethod = getRequestMethod();
            httpUrlConn.setRequestMethod(reqMethod);
        }
        if (httpsUrlConn != null) {
            SSLSocketFactory sslF = getSSLSocketFactory();
            if (sslF != null) httpsUrlConn.setSSLSocketFactory(sslF);
        }
        setRequestHeader(urlConn);
        String contType = getContentType();
        int len = getContentLength();
        InputStream postDataStream = getContentInputStream();
        if (contType != null && postDataStream != null) urlConn.setRequestProperty(HttpHeaders.contentType, contType);
        if (len >= 0 && postDataStream != null) urlConn.setRequestProperty(HttpHeaders.contentLength, "" + len);
        urlConn.setDoInput(true);
        urlConn.setDoOutput(postDataStream != null);
        urlConn.setUseCaches(isUseCaches());
        urlConn.setConnectTimeout(getConnectTimeout());
        urlConn.setReadTimeout(getReadTimeout());
        if (getInstanceFollowRedirects() != null && httpUrlConn != null) {
            httpUrlConn.setInstanceFollowRedirects(getInstanceFollowRedirects());
        }
        if (getIfModifiedSince() != null && httpUrlConn != null) {
            httpUrlConn.setIfModifiedSince(getIfModifiedSince());
        }
        urlConn.connect();
        if (postDataStream != null) {
            OutputStream output = urlConn.getOutputStream();
            FileUtil.copyAllData(postDataStream, output);
            output.flush();
            output.close();
            postDataStream.close();
        }
        HttpResponse response = createResponse(urlConn);
        if (isDisconnect() && httpUrlConn != null) {
            httpUrlConn.disconnect();
        }
        return response;
    }

    private org.gocha.collection.Predicate<Map<String, List<String>>> downloadPredicate = null;

    /**
     Указывает предикат проверки заголовков.
     @see DownloadPredicate
     @return Предикат или null
     */
    public org.gocha.collection.Predicate<Map<String, List<String>>> getDownloadPredicate() {
        return downloadPredicate;
    }

    /**
     Указывает предикат проверки заголовков
     @see DownloadPredicate
     @param downloadPredicate Предикат или null
     */
    public void setDownloadPredicate(org.gocha.collection.Predicate<Map<String, List<String>>> downloadPredicate) {
        this.downloadPredicate = downloadPredicate;
    }
}
