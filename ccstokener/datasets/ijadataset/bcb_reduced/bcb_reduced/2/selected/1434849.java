package com.simoncat.net;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.IOException;

/**
 * A simple example that uses HttpClient to execute an HTTP request against
 * a target site that requires user authentication. 
 */
public class HttpClient {

    private String host;

    private int port;

    private String user;

    private String pwd;

    private String url;

    private String statusLine;

    private String answer;

    private InputStream content;

    InputStreamReader isr;

    LineNumberReader lnr;

    public HttpClient(String host, int port, String user, String pwd, String url) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
        this.url = url;
    }

    public boolean execute() {
        boolean r = false;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(user, pwd));
            HttpGet httpget = new HttpGet(url);
            System.out.println("executing request:" + httpget.getRequestLine());
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            statusLine = response.getStatusLine().toString();
            System.out.println("statusLine:" + statusLine);
            if (entity != null) {
                content = entity.getContent();
                isr = new InputStreamReader(content);
                lnr = new LineNumberReader(isr);
                answer = lnr.readLine();
                if (answer.startsWith("OK") && statusLine.trim().endsWith("OK")) {
                    r = true;
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
        return r;
    }

    public String appState(String appName) {
        url = "http://" + host + ":" + port + "/manager/list";
        boolean r = execute();
        System.out.println("r:" + r);
        String a = "";
        if (r) {
            System.out.println("r1");
            try {
                String ln = lnr.readLine();
                while (ln != null) {
                    if (ln.indexOf(appName + ":") >= 0) a = ln;
                    ln = lnr.readLine();
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }
        return a;
    }

    public boolean appIsRunning(String appName) {
        boolean a = false;
        String st = appState(appName);
        if (st.length() > 0 && st.indexOf(appName = ":running") >= 0) a = true;
        return a;
    }

    public static void main(String[] args) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getCredentialsProvider().setCredentials(new AuthScope("localhost", 8080), new UsernamePasswordCredentials("tomcat", "admin"));
        HttpGet httpget = new HttpGet("http://localhost:8080/manager/undeploy?path=/mvx");
        System.out.println("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
            entity.writeTo(System.out);
            System.out.println("Chunked?: " + entity.isChunked());
        }
        if (entity != null) {
            entity.consumeContent();
        }
        httpget = new HttpGet("http://localhost:8080/manager/list");
        System.out.println("executing request" + httpget.getRequestLine());
        response = httpclient.execute(httpget);
        entity = response.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
            entity.writeTo(System.out);
            System.out.println("Chunked?: " + entity.isChunked());
        }
        if (entity != null) {
            entity.consumeContent();
        }
        httpget = new HttpGet("http://localhost:8080/manager/deploy?path=/Binary&war=file:/tmp/Binary.war");
        System.out.println("executing request" + httpget.getRequestLine());
        response = httpclient.execute(httpget);
        entity = response.getEntity();
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (entity != null) {
            System.out.println("Response content length: " + entity.getContentLength());
            entity.writeTo(System.out);
            System.out.println("Chunked?: " + entity.isChunked());
        }
        if (entity != null) {
            entity.consumeContent();
        }
    }
}
