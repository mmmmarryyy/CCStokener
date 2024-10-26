package cpdetector.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * <p>
 * This implementation provides the default implementation 
 * for the high-level codepage detection method {@link #open(URL)} 
 * of the implemented interface ICodepageProcessor.
 * </p>
 * <p>
 * Also the Comparable interface implementation is provided here by 
 * comparing the class-name strings of the implementations.
 * </p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
 *
 */
public abstract class AbstractCodepageDetector implements ICodepageDetector {

    /**
     * 
     */
    public AbstractCodepageDetector() {
        super();
    }

    /** 
	 * A default delegation to {@link #detectCodepage(URL)} 
	 * that opens the document specified by the given URL with 
	 * the detected codepage.
	 * 
	 * @see cpdetector.io.ICodepageDetector#open(java.net.URL)
	 */
    public final Reader open(URL url) throws IOException {
        Reader ret = null;
        Charset cs = this.detectCodepage(url);
        if (cs != null) {
            ret = new InputStreamReader(new BufferedInputStream(url.openStream()), cs);
        }
        return ret;
    }

    public int compareTo(Object o) {
        String other = o.getClass().getName();
        String mine = this.getClass().getName();
        return mine.compareTo(other);
    }
}
