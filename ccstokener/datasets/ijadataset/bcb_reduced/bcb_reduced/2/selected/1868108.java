package octopus.manager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

public class MySQLConnect {

    public String selectFROM() throws Exception {
        BufferedReader in = null;
        String data = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            URI uri = new URI("http://**.**.**.**/OctopusManager/index2.php");
            HttpGet request = new HttpGet();
            request.setURI(uri);
            HttpResponse httpresponse = httpclient.execute(request);
            HttpEntity httpentity = httpresponse.getEntity();
            in = new BufferedReader(new InputStreamReader(httpentity.getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
            data = sb.toString();
            return data;
        } finally {
            if (in != null) {
                try {
                    in.close();
                    return data;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void insertINTO(String etykieta1, String login, String etykieta2, String password) {
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(etykieta1, login));
        nameValuePairs.add(new BasicNameValuePair(etykieta2, password));
        InputStream is = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://**.*.*.*/OctopusManager/test.php");
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
