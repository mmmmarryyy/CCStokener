package au.edu.diasb.annotation.danno.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import au.edu.diasb.annotation.danno.protocol.DannoClient;
import au.edu.diasb.annotation.danno.protocol.RDFResponse;

/**
 * This mock client fakes a GET, returning the Response that has 
 * previously been registered using
 * one of the setResponse methods. 
 * 
 * @author scrawley
 *
 */
public class MockAnnoteaClient implements DannoClient {

    private String content;

    private Map<String, String> contentMap;

    private String capturedTarget;

    private String capturedContent;

    private String capturedMethod;

    public MockAnnoteaClient(String content) {
        this.content = content;
        this.contentMap = null;
    }

    public MockAnnoteaClient(Map<String, String> contentMap) {
        this.content = null;
        this.contentMap = contentMap;
    }

    protected String getCapturedTarget() {
        return capturedTarget;
    }

    protected String getCapturedContent() {
        return capturedContent;
    }

    protected String getCapturedMethod() {
        return capturedMethod;
    }

    @Override
    public RDFResponse executeRDF(HttpUriRequest request) throws IOException, ClientProtocolException {
        capturedMethod = request.getMethod();
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) request;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            r.getEntity().writeTo(baos);
            capturedContent = baos.toString("UTF-8");
        } else {
            capturedContent = null;
        }
        String replyRdf = (content != null) ? content : contentMap.get(request.getURI().toString());
        return new RDFResponse(new ByteArrayInputStream(replyRdf.getBytes("UTF-8")));
    }

    @Override
    public String executeHTML(HttpUriRequest request) throws IOException, ClientProtocolException {
        capturedMethod = request.getMethod();
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest r = (HttpEntityEnclosingRequest) request;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            r.getEntity().writeTo(baos);
            capturedContent = baos.toString("UTF-8");
        } else {
            capturedContent = null;
        }
        return (content != null) ? content : contentMap.get(request.getURI().toString());
    }

    @Override
    public boolean executeIgnore(HttpUriRequest request) throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException("executeIgnore");
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        throw new UnsupportedOperationException("execute");
    }

    @Override
    public HttpResponse getLastResponse() {
        return new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpServletResponse.SC_OK, "OK");
    }

    @Override
    public boolean isOK() {
        return true;
    }

    @Override
    public void close() {
    }
}
