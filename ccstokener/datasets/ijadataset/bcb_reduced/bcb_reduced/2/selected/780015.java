package net.simpleframework.web.page;

import java.io.IOException;
import java.io.InputStream;
import net.simpleframework.util.IoUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class UrlHttpClientForward extends AbstractUrlForward {

    public UrlHttpClientForward(final String url, final String includeRequestData) {
        super(url, includeRequestData);
    }

    public UrlHttpClientForward(final String url) {
        super(url);
    }

    @Override
    public String getResponseText(final PageRequestResponse requestResponse) {
        final String url = getRequestUrl(requestResponse);
        final HttpClient client = new DefaultHttpClient();
        final HttpGet get = new HttpGet(url);
        try {
            final HttpResponse response = client.execute(get);
            final InputStream inputStream = response.getEntity().getContent();
            try {
                return IoUtils.getStringFromInputStream(inputStream, PageUtils.pageConfig.getCharset());
            } finally {
                inputStream.close();
            }
        } catch (final IOException ex) {
            get.abort();
            throw convertRuntimeException(ex, url);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
