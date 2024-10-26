package com.sin.server.smsimagesjson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;

public class SmsImagesJSONArrProviderImpl implements SmsImagesJSONArrProvider {

    private static final Logger log = Logger.getLogger(SmsImagesJSONArrProviderImpl.class.getName());

    private StringBuffer response;

    private JSONArray jSONArray;

    @Override
    public JSONArray getRemoteJsonArr(URL url) throws IOException, JSONException {
        URLConnection conn = url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
        response = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        jSONArray = new JSONArray(response.toString());
        return jSONArray;
    }
}
