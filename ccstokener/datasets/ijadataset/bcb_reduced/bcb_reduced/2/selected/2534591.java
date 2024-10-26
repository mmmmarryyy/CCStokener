package uk.co.pointofcare.echobase.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

@Deprecated
public class HttpIO<T extends HttpURLConnection> {

    static Logger log = Logger.getLogger(HttpIO.class);

    Authenticator auth = null;

    T connection;

    InputStream in;

    boolean connected;

    @Deprecated
    @SuppressWarnings("unchecked")
    public HttpIO(URL url) throws IOException {
        log.setLevel(Level.INFO);
        connection = (T) url.openConnection();
        setupConnection();
        connected = false;
    }

    @Deprecated
    private void setupConnection() throws IOException {
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5;)");
        connection.setRequestProperty("Accept-Language", "en-us");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setInstanceFollowRedirects(true);
    }

    @Deprecated
    public <S extends HttpURLConnection> HttpIO<S> sessionUrl(Class<S> clz, URL url) throws IOException {
        if (!connected) throw new IOException("doConnect() not called");
        HttpIO<S> out = new HttpIO<S>(url);
        out.setAuthentication(auth);
        out.setupConnection();
        return out;
    }

    @Deprecated
    public void setAuthentication(Authenticator auth) {
        this.auth = auth;
        Authenticator.setDefault(auth);
    }

    @Deprecated
    public void setAuthentication(final String username, final String password) {
        setAuthentication(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });
    }

    @Deprecated
    public static Document wgets(URL url, String username, String password) throws IOException {
        HttpIO<HttpsURLConnection> c = new HttpIO<HttpsURLConnection>(url);
        c.setAuthentication(username, password);
        c.doConnect();
        return c.get(true);
    }

    @Deprecated
    public static Document wgets(URL url, Authenticator auth) throws IOException {
        HttpIO<HttpsURLConnection> c = new HttpIO<HttpsURLConnection>(url);
        c.setAuthentication(auth);
        c.doConnect();
        return c.get(true);
    }

    @Deprecated
    public static Document wgets(URL url, HttpIO<? extends HttpsURLConnection> con) throws IOException {
        HttpIO<HttpsURLConnection> c = con.sessionUrl(HttpsURLConnection.class, url);
        c.doConnect();
        return c.get(true);
    }

    @Deprecated
    public static Document wget(URL url) throws IOException {
        HttpIO<HttpURLConnection> c = new HttpIO<HttpURLConnection>(url);
        c.doConnect();
        return c.get(true);
    }

    @Deprecated
    public static Document wget(URL url, HttpIO<? extends HttpURLConnection> con) throws IOException {
        HttpIO<HttpURLConnection> c = con.sessionUrl(HttpURLConnection.class, url);
        c.doConnect();
        return c.get(true);
    }

    @Deprecated
    public HttpIO<T> doConnect() throws IOException {
        if (!connected) {
            connection.connect();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
                in = connection.getInputStream();
                connected = true;
            } else {
                log.info(headersToString());
                throw new IOException("could not get connection: reponseCode=" + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }
        }
        return this;
    }

    @Deprecated
    public String headersToString() {
        StringBuffer out = new StringBuffer("==RESPONSE==\n");
        for (Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            out.append(entry.getKey() + ": " + entry.getValue().get(0) + "\n");
        }
        return out.toString();
    }

    @Deprecated
    public String requestToString() {
        StringBuffer out = new StringBuffer("==REQUEST==\n");
        for (Entry<String, List<String>> entry : connection.getRequestProperties().entrySet()) {
            out.append(entry.getKey() + ": " + entry.getValue().get(0) + "\n");
        }
        return out.toString();
    }

    @Deprecated
    public Document get(boolean tidy) throws IOException {
        if (!connected) throw new IOException("doConnect() not called or failed");
        Document page;
        try {
            if (tidy) {
                page = XmlIO.getDomFromHtmlStream(in);
            } else {
                page = XmlIO.getDomFromStream(in);
            }
        } catch (Exception e) {
            throw new IOException("malformed html/xml");
        }
        return page;
    }
}
