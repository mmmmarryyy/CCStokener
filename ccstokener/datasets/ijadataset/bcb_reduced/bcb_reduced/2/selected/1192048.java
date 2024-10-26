package org.cheetah.core.endpoint;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.cheetah.core.util.ResourceUtils;

public abstract class Endpoints {

    private static final ConcurrentHashMap<String, Endpoint> map = new ConcurrentHashMap<String, Endpoint>();

    static {
        URL[] urls = ResourceUtils.getURLs("classpath:META-INF/cheetah/cheetah.endpoints");
        if (urls.length > 0) {
            for (int i = 0; i < urls.length; ++i) {
                addEndpoints(urls[i]);
            }
        }
    }

    public static void addEndpoints(URL url) {
        Reader reader = null;
        Properties prop = new Properties();
        try {
            reader = new InputStreamReader(url.openStream());
            prop.load(reader);
        } catch (Throwable t) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable t) {
                }
            }
        }
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            try {
                Class<?> cla = Class.forName((String) entry.getValue(), true, Thread.currentThread().getContextClassLoader());
                addEndpoint((String) entry.getKey(), (Endpoint) cla.newInstance());
            } catch (Throwable t) {
            }
        }
    }

    public static void addEndpoint(String scheme, Endpoint endpoint) {
        if (map.putIfAbsent(scheme, endpoint) != null) {
            throw new IllegalArgumentException("scheme " + scheme + " already exists");
        }
    }

    public static void removeEndpoint(String scheme) {
        map.remove(scheme);
    }

    public static EndpointProducer createProducer(String uri) throws Exception {
        Endpoint endpoint = map.get(new URI(uri).getScheme());
        if (endpoint == null) {
            throw new IllegalArgumentException("unsupported uri " + uri);
        }
        return endpoint.createProducer(uri);
    }

    public static EndpointConsumer createConsumer(String uri) throws Exception {
        Endpoint endpoint = map.get(new URI(uri).getScheme());
        if (endpoint == null) {
            throw new IllegalArgumentException("unsupported uri " + uri);
        }
        return endpoint.createConsumer(uri);
    }
}
