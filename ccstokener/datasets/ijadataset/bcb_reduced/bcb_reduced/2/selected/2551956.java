package org.turms.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.turms.entity.AuthSource;
import org.turms.entity.Source;

/**
 * @author spe_ra (raffaele@speraprojects.com)
 *
 */
public class HTTPSourceResolver implements SourceResolver {

    private Source source;

    private DefaultHttpClient client;

    public HTTPSourceResolver(Source source) {
        this.source = source;
        client = new DefaultHttpClient();
        client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.TRUE);
        if (AuthSource.class.isAssignableFrom(source.getClass())) {
            try {
                login();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public InputStream resolve() throws SourceResolverException {
        HttpGet get = new HttpGet(source.getUrl());
        HttpResponse response = null;
        try {
            response = client.execute(get);
            return response.getEntity().getContent();
        } catch (Exception e) {
            throw new SourceResolverException("resolve.error", e);
        }
    }

    private void login() throws ClientProtocolException, IOException {
        HttpResponse response = client.execute(new HttpGet(((AuthSource) source).getLoginUrl()));
        response.getEntity().consumeContent();
        HttpPost post = new HttpPost(((AuthSource) source).getLoginUrl());
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Properties params = ((AuthSource) source).getLoginParams();
        for (String key : params.stringPropertyNames()) nvps.add(new BasicNameValuePair(key, params.getProperty(key)));
        post.setEntity(new UrlEncodedFormEntity(nvps));
        response = client.execute(post);
        response.getEntity().consumeContent();
    }
}
