package synology.http;

import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.InputStream;
import java.util.Random;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.Iterator;

public class ClientHttpRequest {

    private static Random random = new Random();

    String boundary = "---------------------------" + randomString() + randomString() + randomString();

    URLConnection connection;

    OutputStream os = null;

    Map cookies = new HashMap();

    protected void connect() throws IOException {
        if (os == null) os = connection.getOutputStream();
    }

    protected void write(char c) throws IOException {
        connect();
        os.write(c);
    }

    protected void write(String s) throws IOException {
        connect();
        os.write(s.getBytes());
    }

    protected void newline() throws IOException {
        connect();
        write("\r\n");
    }

    protected void writeln(String s) throws IOException {
        connect();
        write(s);
        newline();
    }

    protected static String randomString() {
        return Long.toString(random.nextLong(), 36);
    }

    private void boundary() throws IOException {
        write("--");
        write(boundary);
    }

    public ClientHttpRequest(URLConnection connection) throws IOException {
        this.connection = connection;
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    public ClientHttpRequest(URL url) throws IOException {
        this(url.openConnection());
    }

    public ClientHttpRequest(String urlString) throws IOException {
        this(new URL(urlString));
    }

    private void postCookies() {
        StringBuffer cookieList = new StringBuffer();
        for (Iterator i = cookies.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) (i.next());
            cookieList.append(entry.getKey().toString() + "=" + entry.getValue());
            if (i.hasNext()) {
                cookieList.append("; ");
            }
        }
        if (cookieList.length() > 0) {
            connection.setRequestProperty("Cookie", cookieList.toString());
        }
    }

    public void setCookie(String name, String value) throws IOException {
        cookies.put(name, value);
    }

    public void setCookies(Map cookies) throws IOException {
        if (cookies == null) return;
        this.cookies.putAll(cookies);
    }

    public void setCookies(String[] cookies) throws IOException {
        if (cookies == null) return;
        for (int i = 0; i < cookies.length - 1; i += 2) {
            setCookie(cookies[i], cookies[i + 1]);
        }
    }

    private void writeName(String name) throws IOException {
        newline();
        write("Content-Disposition: form-data; name=\"");
        write(name);
        write('"');
    }

    public void setParameter(String name, String value) throws IOException {
        boundary();
        writeName(name);
        newline();
        newline();
        writeln(value);
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[500000];
        int nread;
        int navailable;
        int total = 0;
        synchronized (in) {
            while ((nread = in.read(buf, 0, buf.length)) >= 0) {
                out.write(buf, 0, nread);
                total += nread;
            }
        }
        out.flush();
        buf = null;
    }

    public void setParameter(String name, String filename, InputStream is) throws IOException {
        boundary();
        writeName(name);
        write("; filename=\"");
        write(filename);
        write('"');
        newline();
        write("Content-Type: ");
        String type = connection.guessContentTypeFromName(filename);
        if (type == null) type = "application/octet-stream";
        writeln(type);
        newline();
        pipe(is, os);
        newline();
    }

    public void setParameter(String name, File file) throws IOException {
        setParameter(name, file.getPath(), new FileInputStream(file));
    }

    public void setParameter(String name, Object object) throws IOException {
        if (object instanceof File) {
            setParameter(name, (File) object);
        } else {
            setParameter(name, object.toString());
        }
    }

    public void setParameters(Map parameters) throws IOException {
        if (parameters == null) return;
        for (Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            setParameter(entry.getKey().toString(), entry.getValue());
        }
    }

    public void setParameters(Object[] parameters) throws IOException {
        if (parameters == null) return;
        for (int i = 0; i < parameters.length - 1; i += 2) {
            setParameter(parameters[i].toString(), parameters[i + 1]);
        }
    }

    public InputStream post() throws IOException {
        boundary();
        writeln("--");
        os.close();
        return connection.getInputStream();
    }

    public InputStream post(Map parameters) throws IOException {
        setParameters(parameters);
        return post();
    }

    public InputStream post(Object[] parameters) throws IOException {
        setParameters(parameters);
        return post();
    }

    public InputStream post(Map cookies, Map parameters) throws IOException {
        setCookies(cookies);
        setParameters(parameters);
        return post();
    }

    public InputStream post(String[] cookies, Object[] parameters) throws IOException {
        setCookies(cookies);
        setParameters(parameters);
        return post();
    }

    public InputStream post(String name, Object value) throws IOException {
        setParameter(name, value);
        return post();
    }

    public InputStream post(String name1, Object value1, String name2, Object value2) throws IOException {
        setParameter(name1, value1);
        return post(name2, value2);
    }

    public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
        setParameter(name1, value1);
        return post(name2, value2, name3, value3);
    }

    public InputStream post(String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
        setParameter(name1, value1);
        return post(name2, value2, name3, value3, name4, value4);
    }

    public static InputStream post(URL url, Map parameters) throws IOException {
        return new ClientHttpRequest(url).post(parameters);
    }

    public static InputStream post(URL url, Object[] parameters) throws IOException {
        return new ClientHttpRequest(url).post(parameters);
    }

    public static InputStream post(URL url, Map cookies, Map parameters) throws IOException {
        return new ClientHttpRequest(url).post(cookies, parameters);
    }

    public static InputStream post(URL url, String[] cookies, Object[] parameters) throws IOException {
        return new ClientHttpRequest(url).post(cookies, parameters);
    }

    public static InputStream post(URL url, String name1, Object value1) throws IOException {
        return new ClientHttpRequest(url).post(name1, value1);
    }

    public static InputStream post(URL url, String name1, Object value1, String name2, Object value2) throws IOException {
        return new ClientHttpRequest(url).post(name1, value1, name2, value2);
    }

    public static InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3) throws IOException {
        return new ClientHttpRequest(url).post(name1, value1, name2, value2, name3, value3);
    }

    public static InputStream post(URL url, String name1, Object value1, String name2, Object value2, String name3, Object value3, String name4, Object value4) throws IOException {
        return new ClientHttpRequest(url).post(name1, value1, name2, value2, name3, value3, name4, value4);
    }

    public URLConnection getConnection() {
        return connection;
    }

    public void setConnection(URLConnection connection) {
        this.connection = connection;
    }
}
