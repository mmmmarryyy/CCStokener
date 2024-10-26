package net.sourceforge.comeback.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Writes resources to memory accessible by ComeBack! URLs.
 *
 * @author Michael Rudolf
 */
public class MemoryResourceWriter implements ResourceWriter {

    public void writeResource(String name, byte[] code) throws IOException {
        URL url;
        try {
            url = new URL(ComebackURLStreamHandler.PROTOCOL, null, -1, name, ComebackURLStreamHandler.getInstance());
        } catch (MalformedURLException ex) {
            throw (IOException) new IOException(ex.getMessage()).initCause(ex);
        }
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        out.write(code);
        out.close();
    }

    public void close() {
    }
}
