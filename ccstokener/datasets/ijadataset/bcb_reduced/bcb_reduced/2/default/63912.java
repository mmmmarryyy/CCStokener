import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Methods {

    private static boolean DEBUG = JCards.DEBUG;

    private static boolean VERBOSE = JCards.VERBOSE;

    protected static ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = JCards.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    protected static void displaySimpleError(JFrame framer, String error) {
        JOptionPane.showMessageDialog(framer, "ERROR! - " + error, "Error!", JOptionPane.ERROR_MESSAGE);
    }

    protected static void displaySimpleAlert(JFrame framer, String message) {
        JOptionPane.showMessageDialog(framer, message, "Alert!", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean anyMatch(String[] args, String str) {
        int size = args.length;
        if (size < 1) return false;
        boolean matches = false;
        for (int i = 0; i < size; i++) {
            if (args[i].toString().equalsIgnoreCase(str)) return true;
        }
        return matches;
    }

    public static String getCurrentDateString() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        String dateString = dateFormat.format(date) + "(Z)";
        dateString = dateString.replace("/", "-");
        dateString = dateString.replace(":", ".");
        return dateString;
    }

    public static String capFront(String str) {
        str = str.trim();
        String finalStr = "";
        int strLen = str.length();
        char ch;
        char prevCh = '.';
        int i;
        for (i = 0; i < strLen; i++) {
            ch = str.charAt(i);
            if (Character.isLetter(ch) && !Character.isLetter(prevCh) && prevCh != '/') finalStr += Character.toUpperCase(ch); else finalStr += ch;
            prevCh = ch;
        }
        return finalStr;
    }

    protected static void enoughJavaInside(double version) {
        String java_version = System.getProperty("java.version");
        String ver = java_version.trim().substring(0, 3);
        if (Double.valueOf(ver) < version) {
            displaySimpleError(null, "I'm sorry, but you need Java " + JCards.MIN_JAVA_VERSION_REQ + " or greater.\n" + "				Please upgrade Java and try again.");
            System.exit(1);
        } else {
        }
    }

    protected static boolean checkVersion(String address) {
        Scanner scanner = null;
        try {
            URL url = new URL(address);
            InputStream iS = url.openStream();
            scanner = new Scanner(iS);
            if (scanner == null && DEBUG) System.out.println("SCANNER NULL");
            String firstLine = scanner.nextLine();
            double latestVersion = Double.valueOf(firstLine.trim());
            double thisVersion = JCards.VERSION;
            if (thisVersion >= latestVersion) {
                JCards.latestVersion = true;
            } else {
                displaySimpleAlert(null, JCards.VERSION_PREFIX + latestVersion + " is available online!\n" + "Look under the file menu for a link to the download site.");
            }
        } catch (Exception e) {
            if (VERBOSE || DEBUG) {
                System.out.println("Can't decide latest version");
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }
}
