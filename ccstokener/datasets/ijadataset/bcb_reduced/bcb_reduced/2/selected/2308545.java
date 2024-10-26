package net.sipvip.server.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import junit.framework.TestCase;

public class ReqJsonContent {

    private static final Logger log = Logger.getLogger(ReqJsonContent.class.getName());

    private String jsonContectResult;

    private StringBuffer response;

    public ReqJsonContent(String useragent, String urlstr, String domain, String pathinfo, String alarmMessage) throws IOException {
        URL url = new URL(urlstr);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("user-agent", useragent);
        conn.setRequestProperty("pathinfo", pathinfo);
        conn.setRequestProperty("domain", domain);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            response = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            jsonContectResult = response.toString();
        } catch (SocketTimeoutException e) {
            log.severe(alarmMessage + "-> " + e.getMessage());
            jsonContectResult = null;
        } catch (Exception e) {
            log.severe(alarmMessage + "-> " + e.getMessage());
            jsonContectResult = null;
        }
    }

    public String getJsonContentResult() {
        return jsonContectResult;
    }
}
