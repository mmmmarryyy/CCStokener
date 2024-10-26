package jreceiver.server.stream.capture;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.TimerTask;
import java.util.Timer;
import java.util.HashMap;
import java.util.StringTokenizer;
import org.xml.sax.InputSource;
import org.esau.ptarmigan.Generator;
import org.esau.ptarmigan.GeneratorFactory;
import jreceiver.server.stream.transcode.*;
import org.apache.commons.logging.*;

/**
 * This represents a stream of music data from a URL. It provides
 * a conventional 'read' interface. It destroys itself after a period
 * of inactivity (currently 20-40 seconds). The normal method of obtaining
 * a MusicInputStream is to use the MusicInputStreamFactory class.
 *
 * @author Philip Gladstone
 * @version $Revision: 1.14 $ $Date: 2003/05/07 08:28:47 $
 */
public final class MusicInputStream extends InputStream {

    private static final int MARK_BUFFER_SIZE = 65536;

    private boolean locked;

    /**
     * The URL of the stream.
     */
    private URL myurl;

    /**
     * The connection corresponding to the stream.
     */
    private URLConnection conn;

    /**
     * The stream that sources the data. This supports mark -- which is used
     * to handle those times when duplicate blocks of data are fetched!
     */
    private InputStream s;

    /**
     * This indicates if the stream is a never ending stream, or if it has
     * an end.
     */
    private boolean streamHasAnEnd;

    /**
     * This is the byte marker at the end of an ending stream
     */
    private int posAtEnd;

    /**
     * This is the virtual offset into the stream that the Rio knows
     * about
     */
    private int pos;

    /**
     * This is the starting offset of the stream.
     */
    private int posBase;

    /**
     * This is the position when the last timer tick happened. If a timer
     * tick happens with no change in the position, then the stream is
     * shutdown
     */
    private int posAtTick;

    /**
     * This is the timer that ticks every 20 seconds to allow us to
     * monitor when the stream is no longer in use
     */
    private Timer t;

    /**
     * The virtual position in the byte stream for the 'mark' point
     */
    private int marked_pos;

    /**
     * The content-type -- audio/mpeg
     */
    private String contentType;

    /**
     * The MusicMetaData object used to handle notifications
     */
    private MusicMetaData metaData;

    /**
     * The interval in bytes in the data stream for ICY metadata
     */
    private int metaInt;

    /**
     * Set if we need to do the 8 second / 200k prebuffer on the next read
     */
    private boolean needsPrebuffer = false;

    /**
     * Returns true if this stream can answer a read for this offset
     *
     * @param rpos the byte offset requested
     * @return true if possible
     */
    public synchronized boolean posInRange(int rpos) {
        if (locked) {
            try {
                wait(5000);
            } catch (Exception e) {
            }
            if (locked) {
                log.debug("Forcing position not in range: my pos = " + pos + ", requested pos = " + rpos + ", marked_pos = " + marked_pos);
                return false;
            }
        }
        log.debug("Is position in range? my pos = " + pos + ", requested pos = " + rpos + ", marked_pos = " + marked_pos);
        if (posAtEnd >= 0 && rpos >= posAtEnd) return true;
        if (rpos <= pos && rpos >= marked_pos) return true;
        return false;
    }

    /**
     * Gets the current virtual byte offset
     *
     * @return the byte offset
     */
    public int getPos() {
        return pos;
    }

    /**
     * Standard toString method
     *
     * @return a displayable form of the name and position of the stream
     */
    public String toString() {
        return "MusicInputStream: " + myurl.toString() + "@" + pos;
    }

    /**
     * Returns the content type -- normally audio/mpeg
     *
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the MetaData object
     */
    public MusicMetaData getMusicMetaData() {
        return metaData;
    }

    /**
     * Force set the offset of the stream to another value. This is useful
     * if the player thinks it has some particular offset and we cannot
     * find a stream to match. If a new stream is created, then it's offset
     * can be smashed to match the offset required.
     *
     * @param rpos The new position
     */
    public void setOffset(int rpos) {
        marked_pos += (rpos - pos);
        posBase += (rpos - pos);
        pos = rpos;
    }

    /**
     * Adjust the stream pointer to the required virtual offset.
     *
     * @param begin the offset for the next read
     * @exception IOException
     *            if the stream cannot be seeked to the required offset
     */
    public synchronized void seek(int begin) throws IOException {
        if (posAtEnd >= 0 && begin >= posAtEnd) {
            begin = posAtEnd;
        }
        if (begin != pos) {
            if (begin < marked_pos) {
                throw new IOException("Cannot seek to " + begin + ", marked_pos is " + marked_pos);
            }
            if (begin >= marked_pos) {
                s.reset();
                pos = marked_pos;
            }
            if (pos < begin) {
                s.skip(begin - pos);
                pos = begin;
            }
        }
    }

    /**
     * We update the position of the mark here -- it is always about 65k behind
     * where we are
     *
     * @param amountPending the amount of data that we are about to read
     */
    private void updateMark(int amountPending) {
        if (amountPending + pos > marked_pos + MARK_BUFFER_SIZE) {
            try {
                s.reset();
                int amountToSkip = pos + amountPending - marked_pos - MARK_BUFFER_SIZE;
                s.skip(amountToSkip);
                s.mark(MARK_BUFFER_SIZE);
                marked_pos += amountToSkip;
                s.skip(pos - marked_pos);
            } catch (IOException e) {
            }
        }
    }

    /**
     * Reads a block of data from the stream. Note that this function will fill
     * the block.
     *
     * @param b the byte array to be filled with data
     * @exception IOException if something bad happens during the reading
     */
    public void readFully(byte[] b) throws IOException {
        int len = b.length;
        int used = 0;
        while (used < len) {
            int l = read(b, used, len - used);
            if (l <= 0) break;
            used += l;
        }
        if (len != used) {
            throw new IOException("Failed to readFully: read " + used + " out of " + len);
        }
    }

    /**
     * returns the number of bytes that can be read without blocking
     *
     * @return the number of bytes
     * @exception IOException if something bad happens
     */
    public int available() throws IOException {
        return s.available();
    }

    /**
     * reads a byte of data from the stream.
     *
     * @return the byte in the range 0 to 255
     * @exception IOException if something bad happens
     */
    public synchronized int read() throws IOException {
        updateMark(1);
        int l = s.read();
        if (l >= 0) pos++;
        return l;
    }

    /**
     * Reads a block of data from the stream. This function obeys the standard
     * read contract.
     *
     * @param b the byte array to be filled with data
     * @return the number of bytes read
     * @exception IOException if something bad happens during the reading
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * This waits until some metadata has been read (or returns if there is none on
     * this stream
     *
     * @param maxWait the number of milliseconds to wait at most
     */
    public void waitForMetadata(int maxWait) {
        if (metaInt > 0) {
            int amnt = metaInt - (pos - posBase) + 1;
            if (amnt > 0 && amnt < 33000) {
                int got = waitTillBytesAvailable(amnt, maxWait);
                if (amnt > got) amnt = got;
                try {
                    updateMark(amnt);
                    int oldpos = pos;
                    pos += s.skip(amnt);
                    seek(oldpos);
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * This waits until the required number of bytes are available, but
     * not waiting for too long a time.
     *
     * @param amnt the number of bytes required
     * @param maxWait the number of milliseconds to wait at most
     * @return the number of bytes actually available
     */
    private int waitTillBytesAvailable(int amnt, int maxWait) {
        try {
            int sleeptime = 0;
            while (sleeptime < maxWait && available() < amnt) {
                try {
                    Thread.sleep(100);
                    sleeptime += 100;
                } catch (InterruptedException e) {
                }
            }
            return available();
        } catch (IOException e) {
        }
        return 0;
    }

    /**
     * Reads a block of data from the stream. This function obeys the standard
     * read contract.
     *
     * @param b the byte array to be filled with data
     * @param offset the offset to start reading into
     * @param len the maximum length to be read
     * @return the number of bytes read
     * @exception IOException if something bad happens during the reading
     */
    public synchronized int read(byte[] b, int offset, int len) throws IOException {
        if (needsPrebuffer) {
            waitTillBytesAvailable(150000, 8000);
            needsPrebuffer = false;
        }
        updateMark(len);
        int l = s.read(b, offset, len);
        if (l > 0) {
            pos += l;
        } else {
            if (streamHasAnEnd) posAtEnd = pos;
        }
        return l;
    }

    private static URLConnection handleMSRedirect(URLConnection c) throws IOException {
        DataInputStream in = new DataInputStream(c.getInputStream());
        String line;
        URL url = null;
        while ((line = in.readLine()) != null) {
            int i = line.indexOf('=');
            if (i > 0) {
                url = new URL(line.substring(i + 1).trim());
                break;
            }
        }
        in.close();
        if (url == null) {
            throw new IOException("Cannot resolve MS Redirect");
        }
        log.debug("Following redirect to " + url);
        URLConnection nc = url.openConnection();
        nc.setRequestProperty("User-Agent", "JReceiver/@version@");
        nc.setRequestProperty("Pragma", "xPlayStrm=1");
        nc.connect();
        if (nc.getContentType().equals("application/octet-stream")) {
            log.debug("Got live stream");
            return nc;
        }
        nc.getInputStream().close();
        throw new IOException("Couldn't get live data stream");
    }

    public MusicInputStream(URL url) throws IOException {
        this(url, 0, null, null);
    }

    /**
     * Creates a new MusicInputStream object connected to a provided URL. If the
     * URL is an HTTP connection to a Shoutcast server, then the stream name
     * is extracted and saved for later. An offset can be provided. This may not
     * work on live streams
     *
     * @param url the URL to be used for the stream
     * @param offset the position within the stream to start at.
     * @param mimeType the mime type of the required stream (or null for native)
     * @param srcMimeType the mime type of the underlying stream (or null if we don't know)
     * @exception IOException if we fail to connect
     */
    public MusicInputStream(URL url, int offset, String mimeType, String srcMimeType) throws IOException {
        myurl = url;
        conn = url.openConnection();
        conn.setRequestProperty("User-Agent", "JReceiver/@version@");
        conn.setRequestProperty("icy-metadata", "1");
        if (offset > 0) {
            conn.setRequestProperty("Range", "bytes=" + offset + "-");
        }
        conn.connect();
        locked = false;
        contentType = conn.getContentType();
        if ((contentType == null || contentType.equals("content/unknown")) && srcMimeType != null) contentType = srcMimeType;
        if (contentType != null && contentType.equals("video/x-ms-asf")) {
            conn = handleMSRedirect(conn);
            contentType = "audio/x-ms-wma";
        }
        ReadAheadInputStream originalStream = new ReadAheadInputStream(conn.getInputStream());
        s = originalStream;
        if (!s.markSupported()) {
            s = new BufferedInputStream(s);
        }
        metaInt = -1;
        metaData = new MusicMetaData();
        metaData.set("name", conn.getHeaderField("x-audiocast-name"));
        metaData.set("genre", conn.getHeaderField("x-audiocast-genre"));
        metaData.setIf("name", conn.getHeaderField("ice-name"));
        metaData.setIf("genre", conn.getHeaderField("ice-genre"));
        streamHasAnEnd = false;
        if (url.getProtocol().equals("file")) {
            streamHasAnEnd = true;
        }
        if (conn.getContentLength() > 0) {
            streamHasAnEnd = true;
        }
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            int responseCode = hconn.getResponseCode();
            s.mark(32);
            byte[] lbyte = new byte[16];
            int bytes_read = s.read(lbyte);
            String line;
            line = new String(lbyte, 0, bytes_read);
            s.reset();
            if (line != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (st.hasMoreTokens() && st.nextToken().equals("ICY")) {
                    DataInputStream in = new DataInputStream(s);
                    try {
                        responseCode = Integer.decode(st.nextToken()).intValue();
                    } catch (Exception e) {
                    }
                    HashMap headers = new HashMap();
                    while ((line = in.readLine()) != null && line.length() > 0) {
                        int i = line.indexOf(':');
                        if (i > 0) {
                            headers.put(line.substring(0, i).trim().toLowerCase(), line.substring(i + 1).trim());
                        }
                    }
                    contentType = "audio/mpeg";
                    metaData.setIf("name", (String) headers.get("icy-name"));
                    metaData.setIf("genre", (String) headers.get("icy-genre"));
                    try {
                        metaInt = Integer.decode((String) headers.get("icy-metaint")).intValue();
                    } catch (Exception e) {
                        metaInt = -1;
                    }
                }
            }
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Failed to open HTTP connection");
            }
            if (metaInt > 0) {
                s = new BufferedInputStream(new MetaDataInputStream(s, metaInt, metaData));
            }
        }
        pos = 0;
        posBase = 0;
        posAtTick = 0;
        marked_pos = 0;
        posAtEnd = -1;
        setOffset(offset);
        if (mimeType != null && !mimeType.equals(contentType)) {
            InputStream ts = TranscoderInputStreamFactory.getTranscodedInputStream(s, contentType, mimeType);
            if (ts != s) {
                originalStream = new ReadAheadInputStream(ts);
                s = originalStream;
            }
        }
        if (!s.markSupported()) {
            s = new BufferedInputStream(s);
        }
        s.mark(MARK_BUFFER_SIZE);
        class MusicTimer extends TimerTask {

            private MusicInputStream stream;

            MusicTimer(MusicInputStream s) {
                stream = s;
            }

            public void run() {
                stream.tick();
            }
        }
        t = new Timer();
        t.schedule(new MusicTimer(this), 20000, 20000);
        createMPEGAudioFrameHeader();
        metaData.notifyObservers(this);
        originalStream.setBufferSize(200000);
        needsPrebuffer = true;
    }

    /**
     * close access to this stream. We actually do nothing, and let the timer
     * close it down later
     */
    public void close() {
        unlock();
    }

    /**
     * Shuts down the stream and disconnects from the server if possible
     */
    public void shutdown() {
        try {
            s.close();
        } catch (IOException e) {
        }
        MusicInputStreamFactory.removeMusicInputStream(myurl, this);
        if (t != null) t.cancel();
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.disconnect();
        }
    }

    /**
     * Returns an MPEGAudioFrameHeader if this stream corresponds to an
     * MP3 stream.
     *
     * @return the MPEGAudioFrameHeader if possible. Null otherwise
     */
    private void createMPEGAudioFrameHeader() {
        try {
            byte[] b = new byte[4096];
            int initial_pos = pos;
            readFully(b);
            seek(initial_pos);
            Generator generator = GeneratorFactory.newInstance();
            generator.setContentHandler(new MusicMetadataFilter(metaData));
            generator.parse(new InputSource(new ByteArrayInputStream(b)));
        } catch (Exception e) {
            log.debug("Got exception trying to get MPEGAudioFrameHeader: " + e);
        }
    }

    /**
     * Called by the timer tick function. Handles shutting down on
     * inactivity.
     */
    public synchronized void tick() {
        if (posAtTick == pos) {
            shutdown();
        }
        posAtTick = pos;
    }

    /**
     * This locks access to this object. In particular, the inRange method
     * will block until the object is unlocked.
     */
    public synchronized void lock() {
        while (locked) {
            try {
                wait();
            } catch (Exception e) {
            }
        }
        locked = true;
    }

    /**
     * This permits access to this object.
     */
    public synchronized void unlock() {
        locked = false;
        notify();
    }

    /**
     * logging object
     */
    protected static Log log = LogFactory.getLog(MusicInputStream.class);
}
