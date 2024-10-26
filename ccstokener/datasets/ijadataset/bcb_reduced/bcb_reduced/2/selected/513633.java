package org.middleheaven.process.web.client.apache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.middleheaven.io.ManagedIOException;
import org.middleheaven.logging.Log;
import org.middleheaven.process.Attribute;
import org.middleheaven.process.ContextScope;
import org.middleheaven.process.ContextScopeStrategy;
import org.middleheaven.process.web.BrowserInfo;
import org.middleheaven.process.web.HttpEntry;
import org.middleheaven.process.web.HttpProcessException;
import org.middleheaven.process.web.HttpProtocolException;
import org.middleheaven.process.web.HttpUserAgent;
import org.middleheaven.process.web.UserAgent;
import org.middleheaven.process.web.client.HttpClient;
import org.middleheaven.process.web.client.HttpClientRequest;
import org.middleheaven.process.web.client.HttpClientResponse;
import org.middleheaven.util.OperatingSystemInfo;
import org.middleheaven.util.Version;

class HttpClientAdapter implements HttpClient {

    private DefaultHttpClient apacheClient;

    private UserAgent userAgent;

    public HttpClientAdapter(DefaultHttpClient apacheClient) {
        this.apacheClient = apacheClient;
        this.userAgent = new HttpUserAgent(BrowserInfo.browser("MiddleHeaven Client", "ApacheHttpClient", Version.valueOf(1, 0)), OperatingSystemInfo.local());
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public HttpClientResponse process(HttpClientRequest request) throws HttpProcessException {
        switch(request.getMethod()) {
            case GET:
                return processGet(request);
            case POST:
                return processPost(request);
            case DELETE:
                return processDelete(request);
            case HEAD:
                return processHead(request);
            case OPTIONS:
                return processOptions(request);
            case PUT:
                return processPut(request);
            case TRACE:
                return processTrace(request);
            case CONNECT:
            case UNKOWN:
            default:
                throw new IllegalArgumentException("Cannot handle " + request.getMethod() + " http method. It is not supported.");
        }
    }

    /**
	 * @param request
	 * @param httpGet
	 * @return
	 */
    private HttpClientResponse processGet(HttpClientRequest request) throws HttpProcessException {
        StringBuilder builder = new StringBuilder(request.getRequestUrl().toString());
        ContextScopeStrategy attributes = request.getAttributes().getScopeAttributeContext(ContextScope.PARAMETERS);
        List<NameValuePair> qparams = new ArrayList<NameValuePair>(attributes.size());
        for (Attribute attribute : attributes) {
            qparams.add(new BasicNameValuePair(attribute.getName(), attribute.getValue(String.class)));
        }
        HttpGet get;
        if (!qparams.isEmpty()) {
            builder.append("?").append(URLEncodedUtils.format(qparams, "UTF-8"));
        }
        get = new HttpGet(builder.toString());
        addHeaders(request, get);
        return execute(get);
    }

    private void addHeaders(HttpClientRequest request, HttpRequestBase baseRequest) {
        ContextScopeStrategy attributes = request.getAttributes().getScopeAttributeContext(ContextScope.REQUEST_HEADERS);
        for (Attribute attribute : attributes) {
            baseRequest.addHeader(attribute.getName(), attribute.getValue(String.class));
        }
    }

    private HttpClientResponse processPost(HttpClientRequest request) throws HttpProcessException {
        HttpPost post = new HttpPost(request.getRequestUrl().toString());
        addHeaders(request, post);
        HttpEntry entry = request.getEntry();
        if (entry != null) {
            post.setEntity(new HttpEntryEntityAdapter(entry));
        } else {
            Log.onBookFor(this.getClass()).warn("No entity to post to {0}", request.getRequestUrl().toString());
        }
        return execute(post);
    }

    private HttpClientResponse processDelete(HttpClientRequest request) throws HttpProcessException {
        HttpDelete delete = new HttpDelete(request.getRequestUrl().toString());
        addHeaders(request, delete);
        return execute(delete);
    }

    private HttpClientResponse processTrace(HttpClientRequest request) throws HttpProcessException {
        HttpTrace trace = new HttpTrace(request.getRequestUrl().toString());
        addHeaders(request, trace);
        return execute(trace);
    }

    private HttpClientResponse processHead(HttpClientRequest request) throws HttpProcessException {
        HttpHead head = new HttpHead(request.getRequestUrl().toString());
        addHeaders(request, head);
        return execute(head);
    }

    private HttpClientResponse processOptions(HttpClientRequest request) throws HttpProcessException {
        HttpOptions options = new HttpOptions(request.getRequestUrl().toString());
        addHeaders(request, options);
        return execute(options);
    }

    private HttpClientResponse processPut(HttpClientRequest request) throws HttpProcessException {
        HttpPut put = new HttpPut(request.getRequestUrl().toString());
        addHeaders(request, put);
        return execute(put);
    }

    /**
	 * @param request
	 * @return
	 */
    private HttpClientResponse execute(HttpRequestBase request) throws HttpProcessException {
        try {
            request.addHeader("User-Agent", createUserAgentHeaderValue());
            final HttpResponse apacheResponse = apacheClient.execute(request);
            return new HttpClientResponseAdapter(apacheResponse);
        } catch (ClientProtocolException e) {
            throw new HttpProtocolException(e);
        } catch (IOException e) {
            throw new HttpProtocolException(ManagedIOException.manage(e));
        }
    }

    private String createUserAgentHeaderValue() {
        return new StringBuilder(userAgent.getBrowserInfo().getBaseEngine()).append("/4.0 (compatible;").append(userAgent.getBrowserInfo().getName()).append(" ").append(userAgent.getBrowserInfo().getVersion().getMajor()).append(".").append(userAgent.getBrowserInfo().getVersion().getMinor()).append("; ").append(userAgent.getOperatingSystemInfo().getOperatingSystem()).append(" ").append(userAgent.getOperatingSystemInfo().getOperatingSystemVersion()).append(")").toString();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void setUserAgent(UserAgent userAgent) {
        this.userAgent = userAgent;
    }
}
