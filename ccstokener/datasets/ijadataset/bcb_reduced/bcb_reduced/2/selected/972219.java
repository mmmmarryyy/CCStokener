package beans;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class ResourceBean {

    public String getScript(String script, String params) {
        params = params.replaceFirst("&", "?");
        StringBuffer document = new StringBuffer();
        try {
            URL url = new URL(script + params);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                document.append(line + "\n");
            }
            reader.close();
        } catch (Exception e) {
            return e.toString();
        }
        return document.toString();
    }
}
