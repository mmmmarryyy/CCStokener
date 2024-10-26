package edu.columbia.filesystem.impl;

import edu.columbia.filesystem.File;
import edu.columbia.filesystem.IDataLoader;
import edu.columbia.filesystem.FileSystemException;
import java.io.InputStream;
import java.net.URL;
import edu.columbia.filesystem.IFileSystemConstants;

/**
 * Part of the Columbia University Filing System
 *
 * @author Alex Vigdor av317@columbia.edu
 * @version $Revision: 1.1.1.1 $
 */
public class URLDataLoader implements IDataLoader, IFileSystemConstants {

    protected final URL url;

    public URLDataLoader(URL url) {
        this.url = url;
    }

    public InputStream getInputStream() throws FileSystemException {
        InputStream stream;
        try {
            stream = url.openStream();
        } catch (java.io.IOException ioe) {
            throw new FileSystemException(X_IO_ERROR, ioe);
        }
        return stream;
    }
}
