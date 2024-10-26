package CB_Core.Api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Diese Klasse stellt eine verbindung zu Team-Cachebox.de her und gibt dort hinterlegte Informationen
 * zur�k. (GCAuth url ; Versionsnummer)
 * @author Longri
 *
 */
public class CB_Api {

    private static final String CB_API_URL_GET_URLS = "http://team-cachebox.de/CB_API/index.php?get=url_ACB";

    /**
	 * Gibt die bei Team-Cachebox.de hinterlegte GC Auth url zur�ck
	 * @return String
	 */
    public static String getGcAuthUrl() {
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(CB_API_URL_GET_URLS);
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String line = "";
            while ((line = rd.readLine()) != null) {
                result += line + "\n";
            }
            try {
                JSONTokener tokener = new JSONTokener(result);
                JSONObject json = (JSONObject) tokener.nextValue();
                return json.getString("GcAuth_ACB");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return "";
        }
        return "";
    }
}
