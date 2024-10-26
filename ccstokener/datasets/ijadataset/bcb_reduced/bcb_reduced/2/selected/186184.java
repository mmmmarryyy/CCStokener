package simplegraphmlparser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author bchisham
 */
public class PhyloTree extends DefaultHandler {

    private HashMap<String, ArrayList<String>> edges;

    private HashMap<String, String> node_to_label;

    private HashMap<String, String> label_to_node;

    private HashMap<String, String> redges;

    private Set<String> processedNodes;

    private String graphID;

    public PhyloTree() {
        this.edges = new HashMap();
        this.redges = new HashMap();
        this.node_to_label = new HashMap();
        this.processedNodes = new TreeSet();
        this.graphID = "defaultId";
    }

    public void parse(InputStream is) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            sp.parse(is, this);
        } catch (IOException ex) {
            Logger.getLogger(PhyloTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PhyloTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(PhyloTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void parse(File file) throws IOException {
        this.parse(file.toURL());
    }

    public void parse(URL url) throws IOException {
        this.parse(url.openStream());
    }

    private void processSubtree(PrintStream ps, String current, int level) {
        for (int i = 0; i < level; ++i) {
            ps.print("  ");
        }
        ps.println(current);
        ArrayList<String> nextNodes = null;
        if ((nextNodes = this.edges.get(current)) != null) {
            Iterator<String> childit = nextNodes.iterator();
            while (childit.hasNext()) {
                processSubtree(ps, childit.next(), level + 1);
            }
        }
    }

    private void findNodeOrder(String current, ArrayList<String> currentSet) {
        currentSet.add(current);
        ArrayList<String> nextNodes = null;
        if ((nextNodes = this.edges.get(current)) != null) {
            Iterator<String> childit = nextNodes.iterator();
            while (childit.hasNext()) {
                String child = childit.next();
                findNodeOrder(child, currentSet);
            }
        }
    }

    public void printTree(PrintStream ps) {
        processSubtree(ps, findRoot(), 0);
    }

    public void printGraphML(PrintStream ps) {
        ps.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"\n" + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + " xmlns:cdao=\"http://www.evolutionaryontology.org/cdao.owl#\"\n" + " xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n" + " http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");
        ps.println("  <key id=\"d0\" for=\"all\" attr.name=\"IdLabel\" attr.type=\"string\"/>");
        ps.println("  <!--<key id=\"d1\" for=\"edge\" attr.name=\"color\" attr.type=\"string\">black</key>-->");
        ps.println("  <graph id=\"" + this.graphID + "\" edgedefault=\"directed\">");
        ArrayList<String> nodesInOrder = new ArrayList();
        findNodeOrder(findRoot(), nodesInOrder);
        Iterator<String> cnode = nodesInOrder.iterator();
        while (cnode.hasNext()) {
            String tn = cnode.next();
            ps.println("  <node id=\"" + tn + "\">");
            ps.println("   <data key=\"d0\">" + this.node_to_label.get(tn) + "</data>");
            ps.println("  </node>");
        }
        cnode = nodesInOrder.iterator();
        int edgeNo = 0;
        while (cnode.hasNext()) {
            String src = cnode.next();
            ArrayList<String> targets = this.edges.get(src);
            if (targets != null) {
                Iterator<String> ctit = targets.iterator();
                while (ctit.hasNext()) {
                    String ct = ctit.next();
                    ps.println("  <edge id=\"edge" + edgeNo++ + "\" source=\"" + src + "\" target=\"" + ct + "\">");
                    ps.println("<data key=\"d0\">" + src.substring(src.lastIndexOf('#')) + "_" + ct.substring(ct.lastIndexOf('#') + 1) + "</data>");
                    ps.println("</edge>");
                }
            }
        }
        ps.println("</graph></graphml>");
    }

    public String findRoot() {
        Iterator<String> current = this.edges.keySet().iterator();
        while (current.hasNext()) {
            String cnode = current.next();
            if (this.redges.get(cnode) == null) {
                return cnode;
            }
        }
        return null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("edge")) {
            String src = attributes.getValue("source");
            String dest = attributes.getValue("target");
            ArrayList<String> children = edges.get(src);
            if (children == null) {
                children = new ArrayList();
            }
            children.add(dest);
            edges.put(src, children);
            redges.put(dest, src);
        } else if (qName.equals("node")) {
            String currentNode = attributes.getValue("id");
            this.node_to_label.put(currentNode, currentNode.substring(currentNode.lastIndexOf('#')));
        } else if (qName.equals("graph")) {
            this.graphID = attributes.getValue("id");
        } else if (qName.equals("data")) {
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }
}
