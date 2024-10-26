package ru.elifantiev.cityrouter.data;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class JSONHadler {

    private static String jString = "{\n" + "  \"routes\": [\n" + "    {\n" + "      \"items\": [\n" + "           {\n" + "                \"start\": {\n" + "                  \"id\": \"100\", \n" + "                  \"name\": \"6-� ����������\"\n" + "                }, \n" + "                \"end\": {\n" + "                  \"id\": \"101\", \n" + "                  \"name\": \"����� ������\"\n" + "                }, \n" + "                \"route_ids\": [\n" + "                  \"bus80\", \n" + "                  \"bus101\", \n" + "                  \"bus110\", \n" + "                  \"bus180\", \n" + "                  \"bus251\", \n" + "                  \"trolley71\", \n" + "                  \"trolley80\"\n" + "                ]\n" + "          }\n" + "      ]\n" + "    }\n" + "  ]\n" + "}";

    public static JSONObject getJSONData(String url) throws JSONException {
        JSONObject jObject = null;
        InputStream data = null;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        URI uri;
        try {
            uri = new URI(url);
            HttpGet httpGet = new HttpGet(uri);
            HttpResponse response = httpClient.execute(httpGet);
            data = response.getEntity().getContent();
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader;
            reader = new BufferedReader(new InputStreamReader(data), 8192);
            while ((line = reader.readLine()) != null) builder.append(line);
            reader.close();
            jObject = (JSONObject) new JSONTokener(builder.toString()).nextValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jObject;
    }
}
