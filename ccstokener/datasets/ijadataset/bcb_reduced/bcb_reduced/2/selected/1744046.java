package cwi.GraphXML;

import cwi.GraphXML.Elements.*;
import java.util.*;
import java.io.*;
import java.net.URL;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
* Abstract parser. This is a superclass for various xml parsers using different
* XML packages. It implements the management of error listeners, and provides
* the basic interface to access the real parser.
* <p>
* The code relies on the fact that entity reference nodes are expanded by the
* parser. Although some defence mechanisms are built in (to catch and ignore DOM node
* types for entities), entities are not expanded by the rest of the code!
*<p>
* Here is a typical usage of the parser (using the IBM parser as an example):
*
* <pre>
*        AbstractParser theParser = new IBMParser();
*        theParser.setGraphSemantics(new MyGraphSemantics());
*        theParser.addParserErrorListener(new ParserListenerAdapter());
*        
*        theParser.interpret("XMLFileName");
* </pre>
*
* Which uses a specific implementation of a parser, uses a graph semantic class instance,
* (which should implement the <code>GraphSemantics</code> interface), a default
* parser listener for error handling (which prints all messages on the standard error). It then
* calls the parser to interpret a specific file.
* <p>
* By default, a validating parser is used. However, if the system property
* <code>XMLGraph_Validate</code> is set to <code>No</code>, <code>no</code>, <code>False</code>,
* or <code>false</code>, a non-validating parser is used instead.
* <p>
* The error listener mechanism follows the standard Java style, and allows for several
* error reactions to be 'checked in' to the parser.
* <p>
* The parser mechanism is "reentrant", ie no static variables are used. This means that the parser 
* can be invoked recursively by creating a new Parser instance. This may be important when handling,
* for example, metanodes.
* <p>
* The code ensures the DTDPATH facility: if the system property DTDPATH is set, it is considered
* to be a series of directory specification (much like the CLASSPATH variable) and it looks
* for a dtd file in those directory. This is preceded by an attempt to locate the dtd file
* in the same directory as the xml file itself, in the case this latter is specified in term
* of a file and not as a stream.
* Note, however, that this feature may not work with all xml parser implementation. It is known
* to work with xml4j from IBM, but it does not work properly with JAX version 1.0 of SUN.
*
* @see GraphSemantics
*
* @author Ivan Herman
*/
public abstract class AbstractParser implements ParserError {

    /**
    * List of keywords in GraphXML, ie, those node names which are <em>not</em> 
    * user extension nodes. Only node names which do <em>not</em> appear in this
    * list are considered as "extension" nodes (most of the check will be done by
    * the parser anyway...
    * <p>
    * Subtypes may want to change this array by adding some keywords there if they
    * want a finer control over their own extensions. It is not very probably, though,
    * that this would become necessary.
    */
    private static String[] graphxmlNodeNames = { "label", "data", "dataref", "properties", "graph-specific-properties", "style", "subgraph-style", "ref", "graph", "edit", "edit-bundle", "icon", "size", "node", "edge", "position", "transform", "path", "line", "fill", "implementation", "#text", "#comment" };

    /**
    * Node names which may act as stoppers, in the sense that
    * no extension, application data, etc, can appear beyond
    * that point.
    */
    private static String[] stoppers = { "graph", "edit", "edit-bundle", "node", "edge" };

    /**
    * Return the 'stopper' keywords. These are the keywords which follow the
    * 'preamble' in a graph or GraphXML specification. They are used to 
    * stop cycles when rolling through the elements, to avoid unnecessary cycles (for 
    * example when extension elements are looked for.
    * <p> 
    * At this time, these are "graph", "edit", "edit-bundle", "node", and "edge"
    */
    public static String[] getStoppers() {
        return stoppers;
    }

    /**
    * Return the GraphXML keywords. 
    */
    public static String[] getKeywords() {
        return graphxmlNodeNames;
    }

    private ArrayList listeners = new ArrayList();

    /**
    * Add new listener.
    *
    * @param l new listener
    */
    public void addParserErrorListener(ParserErrorListener l) {
        listeners.add(l);
    }

    /**
    * Remove a listener
    *
    * @param listener to be removed.
    */
    public void removeParserErrorListener(ParserErrorListener l) {
        int i = listeners.indexOf(l);
        if (i != -1) try {
            listeners.remove(i);
        } catch (IndexOutOfBoundsException e) {
            ;
        }
    }

    /**
    * Fire parser warning. Subclasses have to call this method from the
    * parser dependend error routines.
    *
    * @param m warning message
    */
    public void fireParserWarning(String m) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            ((ParserErrorListener) (it.next())).parserWarning(m);
        }
    }

    /**
    * Fire parser error. Subclasses have to call this method from the
    * parser dependend error routines.
    *
    * @param m warning message
    */
    public void fireParserError(String m) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            ((ParserErrorListener) (it.next())).parserError(m);
        }
    }

    /**
    * Fire fatal parser error. Subclasses have to call this method from the
    * parser dependend error routines.
    *
    * @param m warning message
    */
    public void fireParserFatalError(String m) {
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            ((ParserErrorListener) (it.next())).parserFatalError(m);
        }
    }

    private void pickPIs(Node doc) {
        NodeList doch = doc.getChildNodes();
        int l = doch.getLength();
        for (int k = 0; k < l; k++) {
            Node ch = doch.item(k);
            int t = ch.getNodeType();
            if (t == Node.PROCESSING_INSTRUCTION_NODE) handleProcessingInstruction(ch);
        }
    }

    private void handleProcessingInstruction(Node node) {
        if (sem == null) return;
        try {
            ProcessingInstruction pi = (ProcessingInstruction) node;
            StringTokenizer tokens = new StringTokenizer(pi.getData());
            HashMap map = new HashMap();
            while (tokens.hasMoreTokens()) {
                StringTokenizer cut = new StringTokenizer(tokens.nextToken(), "=");
                if (cut.hasMoreTokens()) {
                    try {
                        String key = cut.nextToken();
                        key = key.trim();
                        if (cut.hasMoreTokens()) {
                            String value = cut.nextToken();
                            value.trim();
                            value = value.replace('"', ' ');
                            map.put(key, value.trim());
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            sem.handlePI(pi.getTarget(), map);
        } catch (Exception e) {
        }
    }

    private String base = null;

    protected EntityResolver entityResolver = new EntityResolver() {

        /**
            * Resolve system entity names. Remember that returning a 'null'
            * means that the default action is taken by the parser.
            */
        public InputSource resolveEntity(String publicId, String systemId) {
            if (publicId != null || systemId == null) return null;
            URL url = null;
            try {
                url = new URL(systemId);
            } catch (Exception e) {
                return null;
            }
            try {
                return new InputSource(url.openStream());
            } catch (Exception e) {
                ;
            }
            if (!url.getProtocol().equals("file")) {
                return null;
            }
            String fileName = url.getFile();
            if (fileName == null) {
                return null;
            }
            try {
                return new InputSource(new FileInputStream(fileName));
            } catch (Exception ee) {
                ;
            }
            if (base != null) {
                try {
                    File newFile = new File(base, fileName);
                    return new InputSource(new FileInputStream(newFile));
                } catch (Exception ee) {
                    ;
                }
            }
            String dtdp = System.getProperty("DTDPATH");
            String ps = System.getProperty("path.separator");
            if (dtdp != null) {
                StringTokenizer tokens = new StringTokenizer(dtdp, ps);
                while (tokens.hasMoreTokens()) {
                    try {
                        File newFile = new File(tokens.nextToken(), fileName);
                        return new InputSource(new FileInputStream(newFile));
                    } catch (Exception ee) {
                        ;
                    }
                }
            }
            String fs = System.getProperty("file.separator");
            String distr = "cwi" + fs + "GraphXML" + fs + "XML";
            String classes = System.getProperty("java.class.path");
            if (classes != null) {
                StringTokenizer tokens = new StringTokenizer(classes, ps);
                while (tokens.hasMoreTokens()) {
                    try {
                        File newDir = new File(tokens.nextToken(), distr);
                        File newFile = new File(newDir, fileName);
                        return new InputSource(new FileInputStream(newFile));
                    } catch (Exception ee) {
                        ;
                    }
                }
            }
            return null;
        }
    };

    /**
    * Parse a document, return a Document element. The interpretation of the document
    * is left to the caller.
    * <p>
    * This access to the parser is rarely used; use the interpret and decompose methods
    * below instead.
    *
    * @param xmlFile the xml file to parse.
    */
    public Document parse(String xmlFile) {
        InputStream istr = null;
        try {
            istr = new FileInputStream(xmlFile);
            base = (new File(xmlFile)).getAbsoluteFile().getParent();
        } catch (Exception e) {
            fireParserFatalError("[Fatal Error] Exception when opening file: " + e.toString());
            return null;
        }
        return parse(istr);
    }

    /**
    * Parse a document, return a Document element. The interpretation of the document
    * is left to the caller.
    * <p>
    * This access to the parser is rarely used; use the interpret and decompose methods
    * below instead.
    * <p>
    * The system property XMLGraph_Validate is checked agains a "true" or "false" value
    * to decide whether a validating or a non-validating parser is used. If the property
    * is not set, validation is used.
    *
    * @param xmlStream the xml stream to parse.
    */
    public Document parse(InputStream xmlStream) {
        boolean validate = true;
        try {
            String val = System.getProperty("XMLGraph_Validate");
            if (val.equals("false") || val.equals("False") || val.equals("no") || val.equals("No")) validate = false;
        } catch (Exception e) {
            ;
        }
        return parse(xmlStream, validate);
    }

    /**
    * Parse a document, return a Document element. The interpretation of the document
    * is left to the caller.
    * <p>
    * This access to the parser is rarely used; use the interpret and decompose methods
    * below instead.
    * <p>
    * This is the only abstract method in the class, which should be 'filled in' by the
    * XML parser specific sub-class. All other methods can be left intact. 
    * <p>
    * The subclass should take care of including the following code:
    * <pre>
    *      theParser.setEntityResolver(entityResolver);
    * </pre>
    * <p>
    * This is a SAX call which should be used to ensure the DTDPATH facility, ie, that
    * DTD calls may refer to dtd files at various places in the local file directory.
    * Note that not all parser implementation may include that feature yet, or their
    * behaviour might not be appropriate.
    *
    * @param xmlStream the xml stream to parse
    * @param validate decide whether the parser is validating or not
    */
    protected abstract Document parse(InputStream xmlStream, boolean validate);

    private GraphSemantics sem = null;

    /**
    * Set the collection of graph semantic methods.
    *
    * @param semantics GraphSemantics class instance
    */
    public void setGraphSemantics(GraphSemantics semantics) {
        sem = semantics;
    }

    /**
    * This is a simple iteration through the elements of the GraphXML structure,
    * all in one blow.
    *
    * @param doc the full GraphXML document
    */
    private void interpretFullDocument(Document doc) {
        InterpretDocument interpreter = new InterpretDocument(sem, this);
        Element xmlgraph = doc.getDocumentElement();
        NodeList children = xmlgraph.getChildNodes();
        pickPIs(doc);
        Extensions ext = Extensions.createExtensions(children);
        ApplicationData appData = ApplicationData.create(children, this);
        sem.initGraphXML(appData, ext, this);
        interpreter.setFileLevelStyle(Style.create(children, this));
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node child = children.item(i);
            String childName = child.getNodeName();
            int type = child.getNodeType();
            if (type == Node.PROCESSING_INSTRUCTION_NODE) {
                handleProcessingInstruction(child);
            } else if (type == Node.COMMENT_NODE || type == Node.ENTITY_NODE || type == Node.ENTITY_REFERENCE_NODE) {
                continue;
            } else if (childName.equals("graph") == true) {
                interpreter.interpretGraph(child);
            } else if (childName.equals("edit") == true) {
                interpreter.interpretEdit(child);
            } else if (childName.equals("edit-bundle") == true) {
                interpreter.interpretEditBundle(child);
            }
        }
        sem.closeGraphXML();
    }

    /**
    * Creation of a structure iterator for the document, which allows for a finer
    * control over the iteration on various nodes.
    *
    * @param doc the full GraphXML document
    */
    private StructureIterator decomposeDocument(Document doc) {
        InterpretDocument interpreter = new InterpretDocument(sem, this);
        Node[] graphs;
        Node[][] edits;
        pickPIs(doc);
        Element xmlgraph = doc.getDocumentElement();
        NodeList children = xmlgraph.getChildNodes();
        Extensions ext = Extensions.createExtensions(children);
        sem.initGraphXML(ApplicationData.create(children, this), ext, this);
        interpreter.setFileLevelStyle(Style.create(children, this));
        ArrayList theGraphs = new ArrayList();
        ArrayList theEdits = new ArrayList();
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node child = children.item(i);
            String childName = child.getNodeName();
            int type = child.getNodeType();
            if (type == Node.PROCESSING_INSTRUCTION_NODE) {
                handleProcessingInstruction(child);
            } else if (type == Node.COMMENT_NODE || type == Node.ENTITY_NODE || type == Node.ENTITY_REFERENCE_NODE) {
                continue;
            } else if (childName.equals("graph") == true) {
                theGraphs.add(child);
            } else if (childName.equals("edit") == true) {
                Node[] single = new Node[] { child };
                theEdits.add(single);
            } else if (childName.equals("edit-bundle") == true) {
                NodeList list = ((Element) child).getElementsByTagName("edit");
                Node[] multi = new Node[list.getLength()];
                for (int j = 0; j < list.getLength(); j++) multi[j] = list.item(j);
                theEdits.add(multi);
            }
        }
        if (theGraphs.size() > 0) {
            graphs = new Node[theGraphs.size()];
            graphs = (Node[]) theGraphs.toArray(graphs);
        } else {
            graphs = new Node[0];
        }
        if (theEdits.size() > 0) {
            edits = new Node[theEdits.size()][];
            edits = (Node[][]) theEdits.toArray(edits);
        } else {
            edits = new Node[0][];
        }
        return new StructureIterator(interpreter, graphs, edits);
    }

    /**
    * Parse and interpret an xml graph file as one unit.
    *
    * @param xmlFile The xml file
    */
    public void interpret(String xmlFile) {
        if (sem == null) {
            fireParserFatalError("[Fatal Error] No semantic methods have been set");
            return;
        }
        Document doc = parse(xmlFile);
        if (doc != null) {
            interpretFullDocument(doc);
        }
    }

    /**
    * Parse and iterpret an xml graph
    *
    * @param xmlStream the xml stream to parse.
    */
    public void interpret(InputStream xmlStream) {
        if (sem == null) {
            fireParserFatalError("[Fatal Error] No semantic methods have been set");
            return;
        }
        Document doc = parse(xmlStream);
        if (doc != null) {
            interpretFullDocument(doc);
        }
    }

    /**
    * Decompose the document into a structure for a partial interpretation of a document.
    * The document is parsed, broken into 
    * its main constituents only, and this structure is returned to the caller. The
    * idea is that the caller can make a step-by-step interpretation of the content,
    * taking each graph and edit block separately.
    *
    * @param xmlFile the xml file to parse.
    * @return the iteratable structure of the document
    */
    public StructureIterator decompose(String xmlFile) {
        if (sem == null) {
            fireParserFatalError("[Fatal Error] No semantic methods have been set");
            return null;
        }
        Document doc = parse(xmlFile);
        if (doc != null) {
            return decomposeDocument(doc);
        } else {
            return null;
        }
    }

    /**
    * Decompose the document into a structure for a partial interpretation of a document.
    * The document is parsed, broken into 
    * its main constituents only, and this structure is returned to the caller. The
    * idea is that the caller can make a step-by-step interpretation of the content,
    * taking each graph and edit block separately.
    *
    * @param xmlStream the xml stream to parse.
    * @return the iteratable structure of the document
    */
    public StructureIterator decompose(InputStream xmlStream) {
        if (sem == null) {
            fireParserFatalError("[Fatal Error] No semantic methods have been set");
            return null;
        }
        Document doc = parse(xmlStream);
        if (doc != null) {
            return decomposeDocument(doc);
        } else {
            return null;
        }
    }
}
