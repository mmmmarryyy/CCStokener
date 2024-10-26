package org.opennms.protocols.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.opennms.core.utils.ThreadCategory;

/**
 * The class for managing HTTP URL Connection using Apache HTTP Client
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
public class HttpUrlConnection extends URLConnection {

    /** The URL. */
    private URL m_url;

    /**
     * Instantiates a new SFTP URL connection.
     *
     * @param url the URL
     */
    protected HttpUrlConnection(URL url) {
        super(url);
        m_url = url;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            int port = m_url.getPort() > 0 ? m_url.getPort() : m_url.getDefaultPort();
            String[] userInfo = m_url.getUserInfo() == null ? null : m_url.getUserInfo().split(":");
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(URIUtils.createURI(m_url.getProtocol(), m_url.getHost(), port, m_url.getPath(), m_url.getQuery(), null));
            if (userInfo != null) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userInfo[0], userInfo[1]);
                request.addHeader(BasicScheme.authenticate(credentials, "UTF-8", false));
            }
            HttpResponse response = client.execute(request);
            return new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity()));
        } catch (Exception e) {
            throw new IOException("Can't retrieve " + m_url.getPath() + " from " + m_url.getHost() + " because " + e.getMessage());
        }
    }

    /**
     * Log.
     *
     * @return the thread category
     */
    protected ThreadCategory log() {
        return ThreadCategory.getInstance(getClass());
    }
}
