package hudson.zipscript.resource;

import hudson.zipscript.parser.exception.ExecutionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class URLResource extends AbstractResource {

    URL resource;

    long lastModified;

    public URLResource(URL resource) {
        this.resource = resource;
    }

    public InputStream getInputStream() {
        try {
            URLConnection urlConn = resource.openConnection();
            lastModified = urlConn.getLastModified();
            return urlConn.getInputStream();
        } catch (IOException e) {
            throw new ExecutionException("The file '" + resource.getPath() + "' could not be located", null, e);
        }
    }

    public boolean hasBeenModified() {
        try {
            URLConnection conn = resource.openConnection();
            if (conn.getLastModified() > lastModified) return true; else return false;
        } catch (IOException e) {
            return false;
        }
    }
}
