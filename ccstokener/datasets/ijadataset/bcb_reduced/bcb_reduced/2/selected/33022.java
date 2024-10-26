package org.jbrt.client.net.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.net.URL;
import java.util.Random;

/**
 *
 * @author Cipov Peter
 */
class JMultipartHttpFilePost {

    private URLConnection connection;

    private OutputStream os = null;

    private boolean osClosed = true;

    protected void connect() throws IOException {
        if (os == null) {
            os = connection.getOutputStream();
            osClosed = false;
        }
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

    private static Random random = new Random();

    protected static String randomString() {
        return Long.toString(random.nextLong(), 36);
    }

    String boundary = "---------------------------" + randomString() + randomString() + randomString();

    private void boundary() throws IOException {
        write("--");
        write(boundary);
    }

    public JMultipartHttpFilePost(URLConnection connection) throws IOException {
        this.connection = connection;
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.connect();
    }

    public JMultipartHttpFilePost(URL url) throws IOException {
        this(url.openConnection());
    }

    public JMultipartHttpFilePost(String urlString) throws IOException {
        this(new URL(urlString));
    }

    private void writeName(String name) throws IOException {
        newline();
        write("Content-Disposition: form-data; name=\"");
        write(name);
        write('"');
    }

    public void endFile() throws IOException {
        newline();
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public boolean canWrite() {
        return !osClosed;
    }

    public OutputStream startFile(String name, String filename) throws IOException {
        boundary();
        writeName(name);
        write("; filename=\"");
        write(filename);
        write('"');
        newline();
        write("Content-Type: ");
        String type = connection.guessContentTypeFromName(filename);
        if (type == null) {
            type = "application/octet-stream";
        }
        writeln(type);
        newline();
        return os;
    }

    public InputStream post() throws IOException {
        boundary();
        writeln("--");
        os.close();
        osClosed = true;
        return connection.getInputStream();
    }
}
