package org.gwtoolbox.commons.generator.rebind;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Uri Boness
 */
public class XmlUtils {

    private static SAXParserFactory saxParserFactory;

    public static void parse(URL url, ContentHandler handler) {
        InputStream input = null;
        try {
            input = url.openStream();
            SAXParser parser = createSaxParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(handler);
            reader.parse(new InputSource(input));
        } catch (SAXException e) {
            throw new XmlException("Could not parse xml", e);
        } catch (IOException e) {
            throw new XmlException("Could not parse xml", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static SAXParser createSaxParser() {
        try {
            return getSaxParserFactory().newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new XmlException("Could not create SAX parser", e);
        } catch (SAXException e) {
            throw new XmlException("Could not create SAX parser", e);
        }
    }

    public static SAXParserFactory getSaxParserFactory() {
        if (saxParserFactory == null) {
            saxParserFactory = SAXParserFactory.newInstance();
        }
        return saxParserFactory;
    }
}
