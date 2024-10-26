package net.sf.jpasecurity.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.jpasecurity.ExceptionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Arne Limburg
 */
public abstract class AbstractXmlParser<H extends DefaultHandler> {

    private static final Log LOG = LogFactory.getLog(AbstractXmlParser.class);

    private H handler;

    private ExceptionFactory exceptionFactory;

    public AbstractXmlParser(H xmlHandler, ExceptionFactory factory) {
        handler = xmlHandler;
        exceptionFactory = factory;
    }

    protected H getHandler() {
        return handler;
    }

    public void parse(URL url) throws IOException {
        LOG.info("parsing " + url);
        InputStream stream = url.openStream();
        try {
            parse(stream);
        } finally {
            stream.close();
        }
    }

    public void parse(InputStream xml) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            parser.parse(xml, handler);
        } catch (ParserConfigurationException e) {
            throw exceptionFactory.createRuntimeException(e);
        } catch (SAXException e) {
            throw exceptionFactory.createRuntimeException(e);
        } catch (IOException e) {
            throw exceptionFactory.createRuntimeException(e);
        }
    }
}
