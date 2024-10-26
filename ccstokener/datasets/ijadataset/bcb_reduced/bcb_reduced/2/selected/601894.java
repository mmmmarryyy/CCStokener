package org.tolven.core.bean;

import java.io.IOException;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.Scheme;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpParams;
import org.apache.http.impl.io.PlainSocketFactory;
import org.apache.http.io.SocketFactory;
import org.apache.http.message.HttpGet;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.tolven.logging.TolvenLogger;

public class TemplateGen {

    HttpRequestExecutor httpexecutor = null;

    HttpClientConnection conn = null;

    HttpHost host = null;

    String contextPath = null;

    public TemplateGen(String hostName, int port, String contextPath) {
        SocketFactory socketfactory = PlainSocketFactory.getSocketFactory();
        Scheme.registerScheme("http", new Scheme("http", socketfactory, 80));
        HttpParams params = new DefaultHttpParams(null);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Jakarta-HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);
        httpexecutor = new HttpRequestExecutor();
        httpexecutor.setParams(params);
        httpexecutor.addInterceptor(new RequestContent());
        httpexecutor.addInterceptor(new RequestTargetHost());
        httpexecutor.addInterceptor(new RequestConnControl());
        httpexecutor.addInterceptor(new RequestUserAgent());
        httpexecutor.addInterceptor(new RequestExpectContinue());
        host = new HttpHost(hostName, port);
        this.contextPath = contextPath;
    }

    public void connect() {
        conn = new DefaultHttpClientConnection(host);
    }

    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (IOException e) {
        }
    }

    /**
	 * Connect to a page and return the result as 
	 * @param target
	 * @return
	 * @throws IOException
	 * @throws HttpException
	 */
    public String expandTemplate(String target) throws IOException, HttpException {
        connect();
        try {
            HttpGet request = new HttpGet(contextPath + target);
            HttpResponse response = httpexecutor.execute(request, conn);
            TolvenLogger.info("Response: " + response.getStatusLine(), TemplateGen.class);
            disconnect();
            return EntityUtils.toString(response.getEntity());
        } finally {
            disconnect();
        }
    }
}
