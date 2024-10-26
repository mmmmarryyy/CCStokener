package net.myfigurecollection.android.webservices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;
import android.text.format.Time;
import android.util.Log;

/**
 * Subclass of the Apache {@link DefaultHttpClient} that is configured with
 * reasonable default settings and registered schemes for Android, and
 * also lets the user add {@link HttpRequestInterceptor} classes.
 * Don't create this directly, use the {@link #newInstance} factory method.
 * 
 * <p>
 * This client processes cookies but does not retain them by default. To retain cookies, simply add a cookie store to the HttpContext:
 * </p>
 * 
 * <pre>
 * context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
 * </pre>
 */
public final class AndroidHttpClient implements HttpClient {

    public static final int TIMEOUT = 10 * 1000;

    public static long DEFAULT_SYNC_MIN_GZIP_BYTES = 256;

    private static final String TAG = "AndroidHttpClient";

    /** Interceptor throws an exception if the executing thread is blocked */
    private static final HttpRequestInterceptor sThreadCheckInterceptor = new HttpRequestInterceptor() {

        @Override
        public void process(HttpRequest request, HttpContext context) {
            if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
                throw new RuntimeException("This thread forbids HTTP requests");
            }
        }
    };

    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     * 
     * @param userAgent
     *            to report in your HTTP requests
     * @param context
     *            to use for caching SSL sessions (may be null for no caching)
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static AndroidHttpClient newInstance(String userAgent, Context context) {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setConnectionTimeout(params, AndroidHttpClient.TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, AndroidHttpClient.TIMEOUT);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpClientParams.setRedirecting(params, false);
        HttpProtocolParams.setUserAgent(params, userAgent);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);
        return new AndroidHttpClient(manager, params);
    }

    /**
     * Create a new HttpClient with reasonable defaults (which you can update).
     * 
     * @param userAgent
     *            to report in your HTTP requests.
     * @return AndroidHttpClient for you to use for all your requests.
     */
    public static AndroidHttpClient newInstance(String userAgent) {
        return newInstance(userAgent, null);
    }

    private final HttpClient delegate;

    private RuntimeException mLeakedException = new IllegalStateException("AndroidHttpClient created and never closed");

    private AndroidHttpClient(ClientConnectionManager ccm, HttpParams params) {
        this.delegate = new DefaultHttpClient(ccm, params) {

            @Override
            protected BasicHttpProcessor createHttpProcessor() {
                BasicHttpProcessor processor = super.createHttpProcessor();
                processor.addRequestInterceptor(sThreadCheckInterceptor);
                processor.addRequestInterceptor(new CurlLogger());
                return processor;
            }

            @Override
            protected HttpContext createHttpContext() {
                HttpContext context = new BasicHttpContext();
                context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
                context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
                context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
                return context;
            }
        };
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mLeakedException != null) {
            Log.e(TAG, "Leak found", mLeakedException);
            mLeakedException = null;
        }
    }

    /**
     * Modifies a request to indicate to the server that we would like a
     * gzipped response. (Uses the "Accept-Encoding" HTTP header.)
     * 
     * @param request
     *            the request to modify
     * @see #getUngzippedContent
     */
    public static void modifyRequestToAcceptGzipResponse(HttpRequest request) {
        request.addHeader("Accept-Encoding", "gzip");
    }

    /**
     * Gets the input stream from a response entity. If the entity is gzipped
     * then this will get a stream over the uncompressed data.
     * 
     * @param entity
     *            the entity whose content should be read
     * @return the input stream to read from
     * @throws IOException
     */
    public static InputStream getUngzippedContent(HttpEntity entity) throws IOException {
        InputStream responseStream = entity.getContent();
        if (responseStream == null) return responseStream;
        Header header = entity.getContentEncoding();
        if (header == null) return responseStream;
        String contentEncoding = header.getValue();
        if (contentEncoding == null) return responseStream;
        if (contentEncoding.contains("gzip")) responseStream = new GZIPInputStream(responseStream);
        return responseStream;
    }

    /**
     * Release resources associated with this client. You must call this,
     * or significant resources (sockets and memory) may be leaked.
     */
    public void close() {
        if (mLeakedException != null) {
            getConnectionManager().shutdown();
            mLeakedException = null;
        }
    }

    @Override
    public HttpParams getParams() {
        return delegate.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return delegate.getConnectionManager();
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return delegate.execute(request);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
        return delegate.execute(request, context);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
        return delegate.execute(target, request);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return delegate.execute(target, request, context);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return delegate.execute(request, responseHandler);
    }

    @Override
    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return delegate.execute(request, responseHandler, context);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return delegate.execute(target, request, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return delegate.execute(target, request, responseHandler, context);
    }

    /**
     * Compress data to send to server.
     * Creates a Http Entity holding the gzipped data.
     * The data will not be compressed if it is too short.
     * 
     * @param data
     *            The bytes to compress
     * @return Entity holding the data
     */
    public static AbstractHttpEntity getCompressedEntity(byte data[], ContentResolver resolver) throws IOException {
        AbstractHttpEntity entity;
        if (data.length < getMinGzipSize(resolver)) {
            entity = new ByteArrayEntity(data);
        } else {
            ByteArrayOutputStream arr = new ByteArrayOutputStream();
            OutputStream zipper = new GZIPOutputStream(arr);
            zipper.write(data);
            zipper.close();
            entity = new ByteArrayEntity(arr.toByteArray());
            entity.setContentEncoding("gzip");
        }
        return entity;
    }

    /**
     * Retrieves the minimum size for compressing data.
     * Shorter data will not be compressed.
     */
    public static long getMinGzipSize(ContentResolver resolver) {
        return DEFAULT_SYNC_MIN_GZIP_BYTES;
    }

    /**
     * Logging tag and level.
     */
    private static class LoggingConfiguration {

        private final String tag;

        private final int level;

        private LoggingConfiguration(String tag, int level) {
            this.tag = tag;
            this.level = level;
        }

        /**
         * Returns true if logging is turned on for this configuration.
         */
        private boolean isLoggable() {
            return Log.isLoggable(tag, level);
        }

        /**
         * Prints a message using this configuration.
         */
        private void println(String message) {
            Log.println(level, tag, message);
        }
    }

    /** cURL logging configuration. */
    private volatile LoggingConfiguration curlConfiguration;

    /**
     * Enables cURL request logging for this client.
     * 
     * @param name
     *            to log messages with
     * @param level
     *            at which to log messages (see {@link android.util.Log})
     */
    public void enableCurlLogging(String name, int level) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (level < Log.VERBOSE || level > Log.ASSERT) {
            throw new IllegalArgumentException("Level is out of range [" + Log.VERBOSE + ".." + Log.ASSERT + "]");
        }
        curlConfiguration = new LoggingConfiguration(name, level);
    }

    /**
     * Disables cURL logging for this client.
     */
    public void disableCurlLogging() {
        curlConfiguration = null;
    }

    /**
     * Logs cURL commands equivalent to requests.
     */
    private class CurlLogger implements HttpRequestInterceptor {

        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            LoggingConfiguration configuration = curlConfiguration;
            if (configuration != null && configuration.isLoggable() && request instanceof HttpUriRequest) {
                configuration.println(toCurl((HttpUriRequest) request, false));
            }
        }
    }

    /**
     * Generates a cURL command equivalent to the given request.
     */
    private static String toCurl(HttpUriRequest request, boolean logAuthToken) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("curl ");
        for (Header header : request.getAllHeaders()) {
            if (!logAuthToken && (header.getName().equals("Authorization") || header.getName().equals("Cookie"))) {
                continue;
            }
            builder.append("--header \"");
            builder.append(header.toString().trim());
            builder.append("\" ");
        }
        URI uri = request.getURI();
        if (request instanceof RequestWrapper) {
            HttpRequest original = ((RequestWrapper) request).getOriginal();
            if (original instanceof HttpUriRequest) {
                uri = ((HttpUriRequest) original).getURI();
            }
        }
        builder.append("\"");
        builder.append(uri);
        builder.append("\"");
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
            HttpEntity entity = entityRequest.getEntity();
            if (entity != null && entity.isRepeatable()) {
                if (entity.getContentLength() < 1024) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    entity.writeTo(stream);
                    String entityString = stream.toString();
                    builder.append(" --data-ascii \"").append(entityString).append("\"");
                } else {
                    builder.append(" [TOO MUCH DATA TO INCLUDE]");
                }
            }
        }
        return builder.toString();
    }

    /**
     * Returns the date of the given HTTP date string. This method can identify
     * and parse the date formats emitted by common HTTP servers, such as
     * <a href="http://www.ietf.org/rfc/rfc0822.txt">RFC 822</a>,
     * <a href="http://www.ietf.org/rfc/rfc0850.txt">RFC 850</a>,
     * <a href="http://www.ietf.org/rfc/rfc1036.txt">RFC 1036</a>,
     * <a href="http://www.ietf.org/rfc/rfc1123.txt">RFC 1123</a> and
     * <a href="http://www.opengroup.org/onlinepubs/007908799/xsh/asctime.html">ANSI
     * C's asctime()</a>.
     * 
     * @return the number of milliseconds since Jan. 1, 1970, midnight GMT.
     * @throws IllegalArgumentException
     *             if {@code dateString} is not a date or
     *             of an unsupported format.
     */
    public static long parseDate(String timeString) {
        int date = 1;
        int month = Calendar.JANUARY;
        int year = 1970;
        TimeOfDay timeOfDay;
        Matcher rfcMatcher = HTTP_DATE_RFC_PATTERN.matcher(timeString);
        if (rfcMatcher.find()) {
            date = getDate(rfcMatcher.group(1));
            month = getMonth(rfcMatcher.group(2));
            year = getYear(rfcMatcher.group(3));
            timeOfDay = getTime(rfcMatcher.group(4));
        } else {
            Matcher ansicMatcher = HTTP_DATE_ANSIC_PATTERN.matcher(timeString);
            if (ansicMatcher.find()) {
                month = getMonth(ansicMatcher.group(1));
                date = getDate(ansicMatcher.group(2));
                timeOfDay = getTime(ansicMatcher.group(3));
                year = getYear(ansicMatcher.group(4));
            } else {
                throw new IllegalArgumentException();
            }
        }
        if (year >= 2038) {
            year = 2038;
            month = Calendar.JANUARY;
            date = 1;
        }
        Time time = new Time(Time.TIMEZONE_UTC);
        time.set(timeOfDay.second, timeOfDay.minute, timeOfDay.hour, date, month, year);
        return time.toMillis(false);
    }

    private static int getDate(String dateString) {
        if (dateString.length() == 2) {
            return (dateString.charAt(0) - '0') * 10 + (dateString.charAt(1) - '0');
        }
        return (dateString.charAt(0) - '0');
    }

    private static int getMonth(String monthString) {
        int hash = Character.toLowerCase(monthString.charAt(0)) + Character.toLowerCase(monthString.charAt(1)) + Character.toLowerCase(monthString.charAt(2)) - 3 * 'a';
        switch(hash) {
            case 22:
                return Calendar.JANUARY;
            case 10:
                return Calendar.FEBRUARY;
            case 29:
                return Calendar.MARCH;
            case 32:
                return Calendar.APRIL;
            case 36:
                return Calendar.MAY;
            case 42:
                return Calendar.JUNE;
            case 40:
                return Calendar.JULY;
            case 26:
                return Calendar.AUGUST;
            case 37:
                return Calendar.SEPTEMBER;
            case 35:
                return Calendar.OCTOBER;
            case 48:
                return Calendar.NOVEMBER;
            case 9:
                return Calendar.DECEMBER;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static int getYear(String yearString) {
        if (yearString.length() == 2) {
            int year = (yearString.charAt(0) - '0') * 10 + (yearString.charAt(1) - '0');
            if (year >= 70) {
                return year + 1900;
            }
            return year + 2000;
        } else if (yearString.length() == 3) {
            int year = (yearString.charAt(0) - '0') * 100 + (yearString.charAt(1) - '0') * 10 + (yearString.charAt(2) - '0');
            return year + 1900;
        } else if (yearString.length() == 4) {
            return (yearString.charAt(0) - '0') * 1000 + (yearString.charAt(1) - '0') * 100 + (yearString.charAt(2) - '0') * 10 + (yearString.charAt(3) - '0');
        } else {
            return 1970;
        }
    }

    private static TimeOfDay getTime(String timeString) {
        int i = 0;
        int hour = timeString.charAt(i++) - '0';
        if (timeString.charAt(i) != ':') hour = hour * 10 + (timeString.charAt(i++) - '0');
        i++;
        int minute = (timeString.charAt(i++) - '0') * 10 + (timeString.charAt(i++) - '0');
        i++;
        int second = (timeString.charAt(i++) - '0') * 10 + (timeString.charAt(i++) - '0');
        return new TimeOfDay(hour, minute, second);
    }

    private static class TimeOfDay {

        TimeOfDay(int h, int m, int s) {
            this.hour = h;
            this.minute = m;
            this.second = s;
        }

        int hour;

        int minute;

        int second;
    }

    private static final String HTTP_DATE_RFC_REGEXP = "([0-9]{1,2})[- ]([A-Za-z]{3,9})[- ]([0-9]{2,4})[ ]" + "([0-9]{1,2}:[0-9][0-9]:[0-9][0-9])";

    private static final String HTTP_DATE_ANSIC_REGEXP = "[ ]([A-Za-z]{3,9})[ ]+([0-9]{1,2})[ ]" + "([0-9]{1,2}:[0-9][0-9]:[0-9][0-9])[ ]([0-9]{2,4})";

    private static final Pattern HTTP_DATE_RFC_PATTERN = Pattern.compile(HTTP_DATE_RFC_REGEXP);

    private static final Pattern HTTP_DATE_ANSIC_PATTERN = Pattern.compile(HTTP_DATE_ANSIC_REGEXP);
}
