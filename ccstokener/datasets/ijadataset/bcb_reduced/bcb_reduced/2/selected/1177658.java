package ist.ac.simulador.application;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Description: The XTree class is an extension of the javax.swing.JTree class.
 * It behaves in every way like a JTree component, the difference is that additional
 * methods have been provided to facilitate the parsing of an XML document into a
 * DOM object and translating that DOM object into a viewable JTree structure.
 *
 * Copyright (c) March 2001 Kyle Gabhart
 * @author Kyle Gabhart
 * @version 1.0
 */
public class XTree extends JTree implements MouseMotionListener, MouseListener {

    /**
   * This member stores the TreeNode object used to create the model for the JTree.
   * The DefaultMutableTreeNode class is defined in the javax.swing.tree package
   * and provides a default implementation of the MutableTreeNode interface.
   */
    private DefaultMutableTreeNode treeNode;

    /** 
   * These three members are a part of the JAXP API and are used to parse the XML
   * text into a DOM object (of type Document).
   */
    private DocumentBuilderFactory dbf;

    private DocumentBuilder db;

    private Document doc;

    /**
   * This single constructor builds an XTree object using the XML text
   * passed in through the constructor.
   *
   * @param text A String of XML formatted text
   *
   * @exception ParserConfigurationException  This exception is potentially thrown if
   * the constructor configures the parser improperly.  It won't.
   */
    public XTree() throws ParserConfigurationException {
        super();
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setShowsRootHandles(true);
        setEditable(false);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        db = dbf.newDocumentBuilder();
        readTree();
        addMouseMotionListener(this);
        setDragEnabled(true);
        setTransferHandler(new ModuleTransferHandler());
        addMouseListener(this);
    }

    public void mouseDragged(MouseEvent e) {
        if (_firstEvent != null) {
            e.consume();
            System.out.println("HERE!");
            JComponent c = (JComponent) e.getSource();
            TransferHandler transferHandler = new ModuleTransferHandler();
            transferHandler.exportAsDrag(c, _firstEvent, TransferHandler.MOVE);
            _firstEvent = null;
        }
    }

    private boolean nodeIsLeaf(MouseEvent e) {
        int hoveredRow = getRowForLocation(e.getX(), e.getY());
        if (hoveredRow == -1) {
            return false;
        }
        TreePath path = getPathForRow(hoveredRow);
        return ((DefaultMutableTreeNode) path.getLastPathComponent()).isLeaf();
    }

    public void mouseMoved(MouseEvent e) {
        int hoveredRow = getRowForLocation(e.getX(), e.getY());
        if (hoveredRow != -1) {
            TreePath path = getPathForRow(hoveredRow);
            if (((DefaultMutableTreeNode) path.getLastPathComponent()).isLeaf()) setSelectionRow(hoveredRow); else clearSelection();
        } else {
            clearSelection();
        }
    }

    private MouseEvent _firstEvent = null;

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() != 2) return;
        int hoveredRow = getRowForLocation(e.getX(), e.getY());
        if (hoveredRow != -1) {
            TreePath path = getPathForRow(hoveredRow);
            DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (mutableNode.isLeaf()) {
                NodeElement node = (NodeElement) mutableNode.getUserObject();
                if (node == null) return;
                String config = node.getAttribute("helpfile");
                if (config == null) return;
                new AHelpScreen(config).setVisible(true);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        e.consume();
        if (!nodeIsLeaf(e)) {
            clearSelection();
            return;
        }
        _firstEvent = e;
    }

    public void mouseReleased(MouseEvent e) {
        clearSelection();
        _firstEvent = null;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void readTree() {
        treeNode = createTreeNode(parseXml(File2String("data", "ic_list.xml")));
        setModel(new DefaultTreeModel(treeNode));
    }

    public NodeElement getSelectedNode() {
        TreePath path = this.getSelectionPath();
        if (path == null) return null;
        DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!mutableNode.isLeaf()) return null;
        return (NodeElement) mutableNode.getUserObject();
    }

    private String File2String(String directory, String filename) {
        String line;
        InputStream in = null;
        try {
            File f = new File(filename);
            System.out.println("File On:>>>>>>>>>> " + f.getCanonicalPath());
            in = new FileInputStream(f);
        } catch (FileNotFoundException ex) {
            in = null;
        } catch (IOException ex) {
            in = null;
        }
        try {
            if (in == null) {
                filename = directory + "/" + filename;
                java.net.URL urlFile = ClassLoader.getSystemResource(filename);
                if (urlFile == null) {
                    System.out.println("Integrated Chips list file not found: " + filename);
                    System.exit(-1);
                }
                in = urlFile.openStream();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuffer xmlText = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                xmlText.append(line);
            }
            reader.close();
            return xmlText.toString();
        } catch (FileNotFoundException ex) {
            System.out.println("Integrated Chips list file not found");
            System.exit(-1);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    /**
    * This takes a DOM Node and recurses through the children until each one is added
    * to a DefaultMutableTreeNode. The JTree then uses this object as a tree model.
    *
    * @param root org.w3c.Node.Node
    *
    * @return Returns a DefaultMutableTreeNode object based on the root Node passed in
    */
    private DefaultMutableTreeNode createTreeNode(Node root) {
        DefaultMutableTreeNode treeNode = null;
        String type, name, value;
        NamedNodeMap attribs;
        Node attribNode;
        type = getNodeType(root);
        name = root.getNodeName();
        value = root.getNodeValue();
        NodeElement elementnode = new NodeElement(name, value);
        treeNode = new DefaultMutableTreeNode(elementnode);
        attribs = root.getAttributes();
        if (attribs != null) {
            for (int i = 0; i < attribs.getLength(); i++) {
                attribNode = attribs.item(i);
                name = attribNode.getNodeName().trim();
                value = attribNode.getNodeValue().trim();
                if (value != null) {
                    if (value.length() > 0) {
                        elementnode.addAttribute(name, value);
                    }
                }
            }
        }
        if (root.hasChildNodes()) {
            NodeList children;
            int numChildren;
            Node node;
            String data;
            children = root.getChildNodes();
            if (children != null) {
                numChildren = children.getLength();
                for (int i = 0; i < numChildren; i++) {
                    node = children.item(i);
                    if (node != null) {
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            treeNode.add(createTreeNode(node));
                        }
                    }
                }
            }
        }
        return treeNode;
    }

    /**
    * This method returns a string representing the type of node passed in.
    *
    * @param node org.w3c.Node.Node
    *
    * @return Returns a String representing the node type
    */
    private String getNodeType(Node node) {
        String type;
        switch(node.getNodeType()) {
            case Node.ELEMENT_NODE:
                {
                    type = "Element";
                    break;
                }
            case Node.ATTRIBUTE_NODE:
                {
                    type = "Attribute";
                    break;
                }
            case Node.TEXT_NODE:
                {
                    type = "Text";
                    break;
                }
            case Node.CDATA_SECTION_NODE:
                {
                    type = "CData section";
                    break;
                }
            case Node.ENTITY_REFERENCE_NODE:
                {
                    type = "Entity reference";
                    break;
                }
            case Node.ENTITY_NODE:
                {
                    type = "Entity";
                    break;
                }
            case Node.PROCESSING_INSTRUCTION_NODE:
                {
                    type = "Processing instruction";
                    break;
                }
            case Node.COMMENT_NODE:
                {
                    type = "Comment";
                    break;
                }
            case Node.DOCUMENT_NODE:
                {
                    type = "Document";
                    break;
                }
            case Node.DOCUMENT_TYPE_NODE:
                {
                    type = "Document type";
                    break;
                }
            case Node.DOCUMENT_FRAGMENT_NODE:
                {
                    type = "Document fragment";
                    break;
                }
            case Node.NOTATION_NODE:
                {
                    type = "Notation";
                    break;
                }
            default:
                {
                    type = "???";
                    break;
                }
        }
        return type;
    }

    /**
    * This method performs the actual parsing of the XML text
    *
    * @param text A String representing an XML document
    * @return Returns an org.w3c.Node.Node object
    */
    private Node parseXml(String text) {
        ByteArrayInputStream byteStream;
        byteStream = new ByteArrayInputStream(text.getBytes());
        try {
            doc = db.parse(byteStream);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return (Node) doc.getDocumentElement();
    }
}
