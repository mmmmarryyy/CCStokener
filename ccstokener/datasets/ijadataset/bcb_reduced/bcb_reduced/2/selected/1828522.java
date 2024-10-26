package com.volantis.mcs.maml;

import com.volantis.mcs.context.MarinerRequestContext;
import com.volantis.mcs.marlin.sax.AbstractPAPIContentHandler;
import com.volantis.mcs.marlin.sax.MarlinContentHandler;
import com.volantis.mcs.marlin.sax.MarlinSAXHelper;
import com.volantis.mcs.marlin.sax.AbstractMarlinContentHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.util.Map;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.mcs.localization.LocalizationFactory;
import com.volantis.xml.sax.ExtendedSAXException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;

/**
 * @deprecated See {@link MarlinContentHandler} and {@link MarlinSAXHelper}.
 * @volantis-api-include-in PublicAPI
 * @volantis-api-include-in ProfessionalServicesAPI
 * @volantis-api-include-in InternalAPI
 */
public class MamlSAXParser {

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(MamlSAXParser.class);

    /**
     * The handler for maml files
     */
    private ContentHandler handler;

    /**
     * The SAX parser
     */
    private XMLReader parser;

    /**
     * Set the request context for the parser. The appropriate context should
     * be obtained from MarinerRequestContext.createNestedContext(), either a
     * MarinerJspRequestContext for JSP's or a MarinerServletRequestContext for
     * Servlets.
     * @param context The request context to use for the parser.
     */
    public void setRequestContext(MarinerRequestContext context) {
        handler = MarlinSAXHelper.getContentHandler(context);
        parser = MarlinSAXHelper.getXMLReader();
        parser.setContentHandler(handler);
        parser.setErrorHandler(new InternalErrorHandler());
    }

    /**
     * Parse an XML file containing MAML XML source.
     * @param uri The uri of the XML file to parse.
     */
    public void parse(String uri) throws IOException, SAXException {
        parser.parse(uri);
    }

    /**
     * Parse an InputStream containing MAML XML source.
     * @param is The InputStream containing the XML to parse.
     */
    public void parse(InputStream is) throws IOException, SAXException {
        parser.parse(new InputSource(is));
    }

    /**
     * Parse an InputSource containing MAML XML source.
     * @param is The InputSource containing the XML to parse.
     */
    public void parse(InputSource is) throws IOException, SAXException {
        parser.parse(is);
    }

    /**
     * Parse the input directly from a string which contains MAML XML source.
     *
     * @param xml The MAML XML document as a string
     * @throws IOException if there is a problem reading from the document
     * @throws SAXException if there is a problem with the XML in the document
     * @deprecated
     */
    public void parseString(String xml) throws IOException, SAXException {
        parser.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Parse the input from a URL which returns MAML XML source.
     * @param spec  The URL of the input source
     */
    public void parseURL(String spec) throws IOException, SAXException {
        URL url;
        try {
            url = new URL(spec);
        } catch (MalformedURLException mue) {
            logger.error("invalid-url", mue);
            throw new ExtendedSAXException(mue);
        }
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        parser.parse(new InputSource(connection.getInputStream()));
    }

    private class InternalErrorHandler implements ErrorHandler {

        /**
         * Called by the sax parser when a non-serious error is encountered in
         * the XML file.
         * @param exc  A SAXParseException describing the problem
         */
        public void warning(SAXParseException exc) {
            logger.warn("warning", exc);
        }

        /**
         * Called by the sax parser when a serious error is encountered in
         * the XML file.
         * @param exc  A SAXParseException describing the problem
         */
        public void error(SAXParseException exc) {
            logger.error("error", exc);
        }

        /**
         * Called by the sax parser when a fatal error is encountered in
         * the XML file.
         * @param exc  A SAXParseException describing the problem
         */
        public void fatalError(SAXParseException exc) throws SAXException {
            logger.error("error", exc);
            throw new ExtendedSAXException(exc);
        }
    }
}
