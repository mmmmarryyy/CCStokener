package net.sf.doolin.util.xml;

import net.sf.doolin.util.StringCodes;
import net.sf.jstring.LocalizableException;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.URL;

/**
 * Utilities for dealing with a Jakarta Commons Digester.
 * 
 * @author Damien Coraboeuf
 */
public class DigesterUtils {

    private static final Logger log = LoggerFactory.getLogger(DigesterUtils.class);

    /**
	 * Creates a non validating digester
	 * 
	 * @return Digester wrapper
	 */
    public static DigesterUtils createNonValidatingDigester() {
        SAXParser parser = XMLUtils.createSAXNonValidatingParser();
        try {
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
        } catch (Exception ex) {
            log.error("Cannot configure the parser for ignoring DTD");
        }
        Digester digester = new Digester(parser);
        SimpleErrorHandler errorHandler = new SimpleErrorHandler();
        digester.setErrorHandler(errorHandler);
        DigesterUtils utils = new DigesterUtils();
        utils.digester = digester;
        utils.parser = parser;
        utils.errorHandler = errorHandler;
        return utils;
    }

    /**
	 * Creates a digester that validates against a schema.
	 * 
	 * @param schemaURL
	 *            URL to the schema.
	 * @return Digester wrapper.
	 */
    public static DigesterUtils createValidatingDigester(URL schemaURL) {
        SAXParser parser = XMLUtils.createSAXValidatingParser(schemaURL);
        Digester digester = new Digester(parser);
        SimpleErrorHandler errorHandler = new SimpleErrorHandler();
        digester.setErrorHandler(errorHandler);
        DigesterUtils utils = new DigesterUtils();
        utils.digester = digester;
        utils.parser = parser;
        utils.errorHandler = errorHandler;
        return utils;
    }

    private Digester digester;

    private SAXParser parser;

    private SimpleErrorHandler errorHandler;

    /**
	 * Protected constructor, used internally
	 */
    protected DigesterUtils() {
    }

    /**
	 * Access to the underlying digester.
	 * 
	 * @return Returns the digester.
	 */
    public Digester getDigester() {
        return this.digester;
    }

    /**
	 * Returns the associated SAX parser
	 * 
	 * @return SAX parser
	 */
    public SAXParser getParser() {
        return this.parser;
    }

    /**
	 * Parses a file
	 * 
	 * @param file
	 *            File to parse
	 * @param initialValue
	 *            Initial value for the digester
	 */
    public void parse(File file, Object initialValue) {
        if (file == null) {
            throw new IllegalArgumentException("The source to the XML must not be null");
        }
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            try {
                InputSource inputSource = new InputSource(in);
                parse(inputSource, initialValue);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new LocalizableException(StringCodes.STRING_XML_IO_ERROR, e, file, e.getLocalizedMessage());
        }
    }

    /**
	 * Parses an input source.
	 * 
	 * @param inputSource
	 *            Source to parse
	 * @param initialValue
	 *            Initial value for the digester
	 */
    public void parse(InputSource inputSource, Object initialValue) {
        if (inputSource == null) {
            throw new IllegalArgumentException("The source to the XML must not be null");
        }
        this.digester.clear();
        if (initialValue != null) {
            this.digester.push(initialValue);
        }
        try {
            this.parser.parse(inputSource, this.digester);
            this.errorHandler.logWarnings(log);
            this.errorHandler.throwAnyError();
        } catch (SAXException e) {
            throw new LocalizableException(StringCodes.STRING_XML_PARSING_ERROR, e, inputSource.getSystemId(), e.getLocalizedMessage());
        } catch (IOException e) {
            throw new LocalizableException(StringCodes.STRING_XML_IO_ERROR, e, inputSource.getSystemId(), e.getLocalizedMessage());
        }
    }

    /**
	 * Parses an XML stream.
	 * 
	 * @param xml
	 *            XML string to parse
	 * @param initialValue
	 *            Initial value for the digester
	 */
    public void parse(String xml, Object initialValue) {
        if (StringUtils.isBlank(xml)) {
            throw new IllegalArgumentException("The source to the XML must not be blank");
        }
        StringReader reader = new StringReader(xml);
        try {
            InputSource inputSource = new InputSource(reader);
            parse(inputSource, initialValue);
        } finally {
            reader.close();
        }
    }

    /**
	 * Parses an URL.
	 * 
	 * @param url
	 *            URL to the XML document
	 * @param initialValue
	 *            Initial value for the digester
	 */
    public void parse(URL url, Object initialValue) {
        if (url == null) {
            throw new IllegalArgumentException("The source to the XML must not be null");
        }
        try {
            InputStream in = url.openStream();
            try {
                InputSource inputSource = new InputSource(in);
                parse(inputSource, initialValue);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new LocalizableException(StringCodes.STRING_XML_IO_ERROR, e, url, e.getLocalizedMessage());
        }
    }

    /**
	 * Creates a simple rule in the digester to create a new class for a node,
	 * setting its properties and calling a method on it after creation.
	 * 
	 * @param node
	 *            Name (or path) of the XML node. Note that a
	 *            <code>*&#x002F;</code> will be prepended.
	 * @param clazz
	 *            Class of the object to be created for this node.
	 * @param method
	 *            Method to call after the node creation. This method is called
	 *            on the next object of the digester's stack. If
	 *            <code>null</code>, no call is performed.
	 */
    public void ruleObject(String node, Class<?> clazz, String method) {
        String path = "*/" + node;
        this.digester.addObjectCreate(path, clazz);
        this.digester.addSetProperties(path);
        if (method != null) {
            this.digester.addSetNext(path, method);
        }
    }

    /**
	 * Creates a simple rule in the digester to setup an item from for a node,
	 * setting its properties.
	 * 
	 * @param node
	 *            Name (or path) of the XML node. Note that a
	 *            <code>*&#x002F;</code> will be prepended.
	 */
    public void ruleProperties(String node) {
        String path = "*/" + node;
        this.digester.addSetProperties(path);
    }

    /**
	 * Creates a rule for creating a property from a leaf node, using the node's
	 * content as a value.
	 * 
	 * @param node
	 *            Name (or path) of the XML node. Note that a
	 *            <code>*&#x002F;</code> will be prepended.
	 * @param propertySetMethod
	 *            Method name to call to set the property
	 */
    public void ruleProperty(String node, String propertySetMethod) {
        String path = "*/" + node;
        this.digester.addCallMethod(path, propertySetMethod, 1);
        this.digester.addCallParam(path, 0);
    }

    /**
	 * Creates a rule for creating a typed property from a leaf node, using the
	 * node's content as a value.
	 * 
	 * @param node
	 *            Name (or path) of the XML node. Note that a
	 *            <code>*&#x002F;</code> will be prepended.
	 * @param propertySetMethod
	 *            Method name to call to set the property
	 * @param type
	 *            Type of the property
	 */
    public void ruleProperty(String node, String propertySetMethod, Class<?> type) {
        String path = "*/" + node;
        this.digester.addCallMethod(path, propertySetMethod, 1, new Class[] { type });
        this.digester.addCallParam(path, 0);
    }
}
