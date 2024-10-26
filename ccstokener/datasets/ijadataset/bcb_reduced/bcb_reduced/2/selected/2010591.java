package org.apache.ws.commons.tcpmon.core.engine;

import java.io.UnsupportedEncodingException;
import javax.activation.MimeType;
import junit.framework.TestCase;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.ws.commons.tcpmon.core.filter.ReplaceFilter;
import org.apache.ws.commons.tcpmon.core.filter.StreamFilter;
import org.apache.ws.commons.tcpmon.core.filter.mime.ContentFilterFactory;
import org.mortbay.jetty.Server;

public class InterceptorTest extends TestCase {

    private void testWithContentFilter(boolean chunked) throws Exception {
        Server server = TestUtil.createServer(5555);
        server.start();
        InterceptorConfigurationBuilder configBuilder = new InterceptorConfigurationBuilder();
        configBuilder.setTargetHost("localhost");
        configBuilder.setTargetPort(5555);
        configBuilder.setListenPort(8000);
        configBuilder.setRequestContentFilterFactory(new ContentFilterFactory() {

            public StreamFilter[] getContentFilterChain(MimeType contentType) {
                try {
                    return new StreamFilter[] { new ReplaceFilter("pattern", "replacement", "ascii") };
                } catch (UnsupportedEncodingException ex) {
                    return null;
                }
            }
        });
        InterceptorConfiguration config = configBuilder.build();
        Interceptor interceptor = new Interceptor(config, new Dump(System.out));
        HttpClient client = TestUtil.createClient(config);
        HttpPost request = new HttpPost(TestUtil.getBaseUri(config, server) + "/echo");
        request.setEntity(TestUtil.createStringEntity("test-pattern-test", "utf-8", chunked));
        HttpResponse response = client.execute(request);
        assertEquals("test-replacement-test", TestUtil.getResponseAsString(response));
        interceptor.halt();
        server.stop();
    }

    public void testWithContentFilterNotChunked() throws Exception {
        testWithContentFilter(false);
    }

    public void testWithContentFilterChunked() throws Exception {
        testWithContentFilter(true);
    }
}
