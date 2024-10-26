package org.yawni.wordnet;

import com.google.common.collect.Lists;
import org.yawni.util.CharSequences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import org.yawni.util.LightImmutableList;

/**
 * An implementation of {@code FileManagerInterface} that reads WordNet data
 * from jar files or the local file system.  A {@code FileManager} caches the
 * file positions before and after {@link FileManagerInterface#readLineAt}
 * in order to eliminate the redundant I/O activity that a naïve implementation
 * of these methods would necessitate.
 *
 * <p> Instances of this class are guarded; all operations are read-only, but
 * are synchronized per file to maintain state including the file pointers'
 * position.
 */
final class FileManager implements FileManagerInterface {

    private static final Logger log = LoggerFactory.getLogger(FileManager.class.getName());

    private final Map<String, CharStream> fileNameCache = new HashMap<String, CharStream>();

    static class NextLineOffsetCache {

        private String fileName;

        private int previous;

        private int next;

        /**
     * synchronization keeps this consistent since multiple fileName's may call
     * this at the same time
     */
        synchronized void setNextLineOffset(final String fileName, final int previous, final int next) {
            this.fileName = fileName;
            this.previous = previous;
            this.next = next;
        }

        /**
     * synchronization keeps this consistent since multiple fileName's may call
     * this at the same time
     */
        synchronized int matchingOffset(final String fileName, final int offset) {
            if (this.fileName == null || previous != offset || !this.fileName.equals(fileName)) {
                return -1;
            } else {
                return next;
            }
        }
    }

    private final NextLineOffsetCache nextLineOffsetCache = new NextLineOffsetCache();

    /** FIXME
   * Construct a {@code FileManager} backed by a set of files contained in the default WordNet search directory.
   * The default search directory is the location named by the system property {@code $WNSEARCHDIR}; or, if this
   * is undefined, by the directory named {@code $WNHOME/dict}.
   */
    public FileManager() {
    }

    /**
   * Directory which contains all WordNet data files defined by {@code $WNSEARCHDIR}.
   * The {@code WNsearchDir} is typically {@code $WNHOME/dict}
   * @see #getFileStream(String, boolean)
   */
    private static String getWNSearchDir() {
        final String searchDir = getValidatedPathNamed("WNSEARCHDIR");
        if (searchDir != null) {
            return searchDir;
        }
        final String wnHome = getValidatedPathNamed("WNHOME");
        String generatedSearchDir = null;
        if (wnHome != null) {
            generatedSearchDir = wnHome + File.separator + "dict/";
            if (!isReadableFile(generatedSearchDir)) {
                generatedSearchDir = null;
            }
        }
        return generatedSearchDir;
    }

    /**
   * Searches an environment variable and then a Java System Property 
   * named {@code propname} and if its value refers to a readable file,
   * returns that path, otherwise returns {@code null}.
   */
    static String getValidatedPathNamed(final String propName) {
        try {
            String path;
            path = System.getenv(propName);
            if (isReadableFile(path)) {
                return path;
            } else {
                path = System.getProperty(propName);
                if (isReadableFile(path)) {
                    return path;
                }
            }
        } catch (SecurityException ex) {
            log.debug("need plan B due to: {}", ex);
            return null;
        }
        return null;
    }

    static boolean isReadableFile(final String path) {
        File file;
        return path != null && (file = new File(path)).exists() && file.canRead();
    }

    /**
   * Primary abstraction of file content used in {@code FileManager}.
   * NOTE: CharStream is stateful (i.e., not thread-safe)
   */
    abstract static class CharStream implements CharSequence {

        protected final String fileName;

        protected final StringBuilder stringBuffer;

        /** Force subclasses to call this */
        CharStream(final String fileName) {
            this.fileName = fileName;
            this.stringBuffer = new StringBuilder();
        }

        abstract void seek(final int position) throws IOException;

        abstract int position() throws IOException;

        public abstract char charAt(int position);

        public abstract int length();

        public CharSequence subSequence(int s, int e) {
            final boolean doBuffer = true;
            resetBuffer(doBuffer);
            for (int i = s; i < e; i++) {
                stringBuffer.append(charAt(i));
            }
            return stringBuffer.toString();
        }

        /**
     * This works just like {@link RandomAccessFile#readLine} -- doesn't
     * support Unicode.
     */
        abstract String readLine() throws IOException;

        void skipLine() throws IOException {
            readLine();
        }

        String readLineWord() throws IOException {
            final String ret = readLine();
            if (ret == null) {
                return null;
            }
            final int space = ret.indexOf(' ');
            assert space >= 0;
            return ret.substring(0, space);
        }

        /**
     * Treat file contents like an array of lines and return the zero-based,
     * inclusive line corresponding to {@code linenum}
     */
        String readLineNumber(int linenum) throws IOException {
            seek(0);
            for (int i = 0; i < linenum; i++) {
                skipLine();
            }
            return readLine();
        }

        protected void resetBuffer(final boolean doBuffer) {
            if (doBuffer) {
                stringBuffer.setLength(0);
            }
        }
    }

    /**
   * {@link RandomAccessFile}-backed {@code CharStream} implementation.  This {@code CharStream}
   * has the minimum boot time (and the slowest access times).
   */
    static class RAFCharStream extends CharStream {

        private final RandomAccessFile raf;

        RAFCharStream(final String fileName, final RandomAccessFile raf) {
            super(fileName);
            this.raf = raf;
        }

        @Override
        void seek(final int position) throws IOException {
            raf.seek(position);
        }

        @Override
        int position() throws IOException {
            return (int) raf.getFilePointer();
        }

        @Override
        public char charAt(int position) {
            try {
                seek(position);
                return (char) raf.readByte();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public int length() {
            try {
                return (int) raf.length();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        String readLine() throws IOException {
            return raf.readLine();
        }
    }

    /**
   * {@link ByteBuffer} {@code CharStream} implementation.
   * This {@code CharStream} is boots very quickly (little slower than
   * {@code RAFCharStream}) and provides very fast access times, however it
   * requires a {@code ByteBuffer} which is usually most easily derived
   * from an {@code FileChannel}. aka {@code mmap CharStream}
   */
    private static class NIOCharStream extends CharStream implements CharSequence {

        private int position;

        private final ByteBuffer bbuff;

        private final int capacity;

        NIOCharStream(final String fileName, final ByteBuffer bbuff) throws IOException {
            super(fileName);
            this.bbuff = bbuff;
            this.capacity = bbuff.capacity();
        }

        NIOCharStream(final String fileName, final RandomAccessFile raf) throws IOException {
            this(fileName, asByteBuffer(raf));
        }

        private static ByteBuffer asByteBuffer(final RandomAccessFile raf) throws IOException {
            final FileChannel fileChannel = raf.getChannel();
            final long size = fileChannel.size();
            final MappedByteBuffer mmap = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            return mmap;
        }

        @Override
        void seek(final int position) throws IOException {
            this.position = position;
        }

        @Override
        int position() {
            return position;
        }

        @Override
        public char charAt(final int p) {
            return (char) bbuff.get(p);
        }

        @Override
        public int length() {
            return capacity;
        }

        @Override
        String readLine() throws IOException {
            final int s = position;
            final int e = scanForwardToLineBreak(true);
            if ((e - s) <= 0) {
                return null;
            }
            return stringBuffer.toString();
        }

        @Override
        void skipLine() throws IOException {
            scanForwardToLineBreak();
        }

        @Override
        String readLineWord() throws IOException {
            final int s = position;
            bufferUntilSpace();
            final int e = scanForwardToLineBreak();
            if ((e - s) <= 0) {
                return null;
            }
            return stringBuffer.toString();
        }

        /** Modifies {@code position} field */
        private int bufferUntilSpace() {
            resetBuffer(true);
            char c;
            while (position < capacity) {
                c = charAt(position++);
                if (c == ' ') {
                    return position - 1;
                }
                stringBuffer.append(c);
            }
            return bbuff.capacity();
        }

        private int scanForwardToLineBreak() {
            return scanForwardToLineBreak(false);
        }

        /**
     * Returns exclusive offset of next start of the line delimiter, or capacity, whichever comes next.
     * Modifies {@code position} field, leaving it at start of following line.
     *
     * Typical usage is to store current position, s, and e = scanForwardToLineBreak(), line content
     * is buffer[s, e).  This can be done repeatedly to get sequential lines.
     */
        private int scanForwardToLineBreak(final boolean doBuffer) {
            boolean done = false;
            boolean crnl = false;
            resetBuffer(doBuffer);
            char c;
            while (!done && position < capacity) {
                c = charAt(position++);
                switch(c) {
                    case '\r':
                        c = charAt(position++);
                        if (c != '\n') {
                            position--;
                        } else {
                            crnl = true;
                        }
                        done = true;
                        break;
                    case '\n':
                        done = true;
                        break;
                    default:
                        if (doBuffer) {
                            stringBuffer.append(c);
                        }
                }
            }
            return crnl ? position - 2 : position - 1;
        }
    }

    /**
   * Fast {@code CharStream} created from InputStream (e.g., can be read from jar file)
   * backed by a byte[].  This {@code CharStream} is slowest to boot
   * but provides very fast access times.
   */
    private static class InputStreamCharStream extends NIOCharStream {

        /**
     * @param fileName interpretted as classpath relative path
     * @param input
     * @param len the number of bytes in this input stream.  Allows stream to be drained into exactly 
     * 1 buffer thus maximizing efficiency.
     */
        InputStreamCharStream(final String fileName, final InputStream input, final int len) throws IOException {
            super(fileName, asByteBuffer(input, len, fileName));
        }

        /**
     * @param input
     * @param len the number of bytes in this input stream.  Allows stream to be drained into exactly 
     * 1 buffer thus maximizing efficiency.
     * @param fileName
     */
        private static ByteBuffer asByteBuffer(final InputStream input, final int len, final String fileName) throws IOException {
            if (len == -1) {
                throw new RuntimeException("unknown length not currently supported");
            }
            final byte[] buffer = new byte[len];
            int totalBytesRead = 0;
            int bytesRead;
            while ((bytesRead = input.read(buffer, totalBytesRead, len - totalBytesRead)) > 0) {
                totalBytesRead += bytesRead;
            }
            if (len != totalBytesRead) {
                throw new RuntimeException("Read error. Only read " + totalBytesRead + " of " + len + " for " + fileName);
            }
            return ByteBuffer.wrap(buffer);
        }
    }

    private long streamInitTime;

    /**
   * @param fileName
   * @param fileNameIsWnRelative is a boolean which indicates that {@code fileName}
   * is relative (else, it's absolute); this facilitates testing and reuse.
   * @return CharStream representing {@code fileName} or null if no such file exists.
   */
    private synchronized CharStream getFileStream(final String fileName, final boolean fileNameIsWnRelative) throws IOException {
        CharStream stream = fileNameCache.get(fileName);
        if (stream == null) {
            final long start = System.nanoTime();
            stream = getURLStream(fileName);
            if (stream != null) {
                log.trace("URLCharStream: {}", stream);
            } else {
                final String pathname = fileNameIsWnRelative ? getWNSearchDir() + File.separator + fileName : fileName;
                log.trace("fileName: {} pathname: {}", fileName, pathname);
                final File file = new File(pathname);
                log.debug("pathname: {}", pathname);
                if (file.exists() && file.canRead()) {
                    stream = new NIOCharStream(pathname, new RandomAccessFile(file, "r"));
                    log.trace("FileCharStream");
                }
            }
            final long duration = System.nanoTime() - start;
            final long total = streamInitTime += duration;
            log.debug(String.format("total: %,dns curr: %,dns", total, duration));
            fileNameCache.put(fileName, stream);
        }
        return stream;
    }

    synchronized CharStream getFileStream(final String fileName) throws IOException {
        return getFileStream(fileName, true);
    }

    /**
   * Interpret {@code resourcename} as a classpath-relative URL.
   * @param resourcename
   * @return CharStream corresponding to resourcename
   */
    private synchronized CharStream getURLStream(String resourcename) throws IOException {
        resourcename = "dict/" + resourcename;
        final URL url = getClass().getClassLoader().getResource(resourcename);
        if (url == null) {
            log.debug("resourcename: {} not found in classpath", resourcename);
            return null;
        }
        final URLConnection conn = url.openConnection();
        final int len;
        if (conn instanceof JarURLConnection) {
            final JarURLConnection juc = (JarURLConnection) conn;
            len = (int) juc.getJarEntry().getSize();
        } else {
            len = conn.getContentLength();
        }
        final InputStream input = conn.getInputStream();
        return new InputStreamCharStream(resourcename, input, len);
    }

    private void requireStream(final CharStream stream, final String fileName) {
        if (stream == null) {
            throw new IllegalStateException("Yawni can't open '" + fileName + "'. Yawni needs either the yawni-data jar in the classpath, or correctly defined " + " $WNSEARCHDIR or $WNHOME environment variable or system property referencing the WordNet data.");
        }
    }

    /**
   * {@inheritDoc}
   */
    public String readLineNumber(final int linenum, final String fileName) throws IOException {
        final CharStream stream = getFileStream(fileName);
        if (stream == null) {
            return null;
        }
        synchronized (stream) {
            return stream.readLineNumber(linenum);
        }
    }

    /**
   * {@inheritDoc}
   * Core search routine.  Only called from within synchronized blocks.
   */
    public String readLineAt(final int offset, final String fileName) throws IOException {
        final CharStream stream = getFileStream(fileName);
        requireStream(stream, fileName);
        synchronized (stream) {
            stream.seek(offset);
            final String line = stream.readLine();
            int nextOffset = stream.position();
            if (line == null) {
                nextOffset = -1;
            }
            nextLineOffsetCache.setNextLineOffset(fileName, offset, nextOffset);
            return line;
        }
    }

    /**
   * {@inheritDoc}
   * Core search routine.  Only called from within synchronized blocks.
   */
    public int getNextLinePointer(final int offset, final String fileName) throws IOException {
        final CharStream stream = getFileStream(fileName);
        requireStream(stream, fileName);
        synchronized (stream) {
            final int next;
            if (0 <= (next = nextLineOffsetCache.matchingOffset(fileName, offset))) {
                return next;
            }
            stream.seek(offset);
            stream.skipLine();
            return stream.position();
        }
    }

    private static final String TWO_SPACES = "  ";

    public int getMatchingLinePointer(int offset, final Matcher matcher, final String fileName) throws IOException {
        if (matcher.pattern().pattern().length() == 0) {
            return -1;
        }
        final CharStream stream = getFileStream(fileName);
        requireStream(stream, fileName);
        synchronized (stream) {
            stream.seek(offset);
            do {
                final String word = stream.readLineWord();
                final int nextOffset = stream.position();
                if (word == null) {
                    return -1;
                }
                nextLineOffsetCache.setNextLineOffset(fileName, offset, nextOffset);
                if (matcher.reset(word).find()) {
                    return offset;
                }
                offset = nextOffset;
            } while (true);
        }
    }

    public int getPrefixMatchLinePointer(int offset, final CharSequence prefix, final String fileName) throws IOException {
        if (prefix.length() == 0) {
            return -1;
        }
        final int foffset = getIndexedLinePointer(prefix, offset, fileName, true);
        final int zoffset;
        if (foffset < 0) {
            final int moffset = -(foffset + 1);
            final String aline = readLineAt(moffset, fileName);
            if (aline == null || !CharSequences.startsWith(aline, prefix)) {
                zoffset = foffset;
            } else {
                zoffset = moffset;
            }
        } else {
            zoffset = foffset;
        }
        return zoffset;
    }

    int oldGetPrefixMatchLinePointer(int offset, final CharSequence prefix, final String fileName) throws IOException {
        if (prefix.length() == 0) {
            return -1;
        }
        final CharStream stream = getFileStream(fileName);
        final int origOffset = offset;
        synchronized (stream) {
            stream.seek(offset);
            do {
                final String word = stream.readLineWord();
                final int nextOffset = stream.position();
                if (word == null) {
                    return -1;
                }
                nextLineOffsetCache.setNextLineOffset(fileName, offset, nextOffset);
                if (CharSequences.startsWith(word, prefix)) {
                    if (!checkPrefixBinarySearch(prefix, origOffset, fileName)) {
                        throw new IllegalStateException("search failed for prefix: " + prefix + " fileName: " + fileName);
                    }
                    return offset;
                }
                offset = nextOffset;
            } while (true);
        }
    }

    private boolean checkPrefixBinarySearch(final CharSequence prefix, final int offset, final String fileName) throws IOException {
        final int foffset = getIndexedLinePointer(prefix, offset, fileName, true);
        final String aline;
        if (foffset < 0) {
            final int moffset = -(foffset + 1);
            aline = readLineAt(moffset, fileName);
        } else {
            aline = readLineAt(foffset, fileName);
        }
        return aline != null && CharSequences.startsWith(aline, prefix);
    }

    /**
   * {@inheritDoc}
   */
    public int getIndexedLinePointer(final CharSequence target, final String fileName) throws IOException {
        return getIndexedLinePointer(target, 0, fileName, true);
    }

    /**
   * {@inheritDoc}
   */
    public int getIndexedLinePointer(final CharSequence target, int start, final String fileName, final boolean fileNameWnRelative) throws IOException {
        if (target.length() == 0) {
            return -1;
        }
        if (log.isTraceEnabled()) {
            log.trace("target: " + target + " fileName: " + fileName);
        }
        final CharStream stream = getFileStream(fileName, fileNameWnRelative);
        requireStream(stream, fileName);
        synchronized (stream) {
            int stop = stream.length();
            while (true) {
                final int midpoint = (start + stop) / 2;
                stream.seek(midpoint);
                stream.skipLine();
                final int offset = stream.position();
                if (log.isTraceEnabled()) {
                    log.trace("  " + start + ", " + midpoint + ", " + stop + " → " + offset);
                }
                if (offset == start) {
                    return -start - 1;
                } else if (offset == stop) {
                    if (start != 0 && stream.charAt(start - 1) != '\n') {
                        stream.seek(start + 1);
                        stream.skipLine();
                    } else {
                        stream.seek(start);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace(". " + stream.position());
                    }
                    while (stream.position() < stop) {
                        final int result = stream.position();
                        final CharSequence word = stream.readLineWord();
                        if (log.isTraceEnabled()) {
                            log.trace("  . \"" + word + "\" → " + (0 == compare(target, word)));
                        }
                        final int compare = compare(target, word);
                        if (compare == 0) {
                            return result;
                        } else if (compare < 0) {
                            return -result - 1;
                        }
                    }
                    return -stop - 1;
                }
                final int result = stream.position();
                final CharSequence word = stream.readLineWord();
                final int compare = compare(target, word);
                if (log.isTraceEnabled()) {
                    log.trace(word + ": " + compare);
                }
                if (compare == 0) {
                    return result;
                }
                if (compare > 0) {
                    start = offset;
                } else {
                    assert compare < 0;
                    stop = offset;
                }
            }
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Iterable<CharSequence> getMatchingLines(final CharSequence target, final String fileName) throws IOException {
        if (target.length() == 0) {
            return LightImmutableList.of();
        }
        final char last = target.charAt(target.length() - 1);
        final char prev = (char) (last - 1);
        String q = target.toString().substring(0, target.length() - 1) + prev;
        final boolean fileNameWnRelative = false;
        final int i = getIndexedLinePointer(q, 0, fileName, fileNameWnRelative);
        int idx;
        if (i >= 0) {
            idx = i;
        } else {
            idx = -i - 1;
        }
        CharSequence line = readLineAt(idx, fileName);
        final List<CharSequence> matches = Lists.newArrayList();
        int j = idx;
        if (i < 0) {
            assert line == null || !CharSequences.startsWith(line, q);
        } else {
            assert i >= 0;
            while (line != null && CharSequences.startsWith(line, q)) {
                j += (line.length() + 1);
                line = readLineAt(j, fileName);
            }
        }
        while (line != null && CharSequences.startsWith(line, target)) {
            matches.add(line);
            j += (line.length() + 1);
            line = readLineAt(j, fileName);
        }
        return matches;
    }

    /**
   * {@inheritDoc}
   * Note this is a covariant implementation of {@link java.util.Comparator Comparator<CharSequence>}
   */
    public WordNetLexicalComparator comparator() {
        return WordNetLexicalComparator.TO_LOWERCASE_INSTANCE;
    }

    private int compare(final CharSequence s1, final CharSequence s2) {
        return comparator().compare(s1, s2);
    }
}
