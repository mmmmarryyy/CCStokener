package de.beas.explicanto.distribution.portlet.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author dorel
 *  
 */
public class HTTPRemote {

    private String host;

    /**
	 *  
	 */
    public HTTPRemote(String url) {
        this.host = url;
    }

    public String getEDSSessionToken() {
        String sess = null;
        BufferedReader br = null;
        try {
            URL url = new URL(host);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return br.readLine().trim();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e1) {
                }
            }
        }
        return sess;
    }

    public static void main(String[] args) {
        System.out.println(new HTTPRemote("http://dorel:8080/eds/admin/Proxy.action?userId=3839F38F16D64A7773D176E49FCA0E04").getEDSSessionToken());
    }
}
