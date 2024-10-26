package com.definity.toolkit.web.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;

public final class HttpUtils {

    private HttpUtils() {
    }

    public static byte[] get(String site) throws IOException {
        URL url = new URL(site);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        return IOUtils.toByteArray(urlConnection.getInputStream());
    }

    public static byte[] get(String site, String contextPath, String login, String password) throws IOException {
        HttpClient client = new HttpClient();
        String body = "";
        GetMethod get = new GetMethod(site);
        client.executeMethod(get);
        get.releaseConnection();
        PostMethod post = new PostMethod(contextPath + "/j_security_check");
        post.addParameter("j_username", login);
        post.addParameter("j_password", password);
        client.executeMethod(post);
        post.releaseConnection();
        get = new GetMethod(site);
        client.executeMethod(get);
        body = get.getResponseBodyAsString();
        get.releaseConnection();
        return body.getBytes();
    }
}
