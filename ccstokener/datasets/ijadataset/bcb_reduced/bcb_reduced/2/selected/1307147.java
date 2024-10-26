package org.allcolor.alc.filesystem.stream;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

/**
 * @author (Author)
 * @version $Revision$
  */
public final class URLOutputStream extends FilterOutputStream {

    /**
	 * Creates a new URLOutputStream object.
	 *
	 * @param out DOCUMENT ME!
	 */
    public URLOutputStream(final OutputStream out) {
        super(out);
    }

    /**
	 * Creates a new URLOutputStream object.
	 *
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public URLOutputStream(final URL url) throws IOException {
        this(url.openConnection().getOutputStream());
    }

    /**
	 * Creates a new URLOutputStream object.
	 *
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public URLOutputStream(final String url) throws IOException {
        this(new URL(url));
    }
}
