package org.xhtmlrenderer.resource;

import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.io.BufferedInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * <p>FSCatalog loads an XML catalog file to read mappings of public IDs for
 * XML schemas/dtds, to resolve those mappings to a local store for the schemas.
 * The catalog file allows one to have a single mapping of schema IDs to local
 * files, and is useful when there are many schemas, or when schemas are broken
 * into many smaller files. Currently FSCatalog only supports the very simple
 * mapping of public id to local URI using the public element in the catalog XML.
 * <p/>
 * <p>FSCatalog is not an EntityResolver; it only parses a catalog file. See
 * {@link FSEntityResolver} for entity resolution.
 * <p/>
 * <p>To use, instantiate the class, and call {@link #parseCatalog(InputSource)}
 * to retrieve a {@link java.util.Map} keyed by public ids. The class uses
 * an XMLReader instance retrieved via {@link XMLResource#newXMLReader()}, so
 * XMLReader configuration (and specification) follows that of the standard XML
 * parsing in Flying Saucer.
 * <p/>
 * <p>This class is not safe for multi-threaded access.
 *
 * @author Patrick Wright
 */
public class FSCatalog {

    /**
     * Default constructor
     */
    public FSCatalog() {
    }

    /**
     * Parses an XML catalog file and returns a Map of public ids to local URIs read
     * from the catalog. Only the catalog public elements are parsed.
     *
     * @param catalogURI A String URI to a catalog XML file on the classpath.
     */
    public Map parseCatalog(String catalogURI) {
        URL url = null;
        Map map = null;
        try {
            url = FSCatalog.class.getClassLoader().getResource(catalogURI);
            map = parseCatalog(new InputSource(new BufferedInputStream(url.openStream())));
        } catch (Exception ex) {
            XRLog.xmlEntities(Level.WARNING, "Could not open XML catalog from URI '" + catalogURI + "'", ex);
            map = new HashMap();
        }
        return map;
    }

    /**
     * Parses an XML catalog file and returns a Map of public ids to local URIs read
     * from the catalog. Only the catalog public elements are parsed.
     *
     * @param inputSource A SAX InputSource to a catalog XML file on the classpath.
     */
    public Map parseCatalog(InputSource inputSource) {
        XMLReader xmlReader = XMLResource.newXMLReader();
        CatalogContentHandler ch = new CatalogContentHandler();
        addHandlers(xmlReader, ch);
        setFeature(xmlReader, "http://xml.org/sax/features/validation", false);
        try {
            xmlReader.parse(inputSource);
        } catch (Exception ex) {
            throw new RuntimeException("Failed on configuring SAX to DOM transformer.", ex);
        }
        return ch.getEntityMap();
    }

    /**
     * Adds the default EntityResolved and ErrorHandler for the SAX parser.
     */
    private void addHandlers(XMLReader xmlReader, ContentHandler ch) {
        try {
            xmlReader.setContentHandler(ch);
            xmlReader.setErrorHandler(new ErrorHandler() {

                public void error(SAXParseException ex) {
                    XRLog.xmlEntities(Level.WARNING, ex.getMessage());
                }

                public void fatalError(SAXParseException ex) {
                    XRLog.xmlEntities(Level.WARNING, ex.getMessage());
                }

                public void warning(SAXParseException ex) {
                    XRLog.xmlEntities(Level.WARNING, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            throw new XRRuntimeException("Failed on configuring SAX parser/XMLReader.", ex);
        }
    }

    /**
     * A SAX ContentHandler that reads an XML catalog file and builds a Map of
     * public IDs to local URIs. Currently only handles the <public> element and attributes.
     * To use, just call XMLReader.setContentHandler() with an instance of the class,
     * parse, then call getEntityMap().
     */
    private class CatalogContentHandler extends DefaultHandler {

        private Map entityMap;

        public CatalogContentHandler() {
            this.entityMap = new HashMap();
        }

        /**
         * Returns a Map of public Ids to local URIs
         */
        public Map getEntityMap() {
            return entityMap;
        }

        /**
         * Receive notification of the beginning of an element; here used to pick up the mappings
         * for public IDs to local URIs in the catalog.
         */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            if (localName.equals("public") || (localName.equals("") && qName.equals("public"))) {
                entityMap.put(atts.getValue("publicId"), atts.getValue("uri"));
            }
        }
    }

    /**
     * Attempts to set requested feature on the parser; logs exception if not supported
     * or not recognized.
     */
    private void setFeature(XMLReader xmlReader, String featureUri, boolean value) {
        try {
            xmlReader.setFeature(featureUri, value);
            XRLog.xmlEntities(Level.FINE, "SAX Parser feature: " + featureUri.substring(featureUri.lastIndexOf("/")) + " set to " + xmlReader.getFeature(featureUri));
        } catch (SAXNotSupportedException ex) {
            XRLog.xmlEntities(Level.WARNING, "SAX feature not supported on this XMLReader: " + featureUri);
        } catch (SAXNotRecognizedException ex) {
            XRLog.xmlEntities(Level.WARNING, "SAX feature not recognized on this XMLReader: " + featureUri + ". Feature may be properly named, but not recognized by this parser.");
        }
    }
}
