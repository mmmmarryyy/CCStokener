package edu.yale.csgp.vitapad.action;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import edu.yale.csgp.vitapad.VitaPad;
import edu.yale.csgp.vitapad.plugin.PluginJAR;
import edu.yale.csgp.vitapad.resources.VPResources;
import edu.yale.csgp.vitapad.util.logging.ILogger;
import edu.yale.csgp.vitapad.util.logging.LoggerController;

/**
 * DOCUMENT ME!
 * 
 * @author Matt Holford
 */
public class ActionSet {

    private static final Object placeholder = new Object();

    private static final ILogger _log = LoggerController.createLogger(ActionSet.class);

    private Hashtable actions;

    ActionContext context;

    private boolean loaded;

    private String label;

    private PluginJAR _plugin;

    private URL _uri;

    private String _label;

    public ActionSet() {
        actions = new Hashtable();
        loaded = true;
        label = "<No label set>";
    }

    public ActionSet(PluginJAR plugin, String[] cachedActionNames, boolean[] cachedActionToggleFlags, URL uri) {
        this();
        _plugin = plugin;
        _uri = uri;
        if (cachedActionNames != null) {
            for (int i = 0; i < cachedActionNames.length; i++) {
                actions.put(cachedActionNames[i], placeholder);
                VitaPad.setTemporaryProperty(cachedActionNames[i] + ".toggle", cachedActionToggleFlags[i] ? "true" : "false");
            }
        }
        loaded = false;
    }

    public ActionSet(String label) {
        this();
        setLabel(label);
    }

    /**
     * 
     */
    public void setLabel(String label) {
        if (label == null) throw new NullPointerException();
        _label = label;
    }

    /**
     * 
     */
    public String[] getActionNames() {
        String[] actionNames = new String[actions.size()];
        Enumeration e = actions.keys();
        int i = 0;
        while (e.hasMoreElements()) {
            actionNames[i++] = (String) e.nextElement();
        }
        return actionNames;
    }

    /**
     * 
     */
    public VPAction getAction(String name) {
        Object obj = actions.get(name);
        if (obj == placeholder) {
            load();
            obj = actions.get(name);
            if (obj == placeholder) {
                _log.warn("Outdated action cache");
                obj = null;
            }
        }
        return (VPAction) obj;
    }

    /**
     * Load all the actions from this set using dom4j.
     */
    public void load() {
        if (loaded) return;
        loaded = true;
        final String propertyPath = VitaPad.getResources().getPropertyPath();
        URL url = VPResources.class.getResource(propertyPath + "actions.xml");
        System.out.println(propertyPath);
        SAXReader sr = new SAXReader();
        sr.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId.contains("actions.dtd")) return new InputSource(VPResources.class.getResourceAsStream(propertyPath + "actions.dtd")); else return null;
            }
        });
        Document doc = null;
        try {
            doc = sr.read(url.openStream());
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        List actions = root.elements("Action");
        for (int i = 0; i < actions.size(); i++) {
            Element action = (Element) actions.get(i);
            String name = action.attributeValue("Name");
            boolean noRepeat = Boolean.parseBoolean(action.attributeValue("NoRepeat"));
            boolean noRecord = Boolean.parseBoolean(action.attributeValue("NoRecord"));
            boolean noRemember = Boolean.parseBoolean(action.attributeValue("NoRemember"));
            Element actionCode = action.element("Code");
            String code = actionCode.getText();
            Element actionSelected = action.element("OnSelection");
            String onSelection = null;
            if (actionSelected != null) onSelection = actionSelected.getText();
            addAction(new JythonAction(name, code, onSelection, noRepeat, noRecord, noRemember));
        }
    }

    /**
     * 
     */
    public void getActionNames(List list) {
        Enumeration e = actions.keys();
        while (e.hasMoreElements()) list.add(e.nextElement());
    }

    /**
     * DOCUMENT ME!
     */
    public void addAction(VPAction action) {
        actions.put(action.getName(), action);
        if (context != null) {
            context.actionNames = null;
            context.actionHash.put(action.getName(), this);
        }
    }

    /**
     * 
     */
    public void initKeyBindings() {
        InputHandler inputHandler = VitaPad.getInputHandler();
        Iterator iter = actions.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String name = (String) entry.getKey();
            String shortcut1 = VitaPad.getProperty(name + ".shortcut");
            if (shortcut1 != null) inputHandler.addKeyBinding(shortcut1, name);
            String shortcut2 = VitaPad.getProperty(name + ".shortcut2");
            if (shortcut2 != null) inputHandler.addKeyBinding(shortcut2, name);
        }
    }
}
