package team2.billpayreminder.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import team2.billpayreminder.utilities.CommandConstants;
import android.util.Log;

public class DBMediator {

    private static DBMediator dbMediator = new DBMediator();

    private static HttpClient httpClient = new DefaultHttpClient();

    private static HttpPost httppost = new HttpPost(CommandConstants.phpScriptPath);

    private static HttpResponse httpResponse = null;

    private static String response = "";

    private DBMediator() {
    }

    public DBMediator getDBMediator() {
        return dbMediator;
    }

    private static HttpClient getHttpClient() {
        return httpClient;
    }

    private static HttpPost getHttpPost() {
        return httppost;
    }

    private static void setHttpResponse(HttpResponse response) {
        httpResponse = response;
    }

    private static HttpResponse getHttpResponse() {
        return httpResponse;
    }

    private static HttpEntity getHttpEntity(HttpResponse response) throws Exception {
        if (response != null) {
            return response.getEntity();
        } else {
            throw new Exception("Response is null to extract the entity");
        }
    }

    public static void postHttpCommand(ArrayList<NameValuePair> nameValues) throws Exception {
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            HttpPost httppostObj = DBMediator.getHttpPost();
            httppostObj.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));
            DBMediator.setHttpResponse(DBMediator.getHttpClient().execute(httppostObj));
            inputStream = DBMediator.getHttpEntity(DBMediator.getHttpResponse()).getContent();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                Log.i("Response ", line);
                sb.append(line + "\n");
            }
            response = sb.toString();
            Log.i("RESULT OF DB OPERATION IS ", response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        inputStream.close();
        inputStreamReader.close();
        bufferedReader.close();
    }

    public static JSONArray getResponseFromServerAsArray() throws Exception {
        JSONArray jArray = null;
        try {
            Log.i("Response is", response);
            jArray = new JSONArray(response);
        } catch (Exception e) {
            throw new Exception(e);
        }
        return jArray;
    }

    public static JSONObject getResponseFromServerAsObject() throws Exception {
        JSONObject jObject = null;
        try {
            jObject = new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return jObject;
    }
}
