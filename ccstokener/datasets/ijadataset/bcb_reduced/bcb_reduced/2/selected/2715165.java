package com.xenonsoft.bridgetown.resources;

import java.io.IOException;
import java.net.URL;
import org.apache.commons.digester.Digester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import com.xenonsoft.bridgetown.soa.AssemblyConfigException;
import com.xenonsoft.bridgetown.soa.config.ArgumentConfig;
import com.xenonsoft.bridgetown.soa.config.AssemblyConfig;
import com.xenonsoft.bridgetown.soa.config.ContextConfig;
import com.xenonsoft.bridgetown.soa.config.EntryConfig;
import com.xenonsoft.bridgetown.soa.config.InvokeMethodConfig;
import com.xenonsoft.bridgetown.soa.config.JoinpointConfig;
import com.xenonsoft.bridgetown.soa.config.JoinpointsConfig;
import com.xenonsoft.bridgetown.soa.config.ListConfig;
import com.xenonsoft.bridgetown.soa.config.MapConfig;
import com.xenonsoft.bridgetown.soa.config.MethodConfig;
import com.xenonsoft.bridgetown.soa.config.ParameterConfig;
import com.xenonsoft.bridgetown.soa.config.PointcutConfig;
import com.xenonsoft.bridgetown.soa.config.PropertyConfig;
import com.xenonsoft.bridgetown.soa.config.ServiceConfig;
import com.xenonsoft.bridgetown.soa.config.ServicesConfig;
import com.xenonsoft.bridgetown.soa.config.TransactionConfig;
import com.xenonsoft.bridgetown.soa.config.ValueReferenceConfig;

/**
 * This is legacy sample XML configuration loader that uses the 
 * Commons Digester to parse an XML file.
 * 
 * <p>
 * 
 * This class is useful to study the original implementation
 * and offer a full rule of how the various RuleSet fit together
 * to parse the schema.
 * 
 * <p>
 * 
 * <ul>
 * 
 * <li>This class cannot support anonymous data structure to
 * level greater than one, because it is does implement rule sets.
 * </li>
 * 
 * <li>The point cut digester rule reveal the verbosity and
 * duplication.
 * </li>
 * 
 * <li>The digester rules do not take advantage of the
 * object hierarchy
 * </li>
 *  
 * </ul>
 * @author Peter Pilgrim, 04-Aug-2004
 * @version $Id: LegacyXMLConfigLoader.java,v 1.2 2005/02/23 01:31:14 peter_pilgrim Exp $
 */
public class LegacyXMLConfigLoader extends AbstractConfigLoader {

    /** Static logger */
    private static final Log logger = LogFactory.getLog(LegacyXMLConfigLoader.class);

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
     * Default constructor, cannot be instantiated
     */
    private LegacyXMLConfigLoader() {
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
                throw new AssemblyConfigException("Unable to parse the XML assembly configuration");
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
        try {
            for (int i = 0; i < registrations.length; i += 2) {
                URL url = this.getClass().getResource(registrations[i + 1]);
                if (url != null) {
                    digester.register(registrations[i], url.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        digester.addObjectCreate("assembly", AssemblyConfig.class);
        digester.addSetProperties("assembly", "description", "description");
        digester.addObjectCreate("assembly/context", ContextConfig.class);
        digester.addSetProperties("assembly/context", "name", "name");
        digester.addSetProperties("assembly/context", "description", "description");
        digester.addObjectCreate("assembly/context/services", ServicesConfig.class);
        digester.addObjectCreate("assembly/context/services/service", ServiceConfig.class);
        digester.addSetProperties("assembly/context/services/service", "id", "id");
        digester.addSetProperties("assembly/context/services/service", "description", "description");
        digester.addSetProperties("assembly/context/services/service", "interface", "interfaceClass");
        digester.addSetProperties("assembly/context/services/service", "singleton", "singletonText");
        digester.addSetProperties("assembly/context/services/service", "lazyLoad", "lazyLoadText");
        digester.addCallMethod("assembly/context/services/service/interface", "addInterfaceClass", 1);
        digester.addCallParam("assembly/context/services/service/interface", 0);
        digester.addSetProperties("assembly/context/services/service", "type", "impl");
        digester.addBeanPropertySetter("assembly/context/services/service/impl", "impl");
        digester.addObjectCreate("assembly/context/services/service/set-property", PropertyConfig.class);
        digester.addSetProperties("assembly/context/services/service/set-property", "name", "name");
        digester.addSetProperties("assembly/context/services/service/set-property", "value", "value");
        digester.addBeanPropertySetter("assembly/context/services/service/set-property/value", "value");
        digester.addObjectCreate("assembly/context/services/service/set-property/list", ListConfig.class);
        digester.addCallMethod("assembly/context/services/service/set-property/list/value", "addValue", 1);
        digester.addCallParam("assembly/context/services/service/set-property/list/value", 0);
        digester.addObjectCreate("assembly/context/services/service/set-property/list/ref", ValueReferenceConfig.class);
        digester.addSetProperties("assembly/context/services/service/set-property/list/ref", "service", "refService");
        digester.addSetNext("assembly/context/services/service/set-property/list/ref", "addValueRef");
        digester.addSetNext("assembly/context/services/service/set-property/list", "setValueFromListConfig");
        digester.addObjectCreate("assembly/context/services/service/set-property/map", MapConfig.class);
        digester.addObjectCreate("assembly/context/services/service/set-property/map/entry", EntryConfig.class);
        digester.addSetProperties("assembly/context/services/service/set-property/map/entry", "key", "key");
        digester.addSetProperties("assembly/context/services/service/set-property/map/value", "value", "value");
        digester.addBeanPropertySetter("assembly/context/services/service/set-property/map/entry/value", "value");
        digester.addSetNext("assembly/context/services/service/set-property/map/entry", "addEntry");
        digester.addObjectCreate("assembly/context/services/service/set-property/map/entry/ref", ValueReferenceConfig.class);
        digester.addSetProperties("assembly/context/services/service/set-property/map/entry/ref", "service", "refService");
        digester.addSetProperties("assembly/context/services/service/set-property/map/entry/ref", "context", "refContext");
        digester.addSetNext("assembly/context/services/service/set-property/map/entry/ref", "setValueRef");
        digester.addSetNext("assembly/context/services/service/set-property/map", "setValueFromMapConfig");
        digester.addObjectCreate("assembly/context/services/service/set-property/ref", ValueReferenceConfig.class);
        digester.addSetProperties("assembly/context/services/service/set-property/ref", "service", "refService");
        digester.addSetProperties("assembly/context/services/service/set-property/ref", "context", "refContext");
        digester.addSetNext("assembly/context/services/service/set-property/ref", "setValueRef");
        digester.addSetNext("assembly/context/services/service/set-property", "addProperty");
        digester.addObjectCreate("assembly/context/services/service/constructor", MethodConfig.class);
        digester.addObjectCreate("assembly/context/services/service/constructor/arg", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/constructor/arg", "index", "index");
        digester.addSetProperties("assembly/context/services/service/constructor/arg", "type", "type");
        digester.addSetProperties("assembly/context/services/service/constructor/arg", "value", "value");
        digester.addBeanPropertySetter("assembly/context/services/service/constructor/arg/value", "value");
        digester.addSetNext("assembly/context/services/service/constructor/arg", "addArgument");
        digester.addObjectCreate("assembly/context/services/service/constructor/ref", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/constructor/ref", "service", "refService");
        digester.addSetProperties("assembly/context/services/service/constructor/ref", "context", "refContext");
        digester.addSetNext("assembly/context/services/service/constructor/ref", "addArgument");
        digester.addSetNext("assembly/context/services/service/constructor", "setConstructor");
        digester.addObjectCreate("assembly/context/services/service/destroy-method", MethodConfig.class);
        digester.addSetProperties("assembly/context/services/service/destroy-method", "name", "name");
        digester.addObjectCreate("assembly/context/services/service/destroy-method/arg", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/destroy-method/arg", "index", "index");
        digester.addSetProperties("assembly/context/services/service/destroy-method/arg", "type", "type");
        digester.addSetProperties("assembly/context/services/service/destroy-method/arg", "value", "value");
        digester.addBeanPropertySetter("assembly/context/services/service/destroy-method/arg/value", "value");
        digester.addSetNext("assembly/context/services/service/destroy-method/arg", "addArgument");
        digester.addObjectCreate("assembly/context/services/service/destroy-method/ref", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/destroy-method/ref", "service", "refService");
        digester.addSetNext("assembly/context/services/service/destroy-method/ref", "addArgument");
        digester.addSetNext("assembly/context/services/service/destroy-method", "setDestroyMethod");
        digester.addObjectCreate("assembly/context/services/service/method", MethodConfig.class);
        digester.addSetProperties("assembly/context/services/service/method", "name", "name");
        digester.addObjectCreate("assembly/context/services/service/method/arg", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/method/arg", "index", "index");
        digester.addSetProperties("assembly/context/services/service/method/arg", "type", "type");
        digester.addSetProperties("assembly/context/services/service/method/arg", "value", "value");
        digester.addBeanPropertySetter("assembly/context/services/service/method/arg/value", "value");
        digester.addSetNext("assembly/context/services/service/method/arg", "addArgument");
        digester.addObjectCreate("assembly/context/services/service/method/ref", ArgumentConfig.class);
        digester.addSetProperties("assembly/context/services/service/method/ref", "service", "refService");
        digester.addSetNext("assembly/context/services/service/method/ref", "addArgument");
        digester.addSetNext("assembly/context/services/service/method", "addMethod");
        digester.addSetNext("assembly/context/services/service", "addService");
        digester.addSetNext("assembly/context/services", "addServices");
        digester.addObjectCreate("assembly/context/pointcuts/pointcut", PointcutConfig.class);
        digester.addSetProperties("assembly/context/pointcuts/pointcut", "name", "name");
        digester.addSetProperties("assembly/context/pointcuts/pointcut", "description", "description");
        digester.addObjectCreate("assembly/context/pointcuts/pointcut/joinpoints", JoinpointsConfig.class);
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints", "default-service-finder", "defaultServiceBean");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints", "default-class-finder", "defaultClassFinderExpr");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints", "default-method-finder", "defaultMethodFinderExpr");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints", "default-type", "defaultType");
        digester.addObjectCreate("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", JoinpointConfig.class);
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", "type", "type");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", "service-finder", "serviceBean");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", "class-finder", "classFinderExpr");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", "method-finder", "methodFinderExpr");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/joinpoints/joinpoint/description", "description");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/joinpoints/joinpoint/service-finder", "serviceBean");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/joinpoints/joinpoint/class-finder", "classFinderExpr");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/joinpoints/joinpoint/method-finder", "methodFinderExpr");
        digester.addSetNext("assembly/context/pointcuts/pointcut/joinpoints/joinpoint", "addJoinpoint");
        digester.addSetNext("assembly/context/pointcuts/pointcut/joinpoints", "setJoinpointsConfig");
        digester.addObjectCreate("assembly/context/pointcuts/pointcut/transaction", TransactionConfig.class);
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/transaction/trans-attribute", "demarcationFromText");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/transaction/isolation-level", "isolationLevelFromText");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/transaction/read-only", "readOnlyFromText");
        digester.addBeanPropertySetter("assembly/context/pointcuts/pointcut/transaction/trans-timeout", "transactionTimeout");
        digester.addSetNext("assembly/context/pointcuts/pointcut/transaction", "setTransactionConfig");
        digester.addObjectCreate("assembly/context/pointcuts/pointcut/invoke", InvokeMethodConfig.class);
        digester.addSetProperties("assembly/context/pointcuts/pointcut/invoke", "context", "context");
        digester.addSetProperties("assembly/context/pointcuts/pointcut/invoke", "service", "serviceBean");
        digester.addSetNext("assembly/context/pointcuts/pointcut/invoke", "setInvokeMethod");
        digester.addSetNext("assembly/context/pointcuts/pointcut", "addPointcut");
        digester.addSetNext("assembly/context", "addContext");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut", PointcutConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut", "name", "name");
        digester.addSetProperties("assembly/global-pointcuts/pointcut", "description", "description");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/joinpoints", JoinpointsConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints", "default-service-finder", "defaultServiceBean");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints", "default-class-finder", "defaultClassFinderExpr");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints", "default-method-finder", "defaultMethodFinderExpr");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints", "default-type", "defaultType");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", JoinpointConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", "type", "type");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", "service-finder", "serviceBean");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", "class-finder", "classFinderExpr");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", "method-finder", "methodFinderExpr");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/joinpoints/joinpoint/description", "description");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/joinpoints/joinpoint/service-finder", "serviceBean");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/joinpoints/joinpoint/class-finder", "classFinderExpr");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/joinpoints/joinpoint/method-finder", "methodFinderExpr");
        digester.addSetNext("assembly/global-pointcuts/pointcut/joinpoints/joinpoint", "addJoinpoint");
        digester.addSetNext("assembly/global-pointcuts/pointcut/joinpoints", "setJoinpointsConfig");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/transaction", TransactionConfig.class);
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/transaction/trans-attribute", "demarcationFromText");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/transaction/isolation-level", "isolationLevelFromText");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/transaction/read-only", "readOnlyFromText");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/transaction/trans-timeout", "transactionTimeout");
        digester.addSetNext("assembly/global-pointcuts/pointcut/transaction", "setTransactionConfig");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/parameter", ParameterConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut/parameter", "name", "name");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/parameter", "value", "value");
        digester.addBeanPropertySetter("assembly/global-pointcuts/pointcut/parameter/value", "value");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/parameter/ref", ValueReferenceConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut/parameter/ref", "service", "refService");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/parameter/ref", "context", "refContext");
        digester.addSetNext("assembly/global-pointcuts/pointcut/parameter/ref", "setValueRef");
        digester.addSetNext("assembly/global-pointcuts/pointcut/parameter", "addParameter");
        digester.addObjectCreate("assembly/global-pointcuts/pointcut/invoke", InvokeMethodConfig.class);
        digester.addSetProperties("assembly/global-pointcuts/pointcut/invoke", "context", "context");
        digester.addSetProperties("assembly/global-pointcuts/pointcut/invoke", "service", "serviceBean");
        digester.addSetNext("assembly/global-pointcuts/pointcut/invoke", "setInvokeMethod");
        digester.addSetNext("assembly/global-pointcuts/pointcut", "addPointcut");
        return digester;
    }
}
