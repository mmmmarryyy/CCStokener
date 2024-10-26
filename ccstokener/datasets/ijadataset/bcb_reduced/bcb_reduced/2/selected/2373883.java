package net.sipvip.server.services.geoinfo.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import com.google.inject.Inject;
import net.sipvip.server.services.geoinfo.inte.WebClient;

public class WebClientImpl2 implements WebClient {

    private static final Logger log = Logger.getLogger(WebClientImpl2.class.getName());

    private String jsonContectResult;

    @Inject
    WebClientImpl2() {
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
            jsonContectResult = response.toString();
        } catch (SocketTimeoutException e) {
            log.severe("SoketTimeout NO!! RC  try again !!" + e.getMessage());
            jsonContectResult = null;
        } catch (Exception e) {
            log.severe("Except Rescue Start !! RC try again!! " + e.getMessage());
            jsonContectResult = null;
        }
        return jsonContectResult;
    }
}
