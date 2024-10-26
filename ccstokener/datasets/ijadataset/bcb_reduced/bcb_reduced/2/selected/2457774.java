package org.allcolor.alc.filesystem.stream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author (Author)
 * @version $Revision$
  */
public final class URLInputStream extends FilterInputStream {

    /**
	 * Creates a new URLInputStream object.
	 *
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public URLInputStream(final URL url) throws IOException {
        this(url.openConnection().getInputStream());
    }

    /**
	 * Creates a new URLInputStream object.
	 *
	 * @param url DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public URLInputStream(final String url) throws IOException {
        this(new URL(url));
    }

    /**
	 * Creates a new URLInputStream object.
	 *
	 * @param in DOCUMENT ME!
	 */
    private URLInputStream(final InputStream in) {
        super(in);
    }
}
