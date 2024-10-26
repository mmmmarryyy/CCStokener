package com.makeabyte.jhosting.server.io.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class Downloader {

    private Log log = LogFactory.getLog(Downloader.class);

    private BasicAuthToken basicAuthToken;

    private String url;

    private long contentLength;

    public Downloader() {
    }

    /**
	    * Create a new instance of Downloader
	    * 
	    * @param url The fully qualified path to the remote resource (http://domain.com/plugin.jhp)
	    */
    public Downloader(String url) {
        this.url = url;
    }

    /**
	    * Create a new instance of Downloader
	    * 
	    * @param url The fully qualified path to the remote resource (http://domain.com/plugin.jhp)
	    * @param basicAuthToken
	    */
    public Downloader(String url, BasicAuthToken basicAuthToken) {
        this.url = url;
        this.basicAuthToken = basicAuthToken;
    }

    /**
	    * Sets the fully qualified url of the resource to download
	    * @param url
	    */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
	    * Returns the url of the resource
	    * 
	    * @return
	    */
    public String getUrl() {
        return url;
    }

    /**
	    * Returns the content length of a successful download
	    */
    public long getContentLength() {
        return contentLength;
    }

    /**
	    * Downloads the resource specified by url to destination
	    * 
	    * @param destination
	    * @throws Exception
	    */
    public void downloadTo(File destination) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        if (basicAuthToken != null) {
            log.info("Using basic authentication");
            log.debug("username: " + basicAuthToken.getUsername());
            log.debug("password: " + basicAuthToken.getPassword());
            httpclient.getCredentialsProvider().setCredentials(new AuthScope(basicAuthToken.getHost(), basicAuthToken.getPort()), new UsernamePasswordCredentials(basicAuthToken.getUsername(), basicAuthToken.getPassword()));
        }
        HttpGet httpResource = new HttpGet(url);
        log.info(httpResource.getRequestLine());
        HttpResponse response = httpclient.execute(httpResource);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        byte[] b = new byte[1024];
        int len;
        OutputStream out = new FileOutputStream(destination);
        while ((len = in.read(b)) != -1) out.write(b, 0, len);
        log.info(response.getStatusLine());
        if (entity != null) {
            contentLength = entity.getContentLength();
            log.info(entity.getContentType());
            log.info("Content-length: " + entity.getContentLength());
            entity.consumeContent();
        }
        httpclient.getConnectionManager().shutdown();
        log.info("Download saved to " + destination.getAbsolutePath());
    }
}
