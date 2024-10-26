package doors.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General-purpose utilities
 *
 * @author Adam Buckley
 */
public class Util {

    private static final Logger logger = Logger.getLogger("Util");

    /**
	 * Converts "one,two,three" into {"one", "two", "three"}
	 */
    public static String[] parseList(String string) {
        List list = new Vector();
        StringTokenizer st = new StringTokenizer(string, ",");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return (String[]) list.toArray(new String[0]);
    }

    /**
	 * Displays <code>msg</code> and then blocks until the user types
	 * in <code>keyword</code> and presses enter.  If the user enters another
	 * value, <code>msg</code> is displayed.  
	 */
    public static void typeToExit(String msg, String keyword) {
        System.out.println(msg);
        java.io.BufferedReader kb = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        try {
            while (kb.readLine().equalsIgnoreCase(keyword) == false) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Returns the local host name.  If an error occurs, this function returns
	 * "localhost99" where 99 is a two digit random number.
	 */
    public static String getLocalHostName() {
        String name = null;
        try {
            name = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            logger.log(Level.SEVERE, "Cannot get name of localhost", e);
            int randomNumber = new Random().nextInt() % 99;
            name = "localhost" + randomNumber;
        }
        return name;
    }

    /**
	 * Returns <code>str</code> with the trailing digits remove
	 */
    public static String stripTrailingDigits(String str) {
        int crop = str.length() - 1;
        while (str.charAt(crop) >= 48 && str.charAt(crop) <= 57) crop--;
        return str.substring(0, crop + 1);
    }

    /**
	 * Returns a <code>String</code> containing the detail of a
	 * <code>Throwable</code> object
	 *
	 * @param extensive if true, this method also returns the
	 * getLocalizedMessage() and printStackTrace() data from the exception
	 */
    public static String getThrowableDetail(Throwable e, boolean extensive) {
        StringBuffer sb = new StringBuffer("getThrowableDetail(e): ");
        if (e == null) {
            sb.append("Exception was null!");
        } else {
            sb.append("\n");
            if (extensive) if (e.getLocalizedMessage() != null) {
                sb.append("getLocalizedMessage(): ");
                sb.append(e.getLocalizedMessage() + "\n");
            }
            if (e.getMessage() != null) {
                sb.append("getMessage(): ");
                sb.append(e.getMessage() + "\n");
            }
            sb.append("toString(): ");
            sb.append(e.toString() + "\n");
            if (extensive) {
                sb.append("printStackTrace(): ");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream(baos));
                sb.append(baos.toString());
            }
        }
        return sb.toString();
    }

    /**
	 * Returns the contents of a text file as a <code>String</code>
	 */
    public static String readUrl(URL url) throws IOException {
        java.io.BufferedReader is = new java.io.BufferedReader(new java.io.InputStreamReader(url.openStream()));
        StringBuffer sb = new StringBuffer();
        int c = is.read();
        while (c != -1) {
            sb.append((char) c);
            c = is.read();
        }
        return sb.toString();
    }

    /**
	 * Writes a String into a file
	 */
    public static void writeFile(File file, String str) throws java.io.IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(str);
        fw.flush();
        fw.close();
    }

    /**
	 * Converts a <code>String</code> into a list of character values.
	 * 
	 * @param str the <code>String</code> to be converted.
	 * @return a string representing a list of comma-separated character values.
	 */
    public static String stringToDec(String str) {
        StringBuffer r = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            r.append((int) str.charAt(i));
            if (i != (str.length() - 1)) r.append(", ");
        }
        return r.toString();
    }

    /**
	 * Converts a <code>String</code> into a list of hexadecimal character values.
	 * 
	 * @param str the <code>String</code> to be converted.
	 * @return a string representing a list of comma-separated hexadecimal character values.
	 */
    public static String stringToHex(String str) {
        StringBuffer r = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            r.append(Integer.toHexString(str.charAt(i)));
            if (i != (str.length() - 1)) r.append(", ");
        }
        return r.toString();
    }

    /**
	 * Formats a doors.AbsoluteTime (a <code>long</code>) to make it more
	 * readable.  Implemented by applying the default locale formatting rules
	 * to a long integer.
	 */
    public static String formatAbsoluteTime(long t) {
        return java.text.NumberFormat.getInstance().format(t);
    }

    /**
	 * Returns a double rounded to one decimal place
	 */
    public static double toOneDP(double d) {
        long r = Math.round(d * 10);
        return (double) r / 10;
    }

    /**
	 * Converts URL-encoded characters back into their original symbols.
	 * 
	 * For example, a value of %3F is converted into the ? character, and a 
	 * value of %20 is converted into a space.
 	 *
	 * @param str a String which may containing encoded characters
	 * @returns the same String whith the encoded characters converted back
	 * into symbols
	 */
    public static String decodeUrlCharacters(String str) {
        StringBuffer r = new StringBuffer();
        StringTokenizer st = new StringTokenizer(str, "%");
        String token;
        boolean first = true;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (first && (!str.startsWith("%"))) {
                first = false;
                r.append(token);
            } else {
                try {
                    int i = Integer.valueOf(token.substring(0, 2), 16).intValue();
                    r.append((char) i);
                    r.append(token.substring(2));
                } catch (NumberFormatException e) {
                    r.append("%" + token);
                }
            }
        }
        return r.toString();
    }

    /**
	 * printUsage
	 */
    public static void printOut(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = br.readLine();
        while (line != null) {
            System.out.println(line);
            line = br.readLine();
        }
    }

    public static String readPropertyOrExit(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null) {
            logger.severe("Error: Cannot read parameter " + name + " in config file!");
            System.exit(1);
        }
        return value;
    }

    /**
	 * Sets the formatter of any ConsoleHandlers to LoggerFormatter
	 */
    public static void initialiseLogger() {
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            Handler handler = handlers[i];
            if (handler instanceof ConsoleHandler) {
                handler.setFormatter(new SuccinctlLogFormatter());
            }
        }
    }

    /**
	 * @return true if the current platform is any Window variant
	 */
    public static boolean isWin32() {
        return System.getProperty("os.name", "unknown").toLowerCase().indexOf("win") != -1;
    }
}
