package shieh.pnn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

public class Tools {

    public static Vector<String> readFileFromURL(URL url) {
        Vector<String> text = new Vector<String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                text.add(line);
            }
            in.close();
        } catch (Exception e) {
            return null;
        }
        return text;
    }

    public static String getUserDirectory() {
        return System.getProperty("user.dir");
    }

    public static String getFilename(String fullpath) {
        int p = Math.max(fullpath.lastIndexOf("/"), fullpath.lastIndexOf("\\"));
        return fullpath.substring(p + 1);
    }

    public static void warning(String message) {
        System.out.printf("Warning: %s.\n", message);
    }

    public static void error(String message) {
        error(message, -1);
    }

    public static void error(String message, int exitCode) {
        System.out.printf("Error: %s.\n", message);
        System.exit(exitCode);
    }
}
