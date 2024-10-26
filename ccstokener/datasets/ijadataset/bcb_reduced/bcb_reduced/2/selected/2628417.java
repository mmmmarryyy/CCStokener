package gpsmate.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * NetTool
 * 
 * @author longdistancewalker
 */
public class NetTool {

    public static boolean isInternetConnected() {
        try {
            URL url = new URL("http://tile.openstreetmap.org/");
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            @SuppressWarnings("unused") Object objData = urlConnect.getContent();
            return true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        }
        return false;
    }
}
