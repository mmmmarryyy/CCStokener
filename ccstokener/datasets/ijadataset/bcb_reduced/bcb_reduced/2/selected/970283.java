package FFIT.binFileReader;

import FFIT.IdentificationFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The <code>UrlByteReader</code> class is a <code>ByteReader</code> that
 * reads its data from a URL.
 *
 * @author linb
 */
public class UrlByteReader extends StreamByteReader {

    /** Creates a new instance of UrlByteReader */
    private UrlByteReader(IdentificationFile theIDFile, boolean readFile) {
        super(theIDFile);
        if (readFile) {
            this.readUrl();
        }
    }

    /**
     * Static constructor for class.  Trys to read url into a buffer. If it doesn't fit, 
     * save it to a file, and return a FileByteReader with that file.
     */
    static ByteReader newUrlByteReader(IdentificationFile theIDFile, boolean readFile) {
        UrlByteReader byteReader = new UrlByteReader(theIDFile, readFile);
        if (byteReader.tempFile == null) {
            return byteReader;
        } else {
            return new FileByteReader(theIDFile, readFile, byteReader.tempFile.getPath());
        }
    }

    /** Read data into buffer or temporary file from the url specified by <code>theIDFile</code>.
     */
    private void readUrl() {
        URL url;
        try {
            url = new URL(myIDFile.getFilePath());
        } catch (MalformedURLException ex) {
            this.setErrorIdent();
            this.setIdentificationWarning("URL is malformed");
            return;
        }
        try {
            readStream(url.openStream());
        } catch (IOException ex) {
            this.setErrorIdent();
            this.setIdentificationWarning("URL could not be read");
        }
    }

    /**
     * Get a <code>URL<code> object for this path
     * @param path the path for which to get the URL
     * @return the URL represented by <code>path</code> or <code>null</code> if 
     * it cannot be represented
     */
    public static URL getURL(String path) {
        URL url = null;
        try {
            url = new URL(path);
            if (url.getProtocol().equalsIgnoreCase("http")) {
                return url;
            } else {
                return null;
            }
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * Check for a valid URL
     * @param path the URL to check
     * @return <code>true</code> if <code>path</code> is a valid URL
     */
    public static boolean isURL(String path) {
        URL url = null;
        try {
            url = new URL(path);
            if (url.getProtocol().equalsIgnoreCase("http")) {
                return true;
            }
        } catch (MalformedURLException ex) {
            return false;
        }
        return false;
    }
}
