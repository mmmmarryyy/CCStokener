package org.omegat.util;

import java.awt.GraphicsEnvironment;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.*;

/**
 * Static functions taken from
 * CommandThread to reduce file size.
 *
 * @author Keith Godfrey
 * @author Maxym Mykhalchuk
 * @author Henry Pijffers (henry.pijffers@saxnot.com)
 * @author Didier Briel
 * @author Zoltan Bartko - bartkozoltan@bartkozoltan.com
 * @author Alex Buloichik
 */
public class StaticUtils {

    /**
	 * Configuration directory on Windows platforms
	 */
    private static final String WINDOWS_CONFIG_DIR = "\\OmegaT\\";

    /**
	 * Configuration directory on UNIX platforms
	 */
    private static final String UNIX_CONFIG_DIR = "/.omegat/";

    /**
	 * Configuration directory on Mac OS X
	 */
    private static final String OSX_CONFIG_DIR = "/Library/Preferences/OmegaT/";

    /**
	 * Script directory
	 */
    private static final String SCRIPT_DIR = "script";

    /**
	 * Contains the location of the directory containing the configuration files.
	 */
    private static String m_configDir = null;

    /**
	 * Contains the location of the script dir containing the
     * exported text files.
	 */
    private static String m_scriptDir = null;

    /**
     * Builds a list of format tags within the supplied string.
     * Format tags are OmegaT style tags: &lt;xx02&gt; or &lt;/yy01&gt;.
     */
    public static void buildTagList(String str, List<String> tagList) {
        final int STATE_NORMAL = 1;
        final int STATE_COLLECT_TAG = 2;
        String tag = "";
        char c;
        int state = STATE_NORMAL;
        for (int j = 0; j < str.length(); j++) {
            c = str.charAt(j);
            if (c == '<') {
                tag = "";
                state = STATE_COLLECT_TAG;
            } else if (c == '>') {
                if (PatternConsts.OMEGAT_TAG_ONLY.matcher(tag).matches()) tagList.add(tag);
                state = STATE_NORMAL;
                tag = "";
            } else if (state == STATE_COLLECT_TAG) tag += c;
        }
    }

    /**
     * Lists all OmegaT-style tags within the supplied string.
     * Everything that looks like <code>&lt;xx0&gt;</code>,
     * <code>&lt;yy1/&gt;</code> or <code>&lt;/zz2&gt;</code>
     * is considered to probably be a tag.
     *
     * @return a string containing the tags with &lt; and &gt; around,
     * and with a space between tags if there
     * is text between them.
     */
    public static String buildPaintTagList(String str) {
        final int STATE_NORMAL = 1;
        final int STATE_COLLECT_TAG = 2;
        boolean somethingBetween = false;
        int state = STATE_NORMAL;
        String res = "";
        StringBuffer tag = new StringBuffer(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '<') {
                tag.setLength(0);
                tag.append(c);
                state = STATE_COLLECT_TAG;
            } else if (c == '>') {
                tag.append(c);
                if (PatternConsts.OMEGAT_TAG.matcher(tag).matches()) {
                    if (somethingBetween) res += " ";
                    res += tag.toString();
                    tag.setLength(0);
                    state = STATE_NORMAL;
                    somethingBetween = false;
                }
            } else if (state == STATE_COLLECT_TAG) tag.append(c); else if (!StringUtil.isEmpty(res)) somethingBetween = true;
        }
        return res;
    }

    /**
     * Returns a list of all files under the root directory
     * by absolute path.
     */
    public static void buildFileList(List<String> lst, File rootDir, boolean recursive) {
        File flist[] = null;
        try {
            flist = rootDir.listFiles();
        } catch (Exception e) {
        }
        if (flist == null) return;
        for (File file : flist) {
            if (file.isDirectory()) {
                continue;
            }
            lst.add(file.getAbsolutePath());
        }
        if (recursive) {
            for (File file : flist) {
                if (isProperDirectory(file)) {
                    buildFileList(lst, file, true);
                }
            }
        }
    }

    public static void buildDirList(List<String> lst, File rootDir) {
        File[] flist = rootDir.listFiles();
        for (File file : flist) {
            if (isProperDirectory(file)) {
                lst.add(file.getAbsolutePath());
                buildDirList(lst, file);
            }
        }
    }

    /**
     * Returns the names of all font families available.
     */
    public static String[] getFontNames() {
        GraphicsEnvironment graphics;
        graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return graphics.getAvailableFontFamilyNames();
    }

    private static final String CVS_SVN_FOLDERS = "(CVS)|(.svn)|(_svn)";

    private static final Pattern IGNORED_FOLDERS = Pattern.compile(CVS_SVN_FOLDERS);

    /**
     * Tests whether a directory has to be used
     * @return <code>true</code> or <code>false</code>
     */
    private static boolean isProperDirectory(File file) {
        if (file.isDirectory()) {
            Matcher directoryMatch = IGNORED_FOLDERS.matcher(file.getName());
            if (directoryMatch.matches()) return false; else return true;
        } else return false;
    }

    /**
     * Converts a single char into valid XML.
     * Output stream must convert stream to UTF-8 when saving to disk.
     */
    public static String makeValidXML(char c) {
        switch(c) {
            case '&':
                return "&amp;";
            case '>':
                return "&gt;";
            case '<':
                return "&lt;";
            case '"':
                return "&quot;";
            default:
                return String.valueOf(c);
        }
    }

    /**
     * Converts a stream of plaintext into valid XML.
     * Output stream must convert stream to UTF-8 when saving to disk.
     */
    public static String makeValidXML(String plaintext) {
        char c;
        StringBuffer out = new StringBuffer();
        String text = fixChars(plaintext);
        for (int i = 0; i < text.length(); i++) {
            c = text.charAt(i);
            out.append(makeValidXML(c));
        }
        return out.toString();
    }

    /** Compresses spaces in case of non-preformatting paragraph. */
    public static String compressSpaces(String str) {
        int strlen = str.length();
        StringBuffer res = new StringBuffer(strlen);
        boolean wasspace = true;
        for (int i = 0; i < strlen; i++) {
            char ch = str.charAt(i);
            boolean space = Character.isWhitespace(ch);
            if (space) {
                if (!wasspace) wasspace = true;
            } else {
                if (wasspace && res.length() > 0) res.append(' ');
                res.append(ch);
                wasspace = false;
            }
        }
        return res.toString();
    }

    /**
     * Extracts an element of a class path.
     *
     * @param fullcp the classpath
     * @param posInsideElement position inside a class path string, that fits 
     *                          inside some classpath element.
     */
    private static String classPathElement(String fullcp, int posInsideElement) {
        int semicolon1 = fullcp.lastIndexOf(File.pathSeparatorChar, posInsideElement);
        int semicolon2 = fullcp.indexOf(File.pathSeparatorChar, posInsideElement);
        if (semicolon1 < 0) semicolon1 = -1;
        if (semicolon2 < 0) semicolon2 = fullcp.length();
        return fullcp.substring(semicolon1 + 1, semicolon2);
    }

    /** Trying to see if this ending is inside the classpath */
    private static String tryThisClasspathElement(String cp, String ending) {
        try {
            int pos = cp.indexOf(ending);
            if (pos >= 0) {
                String path = classPathElement(cp, pos);
                path = path.substring(0, path.indexOf(ending));
                return path;
            }
        } catch (Exception e) {
        }
        return null;
    }

    /** Caching install dir */
    private static String INSTALLDIR = null;

    /**
     * Returns OmegaT installation directory.
     * The code uses this method to look up for OmegaT documentation.
     */
    public static String installDir() {
        if (INSTALLDIR != null) return INSTALLDIR;
        String cp = System.getProperty("java.class.path");
        String path;
        path = tryThisClasspathElement(cp, OConsts.APPLICATION_JAR);
        if (path == null) path = tryThisClasspathElement(cp, OConsts.DEBUG_CLASSPATH);
        if (path == null) path = ".";
        path = new File(path).getAbsolutePath();
        INSTALLDIR = path;
        return path;
    }

    /**
     * Returns the location of the configuration directory, depending on
     * the user's platform. Also creates the configuration directory,
     * if necessary. If any problems occur while the location of the
     * configuration directory is being determined, an empty string will
     * be returned, resulting in the current working directory being used.
     *
     * Windows XP :  <Documents and Settings>\<User name>\Application Data\OmegaT
     * Windows Vista : User\<User name>\AppData\Roaming
     * Linux:    <User Home>/.omegat
     * Solaris/SunOS:  <User Home>/.omegat
     * FreeBSD:  <User Home>/.omegat
     * Mac OS X: <User Home>/Library/Preferences/OmegaT
     * Other:    User home directory
     *
     * @return The full path of the directory containing the OmegaT
     *         configuration files, including trailing path separator.
     *
     * @author Henry Pijffers (henry.pijffers@saxnot.com)
     */
    public static String getConfigDir() {
        if (m_configDir != null) return m_configDir;
        String cd = RuntimePreferences.getConfigDir();
        if (cd != null) {
            m_configDir = new File(cd).getAbsolutePath() + File.separator;
            return m_configDir;
        }
        String os;
        String home;
        try {
            os = System.getProperty("os.name");
            home = System.getProperty("user.home");
        } catch (SecurityException e) {
            m_configDir = new File(".").getAbsolutePath() + File.separator;
            Log.logErrorRB("SU_USERHOME_PROP_ACCESS_ERROR");
            Log.log(e.toString());
            return m_configDir;
        }
        if ((os == null) || (os.length() == 0) || (home == null) || (home.length() == 0)) {
            m_configDir = new File(".").getAbsolutePath() + File.separator;
            return m_configDir;
        }
        if (os.startsWith("Windows")) {
            File appDataFile = new File(home, "Application Data");
            String appData = null;
            if (appDataFile.exists()) appData = appDataFile.getAbsolutePath(); else {
                File appDataFileVista = new File(home, "AppData\\Roaming");
                if (appDataFileVista.exists()) appData = appDataFileVista.getAbsolutePath();
            }
            if ((appData != null) && (appData.length() > 0)) {
                m_configDir = appData + WINDOWS_CONFIG_DIR;
            } else {
                m_configDir = home + WINDOWS_CONFIG_DIR;
            }
        } else if (os.equals("Linux") || os.equals("SunOS") || os.equals("Solaris") || os.equals("FreeBSD")) {
            m_configDir = home + UNIX_CONFIG_DIR;
        } else if (os.equals("Mac OS X")) {
            m_configDir = home + OSX_CONFIG_DIR;
        } else {
            m_configDir = home + File.separator;
        }
        if (m_configDir.length() > 0) {
            try {
                File dir = new File(m_configDir);
                if (!dir.exists()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        Log.logErrorRB("SU_CONFIG_DIR_CREATE_ERROR");
                        m_configDir = new File(".").getAbsolutePath() + File.separator;
                    }
                }
            } catch (SecurityException e) {
                m_configDir = new File(".").getAbsolutePath() + File.separator;
                Log.logErrorRB("SU_CONFIG_DIR_CREATE_ERROR");
                Log.log(e.toString());
            }
        }
        return m_configDir;
    }

    public static String getScriptDir() {
        if (m_scriptDir != null) return m_scriptDir;
        m_scriptDir = getConfigDir() + SCRIPT_DIR + File.separator;
        try {
            File dir = new File(m_scriptDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    Log.logErrorRB("SU_SCRIPT_DIR_CREATE_ERROR");
                    m_scriptDir = getConfigDir();
                }
            }
        } catch (SecurityException e) {
            m_scriptDir = getConfigDir();
            Log.logErrorRB("SU_SCRIPT_DIR_CREATE_ERROR");
            Log.log(e.toString());
        }
        return m_scriptDir;
    }

    /**
      * Returns true if running on Mac OS X
      */
    public static boolean onMacOSX() {
        String os;
        try {
            os = System.getProperty("os.name");
        } catch (SecurityException e) {
            return false;
        }
        return os.equals("Mac OS X");
    }

    /**
     * Strips all XML tags (converts to plain text).
     */
    public static String stripTags(String xml) {
        return PatternConsts.OMEGAT_TAG.matcher(xml).replaceAll("");
    }

    /**
     * Compares two strings for equality.
     * Handles nulls: if both strings are nulls they are considered equal.
     */
    public static boolean equal(String one, String two) {
        return (one == null && two == null) || (one != null && one.equals(two));
    }

    /**
     * Encodes the array of bytes to store them in a plain text file.
     */
    public static String uuencode(byte[] buf) {
        if (buf.length <= 0) return new String();
        StringBuffer res = new StringBuffer();
        res.append(buf[0]);
        for (int i = 1; i < buf.length; i++) {
            res.append('#');
            res.append(buf[i]);
        }
        return res.toString();
    }

    /**
     * Decodes the array of bytes that was stored in a plain text file
     * as a string, back to array of bytes.
     */
    public static byte[] uudecode(String buf) {
        String[] bytes = buf.split("#");
        byte[] res = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            try {
                res[i] = Byte.parseByte(bytes[i]);
            } catch (NumberFormatException e) {
                res[i] = 0;
            }
        }
        return res;
    }

    /**
     * Makes the file name relative to the given path.
     */
    public static String makeFilenameRelative(String filename, String path) {
        if (filename.toLowerCase().startsWith(path.toLowerCase())) return filename.substring(path.length()); else return filename;
    }

    /**
      * Escapes the passed string for use in regex matching,
      * so special regex characters are interpreted as normal
      * characters during regex searches.
      *
      * This is done by prepending a backslash before each
      * occurrence of the following characters: \^.*+[]{}()&|-:=?!<>
      *
      * @param text The text to escape
      *
      * @return The escaped text
      *
      * @author Henry Pijffers (henry.pijffers@saxnot.com)
      */
    public static String escapeNonRegex(String text) {
        return escapeNonRegex(text, true);
    }

    /**
      * Escapes the passed string for use in regex matching,
      * so special regex characters are interpreted as normal
      * characters during regex searches.
      *
      * This is done by prepending a backslash before each
      * occurrence of the following characters: \^.+[]{}()&|-:=!<>
      *
      * If the parameter escapeWildcards is true, asterisks (*) and
      * questions marks (?) will also be escaped. If false, these
      * will be converted to regex tokens (* -> 
      *
      * @param text            The text to escape
      * @param escapeWildcards If true, asterisks and question marks are also escaped.
      *                        If false, these are converted to there regex equivalents.
      *
      * @return The escaped text
      *
      * @author Henry Pijffers (henry.pijffers@saxnot.com)
      */
    public static String escapeNonRegex(String text, boolean escapeWildcards) {
        text = text.replaceAll("\\\\", "\\\\\\\\");
        String escape = "^.+[]{}()&|-:=!<>";
        for (int i = 0; i < escape.length(); i++) text = text.replaceAll("\\" + escape.charAt(i), "\\\\" + escape.charAt(i));
        if (escapeWildcards) {
            text = text.replaceAll("\\?", "\\\\?");
            text = text.replaceAll("\\*", "\\\\*");
        } else {
            text = text.replaceAll("\\?", "\\\\S");
            text = text.replaceAll("\\*", "\\\\S*");
        }
        return text;
    }

    /**
      * Formats UI strings.
      *
      * Note: This is only a first attempt at putting right what goes
      *       wrong in MessageFormat. Currently it only duplicates
      *       single quotes, but it doesn't even test if the string
      *       contains parameters (numbers in curly braces), and it
      *       doesn't allow for string containg already escaped quotes.
      *
      * @param str       The string to format
      * @param arguments Arguments to use in formatting the string
      *
      * @return The formatted string
      *
      * @author Henry Pijffers (henry.pijffers@saxnot.com)
      */
    public static String format(String str, Object... arguments) {
        str = str.replaceAll("'", "''");
        return MessageFormat.format(str, arguments);
    }

    /**
     * dowload a file from the internet
     */
    public static String downloadFileToString(String urlString) throws IOException {
        URLConnection urlConn = null;
        InputStream in = null;
        URL url = new URL(urlString);
        urlConn = url.openConnection();
        in = urlConn.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            LFileCopy.copy(in, out);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /**
     * dowload a file to the disk
     */
    public static void downloadFileToDisk(String address, String filename) throws MalformedURLException {
        URLConnection urlConn = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            URL url = new URL(address);
            urlConn = url.openConnection();
            in = urlConn.getInputStream();
            out = new BufferedOutputStream(new FileOutputStream(filename));
            byte[] byteBuffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(byteBuffer)) != -1) {
                out.write(byteBuffer, 0, numRead);
            }
        } catch (IOException ex) {
            Log.logErrorRB("IO exception");
            Log.log(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    public static void extractFileFromJar(String archive, List<String> filenames, String destination) throws IOException {
        JarFile jar = new JarFile(archive);
        Enumeration<JarEntry> entryEnum = jar.entries();
        while (entryEnum.hasMoreElements()) {
            JarEntry file = entryEnum.nextElement();
            if (filenames.contains(file.getName())) {
                File f = new File(destination + File.separator + file.getName());
                InputStream in = jar.getInputStream(file);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
                byte[] byteBuffer = new byte[1024];
                int numRead = 0;
                while ((numRead = in.read(byteBuffer)) != -1) {
                    out.write(byteBuffer, 0, numRead);
                }
                in.close();
                out.close();
            }
        }
    }

    public static String ltrim(String source) {
        return source.replaceAll("^\\s+", "");
    }

    public static String rtrim(String source) {
        return source.replaceAll("\\s+$", "");
    }

    /**
     * Replace invalid XML chars by spaces. See supported chars at
     * http://www.w3.org/TR/2006/REC-xml-20060816/#charsets.
     * 
     * @param str input stream
     * @return result stream
     */
    public static String fixChars(String str) {
        char[] result = new char[str.length()];
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0x20) {
                if (c != 0x09 && c != 0x0A && c != 0x0D) {
                    c = ' ';
                }
            } else if (c >= 0x20 && c <= 0xD7FF) {
            } else if (c >= 0xE000 && c <= 0xFFFD) {
            } else if (c >= 0x10000 && c <= 0x10FFFF) {
            } else {
                c = ' ';
            }
            result[i] = c;
        }
        return new String(result);
    }
}
