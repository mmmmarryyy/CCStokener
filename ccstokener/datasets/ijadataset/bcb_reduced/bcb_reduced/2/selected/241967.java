package subget;

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
import subget.exceptions.TimeoutException;

/**
 * <p>Title: Client HTTP Request class</p>
 * <p>Description: this class helps to send POST HTTP requests with various form data,
 * including files. Cookies can be added to be included in the request.</p>
 *
 * @author Vlad Patryshev
 * @version 1.0
 */
public class ClientHttpRequest {

    URLConnection connection;

    OutputStream os = null;

    Map cookies = new HashMap();

    protected void connect() throws IOException, TimeoutException, InterruptedException {
        if (os == null) os = Timeouts.getOutputStream(connection);
    }

    protected void write(char c) throws IOException, TimeoutException, InterruptedException {
        connect();
        os.write(c);
    }

    protected void write(String s) throws IOException, TimeoutException, InterruptedException {
        connect();
        os.write(s.getBytes());
    }

    protected void newline() throws IOException, TimeoutException, InterruptedException {
        connect();
        write("\r\n");
    }

    protected void writeln(String s) throws IOException, TimeoutException, InterruptedException {
        connect();
        write(s);
        newline();
    }

    private static Random random = new Random();

    protected static String randomString() {
        return Long.toString(random.nextLong(), 36);
    }

    String boundary = "---------------------------" + randomString() + randomString() + randomString();

    private void boundary() throws IOException, TimeoutException, InterruptedException {
        write("--");
        write(boundary);
    }

    /**
   * Creates a new multipart POST HTTP request on a freshly opened URLConnection
   *
   * @param connection an already open URL connection
   * @throws IOException
   */
    public ClientHttpRequest(URLConnection connection) throws IOException {
        this.connection = connection;
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    /**
   * Creates a new multipart POST HTTP request for a specified URL
   *
   * @param url the URL to send request to
   * @throws IOException
   */
    public ClientHttpRequest(URL url) throws IOException {
        this(url.openConnection(Global.getProxy()));
    }

    private void writeName(String name) throws IOException, TimeoutException, InterruptedException {
        newline();
        write("Content-Disposition: form-data; name=\"");
        write(name);
        write('"');
    }

    /**
   * adds a string parameter to the request
   * @param name parameter name
   * @param value parameter value
   * @throws IOException
   * @throws TimeoutException
   * @throws InterruptedException
   */
    public void setParameter(String name, String value) throws IOException, TimeoutException, InterruptedException {
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

    /**
   * adds a file parameter to the request
   * @param name parameter name
   * @param filename the name of the file
   * @param is input stream to read the contents of the file from
   * @param type content type, if "guess" function tries to guess type
   * @throws IOException
   */
    public void setParameter(String name, String filename, InputStream is, String type) throws IOException, TimeoutException, InterruptedException {
        boundary();
        writeName(name);
        write("; filename=\"");
        write(filename);
        write('"');
        newline();
        write("Content-Type: ");
        if (type.equals("guess")) {
            type = URLConnection.guessContentTypeFromName(filename);
            if (type == null) type = "application/octet-stream";
        }
        writeln(type);
        newline();
        pipe(is, os);
        newline();
    }

    /**
   * adds a file parameter to the request
   * @param name parameter name
   * @param file the file to upload
   * @param type content type, if "guess" function tries to guess type
   * @throws IOException
   */
    public void setParameter(String name, File file, String type) throws IOException, TimeoutException, InterruptedException {
        setParameter(name, file.getPath(), new FileInputStream(file), type);
    }

    /**
   * posts the requests to the server, with all the cookies and parameters that were added
   * @return input stream with the server response
   * @throws IOException
   */
    public InputStream post() throws IOException, TimeoutException, InterruptedException {
        boundary();
        writeln("--");
        os.close();
        return connection.getInputStream();
    }
}
