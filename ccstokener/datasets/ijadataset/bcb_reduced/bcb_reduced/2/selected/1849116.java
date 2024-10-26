package net.sipvip.server.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

public class ForDomainparReq {

    private static final Logger log = Logger.getLogger(ForSpidersReq.class.getName());

    private String jsonContectResult;

    public ForDomainparReq(String urlstr, String domain) throws IOException {
        URL url = new URL(urlstr);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("domain", domain);
        try {
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
    }
}
