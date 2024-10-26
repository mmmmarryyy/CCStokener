package net.sf.dropboxmq.esb.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import net.sf.dropboxmq.LogHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created: 30 Aug 2011
 *
 * @author <a href="mailto:dwayne@schultz.net">Dwayne Schultz</a>
 * @version $Revision$, $Date$
 */
public class HTTPRequester {

    private static final Log log = LogFactory.getLog(HTTPRequester.class);

    private String defaultURI = null;

    private String defaultRequestMethod = null;

    private int defaultConnectionTimeout = -1;

    private int defaultReadTimeout = -1;

    public HTTPRequester() {
        LogHelper.logMethod(log, toObjectString(), "HTTPRequester()");
    }

    public HTTPRequester(final String defaultURI, final String defaultRequestMethod, final int defaultConnectionTimeout, final int defaultReadTimeout) {
        LogHelper.logMethod(log, toObjectString(), "HTTPRequester(), defaultURI = " + defaultURI + ", defaultRequestMethod = " + defaultRequestMethod + ", defaultConnectionTimeout = " + defaultConnectionTimeout + ", defaultReadTimeout = " + defaultReadTimeout);
        this.defaultURI = defaultURI;
        this.defaultRequestMethod = defaultRequestMethod;
        this.defaultConnectionTimeout = defaultConnectionTimeout;
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public String getDefaultURI() {
        return defaultURI;
    }

    public void setDefaultURI(final String defaultURI) {
        this.defaultURI = defaultURI;
    }

    public String getDefaultRequestMethod() {
        return defaultRequestMethod;
    }

    public void setDefaultRequestMethod(final String defaultRequestMethod) {
        this.defaultRequestMethod = defaultRequestMethod;
    }

    public int getDefaultConnectionTimeout() {
        return defaultConnectionTimeout;
    }

    public void setDefaultConnectionTimeout(final int defaultConnectionTimeout) {
        this.defaultConnectionTimeout = defaultConnectionTimeout;
    }

    public int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public void setDefaultReadTimeout(final int defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public String request(final String content, final Map<String, List<Object>> requestProperties) throws IOException {
        return request(defaultURI, defaultRequestMethod, content, requestProperties, defaultConnectionTimeout, defaultReadTimeout);
    }

    public String request(final String uri, final String requestMethod, final String content, final Map<String, List<Object>> requestProperties, final int connectionTimeout, final int readTimeout) throws IOException {
        LogHelper.logMethod(log, toObjectString(), "request(), uri = " + uri + ", requestMethod = " + requestMethod + ", content = " + content + ", requestProperties = " + requestProperties + ", connectionTimeout = " + connectionTimeout + ", readTimeout = " + readTimeout);
        final URL url = new URL(uri == null ? defaultURI : uri);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(requestMethod == null ? defaultRequestMethod == null ? "GET" : defaultRequestMethod : requestMethod);
        if (requestProperties != null) {
            for (final Map.Entry<String, List<Object>> entry : requestProperties.entrySet()) {
                for (final Object value : entry.getValue()) {
                    connection.addRequestProperty(entry.getKey(), value.toString());
                }
            }
        }
        connection.setConnectTimeout(connectionTimeout == -1 ? defaultConnectionTimeout : connectionTimeout);
        connection.setReadTimeout(readTimeout == -1 ? defaultReadTimeout : readTimeout);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        if (content != null) {
            writeContent(content, connection.getOutputStream());
        }
        connection.connect();
        return readContent(connection.getInputStream());
    }

    private static void writeContent(final String content, final OutputStream outputStream) throws IOException {
        final OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        try {
            writer.write(content);
        } finally {
            writer.close();
        }
    }

    private static String readContent(final InputStream in) throws IOException {
        final StringWriter writer = new StringWriter();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            boolean done = false;
            while (!done) {
                final String line = reader.readLine();
                if (line == null) {
                    done = true;
                } else {
                    writer.write(line + "\n");
                }
            }
        } finally {
            reader.close();
        }
        return writer.toString();
    }

    protected final String toObjectString() {
        return super.toString();
    }
}
