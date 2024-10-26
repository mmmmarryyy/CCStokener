package org.osgi.util.xml;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.xpath.*;
import org.osgi.framework.*;

/**
 * A BundleActivator class that allows any JAXP compliant XML Transformer
 * to register itself as an OSGi parser service.
 *
 * Multiple JAXP compliant transformers can concurrently register by
 * using this BundleActivator class. Bundles who wish to use an XML
 * transformer can then use the framework's service registry to locate
 * available XML transformers with the desired characteristics.
 *
 * <p>
 * The services that this bundle activator enables a bundle to provide are:
 * <ul>
 * <li><code>javax.xml.transform.TransformerFactory</code>
 * <li><code>javax.xml.xpath.XPathFactory</code>
 * </ul>
 *
 * <p>
 * The algorithm to find the implementations of the abstract
 * transformer is derived from the JAR file specifications,
 * specifically the Services API.
 * <p>
 * A TransformerFactory creator assumes that it can find the class
 * file names of available factory classes in the file:
 * <ul>
 * <li><code>/META-INF/services/javax.xml.transform.TransformerFactory</code>
 * inside the JAR file that provides the class.
 * </ul>
 * <p>
 * <p>
 * An XPathFactory creator assumes that it can find the class
 * file names of available factory classes in the file:
 * <ul>
 * <li><code>/META-INF/services/javax.xml.xpath.XPathFactory</code>
 * inside the JAR file that provides the class.
 * </ul>
 * <p>
 *
 * <p>
 * <code>XMLTransformerActivator</code> attempts to instantiate both the
 * <code>TransformerFactory</code> and the <code>XPathFactory</code>.
 * It registers each factory with the framework.
 */
public class XMLTransformerActivator implements BundleActivator, ServiceFactory {

    /** Context of this bundle */
    private BundleContext context;

    /**
   * Filename containing the TransformerFactory class name. Also used as the
   * basis for the <code>SERVICE_PID<code> registration property.
   */
    public static final String TRANSFORMERFACTORYNAME = "javax.xml.transform.TransformerFactory";

    public static final String TRANSFORMERFACOTRYDESCR = "XML Transformer factory";

    /**
   * Filename containing the XPathFactory class name. Also used as the
   * basis for the <code>SERVICE_PID</code> registration property.
   */
    public static final String XPATHFACTORYNAME = "javax.xml.xpath.XPathFactory";

    public static final String XPATHFACOTRYDESCR = "XPath Factory";

    /** Path to the factory class name files */
    private static final String SERVICESFILEPATH = "/META-INF/services/";

    /** Fully qualified path name of TransofrmerFactory Class Name file */
    public static final String TRANSFORMERCLASSFILE = SERVICESFILEPATH + TRANSFORMERFACTORYNAME;

    /** Fully qualified path name of the XPath Factory Class Name file */
    public static final String XPATHCLASSFILE = SERVICESFILEPATH + XPATHFACTORYNAME;

    /**
   * Key for factory class name property - this must be saved in
   * the service registration properties hashtable so that the
   * factory can be instantiated from a ServiceReference
   */
    private static final String FACTORYNAMEKEY = "xalan.factoryname";

    /**
   * Called when this bundle is started so the Framework can perform the
   * bundle-specific activities necessary to start this bundle. This method
   * can be used to register services or to allocate any resources that this
   * bundle needs.
   *
   * <p>
   * This method must complete and return to its caller in a timely manner.
   *
   * <p>
   * This method attempts to register a SAX and DOM parser with the
   * Framework's service registry.
   *
   * @param context The execution context of the bundle being started.
   * @throws java.lang.Exception If this method throws an exception, this
   *         bundle is marked as stopped and the Framework will remove this
   *         bundle's listeners, unregister all services registered by this
   *         bundle, and release all services used by this bundle.
   * @see Bundle#start
   */
    public void start(BundleContext context) throws Exception {
        this.context = context;
        Bundle bundle = context.getBundle();
        try {
            registerTransformers(getFactoryClassNames(bundle.getResource(TRANSFORMERCLASSFILE)));
            registerXPath(getFactoryClassNames(bundle.getResource(XPATHCLASSFILE)));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new TransformerFactoryConfigurationError(ioe);
        }
    }

    /**
   * <p>
   * This method has nothing to do as all active service registrations will
   * automatically get unregistered when the bundle stops.
   *
   * @param context The execution context of the bundle being stopped.
   * @throws java.lang.Exception If this method throws an exception, the
   *         bundle is still marked as stopped, and the Framework will remove
   *         the bundle's listeners, unregister all services registered by the
   *         bundle, and release all services used by the bundle.
   * @see Bundle#stop
   */
    public void stop(BundleContext context) throws Exception {
    }

    /**
   * Given the URL for a file, reads and returns the factory class
   * names. There may be multiple classes specified in this file, one
   * per line. There may also be comment lines in the file, which
   * begin with "#".
   *
   * @param url The URL of the service file containing the class names.
   *        names.
   * @return A vector of strings containing the factory class names or
   *         null if url is null.
   * @throws IOException if there is a problem reading the URL input stream.
   */
    private Vector getFactoryClassNames(URL url) throws IOException {
        Vector v = new Vector(1);
        if (url != null) {
            String transformerFactoryClassName = null;
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (true) {
                transformerFactoryClassName = br.readLine();
                if (transformerFactoryClassName == null) {
                    break;
                }
                String tfcName = transformerFactoryClassName.trim();
                if (tfcName.length() == 0) {
                    continue;
                }
                int commentIdx = tfcName.indexOf("#");
                if (commentIdx == 0) {
                    continue;
                } else {
                    if (commentIdx < 0) {
                        v.addElement(tfcName);
                    } else {
                        v.addElement(tfcName.substring(0, commentIdx).trim());
                    }
                }
            }
            return v;
        } else {
            return null;
        }
    }

    /**
   * Register Transformer Factory Services with the framework.
   *
   * @param factoryClassNames - a <code>Vector</code> of
   *        <code>String</code> objects containing the names of the
   *        transformer Factory Classes
   * @throws TransformerFactoryConfigurationError if thrown from <code>getFactory</code>
   */
    private void registerTransformers(Vector factoryClassNames) throws TransformerFactoryConfigurationError {
        if (factoryClassNames != null) {
            Enumeration e = factoryClassNames.elements();
            int index = 0;
            while (e.hasMoreElements()) {
                String transformerFactoryClassName = (String) e.nextElement();
                Object obj = getFactory(transformerFactoryClassName);
                if (obj instanceof TransformerFactory) {
                    TransformerFactory factory = (TransformerFactory) obj;
                    Hashtable properties = new Hashtable(7);
                    properties.put(Constants.SERVICE_DESCRIPTION, TRANSFORMERFACOTRYDESCR);
                    properties.put(Constants.SERVICE_PID, TRANSFORMERFACTORYNAME + "." + context.getBundle().getBundleId() + "." + index);
                    properties.put(FACTORYNAMEKEY, transformerFactoryClassName);
                    factory = null;
                    context.registerService(TRANSFORMERFACTORYNAME, this, properties);
                    index++;
                }
            }
        }
    }

    /**
   * Register XPath Factory Services with the framework.
   *
   * @param factoryClassNames - a <code>Vector</code> of
   *        <code>String</code> objects containing the names of the
   *        XPath Factory Classes
   * @throws TransformerFactoryConfigurationError if thrown from <code>getFactory</code>
   */
    private void registerXPath(Vector factoryClassNames) throws TransformerFactoryConfigurationError {
        if (factoryClassNames != null) {
            Enumeration e = factoryClassNames.elements();
            int index = 0;
            while (e.hasMoreElements()) {
                String xPathFactoryClassName = (String) e.nextElement();
                Object obj = getFactory(xPathFactoryClassName);
                if (obj instanceof XPathFactory) {
                    XPathFactory factory = (XPathFactory) obj;
                    Hashtable properties = new Hashtable(7);
                    properties.put(Constants.SERVICE_DESCRIPTION, XPATHFACOTRYDESCR);
                    properties.put(Constants.SERVICE_PID, XPATHFACTORYNAME + "." + context.getBundle().getBundleId() + "." + index);
                    properties.put(FACTORYNAMEKEY, xPathFactoryClassName);
                    factory = null;
                    context.registerService(XPATHFACTORYNAME, this, properties);
                    index++;
                }
            }
        }
    }

    /**
   * Given a factory class name, instantiate that class.
   *
   * @param factoryClassName A <code>String</code> object containing
   *        the name of the factory class
   * @return a factoryClass Object
   * @pre factoryClassName!=null
   */
    private Object getFactory(String factoryClassName) throws TransformerFactoryConfigurationError {
        Exception e = null;
        try {
            Class factoryClazz = Class.forName(factoryClassName);
            Object res = factoryClazz.newInstance();
            return res;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InstantiationException ie) {
            e = ie;
        } catch (IllegalAccessException iae) {
            e = iae;
        }
        throw new TransformerFactoryConfigurationError(e);
    }

    /**
   * Creates a new Transformer / XPath Factory object.
   *
   * <p>
   * A unique Factory object is returned for each call to this method.
   *
   * @param bundle The bundle using the service.
   * @param registration The <code>ServiceRegistration</code> object for the
   *        service.
   * @return A new, Factory object or null if a configuration error
   * was encountered.
   */
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        ServiceReference sref = registration.getReference();
        String factoryClassName = (String) sref.getProperty(FACTORYNAMEKEY);
        try {
            Object factory = getFactory(factoryClassName);
            return factory;
        } catch (TransformerFactoryConfigurationError fce) {
            fce.printStackTrace();
            return null;
        }
    }

    /**
   * Releases a Factory object.
   *
   * @param bundle The bundle releasing the service.
   * @param registration The <code>ServiceRegistration</code> object for the
   *        service.
   * @param service The Factory object returned by a previous call
   *        to the <code>getService</code> method.
   */
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
    }
}
