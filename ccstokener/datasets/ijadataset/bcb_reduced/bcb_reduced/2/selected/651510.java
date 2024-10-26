package de.mse.mogwai.utils.erdesigner.plugins;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.mse.mogwai.utils.erdesigner.types.EntityContainer;

/**
 * Plug - In Manager to discover and run ERDesigner Plug - Ins.
 * 
 * @author Mirko Sertic
 */
public class PluginManager {

    private static PluginManager me;

    private DocumentBuilder m_builder;

    private HashMap m_plugins;

    /**
	 * Construct the manager
	 */
    private PluginManager() {
        this.m_plugins = new HashMap();
        try {
            this.m_builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Enumeration enumeration = ClassLoader.getSystemClassLoader().getResources("META-INF/erdesigner_plugins.xml");
            while (enumeration.hasMoreElements()) {
                this.initialize((URL) enumeration.nextElement());
            }
            enumeration = ClassLoader.getSystemClassLoader().getResources("meta-inf/erdesigner_plugins.xml");
            while (enumeration.hasMoreElements()) {
                this.initialize((URL) enumeration.nextElement());
            }
        } catch (Exception e) {
        }
    }

    /**
	 * Initialize a plug in.
	 * 
	 * @param url
	 *            The plug in configuration url
	 * @throws SAXException
	 *             will be thrown on an XML error
	 * @throws IOException
	 *             will be thrown on an IO error
	 */
    private void initialize(URL url) throws SAXException, IOException {
        Document doc = this.m_builder.parse(url.openStream());
        NodeList plugins = doc.getDocumentElement().getElementsByTagName("Plugin");
        for (int count = 0; count < plugins.getLength(); count++) {
            Element plug = (Element) plugins.item(count);
            String name = plug.getAttribute("name");
            String cl = plug.getAttribute("class");
            String group = plug.getAttribute("group");
            this.m_plugins.put(name, new PluginDescriptor(name, cl, group));
        }
    }

    /**
	 * Get the plug in manager instance.
	 * 
	 * @return the singleton instance
	 */
    public static PluginManager getInstance() {
        if (me == null) me = new PluginManager();
        return me;
    }

    /**
	 * Get the list of all registered plugins
	 * 
	 * @return the list of all plug ins
	 */
    public Vector getPlugins() {
        return new Vector(this.m_plugins.values());
    }

    /**
	 * Execute a plug in.
	 * 
	 * @param name
	 *            the plug in name
	 * @param container
	 *            the container
	 */
    public void execute(String name, EntityContainer container) throws Exception {
        Constructor c = Class.forName(((PluginDescriptor) this.m_plugins.get(name)).getClassName()).getConstructor(new Class[] { EntityContainer.class });
        Plugin p = (Plugin) c.newInstance(new Object[] { container });
        p.execute();
    }
}
