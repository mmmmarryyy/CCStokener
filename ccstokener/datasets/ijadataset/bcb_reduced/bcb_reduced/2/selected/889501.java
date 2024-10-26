package org.com.cnc.common.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class CommonHttp {

    public static String executeHttpGet() {
        String page = null;
        BufferedReader in = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        try {
            request.setURI(new URI("http://connectsoftware.com/mapi/login?format=xml&email=vuongvantruong1987%40gmail.com&password=1&submit=Submit+Query"));
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            page = sb.toString();
        } catch (Exception e) {
            page = null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return page;
    }

    public static String executeHttpPost() {
        BufferedReader in = null;
        try {
            HttpClient client = new DefaultHttpClient();
            HttpParams params = client.getParams();
            HttpConnectionParams.setConnectionTimeout(params, 1000);
            HttpPost request = new HttpPost("http://connectsoftware.com/mapi/publication/post");
            List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("format", "json"));
            postParameters.add(new BasicNameValuePair("token", "efc5c336fbc33f21ed5ea5ddb3bcb511"));
            postParameters.add(new BasicNameValuePair("limit", ""));
            postParameters.add(new BasicNameValuePair("start", ""));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
            request.setEntity(formEntity);
            HttpResponse response = client.execute(request);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String result = sb.toString();
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
