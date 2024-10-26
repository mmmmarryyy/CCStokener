package org.npsnet.v.gui;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.xml.parsers.*;
import org.npsnet.v.kernel.Kernel;
import org.npsnet.v.kernel.Module;
import org.npsnet.v.kernel.ModuleContainer;
import org.npsnet.v.properties.model.EntityModel;
import org.npsnet.v.properties.model.WorldModel;
import org.npsnet.v.services.gui.ContentPanel;
import org.npsnet.v.services.gui.ContentPanelContext;
import org.npsnet.v.services.gui.ContentPanelFactory;
import org.npsnet.v.services.gui.ContentPanelProvider;
import org.npsnet.v.services.gui.UnsupportedContextException;
import org.npsnet.v.services.resource.ModuleClassDescriptor;
import org.npsnet.v.services.resource.ResourceManager;
import org.w3c.dom.*;

/**
 * The standard content panel provider.
 *
 * @author Andrzej Kapolka
 */
public class StandardContentPanelProvider extends Module implements ContentPanelProvider {

    /**
     * The namespace URI associated with the module class.
     */
    private static final String MODULE_NAMESPACE_URI = "resource:///org/npsnet/v/kernel/Module.class";

    /**
     * The prototype tag.
     */
    private static final String PROTOTYPE = "Prototype";

    /**
     * The module tag.
     */
    private static final String MODULE = "Module";

    /**
     * The class attribute.
     */
    private static final String CLASS = "class";

    /**
     * The version attribute.
     */
    private static final String VERSION = "version";

    /**
     * The list of registered service extensions.
     */
    private Vector serviceExtensions;

    /**
     * Maps context classes to content panel factories.
     */
    private HashMap contextClassContentPanelFactoryMap;

    /**
     * Maps protocol names to content panel factories.
     */
    private HashMap protocolNameContentPanelFactoryMap;

    /**
     * Maps MIME types to content panel factories.
     */
    private HashMap mimeTypeContentPanelFactoryMap;

    /**
     * Maps XML root elements to content panel factories.
     */
    private HashMap rootElementContentPanelFactoryMap;

    /**
     * Maps module properties to content panel factories.
     */
    private HashMap modulePropertyContentPanelFactoryMap;

    /**
     * An internal class for appying configuration documents in an
     * independent thread.
     */
    private class ConfigurationApplicationThread extends Thread {

        /**
         * The target module.
         */
        private Module target;

        /**
         * The location of the configuration document to apply.
         */
        private URL configuration;

        /**
         * Constructor.
         *
         * @param pTarget the target module
         * @param pConfiguration the location of the configuration
         * document to apply
         */
        public ConfigurationApplicationThread(Module pTarget, URL pConfiguration) {
            target = pTarget;
            configuration = pConfiguration;
        }

        /**
         * The thread execution method.
         */
        public void run() {
            try {
                target.applyConfiguration(configuration);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e, "Error Applying Configuration", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Constructor.
     */
    public StandardContentPanelProvider() {
        serviceExtensions = new Vector();
        contextClassContentPanelFactoryMap = new HashMap();
        protocolNameContentPanelFactoryMap = new HashMap();
        mimeTypeContentPanelFactoryMap = new HashMap();
        rootElementContentPanelFactoryMap = new HashMap();
        modulePropertyContentPanelFactoryMap = new HashMap();
        ContentPanelFactory resourceContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                if (!context.getURL().getPath().endsWith("/")) {
                    throw new UnsupportedContextException();
                }
                ResourceContentPanel rcp = new ResourceContentPanel(StandardContentPanelProvider.this);
                rcp.setContext(context);
                return rcp;
            }
        };
        ContentPanelFactory moduleContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                String protocol = context.getURL().getProtocol();
                if (protocol.equals("module")) {
                    String name = context.getURL().getPath();
                    Module module;
                    if (name.length() < 2) {
                        module = getContainer();
                    } else if (!name.endsWith("/")) {
                        module = getContainer().getModuleNamed(name.substring(1));
                    } else {
                        module = getContainer().getModuleNamed(name.substring(1, name.length() - 1));
                    }
                    if (module != null) {
                        Iterator it = modulePropertyContentPanelFactoryMap.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry me = (Map.Entry) it.next();
                            if (((Class) me.getKey()).isInstance(module)) {
                                ContentPanelFactory cpf = (ContentPanelFactory) me.getValue();
                                try {
                                    return cpf.openContentPanelContext(context);
                                } catch (UnsupportedContextException uce) {
                                }
                            }
                        }
                    }
                } else {
                    ModuleClassDescriptor mcd = NewModuleItemFactory.getModuleClassDescriptor(context.getURL());
                    if (mcd != null) {
                        ContentPanelFactory cpf = factoryForModuleClassDescriptor(mcd);
                        if (cpf != null) {
                            try {
                                return cpf.openContentPanelContext(context);
                            } catch (UnsupportedContextException uce) {
                            }
                        }
                    }
                }
                ModuleContentPanel mcp = new ModuleContentPanel(StandardContentPanelProvider.this);
                mcp.setContext(context);
                return mcp;
            }
        };
        ContentPanelFactory configurationContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                ConfigurationContentPanel ccp = new ConfigurationContentPanel(StandardContentPanelProvider.this);
                ccp.setContext(context);
                return ccp;
            }
        };
        ContentPanelFactory prototypeContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document d = db.parse(context.getURL().toString());
                    Element e = d.getDocumentElement();
                    if (MODULE_NAMESPACE_URI.equals(e.getNamespaceURI()) && PROTOTYPE.equals(e.getLocalName())) {
                        NodeList nl = e.getChildNodes();
                        for (int i = 0; i < nl.getLength(); i++) {
                            if (nl.item(i) instanceof Element && nl.item(i).getNamespaceURI() != null && nl.item(i).getNamespaceURI().equals(MODULE_NAMESPACE_URI) && nl.item(i).getLocalName().equals(MODULE)) {
                                Element me = (Element) nl.item(i);
                                if (me.hasAttribute(CLASS)) {
                                    ResourceManager rm = (ResourceManager) getServiceProvider(ResourceManager.class);
                                    ModuleClassDescriptor mcd;
                                    if (me.hasAttribute(VERSION)) {
                                        mcd = rm.getModuleClassDescriptor(me.getAttribute(CLASS), me.getAttribute(VERSION));
                                    } else {
                                        mcd = rm.getModuleClassDescriptor(me.getAttribute(CLASS));
                                    }
                                    if (mcd != null) {
                                        ContentPanelFactory cpf = factoryForModuleClassDescriptor(mcd);
                                        if (cpf != null) {
                                            try {
                                                return cpf.openContentPanelContext(context);
                                            } catch (UnsupportedContextException uce) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new UnsupportedContextException();
                }
                PrototypeContentPanel pcp = new PrototypeContentPanel(StandardContentPanelProvider.this);
                pcp.setContext(context);
                return pcp;
            }
        };
        ContentPanelFactory modelContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                ModelContentPanel mcp = new ModelContentPanel(StandardContentPanelProvider.this);
                mcp.setContext(context);
                return mcp;
            }
        };
        ContentPanelFactory imageContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                ImageContentPanel icp = new ImageContentPanel(StandardContentPanelProvider.this);
                icp.setContext(context);
                return icp;
            }
        };
        ContentPanelFactory textContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                TextContentPanel tcp = new TextContentPanel(StandardContentPanelProvider.this);
                tcp.setContext(context);
                return tcp;
            }
        };
        ContentPanelFactory worldContentPanelFactory = new ContentPanelFactory() {

            public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
                WorldContentPanel wcp = new WorldContentPanel(StandardContentPanelProvider.this);
                wcp.setContext(context);
                return wcp;
            }
        };
        registerContentPanelFactoryByContextClass(ResourceContentPanel.ResourceContentPanelContext.class, resourceContentPanelFactory);
        registerContentPanelFactoryByProtocol("resource", resourceContentPanelFactory);
        registerContentPanelFactoryByContextClass(ModuleContentPanel.ModuleContentPanelContext.class, moduleContentPanelFactory);
        registerContentPanelFactoryByProtocol("module", moduleContentPanelFactory);
        registerContentPanelFactoryByType("application/x-java-vm", moduleContentPanelFactory);
        registerContentPanelFactoryByContextClass(ConfigurationContentPanel.ConfigurationContentPanelContext.class, configurationContentPanelFactory);
        registerContentPanelFactoryByType("application/x-npsnetv-configuration+xml", configurationContentPanelFactory);
        registerContentPanelFactoryByRootElement("resource:///org/npsnet/v/kernel/Module.class", "Configuration", configurationContentPanelFactory);
        registerContentPanelFactoryByContextClass(PrototypeContentPanel.PrototypeContentPanelContext.class, prototypeContentPanelFactory);
        registerContentPanelFactoryByType("application/x-npsnetv-prototype+xml", prototypeContentPanelFactory);
        registerContentPanelFactoryByRootElement("resource:///org/npsnet/v/kernel/Module.class", "Prototype", prototypeContentPanelFactory);
        registerContentPanelFactoryByContextClass(ModelContentPanel.ModelContentPanelContext.class, modelContentPanelFactory);
        registerContentPanelFactoryByType("model", modelContentPanelFactory);
        registerContentPanelFactoryByContextClass(ImageContentPanel.ImageContentPanelContext.class, imageContentPanelFactory);
        registerContentPanelFactoryByType("image", imageContentPanelFactory);
        registerContentPanelFactoryByContextClass(TextContentPanel.TextContentPanelContext.class, textContentPanelFactory);
        registerContentPanelFactoryByType("text", textContentPanelFactory);
        registerContentPanelFactoryByContextClass(WorldContentPanel.WorldContentPanelContext.class, worldContentPanelFactory);
        registerContentPanelFactoryByModuleProperty(WorldModel.class, worldContentPanelFactory);
    }

    /**
     * Attempts to find and return a content panel factory for the described module
     * class.
     *
     * @param mcd the module class descriptor
     * @return an appropriate content panel factory, or <code>null</code> for none
     */
    private ContentPanelFactory factoryForModuleClassDescriptor(ModuleClassDescriptor mcd) {
        String[] properties = mcd.getProperties();
        for (int i = 0; i < properties.length; i++) {
            try {
                Class pc = Class.forName(properties[i]);
                if (modulePropertyContentPanelFactoryMap.containsKey(pc)) {
                    return (ContentPanelFactory) modulePropertyContentPanelFactoryMap.get(pc);
                }
            } catch (ClassNotFoundException cnfe) {
            }
        }
        return null;
    }

    /**
     * Registers a service extension.
     *
     * @param extension the module to register
     */
    public void registerServiceExtension(Module extension) {
        serviceExtensions.add(extension);
    }

    /**
     * Deregisters a service extension.
     *
     * @param extension the module to deregister
     */
    public void deregisterServiceExtension(Module extension) {
        serviceExtensions.remove(extension);
    }

    /**
     * Returns an immutable collection containing all registered service
     * extensions.  Each element of the collection will be a <code>Module</code>.
     *
     * @return an immutable collection containing all registered service
     * extensions
     */
    public Collection getServiceExtensions() {
        return Collections.unmodifiableCollection(serviceExtensions);
    }

    /**
     * Registers a content panel factory by context class (for example,
     * <code>ResourceContentPanelContext.class</code>).
     *
     * @param contextClass the context class supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByContextClass(Class contextClass, ContentPanelFactory cpf) {
        contextClassContentPanelFactoryMap.put(contextClass, cpf);
    }

    /**
     * Deregisters a content panel factory by context class.
     *
     * @param contextClass the context class supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByContextClass(Class contextClass, ContentPanelFactory cpf) {
        if (contextClassContentPanelFactoryMap.get(contextClass) == cpf) {
            contextClassContentPanelFactoryMap.remove(contextClass);
        }
    }

    /**
     * Registers a content panel factory by protocol (for example, "telnet").
     *
     * @param protocolName the name of the protocol supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByProtocol(String protocolName, ContentPanelFactory cpf) {
        protocolNameContentPanelFactoryMap.put(protocolName, cpf);
    }

    /**
     * Deregisters a content panel factory by protocol.
     *
     * @param protocolName the name of the protocol supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByProtocol(String protocolName, ContentPanelFactory cpf) {
        if (protocolNameContentPanelFactoryMap.get(protocolName) == cpf) {
            protocolNameContentPanelFactoryMap.remove(protocolName);
        }
    }

    /**
     * Registers a content panel factory by MIME type (for example, "image/jpeg").
     *
     * @param mimeType the MIME type supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByType(String mimeType, ContentPanelFactory cpf) {
        mimeTypeContentPanelFactoryMap.put(mimeType, cpf);
    }

    /**
     * Deregisters a content panel factory by MIME type.
     *
     * @param mimeType the MIME type supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByType(String mimeType, ContentPanelFactory cpf) {
        if (mimeTypeContentPanelFactoryMap.get(mimeType) == cpf) {
            mimeTypeContentPanelFactoryMap.remove(mimeType);
        }
    }

    /**
     * Registers a content panel factory by XML root element (for example,
     * "html").
     *
     * @param tagName the name of the root element supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByRootElement(String tagName, ContentPanelFactory cpf) {
        rootElementContentPanelFactoryMap.put(tagName, cpf);
    }

    /**
     * Deregisters a content panel factory by XML root element.
     *
     * @param tagName the name of the root element supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByRootElement(String tagName, ContentPanelFactory cpf) {
        if (rootElementContentPanelFactoryMap.get(tagName) == cpf) {
            rootElementContentPanelFactoryMap.remove(tagName);
        }
    }

    /**
     * Registers a content panel factory by XML root element (for example,
     * "html").
     *
     * @param namespaceURI the namespace URI of the root element
     * @param localName the name of the root element
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByRootElement(String namespaceURI, String localName, ContentPanelFactory cpf) {
        String key = "{" + namespaceURI + "}" + localName;
        rootElementContentPanelFactoryMap.put(key, cpf);
    }

    /**
     * Deregisters a content panel factory by XML root element.
     *
     * @param namespaceURI the namespace URI of the root element
     * @param localName the name of the root element
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByRootElement(String namespaceURI, String localName, ContentPanelFactory cpf) {
        String key = "{" + namespaceURI + "}" + localName;
        if (rootElementContentPanelFactoryMap.get(key) == cpf) {
            rootElementContentPanelFactoryMap.remove(key);
        }
    }

    /**
     * Registers a content panel factory by module property.
     *
     * @param propertyClass the module property class supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void registerContentPanelFactoryByModuleProperty(Class propertyClass, ContentPanelFactory cpf) {
        modulePropertyContentPanelFactoryMap.put(propertyClass, cpf);
    }

    /**
     * Deregisters a content panel factory by module class.
     *
     * @param propertyClass the module property class supported by the content panel factory
     * @param cpf the content panel factory
     */
    public void deregisterContentPanelFactoryByModuleProperty(Class propertyClass, ContentPanelFactory cpf) {
        if (modulePropertyContentPanelFactoryMap.get(propertyClass) == cpf) {
            modulePropertyContentPanelFactoryMap.remove(propertyClass);
        }
    }

    /**
     * Opens the specified context and, if necessary, returns a
     * content panel suitable for viewing and/or modifying the contained
     * content.
     *
     * @param context the context to open
     * @return a <code>ContentPanel</code> for viewing and/or modifying the
     * content, or <code>null</code> if the context does not require an 
     * embedded content panel
     * @exception UnsupportedContextException if the specified context is invalid
     * or unsupported
     */
    public ContentPanel openContentPanelContext(ContentPanelContext context) throws UnsupportedContextException {
        if (context.getSource() != null && context.getSource().isReleased()) {
            context.getSource().setReleased(false);
            try {
                context.getSource().setContext(context);
                return context.getSource();
            } catch (UnsupportedContextException uce) {
            }
        }
        if (contextClassContentPanelFactoryMap.containsKey(context.getClass())) {
            ContentPanelFactory cpf = (ContentPanelFactory) contextClassContentPanelFactoryMap.get(context.getClass());
            try {
                return cpf.openContentPanelContext(context);
            } catch (UnsupportedContextException uce) {
            }
        }
        String protocol = context.getURL().getProtocol();
        if (protocolNameContentPanelFactoryMap.containsKey(protocol)) {
            ContentPanelFactory cpf = (ContentPanelFactory) protocolNameContentPanelFactoryMap.get(protocol);
            try {
                return cpf.openContentPanelContext(context);
            } catch (UnsupportedContextException uce) {
            }
        }
        try {
            URLConnection urlc = context.getURL().openConnection();
            String type = urlc.getContentType();
            if (type != null) {
                if (mimeTypeContentPanelFactoryMap.containsKey(type)) {
                    ContentPanelFactory cpf = (ContentPanelFactory) mimeTypeContentPanelFactoryMap.get(type);
                    try {
                        return cpf.openContentPanelContext(context);
                    } catch (UnsupportedContextException uce) {
                    }
                }
                if (type.indexOf('/') != -1) {
                    String typePrefix = type.substring(0, type.indexOf('/'));
                    if (mimeTypeContentPanelFactoryMap.containsKey(typePrefix)) {
                        ContentPanelFactory cpf = (ContentPanelFactory) mimeTypeContentPanelFactoryMap.get(typePrefix);
                        try {
                            return cpf.openContentPanelContext(context);
                        } catch (UnsupportedContextException uce) {
                        }
                    }
                }
            }
            if (context.getURL().getPath().endsWith(".xml") || (type != null && type.endsWith(".xml"))) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Element docElement = db.parse(urlc.getInputStream()).getDocumentElement();
                String rootElementName;
                if (docElement.getNamespaceURI() == null) {
                    rootElementName = docElement.getTagName();
                } else {
                    rootElementName = "{" + docElement.getNamespaceURI() + "}" + docElement.getLocalName();
                }
                if (rootElementContentPanelFactoryMap.containsKey(rootElementName)) {
                    ContentPanelFactory cpf = (ContentPanelFactory) rootElementContentPanelFactoryMap.get(rootElementName);
                    try {
                        return cpf.openContentPanelContext(context);
                    } catch (UnsupportedContextException uce) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new UnsupportedContextException(context.toString());
    }
}
