package de.moonflower.jfritz.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.moonflower.jfritz.exceptions.WrongPasswordException;

/**
 * Static class for data retrieval from the fritz box
 * and for several global functions
 * 
 * @author akw
 * 
 */
public class JFritzUtils {

    public static final String FILESEP = System.getProperty("file.separator");

    public static final String PATHSEP = System.getProperty("path.separator");

    public static final String binID = FILESEP + "jfritz.jar";

    public static final String rootID = FILESEP;

    /**
	 * This constant can be used to search for the lang-directory.
	 * @see #getFullPath(String)
	 */
    public static final String langID = FILESEP + "lang";

    private static final String PATTERN_WAIT_FOR_X_SECONDS = "var loginBlocked = parseInt\\(\"([^\"]*)\\),10\\);";

    /**
	 * fetches html data from url using POST requests
	 * 
	 * @param urlstr
	 * @param postdata
	 * @return html data
	 * @throws WrongPasswordException
	 * @throws IOException
	 */
    public static String fetchDataFromURL(String urlstr, String postdata, boolean retrieveData) throws WrongPasswordException, IOException {
        URL url = null;
        URLConnection urlConn;
        DataOutputStream printout;
        String data = "";
        boolean wrong_pass = false;
        Debug.debug("Urlstr: " + urlstr);
        Debug.debug("Postdata: " + postdata);
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e) {
            Debug.error("URL invalid: " + urlstr);
            throw new MalformedURLException("URL invalid: " + urlstr);
        }
        if (url != null) {
            urlConn = url.openConnection();
            urlConn.setConnectTimeout(5000);
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            if (postdata != null) {
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                try {
                    printout = new DataOutputStream(urlConn.getOutputStream());
                    printout.writeBytes(postdata);
                    printout.flush();
                    printout.close();
                } catch (SocketTimeoutException ste) {
                    Debug.error("Could not fetch data from url: " + ste.toString());
                }
            }
            BufferedReader d;
            try {
                d = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                String str;
                while (null != ((str = HTMLUtil.stripEntities(d.readLine())))) {
                    if ((str.indexOf("Das angegebene Kennwort ist ung�ltig") > 0) || (str.indexOf("Password not valid") > 0)) {
                        wrong_pass = true;
                    }
                    if (retrieveData) {
                        data += str;
                    }
                }
                d.close();
            } catch (IOException e1) {
                throw new IOException("Network unavailable");
            }
            if (wrong_pass) {
                int wait = 3;
                Pattern waitSeconds = Pattern.compile(PATTERN_WAIT_FOR_X_SECONDS);
                Matcher m = waitSeconds.matcher(data);
                if (m.find()) {
                    try {
                        wait = Integer.parseInt(m.group(1));
                    } catch (NumberFormatException nfe) {
                        wait = 3;
                    }
                }
                throw new WrongPasswordException("Password invalid", wait);
            }
        }
        return data;
    }

    /**
	 * removes all duplicate whitespaces from inputStr
	 * 
	 * @param inputStr
	 * @return outputStr
	 */
    public static String removeDuplicateWhitespace(String inputStr) {
        Pattern p = Pattern.compile("\\s+");
        Matcher matcher = p.matcher(inputStr);
        String outputStr = matcher.replaceAll(" ");
        outputStr.replaceAll(">\\s+<", "><");
        return outputStr;
    }

    /**
	 * creates a String with version and date of CVS Id-Tag
	 * 
	 * @param tag
	 * @return String with version and date of CVS Id-Tag
	 */
    public static String getVersionFromCVSTag(String tag) {
        String[] parts = tag.split(" ");
        return "CVS v" + parts[2] + " (" + parts[3] + ")";
    }

    /**
	 * Wandelt einen String in einen boolean-Wert um
	 * @param input
	 * @return boolean value of input
	 */
    public static boolean parseBoolean(String input) {
        if (input != null && input.equalsIgnoreCase("true")) return true; else return false;
    }

    public static Date setStartOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    public static Date setEndOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    public static String convertSpecialChars(String input) {
        String out = input;
        out = out.replaceAll("&", "&#38;");
        out = out.replaceAll("'", "&#39;");
        out = out.replaceAll("<", "&#60;");
        out = out.replaceAll(">", "&#62;");
        out = out.replaceAll("\"", "&#34;");
        out = out.replaceAll("=", "&#61;");
        return out;
    }

    public static String deconvertSpecialChars(String input) {
        String out = input;
        out = out.replaceAll("&#38;", "&");
        out = out.replaceAll("&#39;", "'");
        out = out.replaceAll("&#60;", "<");
        out = out.replaceAll("&#62;", ">");
        out = out.replaceAll("&#61;", "=");
        out = out.replaceAll("&#34;", "\"");
        return out;
    }

    /**
	 * This function tries to guess the full path for the given
	 * subdirectory.<br />
	 * 
	 * <ol>
	 * <li>It searches for the directory in the class path.</li>
	 * <li>If it does not find it there, it assumes that jfritz.jar and
	 *     the subdirectory are in the same dir.</li>
	 * <li>If for some reason it fails to generate the full path to the
	 *     jfritz binary, it assumes that the subdirectory is in the
	 *     current working directory.</li>  
	 * </ol>
	 * 
	 * @param subDir the subdirectory to search for.
	 *               The directory must start with a leading file separator
	 *               and must not end with a file separator (e.g. "/lang"
	 *               for Linux).
	 *               It's best to use the predefined constants of this class.
	 * @return the full path to the subdirectory
	 * @see #langID 
	 */
    public static String getFullPath(String subDir) {
        Debug.debug("Subdirectory: " + subDir);
        String[] classPath = System.getProperty("java.class.path").split(PATHSEP);
        String userDir = System.getProperty("user.dir");
        if (userDir.endsWith(FILESEP)) userDir = userDir.substring(0, userDir.length() - 1);
        String binDir = null;
        String langDir = null;
        for (int i = 0; i < classPath.length; i++) {
            if (classPath[i].endsWith(binID)) binDir = classPath[i].substring(0, classPath[i].length() - binID.length()); else if (classPath[i].endsWith(subDir)) langDir = classPath[i];
        }
        if (langDir == null) {
            langDir = (binDir != null) ? binDir + subDir : userDir + subDir;
        }
        Debug.debug("full path: " + langDir);
        return langDir;
    }

    public static String capitalize(String str) {
        StringBuffer strBuf = new StringBuffer();
        char ch;
        char prevCh;
        int i;
        prevCh = '.';
        for (i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            if (Character.isLetter(ch) && !Character.isLetter(prevCh)) strBuf.append(Character.toUpperCase(ch)); else strBuf.append(ch);
            prevCh = ch;
        }
        return strBuf.toString();
    }

    public static int parseInt(String property) {
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException nfe) {
            Debug.warning("error converting Int returning 0 instead");
        }
        return 0;
    }

    public static String removeLeadingSpaces(final String str) {
        String currentStr = str;
        while (currentStr.startsWith(" ")) {
            currentStr = currentStr.substring(1);
        }
        while (currentStr.startsWith(" ")) {
            currentStr = currentStr.substring(1);
        }
        return currentStr;
    }

    public static String toAscii(String str) {
        String out = "";
        for (int i = 0; i < str.length(); i++) {
            out = out + "#" + Integer.toHexString(str.charAt(i));
        }
        return out;
    }

    public static String replaceSpecialCharsUTF(final String str) {
        String currentStr = str;
        currentStr = currentStr.replaceAll("&#x[00]*C4;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*D6;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*DC;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*E4;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*F6;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*FC;", "�");
        currentStr = currentStr.replaceAll("&#x[00]*DF;", "�");
        return currentStr;
    }
}
