package org.jarp.core.i18n;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jarp.core.util.PropertyChangeGenerator;

/**
 * Resource bundle that will retrieve all of its information from a single XML
 * resource file.<br>
 * Implementation note: This class is heavily based on a XPath-enabled XML parser.
 * 
 * <p>Created on: 10/10/2002.</p>
 * 
 * @version $Revision: 1.2 $
 * @author <a href="mailto:ricardo_padilha@users.sourceforge.net">Ricardo Sangoi Padilha</a>
 */
public class XMLResourceBundle extends ResourceBundle implements PropertyChangeGenerator {

    /** Name of the property fired when the bundle is (re)loaded */
    public static final String BUNDLE = "resource.bundle";

    /** Name of the property fired when the locale is changed */
    public static final String LOCALE = "resource.locale";

    private Locale locale;

    private boolean isFileBased;

    private String filename;

    private URL url;

    private Document doc;

    private Map strings;

    private Map commands;

    private Map dialogs;

    private PropertyChangeSupport support;

    /**
	 * Constructor for XMLResourceBundle.
	 * @throws IOException from I/O operations on the XML file
	 */
    public XMLResourceBundle() throws IOException {
        this("resources/XMLResourceBundle.xml");
    }

    /**
	 * Constructor for XMLResourceBundle.
	 * @param filename the file where find the resources
	 * @throws IOException from I/O operations on the XML file
	 */
    public XMLResourceBundle(String filename) throws IOException {
        super();
        init();
        load(filename);
    }

    /**
	 * Constructor for XMLResourceBundle.
	 * @param url the file where find the resources
	 * @throws IOException from I/O operations on the XML file
	 */
    public XMLResourceBundle(URL url) throws IOException {
        super();
        init();
        load(url);
    }

    /**
	 * Instantiate variables.
	 */
    private void init() {
        strings = new Hashtable();
        commands = new Hashtable();
        dialogs = new Hashtable();
        support = new PropertyChangeSupport(this);
        setCurrentLocale(Locale.getDefault());
    }

    /**
	 * Clear instance variables. Subclasses must call this method
	 * if it is overriden.
	 */
    protected void reset() {
        strings.clear();
        commands.clear();
        dialogs.clear();
    }

    /**
	 * Loads a file, using the filename. Opens an <code>InputStream</code> and
	 * calls the appropriate method.
	 * @param filename the file to be loaded
	 * @throws IOException from I/O operations on the XML file
	 * @see #load(InputStream)
	 */
    public void load(String filename) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null.");
        }
        isFileBased = true;
        this.filename = filename;
        InputStream in = null;
        try {
            in = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            in = getClass().getResourceAsStream("/" + filename);
        }
        if (in != null) {
            try {
                load(in);
            } finally {
                in.close();
            }
        } else {
            throw new IOException("Error: Could not find the resource file named: " + filename + ".");
        }
    }

    /**
	 * Loads a file, using an URL. Opens an <code>InputStream</code> and
	 * calls the appropriate method.
	 * @param url the URL to get the XML file from
	 * @throws IOException from I/O operations on the XML file
	 * @see #load(InputStream)
	 */
    public void load(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null.");
        }
        isFileBased = false;
        this.url = url;
        InputStream in = null;
        try {
            in = url.openStream();
            load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
	 * Real loading of the file. This method is not public because
	 * the caller, who provides the <code>InputStream</code> is responsible
	 * for closing it afterwards.
	 * @param in the stream to read the content from
	 * @throws IOException from I/O operations on the XML file and wrapped
	 *                      exceptions from the XML parser
	 */
    protected void load(InputStream in) throws IOException {
        SAXReader reader = new SAXReader();
        try {
            Object old = this.doc;
            this.doc = reader.read(in);
            reset();
            support.firePropertyChange(BUNDLE, old, doc);
        } catch (DocumentException e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

    /**
	 * Forces a reload on the original XML file.
	 * @throws IOException from I/O operations on the XML file
	 */
    public void reload() throws IOException {
        if (isFileBased) {
            load(filename);
        } else {
            load(url);
        }
    }

    /**
	 * Returns the current locale.
	 * @return Locale
	 */
    public Locale getCurrentLocale() {
        return locale;
    }

    /**
	 * Sets the current locale.
	 * @param locale The locale to set
	 */
    public void setCurrentLocale(Locale locale) {
        if (locale != null) {
            Object old = this.locale;
            this.locale = locale;
            reset();
            support.firePropertyChange(LOCALE, old, locale);
        } else {
            throw new IllegalArgumentException("Locale cannot be null in XMLResourceBundle");
        }
    }

    /**
	 * @see java.util.ResourceBundle#getKeys()
	 */
    public Enumeration getKeys() {
        List strings = doc.selectNodes("/ResourceBundle/string");
        int k = strings.size();
        Vector keys = new Vector();
        for (int i = 0; i < k; i++) {
            keys.add(((Node) strings.get(i)).valueOf("@name"));
        }
        return keys.elements();
    }

    /**
	 * @see java.util.ResourceBundle#handleGetObject(String)
	 */
    protected Object handleGetObject(String key) throws MissingResourceException {
        Object value = strings.get(key);
        if (value == null) {
            value = getNode("string", key, getCurrentLocale()).valueOf("label");
            strings.put(key, value);
        }
        return value;
    }

    /**
	 * Get an object from a ResourceBundle, parsing the node
	 * and returning an instance. This method is equivalent to calling
	 * <code>getAction(key, false)</code>.
	 * @param key see <code>ResourceBundle</code> class description
	 * @exception MissingResourceException if <code>key</code> is not available.
	 * @return LocalizedTool
	 * @see #getAction(String, boolean)
	 */
    public LocalizedAction getAction(String key) throws MissingResourceException {
        return getAction(key, false);
    }

    /**
	 * Get an object from a ResourceBundle, parsing the node
	 * and returning an instance.
	 * @param key see <code>ResourceBundle</code> class description
	 * @param newInstance if false, return a previously created instance, from
	 * an internal cache
	 * @exception MissingResourceException if <code>key</code> is not available.
	 * @return LocalizedTool
	 */
    public LocalizedAction getAction(String key, boolean newInstance) throws MissingResourceException {
        LocalizedAction value = (LocalizedAction) commands.get(key);
        if (value == null) {
            Node node = getNode("command", key, getCurrentLocale());
            value = new LocalizedAction();
            value.setBundle(this);
            value.setName(key);
            value.setLabel(node.valueOf("label"));
            value.setTooltip(node.valueOf("tooltip"));
            value.setStatus(node.valueOf("status"));
            value.setIcon(node.valueOf("icon"));
            String m = node.valueOf("mnemonic");
            if (m != null && m.length() > 0) {
                value.setMnemonic(new Integer(m.charAt(0)));
            }
            String s = node.valueOf("shortcut");
            if (s != null && s.length() > 0) {
                value.setShortcut(s);
            }
            commands.put(key, value);
        }
        if (newInstance) {
            value = new LocalizedAction(value);
        }
        return value;
    }

    /**
	 * Get an object from a ResourceBundle.
	 * <BR>Convenience method to save casting.
	 * @param key see <code>ResourceBundle</code> class description
	 * @exception MissingResourceException if <code>key</code> is not available.
	 * @return LocalizedDialog
	 */
    public LocalizedDialog getDialog(String key) throws MissingResourceException {
        LocalizedDialog value = (LocalizedDialog) dialogs.get(key);
        if (value == null) {
            Node node = getNode("dialog", key, getCurrentLocale());
            value = new LocalizedDialog();
            value.setTitle(node.valueOf("title"));
            value.setText(node.valueOf("text"));
            value.setIcon(node.valueOf("icon"));
            dialogs.put(key, value);
        }
        return value;
    }

    /**
	 * Returns a node from the XML file based on it's type, name (key) and
	 * required locale.<br>
	 * Note: This method is based in a best-effort algorithm. That means that if
	 * the exact combination of arguments does not return a match, then
	 * the following steps are taken until a result is found:
	 * <br>1. Give up locale information:
	 * <br>1.a. Give up the variant;
	 * <br>1.a. Give up the country;
	 * <br>2. Give up the language: search for a language-independent version;
	 * <br>3. Search for a default value (a node with a "default=true" argument);
	 * <br>4. Return the first node that matches the key.
	 * 
	 * @param type the type of node: can be "string", "command" and "dialog"
	 * @param key see <code>ResourceBundle</code> class description
	 * @param locale the required locale (this is resource-dependent)
	 * @return Node
	 */
    protected Node getNode(String type, String key, Locale locale) {
        if (type == null || key == null) {
            throw new IllegalArgumentException("Type and Key cannot be null.");
        }
        String base = "/ResourceBundle/" + type + "[@name=\"" + key + "\"]/locale";
        Node node = null;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            node = doc.selectSingleNode(base + toXPathQuery(language, country, variant));
            if (node == null) {
                node = doc.selectSingleNode(base + toXPathQuery(language, country, null));
            }
            if (node == null) {
                node = doc.selectSingleNode(base + toXPathQuery(language, null, null));
            }
        }
        if (node == null) {
            node = doc.selectSingleNode(base + toXPathQuery("*", null, null));
        }
        if (node == null) {
            node = doc.selectSingleNode(base + "[@default=\"true\"]");
        }
        if (node == null) {
            node = doc.selectSingleNode(base + "[0]");
        }
        if (node == null) {
            throw new MissingResourceException("Error: Missing resource", getClass().toString(), key);
        }
        return node;
    }

    /**
	 * Converts the language, country and variant in a XPath compatible statement.
	 * @param language cannot be <code>null</code> (for language-independent, 
	 *                  use "*")
	 * @param country if <code>null</code> is ommited
	 * @param variant if <code>null</code> is ommited
	 * @return String
	 */
    protected String toXPathQuery(String language, String country, String variant) {
        StringBuffer query = new StringBuffer();
        query.append("[@language=\"");
        query.append(language);
        query.append("\"]");
        if (country != null && !country.equals("")) {
            query.append("[@country=\"");
            query.append(country);
            query.append("\"]");
        }
        if (variant != null && !variant.equals("")) {
            query.append("[@variant=\"");
            query.append(variant);
            query.append("\"]");
        }
        return query.toString();
    }

    /**
	 * @see org.jarp.core.util.PropertyChangeGenerator#addPropertyChangeListener(PropertyChangeListener)
	 */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
	 * @see org.jarp.core.util.PropertyChangeGenerator#addPropertyChangeListener(String, PropertyChangeListener)
	 */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.addPropertyChangeListener(propertyName, listener);
    }

    /**
	 * @see org.jarp.core.util.PropertyChangeGenerator#removePropertyChangeListener(PropertyChangeListener)
	 */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
	 * @see org.jarp.core.util.PropertyChangeGenerator#removePropertyChangeListener(String, PropertyChangeListener)
	 */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        support.removePropertyChangeListener(propertyName, listener);
    }

    /*************************************************************************/
    public static class XMLResourceBundleTest {

        /**
		 * Test class for XMLResourceBundle.
		 * @param args ignored
		 */
        public static void main(String[] args) {
            try {
                XMLResourceBundle r = new XMLResourceBundle();
                r.setCurrentLocale(new Locale("en", ""));
                System.out.println(r.getString("AppTitle"));
                r.setCurrentLocale(new Locale("fr", "*"));
                System.out.println(r.getString("AppTitle"));
                r.setCurrentLocale(new Locale("pt", "BR"));
                System.out.println(r.getString("AppTitle"));
                r.setCurrentLocale(new Locale("de", ""));
                System.out.println(r.getString("AppTitle"));
                r.setCurrentLocale(new Locale("en", ""));
                System.out.println(r.getString("AppName"));
                for (Enumeration e = r.getKeys(); e.hasMoreElements(); ) {
                    System.out.println(e.nextElement());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
