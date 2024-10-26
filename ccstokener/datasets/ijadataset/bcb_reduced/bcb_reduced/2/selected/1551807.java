package info.monitorenter.cpdetector.io;

import info.monitorenter.cpdetector.io.AbstractCodepageDetector;
import info.monitorenter.cpdetector.io.ICodepageDetector;
import info.monitorenter.cpdetector.io.UnknownCharset;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * <p>
 * This detector identifies byte order marks of the following codepages to give a 100 % deterministic result in case of
 * detection.
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr>
 * <td>00 00 FE FF</td>
 * <td>UCS-4, big-endian machine (1234 order)</td>
 * </tr>
 * <tr>
 * <td>FF FE 00 00</td>
 * <td>UCS-4,little-endian machine (4321 order)</td>
 * </tr>
 * <tr>
 * <td>00 00 FF FE</td>
 * <td>UCS-4, unusual octet order (2143)</td>
 * </tr>
 * <td>FE FF 00 00</td>
 * <td>UCS-4, unusual octet order (3412)</td>
 * </tr>
 * <tr>
 * <td>FE FF ## ##</td>
 * <td>UTF-16, big-endian</td>
 * </tr>
 * <tr>
 * <td>FF FE ## ##</td>
 * <td>UTF-16, little-endian</td>
 * </tr>
 * <tr>
 * <td>EF BB BF</td>
 * <td>UTF-8</td>
 * </tr>
 * </table>
 * </p>
 * <p>
 * Note that this detector is very fast as it only has to read a maximum of 8 bytes to provide a result. Nevertheless it
 * is senseless to add it to the configuration if the documents to detect will have a low rate of documents in the
 * codepages that will be detected. If added to the configuration of {@link info.monitorenter.cpdetector.io.CodepageDetectorProxy}it
 * should be at front position to save computations of the following detection processses.
 * <p>
 * <p>
 * This implementation is based on: <br>
 * <a target="_blank" title="http://www.w3.org/TR/2004/REC-xml-20040204/#sec-guessing-no-ext-info"
 * href="http://www.w3.org/TR/2004/REC-xml-20040204/#sec-guessing-no-ext-info">W3C XML Specification 1.0 3rd Edition,
 * F.1 Detection Without External Encoding Information </a>.
 * </p>
 * 
 * This implementation does the same as <code>{@link ByteOrderMarkDetector}</code> but with a different
 * read strategy (read 4 bytes at once) and elseif blocks. Would
 * be great to have a performance comparison. Maybe the read of 4 bytes in a row combined with the
 * switch could make that other implementation the winner.
 * <p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann </a>
 * 
 * @version $Revision: 1.2 $
 */
public class UnicodeDetector extends AbstractCodepageDetector {

    private static ICodepageDetector instance;

    /**
     * Singleton constructor
     */
    private UnicodeDetector() {
        super();
    }

    public static ICodepageDetector getInstance() {
        if (instance == null) {
            instance = new UnicodeDetector();
        }
        return instance;
    }

    public Charset detectCodepage(InputStream in, int length) throws IOException {
        byte[] bom = new byte[4];
        in.read(bom, 0, 4);
        byte b = (byte) 0xEF;
        if (bom[0] == (byte) 0x00 && bom[1] == (byte) 0x00 && bom[2] == (byte) 0xFE && bom[2] == (byte) 0xFF) return Charset.forName("UTF-32BE");
        if (bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == (byte) 0x00 && bom[2] == (byte) 0x00) return Charset.forName("UTF-32LE");
        if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) return Charset.forName("UTF-8");
        if (bom[0] == (byte) 0xff && bom[1] == (byte) 0xfe) return Charset.forName("UTF-16LE");
        if (bom[0] == (byte) 0xfe && bom[1] == (byte) 0xff) return Charset.forName("UTF-16BE");
        if (bom[0] == (byte) 0 && bom[1] == (byte) 0 && bom[2] == (byte) 0xfe && bom[3] == (byte) 0xff) return Charset.forName("UCS-4");
        return UnknownCharset.getInstance();
    }

    /**
     * @see info.monitorenter.cpdetector.io.ICodepageDetector#detectCodepage(java.net.URL)
     */
    public Charset detectCodepage(final URL url) throws IOException {
        Charset result;
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        result = this.detectCodepage(in, Integer.MAX_VALUE);
        in.close();
        return result;
    }
}
