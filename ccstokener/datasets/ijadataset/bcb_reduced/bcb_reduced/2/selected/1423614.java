package ch.unibe.inkml;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ch.unibe.eindermu.utils.Config;
import ch.unibe.eindermu.utils.StringList;
import ch.unibe.eindermu.utils.StringMap;
import ch.unibe.eindermu.utils.XmlHandler;

/**
 * AnnotationStructure defines the structur of the annotation of InkML elements.
 * This structure is defined in an external XML document.
 * 
 * 
 * @author emanuel
 *
 */
public class AnnotationStructure extends XmlHandler {

    public static final String STRUCTURE_FILE_CONFIG_KEY = "annotation_structure_file";

    public enum NodeNames {

        TRACEVIEW, INK
    }

    /**
     * List of items
     */
    private List<Item> items = new ArrayList<Item>();

    /**
	 * References resolving map
	 */
    private StringMap<Item> refs = new StringMap<Item>();

    /**
	 * File this structure is loaded from
	 */
    private File file;

    /**
	 * Config file where location of the annotationsturcture xml file
	 * and the icons is given.
	 */
    private Config config;

    public AnnotationStructure(Config config) throws IOException {
        assert config != null;
        this.config = config;
        if (config.containsKey(STRUCTURE_FILE_CONFIG_KEY)) {
            init(config.get(STRUCTURE_FILE_CONFIG_KEY));
        } else {
            init(this.getClass().getSimpleName() + ".xml");
        }
    }

    public AnnotationStructure() throws IOException {
        init(this.getClass().getSimpleName() + ".xml");
    }

    public AnnotationStructure(File structureFile) throws IOException {
        init(structureFile.getAbsolutePath());
    }

    private void init(String fileName) throws IOException {
        file = new File(fileName);
        if (file != null && !file.exists()) {
            URL url = this.getClass().getResource(fileName);
            if (url == null && config != null) {
                url = config.getApplication().getClass().getResource(fileName);
            }
            if (url == null) {
                throw new IOException("Annotation structure file '" + fileName + "' not found.");
            }
            this.loadFromStream(url.openStream());
        } else {
            this.loadFromFile(file);
        }
        loadInformation();
    }

    private void loadInformation() {
        loadNode(this.getDocument().getFirstChild(), null);
    }

    /**
	 * Loads the nodes.
	 * @param node
	 * @param parent
	 */
    private void loadNode(Node node, Item parent) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            if (el.getNodeName().equals("annotationStructure")) {
                loadNode(el.getFirstChild(), parent);
            } else if (el.getNodeName().equals("item")) {
                if (el.hasAttribute("itemRef")) {
                    parent.children.add(refs.get(el.getAttribute("itemRef").substring(1)));
                } else {
                    Item i = new Item();
                    if (!el.hasAttribute("node")) {
                        throw new IllegalArgumentException("In the Annotation Structure file '" + file.getPath() + "' there is an item without a node attribut, which is mandatory");
                    }
                    i.node = NodeNames.valueOf(el.getAttribute("node").toUpperCase());
                    if (el.hasAttribute("id")) {
                        i.id = el.getAttribute("id");
                        this.refs.put(i.id, i);
                    }
                    if (el.hasAttribute("copyAnnotationFromRef")) {
                        Item other = refs.get(el.getAttribute("copyAnnotationFromRef").substring(1));
                        for (String name : other.annotationNames()) {
                            i.annotations.put(name, other.getAnnotation(name).clone());
                        }
                    }
                    if (el.hasAttribute("icon")) {
                        i.iconName = el.getAttribute("icon");
                    }
                    if (parent != null) {
                        parent.children.add(i);
                    }
                    loadNode(node.getFirstChild(), i);
                    items.add(i);
                }
            } else if (el.getNodeName().equals("annotation") || el.getNodeName().equals("attribute")) {
                boolean refine = true;
                Annotation a = parent.getAnnotation(el.getAttribute("name"));
                if (a == null) {
                    refine = false;
                    a = new Annotation();
                    a.name = el.getAttribute("name");
                }
                a.type = (el.getNodeName().equals("annotation")) ? Annotation.AType.ANNOTATION : Annotation.AType.ATTRIBUTE;
                if (el.hasAttribute("valueType")) {
                    a.valueType = Annotation.ValueType.valueOf(el.getAttribute("valueType").toUpperCase());
                }
                if (el.hasAttribute("triggerValue")) {
                    a.setTrigger(el.getAttribute("triggerValue"));
                }
                if (el.hasAttribute("optional") && el.getAttribute("optional").equals("true")) {
                    a.optional = true;
                }
                if (!refine) {
                    parent.annotations.put(a.name, a);
                }
                loadNodeInAn(node.getFirstChild(), a);
            } else if (el.getNodeName().equals("traces")) {
                parent.containsTraces = true;
            }
        }
        loadNode(node.getNextSibling(), parent);
    }

    private void loadNodeInAn(Node node, Annotation a) {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            if (el.getNodeName().equals("value")) {
                a.values.add(el.getTextContent());
            }
        }
        loadNodeInAn(node.getNextSibling(), a);
    }

    /**
	 * An Item in AnnotationStructure defines for some InkML elements what annotations are allowed.
	 * The InkML elements such an Item stands for is defined by its element name which has to be the same
	 * as the property "node" in this class. And second, if in the list of annotations there is one which carry
	 * the attribute "triggervalue" then the InkML element must have this specific annotation with the given value.
	 * @author emanuel
	 *
	 */
    public static class Item {

        /**
	     * List of annotation items which are allowed for child InkML elements of the InkML element 
	     * this item represents. 
	     */
        public List<Item> children = new ArrayList<Item>();

        /**
	     * Defines if the annotation items can directly contain Strokes.
	     */
        public boolean containsTraces = false;

        /**
		 * List of annotation tags possible for this InkML element
		 */
        public StringMap<Annotation> annotations = new StringMap<Annotation>();

        /**
		 * Represents the node name of the InkML element, this item defines the annotation for.
		 */
        public NodeNames node;

        /**
		 * The id of this element, for referencing within the annotation structure
		 */
        public String id;

        /**
		 * defines the icon, the InkML element is associated with in the GUI.
		 */
        public String iconName;

        /**
		 * Returns a list of the names of all annotations possible in this item
		 * @return
		 */
        public StringList annotationNames() {
            return annotations.keyList();
        }

        /**
		 * Returns the annotations specified by the name s
		 * @param name
		 * @return
		 */
        public Annotation getAnnotation(String name) {
            return annotations.get(name);
        }

        /**
         * @param child
         * @return
         */
        public boolean containItem(Item child) {
            return children.contains(child);
        }

        /**
         * @param element
         * @return
         */
        public boolean applyTo(InkAnnotatedElement element) {
            if (this.node == NodeNames.INK && element.equals(element.getInk())) {
                return true;
            } else if (this.node == NodeNames.TRACEVIEW && element instanceof InkTraceView) {
                InkTraceView view = (InkTraceView) element;
                for (Annotation a : annotations) {
                    if (a.triggerValue != null) {
                        return view.containsAnnotation(a.name) && view.testAnnotation(a.name, a.triggerValue);
                    }
                }
            }
            return false;
        }

        /**
         * @return
         */
        public String getLabel() {
            String result = node.toString();
            String args = "";
            for (Annotation a : annotations) {
                if (a.triggerValue != null) {
                    args += a.name + "=" + a.triggerValue + ",";
                }
            }
            if (args.isEmpty()) {
                return result;
            }
            return result + "[" + args.substring(0, args.length() - 1) + "]";
        }
    }

    /**
	 * Class representing an annotation
	 * @author emanuel
	 *
	 */
    public static class Annotation {

        public AType type;

        /**
	     * list of possible values, used if valueType is PROPOSED or ENUM
	     */
        public StringList values = new StringList();

        public String triggerValue = null;

        /**
		 * Possible type of values of annotations
		 * @author emanuel
		 *
		 */
        public enum AType {

            ANNOTATION, ATTRIBUTE
        }

        public enum ValueType {

            /**
		     * every possible value type
		     */
            FREE, /**
			 * The values in 'values' are proposed, other are possible
			 */
            PROPOSED, /**
			 * The values in 'values' are mandatory. 
			 */
            ENUM, /**
			 * This is a date, for now same as free
			 * TODO: implement date selector
			 */
            DATE, /**
			 * it has to be an integer
			 */
            INTEGER, LANGUAGECODE, COUNTRYCODE
        }

        /**
		 * name of this annotation
		 */
        public String name;

        /**
		 * type of the values of this annotation
		 */
        public Annotation.ValueType valueType = ValueType.FREE;

        /**
		 * if set to true the annotation can be omitted. If valueType is FREE then 
		 * this property is ignored, since annotation with value type free can be omitted anyway.
		 */
        public boolean optional = false;

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Annotation)) {
                return false;
            }
            Annotation o = (Annotation) other;
            boolean result = o.name.equals(name);
            result = result && (!((valueType == ValueType.ENUM && o.valueType != ValueType.ENUM) || (valueType != ValueType.ENUM && o.valueType == ValueType.ENUM)));
            result = result && (valueType != ValueType.ENUM || values == o.values);
            return result;
        }

        /**
		 * Specify the value which serves as a trigger for an item containing this annotation
		 * @param value
		 */
        public void setTrigger(String value) {
            triggerValue = value;
        }

        @Override
        public Annotation clone() {
            Annotation a = new Annotation();
            a.name = name;
            a.triggerValue = triggerValue;
            a.type = type;
            a.optional = optional;
            a.valueType = valueType;
            a.values.addAll(values);
            return a;
        }
    }

    /**
	 * Return the name of all annotations that can be made for a specified InkML element.
	 * @param element
	 * @return List of annotation names
	 */
    public StringList getAnnotations(InkAnnotatedElement element) {
        Item i = getItem(element);
        if (i != null) {
            return new StringList(i.annotationNames());
        }
        return new StringList();
    }

    /**
	 * Returns the node name of InkML element, as it is used in AnnotationStructure.
	 * @param element
	 * @return
	 */
    private NodeNames getNodeName(InkAnnotatedElement element) {
        if (element instanceof InkTraceView) {
            return NodeNames.TRACEVIEW;
        } else if (element instanceof InkInk) {
            return NodeNames.INK;
        } else {
            throw new IllegalArgumentException("Elements of class " + element.getClass().getName() + " are not yet supported for annotation.");
        }
    }

    /**
	 * Return the list of all the different trigger values available in the Annotation structure
	 * given for specified nodeType and a specified triggerValueType
	 * 
	 * UseCase: For TraceViews we have differnt types. Each type of TraceViews differ, in that it
	 * has different Annotations. Therefore the annotation specifying the type of a TraceView contains the
	 * trigger value to distinguish the different annotation Items in the AnnotationStructure.
	 * So we might want to get a list of all different types defined as trigger types in the AnnotationStructure.
	 * This methods returns that list.
	 * 
	 * @param nodeTypes
	 * @param triggerValueType
	 * @return
	 */
    public StringList getTriggerValues(NodeNames nodeTypes, String triggerValueType) {
        for (Item i : getItems()) {
            if (i.node == nodeTypes && i.getAnnotation(triggerValueType) != null) {
                return i.getAnnotation(triggerValueType).values;
            }
        }
        return new StringList();
    }

    /**
     * Returns all items.
     * @return
     */
    public List<Item> getItems() {
        return new ArrayList<Item>(items);
    }

    /**
     * Returns the item which is most accuratly representing the InkML elements specified as parameter 
     * @param element
     * @return
     */
    public Item getItem(InkAnnotatedElement element) {
        NodeNames node = getNodeName(element);
        Item candidate = null;
        int candidateTriggerCount = 0;
        for (Item i : items) {
            if (i.node.equals(node)) {
                boolean fits = true;
                int triggerCount = 0;
                for (Annotation a : i.annotations) {
                    if (a.triggerValue != null) {
                        if (!element.testAnnotation(a.name, a.triggerValue)) {
                            fits = false;
                            break;
                        }
                        triggerCount++;
                    }
                }
                if (fits) {
                    if (candidate == null || triggerCount > candidateTriggerCount) {
                        candidate = i;
                        candidateTriggerCount = triggerCount;
                    }
                }
            }
        }
        return candidate;
    }

    /**
     * Returns the Item which most accuratly represents InkML elements which have the nodename as specified, and contain the 
     * attribute values as specified by the "query" map, 
     * @param node
     * @param query
     * @return
     */
    public Item getItem(NodeNames node, Map<String, String> query) {
        for (Item i : items) {
            if (i.node.equals(node)) {
                boolean fits = true;
                if (query != null) {
                    for (String name : query.keySet()) {
                        if (name == null || query.get(name) == null) {
                            continue;
                        }
                        Annotation a = i.getAnnotation(name);
                        if (a == null || a.triggerValue == null || !query.get(name).equals(a.triggerValue)) {
                            fits = false;
                            break;
                        }
                    }
                }
                if (fits) {
                    return i;
                }
            }
        }
        return null;
    }

    /**
     * Returns the icon of the item specified
     * The icon is looked up at the following places:
     *  - in the same directory as the main class
     *  - in the same directory as the AnnotationStructure XML File
     * @param i
     * @return
     */
    public Icon getIcon(Item i) {
        if (i == null || i.iconName == null) {
            return null;
        }
        if (config != null || (config = Config.getMain()) != null && config.getApplication() != null) {
            if (config.getApplication().getClass().getResource(i.iconName) != null) {
                return new ImageIcon(config.getApplication().getClass().getResource(i.iconName));
            }
        }
        File f = new File(i.iconName);
        if (!f.exists()) {
            if (file != null) {
                f = new File(this.getFile().getParent() + "/" + i.iconName);
            }
            if (!f.exists()) {
                return null;
            }
        }
        return new ImageIcon(f.getPath());
    }

    /**
     * Returns the icon of the InkAnno element specified
     * @param annotatedElement
     * @return the icon
     */
    public Icon getIcon(InkAnnotatedElement annotatedElement) {
        return getIcon(getItem(annotatedElement));
    }

    private File getFile() {
        return this.file;
    }

    /**
	 * returns the items which contain the item representing the element specified here
	 * @param element
	 * @return
	 */
    public List<Item> getParents(InkAnnotatedElement element) {
        List<Item> parents = new ArrayList<Item>();
        boolean isTrace = element instanceof InkTraceLike<?> && ((InkTraceLike<?>) element).isLeaf();
        Item child = getItem(element);
        for (Item i : getItems()) {
            if ((!isTrace && i.containItem(child)) || (isTrace && i.containsTraces)) {
                parents.add(i);
            }
        }
        return parents;
    }

    /**
	 * returns the items which contain all of the items representing the elements specified by the parameter
	 * @param listOfElements
	 * @return
	 */
    public List<Item> getParents(Collection<? extends InkAnnotatedElement> listOfElements) {
        List<Item> parents = null;
        for (InkAnnotatedElement v : listOfElements) {
            if (parents == null) {
                parents = getParents(v);
            } else {
                List<Item> temp_parents = getParents(v);
                Iterator<Item> it = parents.iterator();
                while (it.hasNext()) {
                    Item i = it.next();
                    if (!temp_parents.contains(i)) {
                        it.remove();
                    }
                }
            }
        }
        return (parents == null) ? new ArrayList<Item>() : parents;
    }

    public static class TriggerQuery extends StringMap<String> {

        public TriggerQuery(String name, String value) {
            super();
            put(name, value);
        }
    }
}
