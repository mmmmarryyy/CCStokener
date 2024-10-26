package httpclient;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class HttpClientDemo {

    public static void main(String[] args) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://www.tianya.cn/publicforum/content/news/1/91217.shtml");
        HttpResponse response = httpclient.execute(httpget);
        System.out.println(response.getStatusLine());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            instream.close();
        }
    }
}
