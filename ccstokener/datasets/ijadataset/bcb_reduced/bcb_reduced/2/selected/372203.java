package org.crawlware.action;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.crawlware.context.JobContext;

/**
 *
 * @author Zhi Le Zou (zouzhile@gmail.com)
 */
public class HTTPGetAction extends AbstractAction {

    private String scheme;

    private String url;

    private String referer;

    private HashMap<String, String> params = new HashMap<String, String>();

    public void executeAction(JobContext context) throws Exception {
        HttpClient httpClient = (HttpClient) context.resolve("httpClient");
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        Iterator<String> keySet = params.keySet().iterator();
        while (keySet.hasNext()) {
            String key = keySet.next();
            String value = params.get(key);
            qparams.add(new BasicNameValuePair(key, value));
        }
        String paramString = URLEncodedUtils.format(qparams, "UTF-8");
        if (this.url.endsWith("/")) {
            this.url = this.url.substring(0, this.url.length() - 1);
        }
        String url = this.url + paramString;
        URI uri = URI.create(url);
        HttpGet httpget = new HttpGet(uri);
        if (!(this.referer == null || this.referer.equals(""))) httpget.setHeader(this.referer, url);
        HttpResponse response = httpClient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String content = "";
        if (entity != null) {
            content = EntityUtils.toString(entity, "UTF-8");
        }
    }
}
