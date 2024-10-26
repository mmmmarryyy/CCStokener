package icm.unicore.plugins.dbbrowser;

import java.net.*;
import java.io.*;

/** Sends forms to the server using GET or POST method.
 * @author Michal Wronski (wrona@mat.uni.torun.pl)
 */
public class FormPoster {

    public static final int GET = 0;

    public static final int POST = 1;

    private String urlString;

    public FormPoster(String urlString) {
        this.urlString = urlString;
    }

    public String getUrlString() {
        return this.urlString;
    }

    public InputStream post(QueryString query, int method) throws Exception {
        if (com.pallas.unicore.resourcemanager.ResourceManager.getUserDefaults().isProxyEnabled()) {
            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", com.pallas.unicore.resourcemanager.ResourceManager.getUserDefaults().getProxyHost());
            System.setProperty("http.proxyPort", com.pallas.unicore.resourcemanager.ResourceManager.getUserDefaults().getProxyPort() + "");
        } else {
            System.setProperty("http.proxySet", "false");
        }
        URL url;
        if (method == POST) {
            url = new URL(urlString);
            URLConnection uc = url.openConnection();
            uc.setDoOutput(true);
            OutputStream os = uc.getOutputStream();
            OutputStreamWriter out = new OutputStreamWriter(uc.getOutputStream(), "UTF-8");
            out.write(query.toString());
            out.flush();
            out.close();
            return uc.getInputStream();
        } else {
            url = new URL(urlString + "?" + query.toString());
            return url.openStream();
        }
    }
}
