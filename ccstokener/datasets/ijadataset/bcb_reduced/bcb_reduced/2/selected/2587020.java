package com.sun.xacml.support.finder;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicySet;
import com.sun.xacml.finder.PolicyFinder;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class is provided as a utility for reading policies from common,
 * simple sources: <code>InputStream</code>s, <code>File</code>s, and
 * <code>URL</code>s. It can optionally schema validate the policies.
 * <p>
 * Note: some of this functionality was previously provided in
 * <code>com.sun.xacml.finder.impl.FilePolicyModule</code>, but as of
 * the 2.0 release, that class has been removed. This new
 * <code>PolicyReader</code> class provides much better functionality
 * for loading policies.
 *
 * @since 2.0
 * @author Seth Proctor
 */
public class PolicyReader implements ErrorHandler {

    /**
     * The property which is used to specify the schema file to validate
     * against (if any). Note that this isn't used directly by
     * <code>PolicyReader</code>, but is referenced by many classes that
     * use this class to load policies.
     */
    public static final String POLICY_SCHEMA_PROPERTY = "com.sun.xacml.PolicySchema";

    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private PolicyFinder finder;

    private DocumentBuilder builder;

    private Logger logger;

    /**
     * Creates a <code>PolicyReader</code> that does not schema-validate
     * policies.
     *
     * @param finder a <code>PolicyFinder</code> that is used by policy sets,
     *               which may be null only if no references are used
     * @param logger a <code>Logger</code> used to report parsing errors
     */
    public PolicyReader(PolicyFinder finder, Logger logger) {
        this(finder, logger, null);
    }

    /**
     * Creates a <code>PolicyReader</code> that may schema-validate policies.
     *
     * @param finder a <code>PolicyFinder</code> that is used by policy sets,
     *               which may be null only if no references are used
     * @param logger a <code>Logger</code> used to report parsing errors
     * @param schemaFile the schema file used to validate policies, or
     *                   null if schema validation is not desired
     */
    public PolicyReader(PolicyFinder finder, Logger logger, File schemaFile) {
        this.logger = logger;
        this.finder = finder;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        if (schemaFile == null) {
            factory.setValidating(false);
        } else {
            factory.setValidating(true);
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            factory.setAttribute(JAXP_SCHEMA_SOURCE, schemaFile);
        }
        try {
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(this);
        } catch (ParserConfigurationException pce) {
            throw new IllegalArgumentException("Filed to setup reader: " + pce.toString());
        }
    }

    /**
     * Tries to read an XACML policy or policy set from the given file.
     *
     * @param file the file containing the policy to read
     *
     * @return a (potentially schema-validated) policy loaded from the
     *         given file
     *
     * @throws ParsingException if an error occurs while reading or
     *                          parsing the policy
     */
    public synchronized AbstractPolicy readPolicy(File file) throws ParsingException {
        try {
            return handleDocument(builder.parse(file));
        } catch (IOException ioe) {
            throw new ParsingException("Failed to read the file", ioe);
        } catch (SAXException saxe) {
            throw new ParsingException("Failed to parse the file", saxe);
        }
    }

    /**
     * Tries to read an XACML policy or policy set from the given stream.
     *
     * @param input the stream containing the policy to read
     *
     * @return a (potentially schema-validated) policy loaded from the
     *         given file
     *
     * @throws ParsingException if an error occurs while reading or
     *                          parsing the policy
     */
    public synchronized AbstractPolicy readPolicy(InputStream input) throws ParsingException {
        try {
            return handleDocument(builder.parse(input));
        } catch (IOException ioe) {
            throw new ParsingException("Failed to read the stream", ioe);
        } catch (SAXException saxe) {
            throw new ParsingException("Failed to parse the stream", saxe);
        }
    }

    /**
     * Tries to read an XACML policy or policy set based on the given URL.
     * This may be any resolvable URL, like a file or http pointer.
     *
     * @param url a URL pointing to the policy to read
     *
     * @return a (potentially schema-validated) policy loaded from the
     *         given file
     *
     * @throws ParsingException if an error occurs while reading or
     *                          parsing the policy, or if the URL can't
     *                          be resolved
     */
    public synchronized AbstractPolicy readPolicy(URL url) throws ParsingException {
        try {
            return readPolicy(url.openStream());
        } catch (IOException ioe) {
            throw new ParsingException("Failed to resolve the URL: " + url.toString(), ioe);
        }
    }

    /**
     * A private method that handles reading the policy and creates the
     * correct kind of AbstractPolicy.
     */
    private AbstractPolicy handleDocument(Document doc) throws ParsingException {
        Element root = doc.getDocumentElement();
        String name = root.getTagName();
        if (name.equals("Policy")) {
            return Policy.getInstance(root);
        } else if (name.equals("PolicySet")) {
            return PolicySet.getInstance(root, finder);
        } else {
            throw new ParsingException("Unknown root document type: " + name);
        }
    }

    /**
     * Standard handler routine for the XML parsing.
     *
     * @param exception information on what caused the problem
     */
    public void warning(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING)) logger.warning("Warning on line " + exception.getLineNumber() + ": " + exception.getMessage());
    }

    /**
     * Standard handler routine for the XML parsing.
     *
     * @param exception information on what caused the problem
     *
     * @throws SAXException always to halt parsing on errors
     */
    public void error(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING)) logger.warning("Error on line " + exception.getLineNumber() + ": " + exception.getMessage() + " ... " + "Policy will not be available");
        throw new SAXException("error parsing policy");
    }

    /**
     * Standard handler routine for the XML parsing.
     *
     * @param exception information on what caused the problem
     *
     * @throws SAXException always to halt parsing on errors
     */
    public void fatalError(SAXParseException exception) throws SAXException {
        if (logger.isLoggable(Level.WARNING)) logger.warning("Fatal error on line " + exception.getLineNumber() + ": " + exception.getMessage() + " ... " + "Policy will not be available");
        throw new SAXException("fatal error parsing policy");
    }
}
