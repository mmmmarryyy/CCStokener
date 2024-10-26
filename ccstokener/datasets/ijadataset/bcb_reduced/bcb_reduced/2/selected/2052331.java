package ru.javawebcrowler.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import ru.javawebcrowler.spider.statistics.HttpResult;

/**
 * Date: 27.04.2009
 *
 * @author Admin
 */
public class HtmlHelper {

    public static void copy(InputStream in, OutputStream out) throws IOException {
        int read;
        byte[] buf = new byte[1000];
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
    }

    public static HttpResult getContent(URL url, HttpClient httpclient) throws IOException {
        HttpGet httpget = new HttpGet(url.toExternalForm());
        HttpResponse response = httpclient.execute(httpget);
        return new HttpResult(response.getEntity().getContent());
    }

    public static void justVisitPage(URL url, HttpClient httpclient) throws IOException {
        HttpGet httpget = new HttpGet(url.toExternalForm());
        HttpResponse response = httpclient.execute(httpget);
        response.getEntity().consumeContent();
    }
}
