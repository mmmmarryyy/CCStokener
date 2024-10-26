package org.foafrealm.tools;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * @author adagze
 *
 */
public class IKHConnector extends Thread {

    private String uri;

    public IKHConnector(String _uri) {
        this.uri = _uri;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(this.uri);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("PUT");
            huc.connect();
            if (huc.getResponseCode() == 200) {
                System.out.println("Harvested: " + this.uri);
            } else if (huc.getResponseCode() > 200) {
                System.out.println("Not Harvested: " + this.uri + " error: " + huc.getResponseCode());
            }
            huc.disconnect();
        } catch (MalformedURLException e) {
        } catch (ProtocolException e) {
        } catch (IOException e) {
        }
    }

    /**
	 * @return Returns the uri.
	 */
    public String getUri() {
        return uri;
    }

    /**
	 * @param uri The uri to set.
	 */
    public void setUri(String uri) {
        this.uri = uri;
    }
}
