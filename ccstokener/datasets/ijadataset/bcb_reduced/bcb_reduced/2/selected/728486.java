package ssg.tools.common.fragmentedFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;

/**
 * Provides access to file as to input stream.
 * Returns input stream positioned at specified offset.
 *
 * For use in FileCmdEditor and FragmentedInputStream.
 * 
 * @author ssg
 */
public class StreamDataSource implements Serializable {

    private String name;

    private File file;

    private URL url;

    private transient InputStream is;

    private transient RandomAccessFile raf;

    private transient boolean cached = false;

    public StreamDataSource(String name, String uri) throws IOException {
        this.name = name;
        if (uri.indexOf("://") != -1) {
            initURL(new URL(uri), true);
        } else {
            File f = new File(uri);
            initFile(f);
        }
    }

    public StreamDataSource(String name, File file) throws IOException {
        this.name = name;
        initFile(file);
    }

    public StreamDataSource(String name, URL url, boolean cache) throws IOException {
        this.name = name;
        initURL(url, cache);
    }

    public void initFile(File file) throws IOException {
        this.file = file;
    }

    public void initURL(URL url, boolean cache) throws IOException {
        this.url = url;
        if (cache) {
            System.out.println(getClass().getName() + ": caching '" + url + "'");
            InputStream urlIS = new BufferedInputStream(url.openStream(), 1024 * 30);
            file = File.createTempFile("_dss_", "_dss_");
            file.deleteOnExit();
            OutputStream cachedOS = new BufferedOutputStream(new FileOutputStream(file), 1024 * 30);
            byte[] buf = new byte[1024 * 4];
            long cachedBytesCount = 0;
            int count = 0;
            while ((count = urlIS.read(buf)) > 0) {
                cachedOS.write(buf, 0, count);
                cachedBytesCount += count;
            }
            urlIS.close();
            cachedOS.flush();
            cachedOS.close();
            this.cached = true;
            System.out.println(getClass().getName() + ": cached " + cachedBytesCount + " bytes into '" + file.getAbsolutePath() + "'");
        }
    }

    public long getLength() {
        if (file != null) {
            return file.length();
        } else {
            try {
                InputStream tis = new BufferedInputStream(getInputStream(0), 1024 * 30);
                long count = 0;
                while (tis.read() != -1) {
                    count++;
                }
                tis.close();
                return count;
            } catch (IOException ioex) {
                return -1;
            }
        }
    }

    public InputStream getInputStream(long offset) throws IOException {
        if (is != null && raf == null) {
            try {
                is.close();
            } catch (Throwable th) {
            } finally {
                is = null;
            }
        }
        if (is == null && getUrl() != null && getFile() == null) {
            is = url.openStream();
        }
        if (is == null) {
            if (getFile() != null) {
                raf = null;
                try {
                    raf = new RandomAccessFile(getFile(), "r");
                } catch (Exception ex) {
                }
                is = (raf != null) ? new RAFInputStream(raf, 0) : new BufferedInputStream(new FileInputStream(getFile()), 1024 * 30);
            }
        }
        if (is instanceof RAFInputStream) {
            RAFInputStream ris = (RAFInputStream) is;
            ris.seek(offset);
        } else {
            is.skip(offset);
        }
        return is;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    public void close() throws IOException {
        if (raf != null) {
            raf.close();
            is = null;
            raf = null;
        } else if (is != null) {
            is.close();
            is = null;
        }
        if (cached && file != null) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Wraps RandomAccessFile as input stream providing explicit stream.
     */
    public static class RAFInputStream extends InputStream {

        RandomAccessFile raf;

        long markedOffset;

        int markLimit;

        public RAFInputStream(RandomAccessFile raf, long startOffset) throws IOException {
            this.raf = raf;
            raf.seek(startOffset);
        }

        @Override
        public int read() throws IOException {
            return raf.read();
        }

        @Override
        public synchronized void mark(int readLimit) {
            try {
                markedOffset = raf.getFilePointer();
                markLimit = readLimit;
            } catch (IOException ioex) {
            }
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return raf.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return raf.read(b, off, len);
        }

        @Override
        public synchronized void reset() throws IOException {
            raf.seek(markedOffset);
        }

        @Override
        public long skip(long n) throws IOException {
            long canSkip = raf.length() - raf.getFilePointer();
            if (n > canSkip) {
                n = canSkip;
            }
            raf.seek(n + raf.getFilePointer());
            return n;
        }

        public long seek(long offset) throws IOException {
            raf.seek(offset);
            return offset;
        }

        public long position() throws IOException {
            return raf.getFilePointer();
        }

        public boolean eof() throws IOException {
            return raf.getFilePointer() >= raf.length();
        }

        @Override
        public void close() throws IOException {
        }
    }
}
