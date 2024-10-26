package org.corrib.s3b.sscf.tools;

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

    private String tags;

    private String mbox;

    public IKHConnector(String _uri, String _tags, String _mbox) {
        this.uri = _uri;
        this.tags = _tags;
        this.mbox = _mbox;
    }

    @Override
    public void run() {
        try {
            URL url = new URL(this.uri);
            String data = "tags=" + this.tags + "&mbox=" + this.mbox + "&_method=put";
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("POST");
            huc.setDoOutput(true);
            huc.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            huc.setRequestProperty("Content-Length", "" + data.length());
            huc.getOutputStream().write(data.getBytes());
            huc.getOutputStream().flush();
            huc.connect();
            if (huc.getResponseCode() == 200) {
                System.out.println("Harvested: " + this.uri);
            } else if (huc.getResponseCode() > 200) {
                System.out.println("Not Harvested: " + this.uri + " error: " + huc.getResponseCode());
            }
            huc.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
    public void setUri(String _uri) {
        this.uri = _uri;
    }

    /**
	 * @return Returns the tags.
	 */
    public String getTags() {
        return tags;
    }

    /**
	 * @param tags The tags to set.
	 */
    public void setTags(String _tags) {
        this.tags = _tags;
    }
}
