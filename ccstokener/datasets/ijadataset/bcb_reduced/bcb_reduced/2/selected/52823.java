package nexmltophyloxml;

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
public class NeXMLTree extends DefaultHandler {

    private HashMap<String, ArrayList<String>> edges;

    private HashMap<String, String> node_to_label;

    private HashMap<String, String> node_to_sequence;

    private HashMap<String, String> label_to_node;

    private HashMap<String, String> redges;

    private Set<String> processedNodes;

    private String graphID;

    private String filename;

    int level;

    public NeXMLTree() {
        this.edges = new HashMap();
        this.redges = new HashMap();
        this.node_to_label = new HashMap();
        this.node_to_sequence = new HashMap();
        this.processedNodes = new TreeSet();
        this.graphID = "defaultId";
        this.level = 0;
    }

    public void parse(InputStream is) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            sp.parse(is, this);
        } catch (IOException ex) {
            Logger.getLogger(NeXMLTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(NeXMLTree.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(NeXMLTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void parse(File file) throws IOException {
        this.filename = file.getPath();
        this.parse(file.toURL());
    }

    public void parse(URL url) throws IOException {
        this.filename = url.toExternalForm();
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

    public void printPhyloXML(PrintStream ps) {
        ps.println("<phyloxml:phyloxml\n" + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + "  xmlns:phyloxml=\"http://www.phyloxml.org\"\n" + "  xsi:schemaLocation=\"http://www.phyloxml.org http://www.phyloxml.org/1.10/phyloxml.xsd\">\n");
        ps.println("  <phyloxml:phylogeny>");
        ps.println("    <phyloxml:name>" + this.graphID + "</phyloxml:name>");
        ps.println("     <phyloxml:description>" + this.filename + "</phyloxml:description>");
        makeClades(ps, findRoot(), 2);
        ps.println("    </phyloxml:phylogeny>");
        ps.println(" </phyloxml:phyloxml>");
    }

    private String levelPrefix(int level) {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < level; ++i) {
            ret = ret.append("   ");
        }
        return ret.toString();
    }

    private void makeClades(PrintStream ps, String current, int level) {
        ArrayList<String> nextNodes;
        String prefix = levelPrefix(level);
        ps.println(prefix + "<phyloxml:clade>");
        if (this.node_to_label.containsKey(current)) {
            ps.println(prefix + " <phyloxml:taxonomy>");
            ps.println(prefix + "     <phyloxml:scientific_name>" + this.node_to_label.get(current) + "</phyloxml:scientific_name>");
            ps.println(prefix + " </phyloxml:taxonomy>");
        }
        if ((nextNodes = this.edges.get(current)) != null) {
            Iterator<String> childit = nextNodes.iterator();
            while (childit.hasNext()) {
                String child = childit.next();
                makeClades(ps, child, level + 1);
            }
        }
        ps.println(prefix + "</phyloxml:clade>");
    }

    private String findRoot() {
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
            String currentLabel = attributes.getValue("label");
            if (currentLabel != null && currentLabel.length() > 0) {
                this.node_to_label.put(currentNode, currentLabel);
            }
        } else if (qName.equals("tree") || qName.equals("network")) {
            this.graphID = attributes.getValue("id");
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }
}
