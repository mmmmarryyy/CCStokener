package org.dishevelled.matrix.io.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.dishevelled.matrix.Matrix2D;
import org.dishevelled.matrix.io.Matrix2DReader;

/**
 * Abstract reader for matrices of objects in two dimensions.
 *
 * @param <E> 2D matrix element type
 * @author  Michael Heuer
 * @version $Revision$ $Date$
 */
public abstract class AbstractMatrix2DReader<E> implements Matrix2DReader<E> {

    /**
     * Parse the specified value to an instance of type <code>E</code>.
     *
     * @param value value to parse
     * @return an instance of type <code>E</code>
     * @throws IOException if an IO error occurs
     */
    protected abstract E parse(String value) throws IOException;

    /**
     * Create and return a new instance of an implementation of Matrix2D.
     *
     * @param rows number of rows
     * @param columns number of columns
     * @param cardinality approximate cardinality
     * @return a new instance of an implementation of Matrix2D
     */
    protected abstract Matrix2D<E> createMatrix2D(long rows, long columns, int cardinality);

    /** {@inheritDoc} */
    public final Matrix2D<E> read(final File file) throws IOException {
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
            MatrixIOUtils.closeQuietly(inputStream);
        }
    }

    /** {@inheritDoc} */
    public final Matrix2D<E> read(final URL url) throws IOException {
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
            MatrixIOUtils.closeQuietly(inputStream);
        }
    }
}
