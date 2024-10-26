package nu.staldal.lagoon.producer;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import nu.staldal.lagoon.core.*;

public class URLRead extends Read {

    private URL url;

    public void init() throws LagoonException {
        String u = getParam("name");
        if (u == null) {
            throw new LagoonException("url parameter not specified");
        }
        try {
            url = new URL(u);
        } catch (MalformedURLException e) {
            throw new LagoonException("Malformed URL: " + e.getMessage());
        }
    }

    public void start(OutputStream bytes, Target target) throws IOException {
        URLConnection conn = url.openConnection();
        InputStream fis = conn.getInputStream();
        byte[] buf = new byte[4096];
        while (true) {
            int bytesRead = fis.read(buf);
            if (bytesRead < 1) break;
            bytes.write(buf, 0, bytesRead);
        }
        fis.close();
    }

    public boolean hasBeenUpdated(long when) {
        return true;
    }
}
