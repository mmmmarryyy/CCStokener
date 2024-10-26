package connect_tx_sdk.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

/**
 * 
 * @创建作者：hiyoucai@126.com
 * @创建时间：2009-9-10 上午09:10:11
 * @文件描述：破解银行模拟浏览器类
 * @文件名称：connect.skeleton.utils.HttpClientUtils.java
 */
public class HttpClientUtils {

    static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }

        public InputStream getContent() throws IOException, IllegalStateException {
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }

        public long getContentLength() {
            return -1;
        }
    }

    public static DefaultHttpClient getHttpClient() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setUserAgent(params, "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)");
        HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);
        HttpConnectionParams.setConnectionTimeout(params, 30 * 1000);
        HttpConnectionParams.setSoTimeout(params, 30 * 1000);
        ConnManagerParams.setTimeout(params, 30 * 1000);
        DefaultHttpClient httpclient = new DefaultHttpClient(params);
        httpclient.addRequestInterceptor(new HttpRequestInterceptor() {

            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
            }
        });
        httpclient.addResponseInterceptor(new HttpResponseInterceptor() {

            public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
                HttpEntity entity = response.getEntity();
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        });
        return httpclient;
    }

    public static String getHtml(HttpResponse res, String encode, Boolean breakLine) throws Exception {
        breakLine = (breakLine == null) ? false : breakLine;
        InputStream input = null;
        try {
            StatusLine status = res.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new RuntimeException("50001");
            }
            if (res.getEntity() == null) {
                return "";
            }
            input = res.getEntity().getContent();
            InputStreamReader reader = new InputStreamReader(input, encode);
            BufferedReader bufReader = new BufferedReader(reader);
            String tmp = null, html = "";
            while ((tmp = bufReader.readLine()) != null) {
                html += tmp + (breakLine ? "\r\n" : "");
            }
            return html;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("50002");
        } finally {
            if (input != null) input.close();
        }
    }

    public static String getHtml(DefaultHttpClient httpclient, String url, String encode) throws Exception {
        InputStream input = null;
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse res = httpclient.execute(get);
            StatusLine status = res.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new RuntimeException("50001");
            }
            if (res.getEntity() == null) {
                return "";
            }
            input = res.getEntity().getContent();
            InputStreamReader reader = new InputStreamReader(input, encode);
            BufferedReader bufReader = new BufferedReader(reader);
            String tmp = null, html = "";
            while ((tmp = bufReader.readLine()) != null) {
                html += tmp;
            }
            return html;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("50002");
        } finally {
            if (input != null) input.close();
        }
    }
}
