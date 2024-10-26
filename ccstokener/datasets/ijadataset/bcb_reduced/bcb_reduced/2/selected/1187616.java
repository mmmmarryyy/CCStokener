package com.sin.server.commonserv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import com.google.inject.Inject;

public class WebClientImpl implements WebClient {

    private static final Logger log = Logger.getLogger(WebClientImpl.class.getName());

    private String result;

    public WebClientImpl() {
    }

    ;

    @Override
    public String getContent(URL url) {
        try {
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            StringBuffer response = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            result = response.toString();
        } catch (SocketTimeoutException e) {
            log.severe("SoketTimeout NO!! RC  try again !!" + e.getMessage());
            result = null;
        } catch (Exception e) {
            log.severe("Except Rescue Start !! RC try again!! " + e.getMessage());
            result = null;
        }
        return result;
    }
}
