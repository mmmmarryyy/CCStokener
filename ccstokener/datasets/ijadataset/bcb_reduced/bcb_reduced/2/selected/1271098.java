package net.pandoragames.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Implementation of the CharSequence interface over a FileInputStream. This class
 * may be used when very large portions of text are to be handled, too large to be
 * kept in memory (i.e. as String or StringBuffer).<br>
 * Despite its name, the object may as well be created with a URL as resource.
 * 
 * @author Olivier Wehner
 * <!-- copyright note --> 
 */
public class FileBuffer implements CharSequence {

    private Source source;

    private Charset encoding;

    private int buffersize = 8 * 1024;

    private int characterIndex = 0;

    private int length = -1;

    private Reader reader;

    private StringBuilder buffer;

    /**
	 * Creates the buffer with the specified file and the default character set.
	 * @param textFile to be read into this buffer
	 * @throws IOException if the file can not be opend
	 */
    public FileBuffer(File textFile) throws IOException {
        this(textFile, Charset.defaultCharset());
    }

    /**
	 * Creates the buffer from the specified URL and the default character set.
	 * @param textFile resource for this buffer
	 * @throws IOException if the URL can not be opend
	 */
    public FileBuffer(URL textFile) throws IOException {
        this(textFile, Charset.defaultCharset());
    }

    /**
	 * Creates the buffer with the specified file and character set.
	 * @param textFile to be read into this buffer
	 * @param charset to be used to decode the file
	 * @throws IOException if the file can not be opend
	 */
    public FileBuffer(File textFile, Charset charset) throws IOException {
        if (textFile == null) throw new NullPointerException("The supplied file must not be null");
        source = new Source(textFile);
        encoding = charset;
        openFile();
    }

    /**
	 * Creates the buffer from the specified URL using the indicated character set.
	 * @param textFile resource for this buffer
	 * @param charset to be used to decode the resource
	 * @throws IOException if the URL can not be opend
	 */
    public FileBuffer(URL textFile, Charset charset) throws IOException {
        if (textFile == null) throw new NullPointerException("The supplied url must not be null");
        source = new Source(textFile);
        encoding = charset;
        openFile();
    }

    /**
	 * {@inheritDoc}
	 */
    public int length() {
        if (length < 0) {
            try {
                if (reader == null) openFile();
                while (blockRead()) ;
            } catch (IOException iox) {
                throw new IllegalStateException(iox.getMessage());
            }
        }
        return length;
    }

    /**
	 * {@inheritDoc}
	 */
    public char charAt(int index) {
        if (index < characterIndex) {
            close();
            try {
                openFile();
            } catch (IOException iox) {
                throw new IllegalStateException(iox.getMessage());
            }
            return charAt(index);
        } else if (characterIndex + buffer.length() <= index) {
            if ((0 <= length) && (length <= index)) throw new IndexOutOfBoundsException(Integer.toString(index));
            blockRead();
            return charAt(index);
        } else {
            return buffer.charAt(index - characterIndex);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public CharSequence subSequence(int start, int end) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < (end - start); i++) {
            result.append(charAt(start + i));
        }
        return result;
    }

    /**
	 * Returns a string containing the characters in this sequence in the same order as this sequence. 
	 * The length of the string will be the length of this sequence. 
	 */
    public String toString() {
        int end = length();
        return subSequence(0, end).toString();
    }

    /**
	 * Closes this buffer.
	 * Should be called before disposing this object,
	 * otherwise the underlying resource might be locked.
	 */
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException iox) {
            }
            reader = null;
        }
    }

    /**
	 * Calls the close() method.
	 */
    protected void finalize() {
        close();
    }

    /**
	 * Sets the internal buffer size in kilo byte.
	 * @param k kilo byte
	 */
    public void setBufferSizeKb(int k) {
        buffersize = k * 1024;
    }

    /**
	 * Returns the internal buffer size in kilo byte.
	 * The default is 8.
	 * @return internal buffer size in kilo byte
	 */
    public int getBufferSizeKb() {
        return buffersize % 1024;
    }

    private void openFile() throws IOException {
        reader = source.getReader();
        buffer = new StringBuilder();
        characterIndex = 0;
        readToBuffer();
    }

    private boolean blockRead() {
        if (reader == null) return false;
        if (characterIndex + buffer.length() == length) return false;
        characterIndex = characterIndex + buffer.length() - 512;
        buffer.delete(0, buffer.length() - 512);
        try {
            return readToBuffer();
        } catch (IOException iox) {
            throw new IllegalStateException(iox.getMessage());
        }
    }

    private boolean readToBuffer() throws IOException {
        int ccount = 0;
        char[] cbuf = new char[1024];
        do {
            ccount = reader.read(cbuf);
            if (ccount > 0) {
                buffer.append(cbuf, 0, ccount);
            }
        } while ((ccount == 1024) && (buffer.length() < buffersize));
        if (ccount < 1024) length = characterIndex + buffer.length();
        return (ccount == 1024);
    }

    /**
	 * Wrapper around a File or URL object that creates a resource neutral Reader.
	 *
	 */
    class Source {

        private File file;

        private URL url;

        public Source(File f) {
            file = f;
        }

        public Source(URL u) {
            url = u;
        }

        public Reader getReader() throws IOException {
            if (file != null) {
                return new InputStreamReader(new FileInputStream(file), encoding);
            } else {
                return new InputStreamReader(url.openStream(), encoding);
            }
        }
    }
}
