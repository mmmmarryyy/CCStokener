package org.openorb.orb.config;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.HashMap;
import org.apache.avalon.framework.logger.Logger;
import org.openorb.util.ExceptionTool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is used to manage OpenORB XML configuration file.
 *
 * @author Jerome Daniel
 * @author Chris Wood
 * @version $Revision: 1.23 $ $Date: 2004/07/20 23:50:44 $
 */
public class Configurator {

    private org.openorb.orb.config.Properties m_props = new org.openorb.orb.config.Properties();

    private static final String[] SPECIAL_PROPS = new String[] { "openorb.home", "openorb.config", "openorb.profile", "OpenORB", "openorb.dir", "Config", "Profile" };

    /**
     * Do not parse XML files. All settings must be passed via Properties.
     * This increases the startup perfromance significantly.
     */
    private boolean m_ignoreXML = false;

    /**
     * current base document. Relative URLs are resolved relative to this.
     */
    private URL m_currDoc = null;

    /**
     * URLs of loaded module and profile configurations, and loaded
     * property files. The values in the map for profiles and property
     * files are null, for modules the initializer's name or the empty
     * string (if the initializer attribute is empty).
     */
    private HashMap m_initializedModules = new HashMap();

    /**
     * Map containing the Document objects of previously parsed files.
     * the keys in the map are the fragmentless URLs
     */
    private HashMap m_parsedFiles = new HashMap();

    /**
     * Aliases for properties to use on the command line.
     */
    private HashMap m_cmdLineAlias = new HashMap();

    /**
     * List of initializers.  This set contains full names of classes
     * which implement either
     * org.omg.PortableInterceptor.ORBInitializer
     * or
     * org.openorb.orb.pi.FeatureInitializer.
     */
    private Set m_initializers = new HashSet();

    private static final String CONFIG_PUBLIC = "-//openorb.sf.net//OpenORB Config//EN";

    private Logger m_logger;

    private javax.xml.parsers.DocumentBuilder m_parser;

    private static final String ORB_INITIALIZER_PATTERN = "org.omg.PortableInterceptor.ORBInitializerClass.";

    private static final String FEATURE_INITIALIZER_PATTERN = "org.openorb.PI.FeatureInitializerClass.";

    /**
     * Configurator constructor.
     * @param args
     * @param props
     * @param logger Logger
     */
    public Configurator(String[] args, Properties props, final Logger logger) {
        m_logger = logger;
        m_props.enableLogging(getLogger());
        setupHandler(logger);
        Properties orbProps = getORBProperties();
        Properties sysProps = null;
        try {
            sysProps = System.getProperties();
        } catch (SecurityException ex) {
            logger.warn("Security settings do not allow reading system properties." + "The ORB may not work properly in this case.");
        }
        m_ignoreXML = getIgnoreXMLPropertySetting(orbProps, sysProps, props);
        parseSpecialArgs(args, props, orbProps);
        if (!m_ignoreXML) {
            m_currDoc = findRootConfig();
            handleXMLImport(m_currDoc);
        }
        if (orbProps != null) {
            addProperties("", orbProps);
        }
        if (sysProps != null) {
            addProperties("", sysProps);
        }
        if (props != null) {
            addProperties("", props);
        }
        if (args != null) {
            m_cmdLineAlias.put("DefaultInitialReference", "openorb.defaultInitialReference");
            m_cmdLineAlias.put("SrvName", "openorb.server.alias");
            m_cmdLineAlias.put("Debug", "openorb.debug.level");
            m_cmdLineAlias.put("Trace", "openorb.debug.trace");
            m_cmdLineAlias.put("CSIv2", "ImportModule.CSIv2");
            m_cmdLineAlias.put("CSIv2Realm", "csiv2.tss.realm");
            m_cmdLineAlias.put("GSSClientUser", "csiv2.css.user");
            m_cmdLineAlias.put("ClientIdentity", "csiv2.css.identity");
            parseArgs(args);
        }
    }

    private Logger getLogger() {
        return m_logger;
    }

    /**
     * Get the fully initialized property set.
     */
    public org.openorb.orb.config.Properties getProperties() {
        return m_props;
    }

    /**
     * Get the list of intializer class names.
     */
    public String[] getInitializers() {
        String[] ret = new String[m_initializers.size()];
        m_initializers.toArray(ret);
        return ret;
    }

    /**
     * Add a fragment to a URL
     */
    public URL addFragment(URL base, String fragment) throws MalformedURLException {
        String oldurl = base.toString();
        int idx = oldurl.indexOf('#');
        if (idx > 0) {
            oldurl = oldurl.substring(0, idx);
        }
        if (fragment == null) {
            return new URL(oldurl);
        }
        return new URL(oldurl + '#' + fragment.toLowerCase());
    }

    /**
     * Add properties, filtering out any orb or openorb initializers.
     */
    private void addProperty(String name, String value, boolean processImportsFlag, boolean processNonImportsFlag) {
        if (name.equals("ImportModule.CSIv2")) {
            value = "${openorb.home}config/CSIv2.xml#csiv2";
        }
        String lc = name.toLowerCase();
        for (int i = 0; i < SPECIAL_PROPS.length; ++i) {
            if (lc.startsWith(SPECIAL_PROPS[i])) {
                return;
            }
        }
        if (processImportsFlag) {
            if (name.startsWith("ImportModule") && !m_ignoreXML) {
                URL url;
                try {
                    url = new URL(m_props.formatString(value));
                } catch (final MalformedURLException ex) {
                    try {
                        url = new URL(m_props.formatString("${openorb.config}#" + value));
                    } catch (final MalformedURLException ex1) {
                        getLogger().error("Malformed URL in ImportModule", ex1);
                        throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE("Malformed URL in ImportModule (" + ex1 + ")"), ex);
                    }
                }
                handleURLImport(url);
                return;
            }
        }
        if (processNonImportsFlag) {
            if (name.startsWith("org.openorb.")) {
                if (name.startsWith("org.openorb.messaging.MessagingInitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.messaging.MessagingInitializerClass.");
                    if (name.length() >= "org.openorb.messaging.MessagingInitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith("org.openorb.rmi.InitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.rmi.InitializerClass.");
                    if (name.length() >= "org.openorb.rmi.InitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith("org.openorb.iiop.IIOPProtocolInitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.iiop.IIOPProtocolInitializerClass.");
                    if (name.length() >= "org.openorb.iiop.IIOPProtocolInitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith("org.openorb.adapter.fwd.ForwardInitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.adapter.fwd.ForwardInitializerClass.");
                    if (name.length() >= "org.openorb.adapter.fwd.ForwardInitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith("org.openorb.adapter.boa.BOAInitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.adapter.boa.BOAInitializerClass.");
                    if (name.length() >= "org.openorb.adapter.boa.BOAInitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith("org.openorb.adapter.poa.POAInitializerClass.")) {
                    System.err.println("Non-standard initializer specification is deprecated: " + "org.openorb.adapter.poa.POAInitializerClass.");
                    if (name.length() >= "org.openorb.adapter.poa.POAInitializerClass.".length()) {
                        if (m_initializedModules.containsKey(name)) {
                            m_initializers.remove(m_initializedModules.get(name));
                        }
                        m_initializedModules.put(name, value);
                    }
                    m_initializers.add(value);
                } else if (name.startsWith(FEATURE_INITIALIZER_PATTERN)) {
                    String className = name.substring(FEATURE_INITIALIZER_PATTERN.length());
                    m_initializers.add(className);
                }
            } else if (name.startsWith(ORB_INITIALIZER_PATTERN)) {
                String className = name.substring(ORB_INITIALIZER_PATTERN.length());
                m_initializers.add(className);
            } else if (!name.startsWith("ImportModule") || m_ignoreXML) {
                getProperties().addProperty(name, value);
            }
        }
    }

    /**
     * Add properties, filtering out any orb or openorb initializers.
     * This version processes all properties.
     */
    private void addProperty(String name, String value) {
        addProperty(name, value, true, true);
    }

    /**
     * Add all properties from java style properties.
     * @param props java style property set.
     */
    private void addProperties(String prefix, Properties props) {
        if (props == null) {
            return;
        }
        java.util.Enumeration enumeration;
        boolean processImportsFlag, processNonImportsFlag;
        for (int count = 0; count < 2; ++count) {
            try {
                enumeration = props.propertyNames();
            } catch (SecurityException ex) {
                return;
            }
            if (count == 0) {
                processImportsFlag = true;
                processNonImportsFlag = false;
            } else {
                processImportsFlag = false;
                processNonImportsFlag = true;
            }
            while (enumeration.hasMoreElements()) {
                String key = null;
                try {
                    key = (String) enumeration.nextElement();
                    if (!key.startsWith("sun.") && !key.startsWith("os.") && !key.startsWith("awt.") && !key.startsWith("java.")) {
                        addProperty(prefix + key, props.getProperty(key), processImportsFlag, processNonImportsFlag);
                    }
                } catch (SecurityException ex) {
                    getLogger().warn("The property '" + key + "' could not be added to the internal" + " property set due to security reasons. (" + ex + ")");
                }
            }
        }
    }

    /**
     * Installs our resource: protocol handler.
     */
    private void setupHandler(Logger logger) {
        try {
            String oldPkgs = System.getProperty("java.protocol.handler.pkgs");
            if (oldPkgs == null) {
                System.setProperty("java.protocol.handler.pkgs", "org.openorb.util.urlhandler");
            } else if (oldPkgs.indexOf("java.protocol.handler.pkgs") < 0) {
                System.setProperty("java.protocol.handler.pkgs", oldPkgs + "|" + "org.openorb.util.urlhandler");
            }
        } catch (SecurityException ex) {
            if (logger != null) {
                logger.warn("The URLHandler could not be set! The security settings " + "do not allow system properties to be set! (" + ex + ")");
            }
        }
    }

    private Properties getORBProperties() {
        java.util.Properties fileProps = new java.util.Properties();
        String javaHome;
        try {
            javaHome = System.getProperty("java.home");
        } catch (SecurityException ex) {
            return null;
        }
        File propFile = new File(javaHome + File.separator + "lib" + File.separator + "orb.properties");
        if (!propFile.exists()) {
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(propFile);
            try {
                fileProps.load(fis);
            } finally {
                fis.close();
            }
            return fileProps;
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    /**
     * Remove any ${property} settings and convert files into urls.
     */
    private URL parseURL(String str) {
        str = m_props.formatString(str);
        int i = str.lastIndexOf('#');
        if (i >= 0) {
            str = str.substring(0, i) + str.substring(i).toLowerCase();
        }
        try {
            return new URL(m_currDoc, str);
        } catch (MalformedURLException ex) {
            File f = new File(str);
            if (f.exists()) {
                try {
                    return f.toURL();
                } catch (MalformedURLException ex1) {
                }
            }
        }
        return null;
    }

    private String parseSpecialArg(String module, String name, String alias, String[] args, Properties props, Properties orbProps) {
        if (args != null) {
            for (int i = args.length - 1; i >= 0; --i) {
                if (args[i] == null || !args[i].startsWith("-ORB")) {
                    continue;
                }
                int idx = args[i].indexOf('=');
                if (idx >= 0) {
                    String expProp = args[i].substring(4, idx);
                    if (expProp.equals(alias) || expProp.equalsIgnoreCase(module + "." + name)) {
                        return args[i].substring(idx + 1);
                    }
                } else {
                    if (i + 1 >= args.length) {
                        continue;
                    }
                    idx = args[i + 1].indexOf('=');
                    if (idx >= 0) {
                        if (args[i].substring(4).equalsIgnoreCase(module) && args[i + 1].substring(0, idx).equalsIgnoreCase(name)) {
                            return args[i + 1].substring(idx + 1);
                        }
                    } else {
                        String expProp = args[i].substring(4);
                        if (expProp.equals(alias) || expProp.equalsIgnoreCase(module + "." + name)) {
                            return args[i + 1];
                        }
                    }
                }
            }
        }
        String expProp = module + "." + name;
        if (props != null) {
            String val = props.getProperty(expProp);
            if (val != null) {
                return val;
            }
        }
        try {
            String val = System.getProperty(expProp);
            if (val != null) {
                return val;
            }
        } catch (SecurityException ex) {
        }
        if (orbProps != null) {
            return orbProps.getProperty(expProp);
        }
        return null;
    }

    /**
     * Find the openorb.home and the openorb.profile.
     */
    private void parseSpecialArgs(String[] args, Properties props, Properties orbProps) {
        URL url = null;
        String val;
        val = parseSpecialArg("openorb", "home", "OpenORB", args, props, orbProps);
        if (val != null) {
            url = parseURL(val);
            if (url == null) {
                throw new org.omg.CORBA.INITIALIZE("Can't parse openorb.home property as a URL");
            }
            m_props.addProperty("openorb.home", url);
        } else {
            try {
                URL u = null;
                if (props != null) {
                    u = new URL(props.getProperty("openorb.home.urn", "resource:") + "/org/openorb/");
                } else {
                    u = new URL("resource", "", -1, "/org/openorb/", new org.openorb.util.urlhandler.resource.Handler(Thread.currentThread().getContextClassLoader()));
                }
                m_props.addProperty("openorb.home", u);
            } catch (final MalformedURLException ex) {
                if (url != null) {
                    m_props.addProperty("openorb.home", url);
                } else {
                    throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE("Can't determine openorb.home property (" + ex + ")"), ex);
                }
            }
        }
        val = parseSpecialArg("openorb", "config", "Config", args, props, orbProps);
        if (val != null) {
            url = parseURL(val);
            if (url == null) {
                throw new org.omg.CORBA.INITIALIZE("Can't parse openorb.config property as a URL");
            }
            m_props.addProperty("openorb.config", url);
        }
        val = parseSpecialArg("openorb", "profile", "Profile", args, props, orbProps);
        if (val != null) {
            m_props.addProperty("openorb.profile", val);
        }
    }

    private URL findRootConfigIn(String prop, String file, String profile) {
        File confFile = null;
        try {
            confFile = new File(System.getProperty(prop), file);
        } catch (SecurityException ex) {
        }
        if (confFile != null && confFile.exists()) {
            try {
                URL config = confFile.toURL();
                m_props.addProperty("openorb.config", config);
                if (profile != null) {
                    config = addFragment(config, profile);
                }
                return config;
            } catch (MalformedURLException ex) {
            }
        }
        return null;
    }

    /**
     * Find the root config file.
     */
    private URL findRootConfig() {
        URL config;
        String profile = m_props.getStringProperty("openorb.profile", null);
        if ((config = m_props.getURLProperty("openorb.config", null)) != null) {
            if (config.getRef() == null && profile != null) {
                try {
                    config = addFragment(config, profile);
                } catch (final MalformedURLException ex) {
                    getLogger().error("Invalid URL in property openorb.config.", ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE("Unable to parse profile from openorb.profile (" + ex + ")"), ex);
                }
            }
            return config;
        }
        config = findRootConfigIn("user.dir", "OpenORB.xml", profile);
        if (config != null) {
            return config;
        }
        config = findRootConfigIn("user.home", "OpenORB.xml", profile);
        if (config != null) {
            return config;
        }
        config = findRootConfigIn("user.home", ".OpenORB.xml", profile);
        if (config != null) {
            return config;
        }
        config = findRootConfigIn("java.home", "OpenORB.xml", profile);
        if (config != null) {
            return config;
        }
        config = m_props.getURLProperty("openorb.home", null);
        if (config != null) {
            try {
                config = new URL(config, "config/OpenORB.xml");
                m_props.addProperty("openorb.config", config);
                if (profile != null) {
                    config = addFragment(config, profile);
                }
                return config;
            } catch (MalformedURLException ex) {
            }
        }
        throw new org.omg.CORBA.INITIALIZE("Unable to locate OpenORB.xml");
    }

    /**
     * Import module from a URL.
     */
    private String handleURLImport(URL url) {
        String file = url.getFile();
        if (file.endsWith(".properties")) {
            return handlePropertiesImport(url);
        } else if (!m_ignoreXML && (file.indexOf(".xml") > 0)) {
            return handleXMLImport(url);
        } else {
            throw new org.omg.CORBA.INITIALIZE("Unknown type for URL \"" + url + "\"");
        }
    }

    private String handlePropertiesImport(URL url) {
        if (m_initializedModules.containsKey(url)) {
            return url.getRef();
        }
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(url.openStream());
            m_initializedModules.put(url, null);
            String fragment = url.getRef();
            if (fragment == null) {
                addProperties("", props);
            } else {
                addProperties(fragment + ".", props);
            }
            return fragment;
        } catch (final java.io.IOException ex) {
            final String msg = "Unable to read properties file from URL \"" + url + "\"";
            getLogger().error(msg, ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg + " (" + ex + ")"), ex);
        }
    }

    private Document parseXML(URL url) {
        try {
            Document ret = (Document) m_parsedFiles.get(url);
            if (ret != null) {
                return ret;
            }
            if (m_parser == null) {
                javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                m_parser = dbf.newDocumentBuilder();
                m_parser.setEntityResolver(new org.xml.sax.EntityResolver() {

                    public org.xml.sax.InputSource resolveEntity(String publicId, String systemId) {
                        if (CONFIG_PUBLIC.equals(publicId)) {
                            URL dtdurl = parseURL("${openorb.home}config/OpenORB.dtd");
                            org.xml.sax.InputSource is = new org.xml.sax.InputSource(dtdurl.toString());
                            try {
                                is.setByteStream(dtdurl.openStream());
                            } catch (java.io.IOException ex) {
                            }
                            return is;
                        }
                        return null;
                    }
                });
            }
            ret = m_parser.parse(url.toString());
            m_parsedFiles.put(url, ret);
            return ret;
        } catch (final javax.xml.parsers.ParserConfigurationException ex) {
            final String msg = "ParserConfigurationException while parsing XML File \"" + url + "\"";
            getLogger().error(msg, ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg + " (" + ex + ")"), ex);
        } catch (final org.xml.sax.SAXException ex) {
            final String msg = "SAXException while parsing XML File \"" + url + "\"";
            getLogger().error(msg, ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg + " (" + ex + ")"), ex);
        } catch (final java.io.IOException ex) {
            final String msg = "IOException while parsing XML File \"" + url + "\"";
            getLogger().error(msg, ex);
            throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg + " (" + ex + ")"), ex);
        }
    }

    private String handleXMLImport(URL url) {
        URL prevDoc = m_currDoc;
        try {
            m_currDoc = url;
            String fragment = url.getRef();
            if (fragment != null) {
                if (m_initializedModules.containsKey(url)) {
                    if (m_initializedModules.get(url) == null) {
                        return null;
                    }
                    return fragment;
                }
                try {
                    m_currDoc = addFragment(url, null);
                } catch (final MalformedURLException ex) {
                    final String msg = "Invalid URL : " + url;
                    getLogger().error(msg, ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.INTERNAL(msg), ex);
                }
            }
            Document docRoot = parseXML(m_currDoc);
            Element rootElem = docRoot.getDocumentElement();
            if (fragment == null) {
                String username = null;
                try {
                    username = System.getProperty("user.name");
                } catch (SecurityException ex) {
                }
                if (username != null) {
                    NodeList list = rootElem.getElementsByTagName("associations");
                    switch(list.getLength()) {
                        case 0:
                            break;
                        case 1:
                            list = ((Element) list.item(0)).getElementsByTagName("association");
                            for (int i = 0; i < list.getLength() && fragment == null; ++i) {
                                Element assoc = (Element) list.item(i);
                                if (!assoc.hasAttribute("profile")) {
                                    throw new org.omg.CORBA.INITIALIZE("Association in \"" + m_currDoc + "\" is missing profile.");
                                }
                                StringTokenizer strtok = new StringTokenizer(assoc.getAttribute("user"));
                                while (strtok.hasMoreTokens()) {
                                    if (strtok.nextToken().equals(username)) {
                                        fragment = assoc.getAttribute("profile");
                                        break;
                                    }
                                }
                            }
                            break;
                        default:
                            throw new org.omg.CORBA.INITIALIZE("Document \"" + m_currDoc + "\" has multiple associaton elements");
                    }
                }
                if (fragment == null) {
                    if (rootElem.hasAttribute("profile")) {
                        fragment = rootElem.getAttribute("profile");
                    } else {
                        fragment = "default";
                    }
                }
                try {
                    url = addFragment(url, fragment);
                } catch (final MalformedURLException ex) {
                    final String msg = "Unable to parse default profile name from \"" + m_currDoc + "\"";
                    getLogger().error(msg, ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg), ex);
                }
                if (m_initializedModules.containsKey(url)) {
                    if (m_initializedModules.get(url) == null) {
                        return null;
                    }
                    return fragment;
                }
            }
            if (rootElem.hasAttribute("xml:base")) {
                try {
                    m_currDoc = new URL(m_currDoc, m_props.formatString(rootElem.getAttribute("xml:base")));
                } catch (final MalformedURLException ex) {
                    final String msg = "The xml:base attribute of the xml file \"" + m_currDoc + "\" cannot be parsed";
                    getLogger().error(msg, ex);
                    throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg), ex);
                }
            }
            Element impElem = docRoot.getElementById(fragment);
            if (impElem == null) {
                NodeList list = rootElem.getChildNodes();
                Node node;
                Element elem;
                for (int i = 0; i < list.getLength(); ++i) {
                    node = list.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        elem = (Element) node;
                        if (fragment.equalsIgnoreCase(elem.getAttribute("name"))) {
                            impElem = elem;
                            break;
                        }
                    }
                }
                if (impElem == null) {
                    throw new org.omg.CORBA.INITIALIZE("Cannot find element \"" + url + "\"");
                }
            }
            String type = impElem.getTagName();
            if (type.equals("profile")) {
                handleImportElem(impElem, url, true);
                m_initializedModules.put(url, null);
                fragment = null;
            } else if (type.equals("module")) {
                if (impElem.hasAttribute("initializer")) {
                    m_initializers.add(impElem.getAttribute("initializer"));
                }
                m_initializedModules.put(url, fragment);
            }
            handleProperties(impElem, fragment, url);
            return fragment;
        } finally {
            m_currDoc = prevDoc;
        }
    }

    private boolean handleProperties(Element docRoot, String module, URL url) {
        NodeList list = docRoot.getChildNodes();
        Node node;
        Element nextElem;
        String subtype;
        String impModule;
        for (int i = 0; i < list.getLength(); ++i) {
            node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            nextElem = (Element) node;
            subtype = nextElem.getTagName();
            if (subtype.equals("import")) {
                if (url == null) {
                    return false;
                }
                impModule = handleImportElem(nextElem, url, false);
                if (!handleProperties(nextElem, impModule, null)) {
                    if (impModule == null) {
                        throw new org.omg.CORBA.INITIALIZE("Attempt to set properties from import of profile in \"" + url + "\"");
                    } else {
                        throw new org.omg.CORBA.INITIALIZE("Import statment in \"" + url + "\" has child import");
                    }
                }
            } else if (subtype.equals("property") || subtype.equals("rootproperty")) {
                if (module == null) {
                    if (url == null) {
                        return false;
                    }
                    throw new org.omg.CORBA.INITIALIZE("Profile \"" + url + "\" defines properties, disallowed");
                }
                handlePropertyElem(nextElem, module, url);
            } else if (subtype.equals("propalias")) {
                if (module == null) {
                    if (url == null) {
                        return false;
                    }
                    throw new org.omg.CORBA.INITIALIZE("Profile \"" + url + "\" defines propaliases, disallowed");
                }
                handlePropaliasElem(nextElem, module, url);
            } else if (subtype.equals("propertyset")) {
                if (module == null) {
                    if (url == null) {
                        return false;
                    }
                    throw new org.omg.CORBA.INITIALIZE("Profile \"" + url + "\" defines propertyset, disallowed");
                }
                handlePropertySet(nextElem, module);
            } else if (subtype.equals("description")) {
            } else {
                throw new org.omg.CORBA.INITIALIZE("Unknown element type " + subtype + " in \"" + url + "\"");
            }
        }
        return true;
    }

    private Element nextElement(Node prev) {
        while (prev != null && prev.getNodeType() != Node.ELEMENT_NODE) {
            prev = prev.getNextSibling();
        }
        return (Element) prev;
    }

    private String handleImportElem(Element impElem, URL url, boolean profile) {
        boolean hasExtends = impElem.hasAttribute("extends");
        boolean hasModule = impElem.hasAttribute("module");
        boolean hasProfile = impElem.hasAttribute("profile");
        boolean hasLink = impElem.hasAttribute("xlink:href");
        switch((hasExtends ? 1 : 0) + (hasModule ? 1 : 0) + (hasProfile ? 1 : 0) + (hasLink ? 1 : 0)) {
            case 0:
                if (!profile) {
                    throw new org.omg.CORBA.INITIALIZE("Import from \"" + url + "\" does not specify a target");
                }
                return null;
            case 1:
                if (!hasLink && !(profile ? hasExtends : (hasProfile || hasModule))) {
                    throw new org.omg.CORBA.INITIALIZE("Atribute " + (profile ? (hasProfile ? "profile" : "module") : "extends") + " found when " + (profile ? "extends" : "profile or module") + " attribute is expected in \"" + url + "\"");
                }
                break;
            default:
                throw new org.omg.CORBA.INITIALIZE("Multiple " + (profile ? "extends present in profile" : "import targets in module") + " at \"" + url + "\"");
        }
        URL importURL;
        if (!hasLink) {
            try {
                if (hasExtends) {
                    importURL = addFragment(url, impElem.getAttribute("extends"));
                } else if (hasModule) {
                    importURL = addFragment(url, impElem.getAttribute("module"));
                } else if (hasProfile) {
                    importURL = addFragment(url, impElem.getAttribute("profile"));
                } else {
                    throw new Error("Impossible state");
                }
            } catch (final MalformedURLException ex) {
                final String msg = "Unable to parse " + (profile ? "extends" : "module") + " for " + (profile ? "profile" : "module import in") + " \"" + url + "\"";
                getLogger().error(msg, ex);
                throw ExceptionTool.initCause(new org.omg.CORBA.INITIALIZE(msg), ex);
            }
        } else if (impElem.hasAttribute("xlink:href")) {
            String ref = impElem.getAttribute("xlink:href");
            if ((importURL = parseURL(ref)) == null) {
                throw new org.omg.CORBA.INITIALIZE("Unable to parse URL \"" + ref + "\" from " + (profile ? "profile" : "module import in") + " \"" + url + "\"");
            }
        } else if (!profile) {
            throw new org.omg.CORBA.INITIALIZE("Import from \"" + url + "\" does not specify a target");
        } else {
            return null;
        }
        String ret = handleURLImport(importURL);
        if (profile) {
            if (ret != null) {
                throw new org.omg.CORBA.INITIALIZE("Attempted to extend module \"" + importURL + "\" from profile \"" + url + "\"");
            }
        } else if (!hasLink && (ret == null ? hasModule : hasProfile)) {
            throw new org.omg.CORBA.INITIALIZE("Attempted to import " + ((ret == null) ? "profile" : "module") + "  \"" + importURL + "\" from " + ((ret == null) ? "module" : "profile") + " \"" + url + "\"");
        }
        return ret;
    }

    private void handlePropaliasElem(Element impElem, String module, URL url) {
        String name = impElem.getAttribute("name");
        if (name.length() == 0) {
            throw new org.omg.CORBA.INITIALIZE("Propalias in \"" + url + "\" does not have a name");
        }
        String alias = impElem.getAttribute("alias");
        if (alias.length() == 0) {
            throw new org.omg.CORBA.INITIALIZE("Propalias in \"" + url + "\" does not have an alias");
        }
        name = module + "." + name;
        m_cmdLineAlias.put(alias, name);
    }

    private void handlePropertyElem(Element impElem, String module, URL url) {
        String name = impElem.getAttribute("name");
        if (name.length() == 0) {
            throw new org.omg.CORBA.INITIALIZE("Property in \"" + url + "\" does not have a name");
        }
        String value;
        if (impElem.hasAttribute("value")) {
            value = impElem.getAttribute("value");
        } else {
            value = "true";
        }
        if (impElem.hasAttribute("root")) {
            String isRoot = impElem.getAttribute("root");
            if (isRoot.equals("false")) {
                name = module + "." + name;
            } else if (!isRoot.equals("true")) {
                throw new org.omg.CORBA.INITIALIZE("Property " + name + " in \"" + url + "\" has illegal value for root attribute");
            }
        } else if (!impElem.getTagName().equals("rootproperty")) {
            name = module + "." + name;
        }
        addProperty(name, value);
    }

    /**
     * Handle propertyset elements.
     */
    private void handlePropertySet(Element element, String moduleName) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if ((node.getNodeType() == Node.ELEMENT_NODE)) {
                Element elem = (Element) node;
                if (!elem.getTagName().equals("description")) {
                    m_props.addProperty(moduleName + "." + element.getAttribute("prefix") + "." + elem.getAttribute("name"), elem.getAttribute("value"));
                }
            }
        }
    }

    /**
     * Parse command line arguments. Three possible formats for argument strings:<p>
     * -ORBprop             gives property prop=true<p>
     * -ORBprop val         gives property prop=val<p>
     * -ORBmodule prop=val  gives property module.prop=val<p>
     */
    private void parseArgs(String[] args) {
        String name;
        String value;
        for (int i = 0; i < args.length; ++i) {
            if (args[i] != null && args[i].startsWith("-ORB")) {
                int keyIdx = args[i].indexOf('=');
                if (keyIdx < 0) {
                    if (args.length <= i + 1 || args[i + 1] == null || args[i + 1].startsWith("-")) {
                        name = args[i].substring(4);
                        value = "true";
                    } else {
                        keyIdx = args[i + 1].indexOf('=');
                        if (keyIdx < 0) {
                            name = args[i].substring(4);
                            value = args[i + 1];
                        } else {
                            name = args[i].substring(4) + "." + args[i + 1].substring(0, keyIdx);
                            value = args[i + 1].substring(keyIdx + 1);
                        }
                        ++i;
                    }
                } else {
                    name = args[i].substring(4, keyIdx);
                    value = args[i].substring(keyIdx + 1);
                }
                if (m_cmdLineAlias.containsKey(name)) {
                    name = (String) m_cmdLineAlias.get(name);
                }
                addProperty(name, value);
            }
        }
    }

    /**
     * Determine whether the ignoreXML property has been set.
     * Check the ORB properties, System properties, and finally
     * the cmdline properties (in that order).
     *
     * @param orbProps ORB properties
     * @param sysProps Java System properties
     * @param props cmdline properties
     */
    private boolean getIgnoreXMLPropertySetting(Properties orbProps, Properties sysProps, Properties props) {
        boolean ignoreXML = false;
        String key = "openorb.ignoreXML";
        if (orbProps != null) {
            String value = orbProps.getProperty(key);
            ignoreXML = getValue(value, ignoreXML);
        }
        if (sysProps != null) {
            String value = sysProps.getProperty(key);
            ignoreXML = getValue(value, ignoreXML);
        }
        if (props != null) {
            String value = props.getProperty(key);
            ignoreXML = getValue(value, ignoreXML);
        }
        if (getLogger() != null && getLogger().isDebugEnabled()) {
            getLogger().debug(key + "=" + ignoreXML);
        }
        return ignoreXML;
    }

    private boolean getValue(String value, boolean fallback) {
        if (value != null) {
            return Boolean.valueOf(value.toLowerCase()).booleanValue();
        } else {
            return fallback;
        }
    }
}
