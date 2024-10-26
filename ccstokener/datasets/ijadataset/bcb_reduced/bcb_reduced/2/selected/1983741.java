package gem.parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Reads a given URL.
 *
 * @author Ozgun Babur
 */
public class Fetcher {

    public static BufferedReader fetch(String address) throws Throwable {
        URL url = new URL(address);
        BufferedReader reader = null;
        do {
            try {
                URLConnection con = url.openConnection();
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } catch (Exception e) {
                System.err.println(e);
                Thread.currentThread().wait(2000);
            }
        } while (reader == null);
        return reader;
    }
}
