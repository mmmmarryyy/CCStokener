package pl.mn.communicator.packet.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import pl.mn.communicator.IGGConfiguration;

/**
 * Created on 2005-01-27
 * 
 * @author <a href="mailto:mati@sz.home.pl">Mateusz Szczap</a>
 * @version $Id: HttpRequest.java,v 1.1 2005/11/05 23:34:53 winnetou25 Exp $
 */
public abstract class HttpRequest {

    public static final String WINDOW_ENCODING = "windows-1250";

    protected final IGGConfiguration m_ggconfiguration;

    protected final HttpURLConnection m_huc;

    protected HttpRequest(IGGConfiguration configuration) throws IOException {
        if (configuration == null) throw new IllegalArgumentException("configuration cannot be null");
        m_ggconfiguration = configuration;
        URL url = new URL(getURL());
        m_huc = (HttpURLConnection) url.openConnection();
        m_huc.setRequestMethod("POST");
        m_huc.setDoInput(true);
        if (wannaWrite()) {
            m_huc.setDoOutput(true);
        }
        m_huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        m_huc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98)");
    }

    public HttpURLConnection connect() throws IOException {
        m_huc.setRequestProperty("Content-Length", String.valueOf(getRequestBody().length()));
        m_huc.connect();
        return m_huc;
    }

    public HttpURLConnection sendRequest() throws IOException {
        if (wannaWrite()) {
            PrintWriter out = new PrintWriter(m_huc.getOutputStream(), true);
            out.println(getRequestBody());
            out.close();
        }
        return m_huc;
    }

    public HttpURLConnection disconnect() {
        if (m_huc == null) throw new IllegalStateException("must call connect() and sendRequest() first");
        m_huc.disconnect();
        return m_huc;
    }

    public abstract HttpResponse getResponse() throws IOException;

    protected abstract String getURL();

    protected abstract String getRequestBody() throws UnsupportedEncodingException;

    protected abstract boolean wannaWrite();
}
