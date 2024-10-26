package org.tm4j.topicmap.cmd;

import org.tm4j.net.Locator;
import org.tm4j.net.LocatorFactory;
import org.tm4j.net.LocatorFactoryException;
import org.tm4j.topicmap.source.SerializedTopicMapSource;
import org.tm4j.topicmap.source.TopicMapSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.EOFException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a list of strings from a command line
 * into a set of topic map source/base URI pairs
 * In the list of strings, the first list item is
 * treated as a topic map source address and is parsed
 * to either a file name or a URL. Then, if the following
 * item in the list matches the short or long base uri option
 * strings, then the string following that is parsed as a Locator
 * and set as the base URI for the topic map. If the item
 * following the topic map source address does not match the short or
 * the long base uri option strings, then the string is treated as
 * the address of another topic map and the process starts again
 * with this entry.
 */
public class TopicMapList {

    private String m_shortArg;

    private String m_longArg;

    /** to minimize filedescriptor usage */
    protected static final boolean useOnDemandInputStreams = true;

    public TopicMapList(char shortArg, String longArg) {
        m_shortArg = "-" + shortArg;
        m_longArg = "--" + longArg;
    }

    /**
     * Parses <code>args</code> into a list of
     * topic map address, base URI pairs.
     * @param args The command line arguments to be parsed
     * @returns A List of {@link TopicMapList.SourceAddressPair} objects.
     */
    public List parse(String[] args, LocatorFactory locFactory) throws ParseException {
        ArrayList ret = new ArrayList();
        int ix = 0;
        while (ix < args.length) {
            String tmSource = args[ix];
            Locator base = null;
            ix = ix + 1;
            if (args.length > ix) {
                if (args[ix].equals(m_shortArg) || args[ix].equals(m_longArg)) {
                    if (args.length == (ix + 1)) {
                        throw new TopicMapList.ParseException("Expected a URI to follow " + m_shortArg + " or " + m_longArg);
                    }
                    try {
                        base = locFactory.createLocator("URI", args[ix + 1]);
                    } catch (LocatorFactoryException ex) {
                        throw new ParseException("Could not parse base URL '" + args[ix + 1] + "' - " + ex.toString());
                    }
                    ix = ix + 2;
                }
            }
            TopicMapSource entry = parseSourceAddress(tmSource, base, locFactory);
            ret.add(entry);
        }
        return ret;
    }

    public TopicMapSource parseSourceAddress(String source, Locator base, LocatorFactory locFactory) throws ParseException {
        InputStream tmStream = null;
        Locator tmBase = base;
        File sourceFile = new File(source);
        if (sourceFile.exists()) {
            try {
                if (useOnDemandInputStreams) {
                    tmStream = new OnDemandFileInputStream(sourceFile);
                } else {
                    tmStream = new FileInputStream(sourceFile);
                }
                if (tmBase == null) {
                    tmBase = locFactory.createLocator("URI", sourceFile.toURL().toString());
                }
            } catch (FileNotFoundException ex) {
            } catch (MalformedURLException ex) {
                throw new ParseException("Error in converting file name '" + sourceFile.getPath() + "' to a URL: " + ex.getMessage());
            } catch (LocatorFactoryException ex) {
                throw new ParseException("Error in convering URL for file name '" + sourceFile.getPath() + "' into a TM4J Locator. " + ex.getMessage());
            }
        }
        if (tmStream == null) {
            try {
                URL url = new URL(source);
                if (useOnDemandInputStreams) {
                    tmStream = new OnDemandURLInputStream(url);
                } else {
                    tmStream = url.openStream();
                }
                if (tmBase == null) {
                    tmBase = locFactory.createLocator("URI", url.toString());
                }
            } catch (MalformedURLException ex) {
                throw new ParseException("Could not convert '" + source + "' to a local file name or to a URL.", ex);
            } catch (IOException ex) {
                throw new ParseException("Error opening URL '" + source + "'. " + ex.getMessage());
            } catch (LocatorFactoryException ex) {
                throw new ParseException("Error convering URL '" + source + "' to a TM4J Locator." + ex.getMessage());
            }
        }
        if (tmStream == null) {
            throw new ParseException("Unable to parse string '" + source + "' as either a file name or a URL");
        }
        if (tmBase == null) {
            throw new ParseException("Unable to extract a base URI for source '" + source + "'");
        }
        return new SerializedTopicMapSource(tmStream, tmBase);
    }

    public class ParseException extends Exception {

        ParseException(String msg) {
            super(msg);
        }

        ParseException(String msg, Exception cause) {
            super(msg, cause);
        }
    }

    public class OnDemandURLInputStream extends OnDemandInputStream {

        URL url;

        public OnDemandURLInputStream(URL url) {
            this.url = url;
        }

        protected InputStream internalOpen() throws IOException {
            return url.openStream();
        }
    }

    public class OnDemandFileInputStream extends OnDemandInputStream {

        File file;

        public OnDemandFileInputStream(File file) {
            this.file = file;
        }

        protected InputStream internalOpen() throws IOException {
            return new FileInputStream(file);
        }
    }

    public abstract class OnDemandInputStream extends FilterInputStream {

        boolean alreadyOpened;

        boolean alreadyClosed;

        public OnDemandInputStream() {
            super(null);
        }

        protected void ensureOpen() throws IOException {
            if (!alreadyOpened && !alreadyClosed) {
                in = internalOpen();
                alreadyOpened = true;
            }
        }

        protected abstract InputStream internalOpen() throws IOException;

        protected void internalClose() throws IOException {
            if (alreadyOpened) {
                if (!alreadyClosed) {
                    alreadyClosed = true;
                    in.close();
                }
            }
        }

        protected void handlePossibleEOF(int readReturn) throws IOException {
            if (readReturn == -1) {
                handleEOF();
            }
        }

        protected void handleEOF() throws IOException {
            internalClose();
        }

        public int available() throws IOException {
            ensureOpen();
            return super.available();
        }

        public long skip(long n) throws IOException {
            ensureOpen();
            return super.skip(n);
        }

        public int read() throws IOException {
            ensureOpen();
            try {
                int result = super.read();
                handlePossibleEOF(result);
                return result;
            } catch (EOFException e) {
                handleEOF();
                throw e;
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            ensureOpen();
            try {
                int result = super.read(b, off, len);
                handlePossibleEOF(result);
                return result;
            } catch (EOFException e) {
                handleEOF();
                throw e;
            }
        }

        public boolean markSupported() {
            return false;
        }

        public void mark() {
        }

        public void reset() throws IOException {
            throw new IOException("mark() not supported.");
        }

        public void close() throws IOException {
            internalClose();
        }
    }
}
