package org.josef.web.el;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.josef.util.InputOutputUtil;
import org.josef.web.html.HtmlUtil;

/**
 * Utility class with functions that can be called from within the
 * Expression Language (EL).
 * @author Kees Schotanus.
 * @version 1.0 $Revision$
 */
public final class JosefFunctions {

    /**
     * Private constructor prevents creation of an instance outside this class.
     */
    private JosefFunctions() {
    }

    /**
     * Gets the text from the supplied url.
     * @param url The URL of the text file to get.
     * @return Text located at the supplied url.
     *  <br>When an error occurs the error message is returned instead of the
     *  file content.
     */
    public static String getTextFromUrl(final String url) {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            final StringBuilder result = new StringBuilder();
            inputStreamReader = new InputStreamReader(new URL(url).openStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(HtmlUtil.quoteHtml(line)).append("\r");
            }
            return result.toString();
        } catch (final IOException exception) {
            return exception.getMessage();
        } finally {
            InputOutputUtil.close(bufferedReader);
            InputOutputUtil.close(inputStreamReader);
        }
    }
}
