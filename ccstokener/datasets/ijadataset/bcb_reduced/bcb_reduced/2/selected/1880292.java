package org.mainlove.project.web.demo.baiduOpenId;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.mainlove.project.web.demo.baiduOpenId.config.URIConfig;

public class SimpleHttpClient {

    private static HttpClient httpClient;

    public static URI getURI(URIConfig uriConfig) throws Exception {
        URI uri = URIUtils.createURI(uriConfig.getScheme(), uriConfig.getHost(), -1, "", URLEncodedUtils.format(uriConfig.getQparams(), "UTF-8"), null);
        return uri;
    }

    public static String getGetResult(URI uri) throws Exception {
        HttpGet httpget = new HttpGet(uri);
        HttpClient httpclient = getSimpleHttpClient();
        HttpResponse httpResponse = httpclient.execute(httpget);
        return getResponseContent(httpResponse);
    }

    private static HttpClient getSimpleHttpClient() {
        if (httpClient == null) {
            httpClient = new ContentEncodingHttpClient();
            HttpHost proxy = new HttpHost("10.1.2.188", 80);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        return httpClient;
    }

    private static String getResponseContent(HttpResponse httpResponse) throws Exception {
        InputStream input = null;
        input = httpResponse.getEntity().getContent();
        InputStreamReader reader = new InputStreamReader(input, "UTF-8");
        BufferedReader bufReader = new BufferedReader(reader);
        String tmp = null, html = "";
        while ((tmp = bufReader.readLine()) != null) {
            html += tmp;
        }
        if (input != null) {
            input.close();
        }
        return html;
    }
}
