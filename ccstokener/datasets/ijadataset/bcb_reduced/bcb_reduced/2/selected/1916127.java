package org.echarts.servlet.sip.examples.async;

import java.util.concurrent.*;
import java.io.InputStream;
import javax.servlet.ServletConfig;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.echarts.monitor.DebugEvent;
import org.echarts.monitor.ErrorEvent;
import org.echarts.monitor.InfoEvent;
import org.echarts.monitor.WarnEvent;
import org.echarts.servlet.sip.EChartsMachineToJava;
import org.echarts.servlet.sip.Monitor;
import org.echarts.servlet.sip.NonSipPort;
import org.echarts.servlet.sip.utilities.FileBasedConfiguration;

public class AsyncSampleFSMToJavaImpl extends EChartsMachineToJava implements AsyncSampleFSMToJava {

    public static final String rcsid = "$Id: AsyncSampleFSMToJavaImpl.java 2062 2012-01-24 21:47:54Z plisenhour $ $Name:  $";

    private static FileBasedConfiguration config;

    private static String requestURL;

    private static ExecutorService pool;

    private static DefaultHttpClient httpClient;

    static void servletInit(ServletConfig sc) throws Exception {
        config = FileBasedConfiguration.getInstance();
        requestURL = config.getParameter("asyncRequestURL");
        pool = Executors.newFixedThreadPool(10);
        httpClient = new DefaultHttpClient();
        httpClient.setReuseStrategy(new NoConnectionReuseStrategy());
    }

    static void destroy() throws Exception {
    }

    public AsyncSampleFSMToJavaImpl() throws Exception {
        super();
    }

    public void submitRequest(NonSipPort nonSipPort) {
        pool.execute(new RequestRunnable(nonSipPort));
    }

    class RequestRunnable implements Runnable {

        NonSipPort nonSipPort;

        public RequestRunnable(NonSipPort nonSipPort) {
            this.nonSipPort = nonSipPort;
        }

        public void run() {
            try {
                putEvent(new DebugEvent("about to place HTTP request"));
                HttpGet req = new HttpGet(requestURL);
                req.addHeader("Connection", "close");
                HttpResponse httpResponse = httpClient.execute(req);
                putEvent(new DebugEvent("got response to HTTP request"));
                nonSipPort.input(new Integer(httpResponse.getStatusLine().getStatusCode()));
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    InputStream in = entity.getContent();
                    if (in != null) in.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
