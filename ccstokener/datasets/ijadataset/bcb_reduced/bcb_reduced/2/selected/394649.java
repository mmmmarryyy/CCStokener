package sk.yw.azetclient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author error216
 */
public class HttpURLHandler {

    private static final Logger logger = Logger.getLogger(HttpURLHandler.class);

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    private static Proxy proxy = Proxy.NO_PROXY;

    private InputStream inputStream;

    public static void setProxy(Proxy proxy) {
        HttpURLHandler.proxy = proxy;
    }

    public HttpURLHandler(URL url, String requestMethod, Map<String, String> parameters, String outputEncoding) throws IOException {
        logger.debug("Creating http url handler for: " + url + "; using method: " + requestMethod + "; with parameters: " + parameters);
        if (url == null) throw new IllegalArgumentException("Null pointer in url");
        if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) throw new IllegalArgumentException("Illegal url protocol: \"" + url.getProtocol() + "\"; must be \"http\" or \"https\"");
        if (requestMethod == null) throw new IllegalArgumentException("Null pointer in requestMethod");
        if (!"GET".equals(requestMethod) && !"POST".equals(requestMethod)) throw new IllegalArgumentException("Illegal request method: " + requestMethod + "; must be \"GET\" or \"POST\"");
        if (parameters == null) throw new IllegalArgumentException("Null pointer in parameters");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
        connection.setRequestMethod(requestMethod);
        connection.setUseCaches(false);
        if (EMPTY_MAP.equals(parameters)) {
            connection.setDoOutput(false);
        } else {
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            writeParameters(out, parameters, outputEncoding);
            out.close();
        }
        inputStream = connection.getInputStream();
    }

    public HttpURLHandler(URL url) throws IOException {
        this(url, "GET", EMPTY_MAP, null);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    private void writeParameters(OutputStream out, Map<String, String> parameters, String outputEncoding) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, outputEncoding));
        boolean first = true;
        for (String key : parameters.keySet()) {
            if (first) first = false; else writer.write('&');
            writer.write(key);
            writer.write('=');
            writer.write(URLEncoder.encode(parameters.get(key), outputEncoding));
        }
        writer.flush();
    }
}
