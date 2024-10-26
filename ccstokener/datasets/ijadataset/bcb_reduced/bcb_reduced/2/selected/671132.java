package org.wam.parser;

import java.io.IOException;
import java.io.Reader;
import org.dom4j.Element;
import org.wam.core.*;
import org.wam.style.StyleDomain;

/**
 * Parses WAM components using the DOM4J library
 */
public class WamDomParser implements WamParser {

    private org.dom4j.DocumentFactory theDF;

    java.util.HashMap<String, WamToolkit> theToolkits;

    /**
	 * Creates a WAM parser
	 */
    public WamDomParser() {
        this(org.dom4j.DocumentFactory.getInstance());
        theToolkits = new java.util.HashMap<String, WamToolkit>();
    }

    /**
	 * Creates a WAM parser with a non-default document factory
	 * 
	 * @param factory The document factory to use instead of the default
	 */
    public WamDomParser(org.dom4j.DocumentFactory factory) {
        theDF = factory;
    }

    public WamToolkit getToolkit(String uri, WamDocument doc) throws WamParseException, IOException {
        WamToolkit ret = theToolkits.get(uri);
        if (ret != null) return ret;
        java.net.URL url;
        try {
            url = new java.net.URL(uri);
        } catch (java.net.MalformedURLException e) {
            throw new WamParseException("Could not parse URL " + uri, e);
        }
        Element rootEl;
        try {
            rootEl = new org.dom4j.io.SAXReader(theDF).read(new java.io.InputStreamReader(url.openStream())).getRootElement();
        } catch (org.dom4j.DocumentException e) {
            throw new WamParseException("Could not parse toolkit XML for " + uri, e);
        }
        String name = rootEl.elementTextTrim("name");
        if (name == null) throw new WamParseException("No name element for toolkit at " + uri);
        String descrip = rootEl.elementTextTrim("description");
        if (descrip == null) throw new WamParseException("No description element for toolkit at " + uri);
        String version = rootEl.elementTextTrim("version");
        if (version == null) throw new WamParseException("No version element for toolkit at " + uri);
        if (doc == null || doc.getDefaultToolkit() == null) ret = new WamToolkit(url, name, descrip, version); else ret = new WamToolkit(doc.getDefaultToolkit(), url, name, descrip, version);
        for (Element el : (java.util.List<Element>) rootEl.elements()) {
            String elName = el.getName();
            if (elName.equals("name") || elName.equals("descrip") || elName.equals("version")) continue;
            if (elName.equals("dependencies")) {
                for (Element dEl : (java.util.List<Element>) el.elements()) {
                    if (!dEl.getName().equals("depends")) throw new WamParseException("Illegal element under " + elName);
                    if (doc == null || doc.getDefaultToolkit() == null) throw new WamParseException("Default toolkit cannot have dependencies");
                    WamToolkit dependency = getToolkit(dEl.getTextTrim(), doc);
                    try {
                        ret.addDependency(dependency);
                    } catch (WamException e) {
                        throw new WamParseException("Toolkit is already sealed?", e);
                    }
                }
            } else if (elName.equals("types")) {
                for (Element tEl : (java.util.List<Element>) el.elements()) {
                    if (!tEl.getName().equals("type")) throw new WamParseException("Illegal element under " + elName);
                    String tagName = tEl.attributeValue("tag");
                    if (tagName == null) throw new WamParseException("tag attribute expected for " + tEl.getName() + " element");
                    String className = tEl.getTextTrim();
                    if (className == null || className.length() == 0) throw new WamParseException("Class name expected for element " + tEl.getName());
                    try {
                        ret.map(tagName, className);
                    } catch (WamException e) {
                        throw new WamParseException("Toolkit is already sealed?", e);
                    }
                }
            } else if (elName.equals("security")) {
                for (Element pEl : (java.util.List<Element>) el.elements()) {
                    if (!pEl.getName().equals("permission")) throw new WamParseException("Illegal element under " + elName);
                    String typeName = pEl.attributeValue("type");
                    if (typeName == null) throw new WamParseException("No type name in permission element");
                    typeName = typeName.toLowerCase();
                    int idx = typeName.indexOf("/");
                    String subTypeName = null;
                    if (idx >= 0) {
                        subTypeName = typeName.substring(idx + 1).trim();
                        typeName = typeName.substring(0, idx).trim();
                    }
                    WamPermission.Type type = WamPermission.Type.byKey(typeName);
                    if (type == null) throw new WamParseException("No such permission type: " + typeName);
                    WamPermission.SubType[] allSubTypes = type.getSubTypes();
                    WamPermission.SubType subType = null;
                    if (allSubTypes != null && allSubTypes.length > 0) {
                        if (subType == null) throw new WamParseException("No sub-type specified for permission type " + type);
                        for (WamPermission.SubType st : allSubTypes) if (st.getKey().equals(subTypeName)) subType = st;
                        if (subType == null) throw new WamParseException("No such sub-type " + subTypeName + " for permission type " + type);
                    } else if (subTypeName != null) throw new WamParseException("No sub-types exist (such as " + subTypeName + ") for permission type " + type);
                    boolean req = "true".equalsIgnoreCase(pEl.attributeValue("required"));
                    String explanation = pEl.getTextTrim();
                    String[] params = new String[subType == null ? 0 : subType.getParameters().length];
                    if (subType != null) for (int p = 0; p < subType.getParameters().length; p++) {
                        params[p] = pEl.attributeValue(subType.getParameters()[p].getKey());
                        String val = subType.getParameters()[p].validate(params[p]);
                        if (val != null) throw new WamParseException("Invalid parameter " + subType.getParameters()[p].getName() + ": " + val);
                    }
                    try {
                        ret.addPermission(new WamPermission(type, subType, params, req, explanation));
                    } catch (WamException e) {
                        throw new WamParseException("Unexpected WAM Exception: toolkit is sealed?", e);
                    }
                }
            } else throw new WamParseException("Illegal element under " + elName);
        }
        return ret;
    }

    public WamDocument parseDocument(Reader reader, org.wam.core.WamDocument.GraphicsGetter graphics) throws WamParseException, IOException {
        WamToolkit dt = getToolkit(WamDocument.class.getResource("/WAMRegistry.xml").getQuery(), null);
        Element rootEl;
        try {
            rootEl = new org.dom4j.io.SAXReader(theDF).read(reader).getRootElement();
        } catch (org.dom4j.DocumentException e) {
            throw new WamParseException("Could not parse document XML", e);
        }
        WamDocument doc = new WamDocument(graphics);
        doc.initDocument(this, dt);
        initClassView(doc, rootEl);
        Element[] head = (Element[]) rootEl.elements("head").toArray(new Element[0]);
        if (head.length > 1) doc.error("Multiple head elements in document XML", null);
        if (head.length > 0) {
            String title = head[0].elementTextTrim("title");
            if (title != null) doc.getHead().setTitle(title);
            Element[] styles = (Element[]) head[0].elements("style").toArray(new Element[0]);
            for (Element style : styles) applyStyle(doc, style);
        }
        Element[] body = (Element[]) rootEl.elements("body").toArray(new Element[0]);
        if (body.length > 1) doc.error("Multiple body elements in document XML", null);
        if (body.length == 0) throw new WamParseException("No body in document XML");
        for (Element el : (java.util.List<Element>) rootEl.elements()) {
            if (el.getName().equals("head") || el.getName().equals("body")) continue;
            doc.error("Extra element " + el.getName() + " in document XML", null);
        }
        WamClassView classView = getClassView(doc.getRoot(), body[0]);
        doc.getRoot().init(doc, doc.getDefaultToolkit(), classView, null, null, body[0].getName());
        applyAttributes(doc.getRoot(), body[0]);
        WamElement[] content = parseContent(body[0], doc.getRoot());
        doc.getRoot().initChildren(content);
        return doc;
    }

    /**
	 * Initialized aclass view for a new document
	 * 
	 * @param wam The docuent to modify the class view for
	 * @param xml The xml element to get namespaces to map
	 */
    protected void initClassView(WamDocument wam, Element xml) {
        WamClassView ret = wam.getClassView();
        for (org.dom4j.Namespace ns : (java.util.List<org.dom4j.Namespace>) xml.declaredNamespaces()) {
            WamToolkit toolkit;
            try {
                toolkit = getToolkit(ns.getURI(), wam);
            } catch (IOException e) {
                wam.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            } catch (WamParseException e) {
                wam.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            }
            try {
                ret.addNamespace(ns.getPrefix(), toolkit);
            } catch (WamException e) {
                wam.error("Could not add namespace", e);
            }
        }
        ret.seal();
    }

    /**
	 * Applies the style element to a document
	 * 
	 * @param doc The document containing the styles to apply
	 * @param style The XML element containing the style information
	 */
    protected void applyStyle(WamDocument doc, Element style) {
        java.util.Map<String, WamToolkit> styleNamespaces = new java.util.HashMap<String, WamToolkit>();
        applyNamespaces(doc, style, styleNamespaces);
        for (Element groupEl : (java.util.List<Element>) style.elements("group")) {
            java.util.Map<String, WamToolkit> groupNamespaces = new java.util.HashMap<String, WamToolkit>();
            applyNamespaces(doc, groupEl, groupNamespaces);
            String name = groupEl.attributeValue("name");
            if (name == null) name = "";
            org.wam.style.NamedStyleGroup group = doc.getGroup(name);
            applyStyleGroupAttribs(doc, group, groupEl, styleNamespaces, groupNamespaces);
        }
    }

    /**
	 * Parses namespaces associated with a style element
	 * 
	 * @param doc The WAM document that the style is for
	 * @param xml The XML element to parse the namespaces from
	 * @param namespaces The map to put the toolkits mapped to namespaces in the XML element
	 */
    protected void applyNamespaces(WamDocument doc, Element xml, java.util.Map<String, WamToolkit> namespaces) {
        for (org.dom4j.Namespace ns : (java.util.List<org.dom4j.Namespace>) xml.declaredNamespaces()) {
            WamToolkit toolkit;
            try {
                toolkit = getToolkit(ns.getURI(), doc);
            } catch (IOException e) {
                doc.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            } catch (WamParseException e) {
                doc.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            }
            namespaces.put(ns.getPrefix(), toolkit);
        }
    }

    private <T extends WamElement> void applyStyleGroupAttribs(WamDocument doc, org.wam.style.TypedStyleGroup<T> group, Element groupEl, java.util.Map<String, WamToolkit> styleNamespaces, java.util.Map<String, WamToolkit> groupNamespaces) {
        for (org.dom4j.Attribute attr : (java.util.List<org.dom4j.Attribute>) groupEl.attributes()) {
            String fullDomain = attr.getQualifiedName();
            String ns = attr.getQName().getNamespacePrefix();
            String domainName = attr.getName();
            int idx = domainName.indexOf(".");
            String attrName = null;
            if (idx >= 0) {
                attrName = domainName.substring(idx + 1);
                domainName = domainName.substring(0, idx);
            }
            WamToolkit toolkit = getToolkit(doc, styleNamespaces, groupNamespaces, ns);
            if (toolkit == null) {
                doc.error("No toolkit mapped to namespace " + ns, null);
                continue;
            }
            String domainType = toolkit.getMappedClass(domainName);
            if (domainType == null) {
                doc.error("No such style domain " + fullDomain + " in toolkit " + toolkit.getName(), null);
                continue;
            }
            Class<? extends org.wam.style.StyleDomain> domainClass;
            try {
                domainClass = toolkit.loadClass(domainType, org.wam.style.StyleDomain.class);
            } catch (WamException e) {
                doc.error("Could not load style domain " + domainType, e);
                continue;
            }
            org.wam.style.StyleDomain domain;
            try {
                domain = (org.wam.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null, new Object[0]);
            } catch (Exception e) {
                doc.error("Could not get domain instance", e);
                continue;
            }
            if (attrName != null) applyStyleAttribute(group, domain, attrName, attr.getValue(), doc); else applyStyleSet(group, domain, attr.getValue(), doc);
            for (Element typeEl : (java.util.List<Element>) groupEl.elements()) {
                String fullTypeName = typeEl.getQualifiedName();
                String typeNS = typeEl.getQName().getNamespacePrefix();
                String typeName = typeEl.getName();
                WamToolkit typeToolkit = getToolkit(doc, styleNamespaces, groupNamespaces, typeNS);
                if (typeToolkit == null) {
                    doc.error("No toolkit mapped to namespace " + typeNS, null);
                    continue;
                }
                String javaTypeName = typeToolkit.getMappedClass(typeName);
                if (javaTypeName == null) {
                    doc.error("No such element type " + fullTypeName + " in toolkit " + typeToolkit.getName(), null);
                    continue;
                }
                Class<? extends WamElement> typeClass;
                try {
                    typeClass = typeToolkit.loadClass(javaTypeName, WamElement.class);
                } catch (WamException e) {
                    doc.error("Could not load element class " + javaTypeName, e);
                    continue;
                }
                if (!group.getType().isAssignableFrom(typeClass)) {
                    doc.error("Element type " + javaTypeName + " is not a subtype of " + group.getType().getName(), null);
                    continue;
                }
                org.wam.style.TypedStyleGroup<? extends WamElement> subGroup = group.insertTypedGroup(typeClass.asSubclass(group.getType()));
                applyStyleGroupAttribs(doc, subGroup, typeEl, styleNamespaces, groupNamespaces);
            }
        }
    }

    @SuppressWarnings("unchecked")
    void applyStyleAttribute(org.wam.style.WamStyle style, StyleDomain domain, String attrName, String valueStr, WamMessage.WamMessageCenter messager) {
        org.wam.style.StyleAttribute styleAttr = null;
        for (org.wam.style.StyleAttribute attrib : domain) if (attrib.name.equals(attrName)) styleAttr = attrib;
        if (styleAttr == null) {
            messager.warn("No such attribute " + attrName + " in domain " + domain.getName());
            return;
        }
        Object value;
        try {
            value = styleAttr.parse(valueStr);
        } catch (WamException e) {
            messager.warn("Value " + valueStr + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName(), e);
            return;
        }
        String error = styleAttr.validate(value);
        if (error != null) {
            messager.warn("Value " + valueStr + " is not appropriate for style attribute " + attrName + " of domain " + domain.getName() + ": " + error);
            return;
        }
        style.set(styleAttr, value);
    }

    private void applyStyleSet(org.wam.style.WamStyle style, StyleDomain domain, String valueStr, WamMessage.WamMessageCenter messager) {
        if (valueStr.length() < 2 || valueStr.charAt(0) != '{' || valueStr.charAt(1) != '}') {
            messager.warn("When only a domain is specified, styles must be in the form" + " {property:value, property:value}");
            return;
        }
        String[] propEntries = valueStr.substring(1, valueStr.length() - 1).split(",");
        for (String propEntry : propEntries) {
            int idx = propEntry.indexOf(':');
            if (idx < 0) {
                messager.warn("Bulk style setting " + propEntry.trim() + " is missing a colon");
                continue;
            }
            String attrName = propEntry.substring(0, idx).trim();
            String propVal = propEntry.substring(idx + 1).trim();
            applyStyleAttribute(style, domain, attrName, propVal, messager);
        }
    }

    private WamToolkit getToolkit(WamDocument doc, java.util.Map<String, WamToolkit> styleNS, java.util.Map<String, WamToolkit> groupNS, String ns) {
        if (groupNS.containsKey(ns)) return groupNS.get(ns); else if (styleNS.containsKey(ns)) return styleNS.get(ns); else return doc.getClassView().getToolkit(ns);
    }

    public WamElement[] parseContent(Reader reader, WamElement parent, boolean useRootAttrs) throws IOException, WamParseException {
        Element rootEl;
        try {
            rootEl = new org.dom4j.io.SAXReader(theDF).read(reader).getRootElement();
        } catch (org.dom4j.DocumentException e) {
            throw new WamParseException("Could not parse document XML", e);
        }
        if (useRootAttrs) applyAttributes(parent, rootEl);
        return parseContent(rootEl, parent);
    }

    /**
	 * Creates a fully-initialized class view for a new element
	 * 
	 * @param wam The element to create the class view for
	 * @param xml The xml element to get namespaces to map
	 * @return The class view for the element
	 */
    protected WamClassView getClassView(WamElement wam, Element xml) {
        WamClassView ret = new WamClassView(wam);
        for (org.dom4j.Namespace ns : (java.util.List<org.dom4j.Namespace>) xml.declaredNamespaces()) {
            WamToolkit toolkit;
            try {
                toolkit = getToolkit(ns.getURI(), wam.getDocument());
            } catch (IOException e) {
                wam.error("Could not read toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            } catch (WamParseException e) {
                wam.error("Could not parse toolkit " + ns.getPrefix() + ":" + ns.getURI(), e);
                continue;
            }
            try {
                ret.addNamespace(ns.getPrefix(), toolkit);
            } catch (WamException e) {
                wam.error("Could not add namespace", e);
            }
        }
        ret.seal();
        return ret;
    }

    /**
	 * Applies attributes in an XML element to a WAM element
	 * 
	 * @param wam The WAM element to apply the attributes to
	 * @param xml The XML element to get the attributes from
	 */
    protected void applyAttributes(WamElement wam, Element xml) {
        for (org.dom4j.Attribute attr : (java.util.List<org.dom4j.Attribute>) xml.attributes()) {
            if (attr.getName().matches("style[0-9]*")) applyElementStyle(wam, attr.getValue()); else if (attr.getName().matches("group[0-9]*")) applyElementGroups(wam, attr.getValue()); else if (attr.getName().matches("attach[0-9]*")) applyElementAttaches(wam, attr.getValue());
            try {
                wam.setAttribute(attr.getName(), attr.getValue());
            } catch (WamException e) {
                wam.error("Could not set attribute--stage is not init?", e);
            }
        }
    }

    /**
	 * Applies a style attribute's value to an element
	 * 
	 * @param wam The element to apply the style attributes to
	 * @param styleValue The style-attribute value containing the style to apply
	 */
    protected void applyElementStyle(WamElement wam, String styleValue) {
        String[] styles = styleValue.split(";");
        for (String style : styles) {
            int equalIdx = style.indexOf("=");
            if (equalIdx < 0) {
                wam.error("Invalid style: " + style + ".  No '='", null);
                continue;
            }
            String attr = style.substring(0, equalIdx).trim();
            String valueStr = style.substring(equalIdx + 1).trim();
            String ns, domainName, attrName;
            int nsIdx = attr.indexOf(':');
            if (nsIdx >= 0) {
                ns = attr.substring(0, nsIdx).trim();
                domainName = attr.substring(nsIdx + 1).trim();
            } else {
                ns = null;
                domainName = attr;
            }
            WamToolkit toolkit = wam.getClassView().getToolkit(ns);
            if (toolkit == null) {
                wam.warn("No toolkit mapped to namespace " + ns + " for style " + style);
                continue;
            }
            int dotIdx = domainName.indexOf('.');
            if (dotIdx >= 0) {
                attrName = domainName.substring(dotIdx + 1).trim();
                domainName = domainName.substring(0, dotIdx).trim();
            } else attrName = null;
            String domainClassName = toolkit.getMappedClass(domainName);
            if (domainClassName == null) {
                wam.warn("No style domain mapped to " + domainName + " in toolkit " + toolkit.getName());
                continue;
            }
            Class<? extends org.wam.style.StyleDomain> domainClass;
            try {
                domainClass = toolkit.loadClass(domainClassName, org.wam.style.StyleDomain.class);
            } catch (WamException e) {
                wam.warn("Could not load domain class " + domainClassName + " from toolkit " + toolkit.getName(), e);
                continue;
            }
            org.wam.style.StyleDomain domain;
            try {
                domain = (org.wam.style.StyleDomain) domainClass.getMethod("getDomainInstance", new Class[0]).invoke(null, new Object[0]);
            } catch (Exception e) {
                wam.warn("Could not get domain instance", e);
                continue;
            }
            if (attrName != null) applyStyleAttribute(wam.getStyle(), domain, attrName, valueStr, wam); else applyStyleSet(wam.getStyle(), domain, valueStr, wam);
        }
    }

    /**
	 * Adds an element to groups by name
	 * 
	 * @param element The element to add to the groups
	 * @param groupValue The names of the groups, separated by whitespace
	 */
    protected void applyElementGroups(WamElement element, String groupValue) {
        String[] groupNames = groupValue.split("\\w*");
        for (String name : groupNames) {
            org.wam.style.NamedStyleGroup group = element.getDocument().getGroup(name);
            if (group == null) {
                element.warn("No such group named \"" + name + "\"");
                continue;
            }
            element.getStyle().addGroup(group);
        }
    }

    /**
	 * Attaches {@link WamElementAttachment}s to an element
	 * 
	 * @param wam The element to attach to
	 * @param attachAttr The attribute value containing the attachments
	 */
    protected void applyElementAttaches(WamElement wam, String attachAttr) {
        String[] attaches = attachAttr.split("\\w*,\\w*");
        for (String attach : attaches) {
            String ns = null, tag = attach;
            int index = attach.indexOf(":");
            if (index >= 0) {
                ns = attach.substring(0, index);
                tag = attach.substring(index + 1);
            }
            WamToolkit toolkit = wam.getClassView().getToolkit(ns);
            if (toolkit == null) {
                wam.error("No such toolkit mapped to namespace \"" + ns + "\"", null);
                continue;
            }
            String attachClassName = toolkit.getMappedClass(tag);
            if (attachClassName == null) {
                wam.warn("No attachment class mapped to " + tag + " in toolkit " + toolkit.getName());
                continue;
            }
            Class<? extends WamElementAttachment> clazz;
            try {
                clazz = toolkit.loadClass(attachClassName, WamElementAttachment.class);
            } catch (WamException e) {
                wam.warn("Could not load attachment class " + attachClassName + " from toolkit " + toolkit.getName(), e);
                continue;
            }
            WamElementAttachment attachInstance;
            try {
                attachInstance = clazz.newInstance();
            } catch (Throwable e) {
                wam.error("Could not instantiate attachment class " + attachClassName, e);
                continue;
            }
            attachInstance.attach(wam);
        }
    }

    /**
	 * Parses WAM content from an XML element
	 * 
	 * @param xml The XML element to parse the content of
	 * @param parent The parent element whose content to parse
	 * @return The parsed and initialized content
	 */
    protected WamElement[] parseContent(Element xml, WamElement parent) {
        WamElement[] ret = new WamElement[0];
        for (Element child : (java.util.List<Element>) xml.elements()) {
            WamElement newChild;
            try {
                newChild = createElement(child, parent);
            } catch (WamParseException e) {
                parent.error("Could not create WAM element for " + xml.getQualifiedName(), e);
                continue;
            }
            applyAttributes(newChild, child);
            ret = prisms.util.ArrayUtils.add(ret, newChild);
            WamElement[] subContent = parseContent(child, newChild);
            newChild.initChildren(subContent);
        }
        return ret;
    }

    /**
	 * Creates an element, initializing it but not its content
	 * 
	 * @param xml The XML element representing the element to create
	 * @param parent The parent for the element to create
	 * @return The new element
	 * @throws WamParseException If an error occurs creating the element
	 */
    protected WamElement createElement(Element xml, WamElement parent) throws WamParseException {
        String ns = xml.getNamespacePrefix();
        if (ns.length() == 0) ns = null;
        WamToolkit toolkit = parent.getClassView().getToolkit(ns);
        if (toolkit == null && ns == null) toolkit = parent.getDocument().getDefaultToolkit();
        if (toolkit == null) throw new WamParseException("No WAM toolkit mapped to namespace " + ns);
        String className = toolkit.getMappedClass(xml.getName());
        if (className == null) throw new WamParseException("No tag name " + xml.getName() + " mapped for namespace " + ns);
        Class<? extends WamElement> wamClass;
        try {
            wamClass = parent.getToolkit().loadClass(className, WamElement.class);
        } catch (WamException e) {
            throw new WamParseException("Could not load WAM element class " + className, e);
        }
        WamElement ret;
        try {
            ret = wamClass.newInstance();
        } catch (Throwable e) {
            throw new WamParseException("Could not instantiate WAM element class " + className, e);
        }
        WamClassView classView = getClassView(ret, xml);
        ret.init(parent.getDocument(), toolkit, classView, parent, ns, xml.getName());
        return ret;
    }
}
