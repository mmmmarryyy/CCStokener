package org.gdi3d.xnavi.panels.caps;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.services.w3ds.Web3DService;

public class GetCapabilitiesPanel2 extends JScrollPane {

    public GetCapabilitiesPanel2() {
        super();
        JPanel ServerPanel = new JPanel();
        this.getVerticalScrollBar().setUnitIncrement(20);
        ServerPanel.setBackground(new java.awt.Color(255, 255, 255));
        ServerPanel.setLayout(null);
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Root");
        SAXTreeBuilder saxTree = new SAXTreeBuilder(top);
        InputStream urlIn = null;
        for (int w = 0; w < Navigator.web3DServices.length; w++) {
            Web3DService web3DService = Navigator.web3DServices[w];
            try {
                SAXParser saxParser = new SAXParser();
                saxParser.setContentHandler(saxTree);
                String request = web3DService.getServiceEndPoint() + "?" + "SERVICE=" + web3DService.getService() + "&" + "REQUEST=GetCapabilities&" + "ACCEPTFORMATS=text/xml&" + "ACCEPTVERSIONS=";
                for (int i = 0; i < web3DService.getAcceptVersions().length; i++) {
                    if (i > 0) request += ",";
                    request += web3DService.getAcceptVersions()[i];
                }
                System.out.println(request);
                URL url = new URL(request);
                URLConnection urlc = url.openConnection();
                urlc.setReadTimeout(Navigator.TIME_OUT);
                if (web3DService.getEncoding() != null) {
                    urlc.setRequestProperty("Authorization", "Basic " + web3DService.getEncoding());
                }
                urlIn = urlc.getInputStream();
                saxParser.parse(new InputSource(urlIn));
            } catch (Exception ex) {
                top.add(new DefaultMutableTreeNode(ex.getMessage()));
            }
        }
        try {
            urlIn.close();
        } catch (Exception e) {
        }
        JTree tree = new JTree(saxTree.getTree());
        ClassLoader cl = this.getClass().getClassLoader();
        ImageIcon leafIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(cl.getResource("resources/leaficon.png")));
        ImageIcon openIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(cl.getResource("resources/openicon.png")));
        ImageIcon closedIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(cl.getResource("resources/closedicon.png")));
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(leafIcon);
        renderer.setOpenIcon(openIcon);
        renderer.setClosedIcon(closedIcon);
        tree.setCellRenderer(renderer);
        this.setViewportView(tree);
        expandAll(tree);
    }

    public void expandAll(JTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }
}

class SAXTreeBuilder extends DefaultHandler {

    private DefaultMutableTreeNode currentNode = null;

    private DefaultMutableTreeNode previousNode = null;

    private DefaultMutableTreeNode rootNode = null;

    public SAXTreeBuilder(DefaultMutableTreeNode root) {
        rootNode = root;
    }

    public void startDocument() {
        currentNode = rootNode;
    }

    public void endDocument() {
    }

    public void characters(char[] data, int start, int end) {
        String str = new String(data, start, end);
        if (!str.equals("") && Character.isLetter(str.charAt(0))) {
            DefaultMutableTreeNode mtn = new DefaultMutableTreeNode(str);
            currentNode.add(mtn);
        }
    }

    public void startElement(String uri, String qName, String lName, Attributes atts) {
        previousNode = currentNode;
        currentNode = new DefaultMutableTreeNode(lName);
        attachAttributeList(currentNode, atts);
        previousNode.add(currentNode);
    }

    public void endElement(String uri, String qName, String lName) {
        if (currentNode.getUserObject().equals(lName)) currentNode = (DefaultMutableTreeNode) currentNode.getParent();
    }

    public DefaultMutableTreeNode getTree() {
        return rootNode;
    }

    private void attachAttributeList(DefaultMutableTreeNode node, Attributes atts) {
        for (int i = 0; i < atts.getLength(); i++) {
            String name = atts.getLocalName(i);
            String value = atts.getValue(name);
            node.add(new DefaultMutableTreeNode(name + " = " + value));
        }
    }
}
