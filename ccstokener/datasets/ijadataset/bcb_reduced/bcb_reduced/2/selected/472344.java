package org.iwidget.desktop.core.javascript;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.mozilla.javascript.ScriptableObject;

public class IwidgetURL extends ScriptableObject {

    public String getClassName() {
        return "URL";
    }

    public IwidgetURL() {
    }

    public void jsConstructor() {
    }

    public String jsFunction_fetch(String urlString) {
        try {
            StringBuffer sb;
            sb = new StringBuffer();
            BufferedReader br = postServerCommand(urlString);
            if (br == null) return "";
            for (String line = br.readLine(); line != null; line = br.readLine()) sb.append(line).append("\n");
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public BufferedReader postServerCommand(String serverURL) {
        try {
            BufferedReader dI;
            URL url = new URL(serverURL);
            URLConnection connection = url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            dI = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return dI;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final long serialVersionUID = 0x2d38363131383834L;
}
