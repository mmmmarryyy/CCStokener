package com.leemba.monitor.server.sensor.active.http;

import com.leemba.monitor.server.dao.config.Config;
import com.leemba.monitor.server.objects.LifecycleListener;
import com.leemba.monitor.server.sensor.TrustingManager;
import com.leemba.monitor.util.StopWatch;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.log4j.Logger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.mina.util.Base64;

/**
 *
 * @author mrjohnson
 */
public class HttpClient implements LifecycleListener {

    private static final transient Logger log = Logger.getLogger(HttpClient.class);

    private static final int DEFAULT_CONNECTION_TIMEOUT = 10;

    private static final int MAX_BODY = 102400;

    private static final String SSL_CONTEXT_NAMES[] = { "TLS", "Default", "SSL" };

    public static final HttpClient instance = new HttpClient();

    private HttpClient() {
    }

    public void check(final SensorHttpRequest request) throws UnknownHostException {
        check(request, null);
    }

    public void check(final SensorHttpRequest request, final HttpClientListener listener) throws UnknownHostException {
        final URL url = request.getUrl();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, DEFAULT_CONNECTION_TIMEOUT * 1000);
        HttpConnectionParams.setConnectionTimeout(params, DEFAULT_CONNECTION_TIMEOUT * 1000);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (compatible; Leemba/" + Config.VERSION + "; +http://www.leemba.com)");
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setHttpElementCharset(params, "UTF-8");
        DefaultHttpClient client = new DefaultHttpClient(params);
        SSLSocketFactory sf = null;
        for (String contextName : SSL_CONTEXT_NAMES) {
            try {
                SSLContext sslContext = SSLContext.getInstance(contextName);
                sslContext.init(null, new TrustManager[] { new TrustingManager() }, null);
                sf = new SSLSocketFactory(sslContext);
                break;
            } catch (NoSuchAlgorithmException e) {
                log.debug("SSLContext algorithm not available: " + contextName);
            } catch (Exception e) {
                log.debug("SSLContext can't be initialized: " + contextName, e);
            }
        }
        if (sf != null) {
            sf.setHostnameVerifier(new DummyX509HostnameVerifier());
            client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sf, 443));
        } else log.error("No valid SSLContext found for https");
        HttpUriRequest req = null;
        if (request.method.equalsIgnoreCase("HEAD")) req = new HttpHead(url.toString()); else if (request.method.equalsIgnoreCase("POST")) req = new HttpPost(url.toString()); else req = new HttpGet(url.toString());
        if (request.getBasicAuthUsername() != null) {
            client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(request.getBasicAuthUsername(), request.getBasicAuthPassword()));
        }
        req.addHeader("Connection", "close");
        final StopWatch watch = new StopWatch().start();
        try {
            HttpResponse result = client.execute(req);
            ClientHttpResponse cr = new ClientHttpResponse();
            cr.setDuration((int) watch.stop());
            cr.setStatus(result.getStatusLine().getStatusCode());
            String charset = "UTF-8";
            HttpEntity entity = result.getEntity();
            if (entity != null && entity.getContentEncoding() != null) entity.getContentEncoding().getValue();
            cr.setMessage(result.getStatusLine().getStatusCode() + " - " + result.getStatusLine().getReasonPhrase());
            if (entity != null) {
                InputStream in = result.getEntity().getContent();
                byte buf[] = new byte[2048];
                int count = 0;
                StringWriter writer = new StringWriter();
                for (int total = 0; (count = in.read(buf)) > 0 && total < MAX_BODY; total += count) writer.write(new String(buf, 0, count, charset));
                in.close();
                String body = writer.toString();
                cr.setBody(body.replace("\r\n", "\n"));
            }
            listener.responseReceived(cr);
        } catch (Throwable t) {
            listener.exceptionCaught(t);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Override
    public LifecycleListener shutdown() {
        return this;
    }

    @Override
    public LifecycleListener startup() {
        return this;
    }

    private static class DummyX509HostnameVerifier extends AbstractVerifier {

        @Override
        public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            try {
                verify(host, cns, subjectAlts, false);
            } catch (SSLException e) {
                log.error("Invalid SSL certificate for " + host + ": " + e.getMessage());
            }
        }

        @Override
        public final String toString() {
            return "DUMMY_VERIFIER";
        }
    }
}
