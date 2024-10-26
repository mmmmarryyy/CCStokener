package it.tukano.jps.launcher;

import it.tukano.jps.core.Module;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Load modules from a modules.xml configuration file in classpath
 */
public class ModuleLoader {

    /**
     * Default no arg constructor
     */
    public ModuleLoader() {
    }

    /**
     * Load modules from the modules.xml configuration file.
     * @return a list of module read from the modules.xml configuration file.
     */
    public Collection<Module> loadModules() {
        URL url = getClass().getResource("/modules.xml");
        if (url == null) {
            java.util.logging.Logger.getLogger(ModuleLoader.class.getName()).log(java.util.logging.Level.SEVERE, "Cannot find modules.xml file in classpath");
            return Collections.<Module>emptyList();
        }
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        Document document = null;
        InputStream input = null;
        try {
            input = url.openStream();
            DocumentBuilder bui = fac.newDocumentBuilder();
            document = bui.parse(url.openStream());
        } catch (SAXException ex) {
            Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (document == null) {
            return Collections.<Module>emptyList();
        }
        List<Module> modules = new LinkedList<Module>();
        NodeList moduleListNodes = document.getElementsByTagName("module-list");
        for (int i = 0; i < moduleListNodes.getLength(); i++) {
            Element moduleListNode = (Element) moduleListNodes.item(i);
            NodeList moduleNodes = moduleListNode.getElementsByTagName("module");
            for (int j = 0; j < moduleNodes.getLength(); j++) {
                Element moduleNode = (Element) moduleNodes.item(j);
                String moduleClass = moduleNode.getAttribute("class");
                if (moduleClass != null) {
                    instantiateModule(moduleClass, modules);
                }
            }
        }
        return modules;
    }

    private void instantiateModule(String moduleClass, List<Module> modules) {
        try {
            Class<?> type = Class.forName(moduleClass);
            if (Module.class.isAssignableFrom(type)) {
                try {
                    modules.add((Module) type.newInstance());
                } catch (InstantiationException ex) {
                    Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, "Module instantiation failure.", ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, "Module instantiation failure.", ex);
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ModuleLoader.class.getName()).log(Level.SEVERE, "Module instantiation failure", ex);
        }
    }
}
