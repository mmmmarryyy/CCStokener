package sk.sigp.tetras.findemail.google;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import sk.sigp.tetras.service.PreferenceService;
import sk.sigp.tetras.util.URLUTF8Encoder;

/**
 * base code from http://www.ajaxlines.com 
 * @author mathew
 *
 */
public class GoogleSearchWrapper {

    private static Logger LOG = Logger.getLogger(GoogleSearchWrapper.class);

    private PreferenceService preferenceService;

    public List<String> makeQuery(String query) {
        List<String> result = new ArrayList<String>();
        try {
            query = URLUTF8Encoder.encode(query);
            URL url = new URL("http://ajax.googleapis.com/ajax/services/search/web?start=0&rsz=large&v=1.0&q=" + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", "http://poo.sk");
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String response = builder.toString();
            JSONObject json = new JSONObject(response);
            Long count = Long.decode(json.getJSONObject("responseData").getJSONObject("cursor").getString("estimatedResultCount"));
            LOG.info("Found " + count + " potential pages");
            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject j = ja.getJSONObject(i);
                result.add(j.getString("url"));
            }
        } catch (Exception e) {
            LOG.error("Couldnt query Google for some reason check exception below");
            e.printStackTrace();
        }
        return result;
    }

    public PreferenceService getPreferenceService() {
        return preferenceService;
    }

    public void setPreferenceService(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }
}
