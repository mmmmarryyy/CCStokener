package com.novocode.naf.resource.xml;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import com.novocode.naf.app.*;
import com.novocode.naf.resource.ConfigurableFactory;
import com.novocode.naf.resource.ConfigurableObject;
import com.novocode.naf.resource.Import;
import com.novocode.naf.resource.NGNode;
import com.novocode.naf.resource.Resource;
import com.novocode.naf.resource.ResourceLoader;
import com.novocode.naf.resource.ResourceManager;
import com.novocode.naf.resource.ConfPropertyDescriptor;
import com.novocode.naf.resource.ConfPropertyManager;
import com.novocode.naf.xml.DOMUtil;

/**
 * Reads NAF resource files and creates Objects from them.
 * 
 * @author Stefan Zeiger (szeiger@novocode.com)
 * @since Nov 24, 2003
 * @version $Id: XMLResourceLoader.java 408 2008-05-02 22:13:45Z szeiger $
 */
public final class XMLResourceLoader implements ResourceLoader {

    public static final String NAF_NAMESPACE_URI = "http://www.novocode.com/namespaces/naf";

    private static final Logger LOGGER_DUMP = LoggerFactory.getLogger(XMLResourceLoader.class.getName() + ".dump");

    private final DocumentBuilder docbuilder;

    private final StyleManager styleManager = new StyleManager();

    private final ResourceManager resourceManager;

    public XMLResourceLoader(ResourceManager resourceManager) throws NAFException {
        this.resourceManager = resourceManager;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            this.docbuilder = factory.newDocumentBuilder();
        } catch (Exception ex) {
            throw new NAFException("Error creating XMLResourceLoader", ex);
        }
    }

    public Resource readResource(URL url, ResourceManager resourceManager) throws NAFException {
        XMLResource resource = new XMLResource(resourceManager, url);
        InputStream in = null;
        try {
            in = url.openStream();
            ArrayList<Transformer> trList = null;
            Document doc = docbuilder.parse(in);
            for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE && "xml-stylesheet".equals(n.getNodeName())) {
                    ProcessingInstruction pi = (ProcessingInstruction) n;
                    Map<String, String> attrs = DOMUtil.parseProcessingInstructionAttributes(pi);
                    if ("text/xsl".equals(attrs.get("type"))) {
                        String href = attrs.get("href");
                        if (href == null) throw new NAFException("Style sheet processing instructions must have an \"href\" attribute");
                        try {
                            Transformer t = styleManager.createTransformer(new URL(url, href));
                            if (trList == null) trList = new ArrayList<Transformer>();
                            trList.add(t);
                        } catch (Exception ex) {
                            throw new NAFException("Error reading style sheet resource \"" + href + "\"");
                        }
                    }
                }
            }
            if (trList != null) {
                for (Transformer t : trList) {
                    doc = (Document) styleManager.transform(t, doc);
                    if (LOGGER_DUMP.isDebugEnabled()) {
                        StringWriter swr = new StringWriter();
                        DOMUtil.dumpNode(doc, swr);
                        LOGGER_DUMP.debug("Transformed instance:\n" + swr + "\n");
                    }
                }
            }
            Element rootE = doc.getDocumentElement();
            if (!NAF_NAMESPACE_URI.equals(rootE.getNamespaceURI())) throw new NAFException("Root element does not use the NAF namespace");
            Object comp = createComponent(rootE, resource, null);
            resource.setRootObject(comp);
            return resource;
        } catch (Exception ex) {
            throw new NAFException("Error reading NAF resource \"" + url.toExternalForm() + "\"", ex);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    private Object createComponent(Element e, XMLResource resource, ConfigurableObject parent) throws NAFException {
        String name = e.getLocalName();
        Class<?> cl = resource.resolveLocalName(name);
        if (cl == null) cl = resourceManager.resolveGlobalName(name);
        if (cl == null) throw new NAFException("Unknown element tag \"" + name + "\"");
        Object o = createDirectComponent(cl, e, resource, parent, false);
        if (o instanceof NGNode) {
            String id = ((NGNode) o).getID();
            if (id != null) resource.putObject(id, o);
        }
        return o;
    }

    private void addDirectComponent(ConfPropertyDescriptor pd, Element e, XMLResource resource, ConfigurableObject target) throws NAFException {
        Object ch;
        Class<? extends ConfigurableFactory> facClass = pd.getFactory();
        if (facClass != null) {
            ConfigurableFactory fac = createDirectComponent(facClass, e, resource, target, true);
            ch = fac.createInstance();
            if (ch instanceof ConfigurableObject) initObject(((ConfigurableObject) ch), e, resource, target, false);
        } else {
            if (pd.isMap()) {
                ch = pd.getValue(target);
                NamedNodeMap nnm = e.getAttributes();
                int nnmLength = nnm.getLength();
                for (int i = 0; i < nnmLength; i++) {
                    Node n = nnm.item(i);
                    ch = pd.addMapValue(target, resource, ch, n.getNodeName(), n.getNodeValue());
                }
            } else ch = createDirectComponent(pd.getInstanceType(), e, resource, target, false);
        }
        pd.addCompound(target, ch);
    }

    @SuppressWarnings("unchecked")
    private <T> T createDirectComponent(Class<T> clazz, Element e, XMLResource resource, ConfigurableObject parent, boolean ignoreUnknownElements) throws NAFException {
        T co;
        try {
            co = clazz.newInstance();
        } catch (Exception ex) {
            throw new NAFException("Error instantiating element object from class " + clazz.getName(), ex);
        }
        if (co instanceof ConfigurableObject) initObject(((ConfigurableObject) co), e, resource, parent, ignoreUnknownElements);
        return co;
    }

    private void initObject(ConfigurableObject o, Element thisEl, XMLResource resource, ConfigurableObject parent, boolean ignoreUnknownElements) throws NAFException {
        ConfPropertyManager propertyManager = ConfPropertyManager.forClass(o.getClass());
        if (propertyManager == null) return;
        Set<String> required = propertyManager.getRequiredCopy();
        ConfPropertyDescriptor pd = propertyManager.getAttributePropertyDescriptor(":resource", '.');
        if (pd != null) pd.setValue(o, resource);
        pd = propertyManager.getAttributePropertyDescriptor(":elementName", '.');
        if (pd != null) pd.setValue(o, thisEl.getNodeName());
        pd = propertyManager.getAttributePropertyDescriptor(":parent", '.');
        if (pd != null) pd.setValue(o, parent);
        for (Node n = thisEl.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && XMLResourceLoader.NAF_NAMESPACE_URI.equals(n.getNamespaceURI())) {
                Element e = (Element) n;
                String ln = e.getLocalName();
                if (ln.equals("import")) resource.addImport(new Import(e)); else if ((pd = propertyManager.getElementPropertyDescriptor(ln)) != null) addDirectComponent(pd, e, resource, o); else {
                    if ((pd = propertyManager.getNestedElementsPropertyDescriptor()) != null) pd.addCompound(o, createComponent(e, resource, o)); else if (!ignoreUnknownElements) throw new NAFException("Unrecognized child element " + e.getLocalName() + " in " + o.getClass().getName());
                }
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                if ((pd = propertyManager.getAttributePropertyDescriptor(":text", '.')) != null) pd.addCompound(o, ((Text) n).getData());
            }
        }
        NamedNodeMap attrs = thisEl.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            if (a.getNamespaceURI() != null) continue;
            String name = a.getLocalName();
            if ((pd = propertyManager.getAttributePropertyDescriptor(name, '.')) != null) {
                if (pd.isCompound()) {
                    Object map = pd.getValue(o);
                    map = pd.addMapValue(o, resource, map, pd.removePrefixFrom(name), a.getValue());
                    pd.addCompound(o, map);
                } else {
                    pd.setSimple(o, a.getValue());
                    if (required != null) required.remove(pd.getXMLPropertyName());
                }
            }
        }
        if (required != null && required.size() > 0) {
            String s = new ArrayList<String>(required).toString();
            throw new NAFException("Required attributes missing for " + o.getClass().getName() + ": " + s);
        }
    }
}
