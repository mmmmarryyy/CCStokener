package fit5030.assignment1.WS;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import JSON.JSONObject;

/**
 * Created by IntelliJ IDEA.
 * User: Liwen
 * Date: 1/04/11
 * Time: 7:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class GoogleWebSearch {

    public JSONObject getJSON() {
        try {
            URL url = new URL("https://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=8&" + "q=Paris%20Hilton&key=ABQIAAAAJEAQphkWpsdOT83DFvAtyRTwM0brOpm-All5BF6PoaKBxRWWERSbOAXHz0vNQ4WhXfBh1hJPzUb7CA");
            URLConnection connection = url.openConnection();
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject json = new JSONObject(builder.toString());
            return json;
        } catch (Exception e) {
            return null;
        }
    }
}
