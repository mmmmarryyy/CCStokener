package org.allcolor.alc.filesystem.url;

import org.allcolor.alc.filesystem.Directory;
import org.allcolor.alc.filesystem.FSUtils.DirectoryElement;
import org.allcolor.alc.filesystem.FileSystem;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author (Author)
 * @version $Revision$
  */
final class UrlDirectory extends DirectoryElement {

    /**
	 * DOCUMENT ME!
	 */
    protected final URL url;

    /**
	 * Creates a new UrlDirectory object.
	 *
	 * @param name DOCUMENT ME!
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    protected UrlDirectory(final String name, final URL url) throws IOException {
        super(name);
        this.url = url;
    }

    /**
	 * Creates a new UrlDirectory object.
	 *
	 * @param name DOCUMENT ME!
	 * @param fs DOCUMENT ME!
	 * @param parent DOCUMENT ME!
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    protected UrlDirectory(final String name, final FileSystem fs, final Directory parent, final URL url) throws IOException {
        super(fs, parent, name);
        this.url = url;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    public final boolean exists() {
        try {
            final URLConnection uc = this.url.openConnection();
            uc.connect();
            uc.getInputStream().close();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    public long getLastModifiedOn() {
        try {
            final URLConnection uc = this.url.openConnection();
            uc.connect();
            final long res = uc.getLastModified();
            try {
                uc.getInputStream().close();
            } catch (final Exception ignore) {
            }
            return res;
        } catch (final IOException e) {
            return 0;
        }
    }
}
