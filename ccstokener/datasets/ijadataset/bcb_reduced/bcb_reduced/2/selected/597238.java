package com.mgensystems.jarindexer.model.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import com.mgensystems.jarindexer.model.index.IndexEntry;
import com.mgensystems.jarindexer.model.index.IndexSource;

/**
 * <b>Title:</b>URL Source <br />
 * <b>Description:</b>A source which will download the target url to provide
 * index entries.  This class is *not* thread-safe. <br />
 * <b>Changes:</b><li></li>
 * 
 * @author raykroeker@gmail.com
 */
public final class URLSource implements IndexSource {

    /** A download buffer. */
    private static final byte[] buffer;

    static {
        buffer = new byte[1024 * 1024];
    }

    /** The downloaded file. */
    private File file;

    /** A wrapped file source. */
    private IndexSource source;

    /** The url. */
    private final URL url;

    /**
	 * Create URLSource.
	 * 
	 * @param url
	 *            A <code>URL</code>.
	 */
    public URLSource(final URL url) {
        super();
        this.url = url;
    }

    /**
	 * @see com.mgensystems.jarindexer.model.index.IndexSource#close()
	 *
	 */
    @Override
    public void close() throws IOException {
        try {
            source.close();
        } finally {
            if (file.exists()) {
                if (false == file.delete()) {
                    throw new IOException("Cannot delete:" + file);
                }
            }
        }
    }

    /**
	 * @see com.mgensystems.jarindexer.model.index.IndexSource#getLocation()
	 *
	 */
    @Override
    public String getLocation() {
        return url.toString();
    }

    /**
	 * @see com.mgensystems.jarindexer.model.index.IndexSource#iterator()
	 *
	 */
    @Override
    public Iterator<IndexEntry> iterator() throws IOException {
        return source.iterator();
    }

    /**
	 * @see com.mgensystems.jarindexer.model.index.IndexSource#open()
	 *
	 */
    @Override
    public void open() throws IOException {
        file = File.createTempFile(System.getProperty("user.name"), System.getProperty("application.simplename"));
        final InputStream input = url.openStream();
        try {
            final OutputStream output = new FileOutputStream(file);
            try {
                int bytes = input.read(buffer);
                while (-1 != bytes) {
                    output.write(buffer, 0, bytes);
                    output.flush();
                    bytes = input.read(buffer);
                }
            } finally {
                output.close();
            }
        } finally {
            input.close();
        }
        source = new FileSource(IndexEntryBuilder.newBuilder().setSource(url), file);
        source.open();
    }
}
