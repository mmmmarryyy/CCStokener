package utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Raimon Bosch
 */
public class HttpValidator {

    /** Creates a new instance of MyHTTP */
    public HttpValidator() {
    }

    public static int validate(String url) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) (new URL(url)).openConnection();
        } catch (MalformedURLException ex) {
            return -1;
        } catch (IOException ex) {
            return -2;
        }
        try {
            if (con != null && con.getResponseCode() != 200) {
                return con.getResponseCode();
            } else if (con == null) {
                return -3;
            }
        } catch (IOException ex) {
            return -4;
        }
        return 1;
    }
}
