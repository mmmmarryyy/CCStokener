package com.google.code.hibernate.rest.method;

import static com.google.code.hibernate.rest.internal.InternalPreconditions.checkNotNull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import com.google.code.hibernate.rest.internal.InternalStreams;

/**
 * 
 * @author wangzijian
 * 
 */
public class Http {

    private Http() {
    }

    public static boolean exists(String url) {
        try {
            return get(url).getCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    public static Response put(String url, String body) throws IOException {
        return invoke(url, "PUT", body);
    }

    public static Response post(String url, String body) throws IOException {
        return invoke(url, "POST", body);
    }

    public static Response delete(String url) throws IOException {
        return invoke(url, "DELETE");
    }

    public static Response get(String url) throws IOException {
        return invoke(url, "GET");
    }

    public static Response head(String url) throws IOException {
        return invoke(url, "HEAD");
    }

    public static Response options(String url) throws IOException {
        return invoke(url, "OPTIONS");
    }

    public static Response get(String url, Map<String, String> headers) throws IOException {
        checkNotNull(url, "url");
        checkNotNull(headers, "headers");
        HttpURLConnection connection = connect(url);
        for (Entry<String, String> each : headers.entrySet()) {
            connection.addRequestProperty(each.getKey(), each.getValue());
        }
        return Response.of(connection);
    }

    private static Response invoke(String url, String method) throws IOException {
        return invoke(url, method, null);
    }

    private static Response invoke(String url, String method, String body) throws IOException {
        checkNotNull(url, "url");
        checkNotNull(method, "method");
        HttpURLConnection connection = connect(url);
        connection.setRequestMethod(method);
        if (body != null) {
            write(body, connection);
        }
        return Response.of(connection);
    }

    private static void write(String body, HttpURLConnection connection) throws IOException {
        connection.setDoOutput(true);
        InternalStreams.write(body, connection.getOutputStream());
    }

    private static HttpURLConnection connect(String url) throws IOException, MalformedURLException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
}
