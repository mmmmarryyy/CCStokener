package org.hsqldb.jdbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.NoSuchElementException;
import org.hsqldb.lib.Iterator;

/**
 * Retrieves line-oriented, semicolon terminated character sequences
 * from a BufferedReader or URL.
 *
 * Ignores lines starting with '//' and '--', as well as lines consisting only
 * of whitespace.
 *
 * @author boucherb@users
 */
public class ScriptIterator implements Iterator {

    private static final String SLASH_COMMENT = "//";

    private static final String DASH_COMMENT = "--";

    private static final String SEMI = ";";

    private String segment;

    private BufferedReader reader;

    /**
     * Constructs a new ScriptIterator.
     *
     * @param reader from which to read SQL statements
     */
    public ScriptIterator(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Constructs a new ScriptIterator.
     *
     * @param url from which to read SQL statements
     */
    public ScriptIterator(URL url) throws IOException {
        this(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Retrieves whether there is an SQL segment available.
     *
     *
     * @return true if there is an SQL segment available
     * @throws java.lang.RuntimeException if an internal IOException occurs
     */
    public boolean hasNext() throws RuntimeException {
        String line;
        StringBuffer sb;
        if (this.reader == null) {
            return false;
        } else if (this.segment == null) {
            sb = null;
            line = null;
            while (true) {
                try {
                    line = this.reader.readLine();
                } catch (IOException ioe) {
                    this.reader = null;
                    throw new RuntimeException(ioe);
                }
                if (line == null) {
                    this.reader = null;
                    break;
                } else if (line.trim().length() == 0) {
                    continue;
                } else if (line.trim().startsWith(SLASH_COMMENT)) {
                    continue;
                } else if (line.trim().startsWith(DASH_COMMENT)) {
                    continue;
                } else {
                    if (sb == null) {
                        sb = new StringBuffer();
                    }
                    sb.append(line);
                }
                if (line.trim().endsWith(SEMI)) {
                    this.segment = sb.toString();
                    break;
                }
            }
        }
        return (this.segment != null);
    }

    /**
     * Retrieves the next available SQL segment as a String.
     *
     *
     * @return the next available SQL segment
     * @throws java.util.NoSuchElementException if there is
     * 	    no available SQL segment
     */
    public Object next() throws NoSuchElementException {
        String out = null;
        if (this.hasNext()) {
            out = this.segment;
            this.segment = null;
        }
        if (out == null) {
            throw new NoSuchElementException();
        }
        return out;
    }

    /**
     * Unsupported.
     *
     * @throws java.lang.UnsupportedOperationException always
     */
    public int nextInt() throws NoSuchElementException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported.
     *
     * @throws java.lang.UnsupportedOperationException always
     */
    public long nextLong() throws NoSuchElementException {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported.
     *
     * @throws java.lang.UnsupportedOperationException always
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
