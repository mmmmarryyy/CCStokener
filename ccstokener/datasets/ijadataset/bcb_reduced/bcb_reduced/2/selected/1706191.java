package org.allcolor.alc.utils.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import org.allcolor.alc.filesystem.File;

/**
 * 
 DOCUMENT ME!
 * 
 * @author Quentin Anciaux
 * @version 0.1.0
 */
public class ReaderUtils {

    /**
	 * @author Quentin Anciaux
	 * @version 0.1.0
	 */
    public static interface LineListener {

        /**
		 * DOCUMENT ME!
		 * 
		 * @param ioe
		 *            DOCUMENT ME!
		 */
        public abstract void exception(final IOException ioe);

        /**
		 * DOCUMENT ME!
		 * 
		 * @param line
		 *            DOCUMENT ME!
		 * 
		 * @return DOCUMENT ME!
		 * 
		 * @throws IOException
		 *             DOCUMENT ME!
		 */
        public abstract boolean line(final String line) throws IOException;
    }

    public abstract static class LineListenerImpl implements LineListener {

        protected final StringBuilder internalBuffer = new StringBuilder();

        public void exception(final IOException ioe) {
            throw new RuntimeException(ioe);
        }

        @Override
        public String toString() {
            return this.internalBuffer.toString();
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param file
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final File file, final LineListener lit) {
        try {
            ReaderUtils.forEachLine(file.getInputStream(), lit);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param file
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 * @param encoding
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final File file, final LineListener lit, final String encoding) {
        try {
            ReaderUtils.forEachLine(file.getInputStream(), lit, encoding);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param in
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final InputStream in, final LineListener lit) {
        ReaderUtils.forEachLine(in, lit, "utf-8");
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param in
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 * @param encoding
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final InputStream in, final LineListener lit, final String encoding) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!lit.line(line)) {
                    break;
                }
            }
        } catch (final IOException ioe) {
            lit.exception(ioe);
        } finally {
            try {
                reader.close();
            } catch (final Exception ignore) {
                ;
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param file
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final java.io.File file, final LineListener lit) {
        try {
            ReaderUtils.forEachLine(new FileInputStream(file), lit);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param file
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 * @param encoding
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final java.io.File file, final LineListener lit, final String encoding) {
        try {
            ReaderUtils.forEachLine(new FileInputStream(file), lit, encoding);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param url
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final URL url, final LineListener lit) {
        try {
            ReaderUtils.forEachLine(url.openStream(), lit);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }

    /**
	 * DOCUMENT ME!
	 * 
	 * @param url
	 *            DOCUMENT ME!
	 * @param lit
	 *            DOCUMENT ME!
	 * @param encoding
	 *            DOCUMENT ME!
	 */
    public static void forEachLine(final URL url, final LineListener lit, final String encoding) {
        try {
            ReaderUtils.forEachLine(url.openStream(), lit);
        } catch (final IOException ioe) {
            lit.exception(ioe);
        }
    }
}
