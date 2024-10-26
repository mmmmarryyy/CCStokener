package org.ufp.freecard.xmlstore;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.net.*;
import java.util.*;
import org.apache.commons.logging.*;
import org.jdom.*;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.ufp.freecard.runtime.*;
import org.ufp.freecard.runtime.Stack;
import org.ufp.freecard.runtime.io.storage.DataStore;
import org.ufp.freecard.runtime.service.FCService;

/** <P>This class provides the methods to read and write a data in XML
 * format.</P>
 *
 * <P>This implementation does not support concurrent modifications by
 * multiple users but does allow multiple users to simultaneously read
 * the file.</P>
 * 
 * @author <a href="mailto:adrian@intencha.com">Adrian Sutton</a>
 * @version $Revision: 1.6 $
 */
public class XMLStore extends DataStore {

    /** The namespace for FreeCard XML files. */
    public static final Namespace FC_NAMESPACE = Namespace.getNamespace("fc", "http://www.freecard.org/2003/FCProject");

    /** The XML parser feature for schema validation. */
    private static final String SCHEMA_VALIDATION = "http://apache.org/xml/features/validation/schema";

    /** The XML document used as the backing store for this DataStore. */
    private Document _document;

    /** The log object used by this class. */
    private static final Log log = LogFactory.getFactory().getInstance(XMLStore.class);

    /** The SAXBuilder to use for creating documents. */
    private SAXBuilder _builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);

    /** The cache of created FCObjects. */
    private Map _cache = new HashMap();

    /** The caching factory used to create the JDOM nodes for the document. */
    private CachingJDOMFactory _factory = new CachingJDOMFactory();

    /** Creates a new XMLStore and performs basic initialization.
     */
    public XMLStore() {
        _builder.setFeature(SCHEMA_VALIDATION, true);
        _builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation", FC_NAMESPACE.getURI() + " " + getClass().getResource("fcstack.xsd"));
        _builder.setFactory(_factory);
    }

    /** @see org.ufp.freecard.runtime.io.storage.DataStore#getFCObject()
     */
    public FCObject getFCObject() throws FCException {
        Element root = _document.getRootElement();
        return getObjectFor(root, null);
    }

    /** @see DataStore#getFCObjectByID(org.ufp.freecard.runtime.FCValue)
     */
    public FCObject getFCObjectByID(FCValue id) throws FCException {
        return getObjectFor(_factory.getElementWithID(id.getAsString()));
    }

    /** Returns the FCObject for the specified element.  This method handles the work of finding
     * the FCObject representing the parent then passes the work of creating the element itself
     * to {@link #getObjectFor(Element, FCObject)}.
     * 
     * @param el the element to find the FCObject for.
     * @return the FCObject for <i>el</i> or none if none is available.
     * @throws FCException if an error occurs while creating the FCObject.
     */
    protected FCObject getObjectFor(Element el) throws FCException {
        Attribute idAttr = el.getAttribute("id");
        while (idAttr == null && el != null) {
            if (el.getParent() instanceof Element) {
                el = (Element) el.getParent();
                idAttr = el.getAttribute("id");
            } else {
                el = null;
            }
        }
        if (el == null || idAttr == null) {
            return null;
        }
        FCObject obj = fromCache(new FCValue(idAttr.getValue()));
        if (obj == null) {
            if (el.getParent() instanceof Element) {
                FCObject parent = getObjectFor((Element) el.getParent());
                obj = getObjectFor(el, parent);
            } else {
                obj = getObjectFor(el, null);
            }
        }
        return obj;
    }

    /** @see DataStore#getFCObjectByName(FCValue, FCObject)
     */
    public FCObject getFCObjectByName(FCValue name, FCObject parent) throws FCException {
        List children = getChildren(parent);
        for (Iterator i = children.iterator(); i.hasNext(); ) {
            FCObject child = (FCObject) i.next();
            if (name.equals(child.getProperty(FCObject.NAME))) {
                return child;
            }
        }
        return null;
    }

    /** @see DataStore#getFCObjectByNumber(FCValue, FCValue, FCObject)
     */
    public FCObject getFCObjectByNumber(FCValue num, FCValue type, FCObject parent) throws FCException {
        List children = getChildrenOfType(type, parent);
        if (num.getAsInt() >= children.size()) {
            throw new FCException("No such " + type);
        }
        return getObjectFor((Element) children.get(num.getAsInt()), parent);
    }

    /** @see DataStore#getChildCount(FCValue, FCObject)
     */
    public FCValue getChildCount(FCValue type, FCObject parent) throws FCException {
        List children = getChildrenOfType(type, parent);
        return new FCValue(children.size());
    }

    /** Returns a list of children matching the specified type.  Any modifications to the list will
     * be reflected in the XML DOM.
     * 
     * @param type the type of child to return, eg: card, background etc.
     * @param parent the FCObject to return the children of.
     * @throws FCException if an error occurs while retrieving the child listing.
     * @return a {@see List} of {@see FCObject FCObjects} that are both children of <i>parent</i>
     * and of type <i>type</i>.
     */
    protected List getChildrenOfType(FCValue type, FCObject parent) throws FCException {
        Element parentEl = (Element) parent.getDataStoreData();
        Filter filter = null;
        String elementName;
        if (type.equals(FCRuntime.CARD_TYPE)) {
            elementName = "card";
            if (parentEl.getName().equals("background")) {
                filter = new AttributeFilter("background", parentEl.getAttribute("id").getValue());
                parentEl = (Element) ((Element) parentEl.getParent()).getParent();
            }
            parentEl = parentEl.getChild("cards", FC_NAMESPACE);
        } else if (type.equals(FCRuntime.BACKGROUND_TYPE)) {
            elementName = "background";
            parentEl = parentEl.getChild("backgrounds", FC_NAMESPACE);
        } else if (type.equals(FCRuntime.STACK_TYPE)) {
            elementName = "stack";
            parentEl = parentEl.getChild("components", FC_NAMESPACE);
        } else if (type.equals(FCRuntime.PART_TYPE)) {
            elementName = "part";
            parentEl = parentEl.getChild("parts", FC_NAMESPACE);
        } else {
            log.error("Invalid type: " + type);
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (parentEl == null) {
            throw new FCException("Couldn't find children for: " + type);
        }
        List children = parentEl.getChildren(elementName, FC_NAMESPACE);
        if (filter != null) {
            children = new FilterList(filter, children);
        }
        return children;
    }

    /** Returns the list of all child FCObjects of <i>parent</i>.
     * 
     * @param parent the parent to return the children of.
     * @return a List of all child FCObjects of <i>parent</i>.
     * @throws FCException if an error occurs while retrieving the children.
     */
    protected List getChildren(FCObject parent) throws FCException {
        Element el = (Element) parent.getDataStoreData();
        return getChildren(parent, el);
    }

    /** Returns the list of all child FCObjects of <i>parent</i>.  This function is used to
     * recursively check wrapper nodes.
     * 
     * @param parent the parent FCObject to return the children of.
     * @param el the DOM element to look for children in.
     * @return the List of FCObjects tht are children of <i>parent</i> and defined in <i>el</i>.
     * @throws FCException if an error occurs while retrieving children.
     */
    private List getChildren(FCObject parent, Element el) throws FCException {
        List children = new LinkedList();
        List elChildren = el.getChildren();
        for (Iterator i = elChildren.iterator(); i.hasNext(); ) {
            Element child = (Element) i.next();
            if (child.getAttribute("id") != null && !child.getName().equals("resource")) {
                children.add(getObjectFor(child, parent));
            } else {
                children.addAll(getChildren(parent, child));
            }
        }
        return children;
    }

    /** Returns an {@see FCObject} that is represented by the {@see Element} <i>el</i>.  Objects
     * are cached to improve performance.
     * 
     * @param el the Element representing the object to return.
     * @param parent the parent FCObject for the object to return.
     * @return the FCObject represented by the <i>el</i>.
     * @throws FCException if the FCObject cannot be created.
     */
    protected FCObject getObjectFor(Element el, FCObject parent) throws FCException {
        FCValue id = new FCValue(el.getAttributeValue("id"));
        FCObject obj = fromCache(id);
        if (obj != null) {
            return obj;
        }
        String type = el.getName();
        if (type.equals("stack")) {
            obj = new Stack(id, getRuntime(), this, el);
        } else if (type.equals("card")) {
            obj = new Card(id, getRuntime(), (Stack) parent, this, el);
        } else if (type.equals("background")) {
            obj = new Background(id, getRuntime(), (Stack) parent, this, el);
        } else if (type.equals("part")) {
            try {
                Class impl = new FCValue(el.getAttributeValue("type")).getAsClass(getRuntime());
                Class[] paramTypes = { FCValue.class, FCRuntime.class, FCObject.class, DataStore.class, Object.class };
                Constructor create = impl.getConstructor(paramTypes);
                Object[] args = { id, getRuntime(), parent, this, el };
                obj = (FCObject) create.newInstance(args);
            } catch (Exception e) {
                throw new FCException("Failed to create item of type \"" + el.getAttributeValue("type") + "\"", e);
            }
        }
        if (obj == null) {
            throw new FCException("Unsupported element type: " + type);
        }
        toCache(id, obj);
        return obj;
    }

    /** Retrieves an object from the cache.
     * 
     * @param id the id of the object to retrieve.
     * @return the cached {@see FCObject} if available or <tt>null</tt> if no object with the id
     * <i>id</i> is available in the cache.
     */
    protected FCObject fromCache(FCValue id) {
        SoftReference ref = (SoftReference) _cache.get(id);
        if (ref != null) {
            FCObject obj = (FCObject) ref.get();
            if (obj == null) {
                _cache.remove(ref);
            }
            return obj;
        }
        return null;
    }

    /** Adds the given object to the cache.
     * 
     * @param id the id of the object to add to the cache.
     * @param obj the object to add.
     */
    protected void toCache(FCValue id, FCObject obj) {
        _cache.put(id, new SoftReference(obj));
    }

    /** Sets the value of the specified property.
     * @param property the property to set the value of.
     * @param object the object the property belongs to.
     * @param value the new value to set the property to.
     * @exception FCException if an error occurs while writing the
     * property.
     */
    public void setProperty(FCValue property, FCObject object, FCValue value) throws FCException {
        Element el = (Element) object.getDataStoreData();
        if (property.equals("id")) {
            el.setAttribute("id", value.getAsString());
        }
        Element elProperties = el.getChild("properties", FC_NAMESPACE);
        List properties = elProperties.getChildren("property", FC_NAMESPACE);
        Iterator i = properties.iterator();
        boolean found = false;
        while (i.hasNext()) {
            Element prop = (Element) i.next();
            if (property.equals(prop.getAttributeValue("name"))) {
                prop.setText(value.getAsString());
                found = true;
            }
        }
        if (!found) {
            Element prop = new Element("property", FC_NAMESPACE);
            prop.setAttribute("name", property.getAsString());
            prop.setText(value.getAsString());
            elProperties.addContent(prop);
        }
    }

    /** Returns the value of the specified property of the specified
     * object.
     * @param property the property to return the value of.
     * @param object the object to retrieve a property for.
     * @return a FCValue containing the value of the specified property.
     * If the property does not exist, an empty string FCValue will be
     * returned.
     * @exception FCException if an error occurs while reading the
     * property.
     */
    public FCValue getProperty(FCValue property, FCObject object) throws FCException {
        log.trace("enter getProperty(FCValue property, FCObject object)");
        Element el = (Element) object.getDataStoreData();
        if (el == null) {
            throw new FCException("Could not find object in XML.");
        }
        el = el.getChild("properties", FC_NAMESPACE);
        if (el == null) {
            throw new FCException("Stack is invalid: " + object.getDataStoreData());
        }
        if (property.equals("id")) {
            return new FCValue(el.getAttributeValue("id"));
        }
        List properties = el.getChildren("property", FC_NAMESPACE);
        Iterator i = properties.iterator();
        while (i.hasNext()) {
            Element prop = (Element) i.next();
            if (property.equals(prop.getAttributeValue("name"))) {
                return new FCValue(prop.getText());
            }
        }
        return FCValue.EMPTY;
    }

    /** @see DataStore#getResource(FCValue)
     */
    public InputStream getResource(FCValue name) throws FCException {
        Element el = _factory.getElementWithID(name.getAsString());
        if (el == null) {
            throw new FCException("Could not find resource \"" + name + "\"");
        }
        String urlString = el.getTextTrim();
        if (!urlString.startsWith("http")) {
            try {
                log.debug("Get resource: " + urlString);
                URL url;
                if (urlString.startsWith("file:")) {
                    url = new URL(urlString);
                } else {
                    url = getClass().getResource(urlString);
                }
                return url.openStream();
            } catch (Exception e) {
                throw new FCException("Failed to load resource.", e);
            }
        } else {
            try {
                FCService http = getRuntime().getServiceFor(FCService.HTTP_DOWNLOAD);
                return http.perform(new FCValue[] { name }).getAsInputStream();
            } catch (Exception e) {
                throw new FCException("Failed to load resource.", e);
            }
        }
    }

    /** @see DataStore#storeResource(FCValue, FCValue)
     */
    public void storeResource(FCValue name, InputStream value) {
    }

    /** Returns true if the specified resource can be loaded by this
     * implementation.
     * @param resource a String descriptor of the resource to load.
     * @return true if the specified resource can be read by this
     * implementation, otherwise false.
     */
    public boolean canLoadResource(String resource) {
        log.trace("enter canLoadResource(String resource)");
        log.debug("Can we load: " + resource);
        try {
            URL url = new URL(resource);
            String protocol = url.getProtocol();
            return (protocol.equals("http") || protocol.equals("https") || protocol.equals("file") || protocol.equals("jar")) && resource.toLowerCase().endsWith(".xml");
        } catch (Exception e) {
            File f = new File(resource);
            return (f.exists() && f.canRead() && resource.toLowerCase().endsWith(".xml"));
        }
    }

    /** Loads the specified resource.
     * @param resource a String descriptor of the resource to load.
     * @exception FCException if an error occurs while loading the
     * resource.
     */
    public void loadResource(String resource) throws FCException {
        log.trace("enter loadResource(String resource)");
        try {
            String url;
            try {
                url = new URL(resource).toString();
            } catch (MalformedURLException mue) {
                log.debug("Failed to create url from resource: " + resource, mue);
                try {
                    url = new File(resource).toURL().toString();
                } catch (Exception e) {
                    log.debug("Failed to create file from resource: " + resource, e);
                    url = resource;
                }
            }
            InputStream stream = null;
            if (url.startsWith("http")) {
                FCService http = getRuntime().getServiceFor(FCService.HTTP_DOWNLOAD);
                stream = http.perform(new FCValue[] { new FCValue(resource) }).getAsInputStream();
            } else if (url.startsWith("file")) {
                stream = new URL(url).openStream();
            } else {
                throw new FCException("Unsupported protocol.");
            }
            _document = _builder.build(stream);
        } catch (Exception e) {
            throw new FCException(e.getMessage(), e);
        }
    }

    /** @see DataStore#addFCObject(FCValue, FCObject, FCValue, FCValue)
     */
    public FCObject addFCObject(FCValue number, FCObject parent, FCValue type, FCValue itemType) throws FCException {
        Element newElement = new Element(type.getAsString(), FC_NAMESPACE);
        Element parentEl = (Element) parent.getDataStoreData();
        newElement.setAttribute("id", "0");
        if (type.equals(FCRuntime.PART_TYPE)) {
            newElement.setAttribute("type", itemType.getAsString());
        } else if (type.equals(FCRuntime.CARD_TYPE)) {
            newElement.setAttribute("background", parentEl.getAttributeValue("id"));
        }
        List children = getChildrenOfType(type, parent);
        int pos = number.getAsInt();
        if (pos >= children.size()) {
            children.add(newElement);
        } else {
            children.add(number.getAsInt(), newElement);
        }
        newElement.addContent(new Element("properties", FC_NAMESPACE));
        if (type.equals(FCRuntime.STACK_TYPE)) {
            newElement.addContent(new Element("backgrounds", FC_NAMESPACE));
            newElement.addContent(new Element("cards", FC_NAMESPACE));
            newElement.addContent(new Element("components", FC_NAMESPACE));
        } else if (type.equals(FCRuntime.BACKGROUND_TYPE)) {
            newElement.addContent(new Element("parts", FC_NAMESPACE));
            newElement.addContent(new Element("groups", FC_NAMESPACE));
        } else if (type.equals(FCRuntime.PART_TYPE)) {
            newElement.addContent(new Element("parts", FC_NAMESPACE));
        }
        return getObjectFor(newElement, parent);
    }

    /** @see DataStore#removeFCObject(FCObject, FCObject)
     */
    public void removeFCObject(FCObject parent, FCObject child) throws FCException {
        Element elChild = (Element) child.getDataStoreData();
        if (elChild.getParent() instanceof Element) {
            Element elParent = (Element) elChild.getParent();
            elParent.getChildren().remove(elChild);
        } else {
            throw new FCException("Cannot remove main stack.");
        }
    }
}
