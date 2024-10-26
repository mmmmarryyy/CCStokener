package net.sf.aft.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.net.URL;
import org.apache.tools.ant.BuildException;

/**
 * Various utility methods.
 *
 * @author <a href="mailto:ovidiu@cup.hp.com">Ovidiu Predescu</a>
 * @author <a href="mailto:jefft@apache.org">Jeff Turner</a>
 * @version $Revision: 1.1.1.1 $ $Date: 2001/12/31 16:24:54 $
 * @since October 1, 2001
 */
public class Utils {

    private static final char QUOT_BEGIN = '�';

    private static final char QUOT_END = '�';

    /**
   * Returns the content of an URL resource and returns it into a
   * string. Relative <code>file:</code> URLs can be specified, they
   * are relative to the current directory.
   *
   * @param href a <code>String</code> value
   * @return a <code>String</code> value
   * @exception BuildException if an error occurs
   */
    public static String getURLContent(String href) throws BuildException {
        URL url = null;
        String content;
        try {
            URL context = new URL("file:" + System.getProperty("user.dir") + "/");
            url = new URL(context, href);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            StringBuffer stringBuffer = new StringBuffer();
            char[] buffer = new char[1024];
            int len;
            while ((len = isr.read(buffer, 0, 1024)) > 0) stringBuffer.append(buffer, 0, len);
            content = stringBuffer.toString();
            isr.close();
        } catch (Exception ex) {
            throw new BuildException("Cannot get content of URL " + href + ": " + ex);
        }
        return content;
    }

    /**
   * String comparison with precise error reporting.
   * Compares an expected and actual String for equality.
   * @return An error message if the strings don't match, <code>null</code> if
   * they do.
   */
    public static String compare(String expected, String actual) {
        if (expected == null || actual == null) return "Null string";
        if (expected.length() != actual.length()) {
            return "Wrong size: expected " + expected.length() + ", got " + actual.length();
        }
        for (int i = 0; i < expected.length(); i++) {
            if (expected.charAt(i) != actual.charAt(i)) {
                return "Error at " + i + ": expected " + expected.charAt(1) + ", got " + actual.charAt(i);
            }
        }
        return null;
    }

    /**
   * String comparison with precise error reporting, ignoring whitespace.
   * Equivalent to {@link #compare}, but ignores whitespace [\t\n\r\f].
   *
   * @return An error message if the strings don't match, <code>null</code> if
   * they do.
   */
    public static String compareWeak(String expected, String actual) {
        if (expected == null || actual == null) return "Null string";
        StringTokenizer expectedST = new StringTokenizer(expected);
        StringTokenizer actualST = new StringTokenizer(actual);
        while (expectedST.hasMoreTokens() && actualST.hasMoreTokens()) {
            String expectedTok = expectedST.nextToken();
            String actualTok = actualST.nextToken();
            if (!expectedTok.equals(actualTok)) {
                return "The received word " + QUOT_BEGIN + actualTok + QUOT_END + " did not match expected word " + QUOT_BEGIN + expectedTok + QUOT_END;
            }
        }
        if (expectedST.hasMoreTokens()) return "Fewer bytes returned than expected";
        if (actualST.hasMoreTokens()) return "More bytes returned than expected";
        return null;
    }
}
