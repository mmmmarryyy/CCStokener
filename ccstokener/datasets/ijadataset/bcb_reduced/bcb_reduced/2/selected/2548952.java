package org.xul.script.xul;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xul.script.scripts.Script;
import org.xul.script.scripts.ScriptFactory;
import org.xul.script.scripts.js.JSFunctionBinding;
import org.xul.script.scripts.js.JSScript;
import org.xul.script.xul.model.AbstractXULWidget;
import org.xul.script.xul.model.XULContainer;
import org.xul.script.xul.model.XULDocument;

/** Handle the creation of the component tree with the XUL Document, after the initial parsing.
 * This class will create a bridge between DOM Nodes and the associated GUI Components.
 * 
 * @version 0.4
 */
public abstract class AbstractXULHandler implements XULConstants {

    protected Map<Element, Boolean> unsizedContainers = new HashMap();

    private Map<Element, JComponent> parentNodes = new HashMap();

    protected Map<Element, AbstractXULWidget> widgets = new HashMap();

    protected Map<String, JComponent> compNames = new HashMap();

    protected Map<JComponent, String> compToNames = new HashMap();

    protected JPanel rootpanel;

    protected Map<URL, JSScript> externaljsScripts = new HashMap(10);

    protected Map<Element, JSScript> embeddedjsScripts = new HashMap(10);

    protected Map<String, JSFunctionBinding> jsfunctionBindings = new HashMap();

    protected Set<URL> scripts = new HashSet(10);

    protected NamedNodeMap attrs = null;

    protected URL baseDir = null;

    protected Map<Element, Integer> layouts = new HashMap();

    private static final Pattern dimPat = Pattern.compile("\\s*(\\d+)\\s*([a-zA-Z]+)\\s*");

    protected XULDocument doc = null;

    protected List<Integer> embeddedScriptLines;

    protected Map<String, Integer> elementsLocation;

    protected Iterator<Integer> scriptLinesIterator;

    private ScriptFactory fac = ScriptFactory.getInstance();

    private Map<URL, String> nullScripts = new HashMap();

    public AbstractXULHandler(URL baseDir, XULDocument doc, List<Integer> embeddedScriptLines, Map<String, Integer> elementsLocation) {
        rootpanel = new JPanel();
        this.baseDir = baseDir;
        rootpanel.setLayout(new BorderLayout());
        this.doc = doc;
        this.embeddedScriptLines = embeddedScriptLines;
        this.elementsLocation = elementsLocation;
    }

    public Map<JComponent, String> getComponentsToNames() {
        return compToNames;
    }

    /** @return the root GUI component of the XUL Script.
     */
    public JComponent getRoot() {
        return rootpanel;
    }

    public Map<URL, JSScript> getExternalJSScripts() {
        return externaljsScripts;
    }

    public Map<Element, JSScript> getEmbeddedJSScripts() {
        return embeddedjsScripts;
    }

    public Map<String, JSFunctionBinding> getJSFunctionsBindings() {
        return jsfunctionBindings;
    }

    public Map<URL, String> getNotExistingScripts() {
        return nullScripts;
    }

    public Set<URL> getScripts() {
        return scripts;
    }

    protected void addLayout(Element elt, int type) {
        layouts.put(elt, type);
    }

    protected int getLayoutType(Element elt) {
        if (layouts.containsKey(elt)) {
            return layouts.get(elt).intValue();
        } else {
            return XULContainer.LAYOUT_UNDEFINED;
        }
    }

    protected String getAttrValue(String attrname) {
        if (attrs.getNamedItem(attrname) != null) {
            Attr attr = (Attr) attrs.getNamedItem(attrname);
            return attr.getValue();
        } else {
            return null;
        }
    }

    protected boolean hasAttr(String attrname) {
        return (attrs.getNamedItem(attrname) != null);
    }

    protected int getAttrValueAsInteger(String attrname) {
        if (attrs.getNamedItem(attrname) != null) {
            Attr attr = (Attr) attrs.getNamedItem(attrname);
            int value = 0;
            try {
                return Integer.parseInt(attr.getValue());
            } catch (NumberFormatException e) {
            }
            return value;
        } else {
            return 0;
        }
    }

    protected int getAttrValueAsDimension(String attrname) {
        if (attrs.getNamedItem(attrname) != null) {
            Attr attr = (Attr) attrs.getNamedItem(attrname);
            int value = 0;
            Matcher mat = dimPat.matcher(attr.getValue());
            if (mat.matches()) {
                try {
                    value = Integer.parseInt(mat.group(1));
                } catch (NumberFormatException e) {
                }
            } else {
                try {
                    value = Integer.parseInt(attr.getValue());
                } catch (NumberFormatException e) {
                }
            }
            return value;
        } else {
            return 0;
        }
    }

    protected boolean getAttrValueAsBoolean(String attrname) {
        if (attrs.getNamedItem(attrname) != null) {
            Attr attr = (Attr) attrs.getNamedItem(attrname);
            if (attr.getValue().equalsIgnoreCase("true")) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected ImageIcon getAttrValueAsIcon(String attrname) {
        if (attrs.getNamedItem(attrname) != null) {
            Attr attr = (Attr) attrs.getNamedItem(attrname);
            String path = attr.getValue();
            URL url = FileUtilities.getChildURL(baseDir, path);
            ImageIcon icon = new ImageIcon(url);
            return icon;
        } else {
            return null;
        }
    }

    protected String getCurrentID() {
        return getAttrValue(ID);
    }

    /** Set the sze of a component.
     * @return true if the component has a defined size
     */
    protected boolean setComponentSize(Node node, JComponent comp) {
        if (hasAttr(HIDDEN)) {
            boolean hidden = getAttrValueAsBoolean(HIDDEN);
            comp.setVisible(!hidden);
        }
        boolean hasSize = false;
        if ((hasAttr(WIDTH)) && (hasAttr(HEIGHT))) {
            int width = getAttrValueAsDimension(WIDTH);
            int height = getAttrValueAsDimension(HEIGHT);
            comp.setSize(width, height);
            comp.setPreferredSize(comp.getSize());
            comp.setMaximumSize(comp.getSize());
            hasSize = true;
        } else if (hasAttr(WIDTH)) {
            int width = getAttrValueAsDimension(WIDTH);
            int height = comp.getPreferredSize().height;
            Dimension dim = new Dimension(width, height);
            comp.setSize(dim);
            comp.setPreferredSize(dim);
            comp.setMaximumSize(dim);
            hasSize = true;
        } else if (hasAttr(HEIGHT)) {
            int width = comp.getPreferredSize().width;
            int height = getAttrValueAsDimension(HEIGHT);
            Dimension dim = new Dimension(width, height);
            comp.setSize(dim);
            comp.setPreferredSize(dim);
            comp.setMaximumSize(dim);
            hasSize = true;
        }
        if ((hasAttr(MIN_WIDTH)) && (hasAttr(MIN_HEIGHT))) {
            int width = getAttrValueAsDimension(MIN_WIDTH);
            int height = getAttrValueAsDimension(MIN_HEIGHT);
            comp.setMinimumSize(new Dimension(width, height));
            hasSize = true;
        }
        if ((hasAttr(MAX_WIDTH)) && (hasAttr(MAX_WIDTH))) {
            int width = getAttrValueAsDimension(MIN_WIDTH);
            int height = getAttrValueAsDimension(MAX_HEIGHT);
            comp.setMaximumSize(new Dimension(width, height));
            hasSize = true;
        }
        if ((!hasSize) && (comp instanceof JPanel)) {
            unsizedContainers.put((Element) node, Boolean.TRUE);
        }
        return hasSize;
    }

    protected void setComponentLocation(Element elt, JComponent comp) {
        int layoutType = getLayoutType((Element) elt.getParentNode());
        if (layoutType == XULContainer.LAYOUT_STACK) {
            if ((hasAttr(LEFT)) && (hasAttr(TOP))) {
                int left = getAttrValueAsDimension(LEFT);
                int top = getAttrValueAsDimension(TOP);
                comp.setLocation(left, top);
            }
        } else if ((layoutType == XULContainer.LAYOUT_HBOX) || (layoutType == XULContainer.LAYOUT_HGROUPBOX)) {
            comp.setAlignmentY(Component.TOP_ALIGNMENT);
        } else if ((layoutType == XULContainer.LAYOUT_VBOX) || (layoutType == XULContainer.LAYOUT_VGROUPBOX)) {
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (layoutType == XULContainer.LAYOUT_HRADIOGROUP) {
            comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else if (layoutType == XULContainer.LAYOUT_VRADIOGROUP) {
            comp.setAlignmentX(Component.TOP_ALIGNMENT);
        }
    }

    protected JComponent getParentComponent(Node node) {
        Node parent = node.getParentNode();
        if (parent == null) {
            return rootpanel;
        } else {
            return parentNodes.get((Element) parent);
        }
    }

    protected JComponent getComponent(Node node) {
        return parentNodes.get((Element) node);
    }

    protected void parseCommand(Element elt, AbstractXULWidget widget) {
        String command = getAttrValue("oncommand");
        if (command != null) {
            doc.addAction(widget, command);
            widget.bindAction();
        }
    }

    /** Add the component to the hierarchy.
     * @return true if the component has a defined size
     */
    protected boolean addToHierarchy(Element elt, String id, AbstractXULWidget widget) {
        if (widget.hasAttr(TOOLTIPTEXT)) {
            String text = widget.getAttrValue(TOOLTIPTEXT);
            widget.setToolTipText(text);
        }
        if (elementsLocation.containsKey(id)) {
            widget.setLocation(elementsLocation.get(id));
        }
        JComponent comp = widget.getComponent();
        boolean hasSize = setComponentSize(elt, comp);
        parentNodes.put(elt, comp);
        widgets.put(elt, widget);
        parseCommand(elt, widget);
        if (id != null) {
            compNames.put(id, comp);
            compToNames.put(comp, id);
            doc.addWidget(widget);
        }
        Element parent = (Element) elt.getParentNode();
        if (parentNodes.containsKey(parent)) {
            Integer layoutType = layouts.get(parent);
            if (layoutType == null) {
                parentNodes.get(parent).add(comp, null, 0);
            } else {
                int type = layoutType.intValue();
                if (type == XULContainer.LAYOUT_STACK) {
                    parentNodes.get(parent).add(comp, null, 0);
                } else if (type == XULContainer.LAYOUT_UNDEFINED) {
                    parentNodes.get(parent).add(comp, null, 0);
                } else {
                    parentNodes.get(parent).add(comp);
                }
            }
        } else {
            rootpanel.add(comp, null, 0);
        }
        setComponentLocation(elt, comp);
        return hasSize;
    }

    public void startDocument() {
        scriptLinesIterator = embeddedScriptLines.iterator();
    }

    public abstract void startElementNode(String localName, NamedNodeMap attrs, Element elt);

    public abstract void endDocument();

    public void startTextNode(String nodeValue, Node parentNode) {
        if (embeddedjsScripts.containsKey((Element) parentNode)) {
            embeddedjsScripts.get((Element) parentNode).setScript(nodeValue);
            XULScriptParser parser = new XULScriptParser(nodeValue, embeddedjsScripts.get((Element) parentNode), false);
            parser.parse();
            Map<String, JSFunctionBinding> _functionNames = parser.getFunctionsLocation();
            jsfunctionBindings.putAll(_functionNames);
        }
    }

    protected boolean isLastChild(Node node) {
        return ((node.getNextSibling() == null) || (node.getNextSibling().getNodeType() != Node.ELEMENT_NODE));
    }

    private String getText(URL url) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        boolean start = true;
        while (true) {
            String line = reader.readLine();
            if (line != null) {
                if (!start) {
                    buf.append('\n');
                }
                buf.append(line);
                start = false;
            } else {
                break;
            }
        }
        reader.close();
        return buf.toString();
    }

    /** Parse a "script" element in the XUL Document. This can be either a BSH or
     * a Javascript script, and Javascript scripts can either be define inline or in
     * an external File.
     */
    protected void parseScript(Element elt) {
        String scriptMIMEType = getAttrValue("type");
        String src = getAttrValue("src");
        if (src != null) {
            if ((scriptMIMEType == null) || (scriptMIMEType.equals(JSScript.MIME))) {
                JSScript script = new JSScript(JSScript.TYPE_EXTERNAL);
                try {
                    URL url = FileUtilities.getChildURL(baseDir, src);
                    File file = new File(url.getFile());
                    if (file.exists()) {
                        script.setFile(file);
                        externaljsScripts.put(url, script);
                        String text = getText(url);
                        XULScriptParser parser = new XULScriptParser(text, script, true);
                        parser.parse();
                        Map<String, JSFunctionBinding> _functionNames = parser.getFunctionsLocation();
                        jsfunctionBindings.putAll(_functionNames);
                        script.setScript(text);
                    } else {
                        nullScripts.put(url, JSScript.MIME);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                URL url = FileUtilities.getChildURL(baseDir, src);
                File file = new File(url.getFile());
                if (file.exists()) {
                    Script script = fac.addScript(scriptMIMEType, url);
                    if (script != null) {
                        scripts.add(url);
                    }
                } else {
                    nullScripts.put(url, scriptMIMEType);
                }
            }
        } else {
            JSScript script = new JSScript(JSScript.TYPE_EMBEDDED);
            int location = -1;
            if (scriptLinesIterator.hasNext()) {
                location = scriptLinesIterator.next();
            }
            script.setFile(doc.getFile());
            script.setOffset(location);
            embeddedjsScripts.put(elt, script);
        }
    }
}
