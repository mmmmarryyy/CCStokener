package utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.swing.JOptionPane;
import org.simpleframework.http.Request;

/**
 *
 * @author Nuno Brito, 10th August 2010 in Coimbra, Portugal.
 */
public class internet {

    static internet action = new internet();

    String[] test_connectivity = { "http://google.com", "http://cnn.com", "http://digg.com", "http://wordpress.com", "http://www.w3.org", "http://yahoo.com" };

    Boolean debug = true;

    public static boolean isInternetReachable() {
        try {
            int i = action.test_connectivity.length;
            String test = action.test_connectivity[utils.math.RandomInteger(0, i - 1)];
            debug("Testing connection to " + test);
            URL url = new URL(test);
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            Object objData = urlConnect.getContent();
            objData.toString();
        } catch (UnknownHostException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static String getTextFile(String TextFileURL) {
        String text_out = "";
        try {
            URL url = new URL(TextFileURL);
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            urlConnect.setDoInput(true);
            urlConnect.setUseCaches(false);
            DataInputStream dis;
            String s;
            dis = new DataInputStream(urlConnect.getInputStream());
            while ((s = dis.readLine()) != null) {
                if (s.length() == 0) continue;
                text_out = text_out.concat(s + "\n");
            }
            dis.close();
        } catch (UnknownHostException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
        return text_out;
    }

    /**
 * <b>Bare Bones Browser Launch for Java</b><br>
 * Utility class to open a web page from a Swing application
 * in the user's default browser.<br>
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP/Vista/7<br>
 * Example Usage:<code><br> &nbsp; &nbsp;
 *    String url = "http://www.google.com/";<br> &nbsp; &nbsp;
 *    BareBonesBrowserLaunch.openURL(url);<br></code>
 * Latest Version: <a href="http://www.centerkey.com/java/browser/">www.centerkey.com/java/browser</a><br>
 * Author: Dem Pilafian<br>
 * Public Domain Software -- Free to Use as You Like
 * @version 3.0, February 7, 2010
 */
    static final String[] browsers = { "google-chrome", "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla" };

    static final String errMsg = "Error attempting to launch web browser";

    /**
    * Opens the specified web page in the user's default browser
    * @param url A web address (URL) of a web page (ex: "http://www.google.com/")
    */
    public static void openURL(String url) {
        try {
            Class<?> d = Class.forName("java.awt.Desktop");
            d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
        } catch (Exception ignore) {
            String osName = System.getProperty("os.name");
            try {
                if (osName.startsWith("Mac OS")) {
                    Class.forName("com.apple.eio.FileManager").getDeclaredMethod("openURL", new Class[] { String.class }).invoke(null, new Object[] { url });
                } else if (osName.startsWith("Windows")) Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url); else {
                    boolean found = false;
                    for (String browser : browsers) if (!found) {
                        found = Runtime.getRuntime().exec(new String[] { "which", browser }).waitFor() == 0;
                        if (found) Runtime.getRuntime().exec(new String[] { browser, url });
                    }
                    if (!found) throw new Exception(Arrays.toString(browsers));
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, errMsg + "\n" + e.toString());
            }
        }
    }

    /** Get the value from an HTML provided on command line */
    public static String getHTMLparameter(Request request, String parameter) {
        String result = "";
        try {
            result = request.getParameter(parameter);
        } catch (IOException ex) {
        }
        if (result == null) result = "";
        return result;
    }

    private void log(String gender, String message) {
        System.out.println("internet [" + gender + "] " + message);
    }

    private static void debug(String message) {
        if (action.debug) action.log("debug", message);
    }
}
