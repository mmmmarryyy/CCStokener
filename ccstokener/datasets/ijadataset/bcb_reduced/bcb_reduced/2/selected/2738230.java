package org.doit.android.bobple.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;

/**
 * @author Choi HongKi
 *
 */
public final class ServerRequester {

    private static final String SERVER_HOST = "10.67.31.201";

    private static final int SERVER_PORT = 38080;

    private ServerRequester() {
    }

    public static ServerResponseJson request(String request) {
        return request(request, null);
    }

    public static ServerResponseJson request(String request, Map<String, String> paramMap) {
        ServerResponseJson serverResponseJson = null;
        InputStream is = null;
        try {
            HttpClient httpClient = new DefaultHttpClient();
            Builder uriBuilder = Uri.parse(String.format("http://%s:%s/%s", SERVER_HOST, SERVER_PORT, request)).buildUpon();
            if (paramMap != null) {
                for (Entry<String, String> entry : paramMap.entrySet()) {
                    uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
                }
            }
            HttpResponse response = httpClient.execute(new HttpGet(uriBuilder.toString()));
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                is = entity.getContent();
                String jsonString = convertStreamToString(is);
                serverResponseJson = new ServerResponseJson(new JSONObject(jsonString));
            }
        } catch (Exception ex) {
            Log.e(ServerRequester.class.getName(), ex.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return serverResponseJson;
    }

    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
