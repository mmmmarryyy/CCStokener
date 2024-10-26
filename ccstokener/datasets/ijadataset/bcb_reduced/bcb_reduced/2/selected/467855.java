package org.silabsoft.rs.web.toolbar.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Silabsoft
 */
public class CookieHTTPXMLResource extends HTTPXMLResource {

    private String toolbarHash;

    private String toolbarCode;

    /**
     *
     * @param toolbarHash
     * @param toolbarCode
     * @param url
     */
    public CookieHTTPXMLResource(String url, String toolbarHash, String toolbarCode) throws MalformedURLException {
        super(url);
        this.toolbarHash = toolbarHash;
        this.toolbarCode = toolbarCode;
    }

    public CookieHTTPXMLResource(URL url, String toolbarHash, String toolbarCode) {
        super(url);
        this.toolbarHash = toolbarHash;
        this.toolbarCode = toolbarCode;
    }

    /**
     *
     * @return
     * @throws Exception
     */
    @Override
    public InputStream getResource() {
        try {
            URLConnection urlConn = url.openConnection();
            urlConn.setRequestProperty("Cookie", "toolbar_hash=" + this.toolbarHash.replaceAll(" ", "_") + "; toolbar_code=" + this.toolbarCode + ";");
            urlConn.connect();
            return urlConn.getInputStream();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setToolbarCode(String toolbarCode) {
        this.toolbarCode = toolbarCode;
    }

    public void setToolbarHash(String toolbarHash) {
        this.toolbarHash = toolbarHash;
    }

    public String getToolbarCode() {
        return toolbarCode;
    }

    public String getToolbarHash() {
        return toolbarHash;
    }
}
