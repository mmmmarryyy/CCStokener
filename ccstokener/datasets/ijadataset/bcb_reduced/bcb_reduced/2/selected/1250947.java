package org.xfc.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import org.xfc.XApp;
import org.xfc.XComponentFactory;
import org.xfc.components.XDefaultAction;
import org.xfc.components.XProxyAction;
import org.xfc.util.platform.XPlatform;

/**
 * Contains all the application resources - things like I18N strings, menubars and toolbars,
 * and icons.
 *
 * @author Devon Carew
 */
public class XResources {

    private XApp app;

    private List intlResources = new ArrayList();

    private Map actions = new HashMap();

    private Map components = new HashMap();

    private Map icons = new HashMap();

    /**
	 * 
	 * @param app
	 */
    public XResources(XApp app) {
        this.app = app;
    }

    /**
	 * @return a helper method to return the component factory for the app
	 */
    public XComponentFactory getComponentFactory() {
        return (XComponentFactory) app.getServiceLocator().getService(XComponentFactory.class);
    }

    /**
	 * @param bundle
	 */
    public void addInternationalizationResources(ResourceBundle bundle) {
        Properties properties = new Properties();
        Enumeration keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            properties.put(key, bundle.getObject(key));
        }
        intlResources.add(properties);
    }

    /**
	 * @param clazz
	 */
    public void addI18NResources(Class clazz) {
        addInternationalizationResources(ResourceBundle.getBundle(clazz.getName()));
    }

    /**
	 * @return the resource bundles that have been registered with this applciation
	 */
    public Properties[] getInternationalizationResources() {
        return (Properties[]) intlResources.toArray(new Properties[intlResources.size()]);
    }

    /**
	 * Returns the I18N string for the given key.
	 * 
	 * @param key
	 * @return the I18N string for the given key, or <code>null</code> if the key was not found
	 */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
	 * Given an icon's path (either a disk location or in the resource path) return the
	 * icon. If the icon cannot be found this method will return null. This class will
	 * cache icons - it will only ever load one copy of each icon.
	 * 
	 * @param path
	 *            a disk location or the resource path
	 * @return the icon if found; null if not
	 */
    public ImageIcon getIcon(String path) {
        ImageIcon icon = (ImageIcon) icons.get(path);
        if (icon != null) return icon;
        URL url = getClass().getResource(path);
        if (url == null) return null;
        icon = new ImageIcon(url);
        icons.put(path, icon);
        return icon;
    }

    /**
	 * Returns the I18N string for the given key.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return the I18N string for the given key, or <code>defaultValue</code> if the key was not found
	 */
    public String getString(String key, String defaultValue) {
        for (int i = 0; i < intlResources.size(); i++) {
            Properties properties = (Properties) intlResources.get(i);
            if (properties.getProperty(key) != null) return properties.getProperty(key);
        }
        return defaultValue;
    }

    /**
	 * @param clazz
	 */
    public void addXMLResources(Class clazz) {
        try {
            addXMLResources(clazz.getResource(XUtils.getShortName(clazz) + ".xml"));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        try {
            URL url = clazz.getResource(XUtils.getShortName(clazz) + "_" + XPlatform.getShortPlatformId() + ".xml");
            if (url != null) addXMLResources(url);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
	 * @param url
	 * @throws IOException
	 */
    public void addXMLResources(URL url) throws IOException {
        try {
            Document document = new Builder().build(url.openStream());
            Element root = document.getRootElement();
            if (!root.getLocalName().equals("resources")) throw new IOException("Document root must be <resources>");
            Elements elements = root.getChildElements();
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                if (element.getLocalName().equals("string")) parseString(element); else if (element.getLocalName().equals("menubar")) parseMenubar(element); else if (element.getLocalName().equals("menu")) parseMenu(element); else if (element.getLocalName().equals("toolbar")) parseToolbar(element); else throw new IOException("Unrecognized element: <" + element.getLocalName() + ">");
            }
        } catch (ParsingException pe) {
            IOException ioe = new IOException(pe.getMessage());
            ioe.initCause(pe);
            throw ioe;
        }
    }

    /**
	 * @return the collection of actions defined
	 */
    public Collection getActions() {
        return actions.values();
    }

    /**
	 * @param command
	 * @return the action with the given name
	 */
    public XProxyAction getAction(String command) {
        XProxyAction action = (XProxyAction) actions.get(command);
        if (action == null) {
            action = getComponentFactory().createAction(command);
            getComponentFactory().applyAction(action);
            if (action.getText() == null) action.setText(action.getCommand());
            actions.put(action.getCommand(), action);
        }
        return action;
    }

    /**
	 * @param name
	 * @return the toolbar for the given name
	 */
    public JToolBar getToolBar(String name) {
        return (JToolBar) components.get(name);
    }

    /**
	 * @param name
	 * @return the menubar for the given name
	 */
    public JMenuBar getMenuBar(String name) {
        return (JMenuBar) components.get(name);
    }

    /**
	 * @param name
	 * @return the menu for the given name
	 */
    public JMenu getMenu(String name) {
        return (JMenu) components.get(name);
    }

    private void parseString(Element element) {
    }

    private JMenuBar parseMenubar(Element element) throws IOException {
        JMenuBar menubar = getComponentFactory().createMenuBar();
        menubar.setName(element.getAttributeValue("name"));
        Elements elements = element.getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            if (e.getLocalName().equals("menu")) {
                menubar.add(parseMenu(e));
            } else {
                throw new IOException("Unrecognized element: <" + e.getLocalName() + ">");
            }
        }
        if (menubar.getName() != null) components.put(menubar.getName(), menubar);
        return menubar;
    }

    private JMenu parseMenu(Element element) throws IOException {
        JMenu menu = getComponentFactory().createMenu();
        menu.setName(element.getAttributeValue("name"));
        getComponentFactory().applyMenu(menu);
        if (menu.getText() == null || menu.getText().length() == 0) menu.setText(menu.getName());
        Elements elements = element.getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            if (e.getLocalName().equals("action")) {
                menu.add(parseAction(e));
            } else if (e.getLocalName().equals("menu")) {
                menu.add(parseMenu(e));
            } else if (e.getLocalName().equals("separator")) {
                menu.addSeparator();
            } else {
                throw new IOException("Unrecognized element: <" + e.getLocalName() + ">");
            }
        }
        if (menu.getName() != null) components.put(menu.getName(), menu);
        return menu;
    }

    private XDefaultAction parseAction(Element element) {
        XDefaultAction action = getAction(element.getAttributeValue("name"));
        if (action == null) {
            action = getComponentFactory().createAction(element.getAttributeValue("name"));
            getComponentFactory().applyAction(action);
            if (action.getText() == null) action.setText(action.getCommand());
            actions.put(action.getCommand(), action);
        }
        return action;
    }

    private JToolBar parseToolbar(Element element) throws IOException {
        JToolBar toolbar = getComponentFactory().createToolBar();
        toolbar.setName(element.getAttributeValue("name"));
        Elements elements = element.getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            if (e.getLocalName().equals("action")) {
                toolbar.add(parseAction(e));
            } else if (e.getLocalName().equals("separator")) {
                toolbar.addSeparator();
            } else {
                throw new IOException("Unrecognized element: <" + e.getLocalName() + ">");
            }
        }
        if (toolbar.getName() != null) components.put(toolbar.getName(), toolbar);
        return toolbar;
    }
}
