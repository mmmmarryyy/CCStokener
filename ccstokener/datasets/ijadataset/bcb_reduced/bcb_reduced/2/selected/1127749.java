package com.parfumball.analyzer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The PacketAnalyzerRegistry loads a list of PacketAnalyzer implementations
 * from a packet analyzer registry file. The registry file is an XML file. 
 *  
 * @author prasanna
 */
public class PacketAnalyzerRegistry {

    /**
     * The packetanalyzer element.
     */
    public static final String PACKET_ANALYZER = "packetanalyzer";

    /**
     * The id attribute.
     */
    public static final String ID = "id";

    /**
     * The name attribute.
     */
    public static final String NAME = "name";

    /**
     * The class attribute.
     */
    public static final String CLASS = "class";

    /**
     * The extends attribute.
     */
    public static final String EXTENDS = "extends";

    /**
     * A hashtable of analyzer descriptors.
     */
    private Hashtable analyzers;

    /**
     * A Vector of Root analyzers. A Root analyzer
     * does not extend any other analyzer.
     */
    private Vector roots;

    /**
     * The one and only registry.
     */
    private static PacketAnalyzerRegistry registry;

    /**
     * Disable external creation.
     */
    private PacketAnalyzerRegistry() {
        super();
    }

    /**
     * Returns the singleton PacketAnalyzerRegistry instance.
     * 
     * @return
     */
    public static synchronized PacketAnalyzerRegistry getRegistry() {
        if (registry == null) {
            registry = new PacketAnalyzerRegistry();
        }
        return registry;
    }

    /**
     * Returns the PacketAnalyzerDescriptor corresponding to the given ID. 
     * If the given ID is null, throws a NullPointerException. If no 
     * PacketAnalyzerDescriptor is available corresponding to the ID, 
     * null is returned.
     * 
     * @param id
     * @return
     */
    public PacketAnalyzerDescriptor getDescriptor(String id) {
        if (id == null) {
            throw new NullPointerException("Given descriptor ID is null.");
        }
        return (PacketAnalyzerDescriptor) analyzers.get(id);
    }

    /**
     * Returns the PacketAnalyzer corresponding to the given ID. If the ID is null,
     * a NullPointerException is thrown. The PacketAnalyzer is instantiated if necessary. 
     * 
     * @param id The ID corresponding to the PacketAnalyzer.
     * @return The PacketAnalyzer implementation.
     * @throws PacketAnalyzerException If there is an error creating the PacketAnalyzer 
     * implementation or if the analyzer class does not implement the PacketAnalyzer interface.  
     */
    public synchronized PacketAnalyzer getAnalyzer(String id) throws PacketAnalyzerException {
        PacketAnalyzerDescriptor descriptor = getDescriptor(id);
        if (descriptor == null) {
            return null;
        }
        PacketAnalyzer analyzer = descriptor.getAnalyzer();
        if (analyzer != null) {
            return analyzer;
        }
        try {
            Class analyzerClass = Class.forName(descriptor.getImplementation());
            if (PacketAnalyzer.class.isAssignableFrom(analyzerClass)) {
                analyzer = (PacketAnalyzer) analyzerClass.newInstance();
                descriptor.setAnalyzer(analyzer);
                return analyzer;
            }
            throw new PacketAnalyzerException("Specified class [" + descriptor.getImplementation() + "] does not implement com.parfumball.analyzer.PacketAnalyzer.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new PacketAnalyzerException("Unable to create analyzer [" + id + "]", e);
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new PacketAnalyzerException("Unable to instantiate analyzer [" + id + "]", e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new PacketAnalyzerException("Unable to instantiate analyzer [" + id + "]", e);
        }
    }

    /**
     * Returns the root analyzers. Returns null if no root analyzers
     * are found.
     * 
     * @return
     */
    public PacketAnalyzerDescriptor[] getRootAnalyzers() {
        int count = roots.size();
        if (count == 0) {
            return null;
        }
        PacketAnalyzerDescriptor[] ret = new PacketAnalyzerDescriptor[count];
        for (int i = 0; i < count; i++) {
            ret[i] = (PacketAnalyzerDescriptor) roots.elementAt(i);
        }
        return ret;
    }

    /**
     * Loads the registry from the given URL. The process of loading the registry 
     * involves parsing the registry file and building a tree of PacketAnalyzerDescriptor
     * instances.
     * 
     * @param url The URL from which to load the registry.
     * @throws PacketAnalyzerRegistryException If an error occurs loading the registry.
     */
    public void loadRegistry(URL url) throws PacketAnalyzerRegistryException {
        if (analyzers != null) {
            return;
        }
        analyzers = new Hashtable();
        roots = new Vector();
        try {
            InputStream in = url.openStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(in);
            NodeList list = doc.getElementsByTagName(PACKET_ANALYZER);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                NamedNodeMap map = node.getAttributes();
                String id = map.getNamedItem(ID).getNodeValue();
                String name = map.getNamedItem(NAME).getNodeValue();
                String clazz = map.getNamedItem(CLASS).getNodeValue();
                Node n = map.getNamedItem(EXTENDS);
                String[] split = null;
                if (n != null) {
                    String extendedAnalyzers = n.getNodeValue();
                    if (extendedAnalyzers.trim().length() != 0) {
                        split = extendedAnalyzers.split("\\s*\\,+\\s*");
                    }
                }
                PacketAnalyzerDescriptor descriptor = new PacketAnalyzerDescriptor(id, name, clazz, split);
                addDescriptor(descriptor);
            }
            if (roots.size() == 0) {
                throw new PacketAnalyzerRegistryException("There is no root analyzer in the registry!");
            }
        } catch (IOException e) {
            throw new PacketAnalyzerRegistryException("Cannot open registry file.", e);
        } catch (ParserConfigurationException e) {
            throw new PacketAnalyzerRegistryException("Cannot parse registry file.", e);
        } catch (SAXException e) {
            throw new PacketAnalyzerRegistryException("Cannot parse registry file", e);
        } catch (Throwable e) {
            throw new PacketAnalyzerRegistryException("Cannot build PacketAnalyzerRegistry.", e);
        }
    }

    /**
     * Adds the given descriptor to the set of known descriptors. If the
     * analyzer corresponding to the descriptor does not specify a list of
     * analyzers that it extends, it is considered a root analyzer. Packet 
     * analysis starts with a root analyzer. All root analyzers are instantiated
     * in advance.  
     *  
     * @param descriptor
     * @throws PacketAnalyzerRegistryException
     */
    private void addDescriptor(PacketAnalyzerDescriptor descriptor) throws PacketAnalyzerRegistryException {
        String id = descriptor.getID();
        if (analyzers.get(id) != null) {
            throw new PacketAnalyzerRegistryException("Duplicate id [" + id + "] encountered.");
        }
        analyzers.put(id, descriptor);
        String[] extendedAnalyzers = descriptor.getExtendedAnalyzers();
        if (extendedAnalyzers == null || extendedAnalyzers.length == 0) {
            roots.add(descriptor);
            try {
                getAnalyzer(id);
            } catch (PacketAnalyzerException e) {
                e.printStackTrace();
                throw new PacketAnalyzerRegistryException("Unable to create root analyzer [" + id + "]", e);
            }
            return;
        }
        for (int i = 0; i < extendedAnalyzers.length; i++) {
            PacketAnalyzerDescriptor parent = (PacketAnalyzerDescriptor) analyzers.get(extendedAnalyzers[i]);
            parent.addDescriptor(descriptor);
        }
    }
}
