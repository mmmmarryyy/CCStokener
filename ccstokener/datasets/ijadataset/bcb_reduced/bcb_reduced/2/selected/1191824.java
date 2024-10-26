package org.josef.util;

import static org.josef.annotations.Status.Stage.PRODUCTION;
import static org.josef.annotations.Status.UnitTests.COMPLETE;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.josef.annotations.Review;
import org.josef.annotations.Status;

/**
 * URL Utility class.
 * @author Kees Schotanus
 * @version 1.0 $Revision: 2840 $
 */
@Status(stage = PRODUCTION, unitTests = COMPLETE)
@Review(by = "Kees Schotanus", at = "2011-03-04")
public final class UrlUtil {

    /**
     * Private constructor prevents creation of an instance outside this class.
     */
    private UrlUtil() {
    }

    /**
     * Gets the text from the supplied url.
     * <br>The returned text will have line separators equal to the platform on
     * which this code is run, not necessarily the line separators of the
     * original text file.
     * @param url The URL of the text file to get.
     * @return Text located at the supplied url.
     * @throws IOException When no text could be read from the supplied url.
     * @throws java.net.MalformedURLException When the supplied url is not a
     *  valid url.
     */
    public static String getTextFromUrl(final String url) throws IOException {
        final String lineSeparator = System.getProperty("line.separator");
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            final StringBuilder result = new StringBuilder();
            inputStreamReader = new InputStreamReader(new URL(url).openStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line).append(lineSeparator);
            }
            return result.toString();
        } finally {
            InputOutputUtil.close(bufferedReader, inputStreamReader);
        }
    }
}
