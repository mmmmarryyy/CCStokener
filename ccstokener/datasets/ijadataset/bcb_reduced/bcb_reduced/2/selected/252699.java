package com.ever365.search;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.json.JSONObject;
import com.mongodb.BasicDBObject;

public class SolrSearchService {

    private HttpClient httpClient;

    public SolrSearchService() {
        super();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(schemeRegistry);
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);
        HttpHost localhost = new HttpHost("locahost", 80);
        cm.setMaxForRoute(new HttpRoute(localhost), 100);
        httpClient = new DefaultHttpClient(cm);
    }

    /**
	 * @param docMap
	 */
    public void updateDocument(Map<String, Object> docMap) {
        HttpPost post = new HttpPost("http://localhost/solr/update/json?commit=true");
        post.setHeader("Content-type", "application/json");
        try {
            post.setEntity(new StringEntity("[" + new JSONObject(docMap).toString() + "]", "utf-8"));
            HttpResponse result = httpClient.execute(post);
            int code = result.getStatusLine().getStatusCode();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    public static void main(String[] args) {
        SolrSearchService sss = new SolrSearchService();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", "小王八");
        map.put("title", "小哦八");
        map.put("series_t", "2233");
        map.put("inStock", true);
        map.put("vv", true);
        map.put("id", "12127");
        sss.updateDocument(map);
    }
}
