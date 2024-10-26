package net.sourceforge.javautil.common.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import net.sourceforge.javautil.common.io.IVirtualArtifactIOHandler;

/**
 * This will allow a URL to be the source of an input / output stream
 * for a {@link DirectoryFile}.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: URLIOHandler.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class URLIOHandler implements IVirtualArtifactIOHandler<DirectoryFile> {

    protected final URL url;

    public URLIOHandler(URL url) {
        this.url = url;
    }

    public InputStream getInputStream(DirectoryFile artifact, InputStream input) throws IOException {
        return url.openStream();
    }

    public OutputStream getOutputStream(DirectoryFile artifact, OutputStream output) throws IOException {
        return url.openConnection().getOutputStream();
    }
}
