package de.sistemich.mafrasi.stopmotion.gui;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import de.sistemich.mafrasi.i18n.FileResourceBundle;

public class Messages {

    private static final ResourceBundle RESOURCE_BUNDLE;

    private static final ArrayList<String> MISSING_KEYS;

    static {
        ResourceBundle rb;
        try {
            rb = FileResourceBundle.getBundle(SMCUtilities.getResourceFile("resources/i18n/"), "language");
        } catch (IOException e1) {
            rb = new ResourceBundle() {

                @Override
                protected String handleGetObject(String key) {
                    return "i18n not found";
                }

                @Override
                public Enumeration<String> getKeys() {
                    return new Enumeration<String>() {

                        @Override
                        public boolean hasMoreElements() {
                            return false;
                        }

                        @Override
                        public String nextElement() {
                            return "";
                        }
                    };
                }
            };
            e1.printStackTrace();
        }
        RESOURCE_BUNDLE = rb;
        MISSING_KEYS = new ArrayList<String>();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (MISSING_KEYS.size() > 0) {
                    try {
                        System.out.println("Sending missing phrases to author...:");
                        String message = "Stop Motion Capture:\nFollowing phrases do nott exist: \n";
                        for (String key : MISSING_KEYS) {
                            message += key + "\n";
                            System.out.println(" - \"" + key + "\"");
                        }
                        URL url = new URL("http://mafrasi.sistemich.de/java/sendMail.php");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        PrintStream ps = new PrintStream(connection.getOutputStream());
                        ps.write(("info=" + java.net.URLEncoder.encode(message, "UTF-8")).getBytes());
                        ps.flush();
                        String r = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\Z").next();
                        System.out.println("Server answer: " + r);
                        ps.close();
                        connection.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));
    }

    /**
	 * Searches the string with the key. If that key was not found, it sends an email to the author
	 * @param key the key for the string
	 * @return a string that with the given key
	 */
    public static String getString(String key) {
        if (key == null) throw new NullPointerException("key == null");
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            if (!MISSING_KEYS.contains(key)) {
                MISSING_KEYS.add(key);
            }
            return key;
        }
    }
}
