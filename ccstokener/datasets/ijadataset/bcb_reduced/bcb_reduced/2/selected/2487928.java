package org.apache.http.examples.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * A simple example that uses HttpClient to execute an HTTP request 
 * over a secure connection tunneled through an authenticating proxy. 
 */
public class ClientProxyAuthentication {

    public static void main(String[] args) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(new AuthScope("localhost", 8080), new UsernamePasswordCredentials("username", "password"));
        HttpHost targetHost = new HttpHost("www.verisign.com", 443, "https");
        HttpHost proxy = new HttpHost("localhost", 8080);
        httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        HttpGet httpget = new HttpGet("/");
        System.out.println("executing request: " + httpget.getRequestLine());
        System.out.println("via proxy: " + proxy);
        System.out.println("to target: " + targetHost);
        HttpResponse response = httpclient.execute(targetHost, httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
        }
        if (entity != null) {
            entity.consumeContent();
        }
    }
}
