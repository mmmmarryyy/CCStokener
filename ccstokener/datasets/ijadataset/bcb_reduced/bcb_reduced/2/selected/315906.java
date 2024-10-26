package soapdust.urlhandler.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * This class handles test: urls. The jvm is automatically initialized
 * so that this class is used to resolve test: urls.
 * 
 * test: urls allows you to test an application that access to a
 * http server by pointing your application to a test: url instead
 * of a http: url.
 * 
 * With a test: url, you can specify the http status code you want the
 * http request to return and also a file which content will be
 * returned as the http response content or error content.
 * 
 * test: may be of the form :
 * 
 * test:status:500 => requesting this url will result in a 500 http status
 * 
 * test:file:test/response.xml => requesting this url will return the content of the given file
 * 
 * test:status:500;file:test/response.xml
 * 
 * status: is optionnal and defaults to 200
 * file: if optionnal and defaults to empty file
 *
 * One can consult the data written "to" a test: url by accessing the public
 * HashTable saved in this class. Data is indexed by url.
 * 
 * See HandlerTest.java for examples of using this class.
 *
 */
public class Handler extends URLStreamHandler {

    private static final String STATUS_CAPTURE = "$2";

    private static final String STATUS_REGEX = "(|.*;)status:([^;]*).*";

    public static Hashtable<String, List<ByteArrayOutputStream>> saved = new Hashtable<String, List<ByteArrayOutputStream>>();

    public static ByteArrayOutputStream lastSaved(String url) {
        List<ByteArrayOutputStream> list = saved.get(url);
        return list.get(list.size() - 1);
    }

    private static Hashtable<String, Matcher> savedMatchers = new Hashtable<String, Matcher>();

    public static void clear() {
        saved.clear();
        savedMatchers.clear();
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        final String urlPath = url.getPath();
        final int status;
        final String path;
        String statusAsString = extractValue(urlPath, STATUS_REGEX, STATUS_CAPTURE);
        status = statusAsString == null ? 200 : Integer.parseInt(statusAsString);
        path = extractPath(urlPath);
        if (saved.get(url.toString()) == null) {
            saved.put(url.toString(), new ArrayList<ByteArrayOutputStream>());
        }
        return new HttpsURLConnection(url) {

            @Override
            public int getResponseCode() throws IOException {
                return status;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                if (errorStatus()) throw new IOException("fake server returned a fake error");
                if (path == null) {
                    return new ByteArrayInputStream(new byte[0]);
                } else {
                    return new FileInputStream(path);
                }
            }

            @Override
            public InputStream getErrorStream() {
                if (errorStatus()) {
                    try {
                        return new FileInputStream(path);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return null;
                }
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                Handler.saved.get(url.toString()).add(out);
                return out;
            }

            @Override
            public String getContentType() {
                return "test/plain";
            }

            @Override
            public void connect() throws IOException {
            }

            @Override
            public boolean usingProxy() {
                return false;
            }

            @Override
            public void disconnect() {
            }

            private boolean errorStatus() {
                return status >= 500 && status <= 599;
            }

            @Override
            public String getCipherSuite() {
                return null;
            }

            @Override
            public Certificate[] getLocalCertificates() {
                return null;
            }

            @Override
            public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
                return null;
            }
        };
    }

    private String extractPath(final String urlPath) {
        final String path;
        Matcher matcher = savedMatchers.get(urlPath);
        if (matcher == null) {
            matcher = Pattern.compile("file:([^;]*)").matcher(urlPath);
            savedMatchers.put(urlPath, matcher);
        }
        if (!matcher.find()) {
            matcher.reset();
            path = matcher.find() ? matcher.group(1) : null;
        } else {
            path = matcher.group(1);
        }
        return path;
    }

    private String extractValue(final String urlPath, String regex, String capture) {
        if (urlPath.matches(regex)) {
            return urlPath.replaceAll(regex, capture);
        } else {
            return null;
        }
    }
}
