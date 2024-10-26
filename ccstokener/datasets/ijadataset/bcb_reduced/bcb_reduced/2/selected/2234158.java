package net.anydigit.jiliu.http;

import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 *
 */
public class TestHttpClient {

    /**
	 * @param args
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
    public static void main(String[] args) throws ClientProtocolException, IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet("http://s.goso.cn/");
        httpget.addHeader("X-Forwarded-For", "211.100.48.140");
        HttpResponse response = httpclient.execute(httpget);
        StatusLine sl = response.getStatusLine();
        System.out.println(sl.getProtocolVersion().getProtocol() + " " + sl.getStatusCode() + " " + sl.getReasonPhrase());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();
            int l;
            byte[] tmp = new byte[2048];
            while ((l = instream.read(tmp)) != -1) {
                System.out.print(new String(tmp, 0, l));
            }
            instream.close();
        }
    }
}
