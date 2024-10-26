package tests.jd;

import java.io.*;
import java.net.*;

/**
 *
 * @author  jdepons
 */
public class Scrape {

    /** Creates a new instance of Scrape */
    public Scrape() {
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://www.yahoo.com");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        String page = "";
        while ((line = br.readLine()) != null) {
            page += line;
        }
        System.out.println(page);
    }
}
