package jimm.twice.util;

import jimm.twice.util.Logger;
import jimm.twice.ice.xml.SoapEnvelope;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Handles request and request/receive communications with an HTTP server.
 * This class exists so we can replace instances during testing.
 */
public class HttpTalker {

    protected static final String TEMP_FILE_PREFIX = "ice";

    protected static final String TEMP_FILE_SUFFIX = ".xml";

    protected static final String DEFAULT_LOGGER_PREFIX = "ice";

    protected static final int TEMP_FILE_BUFSIZ = 4096;

    protected static HttpTalker instance = new HttpTalker();

    protected String loggerPrefix;

    /**
 * Returns an instance of <var>instanceClassNameK</var>, which is
 * HttpTalker by default.
 *
 * @return an HttpTalker
 */
    public static HttpTalker getInstance() {
        return instance;
    }

    /**
 * Sets class name for {@link #getInstance} to return.
 *
 * @param name the name of (a subclass of) HttpTalker
 */
    public static void setInstance(HttpTalker talker) {
        instance = talker;
    }

    public HttpTalker() {
        loggerPrefix = DEFAULT_LOGGER_PREFIX;
    }

    /**
 * Sends a payload, saves the XML response to a temp file, and returns
 * the temp file.
 * <p>
 * Tech note: the order of the HTTP I/O fault (get output, write output,
 * get input, read input) is used by the <code>HttpURLConnection</code> class
 * to decide that a POST is appropriate instead of a GET.
 * <p>
 * If an <code>IOException</code> exception is caught, it is logged and
 * rethrown.
 *
 * @param payload the payload we want to send
 * @param URL a URL (golly!)
 * @return a file containing the response XML
 */
    public File sendPayload(SoapEnvelope payload, URL url) throws IOException {
        URLConnection conn = null;
        File tempFile = null;
        Logger l = Logger.instance();
        String className = getClass().getName();
        l.log(Logger.DEBUG, loggerPrefix, className + ".sendPayload", "sending payload to " + url.toString());
        try {
            conn = url.openConnection();
            conn.setDoOutput(true);
            payload.writeTo(conn.getOutputStream());
            tempFile = readIntoTempFile(conn.getInputStream());
        } catch (IOException ioe) {
            l.log(Logger.ERROR, loggerPrefix, className + ".sendPayload", ioe);
            throw ioe;
        } finally {
            conn = null;
        }
        l.log(Logger.DEBUG, loggerPrefix, className + ".sendPayload", "received response");
        return tempFile;
    }

    /**
 * Retrieves the contents of a URL, stores it in a temp file, and returns the
 * file. No ICE message is sent first; we use HTTP GET to retrieve the URL's
 * contents.
 * <p>
 * The file returned is a temp file, marked for deletion upon exit. It is
 * still a good idea to delete the file when you are done with it.
 * <p>
 * If an <code>IOException</code> exception is caught, it is logged and
 * rethrown.
 *
 * @param url the URL to GET
 * @return a temp XML file containing the contents of the URL
 */
    public File getURL(URL url) throws IOException {
        URLConnection conn = null;
        File tempFile = null;
        Logger l = Logger.instance();
        String className = getClass().getName();
        l.log(Logger.DEBUG, loggerPrefix, className + ".getURL", "GET URL " + url.toString());
        try {
            conn = url.openConnection();
            tempFile = readIntoTempFile(conn.getInputStream());
        } catch (IOException ioe) {
            l.log(Logger.ERROR, loggerPrefix, className + ".getURL", ioe);
            throw ioe;
        } finally {
            conn = null;
        }
        l.log(Logger.DEBUG, loggerPrefix, className + ".getURL", "received URL");
        return tempFile;
    }

    protected File readIntoTempFile(InputStream source) throws IOException {
        File tempFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        tempFile.deleteOnExit();
        InputStreamReader in = new InputStreamReader(source);
        FileWriter out = new FileWriter(tempFile);
        char[] buf = new char[TEMP_FILE_BUFSIZ];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        in = null;
        out.close();
        out = null;
        return tempFile;
    }
}
