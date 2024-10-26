package net.sipvip.server.services.geoinfo.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;
import com.google.inject.Inject;
import net.sipvip.server.services.geoinfo.inte.WebClient;

public class WebClientImpl implements WebClient {

    private static final Logger log = Logger.getLogger(WebClientImpl.class.getName());

    @Inject
    WebClientImpl() {
    }

    ;

    public String getContent(URL url) {
        log.info("Start WebClientImpl " + url.getHost());
        StringBuffer content = new StringBuffer();
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            InputStream is = connection.getInputStream();
            byte[] buffer = new byte[2048];
            int count;
            while (-1 != (count = is.read(buffer))) {
                content.append(new String(buffer, 0, count));
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
            return null;
        }
        return content.toString();
    }
}
