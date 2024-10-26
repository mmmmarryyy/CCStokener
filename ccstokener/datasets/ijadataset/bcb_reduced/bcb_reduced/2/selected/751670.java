package com.myrealtor.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HTMLUtil {

    protected static final Log log = LogFactory.getLog(HTMLUtil.class);

    public static String getURLContent(String urlStr) throws MalformedURLException, IOException {
        URL url = new URL(urlStr);
        log.info("url: " + url);
        URLConnection conn = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer buf = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            buf.append(inputLine);
        }
        in.close();
        return buf.toString();
    }
}
