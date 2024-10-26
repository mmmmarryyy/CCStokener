package org.azrul.epice.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

/**
 *
 * @author Azrul
 */
public class LoginUtil {

    public String login(String address, String username, String pasword) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        ResourceBundle props = ResourceBundle.getBundle("org.azrul.epice.config.epice");
        try {
            URL url = new URL(address);
            conn = url.openConnection();
            String sessionid = conn.getHeaderField("sessionid");
            if (sessionid != null) {
                if (!("loginError").equals(sessionid)) {
                    return sessionid;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
