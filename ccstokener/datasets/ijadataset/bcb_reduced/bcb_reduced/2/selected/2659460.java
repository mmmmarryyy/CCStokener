package jnlp.sample.servlet.download;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import jnlp.sample.util.ObjectUtil;

public class ResourceFileDownloadResponse extends FileDownloadResponse {

    private URL _url;

    public URL getURL() {
        return _url;
    }

    private URLConnection _urlConn;

    public void setURL(URL u) {
        if (_url != u) {
            _url = u;
            if (_urlConn != null) _urlConn = null;
        }
    }

    public ResourceFileDownloadResponse(URL url, String mimeType, String versionId, long lastModified) {
        super(mimeType, versionId, lastModified, (null == url) ? null : url.toString());
        _url = url;
    }

    public ResourceFileDownloadResponse() {
        this(null, null, null, 0L);
    }

    protected synchronized URLConnection getURLConnection() throws IOException {
        if (null == _urlConn) {
            final URL url = getURL();
            _urlConn = (null == url) ? null : url.openConnection();
        }
        return _urlConn;
    }

    @Override
    public int getContentLength() throws IOException {
        final URLConnection conn = getURLConnection();
        if (null == conn) throw new FileNotFoundException("getContentLength(" + getURL() + ") no " + URLConnection.class.getSimpleName());
        return conn.getContentLength();
    }

    @Override
    public InputStream getContent() throws IOException {
        return ObjectUtil.openResource(getURL());
    }

    @Override
    public String toString() {
        return super.toString() + "[ " + getArgString() + "]";
    }

    @Override
    public ResourceFileDownloadResponse clone() throws CloneNotSupportedException {
        return getClass().cast(super.clone());
    }
}
