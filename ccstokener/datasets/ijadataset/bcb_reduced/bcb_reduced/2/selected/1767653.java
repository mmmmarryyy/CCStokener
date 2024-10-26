package wei.liu.my.health.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

/**
 * 通过HTTP请求与服务器交互
 * Create by 2012-5-7
 * @author Liuw
 * @copyright Copyright(c) 2012-2014  Liuw
 */
public class MyHttpRequest {

    private HttpClient client;

    private static final MyHttpRequest hr = new MyHttpRequest();

    private MyHttpRequest() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 6000);
        HttpConnectionParams.setSoTimeout(params, 30000);
        client = new DefaultHttpClient(params);
    }

    public static MyHttpRequest getInstance() {
        return hr;
    }

    /**
	 * GET 请求
	 * Create by 2012-5-7
	 * @author Liuw
	 * @param urlKey
	 * @param params
	 * @return
	 */
    public String requestGET(String baseUrl, Map<String, String> params) throws Exception {
        String result = "";
        StringBuffer url = new StringBuffer();
        url.append(baseUrl);
        if (params != null && !params.isEmpty()) {
            List<String> keys = new ArrayList<String>(params.keySet());
            for (String key : keys) {
                url.append(key);
                url.append("/");
                url.append(URLEncoder.encode(params.get(key), "UTF-8"));
            }
        }
        HttpGet get = new HttpGet(url.toString());
        HttpResponse res = client.execute(get);
        HttpEntity entity = null;
        if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            entity = res.getEntity();
            BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
            String read = "";
            StringBuffer content = new StringBuffer();
            while ((read = in.readLine()) != null) {
                content.append(read);
            }
            in.close();
            JSONObject rObj = new JSONObject(content.toString());
            result = rObj.getString("msg");
        } else {
            result = "HTTP请求失败";
        }
        if (entity != null) entity.consumeContent();
        client.getConnectionManager().shutdown();
        get = null;
        return result;
    }

    /**
	 * POST请求 
	 * Create by 2012-5-7
	 * @author Liuw
	 * @param baseUrl
	 * @param params
	 * @return
	 * @throws Exception
	 */
    public String requestPOST(String baseUrl, Map<String, String> params) throws Exception {
        String result = "";
        HttpPost request = new HttpPost(baseUrl);
        HttpEntity entity = null;
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
            List<String> keys = new ArrayList<String>(params.keySet());
            for (String key : keys) {
                postParams.add(new BasicNameValuePair(key, params.get(key)));
            }
            entity = new UrlEncodedFormEntity(postParams, "utf-8");
            request.setEntity(entity);
        }
        HttpResponse res = client.execute(request);
        if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            entity = res.getEntity();
            BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
            StringBuffer content = new StringBuffer();
            String read = "";
            while ((read = in.readLine()) != null) {
                content.append(read);
            }
            in.close();
            JSONObject rObj = new JSONObject(content.toString());
            result = rObj.getString("msg");
        } else result = "请求失败了";
        entity.consumeContent();
        client.getConnectionManager().shutdown();
        request = null;
        return result;
    }
}
