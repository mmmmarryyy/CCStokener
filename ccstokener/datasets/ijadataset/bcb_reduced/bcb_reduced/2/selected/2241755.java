package org.jtools.filemanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

public class URLFileObject extends AbstractJavaFileObject {

    private final URL url;

    /**
     * Constructs a new StringFileObject.
     * @param name the name of the compilation unit represented by this file object
     * @param code the source code for the compilation unit represented by this file object
     * @throws MalformedURLException 
     */
    public URLFileObject(URI uri, Kind kind) {
        super(uri, kind);
        try {
            this.url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        Reader reader = new BufferedReader(reader(ignoreEncodingErrors));
        StringBuilder sb = new StringBuilder();
        for (int result = reader.read(); result != -1; result = reader.read()) sb.append((char) result);
        reader.close();
        reader = null;
        return sb.toString();
    }

    @Override
    protected Reader reader(boolean ignoreEncodingErrors) throws IOException {
        return new InputStreamReader(inputStream());
    }

    @Override
    protected Writer writer() throws IOException {
        return super.openWriter();
    }

    @Override
    protected InputStream inputStream() throws IOException {
        URLConnection conn = url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.connect();
        return conn.getInputStream();
    }

    @Override
    protected OutputStream outputStream() throws IOException {
        URLConnection conn = url.openConnection();
        conn.setDoInput(false);
        conn.setDoOutput(true);
        conn.connect();
        return conn.getOutputStream();
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public long getLastModified() {
        try {
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            return 0L;
        }
    }

    @Override
    public boolean setLastModified(long millis) {
        return false;
    }
}
