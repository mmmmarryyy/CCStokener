package com.chinaoryx.ajax.proxy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * <pre>
 * this
 * </pre>
 * @author haipeng zhang
 * @
 */
public class UrlProxy {

    /**
     * <pre>
     * this method will return the content of url
     * </pre>
     * @param url String
     * @return String
     * @throws java.net.MalformedURLException
     * @throws java.io.IOException
     */
    public String getContent(String url) throws MalformedURLException, IOException {
        URLConnection httpConnection = new URL(url).openConnection();
        httpConnection.connect();
        BufferedInputStream bis = new BufferedInputStream(httpConnection.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        int bytesRead;
        while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
            baos.write(buff, 0, bytesRead);
        }
        return baos.toString("utf-8");
    }

    public static final void main(String[] args) throws MalformedURLException, IOException {
        UrlProxy proxy = new UrlProxy();
        String content = proxy.getContent("http://www.google.com/complete/search?hl=en&js=true&qu=ajax");
        System.out.println(content);
    }
}
