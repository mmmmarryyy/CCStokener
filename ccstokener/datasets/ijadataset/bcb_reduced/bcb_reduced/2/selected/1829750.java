package newsatort.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Date;
import newsatort.Application;
import newsatort.exception.HttpException;
import newsatort.exception.HttpServerException;
import newsatort.log.LogLevel;
import newsatort.manager.AbstractManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.eclipse.swt.graphics.ImageData;

/**
 * https://grizzly.dev.java.net/
 */
public class HttpManager extends AbstractManager implements IHttpManager {

    private final HttpParams defaultParameters;

    private final SchemeRegistry supportedSchemes;

    private final ClientConnectionManager ccm;

    private final HttpClient client;

    public HttpManager() {
        super();
        defaultParameters = new BasicHttpParams();
        HttpProtocolParams.setVersion(defaultParameters, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(defaultParameters, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(defaultParameters, true);
        HttpProtocolParams.setUserAgent(defaultParameters, "Test");
        HttpConnectionParams.setConnectionTimeout(defaultParameters, 60000);
        HttpConnectionParams.setSoTimeout(defaultParameters, 60000);
        final SocketFactory sf = PlainSocketFactory.getSocketFactory();
        supportedSchemes = new SchemeRegistry();
        supportedSchemes.register(new Scheme(HttpHost.DEFAULT_SCHEME_NAME, sf, 80));
        ccm = new ThreadSafeClientConnManager(defaultParameters, supportedSchemes);
        client = new DefaultHttpClient(ccm, defaultParameters);
    }

    public String getPage(URI uri) throws HttpException {
        String page = null;
        logManager.addLog(LogLevel.VERBOSE, "URI: " + uri);
        HttpStream stream = null;
        try {
            stream = getStream(uri);
            if (stream != null) {
                String charset = EntityUtils.getContentCharSet(stream.getEntity());
                if (charset == null) {
                }
                Reader reader = new BufferedReader(new InputStreamReader(stream.getStream(), stream.getCharset()));
                StringBuilder stringBuilder = new StringBuilder((int) stream.getContentLength());
                try {
                    char[] tmp = new char[4096];
                    int l;
                    while ((l = reader.read(tmp)) != -1) {
                        stringBuilder.append(tmp, 0, l);
                    }
                } finally {
                    reader.close();
                }
                page = stringBuilder.toString();
            }
        } catch (IllegalStateException exception) {
            throw new HttpException(exception);
        } catch (IOException exception) {
            throw new HttpException(exception);
        } finally {
            closeStream(stream);
        }
        logManager.addLog(LogLevel.VERBOSE, "page pour l'URI: " + uri + " - " + page.length() + " octets");
        return page;
    }

    public ImageData getImage(URI uri) throws HttpException {
        ImageData image = null;
        logManager.addLog(LogLevel.VERBOSE, "URI: " + uri);
        HttpStream stream = null;
        try {
            stream = getStream(uri);
            if (stream != null) {
                image = new ImageData(stream.getStream());
            }
        } catch (IllegalStateException exception) {
            throw new HttpException(exception);
        } finally {
            closeStream(stream);
        }
        logManager.addLog(LogLevel.VERBOSE, "fini pour l'URI: " + uri);
        return image;
    }

    public HttpStream getStream(URI uri, Date lastModifiedDate, String eTag, boolean deltaEncoding) throws HttpException {
        HttpStream stream = null;
        logManager.addLog(LogLevel.VERBOSE, "URI: " + uri);
        final HttpHost target = new HttpHost(uri.getHost(), uri.getPort(), "http");
        try {
            final HttpRequest req = new HttpGet(uri);
            req.setHeader("Accept-Encoding", "gzip");
            if (eTag != null) req.setHeader("If-None-Match", eTag);
            if (lastModifiedDate != null) req.setHeader("If-Modified-Since", DateUtils.formatDate(lastModifiedDate));
            if (deltaEncoding) req.setHeader("A-IM", "feed");
            final HttpResponse response = client.execute(target, req);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) throw new HttpServerException(response.getStatusLine());
            stream = new HttpStream(response);
        } catch (ClientProtocolException exception) {
            throw new HttpException(exception);
        } catch (IOException exception) {
            throw new HttpException(exception);
        }
        logManager.addLog(LogLevel.VERBOSE, "fini pour l'URI: " + uri);
        return stream;
    }

    public HttpStream getStream(URI uri) throws HttpException {
        return getStream(uri, null, null, false);
    }

    private void closeStream(HttpStream stream) throws HttpException {
        if (stream != null) stream.close();
    }

    public static void main(String[] args) throws Exception {
        new Application();
        final HttpManager httpManager = new HttpManager();
        httpManager.start();
        String page = httpManager.getPage(new URI("http://www.pays-ancenis.com/fileadmin/template/compa/export_actualites_5091.xml"));
        System.out.println();
        System.out.println(page);
        System.exit(0);
    }
}
