package wayic.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Loads data from a URL and provides the data as byte array, String, Reader or InputStream.
 * It takes content encoding into account when returning String or Reader.
 * 
 * @deprecated
 * @author Ashesh Nishant
 *
 */
public class AlphaUrlLoader {

    private static final Logger LOGGER = LogManager.getLogger(AlphaUrlLoader.class);

    private static final int BUFFER_SIZE = 1024;

    public static final String DEFAULT_ENCODING = "UTF-8";

    private URL url;

    private String encoding;

    private byte[] data;

    /**
	 * Default Constructor
	 * @param url URL
	 */
    public AlphaUrlLoader(URL url) {
        this.url = url;
    }

    /**
	 * Overloaded to accept URL as a String
	 * @param url String
	 * @throws MalformedURLException
	 */
    public AlphaUrlLoader(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    /**
	 * Opens a URL connection and reads data from its InputStream.
	 * This needs to be called before getting data from this object.
	 * @return InputStream
	 * @throws IOException
	 */
    public InputStream load() throws IOException {
        URLConnection connection = url.openConnection();
        InputStream input = connection.getInputStream();
        encoding = connection.getContentEncoding();
        LOGGER.debug("open connection to " + url);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            int len = 0;
            do {
                len = input.read(buf);
                LOGGER.debug("read " + len + " bytes");
                if (len > 0) baos.write(buf, 0, len);
            } while (len > 0);
        } catch (IOException io) {
            LOGGER.debug(io);
        }
        data = baos.toByteArray();
        return new ByteArrayInputStream(data);
    }

    /**
	 * @return String with a default encoding UTF-8
	 * @throws UnsupportedEncodingException
	 */
    public String getAsString() throws UnsupportedEncodingException {
        return getAsString(getEncoding());
    }

    /**
	 * @param encod String
	 * @return String with a default encoding UTF-8
	 * @throws UnsupportedEncodingException
	 */
    public String getAsString(String encod) throws UnsupportedEncodingException {
        if (encod == null) {
            encod = DEFAULT_ENCODING;
        }
        return new String(data, encod);
    }

    /**
	 * @return Reader with a default encoding UTF-8
	 * @throws UnsupportedEncodingException
	 */
    public Reader getAsReader() throws UnsupportedEncodingException {
        return getAsReader(getEncoding());
    }

    /**
	 * @param encod String
	 * @return Reader with a default encoding UTF-8
	 * @throws UnsupportedEncodingException
	 */
    public Reader getAsReader(String encod) throws UnsupportedEncodingException {
        return new StringReader(getAsString(encod));
    }

    /**
	 * @return InputStream
	 */
    public InputStream getAsInputStream() {
        return new ByteArrayInputStream(data);
    }

    /**
	 * @return String encoding
	 */
    public String getEncoding() {
        return encoding;
    }

    /**
	 * @param encoding String
	 */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
	 * test method
	 * @param args
	 */
    public static void main(String[] args) throws IOException {
        AlphaUrlLoader loader = new AlphaUrlLoader("http://google.com");
        long begin = System.currentTimeMillis();
        loader.load();
        LOGGER.info("Test Data from Google:" + "[" + loader.getAsString() + "]");
        LOGGER.info("Loaded in " + (System.currentTimeMillis() - begin) + " ms");
    }
}
