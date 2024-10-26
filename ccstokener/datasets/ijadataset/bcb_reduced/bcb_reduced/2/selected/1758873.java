package com.xenonsoft.bridgetown.resources;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import com.xenonsoft.bridgetown.soa.AssemblyConfigException;
import com.xenonsoft.bridgetown.soa.InfrastructureException;
import com.xenonsoft.bridgetown.soa.config.AssemblyConfig;

/**
 * Default sample XML configuration loader that uses the Commons Digester
 * to parse an XML file.
 *  
 * @author Peter Pilgrim, 04-Aug-2004
 * @version $Id: XMLConfigLoader.java,v 1.9 2005/04/20 02:54:26 peter_pilgrim Exp $
 */
public class XMLConfigLoader extends AbstractConfigLoader {

    /** Static logger */
    private static final Log logger = LogFactory.getLog(XMLConfigLoader.class);

    /**
     * <p>The set of public identifiers, and corresponding resource names, for
     * the versions of the configuration file DTDs that we know about.  There
     * <strong>MUST</strong> be an even number of Strings in this list!</p>
     */
    protected static final String registrations[] = { "-//XeNoNSoFT.com Open Source//DTD Bridgetown IoC Configuration//EN", "/com/xenonsoft/bridgetown/service-assembly-1.0.dtd" };

    /**
     * boolean flag if the XML parser should be validating
     */
    protected boolean validating = true;

    /**
     * boolean flag if the XML parser should be produce debuggable output
     */
    protected boolean debug = false;

    /**
     * boolean flag if the XML parser should be XML namespace aware or not
     */
    protected boolean namespaceAware = true;

    /**
     * Default constructor
     */
    public XMLConfigLoader() {
        super();
    }

    /**
     * Gets the debug output flag for the Commons Digester parser 
     * @return Returns the debug.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Sets the debug output flag for the Commons Digester parser 
     * @param debug the new value for debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Gets the XML validating flag for the Commons Digester parser 
     * @return Returns the validating.
     */
    public boolean isValidating() {
        return validating;
    }

    /**
     * Sets the XML validating flag for the Commons Digester parser 
     * @param validating the new value for validating
     */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    /**
     * Gets boolean flag if the XML parser should be XML namespace aware or not 
     * @return Returns the namespaceAware.
     */
    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    /**
     * Sets boolean flag if the XML parser should be XML namespace aware or not
     * @param namespaceAware the new value for namespaceAware
     */
    public void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    /** 
     * 
     * Loads the service assembly configuration from the supplied resource.
     * 
     * <ol>
     * 
     * <li>
     * The implementation checks for existance of an <code>InputStream</code>,
     * if it is found, and the stream is opened and available,
     * then method uses the stream.
     * </li>
     * 
     * <li>
     * If the <code>File</code> exists, then the method loads the 
     * configuration from the system file.
     * </li>
     * 
     * <li>
     * Finally the implementation tries to load the configuration
     * URI path. It constructs an URL and attempts to retrieve
     * stream from the resource.
     * </li>
     * 
     * </ol>
     * 
     * <p>
     * <font color="#FF0000"><strong>CAUTION:</strong></font>
     * THIS CLASS MIGHT BE REFACTORED or REDESIGNED along with the superclasses.
     * 
     * @return Assembly configuration root
     * 
     * @throws AssemblyConfigException if the configuration fails to load
     * @see com.xenonsoft.bridgetown.resources.IConfigLoader#load()
     */
    public AssemblyConfig load() {
        AssemblyConfig assembly = null;
        Digester digester = createParser();
        try {
            if (inputStream != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("XML config loader is parsing an inputstream:" + inputStream);
                }
                assembly = (AssemblyConfig) digester.parse(inputStream);
            } else if (file != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("XML config loader is parsing a file:" + file);
                }
                assembly = (AssemblyConfig) digester.parse(file);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("XML config loader is parsing a URI:" + uri);
                }
                URL url = new URL(uri);
                inputStream = url.openStream();
                assembly = (AssemblyConfig) digester.parse(inputStream);
            }
            if (assembly != null) {
                logger.debug("Services from XML configuration are: ");
                logger.debug(assembly.toString());
            } else {
                throw new AssemblyConfigException("Unable to parse the XML assembly configuration. " + "HINT: Please check the schema/grammar of the supplied " + "XML document and verify the XML namespace is correct.");
            }
        } catch (IOException ioe) {
            throw new AssemblyConfigException("I/O failure, unable to process configuration", ioe);
        } catch (SAXException sxe) {
            throw new AssemblyConfigException("XML Reader failure, unable to process configuration", sxe);
        }
        return assembly;
    }

    /**
     * Creates the digester parse with the rules all set up.
     * @return the Common Digester parser ready to parse and load a configuration
     */
    protected Digester createParser() {
        Digester digester = new Digester();
        digester.setValidating(isValidating());
        digester.setValidating(isDebug());
        digester.setNamespaceAware(isNamespaceAware());
        try {
            for (int i = 0; i < registrations.length; i += 2) {
                URL url = this.getClass().getResource(registrations[i + 1]);
                if (url != null) {
                    digester.register(registrations[i], url.toString());
                }
            }
        } catch (Exception e) {
            throw new InfrastructureException("Failed to register bridgetwon framework URL registration with " + "Commons Digester with this configuration loader :" + this);
        }
        digester.addRuleSet(new CoreRuleSet());
        digester.addRuleSet(new ListConfigRuleSet());
        digester.addRuleSet(new SetConfigRuleSet());
        digester.addRuleSet(new MapConfigRuleSet());
        digester.addRuleSet(new PropertiesCollectionConfigRuleSet());
        digester.addRuleSet(new MixinRuleSet());
        digester.addRuleSet(new PointcutRuleSet());
        return digester;
    }
}
