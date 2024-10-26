package hu.sztaki.lpds.portal.util.stream;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * @author krisztian
 */
public class HttpClient {

    private HttpPost httpPost;

    private DefaultHttpClient httpclient;

    private CookieStore cookieStore;

    private HttpContext localContext;

    /**
 * Opening the connection
 * @param pURL resource URL
 */
    public void open(String pURL) {
        httpclient = new DefaultHttpClient();
        cookieStore = new BasicCookieStore();
        localContext = new BasicHttpContext();
        httpPost = new HttpPost(pURL);
        localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
 * Getting the stream
 * @param pValue list of the parameters used during the connection
 * @return  datastream
 * @throws java.io.IOException communication error
 */
    public InputStream getStream(Hashtable<String, String> pValue) throws IOException {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Enumeration<String> enm = pValue.keys();
        String key;
        while (enm.hasMoreElements()) {
            key = enm.nextElement();
            nvps.add(new BasicNameValuePair(key, pValue.get(key)));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    /**
 * Closing the connection
 */
    public void close() {
        httpclient.getConnectionManager().shutdown();
    }

    /**
 * File upload
 * @param pFile file to be uploaded
 * @param uploadName upload naem
 * @param pValue parameters used during the connection
 * @return http answer code
 * @throws java.lang.Exception communication error
 */
    public int fileUpload(File pFile, String uploadName, Hashtable pValue) throws Exception {
        int res = 0;
        MultipartEntity reqEntity = new MultipartEntity();
        if (uploadName != null) {
            FileBody bin = new FileBody(pFile);
            reqEntity.addPart(uploadName, bin);
        }
        Enumeration<String> enm = pValue.keys();
        String key;
        while (enm.hasMoreElements()) {
            key = enm.nextElement();
            reqEntity.addPart(key, new StringBody("" + pValue.get(key)));
        }
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpclient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();
        res = response.getStatusLine().getStatusCode();
        close();
        return res;
    }
}
