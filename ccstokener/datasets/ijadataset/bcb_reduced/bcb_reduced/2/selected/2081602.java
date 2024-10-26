package com.k42b3.aletheia.http;

import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import com.k42b3.aletheia.Aletheia;
import com.k42b3.aletheia.filter.CallbackInterface;
import com.k42b3.aletheia.filter.ResponseFilterAbstract;

/**
 * Http
 *
 * @author     Christoph Kappestein <k42b3.x@gmail.com>
 * @license    http://www.gnu.org/licenses/gpl.html GPLv3
 * @link       http://code.google.com/p/delta-quadrant
 * @version    $Revision: 4 $
 */
public class Http implements Runnable {

    public static final String newLine = "\r\n";

    public static final String type = "HTTP/1.1";

    public static final String method = "GET";

    private Request request;

    private Response response;

    private CallbackInterface callback;

    private ArrayList<ResponseFilterAbstract> responseFilter = new ArrayList<ResponseFilterAbstract>();

    private Logger logger = Logger.getLogger("com.k42b3.aletheia");

    private HttpParams params;

    private HttpHost host;

    private HttpProcessor httpproc;

    private HttpRequestExecutor httpexecutor;

    private HttpContext context;

    private ConnectionReuseStrategy connStrategy;

    private DefaultHttpClientConnection conn;

    public Http(String rawUrl, Request request, CallbackInterface callback) throws Exception {
        URL url;
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            url = new URL(rawUrl);
        } else {
            url = new URL("http://" + rawUrl);
        }
        this.request = request;
        this.callback = callback;
        params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Aletheia " + Aletheia.VERSION);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpRequestInterceptor[] interceptors = { new RequestContent(), new RequestTargetHost(), new RequestConnControl(), new RequestUserAgent(), new RequestExpectContinue() };
        httpproc = new ImmutableHttpProcessor(interceptors);
        httpexecutor = new HttpRequestExecutor();
        context = new BasicHttpContext(null);
        host = new HttpHost(url.getHost(), url.getPort() == -1 ? 80 : url.getPort());
        conn = new DefaultHttpClientConnection();
        connStrategy = new DefaultConnectionReuseStrategy();
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
    }

    public void addResponseFilter(ResponseFilterAbstract filter) {
        this.responseFilter.add(filter);
    }

    public void run() {
        try {
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket, params);
            BasicHttpRequest request;
            if (!this.request.getBody().isEmpty()) {
                request = new BasicHttpEntityEnclosingRequest(this.request.getMethod(), this.request.getPath());
            } else {
                request = new BasicHttpRequest(this.request.getMethod(), this.request.getPath());
            }
            Map<String, String> headers = this.request.getHeader();
            Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pairs = it.next();
                request.addHeader(pairs.getKey(), pairs.getValue());
            }
            if (request instanceof BasicHttpEntityEnclosingRequest) {
                StringEntity body = new StringEntity(this.request.getBody());
                ((BasicHttpEntityEnclosingRequest) request).setEntity(body);
            }
            logger.info("> " + request.getRequestLine().getUri());
            request.setParams(params);
            httpexecutor.preProcess(request, httpproc, context);
            HttpResponse response = httpexecutor.execute(request, conn, context);
            response.setParams(params);
            httpexecutor.postProcess(response, httpproc, context);
            logger.info("< " + response.getStatusLine());
            this.response = new Response(response);
            for (int i = 0; i < this.responseFilter.size(); i++) {
                try {
                    this.responseFilter.get(i).exec(this.response);
                } catch (Exception e) {
                    Aletheia.handleException(e);
                }
            }
            callback.response(this.response.toString());
        } catch (Exception e) {
            Aletheia.handleException(e);
        } finally {
            try {
                conn.close();
            } catch (Exception e) {
                Aletheia.handleException(e);
            }
        }
    }

    public Request getRequest() {
        return this.request;
    }

    public Response getResponse() {
        return this.response;
    }
}
