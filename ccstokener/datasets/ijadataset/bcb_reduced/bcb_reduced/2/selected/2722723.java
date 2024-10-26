package be.ac.fundp.infonet.econf.util;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.XPathException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import be.ac.fundp.infonet.econf.ConfigurationException;

/**
 *
 *
 * @author     gjoseph
 * @author     $Author: stephane_nicoll $ (last edit)
 * @version    $Revision: 1.1 $
 * @created    03-aug-02
 */
public class XMLResource {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XMLResource.class.getName());

    private Document doc;

    private String url;

    /**
     * Creates a new XMLResource from the specified url
     *
     * @param url the url to load the XMLResource from. Can be in form of
     *            "file://" "http://" or "classpath://" (which loads from the
     *            classpath. To make sure you load from the classpath (you
     *            "never" know exactly what the current directory is in the
     *            classpath, do you?), use "classpath:///somedir/somefile.xml".
     *
     * @throws ConfigException if the url of the xml file is invalid
     */
    public XMLResource(String url) throws ConfigurationException {
        DOMParser parser = new DOMParser();
        try {
            this.url = url;
            InputStream is = createInputStream(url);
            parser.parse(new InputSource(is));
            this.doc = parser.getDocument();
            is.close();
        } catch (SAXException ex) {
            logger.warn("Coulnd't load [" + url + "]", ex);
            throw new ConfigurationException("Couldn't load [" + url + "]: " + ex.getMessage());
        } catch (IOException ex) {
            logger.warn("Couldn't load [" + url + "]", ex);
            throw new ConfigurationException("Couldn't load [" + url + "]: " + ex.getMessage());
        }
    }

    /**
     * Creates a new XMLResource from the specified file.
     *
     * @param f   the file containing the XML resourece.
     * @throws ConfigurationException if the path of the xml file is invalid
     */
    public XMLResource(File f) throws ConfigurationException {
        DOMParser parser = new DOMParser();
        try {
            if (!f.exists()) throw new ConfigurationException("Couldn't load [" + f + "]: file does not exist");
            this.url = f.toURL().toString();
            InputStream is = new FileInputStream(f);
            parser.parse(new InputSource(is));
            this.doc = parser.getDocument();
            is.close();
        } catch (SAXException ex) {
            logger.warn("Coulnd't load [" + f + "]", ex);
            throw new ConfigurationException("Couldn't load [" + f + "]: " + ex.getMessage());
        } catch (IOException ex) {
            logger.warn("Couldn't load [" + f + "]", ex);
            throw new ConfigurationException("Couldn't load [" + f + "]: " + ex.getMessage());
        }
    }

    /**
     * Returns a string from the XMLResource using the specified xpath query
     *
     * @param xpath the path to the element needed
     * @return a <code>String</code> corresponding to the value of the element
     *         to which the xpath points to
     * @throws TransformerException
     * @throws XPathException
     */
    public String getString(String xpath) throws TransformerException, XPathException {
        Node rootNode = doc.getFirstChild();
        Node n = XPathAPI.selectSingleNode(rootNode, xpath);
        if (n == null) throw new XPathException("Incorrect XPath[" + xpath + "]");
        return n.getFirstChild().getNodeValue();
    }

    /**
     * Returns a <code>List</code> of Strings from the XMLResource using the
     * specified xpath query
     *
     * @param xpath the path to the elements needed
     * @return a <code>List</code> of <code>String</code>s corresponding to
     *         the value of the element to which the xpath points to
     * @throws TransformerException
     * @throws XPathException
     */
    public List getStrings(String xpath) throws TransformerException, XPathException {
        Node rootNode = doc.getFirstChild();
        NodeList nl = XPathAPI.selectNodeList(rootNode, xpath);
        List list = new ArrayList();
        String s;
        for (int i = 0, j = nl.getLength(); i < j; i++) {
            s = nl.item(i).getFirstChild().getNodeValue();
            list.add(s);
        }
        return list;
    }

    public List getElements(String xpath) throws TransformerException, XPathException {
        Node rootNode = doc.getFirstChild();
        NodeList nl = XPathAPI.selectNodeList(rootNode, xpath);
        List list = new ArrayList();
        Node n;
        for (int i = 0, j = nl.getLength(); i < j; i++) {
            n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) list.add((Element) n);
        }
        return list;
    }

    public Element getElement(String xpath) throws TransformerException, XPathException {
        List res = getElements(xpath);
        if (res.isEmpty()) return null; else return (Element) res.get(0);
    }

    /**
     * Returns an <code>InputStream</code> created using an url that should be
     * in the element to which the xpath points to.
     * This could be "dangerous". To be used with great care.
     *
     * dev-note: Maybe we should handle some of the exceptions. Not sure.
     *
     * @param xpath the path to the url needed
     * @return an <code>InputStream</code> created with the url retrieved
     *          from the value of the element to which the xpath points to
     * @throws TransformerException
     * @throws XPathException
     * @throws MalformedURLException
     * @throws IOException
     */
    public InputStream getInputStream(String xpath) throws TransformerException, XPathException, MalformedURLException, IOException {
        String url = getString(xpath);
        return createInputStream(url);
    }

    /**
     * returns the url of the source document of the XMLResource
     * @return the url of the source document of the XMLResource
     */
    public String getURL() {
        return this.url;
    }

    /**
     * gets an inputstream for either the classpath
     * (url must begin with "classpath://"),
     * a file or url (file://, http://, ...)
     *
     * @param url the url to load from
     * @return an InputStream created from the specified url
     * @throws MalformedURLException
     * @throws IOException
     */
    private InputStream createInputStream(String url) throws MalformedURLException, IOException {
        if (url.startsWith("classpath://")) {
            InputStream is = XMLResource.class.getResourceAsStream(url.substring(12));
            if (is == null) throw new IOException("Couldn't open stream to [" + url + "]");
            return is;
        } else {
            return new URL(url).openStream();
        }
    }
}
