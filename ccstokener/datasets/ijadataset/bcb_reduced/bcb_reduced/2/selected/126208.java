package openPayments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import javax.net.ssl.SSLPeerUnverifiedException;
import openPayments.Logging.DefaultExceptionHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ServiceHelpers {

    public static String AddQueryString(String name, String value, boolean isLastParameter) {
        String str = "";
        if (name != null && value != null) {
            str = name.trim() + "=" + URLEncoder.encode(value.trim());
            if (!isLastParameter) str += "&";
        }
        return str;
    }

    public static void NavigateTo(Context ctx, String pathSegment) {
        String url = Variables.BASE_URI + pathSegment;
        NavigateToFullUrl(ctx, url);
    }

    public static void NavigateToFullUrl(Context ctx, String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        launchBrowser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(launchBrowser);
    }

    public static String GetString(JSONArray response) {
        try {
            if (response == null) return "";
            if (response.length() == 0) return "";
            if (response.length() == 1) return response.toString();
            return response.get(0).toString();
        } catch (JSONException e) {
            DefaultExceptionHandler.logException(Thread.currentThread(), e);
        }
        return "";
    }

    public static JSONArray Get(String pathSegment) throws IOException, ClientProtocolException, UnsupportedEncodingException, JSONException {
        return Get(pathSegment, 6000, 12000);
    }

    public static JSONArray Get(String pathSegment, int timeoutConnection, int timeoutSocket) throws ClientProtocolException, IOException, JSONException {
        try {
            URI url = URI.create(Variables.BASE_URI + pathSegment);
            HttpGet get = new HttpGet(url);
            DefaultHttpClient client = new DefaultHttpClient();
            if (timeoutConnection > 0 && timeoutSocket > 0) {
                HttpParams httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
                client.setParams(httpParameters);
            }
            HttpResponse response = client.execute(get);
            return parseResponse(response);
        } catch (ConnectTimeoutException e) {
            return null;
        } catch (UnknownHostException e1) {
            return null;
        } catch (HttpHostConnectException e1) {
            return null;
        } catch (SocketException e1) {
            return null;
        } catch (SocketTimeoutException e1) {
            return null;
        } catch (SSLPeerUnverifiedException e1) {
            return null;
        } catch (ClientProtocolException e2) {
            return null;
        } catch (IOException e3) {
            throw e3;
        } catch (JSONException e4) {
            throw e4;
        }
    }

    public static JSONArray Post(String pathSegment, JSONObject params) throws IOException, ClientProtocolException, UnsupportedEncodingException, JSONException {
        URI url = URI.create(Variables.BASE_URI + pathSegment);
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(params.toString(), HTTP.UTF_8);
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        HttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(httpPost);
        return parseResponse(response);
    }

    private static JSONArray parseResponse(HttpResponse response) throws UnsupportedEncodingException, IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        for (String line = null; (line = reader.readLine()) != null; ) {
            builder.append(line).append("\n");
        }
        JSONArray finalResult = null;
        String data = builder.toString();
        if (data.startsWith("[")) {
            JSONTokener tokener = new JSONTokener(builder.toString());
            finalResult = new JSONArray(tokener);
        } else {
            finalResult = new JSONArray();
            finalResult.put(builder.toString());
        }
        return finalResult;
    }
}
