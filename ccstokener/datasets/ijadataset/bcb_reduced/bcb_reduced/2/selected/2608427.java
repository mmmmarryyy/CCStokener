package src.update;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import src.Constants;
import static src.Constants.Updater;

/**
 * This class checks whether there exists an update of wiki2xhtml
 * by reading out a file in the web.
 * TODO 0 update
 *
 * @author Simon Eugster
 */
public final class UpdateChecker {

    /** Contains the update information from the file in the web */
    private static final StringBuffer content = new StringBuffer();

    public static boolean isEmpty() {
        return content.length() == 0;
    }

    public static String getProperty(Constants.Updater property) {
        Matcher m = Pattern.compile("(?m)^" + property.keyword() + ":\\s*(.+)$").matcher(content);
        if (m.find()) return m.group(1); else return "";
    }

    /** Checks whether a newer version of wiki2xhtml is available in the web. Call {@link #refresh()} first. */
    public static boolean isNewerVersionAvailable() {
        String newestVersion = getProperty(Constants.Updater.currentVersion);
        if (newestVersion.length() == 0) return false;
        if (Constants.Wiki2xhtml.versionNumber.equals(newestVersion)) return false;
        for (String s : Constants.Wiki2xhtml.versionsTillNow) {
            if (s.equals(newestVersion)) {
                return false;
            }
        }
        return true;
    }

    /** Checks whether a property is set to a value */
    public static boolean isPropertyEqualTo(Constants.Updater property, String value) {
        return getProperty(property).equals(value);
    }

    /** Checks whether the size of the input filed has to be enlarged for displaying the necessary text.  Call {@link #refresh()} first. */
    public static int getIncreaseSize() {
        String increaseSize = getProperty(Constants.Updater.increaseSizeBy);
        try {
            return Integer.parseInt(increaseSize);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Loads the file containing information about the current version of wiki2xhtml from the web. */
    public static void refresh() {
        URL[] urls = Constants.Wiki2xhtml.getUpdateURLs();
        content.setLength(0);
        InputStream is = null;
        BufferedReader br = null;
        for (int i = 0; i < urls.length; i++) {
            try {
                is = urls[i].openStream();
                br = new BufferedReader(new InputStreamReader(is));
                String s;
                while ((s = br.readLine()) != null) {
                    if (s.length() == 0) continue;
                    if (s.startsWith("--")) break;
                    content.append(s + '\n');
                }
                is.close();
                break;
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + urls[i].getHost() + urls[i].getPath());
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    /** Creates a message describing the current actuality status of the local program */
    public static StringBuffer getUpdateMessage() {
        StringBuffer out = new StringBuffer();
        refresh();
        if (isNewerVersionAvailable()) {
            out.append(String.format("<p>New version: <strong>%s</strong> from %s.</p>", getProperty(Updater.currentVersionFullname), getProperty(Updater.currentVersionDate)));
            out.append(String.format("<p>Changes in this version: %s</p>", getProperty(Updater.htmlVersionNotes)));
        } else {
            out.append("<p>Your version of wiki2xhtml is up-to-date.</p>");
        }
        return out;
    }
}
