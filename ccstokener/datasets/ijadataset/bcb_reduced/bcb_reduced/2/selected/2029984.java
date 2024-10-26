package cpdetector.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

/**
 * <p>
 * A fa�ade for jchardet codepage detection. <a href="http://www.i18nfaq.com/"
 * target="_blank">JChardet </a> is the java port of Frank Yung-Fong Tang's
 * Mozilla charset detector.
 * </p>
 * <p>
 * This charset detector works on guessing the codepage. <i>"The algorithm looks
 * into the byte sequence and based on the values of each byte uses a
 * elimination logic to narrow down to the final charset. If there is a tie
 * between EUC charsets, it uses the second logic to narrow down. This logic
 * uses the frequency statistics of characters in a given language." </i>( <a
 * href="http://www.i18nfaq.com/chardet.html#8">source of description </a>).
 * </p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann </a>
 *  
 */
public final class JChardetFacade extends AbstractCodepageDetector implements nsICharsetDetectionObserver {

    private static JChardetFacade instance = null;

    private static nsDetector det = new nsDetector(nsPSMDetector.ALL);

    private static byte[] buf = new byte[4096];

    private Charset codpage = null;

    /**
    *  
    */
    private JChardetFacade() {
        super();
        det.Init(this);
    }

    public static JChardetFacade getInstance() {
        if (instance == null) {
            instance = new JChardetFacade();
        }
        return instance;
    }

    /**
    * 
    * @param url
    *           Should link to a file containing textual document. No check for
    *           images or other resources is made.
    * @throws IOException
    *            If a problem with the url - handling occurs.
    */
    public Charset detectCodepage(URL url) throws IOException {
        return this.detectCodepage(new BufferedInputStream(url.openStream()), Integer.MAX_VALUE);
    }

    public Charset detectCodepage(InputStream in, int length) throws IOException {
        int len;
        int read = 0;
        boolean done = false;
        boolean isAscii = true;
        Charset ret = null;
        do {
            len = in.read(buf, 0, Math.min(buf.length, length - read));
            if (len > 0) {
                read += len;
            }
            if (isAscii) isAscii = det.isAscii(buf, len);
            if (!isAscii && !done) done = det.DoIt(buf, len, false);
        } while (len != -1);
        det.DataEnd();
        if (isAscii) {
            ret = Charset.forName("US-ASCII");
        } else {
            ret = this.codpage;
        }
        det.Reset();
        return ret;
    }

    public void Notify(String charset) {
        this.codpage = Charset.forName(charset);
    }
}
