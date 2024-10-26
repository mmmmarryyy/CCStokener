package org.apache.commons.vfs.provider.url;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileObject;
import org.apache.commons.vfs.provider.URLFileName;

/**
 * A {@link FileObject} implementation backed by a {@link URL}.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 480428 $ $Date: 2006-11-28 22:15:24 -0800 (Tue, 28 Nov 2006) $
 * @todo Implement set lastModified and get/set attribute
 * @todo Implement getOutputStream()
 */
public class UrlFileObject extends AbstractFileObject implements FileObject {

    private URL url;

    protected UrlFileObject(final UrlFileSystem fs, final FileName fileName) {
        super(fileName, fs);
    }

    /**
     * Attaches this file object to its file resource.  This method is called
     * before any of the doBlah() or onBlah() methods.  Sub-classes can use
     * this method to perform lazy initialisation.
     */
    protected void doAttach() throws Exception {
        if (url == null) {
            url = createURL(getName());
        }
    }

    protected URL createURL(final FileName name) throws MalformedURLException, FileSystemException, URIException {
        if (name instanceof URLFileName) {
            URLFileName urlName = (URLFileName) getName();
            return new URL(urlName.getURIEncoded(null));
        }
        return new URL(getName().getURI());
    }

    /**
     * Determines the type of the file.
     */
    protected FileType doGetType() throws Exception {
        try {
            final URLConnection conn = url.openConnection();
            final InputStream in = conn.getInputStream();
            try {
                if (conn instanceof HttpURLConnection) {
                    final int status = ((HttpURLConnection) conn).getResponseCode();
                    if (HttpURLConnection.HTTP_OK != status) {
                        return FileType.IMAGINARY;
                    }
                }
                return FileType.FILE;
            } finally {
                in.close();
            }
        } catch (final FileNotFoundException e) {
            return FileType.IMAGINARY;
        }
    }

    /**
     * Returns the size of the file content (in bytes).
     */
    protected long doGetContentSize() throws Exception {
        final URLConnection conn = url.openConnection();
        final InputStream in = conn.getInputStream();
        try {
            return conn.getContentLength();
        } finally {
            in.close();
        }
    }

    /**
     * Returns the last modified time of this file.
     */
    protected long doGetLastModifiedTime() throws Exception {
        final URLConnection conn = url.openConnection();
        final InputStream in = conn.getInputStream();
        try {
            return conn.getLastModified();
        } finally {
            in.close();
        }
    }

    /**
     * Lists the children of the file.
     */
    protected String[] doListChildren() throws Exception {
        throw new FileSystemException("Not implemented.");
    }

    /**
     * Creates an input stream to read the file content from.
     */
    protected InputStream doGetInputStream() throws Exception {
        return url.openStream();
    }
}
