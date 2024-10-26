package com.daycomtech.jgooglechart.util;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.imageio.ImageIO;
import com.daycomtech.jgooglechart.exception.JGoogleChartException;

public class JGChartCallerUtil {

    public static Image getImage(String urlChartString) throws IOException, JGoogleChartException {
        Image image = null;
        HttpURLConnection urlConn = null;
        URL url = new URL(urlChartString);
        urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestMethod("GET");
        urlConn.setAllowUserInteraction(false);
        urlConn.setRequestProperty("HTTP-Version", "HTTP/1.1");
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        int responseCode = urlConn.getResponseCode();
        if (responseCode != 200) {
            throw new JGoogleChartException(JGoogleChartException.MSG_HTTP_ERROR_CODE + responseCode + " (" + urlConn.getResponseMessage());
        }
        InputStream istream = urlConn.getInputStream();
        image = ImageIO.read(istream);
        return image;
    }
}
