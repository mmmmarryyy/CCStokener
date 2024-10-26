package fi.iki.asb.util.config;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

/**
 * This class constructs objects from an XML file.
 *
 * @author Antti S. Brax
 * @author Jeon Jiwon
 * @version 2.0.0-b6
 */
public class XmlConfig extends HashMap {

    /**
     * The program version.
     */
    private static final String VERSION = "2.0.0-b6";

    /**
     * This attribute contains the new configuration during configuration
     * and reconfiguration.
     */
    private Map newconf = null;

    /**
     * The document builder.
     */
    private DocumentBuilder db;

    /**
     * This attribute stores a reference to the JDOM tree from which this
     * configuration was created.
     */
    private Document doc = null;

    /**
     * Replace ${name} type variables?
     */
    private boolean replaceVariables = false;

    /**
     * Create an empty configuration.
     */
    public XmlConfig() {
    }

    /**
     * Read a configuration from the InputStream.
     *
     * @param in the input stream which provides the new configuration.
     * @exception The configuration process failed.
     * @see #addConfiguration(java.io.InputStream)
     */
    public XmlConfig(InputStream in) throws XmlConfigException, IOException {
        addConfiguration(in);
    }

    /**
     * Read a configuration from the File.
     *
     * @exception The configuration process failed.
     * @see #addConfiguration(java.io.File)
     */
    public XmlConfig(File file) throws XmlConfigException, IOException {
        addConfiguration(file);
    }

    /**
     * Add configuration from the specified file.
     */
    public void addConfiguration(File file) throws XmlConfigException, IOException {
        addConfiguration(new FileInputStream(file));
    }

    /**
     * Add configuration from the specified input stream.
     */
    public void addConfiguration(InputStream in) throws XmlConfigException, IOException {
        if (db == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setCoalescing(true);
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new XmlConfigException("Could not create parser", ex);
            }
        }
        Document doc;
        try {
            doc = db.parse(in);
        } catch (SAXException ex) {
            throw new XmlConfigException("Could not parse configuration", ex);
        } catch (IOException ex) {
            throw new XmlConfigException("Error while reading configuration", ex);
        }
        newconf = new HashMap(this);
        handleDocument(doc);
        putAll(newconf);
        this.doc = doc;
    }

    /**
     * Get a string property. Equivalent to <code>(String)get(name)</code>.
     *
     * @param name the name of the property.
     */
    public String getProperty(String name) {
        return (String) get(name);
    }

    /**
     * Set a property. Equivalent to <code>put(name, value)</code>.
     *
     * @param name the name of the property.
     * @param value the value of the property.
     */
    public void setProperty(String name, Object value) {
        put(name, value);
    }

    /**
     * Handle the JDOM document.
     */
    private void handleDocument(Document doc) throws XmlConfigException {
        handleConf(doc.getDocumentElement());
    }

    /**
     * Handle &lt;conf&gt; elements.
     *
     * @param conf the conf element
     */
    private void handleConf(Element conf) throws XmlConfigException {
        String version = getAttributeValue(conf, "version");
        if (version != null && version.startsWith("2.")) {
            Value val = ElementHandler.handleNode(conf, new HashMap());
            newconf.putAll((Map) val.getValue());
            return;
        }
        try {
            oldHandleConf(conf);
        } catch (Exception ex) {
            throw new XmlConfigException("Error during configuration", ex);
        }
    }

    /**
     * Old handler.
     */
    private void oldHandleConf(Element conf) throws Exception {
        boolean oldReplaceVariables = replaceVariables;
        String str = getAttributeValue(conf, "replacevars");
        if (str == null || str.equals("inherit")) {
        } else if (str.equals("true")) {
            replaceVariables = true;
        } else if (str.equals("false")) {
            replaceVariables = false;
        } else {
            String msg = "Unknown variable replacement policy \"" + str + "\"";
            throw new XmlConfigException(msg);
        }
        NodeList list = conf.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                if (child.getNodeName().equals("include")) {
                    handleInclude((Element) child);
                } else if (child.getNodeName().equals("bean")) {
                    handleBean((Element) child);
                } else if (child.getNodeName().equals("property")) {
                    String name = getAttributeValue(child, "name");
                    String value = getTextContent(child);
                    boolean system = false;
                    String tmp = getAttributeValue(child, "system");
                    if (tmp == null) {
                        system = false;
                    } else if (tmp.equalsIgnoreCase("true")) {
                        system = true;
                    } else if (tmp.equalsIgnoreCase("false")) {
                        system = false;
                    } else {
                        String msg = "Unknown system-attribute value \"" + tmp + "\"";
                        throw new XmlConfigException(msg);
                    }
                    if (system) {
                        System.setProperty(name, value);
                    } else {
                        newconf.put(name, value);
                    }
                } else {
                    String msg = "Unknown element <" + child.getNodeName() + "> in <conf>";
                    throw new XmlConfigException(msg);
                }
            }
        }
        replaceVariables = oldReplaceVariables;
    }

    /**
     * Handle &lt;include&gt; elements.
     */
    private void handleInclude(Element elem) throws Exception {
        String source = getTextContent(elem);
        URL url = null;
        try {
            url = new URL(source);
        } catch (MalformedURLException e) {
            url = XmlConfig.class.getResource(source);
        }
        Document doc = db.parse(url.openStream());
        handleDocument(doc);
    }

    /**
     * Handle &lt;bean&gt; elements.
     *
     * @param elem the bean element
     */
    Object handleBean(Element elem) throws Exception {
        Object bean = null;
        String name = getAttributeValue(elem, "name");
        if (name != null) {
            bean = get(name);
            if (bean == null) {
                bean = newconf.get(name);
            }
        }
        List constructors = XmlTool.getChildrenByTagName(elem, "constructor");
        if (constructors.size() > 1) {
            throw new XmlConfigException("Multiple constructors defined for " + name);
        }
        Class clazz = null;
        String clazzName = getAttributeValue(elem, "class");
        if (bean == null) {
            if (clazzName != null && clazzName.length() != 0) {
                clazz = Class.forName(clazzName);
                if (constructors.size() > 0) {
                    bean = initializeBean(clazz, (Node) constructors.get(0));
                } else {
                    bean = clazz.newInstance();
                }
            } else {
                String msg;
                if (name != null) {
                    msg = "No class given to a new bean (" + name + ")";
                } else {
                    msg = "No class given to an unnamed bean";
                }
                throw new XmlConfigException(msg);
            }
        } else if (clazzName != null && clazzName.length() != 0 && clazzName.equals(bean.getClass().getName()) == false) {
            String msg = "Bean's (" + name + ") class has changed from " + clazzName + " to " + bean.getClass().getName();
            throw new XmlConfigException(msg);
        }
        NodeList list = elem.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                if (child.getNodeName().equals("attr")) {
                    handleAttr(bean, child);
                } else if (child.getNodeName().equals("call")) {
                    handleCall(bean, child);
                } else if (child.getNodeName().equals("list")) {
                    handleList(bean, child);
                } else if (child.getNodeName().equals("constructor")) {
                } else {
                    String msg = "Unknown element <" + child.getNodeName() + "> in <bean>";
                    throw new XmlConfigException(msg);
                }
            }
        }
        if (name != null && name.length() != 0) {
            newconf.put(name, bean);
        }
        return bean;
    }

    /**
     * Handle &lt;attr&gt; elements.
     *
     * @param bean the object that contains the attribute
     * @param attr the attr element
     */
    private void handleAttr(Object bean, Node node) throws Exception {
        Attr attr = new Attr(this, node);
        Class b_class = bean.getClass();
        String m_name = getSetterMethodName(attr.getName());
        Class[] p_classes = new Class[] { attr.getClazz() };
        Method method = getDeclaredMethod(b_class, m_name, p_classes);
        if (method == null) {
            throw new XmlConfigException("Class " + b_class + " does " + "not contain method " + getMethodName(m_name, p_classes));
        }
        method.invoke(bean, new Object[] { attr.getValue() });
    }

    /**
     * Handle &lt;call&gt; elements.
     *
     * @param bean the object that contains the method
     * @param call the call element
     */
    private void handleCall(Object bean, Node call) throws Exception {
        String m_name = getAttributeValue(call, "name");
        if (m_name == null) {
            throw new XmlConfigException("No name attribute in <call>");
        }
        Class b_class = bean.getClass();
        List params = XmlTool.getChildrenByTagName((Element) call, "param");
        Class[] p_classes = new Class[params.size()];
        Object[] p_values = new Object[params.size()];
        for (int i = 0; i < params.size(); i++) {
            Param param = new Param(this, (Node) params.get(i));
            p_classes[i] = param.getClazz();
            p_values[i] = param.getValue();
        }
        Method method = getDeclaredMethod(b_class, m_name, p_classes);
        if (method == null) {
            throw new XmlConfigException("Class " + b_class + " does " + "not contain method " + getMethodName(m_name, p_classes));
        }
        method.invoke(bean, p_values);
    }

    /**
     * Handle &lt;list&gt; elements.
     *
     * @param bean the object that contains the list
     * @param list the list element
     */
    private void handleList(Object bean, Node list) throws Exception {
        String l_name = getAttributeValue(list, "name");
        if (l_name == null) {
            throw new XmlConfigException("No name attribute in <list>");
        }
        String l_classname = getAttributeValue(list, "class");
        if (l_classname == null) {
            throw new XmlConfigException("No class attribute in <list name=\"" + l_name + "\">");
        }
        Class l_class = Class.forName(l_classname);
        Class b_class = bean.getClass();
        String m_name = getAdderMethodName(l_name);
        Class p_classes[] = new Class[] { l_class };
        Object m_param[] = new Object[1];
        Method method = getDeclaredMethod(b_class, m_name, p_classes);
        if (method == null) {
            throw new XmlConfigException("Class " + bean.getClass() + " does " + "not contain method " + getMethodName(m_name, p_classes));
        }
        NodeList nodeList = list.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child instanceof Element) {
                m_param[0] = handleBean((Element) child);
                method.invoke(bean, m_param);
            }
        }
    }

    /**
     * Initialize a bean with a specified constructor.
     */
    private Object initializeBean(Class clazz, Node node) throws Exception {
        List params = XmlTool.getChildrenByTagName((Element) node, "param");
        Class[] p_classes = new Class[params.size()];
        Object[] p_values = new Object[params.size()];
        for (int i = 0; i < params.size(); i++) {
            Param param = new Param(this, (Node) params.get(i));
            p_classes[i] = param.getClazz();
            p_values[i] = param.getValue();
        }
        Constructor constructor = null;
        constructor = clazz.getDeclaredConstructor(p_classes);
        try {
            return constructor.newInstance(p_values);
        } catch (Exception ex) {
            throw new XmlConfigException("Failed to invoke method " + clazz.getName() + "." + getMethodName("<init>", p_classes), ex);
        }
    }

    /**
     * Replace ${name} type variables from the string.
     */
    String replaceVariables(String orig) {
        if (replaceVariables == false || orig == null) {
            return orig;
        }
        StringBuffer sb = new StringBuffer();
        int start, end, index;
        index = 0;
        while ((start = orig.indexOf("${", index)) != -1) {
            sb.append(orig.substring(index, start));
            index = start;
            if ((end = orig.indexOf("}", index)) != -1) {
                String key = orig.substring(start + 2, end);
                index = end + 1;
                String val = (String) newconf.get(key);
                if (val != null) {
                    sb.append(val);
                }
            } else {
                break;
            }
        }
        if (index < orig.length()) {
            sb.append(orig.substring(index, orig.length()));
        }
        return sb.toString();
    }

    /**
     * Get the text content of the spesified element as a String.
     */
    String getTextContent(Node elem) {
        return replaceVariables(elem.getFirstChild().getNodeValue().trim());
    }

    /**
     * Get the name of an attribute.
     *
     * @param name the name of the attribute.
     */
    String getAttributeValue(Node elem, String name) {
        Node node = elem.getAttributes().getNamedItem(name);
        if (node != null) {
            return replaceVariables(node.getNodeValue());
        } else {
            return null;
        }
    }

    /**
     * Generate a setter method name for the spesified attribute name.
     */
    String getSetterMethodName(String attr) {
        StringBuffer sb = new StringBuffer("set");
        sb.append(Character.toUpperCase(attr.charAt(0)));
        sb.append(attr.substring(1));
        return sb.toString();
    }

    /**
     * Generate a setter method name for the spesified attribute name.
     */
    String getAdderMethodName(String attr) {
        StringBuffer sb = new StringBuffer("add");
        sb.append(Character.toUpperCase(attr.charAt(0)));
        sb.append(attr.substring(1));
        return sb.toString();
    }

    /**
     * Recursively search through the class hierarchy of <code>c</code>
     * for the spesified method.
     */
    Method getDeclaredMethod(Class c, String n, Class[] p) {
        Method m = null;
        try {
            m = c.getDeclaredMethod(n, p);
        } catch (NoSuchMethodException ex) {
            c = c.getSuperclass();
            if (c != null) {
                m = getDeclaredMethod(c, n, p);
            }
        }
        return m;
    }

    /**
     * Helper method for generating method names.
     */
    String getMethodName(String n, Class[] p) {
        StringBuffer sb = new StringBuffer(n).append("(");
        for (int i = 0; i < p.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(p[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Read a configuration from the InputStream.
     *
     * @param in the input stream which provides the new configuration.
     * @exception The configuration process failed.
     * @see #reconfigure(java.io.InputStream, javax.xml.parsers.DocumentBuilder)
     *
     * @deprecated This will be removed in version 2.1.
     */
    public XmlConfig(InputStream in, DocumentBuilder db) throws XmlConfigException, IOException {
        addConfiguration(in);
    }

    /**
     * Read a configuration from the File.
     *
     * @exception The configuration process failed.
     * @see #reconfigure(java.io.File, javax.xml.parsers.DocumentBuilder)
     *
     * @deprecated This will be removed in version 2.1.
     */
    public XmlConfig(File file, DocumentBuilder db) throws XmlConfigException, IOException {
        this(new FileInputStream(file), db);
    }

    /**
     * Read a configuration from the InputStream.
     *
     * @param in the input stream which provides the configuration.
     * @param replaceVariables replace ${name} type variables?
     * @exception The configuration process failed.
     * @see #reconfigure(java.io.InputStream)
     *
     * @deprecated This will be removed in version 2.0.
     */
    public XmlConfig(InputStream in, boolean replaceVariables) throws Exception {
        System.err.println("XmlConfig.<init>(InputStream, boolean) is " + "deprecated. Define variable replacement policy " + "in the XML document's conf element instead.");
        this.replaceVariables = replaceVariables;
        reconfigure(in);
    }

    /**
     * Read a configuration from the File.
     *
     * @param file the file that contains the configuration.
     * @param replaceVariables replace ${name} type variables?
     * @exception The configuration process failed.
     *
     * @deprecated This will be removed in version 2.0.
     */
    public XmlConfig(File file, boolean replaceVariables) throws Exception {
        this(new FileInputStream(file), replaceVariables);
    }

    /**
     * Read a configuration from a JDOM document.
     *
     * @param doc the JDOM document that contains the configuration.
     * @param replaceVariables replace ${name} type variables?
     * @exception The configuration process failed.
     * @see #reconfigure(org.jdom.Document)
     *
     * @deprecated This will be removed in version 2.0.
     */
    public XmlConfig(Document doc, boolean replaceVariables) throws Exception {
        this.replaceVariables = replaceVariables;
        reconfigure(doc);
    }

    /**
     * Read a configuration from a JDOM document.
     *
     * @param doc the JDOM document that contains the new configuration.
     * @exception The configuration process failed.
     * @see #reconfigure(org.jdom.Document)
     *
     * @deprecated This will be removed in version 2.0.
     */
    public XmlConfig(Document doc) throws Exception {
        this(doc, false);
    }

    /**
     * Read configuration from the InputStream and reconfigure the objects in
     * this configuration. This method calls reconfigure(org.jdom.Document).
     *
     * @param in The input stream which provides the new configuration.
     * @param db The XML parser. Use <code>null</code> to use the default
     *           parser.
     * @see #reconfigure(org.jdom.Document)
     *
     * @deprecated This will be removed in version 2.1.
     */
    public void reconfigure(InputStream in, DocumentBuilder db) throws XmlConfigException, IOException {
        this.db = db;
        reconfigure(in);
    }

    /**
     * Read configuration from the InputStream and reconfigure the objects in
     * this configuration. This method calls reconfigure(org.jdom.Document).
     *
     * @param in the input stream which provides the new configuration.
     * @see #reconfigure(org.jdom.Document)
     *
     * @deprecated This will be removed in version 2.1.
     */
    public void reconfigure(InputStream in) throws XmlConfigException, IOException {
        if (db == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setCoalescing(true);
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException ex) {
                throw new XmlConfigException("Could not create parser", ex);
            }
        }
        Document doc;
        try {
            doc = db.parse(in);
        } catch (SAXException ex) {
            throw new XmlConfigException("Could not parse configuration", ex);
        } catch (IOException ex) {
            throw new XmlConfigException("Error while reading configuration", ex);
        }
        newconf = new HashMap();
        handleDocument(doc);
        clear();
        putAll(newconf);
        this.doc = doc;
    }

    /**
     * Reconfigure the objects in this configuration according to the data
     * in the spesified JDOM document.
     *
     * @param doc the JDOM document that contains the new configuration.
     * @exception Exception the reconfiguration process failed and the
     *            contents of this configuration are in an unspesified
     *            state.
     * @see #reconfigure(java.io.InputStream)
     *
     * @deprecated This will be removed in version 2.0.
     */
    public void reconfigure(Document doc) throws Exception {
        newconf = new HashMap();
        handleDocument(doc);
        clear();
        putAll(newconf);
        this.doc = doc;
    }

    /**
     * Return the JDOM tree representation of the current configuration.
     * If the configuration was constructed or reconfigured from a JDOM
     * tree the returned document is the same one that was passed as
     * a parameter.
     *
     * @deprecated This will be removed in version 2.0.
     */
    public Document getConfig() {
        return this.doc;
    }

    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.err.println("XmlConfig version " + VERSION + ", Copyright 2003 Antti Brax (asb@iki.fi)\n");
            System.err.println("-version : print version number");
            System.err.println("-license : print license\n");
        } else if (args.length == 1) {
            if (args[0].equals("-version")) {
                System.err.println(VERSION);
            } else if (args[0].equals("-license")) {
                BufferedReader r = new BufferedReader(new InputStreamReader(XmlConfig.class.getResourceAsStream("/license")));
                String l;
                while ((l = r.readLine()) != null) {
                    System.err.println(l);
                }
            }
        }
    }
}
