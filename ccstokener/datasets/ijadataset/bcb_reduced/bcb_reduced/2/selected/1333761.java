package au.gov.naa.digipres.xena.kernel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.mozilla.universalchardet.UniversalDetector;

public class CharsetDetector {

    public static String DEFAULT_CHARSET = "UTF-8";

    private static final int MAX_BYTES_FOR_DETECTION = 64 * 1024;

    public static String guessCharSet(URL url) throws IOException {
        return guessCharSet(url.openStream());
    }

    /**
	 * Uses juniversal_chardet to detect the character set of the given InputStream.
	 * A maximum of 64kB of characters will be used for detection.
	 * If every character in the input sample is an ASCII character, then the US-ASCII charset will be returned.
	 * Null is returned if no matching charset is found.
	 * 
	 * 
	 * 
	 * @param is
	 * @return name of the matching charset, or null if no match could be found.
	 * @throws IOException
	 */
    public static String guessCharSet(InputStream is) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);
        byte[] buf = new byte[4096];
        int iterationBytesRead = 0;
        iterationBytesRead = is.read(buf);
        int totalBytesRead = iterationBytesRead;
        boolean fileIsAscii = true;
        while (iterationBytesRead > 0 && totalBytesRead < MAX_BYTES_FOR_DETECTION && !detector.isDone()) {
            detector.handleData(buf, 0, iterationBytesRead);
            fileIsAscii = fileIsAscii && isAscii(buf, 0, iterationBytesRead);
            iterationBytesRead = is.read(buf);
            totalBytesRead += iterationBytesRead;
        }
        detector.dataEnd();
        String detectedEncoding = fileIsAscii ? "US-ASCII" : detector.getDetectedCharset();
        return detectedEncoding;
    }

    /**
	 * Return true if every character in the given byte array is an ASCII character.
	 * @param bytes
	 * @param offset
	 * @param length
	 * @return true if every character in the given byte array is an ASCII character.
	 */
    private static boolean isAscii(byte[] bytes, int offset, int length) {
        for (int i = offset; i < length; i++) {
            if ((0x0080 & bytes[i]) != 0) {
                return false;
            }
        }
        return true;
    }
}
