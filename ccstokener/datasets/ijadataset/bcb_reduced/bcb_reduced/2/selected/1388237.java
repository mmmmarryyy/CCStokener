package net.sourceforge.acmod.adaptiveconfig.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import net.sourceforge.acmod.adaptiveconfig.cache.ObjectPool;
import net.sourceforge.acmod.adaptiveconfig.dom.Document;
import net.sourceforge.acmod.adaptiveconfig.dom.Element;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Parser for the xml configuration
 * 
 * @author cmeadows
 * 
 */
public class XMLParser {

    static Logger log = Logger.getLogger(XMLParser.class);

    private static ObjectPool xmlReaderPool = new ObjectPool(5);

    public static Document parse(URL url, Map properties) {
        Document document = null;
        InputStream xmlIO = null;
        if (url != null) {
            try {
                xmlIO = url.openStream();
                InputSource inputSource = new InputSource(xmlIO);
                XMLContentHandler handler = new XMLContentHandler(properties);
                XMLReader xmlReader = (XMLReader) xmlReaderPool.remove();
                if (xmlReader == null) {
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    factory.setNamespaceAware(false);
                    factory.setValidating(false);
                    xmlReader = factory.newSAXParser().getXMLReader();
                }
                xmlReader.setContentHandler(handler);
                xmlReader.parse(inputSource);
                xmlReaderPool.add(xmlReader);
                document = handler.getDocument();
                xmlIO.close();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return document;
    }
}
