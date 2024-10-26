package gov.noaa.edsd.remotexmldb.urlsToXmldb;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Handles the interaction with the URL.
 * @author tns
 */
public class UrlHandler {

    private URL url = null;

    private long lastModified = -1;

    private Document document = null;

    /**
     * Holds value of property reloadable.
     */
    private boolean reloadable = true;

    ;

    /** Creates a new instance of UrlHandler */
    public UrlHandler() {
    }

    /**Sets the URL using a string spec
     *@param
     */
    public void setUrlSpec(String urlSpec) throws MalformedURLException {
        URL url = new URL(urlSpec);
        this.setUrl(url);
    }

    public void setUrl(URL url) {
        if (this.url != null) {
            throw new IllegalStateException("url has already been set");
        }
        this.url = url;
    }

    /** returns the url*/
    public URL getUrl() {
        return this.url;
    }

    private boolean doReload(URLConnection connection) {
        if (lastModified == -1) {
            return true;
        }
        if (!reloadable) {
            return false;
        }
        if (connection.getLastModified() > this.lastModified) {
            return true;
        } else {
            return false;
        }
    }

    /**
     */
    public Document getContentAsDocument() {
        synchronized (this.url) {
            URLConnection connection = this.url.openConnection();
            if (doReload(connection)) {
                InputSource inputSource = new InputSource(connection.getInputStream());
                DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();
                this.document = factory.newDocumentBuilder().parse(inputSource);
            }
            return this.document;
        }
    }

    /**
     * Getter for property reloadable.
     * @return Value of property reloadable.
     */
    public boolean isReloadable() {
        return this.reloadable;
    }

    /**
     * Setter for property reloadable.
     * @param reloadable New value of property reloadable.
     */
    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }
}
