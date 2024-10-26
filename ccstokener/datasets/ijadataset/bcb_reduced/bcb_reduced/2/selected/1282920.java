package vbullmin;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import vbullmin.gui.GUI;

/**
 * Loginin into vBulletin and setting cookies to Get.cookies
 * @author Onur Aslan
 */
public class Login {

    public Login(String username, String password) {
        try {
            GUI.progress.setMessage("Logining with " + username + " and " + password + "...");
            String data = URLEncoder.encode("vb_login_username", "UTF-8") + "=" + URLEncoder.encode(username, Get.charset);
            data += "&" + URLEncoder.encode("vb_login_password", "UTF-8") + "=" + URLEncoder.encode(password, Get.charset);
            data += "&" + URLEncoder.encode("do", "UTF-8") + "=" + URLEncoder.encode("login", Get.charset);
            URL url = new URL(GUI.urls.url + "/login.php?do=login");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            String cookies = "";
            for (int i = 0; ; i++) {
                String headerName = conn.getHeaderFieldKey(i);
                String headerValue = conn.getHeaderField(i);
                if (headerName == null && headerValue == null) {
                    break;
                }
                if ("Set-Cookie".equalsIgnoreCase(headerName)) {
                    String[] raw = headerValue.split(";");
                    cookies += raw[0] + ";";
                }
            }
            Get.cookies = cookies;
            GUI.progress.setValue(1);
        } catch (MalformedURLException e) {
            new Exception(e, 97);
        } catch (IOException e) {
            new Exception(e, 97);
        }
    }
}
