package org.dishevelled.piccolo.tilemap.io.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.dishevelled.piccolo.tilemap.AbstractTileMap;
import org.dishevelled.piccolo.tilemap.io.TileMapReader;

/**
 * Abstract tile map reader.
 *
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public abstract class AbstractTileMapReader implements TileMapReader {

    /** {@inheritDoc} */
    public final AbstractTileMap read(final File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return read(inputStream);
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(inputStream);
        }
    }

    /** {@inheritDoc} */
    public final AbstractTileMap read(final URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            return read(inputStream);
        } catch (IOException e) {
            throw e;
        } finally {
            closeQuietly(inputStream);
        }
    }

    /**
     * Close the specified input stream quietly.
     *
     * @param inputStream input stream to close
     */
    protected final void closeQuietly(final InputStream inputStream) {
        try {
            inputStream.close();
        } catch (Exception e) {
        }
    }
}
