package com.rooster.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import org.apache.log4j.Logger;

public class URLReader {

    private static Logger logger = Logger.getLogger(URLReader.class);

    public URLReader() {
    }

    public static void main(String[] args) {
        String sHost = "http://api.hostip.info/get_html.php?ip=12.215.42.19&position=true";
        String sStr = readFromURL(sHost);
        try {
            String sCountry = sStr.substring((sStr.indexOf("Country:") + 8), sStr.indexOf("City:"));
            sCountry = sCountry.trim();
            String sCity = sStr.substring((sStr.indexOf("City:") + 5), sStr.indexOf("Latitude:"));
            sCity = sCity.trim();
            String sLatitue = sStr.substring((sStr.indexOf("Latitude:") + 9), sStr.indexOf("Longitude:"));
            sLatitue = sLatitue.trim();
            String sLongitude = sStr.substring((sStr.indexOf("Longitude:") + 10));
            sLongitude = sLongitude.trim();
            log(sCountry);
            log(sCity);
            log(sLatitue);
            log(sLongitude);
        } catch (Exception e) {
        }
        log(sStr);
    }

    public static String readFromURL(String sURL) {
        logger.info("com.rooster.utils.URLReader.readFromURL - Entry");
        String sWebPage = "";
        try {
            URL url = new URL(sURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String inputLine = "";
            while ((inputLine = in.readLine()) != null) {
                sWebPage += inputLine;
            }
            in.close();
        } catch (Exception e) {
            logger.debug("com.rooster.utils.URLReader.readFromURL - Error" + e);
        }
        logger.info("com.rooster.utils.URLReader.readFromURL - Exit");
        return sWebPage;
    }

    private static void log(Object aObject) {
        System.out.println(aObject);
    }
}
