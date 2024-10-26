package sipinspector.versionUpdater;

import sipinspector.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author Zarko Coklin
 */
public class CheckUpdate {

    /**
     * Sends an HTTP GET request to a url, and check the version
     *
     * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
     * @param requestParameters - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
     * @return - The response from the end point
     */
    public static boolean newerVersionExists() {
        float newVersion = 0;
        float oldVersion = Float.parseFloat(SIPInspectorMainFrame.getSIPInspectorVersion());
        boolean flag = false;
        int pos;
        newMessage = null;
        try {
            String urlStr = "http://sites.google.com/site/sipinspectorsite/current_version";
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if ((pos = line.indexOf("[SI_version]")) != -1) {
                    newVersionStr = line.substring(pos + 12, line.indexOf("[/SI_version]"));
                    newVersion = Float.parseFloat(newVersionStr);
                    if (newVersion > oldVersion) {
                        flag = true;
                    }
                } else if ((pos = line.indexOf("[SI_URL]")) != -1) {
                    newVersionURL = line.substring(pos + 8, line.indexOf("[/SI_URL]"));
                } else if ((pos = line.indexOf("[SI_MESSAGE]")) != -1) {
                    newMessage = line.substring(pos + 12, line.indexOf("[/SI_MESSAGE]"));
                    if (newMessage.equals("") == true) {
                        newMessage = null;
                    }
                    rd.close();
                    break;
                }
            }
            if (flag == false) {
                rd.close();
            }
        } catch (Exception e) {
            return flag;
        }
        return flag;
    }

    public static String getNewVersionURL() {
        return newVersionURL;
    }

    public static String getNewMessage() {
        return newMessage;
    }

    public static String newVersionStr;

    public static String newVersionURL;

    public static String newMessage;
}
