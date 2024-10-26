package gov.noaa.eds.xapi.generic.handlers;

import gov.noaa.eds.xapi.generic.DomHandler;
import gov.noaa.eds.xapi.generic.DomHandlerException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.crimson.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handles communication between a URL.
 *
 * @version $Id: UrlHandler.java,v 1.2 2004/12/23 23:39:15 mrxtravis Exp $
 * @author tns
 */
public class UrlHandler implements DomHandler {

    private URLConnection connection = null;

    private Document document = null;

    private long lastModified = 0;

    private DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();

    /**
     * Holds value of property url.
     */
    private URL url;

    /**
     * Holds value of property urlString.
     */
    private String urlString;

    /**
     * Holds value of property millisBetweenChecks.
     */
    private long millisBetweenChecks = 0;

    private long lastCheck = 0;

    /** Creates a new instance of UrlHandler */
    public UrlHandler() {
    }

    /**Unsupported
     *@todo implement this method
     */
    public void setResourceAsNode(org.w3c.dom.Node node) {
        throw new UnsupportedOperationException("URL Handler is read-only.");
    }

    /** Returns the URL as a Document
     *@return The URL converted into a Document
     */
    public org.w3c.dom.Document getResourceAsDocument() throws DomHandlerException {
        if (url == null) {
            throw new IllegalStateException("url property not set.");
        }
        if (this.lastCheck == 0) {
            this.lastCheck = System.currentTimeMillis();
            load();
        } else if (this.millisBetweenChecks == 0 || this.lastCheck + this.millisBetweenChecks > System.currentTimeMillis()) {
            this.lastCheck = System.currentTimeMillis();
            try {
                URLConnection connection = url.openConnection();
                if (connection.getLastModified() > this.lastModified) {
                    load();
                }
            } catch (IOException e) {
                throw new DomHandlerException(e);
            }
        }
        return this.document;
    }

    /** Try to minimalize calls to the URL*/
    private synchronized void load() throws DomHandlerException {
        URLConnection connection = null;
        InputStream is = null;
        try {
            connection = url.openConnection();
            long connectionModified = connection.getLastModified();
            if (connectionModified > this.lastModified) {
                this.lastModified = connectionModified;
                DocumentBuilder builder = factory.newDocumentBuilder();
                is = connection.getInputStream();
                if (is == null) {
                    throw new NullPointerException("Could not get an input stream for url:" + url.toExternalForm());
                }
                Document newDoc = builder.parse(is);
                this.document = newDoc;
            }
        } catch (IOException e) {
            throw new DomHandlerException(e);
        } catch (SAXException e) {
            throw new DomHandlerException(e);
        } catch (ParserConfigurationException e) {
            throw new DomHandlerException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Getter for property url.
     * @return Value of property url.
     */
    public URL getUrl() {
        return this.url;
    }

    /**
     * Setter for property url.
     * @param url New value of property url.
     */
    public void setUrl(URL url) {
        if (this.url != null) {
            throw new IllegalStateException("uri property has already been set.");
        }
        if (url == null) {
            throw new NullPointerException("parameter url must not be null");
        }
        this.url = url;
    }

    /**
     * Getter for property urlString.
     * @return Value of property urlString.
     */
    public String getUrlString() {
        return this.url.toExternalForm();
    }

    /**
     * Setter for property urlString.
     * @param urlString New value of property urlString.
     */
    public void setUrlString(String urlString) throws MalformedURLException {
        URL u = new URL(urlString);
        this.setUrl(u);
    }

    /**
     * Getter for property timeBetweenChecks.
     * @return Value of property timeBetweenChecks.
     */
    public long getMillisBetweenChecks() {
        return this.millisBetweenChecks;
    }

    /**
     * Setter for property timeBetweenChecks.
     * @param timeBetweenChecks New value of property timeBetweenChecks.
     */
    public void setMillisBetweenChecks(long millisBetweenChecks) {
        this.millisBetweenChecks = millisBetweenChecks;
    }
}
