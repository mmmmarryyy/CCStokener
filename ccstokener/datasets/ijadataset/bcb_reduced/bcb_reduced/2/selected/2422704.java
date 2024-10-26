package org.mediavirus.graphl.graph.rdf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.mediavirus.graphl.GraphlRegistry;
import org.mediavirus.graphl.graph.Edge;
import org.mediavirus.graphl.graph.GraphElement;
import org.mediavirus.graphl.graph.Node;
import org.mediavirus.graphl.vocabulary.NS;
import org.mediavirus.util.ParseUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import edu.unika.aifb.rdf.api.syntax.RDFConsumer;
import edu.unika.aifb.rdf.api.syntax.RDFParser;

public class RDFGraphReader implements RDFConsumer {

    RDFGraph graph;

    URL url;

    Node sourceNode;

    boolean reload = false;

    boolean loading = false;

    List<GraphElement> elementsToRemove;

    List<URL> loadedURLs;

    int loadCount;

    float loadAlpha;

    public RDFGraphReader(RDFGraph graph, URL url) {
        this.graph = graph;
        this.url = url;
    }

    public List<URL> read() throws IOException {
        try {
            loadCount = 0;
            loadAlpha = 0;
            loadedURLs = new ArrayList<URL>();
            loadedURLs.add(url);
            sourceNode = graph.getNodeById(url.toString());
            if (sourceNode == null) {
                sourceNode = graph.getNodeOrAdd(url.toString());
                reload = false;
            } else {
                elementsToRemove = new ArrayList<GraphElement>(sourceNode.getNeighbours(NS.graphl + "definedIn", Node.REVERSE));
                elementsToRemove.remove(sourceNode);
                elementsToRemove.addAll(graph.getEdgesWithPropertyValue(NS.graphl + "definedIn", url.toString()));
                reload = true;
            }
            if (sourceNode.getNeighbours(NS.graphl + "definedIn", Node.FORWARD).size() == 0) {
                Edge edge = graph.createEdge(sourceNode, sourceNode);
                edge.setSource(NS.graphl + "SYSTEM");
                edge.setType(NS.graphl + "definedIn");
                graph.addElements(null, Collections.singleton(edge));
            }
            InputSource input;
            input = new InputSource(url.openConnection().getInputStream());
            input.setSystemId(url.toString());
            RDFParser parser = new RDFParser();
            loading = true;
            try {
                parser.parse(input, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            loading = false;
            if (reload) {
                for (GraphElement element : elementsToRemove) {
                    if (element instanceof Node) {
                        Node node = (Node) element;
                        List<Node> nodeSources = node.getNeighbours(NS.graphl + "definedIn", Node.FORWARD);
                        if (nodeSources.size() > 1) {
                            elementsToRemove.remove(node);
                        }
                    }
                }
                graph.removeElements(elementsToRemove);
            }
            return loadedURLs;
        } finally {
            loading = false;
            reload = false;
        }
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#startModel(java.lang.String)
     */
    public void startModel(String physicalURI) throws SAXException {
        if (GraphlRegistry.DEBUG) System.out.println("RDF: startModel");
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#endModel()
     */
    public void endModel() throws SAXException {
        if (GraphlRegistry.DEBUG) System.out.println("RDF: endModel");
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#statementWithResourceValue(java.lang.String, java.lang.String, java.lang.String)
     */
    public void statementWithResourceValue(String subject, String predicate, String object) throws SAXException {
        try {
            subject = new URL(url, subject).toString();
        } catch (MalformedURLException muex) {
        }
        try {
            object = new URL(url, object).toString();
        } catch (MalformedURLException muex) {
        }
        Node snode;
        Node onode;
        try {
            snode = getNodeOrAdd(subject);
            onode = getNodeOrAdd(object);
            List edges = snode.getEdgesFrom();
            RDFEdge edge = null;
            boolean exists = false;
            for (Iterator iter = edges.iterator(); iter.hasNext(); ) {
                edge = (RDFEdge) iter.next();
                if ((edge.getTo() == onode) && (edge.getType().equals(predicate))) {
                    exists = true;
                    if (reload) elementsToRemove.remove(edge);
                    break;
                }
            }
            if (!exists) {
                edge = new RDFEdge(snode, onode);
                edge.setType(predicate);
                edge.setSource(url.toString());
                graph.addElements(null, Collections.singleton((Edge) edge));
                if (GraphlRegistry.DEBUG) System.out.println("created edge " + subject + ", " + predicate + ", " + object);
                if (predicate.equals("http://www.w3.org/2002/07/owl#imports") || predicate.equals(NS.rdfs + "seeAlso")) {
                    try {
                        URL importURL = new URL(url, object);
                        System.out.println("importing " + importURL.toString() + " ... ");
                        RDFGraphReader importReader = new RDFGraphReader(graph, importURL);
                        loadedURLs.addAll(importReader.read());
                    } catch (Exception ex) {
                        System.out.println("Error importing " + object + " : " + ex.toString());
                    }
                }
            }
        } catch (Exception ex) {
            if (GraphlRegistry.DEBUG) System.out.println("Error while reading triple: " + subject + ", " + predicate + ", " + object);
        }
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#statementWithLiteralValue(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void statementWithLiteralValue(String subject, String predicate, String object, String language, String datatype) throws SAXException {
        try {
            subject = new URL(url, subject).toString();
        } catch (MalformedURLException muex) {
        }
        Node snode;
        try {
            snode = getNodeOrAdd(subject);
            snode.setProperty(predicate, object);
            if (GraphlRegistry.DEBUG) System.out.println("created property " + subject + ", " + predicate + ", " + object);
        } catch (Exception ex) {
            if (GraphlRegistry.DEBUG) System.out.println("Error while reading triple: " + subject + ", " + predicate + ", " + object);
        }
    }

    protected Node getNodeOrAdd(String uri) {
        Node node = graph.getNodeById(uri);
        if (node == null) {
            node = graph.getNodeOrAdd(uri);
            float r = 20 + loadCount;
            loadAlpha += 30 / r;
            node.setCenter(r * Math.sin(loadAlpha), r * Math.cos(loadAlpha));
            loadCount++;
        } else if (reload) {
            if (!ParseUtils.guessName(uri).startsWith("genid")) {
                elementsToRemove.remove(node);
            } else {
            }
        }
        boolean found = false;
        for (Iterator i = node.getNeighbours(NS.graphl + "definedIn", Node.FORWARD).iterator(); i.hasNext(); ) {
            Node source = (Node) i.next();
            if (source.equals(sourceNode)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Edge edge = graph.createEdge(node, sourceNode);
            edge.setSource(NS.graphl + "SYSTEM");
            edge.setType(NS.graphl + "definedIn");
            graph.addElements(null, Collections.singleton(edge));
        }
        return node;
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#logicalURI(java.lang.String)
     */
    public void logicalURI(String logicalURI) throws SAXException {
        if (GraphlRegistry.DEBUG) System.out.println("RDF: logicalURI: " + logicalURI);
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#includeModel(java.lang.String, java.lang.String)
     */
    public void includeModel(String logicalURI, String physicalURI) throws SAXException {
        if (GraphlRegistry.DEBUG) System.out.println("RDF: includeModel: " + logicalURI + ", " + physicalURI);
    }

    /**
     * @see edu.unika.aifb.rdf.api.syntax.RDFConsumer#addModelAttribte(java.lang.String, java.lang.String)
     */
    public void addModelAttribte(String key, String value) throws SAXException {
        if (GraphlRegistry.DEBUG) System.out.println("RDF: addModelAttribte" + key + ", " + value);
    }
}
