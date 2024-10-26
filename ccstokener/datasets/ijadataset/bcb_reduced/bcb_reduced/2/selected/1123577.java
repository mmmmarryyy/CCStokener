package hubsniffer.db;

import libjdc.dc.log.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author root
 */
public class PublicHubListGen {

    public static void updateHublist() {
        try {
            URL url = new URL("http://localhost/genhublistxml.php");
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String str;
            while ((str = rd.readLine()) != null) {
                Log.out.println(str);
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace(Log.err);
        }
    }
}
