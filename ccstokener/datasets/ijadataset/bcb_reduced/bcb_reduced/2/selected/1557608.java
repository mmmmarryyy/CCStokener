package com.io_software.utils.web;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.net.URL;
import java.util.Set;
import java.util.Locale;
import java.util.HashSet;
import java.util.Date;
import gnu.regexp.RE;
import gnu.regexp.REException;
import gnu.regexp.REMatch;
import gnu.regexp.REMatchEnumeration;
import gnu.regexp.CharIndexedReader;
import com.abb.util.TeeReader;
import com.io_software.utils.sms.SMSSender;
import com.io_software.catools.search.ReaderFilter;

/** Takes a reader returned from the DWD weather report and
    parses its output into one string for the forecast and one for the
    outlook.<p>

    @author Axel Uhl
    @version $Id: DWDReaderFilter.java,v 1.2 2001/07/14 18:26:25 uhl Exp $
 */
public class DWDReaderFilter {

    /** loads the latest weather forecast and SMS-es it to the given mobile
	phone number.

	@param args the phone number is expected in <tt>args[0]</tt>
	@see com.io_software.utils.sms.SMSSender
    */
    public static void main(String[] args) {
        try {
            forecastPattern = new RE("Vorhersage:</b> *\r?\n</p>" + " *\r?\n<p align=justify>" + " *\r?\n(.*)</p></p>.*" + "<p><b>Weitere Aussichten:</b>" + " *\r?\n<p align=justify> *\r?\n" + "([^<]*)</p></p>", RE.REG_MULTILINE | RE.REG_DOT_NEWLINE);
            URL url = new URL("http://www.dwd.de/ext/e2/dlwett.htm");
            System.out.println("reading weather forecast");
            Reader r = new InputStreamReader(url.openStream());
            StringBuffer sb = new StringBuffer();
            int read;
            do {
                read = r.read();
                if (read != -1) sb.append((char) read);
            } while (read != -1);
            r.close();
            System.out.println("extracting forecast and outlook");
            REMatch match = forecastPattern.getMatch(sb);
            System.out.println("sending as SMS to " + args[0]);
            String forecast = strip(match.toString(1));
            String outlook = strip(match.toString(2));
            new SMSSender().sendLong(args[0], forecast + "; " + outlook);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** removes all multiple blank characters and replaces carriage returns /
	newlines by blanks.

	@param s string to transform
	@return transformed string
    */
    private static String strip(String s) {
        StringBuffer result = new StringBuffer();
        boolean lastWasBlank = true;
        for (int i = 0; i < s.length(); i++) {
            if (" \t\r\n".indexOf(s.charAt(i)) >= 0) {
                if (!lastWasBlank) result.append(" ");
                lastWasBlank = true;
            } else {
                lastWasBlank = false;
                result.append(s.charAt(i));
            }
        }
        return result.toString();
    }

    /** matches the forecast section of the weather reports page. Matches the
	forecast in group #1, the outlook in group #2.
      */
    protected static RE forecastPattern;
}
