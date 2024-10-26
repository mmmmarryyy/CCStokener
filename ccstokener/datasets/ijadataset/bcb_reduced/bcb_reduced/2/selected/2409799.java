package org.sf.bluprints;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>
 * This is the configuration parser for the Bluprints.
 * </p>
 *
 * @author  Brian Pontarelli
 */
public class ConfigurationParser {

    private static final Logger logger = Logger.getLogger(ConfigurationParser.class.getName());

    /**
     * Parses all of the given XML configuration files for Bluprints.
     *
     * @param   uris The classpath URIs of the bluprints XML configuration files. These all must be
     *          in the classpath.
     * @return  A Map containing all of the Bluprints that were parsed from the configuration files.
     */
    public Map<String, BluprintsNamespace> build(String... uris) {
        Map<String, BluprintsNamespace> bluprints = new HashMap<String, BluprintsNamespace>();
        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i];
            Bluprints bps = buildInternal(uri);
            BluprintsNamespace bn = bluprints.get(bps.getNamespace());
            if (bn == null) {
                bn = new BluprintsNamespace();
                bluprints.put(bps.getNamespace(), bn);
            }
            Set<String> newKeys = new HashSet<String>(bps.getPages().keySet());
            Set<String> existingKeys = new HashSet<String>();
            for (Bluprints bp : bn.getBluprints()) {
                existingKeys.addAll(bp.getPages().keySet());
            }
            newKeys.retainAll(existingKeys);
            if (newKeys.size() > 0) {
                throw new IllegalArgumentException("Bluprint configuration file [" + uri + "] for namespace [" + bps.getNamespace() + "] contains these overlapping bluprints " + newKeys.toString());
            }
            bn.addBluprints(bps);
        }
        Collection<BluprintsNamespace> collection = bluprints.values();
        for (BluprintsNamespace bn : collection) {
            for (Bluprints bps : bn.getBluprints()) {
                Collection<Bluprint> pages = bps.getPages().values();
                for (Bluprint bp : pages) {
                    if (!bp.contentComplete) {
                        fillOutBluprintContent(bluprints, bp);
                    }
                }
            }
        }
        for (BluprintsNamespace bn : collection) {
            for (Bluprints bps : bn.getBluprints()) {
                Collection<Bluprint> pages = bps.getPages().values();
                for (Bluprint bp : pages) {
                    if (!bp.parametersComplete) {
                        fillOutBluprintParameters(bluprints, bp, null);
                    }
                }
            }
        }
        return bluprints;
    }

    private Bluprints buildInternal(String uri) {
        InputStream stream = getInputStream(uri);
        Bluprints bps;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setXIncludeAware(true);
            InputStream is = getClass().getResourceAsStream("/bluprints.xsd");
            if (is != null) {
                Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(is));
                Validator v = schema.newValidator();
                v.validate(new SAXSource(new InputSource(stream)));
                logger.finest("Verified schema");
                stream = getInputStream(uri);
            }
            Handler handler = new Handler();
            SAXParser parser = factory.newSAXParser();
            parser.parse(stream, handler);
            logger.finest("parsed okay");
            bps = handler.bluprints;
        } catch (ParserConfigurationException e) {
            logger.severe("Unable to parse bluprints configuration because a SAX parser was not configured.");
            throw new IllegalArgumentException(e);
        } catch (SAXException e) {
            logger.severe("Unable to parse bluprints configuration because the document contained an error.");
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            logger.severe("Unable to parse bluprints configuration because the document could not be parsed.");
            throw new IllegalArgumentException(e);
        }
        return bps;
    }

    private InputStream getInputStream(String uri) {
        InputStream stream;
        URL url = getClass().getResource(uri);
        if (url != null) {
            try {
                stream = url.openStream();
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException("Invalid Bluprints location [" + uri + "]. Must be in the classpath.");
        }
        return stream;
    }

    private void fillOutBluprintContent(final Map<String, BluprintsNamespace> bluprints, Bluprint bluprint) {
        String extend = bluprint.getExtends();
        if (extend != null) {
            Bluprint parent = findBluprint(bluprints, extend, bluprint.getNamespace());
            if (parent == null) {
                throw new IllegalArgumentException("Invalid extends definition [" + extend + "] on bluprint [" + bluprint.getName() + "]");
            }
            if (!parent.contentComplete) {
                fillOutBluprintContent(bluprints, parent);
            }
            Map<String, List<Bluprint>> content = parent.content;
            Set<String> keys = content.keySet();
            for (String key : keys) {
                if (bluprint.content.get(key) == null) {
                    List<Bluprint> list = content.get(key);
                    List<Bluprint> newList = new ArrayList<Bluprint>();
                    for (Bluprint bp : list) {
                        newList.add(bp.clone());
                    }
                    bluprint.content.put(key, newList);
                }
            }
            while (parent.getExtends() != null) {
                String name = parent.getName();
                parent = findBluprint(bluprints, parent.getExtends(), bluprint.getNamespace());
                if (parent == null) {
                    throw new IllegalArgumentException("Invalid extends definition [" + extend + "] on bluprint [" + name + "]");
                }
            }
            bluprint.setUri(parent.getUri());
        }
        Set<String> keys = bluprint.content.keySet();
        for (String key : keys) {
            List<Bluprint> list = bluprint.content.get(key);
            for (Bluprint bp : list) {
                fillOutBluprintContent(bluprints, bp);
            }
        }
        bluprint.contentComplete = true;
    }

    private void fillOutBluprintParameters(final Map<String, BluprintsNamespace> bluprints, Bluprint bluprint, Bluprint container) {
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();
        if (container != null) {
            parameters.putAll(container.getParametersList());
        }
        String extend = bluprint.getExtends();
        if (extend != null) {
            Bluprint parent = findBluprint(bluprints, extend, bluprint.getNamespace());
            if (!parent.parametersComplete) {
                fillOutBluprintParameters(bluprints, parent, null);
            }
            parameters.putAll(parent.getParametersList());
        }
        parameters.putAll(bluprint.parameters);
        if (parameters.size() > 0) {
            bluprint.parameters.clear();
            bluprint.parameters.putAll(parameters);
        }
        Map<String, Bluprint> content = bluprint.getContent();
        Set<String> keys = content.keySet();
        for (String key : keys) {
            Bluprint child = content.get(key);
            fillOutBluprintParameters(bluprints, child, bluprint);
        }
        bluprint.parametersComplete = true;
    }

    private Bluprint findBluprint(final Map<String, BluprintsNamespace> bluprints, String name, String currentNamespace) {
        String[] parts = Bluprints.split(currentNamespace, name);
        BluprintsNamespace bn = bluprints.get(parts[0]);
        if (bn == null) {
            throw new IllegalArgumentException("Bluprint with name [" + name + "] does not exist");
        }
        return bn.getPage(parts[1]);
    }

    public class Handler extends DefaultHandler {

        private Bluprints bluprints;

        private Bluprint bluprint;

        private boolean errors = false;

        private Stack<Bluprint> pagesWithContent = new Stack<Bluprint>();

        private Stack<List<Bluprint>> pages = new Stack<List<Bluprint>>();

        private int depth = 0;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("bluprints")) {
                bluprints = new Bluprints();
                String ns = attributes.getValue("namespace");
                bluprints.setNamespace(ns != null ? ns : Bluprints.DEFAULT_NAMESPACE);
                pages.push(new ArrayList<Bluprint>());
            } else if (qName.equals("content")) {
                pages.push(new ArrayList<Bluprint>());
                pagesWithContent.push(bluprint);
            } else if (qName.equals("bluprint")) {
                bluprint = new Bluprint();
                bluprint.setName(attributes.getValue("name"));
                bluprint.setUri(attributes.getValue("uri"));
                bluprint.setExtends(attributes.getValue("extends"));
                bluprint.setNamespace(bluprints.getNamespace());
                boolean uriGiven = (bluprint.getUri() != null);
                boolean extendsGiven = (bluprint.getExtends() != null);
                if (!(uriGiven ^ extendsGiven)) {
                    throw new IllegalArgumentException("Either a uri or extends attribute must " + "be supplied for all bluprint definitions but only one of them. [" + bluprint.getName() + "] is missing one or has many.");
                }
                depth++;
            } else if (qName.equals("parameter")) {
                String name = attributes.getValue("name");
                String value = attributes.getValue("value");
                List<String> parameters = bluprint.parameters.get(name);
                if (parameters == null) {
                    parameters = new ArrayList<String>();
                    bluprint.parameters.put(name, parameters);
                }
                parameters.add(value);
            } else {
                throw new SAXException("Invalid element name.");
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("bluprints")) {
                bluprints.setPages(pages.pop());
            } else if (qName.equals("content")) {
                bluprint = pagesWithContent.pop();
                bluprint.setContent(pages.pop());
            } else if (qName.equals("bluprint")) {
                if (depth == 1) {
                    List<Bluprint> bps = pages.peek();
                    for (Bluprint bp : bps) {
                        if (bp.getName().equals(bluprint.getName())) {
                            throw new SAXException("There are two bluprint definitions at the same level " + "with the name [" + bluprint.getName() + "]");
                        }
                    }
                }
                pages.peek().add(bluprint);
                depth--;
            }
        }

        public void endDocument() throws SAXException {
            if (errors) {
                throw new SAXException("Errors in XML document. Turn on JDK logging to see the errors.");
            }
            super.endDocument();
        }

        public void warning(SAXParseException e) throws SAXException {
            logger.severe("Warnings in XML document at [line " + e.getLineNumber() + " column " + e.getColumnNumber() + "] - [" + e.getMessage() + "]");
            new Exception().printStackTrace();
            errors = true;
        }

        public void error(SAXParseException e) throws SAXException {
            logger.severe("Error in XML document at [line " + e.getLineNumber() + " column " + e.getColumnNumber() + "] - [" + e.getMessage() + "]");
            new Exception().printStackTrace();
            errors = true;
        }

        public void fatalError(SAXParseException e) throws SAXException {
            logger.severe("FATAL Error in XML document at [line " + e.getLineNumber() + " column " + e.getColumnNumber() + "] - [" + e.getMessage() + "]");
            new Exception().printStackTrace();
            errors = true;
        }
    }
}
