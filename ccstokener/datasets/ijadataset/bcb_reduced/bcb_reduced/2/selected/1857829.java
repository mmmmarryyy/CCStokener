package virtualpostit.com;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import com.google.gson.Gson;

/**
 * TODO
 * 
 */
public class WebService {

    private DefaultHttpClient httpClient;

    private HttpGet httpGet = null;

    private HttpPost httpPost = null;

    private HttpResponse response = null;

    private String webServiceUrl;

    /**
	 * TODO
	 * 
	 * @param o
	 * @return
	 */
    public static JSONObject Object(Object o) {
        try {
            return new JSONObject(new Gson().toJson(o));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * TODO
	 * 
	 * @param serviceName
	 */
    public WebService(String serviceName) {
        HttpParams myParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(myParams, 10000);
        HttpConnectionParams.setSoTimeout(myParams, 10000);
        httpClient = new DefaultHttpClient(myParams);
        webServiceUrl = serviceName;
    }

    /**
     * TODO
     */
    public void abort() {
        try {
            if (httpClient != null) {
                System.out.println("Abort.");
                httpPost.abort();
            }
        } catch (Exception e) {
            System.out.println("Virtual Post it " + e);
        }
    }

    /**
     * TODO
     */
    public void clearCookies() {
        httpClient.getCookieStore().clear();
    }

    /**
	 * TODO
	 * @param urlString
	 * @return
	 * @throws IOException
	 */
    public InputStream getHttpStream(String urlString) throws IOException {
        InputStream in = null;
        int response = -1;
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        if (!(conn instanceof HttpURLConnection)) throw new IOException("Not an HTTP connection");
        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception e) {
            throw new IOException("Error connecting");
        }
        return in;
    }

    /**
     * TODO
     */
    public String webGet(String methodName, ArrayList<NameValuePair> params) {
        String ret = null;
        String getUrl = webServiceUrl + methodName;
        int i = 0;
        for (NameValuePair param : params) {
            if (i == 0) getUrl += "?"; else getUrl += "&";
            try {
                getUrl += param.getName() + "=" + URLEncoder.encode(param.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            i++;
        }
        httpGet = new HttpGet(getUrl);
        Log.e("WebGetURL: ", getUrl);
        try {
            response = httpClient.execute(httpGet);
        } catch (Exception e) {
            Log.e("Eccezione nell'execute Groshie:", e.getMessage());
        }
        try {
            ret = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            Log.e("Groshie:", e.getMessage());
        }
        return ret;
    }

    /**
     * TODO
     */
    public String webPost(String methodName, ArrayList<NameValuePair> params) {
        InputStream is = null;
        String result = null;
        boolean error = false;
        try {
            httpPost = new HttpPost(webServiceUrl + methodName);
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            error = true;
            Log.e("log_tag", "Error in http connection " + e.toString());
        }
        if (!error) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) sb.append(line + "\n");
                is.close();
                result = sb.toString();
            } catch (Exception e) {
                error = true;
                Log.e("log_tag", "Error converting result " + e.toString());
            }
        }
        if (error) return "connection_error"; else return result;
    }
}
