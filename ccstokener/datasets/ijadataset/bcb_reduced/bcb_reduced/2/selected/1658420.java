package net.sf.tidysaucer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

public class Response {

    private final HttpEntity entity;

    private final URL url;

    private final URLConnection uc;

    protected Response(final HttpEntity entity, final URL url) throws IOException {
        this.entity = entity;
        this.url = url;
        uc = entity == null ? url.openConnection() : null;
    }

    public String getContentType() {
        if (entity == null) {
            return uc.getContentType();
        }
        final Header h = entity.getContentType();
        return h == null ? null : h.getValue();
    }

    public InputStream getStream() throws IOException {
        return entity == null ? uc.getInputStream() : entity.getContent();
    }

    public URL getURL() {
        return url;
    }
}
