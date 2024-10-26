package pub.utils;

import java.io.*;
import java.net.*;

/**
 * Perform network pulls to grab external content.
 */
public class Pull {

    /**
     * Performs an HTTP GET, given an url and a query string.
     */
    public static String doGet(String http_url, String get_data) {
        URL url;
        try {
            if ((get_data != "") && (get_data != null)) {
                url = new URL(http_url + "?" + get_data);
            } else {
                url = new URL(http_url);
            }
            URLConnection conn = url.openConnection();
            InputStream stream = new BufferedInputStream(conn.getInputStream());
            try {
                StringBuffer b = new StringBuffer();
                int ch;
                while ((ch = stream.read()) != -1) b.append((char) ch);
                return b.toString();
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            ;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Performs an HTTP POST, given an url and the parameters of the POST.
     * Returns the results of the POST as a String.
     */
    public static String doPost(String http_url, String post_data) {
        if (post_data == null) {
            post_data = "";
        }
        try {
            URLConnection conn = new URL(http_url).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(post_data);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = in.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }
            return buffer.toString();
        } catch (IOException e) {
            ;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return null;
    }
}
