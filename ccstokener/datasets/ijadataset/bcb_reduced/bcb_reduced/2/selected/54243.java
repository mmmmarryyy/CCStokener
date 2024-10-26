package ao.dd.web.apache;

import ao.dd.common.WebUtils;
import ao.dd.web.RequestError;
import ao.util.misc.Factory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ApacheSession implements Session {

    private static final Logger LOG = Logger.getLogger(ApacheSession.class);

    private static final int READ_TIMEOUT = 2 * 60 * 1000;

    private final Factory<HttpClient> CLIENT;

    public ApacheSession(Factory<HttpClient> client) {
        CLIENT = client;
    }

    public String load(URL address) {
        return load(address, null, null, false);
    }

    public String load(URL address, Map<String, String> data, boolean isPost) {
        return load(address, null, data, isPost);
    }

    public String load(URL address, URL referrer, Map<String, String> data, boolean isPost) {
        try {
            return isPost ? loadPost(address, referrer, data) : loadGet(address, referrer, data);
        } catch (Error e) {
            throw e;
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private String loadGet(URL address, URL referer, Map<String, String> data) throws Throwable {
        HttpGet method = new HttpGet(addEncodedData(address, data).toURI());
        setReferrer(method, referer);
        return execute(method);
    }

    private URL addEncodedData(URL url, Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return url;
        }
        String addend = ((url.toString().indexOf("?") != -1) ? "&" : "?") + WebUtils.urlEncode(data);
        return WebUtils.asUrl(url.toString() + addend);
    }

    private String loadPost(URL address, URL referrer, Map<String, String> data) throws Throwable {
        HttpPost method = new HttpPost(address.toURI());
        setReferrer(method, referrer);
        addPostData(method, data);
        return execute(method);
    }

    private void addPostData(HttpPost method, Map<String, String> data) throws UnsupportedEncodingException {
        if (data != null) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> datum : data.entrySet()) {
                nvps.add(new BasicNameValuePair(datum.getKey(), datum.getValue()));
            }
            method.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        }
    }

    private void setReferrer(HttpUriRequest method, URL referrer) {
        if (referrer != null) {
            method.addHeader(new BasicHeader("Referer", referrer.toString()));
        }
    }

    private String execute(final AbortableHttpRequest method) throws Throwable {
        final Throwable error[] = { null };
        final String payload[] = { null };
        final CountDownLatch finished = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    payload[0] = doExecute(method);
                } catch (Throwable t) {
                    error[0] = t;
                } finally {
                    finished.countDown();
                }
            }
        });
        t.start();
        if (!finished.await(READ_TIMEOUT, TimeUnit.MILLISECONDS)) {
            method.abort();
            throw new RequestError("Request took longer than " + READ_TIMEOUT);
        }
        if (error[0] != null) throw error[0];
        return payload[0];
    }

    private String doExecute(AbortableHttpRequest method) throws Throwable {
        HttpClient client = CLIENT.newInstance();
        HttpResponse rsp = client.execute((HttpUriRequest) method);
        HttpEntity entity = rsp.getEntity();
        if (entity == null) throw new RequestError("No entity in method");
        InputStream in = null;
        try {
            in = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder inStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                inStr.append(line).append("\r\n");
            }
            entity.consumeContent();
            return inStr.toString();
        } catch (IOException ex) {
            LOG.error("IO exception: " + ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            method.abort();
            throw ex;
        } finally {
            if (in != null) in.close();
        }
    }
}
