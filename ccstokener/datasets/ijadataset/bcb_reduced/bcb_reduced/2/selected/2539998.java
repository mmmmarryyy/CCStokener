package com.touchgraph.graphlayout.graphelements;

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import net.n3.nanoxml.*;
import java.util.zip.ZipInputStream;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * <b>XMLio:</b> Allows for reading and writing of TouchGraph Link Browser XML
 * files .
 * </p>
 * 
 * NanoXML is the open source XML parser used by the TouchGraph LinkBrowser
 * NanoXML is distrubuted under the zlib/libpng license Copyrighted (c)2000-2001
 * Marc De Scheemaecker, All Rights Reserved http://nanoxml.sourceforge.net/
 * 
 * @author Alexander Shapiro
 * @version 1.20
 */
public class XMLio {

    final GraphEltSet graphEltSet;

    Map<String, String> parameterHash;

    public XMLio(GraphEltSet ges) {
        graphEltSet = ges;
        parameterHash = new Hashtable<String, String>();
    }

    public void setParameterHash(Map<String, String> ph) {
        parameterHash = ph;
    }

    public Map<String, String> getParameterHash() {
        return parameterHash;
    }

    public void read(String fileName, Thread afterReading) throws Exception {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            read(fileName, is, afterReading);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
	 * Reads data from a URL <tt>url</tt>, executing the <tt>afterReading</tt>
	 * Thread after the data is read in.
	 * @throws Exception 
	 */
    public void read(URL url, Thread afterReading) throws Exception {
        read(url.toString(), url.openConnection().getInputStream(), afterReading);
    }

    private void read(String fileName, InputStream in, Thread afterReading) throws Exception {
        InputStream xmlStream;
        if (fileName.toLowerCase(Locale.getDefault()).endsWith(".zip")) {
            xmlStream = new ZipInputStream(in);
            ((ZipInputStream) xmlStream).getNextEntry();
        } else if (fileName.toLowerCase(Locale.getDefault()).endsWith(".gz")) {
            xmlStream = new GZIPInputStream(in);
        } else {
            xmlStream = in;
        }
        IXMLParser parser = new StdXMLParser();
        parser.setBuilder(new StdXMLBuilder());
        parser.setValidator(new NonValidator());
        StdXMLReader reader = new StdXMLReader(xmlStream);
        parser.setReader(reader);
        IXMLElement tglbXML = null;
        try {
            tglbXML = (IXMLElement) parser.parse();
            xmlStream.close();
            buildGraphEltSet(tglbXML, afterReading);
        } catch (Exception e) {
            System.out.println("LINE " + reader.getLineNr());
            e.printStackTrace();
        }
    }

    private boolean getBooleanAttr(IXMLElement elt, String name, boolean def) {
        String value = elt.getAttribute(name, def ? "true" : "false");
        return value.toLowerCase(Locale.getDefault()).equals("true");
    }

    String encodeColor(Color c) {
        if (c == null) return null;
        int rgb = c.getRGB() & 0xffffff;
        String zeros = "000000";
        String data = Integer.toHexString(rgb);
        return (zeros.substring(data.length()) + data).toUpperCase(Locale.getDefault());
    }

    private void buildGraphEltSet(IXMLElement tglbXML, Thread afterReading) throws Exception {
        IXMLElement nodeSet = (IXMLElement) tglbXML.getChildrenNamed("NODESET").firstElement();
        Enumeration<?> nodeEnum = (nodeSet).enumerateChildren();
        while (nodeEnum.hasMoreElements()) {
            IXMLElement node = (IXMLElement) (nodeEnum.nextElement());
            String nodeID = node.getAttribute("nodeID", null);
            Node newNode = new Node(nodeID);
            Vector<?> v;
            v = node.getChildrenNamed("NODE_LOCATION");
            if (!v.isEmpty()) {
                IXMLElement nodeLocation = (IXMLElement) v.firstElement();
                int x = nodeLocation.getAttribute("x", 0);
                int y = nodeLocation.getAttribute("y", 0);
                newNode.setLocation(new Point(x, y));
                newNode.setVisible(getBooleanAttr(nodeLocation, "visible", false));
            }
            v = node.getChildrenNamed("NODE_LABEL");
            if (!v.isEmpty()) {
                IXMLElement nodeLabel = (IXMLElement) v.firstElement();
                newNode.setLabel(nodeLabel.getAttribute("label", " "));
                newNode.setType(nodeLabel.getAttribute("shape", 0));
                try {
                    newNode.setBackColor(Color.decode("#" + nodeLabel.getAttribute("backColor", "000000")));
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
                try {
                    newNode.setTextColor(Color.decode("#" + nodeLabel.getAttribute("textColor", "000000")));
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            graphEltSet.addNode(newNode);
        }
        IXMLElement edgeSet = (IXMLElement) tglbXML.getChildrenNamed("EDGESET").firstElement();
        Enumeration<?> edgeEnum = (edgeSet).enumerateChildren();
        while (edgeEnum.hasMoreElements()) {
            IXMLElement edge = (IXMLElement) (edgeEnum.nextElement());
            String fromID = edge.getAttribute("fromID", null);
            String toID = edge.getAttribute("toID", null);
            int length = edge.getAttribute("length", 4000);
            Node fromNode = graphEltSet.findNode(fromID);
            Node toNode = graphEltSet.findNode(toID);
            final String colorString = edge.getAttribute("color", "000000");
            final Color color = Color.decode("#" + colorString);
            Edge newEdge = new Edge(fromNode, toNode, length, color);
            int edgeType = edge.getAttribute("type", 0);
            switch(edgeType) {
                case 0:
                    newEdge.setType(Edge.BIDIRECTIONAL_EDGE);
                    break;
                case 1:
                    newEdge.setType(Edge.HIERARCHICAL_EDGE);
                    break;
                default:
                    newEdge.setType(Edge.DEFAULT_TYPE);
                    break;
            }
            newEdge.setVisible(getBooleanAttr(edge, "visible", false));
            graphEltSet.addEdge(newEdge);
        }
        parameterHash.clear();
        Vector<?> paramV = tglbXML.getChildrenNamed("PARAMETERS");
        if (paramV != null && !paramV.isEmpty()) {
            IXMLElement parameters = (IXMLElement) paramV.firstElement();
            Enumeration<?> paramEnum = (parameters).enumerateChildren();
            while (paramEnum.hasMoreElements()) {
                IXMLElement param = (IXMLElement) (paramEnum.nextElement());
                String name = param.getAttribute("name", null);
                String value = param.getAttribute("value", null);
                if (name != null) parameterHash.put(name, value);
            }
        }
        if (afterReading != null) afterReading.start();
    }
}
