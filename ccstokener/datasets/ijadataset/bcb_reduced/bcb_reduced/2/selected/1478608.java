package org.jtools.config.xml.sax;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.jtools.config.Configure;
import org.jtools.config.protocol.ConfigProtocol;
import org.jtools.config.protocol.DefaultConfigProtocol;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author Rainer
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class SaxEngine<T_Result> {

    private ConfigProtocol configProtocol;

    private ContentHandler<T_Result> contentHandler;

    public ContentHandler<T_Result> getContentHandler() {
        return contentHandler;
    }

    public synchronized ConfigProtocol getConfigProtocol() {
        return configProtocol;
    }

    /**
     *
     */
    public SaxEngine(Configure<T_Result> configloader, ContentHandler<T_Result> contentHandler, ConfigProtocol configProtocol) {
        this.configProtocol = (configProtocol == null) ? new DefaultConfigProtocol(configloader) : configProtocol;
        this.contentHandler = (contentHandler == null) ? new DefaultContentHandler<T_Result>(this.configProtocol) : contentHandler;
        this.contentHandler.setSaxEngine(this);
    }

    private static SAXParserFactory parserFactory;

    public static synchronized SAXParserFactory getParserFactory() {
        if (parserFactory == null) {
            parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
        }
        return parserFactory;
    }

    public T_Result unmarshall(URL url) throws SAXException, ParserConfigurationException, IOException {
        XMLReader parser = getParserFactory().newSAXParser().getXMLReader();
        parser.setContentHandler(getContentHandler());
        parser.setDTDHandler(getContentHandler());
        parser.setEntityResolver(getContentHandler());
        parser.setErrorHandler(getContentHandler());
        InputSource inputSource = new InputSource(url.openStream());
        inputSource.setSystemId(url.toString());
        parser.parse(inputSource);
        return contentHandler.getRootObject();
    }
}
