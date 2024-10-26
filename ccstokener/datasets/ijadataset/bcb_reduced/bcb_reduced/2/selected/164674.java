package wayic.search;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoogleQuery {

    private static Logger LOGGER = Logger.getLogger(GoogleQuery.class);

    private final String HTTP_REFERER = "http://www.wayic.com/";

    public GoogleQuery() {
    }

    public Result search(Object object) {
        if (object == null || !(object instanceof String)) {
            return null;
        }
        String query = (String) object;
        Result hitResult = new Result();
        Set<Hit> hits = new HashSet<Hit>(8);
        try {
            query = URLEncoder.encode(query, "UTF-8");
            URL url = new URL("http://ajax.googleapis.com/ajax/services/search/web?start=0&rsz=large&v=1.0&q=" + query);
            URLConnection connection = url.openConnection();
            connection.addRequestProperty("Referer", HTTP_REFERER);
            String line;
            StringBuilder builder = new StringBuilder();
            InputStream input = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            input.close();
            String response = builder.toString();
            JSONObject json = new JSONObject(response);
            LOGGER.debug(json.getString("responseData"));
            int count = json.getJSONObject("responseData").getJSONObject("cursor").getInt("estimatedResultCount");
            hitResult.setEstimatedCount(count);
            JSONArray ja = json.getJSONObject("responseData").getJSONArray("results");
            for (int i = 0; i < ja.length(); i++) {
                JSONObject j = ja.getJSONObject(i);
                Hit hit = new Hit();
                String result = j.getString("titleNoFormatting");
                hit.setTitle(result == null || result.equals("") ? "${EMPTY}" : result);
                result = j.getString("url");
                hit.setUrl(new URL(result));
                hits.add(hit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Something went wrong..." + e.getMessage());
        }
        hitResult.setHits(hits);
        return hitResult;
    }
}
