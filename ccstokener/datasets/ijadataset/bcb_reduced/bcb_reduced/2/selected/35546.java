package migool.http.client;

import static migool.http.client.HttpClientUtil.parseCharset;
import java.io.IOException;
import migool.entity.FileEntity;
import migool.entity.MimeTypeEntity;
import migool.util.IOUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.ParserException;

/**
 * 
 * @author Denis Migol
 * 
 */
public final class HttpClientWrapper {

    private String charset;

    private boolean isFirstRequest = true;

    private HttpClient httpClient;

    /**
	 * The constructor
	 */
    public HttpClientWrapper() {
        this(HttpClientFactory.get().newHttpClient());
    }

    /**
	 * 
	 * @param client
	 */
    public HttpClientWrapper(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
	 * 
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    public String requestToString(final HttpUriRequest request) throws IOException {
        final HttpResponse response = httpClient.execute(request);
        final byte[] ret = IOUtil.toByteArray(response.getEntity().getContent());
        if (isFirstRequest) {
            charset = parseCharset(response.getEntity().getContentType().getValue());
            if (charset == null) {
                try {
                    charset = parseCharsetFromHtml(new String(ret));
                } catch (final Exception e) {
                }
            }
            isFirstRequest = false;
        }
        return charset == null ? new String(ret) : new String(ret, charset);
    }

    private static final String parseCharsetFromHtml(final String html) throws ParserException {
        final Parser parser = new Parser(html);
        final MetaTag meta = (MetaTag) parser.parse(new AndFilter(new TagNameFilter("meta"), new HasAttributeFilter("http-equiv", "Content-Type"))).elementAt(0);
        if (meta != null) {
            return parseCharset(meta.getMetaContent());
        }
        return null;
    }

    /**
	 * 
	 * @param request
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    public void requestToVoid(final HttpUriRequest request) throws IOException {
        final HttpResponse response = httpClient.execute(request);
        response.getEntity().getContent().close();
    }

    /**
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
    public FileEntity requestToFileEntity(final HttpUriRequest request) throws IOException {
        final HttpResponse response = httpClient.execute(request);
        final String mimeType = response.getEntity().getContentType().getValue();
        final byte[] bytes = IOUtil.toByteArray(response.getEntity().getContent());
        return new FileEntity(mimeType, bytes, null);
    }

    /**
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
    public MimeTypeEntity requestToMimeTypeEntity(final HttpUriRequest request) throws IOException {
        final HttpResponse response = httpClient.execute(request);
        final String mimeType = response.getEntity().getContentType().getValue();
        final byte[] bytes = IOUtil.toByteArray(response.getEntity().getContent());
        return new MimeTypeEntity(mimeType, bytes);
    }

    /**
	 * 
	 * @param uri
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
    public String getToString(final String uri) throws IOException {
        return requestToString(new HttpGet(uri));
    }

    /**
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
    public FileEntity getToFileEntity(final String uri) throws IOException {
        return requestToFileEntity(new HttpGet(uri));
    }

    /**
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
    public MimeTypeEntity getToMimeTypeEntity(final String uri) throws IOException {
        return requestToMimeTypeEntity(new HttpGet(uri));
    }

    /**
	 * 
	 * @return
	 */
    public String getCharset() {
        return charset;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
    public String getRedirectedUrl(final String url) throws IOException {
        final HttpGet httpget = new HttpGet(url);
        final HttpContext context = new BasicHttpContext();
        final HttpResponse response = httpClient.execute(httpget, context);
        final HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
        final HttpHost currentHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity.consumeContent();
        }
        return currentHost.toURI() + currentReq.getURI();
    }
}
