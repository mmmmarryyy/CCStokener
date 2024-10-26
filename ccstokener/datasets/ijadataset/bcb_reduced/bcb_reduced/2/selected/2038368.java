package org.geoforge.worldwind.util;

import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.geoforge.lang.util.logging.FileHandlerLogger;

/**
 *
 * @author bantchao
 * 
 * an overriden class to handle uncaught WWD exceptions
 */
public class GfrWWXML {

    private static final Logger _LOGGER_ = Logger.getLogger(GfrWWXML.class.getName());

    static {
        GfrWWXML._LOGGER_.addHandler(FileHandlerLogger.s_getInstance());
    }

    public static XMLEventReader openEventReader(Object docSource) throws IOException, XMLStreamException, IllegalArgumentException {
        return _s_openEventReader(docSource, true);
    }

    private static XMLEventReader _s_openEventReader(Object docSource, boolean isNamespaceAware) throws IOException, XMLStreamException, IllegalArgumentException {
        if (docSource == null || WWUtil.isEmpty(docSource)) {
            String str = "docSource == null || WWUtil.isEmpty(docSource)";
            GfrWWXML._LOGGER_.severe(str);
            throw new IllegalArgumentException(str);
        }
        if (docSource instanceof URL) {
            return GfrWWXML._s_openEventReaderURL((URL) docSource, isNamespaceAware);
        }
        return WWXML.openEventReader(docSource, isNamespaceAware);
    }

    private static XMLEventReader _s_openEventReaderURL(URL url, boolean isNamespaceAware) throws IOException, XMLStreamException {
        InputStream inputStream = url.openStream();
        return _s_openEventReaderStream(inputStream, isNamespaceAware);
    }

    private static XMLEventReader _s_openEventReaderStream(InputStream inputStream, boolean isNamespaceAware) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, isNamespaceAware);
        inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        return inputFactory.createXMLEventReader(inputStream);
    }
}
