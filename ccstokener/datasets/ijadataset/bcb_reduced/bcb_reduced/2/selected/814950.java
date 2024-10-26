package com.esl.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebUtil {

    private static Logger logger = LoggerFactory.getLogger("ESL");

    public static BufferedReader getReaderFromURL(String address) throws IOException {
        try {
            HttpURLConnection connection;
            URL url = new URL(address);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/5.0)");
            InputStream in = connection.getInputStream();
            return new BufferedReader(new InputStreamReader(in));
        } catch (IOException e) {
            logger.debug("Exception during connect to " + address, e);
            throw e;
        }
    }
}
