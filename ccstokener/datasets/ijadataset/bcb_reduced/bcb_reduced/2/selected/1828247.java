package org.gdi3d.xnavi.test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.tree.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.xerces.parsers.*;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.services.w3ds.Web3DService;

public class XMLTreeView {

    private SAXTreeBuilder saxTree = null;

    private static String file = "";

    public static void main(String args[]) {
        JFrame frame = new JFrame("XMLTreeView: [ games.xml ]");
        frame.setSize(400, 400);
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent ev) {
                System.exit(0);
            }
        });
        file = "games.xml";
        new XMLTreeView(frame, null);
    }

    public XMLTreeView(JFrame frame, Web3DService web3DService) {
        frame.getContentPane().setLayout(new BorderLayout());
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(file);
        saxTree = new SAXTreeBuilder(top);
        InputStream urlIn = null;
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
        try {
            urlIn.close();
        } catch (Exception e) {
        }
        JTree tree = new JTree(saxTree.getTree());
        JScrollPane scrollPane = new JScrollPane(tree);
        frame.getContentPane().add("Center", scrollPane);
        frame.setVisible(true);
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
        if (!str.equals("") && Character.isLetter(str.charAt(0))) currentNode.add(new DefaultMutableTreeNode(str));
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
