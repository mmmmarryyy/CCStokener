package com.xpresso.utils.web;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;

public class UrlWorker {

    public String getUrlContent(String theUrl) throws IOException {
        URL url = new URL(theUrl);
        return this.getUrlContent(url);
    }

    public String getUrlContent(URL theUrl) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(theUrl.openStream()));
        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        return sb.toString();
    }

    public void writeToFile(String url, String filePath) throws IOException {
        URL imageUrl = new URL(url);
        System.out.println("Requesting to: " + url);
        URLConnection urlC = imageUrl.openConnection();
        System.out.print("Copying resource (type: " + urlC.getContentType());
        Date date = new Date(urlC.getLastModified());
        InputStream is = imageUrl.openStream();
        FileOutputStream fos = new FileOutputStream(filePath);
        int oneChar, count = 0;
        while ((oneChar = is.read()) != -1) {
            fos.write(oneChar);
            count++;
        }
        is.close();
        fos.close();
    }
}
