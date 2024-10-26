package org.apache.commons.vfs.provider;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.FileType;

/**
 * A default URL stream handler that will work for most file systems.
 *
 * @author <a href="mailto:brian@mmmanager.org">Brian Olsen</a>
 * @version $Revision: 480428 $ $Date: 2006-11-28 22:15:24 -0800 (Tue, 28 Nov 2006) $
 */
public class DefaultURLStreamHandler extends URLStreamHandler {

    private final VfsComponentContext context;

    private final FileSystemOptions fileSystemOptions;

    public DefaultURLStreamHandler(final VfsComponentContext context) {
        this(context, null);
    }

    public DefaultURLStreamHandler(final VfsComponentContext context, final FileSystemOptions fileSystemOptions) {
        this.context = context;
        this.fileSystemOptions = fileSystemOptions;
    }

    protected URLConnection openConnection(final URL url) throws IOException {
        final FileObject entry = context.resolveFile(url.toExternalForm(), fileSystemOptions);
        return new DefaultURLConnection(url, entry.getContent());
    }

    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        try {
            FileObject old = context.resolveFile(u.toExternalForm(), fileSystemOptions);
            FileObject newURL;
            if (start > 0 && spec.charAt(start - 1) == ':') {
                newURL = context.resolveFile(old, spec, fileSystemOptions);
            } else {
                if (old.getType() == FileType.FILE && old.getParent() != null) {
                    newURL = old.getParent().resolveFile(spec);
                } else {
                    newURL = old.resolveFile(spec);
                }
            }
            final String url = newURL.getName().getURI();
            final StringBuffer filePart = new StringBuffer();
            final String protocolPart = UriParser.extractScheme(url, filePart);
            setURL(u, protocolPart, "", -1, null, null, filePart.toString(), null, null);
        } catch (FileSystemException fse) {
            throw new RuntimeException(fse.getMessage());
        }
    }

    protected String toExternalForm(final URL u) {
        return u.getProtocol() + ":" + u.getFile();
    }
}
