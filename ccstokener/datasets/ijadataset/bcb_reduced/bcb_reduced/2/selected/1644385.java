package org.apache.ws.jaxme.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationException;
import javax.xml.bind.Validator;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.ws.jaxme.JMManager;
import org.apache.ws.jaxme.JMMarshaller;
import org.apache.ws.jaxme.JMUnmarshaller;
import org.apache.ws.jaxme.JMValidator;
import org.apache.ws.jaxme.PM;
import org.apache.ws.jaxme.PMException;
import org.apache.ws.jaxme.util.Configurator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/** <p>JaxMe's implementation of a JAXBContext.</p>
 *
 * @author <a href="mailto:joe@ispsoft.de">Jochen Wiedmann</a>
 * @version $Id: JAXBContextImpl.java,v 1.1.1.2 2005/09/07 21:59:54 jdmyers Exp $
 */
public class JAXBContextImpl extends JAXBContext {

    /** The namespace of JaxMe's configuration files.
     */
    public static final String CONFIGURATION_URI = "http://ws.apache.org/jaxme/namespaces/jaxme2/configuration";

    private static final SAXParserFactory spf;

    private static final DatatypeConverterImpl datatypeConverter = new DatatypeConverterImpl();

    private ClassLoader cl;

    private String packageNames;

    private Map managersByQName = new HashMap();

    private Map managersByInterface = new HashMap();

    private Class jmMarshallerClass;

    private Class jmUnmarshallerClass;

    private Class jmValidatorClass;

    static {
        spf = SAXParserFactory.newInstance();
        spf.setValidating(false);
        spf.setNamespaceAware(true);
        DatatypeConverter.setDatatypeConverter(datatypeConverter);
    }

    protected JAXBContextImpl() {
    }

    /** <p>Sets the ClassLoader to use.</p>
	 */
    protected void setClassLoader(ClassLoader pClassLoader) {
        cl = pClassLoader;
    }

    /** <p>Returns the ClassLoader to use.</p>
	 */
    public ClassLoader getClassLoader() {
        return cl;
    }

    /** <p>Sets the package names managed by this context.</p>
	 */
    protected void setPackageNames(String pPackageNames) {
        packageNames = pPackageNames;
    }

    /** <p>Returns the package names managed by this context.</p>
	 */
    public String getPackageNames() {
        return packageNames;
    }

    /** <p>Sets the JMMarshaller class to use.</p>
	 */
    protected void setJMMarshallerClass(Class pClass) {
        jmMarshallerClass = pClass;
    }

    /** <p>Returns the JMMarshaller class to use.</p>
	 */
    public Class getJMMarshallerClass() {
        return jmMarshallerClass;
    }

    /** <p>Sets the JMUnmarshaller class to use.</p>
	 */
    protected void setJMUnmarshallerClass(Class pClass) {
        jmUnmarshallerClass = pClass;
    }

    /** <p>Sets the JMUnmarshaller class to use.</p>
	 */
    public Class getJMUnmarshallerClass() {
        return jmUnmarshallerClass;
    }

    /** <p>Sets the JMValidator class to use.</p>
	 */
    protected void setJMValidatorClass(Class pClass) {
        jmValidatorClass = pClass;
    }

    /** <p>Returns the JMValidator class to use.</p>
	 */
    public Class getJMValidatorClass() {
        return jmValidatorClass;
    }

    public Marshaller createMarshaller() throws JAXBException {
        Class c = getJMMarshallerClass();
        try {
            JMMarshaller marshaller = (JMMarshaller) c.newInstance();
            marshaller.setJAXBContextImpl(this);
            return marshaller;
        } catch (InstantiationException e) {
            throw new JAXBException("Failed to instantiate class " + c.getName(), e);
        } catch (IllegalAccessException e) {
            throw new JAXBException("Illegal access to class " + c.getName(), e);
        } catch (ClassCastException e) {
            throw new JAXBException("Class " + c.getName() + " is not implementing " + JMMarshaller.class.getName());
        }
    }

    public Unmarshaller createUnmarshaller() throws JAXBException {
        Class c = getJMUnmarshallerClass();
        try {
            JMUnmarshaller unmarshaller = (JMUnmarshaller) c.newInstance();
            unmarshaller.setJAXBContextImpl(this);
            return unmarshaller;
        } catch (InstantiationException e) {
            throw new JAXBException("Failed to instantiate class " + c.getName(), e);
        } catch (IllegalAccessException e) {
            throw new JAXBException("Illegal access to class " + c.getName(), e);
        } catch (ClassCastException e) {
            throw new JAXBException("Class " + c.getName() + " is not implementing " + JMUnmarshaller.class.getName());
        }
    }

    public Validator createValidator() throws JAXBException {
        Class c = getJMValidatorClass();
        try {
            JMValidator validator = (JMValidator) c.newInstance();
            validator.setJAXBContextImpl(this);
            return validator;
        } catch (InstantiationException e) {
            throw new JAXBException("Failed to instantiate class " + c.getName(), e);
        } catch (IllegalAccessException e) {
            throw new JAXBException("Illegal access to class " + c.getName(), e);
        } catch (ClassCastException e) {
            throw new JAXBException("Class " + c.getName() + " is not implementing " + JMValidator.class.getName());
        }
    }

    protected JMManager getManagerByQName(QName pQName) {
        return (JMManager) managersByQName.get(pQName);
    }

    protected JMManager getManagerByInterface(Class pElementInterface) {
        return (JMManager) managersByInterface.get(pElementInterface);
    }

    /** Returns a Manager for the given QName.
	 *
	 * @throws JAXBException No Manager is registered for the
	 *   given QName.
	 */
    public JMManager getManager(QName pQName) throws JAXBException {
        JMManager manager = getManagerByQName(pQName);
        if (manager == null) {
            throw new JAXBException("A Manager for " + pQName + " is not declared.");
        }
        return manager;
    }

    /** Returns a Manager for the given element interface.
	 * Same method than {@link #getManager(Class)}, except that it
	 * throws a {@link JAXBException}.
	 * @throws JAXBException No Manager is registered for the
	 *   given QName.
	 * @see #getManagerS(Class)
	 */
    public JMManager getManager(Class pElementInterface) throws JAXBException {
        JMManager manager = getManagerByInterface(pElementInterface);
        if (manager == null) {
            throw new JAXBException("A Manager for " + pElementInterface.getName() + " is not declared.");
        }
        return manager;
    }

    /** Returns a Manager for the given element interface.
	 * Same method than {@link #getManager(Class)}, except that it
	 * throws a {@link SAXException}.
	 * @throws SAXException No Manager is registered for the
	 *   given QName.
	 * @see #getManager(Class)
	 */
    public JMManager getManagerS(Class pElementInterface) throws SAXException {
        JMManager manager = getManagerByInterface(pElementInterface);
        if (manager == null) {
            throw new SAXException("A Manager for " + pElementInterface.getName() + " is not declared.");
        }
        return manager;
    }

    /** <p>Returns a new JMMarshaller.</p>
   */
    public JMMarshaller getJMMarshaller() throws MarshalException {
        Class c = getJMMarshallerClass();
        if (c == null) {
            throw new MarshalException("A JMMarshaller class is not set.");
        }
        try {
            return (JMMarshaller) c.newInstance();
        } catch (Exception e) {
            throw new MarshalException("Failed to instantiate JMMarshaller class " + c, e);
        }
    }

    /** <p>Returns a new JMUnmarshaller.</p>
   */
    public JMUnmarshaller getJMUnmarshaller() throws UnmarshalException {
        Class c = getJMUnmarshallerClass();
        if (c == null) {
            throw new UnmarshalException("A JMUnmarshaller class is not set.");
        }
        try {
            return (JMUnmarshaller) c.newInstance();
        } catch (Exception e) {
            throw new UnmarshalException("Failed to instantiate JMUnmarshaller class " + c, e);
        }
    }

    /** <p>Returns a new JMValidator.</p>
   */
    public JMValidator getJMValidator() throws ValidationException {
        Class c = getJMValidatorClass();
        if (c == null) {
            throw new ValidationException("A JMValidator class is not set.");
        }
        try {
            return (JMValidator) c.newInstance();
        } catch (Exception e) {
            throw new ValidationException("Failed to instantiate JMValidator class " + c, e);
        }
    }

    /** <p>Returns a new instance of JMPM.</p>
   */
    public PM getJMPM(Class pElementInterface) throws PMException {
        JMManager manager = getManagerByInterface(pElementInterface);
        Class c = manager.getPmClass();
        if (c == null) {
            throw new PMException("No persistency class configured for " + pElementInterface.getName());
        }
        try {
            PM pm = (PM) c.newInstance();
            pm.init(manager);
            return pm;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new PMException("Could not instantiate persistence manager class " + c.getName(), e);
        }
    }

    /** <p>Returns a new instance of JMPM.</p>
   */
    public PM getJMPM(QName pQName) throws PMException {
        JMManager manager = getManagerByQName(pQName);
        Class c = manager.getPmClass();
        if (c == null) {
            throw new PMException("No persistency class configured for " + pQName);
        }
        try {
            PM pm = (PM) c.newInstance();
            pm.init(manager);
            return pm;
        } catch (Exception e) {
            throw new PMException("Could not instantiate persistence manager class " + c.getName(), e);
        }
    }

    /** <p>Initializes the context by loading the configuration
   * or the configurations from the given classpath.</p>
   */
    protected void init() throws JAXBException {
        if (packageNames == null || packageNames.length() == 0) {
            packageNames = JAXBContextImpl.class.getName();
            packageNames = packageNames.substring(0, packageNames.lastIndexOf('.'));
        }
        boolean first = true;
        for (StringTokenizer st = new StringTokenizer(packageNames, ":"); st.hasMoreTokens(); ) {
            String packageName = st.nextToken();
            String configFileName = ((packageName.length() > 0) ? (packageName.replace('.', '/') + '/') : "") + "Configuration.xml";
            URL url = getClassLoader().getResource(configFileName);
            if (url != null) {
                InputStream istream = null;
                try {
                    Configuration c = new Configuration(this);
                    Configurator configurator = new Configurator();
                    configurator.setNamespace(CONFIGURATION_URI);
                    configurator.setRootObject(c);
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    xr.setContentHandler(configurator);
                    istream = url.openStream();
                    InputSource isource = new InputSource(istream);
                    isource.setSystemId(url.toString());
                    xr.parse(isource);
                    istream.close();
                    istream = null;
                    if (first) {
                        first = false;
                        setJMMarshallerClass(c.getJMMarshallerClass());
                        setJMUnmarshallerClass(c.getJMUnmarshallerClass());
                        setJMValidatorClass(c.getJMValidatorClass());
                    }
                } catch (IOException e) {
                    throw new JAXBException("Failed to load config file " + url, e);
                } catch (SAXParseException e) {
                    Exception f = e.getException() == null ? e : e.getException();
                    throw new JAXBException("Failed to parse config file " + url + " at line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ": " + f.getMessage(), f);
                } catch (SAXException e) {
                    Exception f = e.getException() == null ? e : e.getException();
                    String msg = "Failed to parse config file " + url + ": " + f.getMessage();
                    throw new JAXBException(msg, f);
                } catch (ParserConfigurationException e) {
                    throw new JAXBException("Failed to create a SAX Parser: " + e.getMessage(), e);
                } finally {
                    if (istream != null) {
                        try {
                            istream.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        }
        if (first) {
            throw new JAXBException("Unable to locate configuration file Configuration.xml in " + packageNames);
        }
    }

    /** Creates a new instance of {@link javax.xml.bind.JAXBContext}.
   * Invoked implicitly by
   * {@link javax.xml.bind.JAXBContext#newInstance(java.lang.String)}.
   */
    public static JAXBContextImpl createContext() throws JAXBException {
        return createContext(null, JAXBContextImpl.class.getClassLoader());
    }

    /** Invoked from the SAX handler when loading the config file.
   */
    public Configuration createConfiguration(Attributes pAttributes) throws JAXBException {
        String className = pAttributes.getValue("", "className");
        if (className == null || className.length() == 0) {
            return new Configuration(this);
        } else {
            try {
                return (Configuration) cl.loadClass(className).newInstance();
            } catch (Exception e) {
                throw new JAXBException("Failed to instantiate Configuration class " + className, e);
            }
        }
    }

    private static boolean verbose = true;

    private static void showException(Exception e) {
        if (!verbose) {
            return;
        }
        System.err.println("Exception catched in " + JAXBContextImpl.class.getName() + ".createContext().");
        System.err.println("Set " + JAXBContextImpl.class.getName() + ".verbose = false to suppress this message.");
        e.printStackTrace(System.err);
    }

    /** Creates a new instance of {@link javax.xml.bind.JAXBContext}.
   * Invoked implicitly by
   * {@link javax.xml.bind.JAXBContext#newInstance(String, ClassLoader)}
   */
    public static JAXBContextImpl createContext(String pPackageNames, ClassLoader pClassLoader) throws JAXBException {
        try {
            JAXBContextImpl result = new JAXBContextImpl();
            result.setClassLoader(pClassLoader);
            result.setPackageNames(pPackageNames);
            result.init();
            return result;
        } catch (RuntimeException e) {
            showException(e);
            throw e;
        } catch (JAXBException e) {
            showException(e);
            throw e;
        }
    }

    /** Invoked from the SAX handler when reading the config file
   * for adding another instance of JMManager.
   */
    public void addManager(JMManager pManager) throws JAXBException {
        Class elementInterface = pManager.getElementInterface();
        if (elementInterface == null) {
            throw new JAXBException("The Manager must have its elementInterface set.");
        }
        if (managersByInterface.containsKey(elementInterface)) {
            throw new JAXBException("A Manager for interface " + elementInterface.getName() + " is already set.");
        }
        if (pManager.getDriverClass() == null) {
            throw new IllegalStateException("Missing driver class for " + pManager.getQName());
        }
        if (pManager.getHandlerClass() == null) {
            throw new IllegalStateException("Missing driver class for " + pManager.getQName());
        }
        QName qName = pManager.getQName();
        if (qName != null && managersByQName.containsKey(qName)) {
            throw new JAXBException("A Manager for document type " + qName + " is already set.");
        }
        managersByInterface.put(elementInterface, pManager);
        if (qName != null) {
            managersByQName.put(qName, pManager);
        }
    }
}
