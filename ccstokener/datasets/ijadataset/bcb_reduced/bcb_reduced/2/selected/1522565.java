package hacathon.android.travel;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

/**
 * プロジェクト共通で使用するHTTPクライアント。<br/>
 * 共通の初期設定を行う。
 */
public class CustomHttpClient {

    public static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = HttpVersion.HTTP_1_1;

    public static final String DEFAULT_CONTENT_CHARSET = HTTP.UTF_8;

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (PDA; NF34PPC/1.0; like Gecko) NetFront/3.4";

    public static final int DEFAULT_MAX_RETRY = 3;

    public final DefaultHttpClient mClient;

    public CustomHttpClient() {
        final HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, DEFAULT_PROTOCOL_VERSION);
        HttpProtocolParams.setContentCharset(params, DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUserAgent(params, DEFAULT_USER_AGENT);
        final SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
        mClient = new DefaultHttpClient(manager, params);
        mClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(DEFAULT_MAX_RETRY, false));
    }

    public HttpEntity execute(final HttpRequestBase request) throws IOException, ClientProtocolException {
        final HttpResponse response = mClient.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK | statusCode == HttpStatus.SC_CREATED) {
            return response.getEntity();
        }
        return null;
    }
}
