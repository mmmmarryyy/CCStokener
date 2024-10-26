package com.tad.integs.pownceapi.http;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class HttpUtils {

    public static String addParam(String url, String key, Object value) {
        if (url.indexOf("?") > 0) {
            url += "&";
        } else {
            url += "?";
        }
        return url += key + "=" + value.toString();
    }

    public static String addAppKey(String url, String appKey) {
        return url + "?app_key=" + appKey;
    }

    public static DefaultHttpClient init(String username, String password) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return httpclient;
    }

    public static Document getResponse(HttpClient client, HttpRequestBase request) {
        try {
            HttpResponse response = client.execute(request);
            StatusLine statusLine = response.getStatusLine();
            System.err.println(statusLine.getStatusCode() + " data: " + statusLine.getReasonPhrase());
            System.err.println("executing request " + request.getURI());
            HttpEntity entity = response.getEntity();
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(entity.getContent());
            return doc;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }
}
