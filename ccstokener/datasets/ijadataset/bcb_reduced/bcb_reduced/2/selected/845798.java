package org.apache.axis2.wsdl.util;

import org.apache.axis2.java.security.AccessController;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Import;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class provides support for processing a WSDL4J definition
 * with a lower memory footprint.  This is useful for certain
 * environments.
 * 
 * The Type and Documentation objects consume the most space
 * in many scenarios.  This implementation reloads these objects
 * when then they are requested.
 */
public class WSDLWrapperReloadImpl implements WSDLWrapperImpl {

    private static final Log log = LogFactory.getLog(WSDLWrapperReloadImpl.class);

    private static final boolean isDebugEnabled = log.isDebugEnabled();

    private static final String myClassName = "WSDLWrapperReloadImpl";

    static final long serialVersionUID = -2788807375814097409L;

    private Definition wsdlDefinition = null;

    private URL wsdlURL = null;

    private String wsdlExplicitURI = null;

    private String wsdlDocumentBaseURI = null;

    private static boolean USE_SOFT_REFERENCES = true;

    private transient SoftReference softTypes = null;

    private transient SoftReference softDocElement = null;

    /**
     * Constructor
     * The WSDL Defintion object is owned by the WSDLWrapperReloadImpl object.
     * 
     * @param def    The WSDL Definition
     */
    public WSDLWrapperReloadImpl(Definition def) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperReloadImpl(Definition def) called");
            log.trace(JavaUtils.callStackToString());
        }
        prepare(def, null);
    }

    /**
     * Constructor
     * The WSDL Defintion object is owned by the WSDLWrapperReloadImpl object.
     * 
     * @param def    The WSDL Definition
     * @param wURL   The URL for the wsdl
     */
    public WSDLWrapperReloadImpl(Definition def, URL wURL) {
        if (log.isDebugEnabled()) {
            log.debug("WSDLWrapperReloadImpl(Definition def, URL wURL) called");
            log.trace(JavaUtils.callStackToString());
        }
        prepare(def, wURL);
    }

    /**
     * Initialize the wsdl definition wrapper
     * 
     * @param def    The WSDL4J definition
     * @param wURL   The URL where the WSDL is obtained
     */
    private void prepare(Definition def, URL wURL) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".prepare()");
        }
        wsdlDefinition = def;
        wsdlURL = wURL;
        if (def != null) {
            try {
                wsdlDocumentBaseURI = def.getDocumentBaseURI();
                if ((wsdlURL == null) && (wsdlDocumentBaseURI != null)) {
                    try {
                        URL locURL = new URL(wsdlDocumentBaseURI);
                        wsdlURL = locURL;
                    } catch (Exception uex) {
                    }
                }
                if (wsdlURL != null) {
                    wsdlExplicitURI = getExplicitURI(wsdlURL);
                }
                releaseResources();
            } catch (Exception e) {
                if (isDebugEnabled) {
                    log.debug(myClassName + ".prepare():  Caught exception [" + e.getClass().getName() + "]  error [" + e.getMessage() + "]", e);
                }
            }
        }
        if (isDebugEnabled) {
            log.debug(myClassName + ".prepare():   wsdlDocumentBaseURI [" + wsdlDocumentBaseURI + "]     wsdlExplicitURI [" + wsdlExplicitURI + "]   wsdlURL [" + wsdlURL + "]");
        }
    }

    public void releaseResources() {
        if (wsdlDefinition != null) {
            Types types = wsdlDefinition.getTypes();
            if (types != null) {
                wsdlDefinition.setTypes(null);
            }
            this.setCachedTypes(types);
            Element element = wsdlDefinition.getDocumentationElement();
            if (element != null) {
                wsdlDefinition.setDocumentationElement(null);
            }
            this.setCachedDocElement(element);
        }
    }

    /**
     * Store the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @param types
     */
    private void setCachedTypes(Types types) {
        if (USE_SOFT_REFERENCES) {
            if (softTypes == null || softTypes.get() == null) {
                if (types != null) {
                    softTypes = new SoftReference(types);
                } else {
                    softTypes = new SoftReference(Boolean.FALSE);
                }
            }
        }
    }

    /**
     * Get the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @return types
     */
    private Types getCachedTypes() {
        if (USE_SOFT_REFERENCES) {
            if (softTypes == null || softTypes.get() == null) {
                return null;
            } else if (softTypes.get().equals(Boolean.FALSE)) {
                return null;
            } else {
                return (Types) softTypes.get();
            }
        } else {
            return null;
        }
    }

    private boolean hasCachedTypes() {
        if (USE_SOFT_REFERENCES) {
            return (softTypes != null && softTypes.get() != null);
        } else {
            return false;
        }
    }

    /**
     * Store the cached document element.  Since this is a SOFT reference,
     * the gc may remove it.
     * @param e Element
     */
    private void setCachedDocElement(Element e) {
        if (USE_SOFT_REFERENCES) {
            if (softDocElement == null || softDocElement.get() == null) {
                if (e != null) {
                    softDocElement = new SoftReference(e);
                } else {
                    softDocElement = new SoftReference(Boolean.FALSE);
                }
            }
        }
    }

    /**
     * Get the cached type.  Since this is a SOFT reference,
     * the gc may remove it.
     * @return types
     */
    private Element getCachedDocElement() {
        if (USE_SOFT_REFERENCES) {
            if (softDocElement == null || softDocElement.get() == null) {
                return null;
            } else if (softDocElement.get().equals(Boolean.FALSE)) {
                return null;
            } else {
                return (Element) softDocElement.get();
            }
        } else {
            return null;
        }
    }

    private boolean hasCachedDocElement() {
        if (USE_SOFT_REFERENCES) {
            return (softDocElement != null && softDocElement.get() != null);
        } else {
            return false;
        }
    }

    public Definition getUnwrappedDefinition() {
        Definition def;
        if (wsdlDefinition == null) {
            try {
                def = loadDefinition();
            } catch (Exception e) {
                if (isDebugEnabled) {
                    log.debug(myClassName + ".getUnwrappedDefinition(): error trying to load Definition    [" + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
                }
                def = null;
            }
        } else if (wsdlDefinition instanceof WSDLWrapperBasicImpl) {
            def = ((WSDLWrapperBasicImpl) wsdlDefinition).getUnwrappedDefinition();
        } else {
            try {
                def = loadDefinition();
                if (def == null) {
                    def = wsdlDefinition;
                }
            } catch (Exception e) {
                if (isDebugEnabled) {
                    log.debug(myClassName + ".getUnwrappedDefinition(): error trying to load Definition    [" + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
                }
                def = wsdlDefinition;
            }
        }
        return def;
    }

    /**
     * Sets the WSDL4J Definition object that is being wrapped
     *
     * @param d  the WSDL4J Definition object
     */
    public void setDefinitionToWrap(Definition d) {
        wsdlDefinition = d;
    }

    /**
     * Sets the location for the WSDL4J Definition object that is being wrapped
     */
    public void setWSDLLocation(String uriLocation) {
        if (uriLocation != null) {
            try {
                wsdlURL = new URL(uriLocation);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Gets the location for the WSDL4J Definition object that is being wrapped
     */
    public String getWSDLLocation() {
        if (wsdlURL != null) {
            return wsdlURL.toString();
        } else {
            return null;
        }
    }

    /**
     * Closes the use of the wrapper implementation and allows 
     * internal resources to be released.
     */
    public void close() {
    }

    public void setDocumentBaseURI(String d) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".setDocumentBaseURI(" + d + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setDocumentBaseURI(d);
        }
    }

    public String getDocumentBaseURI() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getDocumentBaseURI()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getDocumentBaseURI();
        }
        return null;
    }

    public void setQName(QName n) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".setQName(" + n + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setQName(n);
        }
    }

    public QName getQName() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getQName()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getQName();
        }
        return null;
    }

    public void setTargetNamespace(String t) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".setTargetNamespace(" + t + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setTargetNamespace(t);
        }
    }

    public String getTargetNamespace() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getTargetNamespace()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getTargetNamespace();
        }
        return null;
    }

    public void addNamespace(String prefix, String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addNamespace(" + prefix + ", " + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addNamespace(prefix, namespaceURI);
        }
    }

    public String removeNamespace(String prefix) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeNamespace(" + prefix + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeNamespace(prefix);
        }
        return null;
    }

    public String getNamespace(String prefix) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getNamespace(" + prefix + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNamespace(prefix);
        }
        return null;
    }

    public String getPrefix(String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getPrefix(" + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPrefix(namespaceURI);
        }
        return null;
    }

    public Map getNamespaces() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getNamespaces()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNamespaces();
        }
        return null;
    }

    public List getNativeAttributeNames() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getNativeAttributeNames()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getNativeAttributeNames();
        }
        return null;
    }

    public void setTypes(Types types) {
        if (wsdlDefinition != null) {
            if (isDebugEnabled) {
                log.debug(myClassName + ".setTypes() from wsdl Definition");
            }
            wsdlDefinition.setTypes(types);
            wsdlDefinition.setTypes(null);
        } else {
            if (isDebugEnabled) {
                log.debug(myClassName + ".setTypes(): nothing to set");
            }
        }
    }

    public Types getTypes() {
        if (isDebugEnabled) {
            log.trace(myClassName + ".getTypes() call stack =" + JavaUtils.callStackToString());
        }
        if (hasCachedTypes()) {
            Types t = getCachedTypes();
            if (isDebugEnabled) {
                log.debug(myClassName + ".getTypes() from soft reference [" + t + "]");
            }
            return t;
        }
        Definition def = null;
        try {
            def = loadDefinition();
        } catch (Exception e) {
            if (isDebugEnabled) {
                log.debug(myClassName + ".getTypes(): error trying to load Definition    [" + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
            }
        }
        if (def != null) {
            Types t = def.getTypes();
            setCachedTypes(t);
            setCachedDocElement(def.getDocumentationElement());
            if (isDebugEnabled) {
                log.debug(myClassName + ".getTypes() from reloaded wsdl Definition returning [" + t + "]");
            }
            return t;
        } else {
            if (isDebugEnabled) {
                log.debug(myClassName + ".getTypes() returning NULL");
            }
            return null;
        }
    }

    public void addImport(Import importDef) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addImport(" + importDef + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addImport(importDef);
        }
    }

    public Import removeImport(Import importDef) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeImport(" + importDef + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeImport(importDef);
        }
        return null;
    }

    public List getImports(String namespaceURI) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getImports(" + namespaceURI + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getImports(namespaceURI);
        }
        return null;
    }

    public Map getImports() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getImports()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getImports();
        }
        return null;
    }

    public void addMessage(Message message) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addMessage(" + message + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addMessage(message);
        }
    }

    public Message getMessage(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getMessage(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getMessage(name);
        }
        return null;
    }

    public Message removeMessage(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeMessage(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeMessage(name);
        }
        return null;
    }

    public Map getMessages() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getMessages()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getMessages();
        }
        return null;
    }

    public void addBinding(Binding binding) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addBinding(" + binding + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addBinding(binding);
        }
    }

    public Binding getBinding(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getBinding(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getBinding(name);
        }
        return null;
    }

    public Binding removeBinding(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeBinding(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeBinding(name);
        }
        return null;
    }

    public Map getBindings() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getBindings()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getBindings();
        }
        return null;
    }

    public Map getAllBindings() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getAllBindings()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllBindings();
        }
        return null;
    }

    public void addPortType(PortType portType) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addPortType(" + portType + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addPortType(portType);
        }
    }

    public PortType getPortType(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getPortType(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPortType(name);
        }
        return null;
    }

    public PortType removePortType(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removePortType(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removePortType(name);
        }
        return null;
    }

    public Map getPortTypes() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getPortTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getPortTypes();
        }
        return null;
    }

    public Map getAllPortTypes() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getAllPortTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllPortTypes();
        }
        return null;
    }

    public void addService(Service service) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addService(" + service + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addService(service);
        }
    }

    public Service getService(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getService(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getService(name);
        }
        return null;
    }

    public Service removeService(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeService(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeService(name);
        }
        return null;
    }

    public Map getServices() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getServices();
        }
        return null;
    }

    public Map getAllServices() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getAllServices()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getAllServices();
        }
        return null;
    }

    public void setDocumentationElement(org.w3c.dom.Element docEl) {
        if (wsdlDefinition != null) {
            if (isDebugEnabled) {
                log.debug(myClassName + ".setDocumentationElement(docEl) from wsdl Definition");
            }
            wsdlDefinition.setDocumentationElement(docEl);
            wsdlDefinition.setDocumentationElement(null);
        } else {
            if (isDebugEnabled) {
                log.debug(myClassName + ".setDocumentationElement(docEl): nothing to set");
            }
        }
    }

    public org.w3c.dom.Element getDocumentationElement() {
        if (isDebugEnabled) {
            log.trace(myClassName + ".getDocumentationElement() call stack =" + JavaUtils.callStackToString());
        }
        if (hasCachedDocElement()) {
            Element e = getCachedDocElement();
            if (log.isDebugEnabled()) {
                log.debug(myClassName + ".getDocumentationElement() from soft reference ");
            }
            return e;
        }
        if (isDebugEnabled) {
            log.debug(myClassName + ".getDocumentationElement() from reloaded wsdl Definition");
        }
        Definition def = null;
        try {
            def = loadDefinition();
        } catch (Exception e) {
            if (isDebugEnabled) {
                log.debug(myClassName + ".getDocumentationElement(): error trying to load Definition    [" + e.getClass().getName() + "]  error [" + e.getMessage() + "] ", e);
            }
        }
        if (def != null) {
            org.w3c.dom.Element docElement = def.getDocumentationElement();
            setCachedDocElement(docElement);
            setCachedTypes(def.getTypes());
            if (isDebugEnabled) {
                if (docElement != null) {
                    log.debug(myClassName + ".getDocumentationElement() from reloaded wsdl Definition returning  NON-NULL org.w3c.dom.Element");
                } else {
                    log.debug(myClassName + ".getDocumentationElement() from reloaded wsdl Definition returning  NULL org.w3c.dom.Element");
                }
            }
            return docElement;
        } else {
            if (isDebugEnabled) {
                log.debug(myClassName + ".getDocumentationElement() returning NULL");
            }
            return null;
        }
    }

    public void addExtensibilityElement(ExtensibilityElement extElement) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".addExtensibilityElement(" + extElement + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.addExtensibilityElement(extElement);
        }
    }

    public List getExtensibilityElements() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getExtensibilityElements()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensibilityElements();
        }
        return null;
    }

    public Binding createBinding() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createBinding()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBinding();
        }
        return null;
    }

    public BindingFault createBindingFault() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createBindingFault()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingFault();
        }
        return null;
    }

    public BindingInput createBindingInput() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createBindingInput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingInput();
        }
        return null;
    }

    public BindingOperation createBindingOperation() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createBindingOperation()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingOperation();
        }
        return null;
    }

    public BindingOutput createBindingOutput() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createBindingOutput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createBindingOutput();
        }
        return null;
    }

    public Fault createFault() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createFault()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createFault();
        }
        return null;
    }

    public Import createImport() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createImport()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createImport();
        }
        return null;
    }

    public Input createInput() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createInput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createInput();
        }
        return null;
    }

    public Message createMessage() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createMessage()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createMessage();
        }
        return null;
    }

    public Operation createOperation() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createOperation()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createOperation();
        }
        return null;
    }

    public Output createOutput() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createOutput()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createOutput();
        }
        return null;
    }

    public Part createPart() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createPart()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPart();
        }
        return null;
    }

    public Port createPort() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createPort()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPort();
        }
        return null;
    }

    public PortType createPortType() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createPortType()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createPortType();
        }
        return null;
    }

    public Service createService() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createService()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createService();
        }
        return null;
    }

    public Types createTypes() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".createTypes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.createTypes();
        }
        return null;
    }

    public void setExtensionRegistry(ExtensionRegistry extReg) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".setExtensionRegistry(" + extReg + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setExtensionRegistry(extReg);
        }
    }

    public ExtensionRegistry getExtensionRegistry() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getExtensionRegistry()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionRegistry();
        }
        return null;
    }

    public String toString() {
        if (wsdlDefinition != null) {
            return this.getClass().getName() + "\n" + wsdlDefinition.toString();
        }
        return this.getClass().getName();
    }

    public ExtensibilityElement removeExtensibilityElement(ExtensibilityElement extElement) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".removeExtensibilityElement(" + extElement + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.removeExtensibilityElement(extElement);
        }
        return null;
    }

    public java.lang.Object getExtensionAttribute(QName name) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getExtensionAttribute(" + name + ")");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionAttribute(name);
        }
        return null;
    }

    public Map getExtensionAttributes() {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getExtensionAttributes()");
        }
        if (wsdlDefinition != null) {
            return wsdlDefinition.getExtensionAttributes();
        }
        return null;
    }

    public void setExtensionAttribute(QName name, java.lang.Object value) {
        if (isDebugEnabled) {
            log.debug(myClassName + ".setExtensionAttribute(" + name + ",  " + value + ")");
        }
        if (wsdlDefinition != null) {
            wsdlDefinition.setExtensionAttribute(name, value);
        }
    }

    private String getExplicitURI(URL wsdlURL) throws WSDLException {
        if (isDebugEnabled) {
            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + ") ");
        }
        String explicitURI = null;
        ClassLoader classLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
        try {
            URL url = wsdlURL;
            String filePath = null;
            boolean isFileProtocol = (url != null && "file".equals(url.getProtocol())) ? true : false;
            if (isFileProtocol) {
                filePath = (url != null) ? url.getPath() : null;
                URI uri = null;
                if (url != null) {
                    uri = new URI(url.toString());
                }
                boolean isRelativePath = (filePath != null && !new File(filePath).isAbsolute()) ? true : false;
                if (isRelativePath) {
                    if (isDebugEnabled) {
                        log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): WSDL URL has a relative path");
                    }
                    url = getAbsoluteURL(classLoader, filePath);
                    if (url == null) {
                        if (isDebugEnabled) {
                            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "WSDL URL for relative path not found in ClassLoader");
                            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "Unable to read WSDL from relative path, check the relative path");
                            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "Relative path example: file:/WEB-INF/wsdl/<wsdlfilename>");
                            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "Using relative path as default wsdl URL to load wsdl Definition.");
                        }
                        url = wsdlURL;
                    } else {
                        if (isDebugEnabled) {
                            log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "WSDL URL found for relative path: " + filePath + " scheme: " + uri.getScheme());
                        }
                    }
                }
            }
            URLConnection urlCon = url.openConnection();
            InputStream is = null;
            try {
                is = getInputStream(urlCon);
            } catch (IOException e) {
                if (isDebugEnabled) {
                    log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "Could not open url connection. Trying to use " + "classloader to get another URL.");
                }
                if (filePath != null) {
                    url = getAbsoluteURL(classLoader, filePath);
                    if (url == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Could not locate URL for wsdl. Reporting error");
                        }
                        throw new WSDLException("WSDL4JWrapper : ", e.getMessage(), e);
                    } else {
                        urlCon = url.openConnection();
                        if (log.isDebugEnabled()) {
                            log.debug("Found URL for WSDL from jar");
                        }
                    }
                } else {
                    if (isDebugEnabled) {
                        log.debug(myClassName + ".getExplicitURI(" + wsdlURL + "): " + "Could not get URL from classloader. Reporting " + "error due to no file path.");
                    }
                    throw new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                }
            }
            if (is != null) {
                is.close();
            }
            explicitURI = urlCon.getURL().toString();
        } catch (Exception ex) {
            throw new WSDLException("WSDLWrapperReloadImpl : ", ex.getMessage(), ex);
        }
        return explicitURI;
    }

    private URL getAbsoluteURL(final ClassLoader classLoader, final String filePath) throws WSDLException {
        URL url = (URL) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return classLoader.getResource(filePath);
            }
        });
        if (url == null) {
            if (log.isDebugEnabled()) {
                log.debug("Could not get URL from classloader. Looking in a jar.");
            }
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlLoader = (URLClassLoader) classLoader;
                url = getURLFromJAR(urlLoader, wsdlURL);
            }
        }
        return url;
    }

    /**
     * Load and Return a Definition object.
     * (The caller will determine if the Definition object should have 
     * its resources freed or not)
     * @return Definition
     * @throws WSDLException
     */
    private Definition loadDefinition() throws WSDLException {
        Definition def = null;
        if (wsdlExplicitURI != null) {
            try {
                def = (Definition) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                    public Object run() throws WSDLException {
                        WSDLReader reader = getWSDLReader();
                        return reader.readWSDL(wsdlExplicitURI);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (isDebugEnabled) {
                    log.debug(myClassName + ".loadDefinition(): " + "Exception thrown from AccessController: " + e);
                    log.trace("Call Stack = " + JavaUtils.callStackToString());
                }
                WSDLException we = new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                throw we;
            }
        }
        if (isDebugEnabled) {
            log.debug(myClassName + ".loadDefinition():  returning Definition [" + def + "]");
            log.trace("Call Stack = " + JavaUtils.callStackToString());
        }
        return def;
    }

    private URL getURLFromJAR(URLClassLoader urlLoader, URL relativeURL) throws WSDLException {
        URL[] urlList = urlLoader.getURLs();
        if (urlList == null) {
            return null;
        }
        for (int i = 0; i < urlList.length; i++) {
            URL url = urlList[i];
            if (url == null) {
                return null;
            }
            if ("file".equals(url.getProtocol())) {
                File f = new File(url.getPath());
                if (f.exists() && !f.isDirectory()) {
                    try {
                        JarFile jf = new JarFile(f);
                        Enumeration entries = jf.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry je = (JarEntry) entries.nextElement();
                            String name = je.getName();
                            if (name.endsWith(".wsdl")) {
                                String relativePath = relativeURL.getPath();
                                if (relativePath.endsWith(name)) {
                                    String path = f.getAbsolutePath();
                                    if (path != null && path.indexOf("/") == 0) {
                                        path = path.substring(1, path.length());
                                    }
                                    URL absoluteUrl = new URL("jar:file:/" + path + "!/" + je.getName());
                                    return absoluteUrl;
                                }
                            }
                        }
                    } catch (Exception e) {
                        WSDLException we = new WSDLException("WSDLWrapperReloadImpl : ", e.getMessage(), e);
                        throw we;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a wsdl reader for the wsdl
     * 
     * @return WSDLReader
     * @exception WSDLException
     */
    private WSDLReader getWSDLReader() throws WSDLException {
        WSDLReader reader;
        try {
            reader = (WSDLReader) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws WSDLException {
                    WSDLFactory factory = WSDLFactory.newInstance();
                    return factory.newWSDLReader();
                }
            });
        } catch (PrivilegedActionException e) {
            throw (WSDLException) e.getException();
        }
        reader.setFeature(com.ibm.wsdl.Constants.FEATURE_VERBOSE, false);
        return reader;
    }

    /**
     * This method provides a Java2 Security compliant way to obtain the InputStream
     * for a given URLConnection object. This is needed as a given URLConnection object
     * may be an instance of a FileURLConnection object which would require access 
     * permissions if Java2 Security was enabled.
     */
    private InputStream getInputStream(URLConnection urlCon) throws Exception {
        final URLConnection finalURLCon = urlCon;
        InputStream is = null;
        try {
            is = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws IOException {
                    return finalURLCon.getInputStream();
                }
            });
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
        return is;
    }
}
