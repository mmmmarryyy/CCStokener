package net.sbbi.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Handy stuff for dealing with XML
 * 
 * @author ryanm
 */
public class XMLUtil {

    /** {@link XPath} instance */
    public static final XPath xpath = XPathFactory.newInstance().newXPath();

    private static final char buggyChar = (char) 0;

    private static final DocumentBuilder builder;

    static {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder b = null;
        try {
            b = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        builder = b;
    }

    /**
	 * @param url
	 * @return the xml string at that url
	 */
    public static String getXMLString(URL url) {
        try {
            InputStream in = url.openStream();
            StringBuilder xml = new StringBuilder();
            byte[] buffer = new byte[512];
            int readen = 0;
            while ((readen = in.read(buffer)) != -1) {
                xml.append(new String(buffer, 0, readen));
            }
            String doc = xml.toString();
            if (doc.indexOf(buggyChar) != -1) {
                doc = doc.replace(buggyChar, ' ');
            }
            return doc;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Fetches the xml, fixes any wonky characters in it
	 * 
	 * @param url
	 * @return The xml {@link Document}
	 */
    public static Document getXML(URL url) {
        try {
            String doc = getXMLString(url);
            ByteArrayInputStream in2 = new ByteArrayInputStream(doc.getBytes());
            return builder.parse(in2);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
