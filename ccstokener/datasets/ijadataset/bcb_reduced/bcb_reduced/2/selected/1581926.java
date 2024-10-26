package com.googlecode.httl.support.loaders;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import com.googlecode.httl.support.Loader;

/**
 * UrlResource. (SPI, Prototype, ThreadSafe)
 * 
 * @see com.googlecode.httl.support.loaders.UrlLoader#load(String, String)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public class UrlResource extends InputStreamResource {

    private static final long serialVersionUID = 1L;

    private static final String FILE_PROTOCOL = "file";

    private final URL url;

    private final File file;

    public UrlResource(Loader loader, String name, String encoding, String path) throws IOException {
        super(loader, name, encoding);
        this.url = new URL(path);
        this.file = toFile(url);
    }

    private File toFile(URL url) throws IOException {
        if (FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            try {
                return new File(url.toURI().getSchemeSpecificPart());
            } catch (URISyntaxException e) {
                throw new MalformedURLException(e.getMessage());
            }
        }
        return null;
    }

    public long getLastModified() {
        if (file != null) {
            return file.lastModified();
        }
        return super.getLastModified();
    }

    public long getLength() {
        if (file != null) {
            return file.length();
        }
        return super.getLength();
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return url.openStream();
    }
}
