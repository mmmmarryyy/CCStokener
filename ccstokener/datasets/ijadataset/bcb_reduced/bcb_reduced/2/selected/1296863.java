package prajna.semantic.accessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import prajna.data.*;
import prajna.semantic.*;
import prajna.util.*;

/**
 * This class is designed to read data from GraphML and TreeML files, and
 * provide the data as a DataAccessor. The CommonMLAccessor aggregates a list
 * of files that it has read, and will convert any of them as needed. It does
 * not retain the information on any particular data structure, so that if the
 * underlying file changes, a new call will regenerate the data as needed.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 */
public class CommonMLAccessor extends SemanticAccessor {

    private HashMap<String, URL> urlRefs = new HashMap<String, URL>();

    private static DocumentBuilder docBuild;

    private static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    static {
        try {
            docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            System.err.println("Cannot initialize CommonMLAccessor:");
            exc.printStackTrace();
        }
    }

    /**
     * Adds a file to this accessor. This method will scan the file to
     * determine which type of file it is, if it is supported, and what the
     * data structures contained within it are.
     * 
     * @param file the file
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException if there is a problem with the XML format
     */
    public void addFile(File file) throws IOException, SAXException {
        addUrl(file.toURI().toURL());
    }

    /**
     * Adds a file by name to this accessor. This method will scan the file to
     * determine which type of file it is, if it is supported, and what the
     * data structures contained within it are.
     * 
     * @param fileName the file name
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException if there is a problem with the XML format
     */
    public void addFile(String fileName) throws IOException, SAXException {
        addUrl(new File(fileName).toURI().toURL());
    }

    /**
     * Adds a file by url to this accessor. This method will scan the file to
     * determine which type of file it is, if it is supported, and what the
     * data structures contained within it are.
     * 
     * @param url the url to the file
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException if there is a problem with the XML format
     * @throws IllegalArgumentException if the file is in an unsupported
     *             format.
     */
    public void addUrl(URL url) throws IOException, SAXException {
        InputStream inStream = url.openStream();
        String path = url.getPath();
        int slashInx = path.lastIndexOf('/');
        String name = path.substring(slashInx + 1);
        Document doc = docBuild.parse(inStream);
        Element root = doc.getDocumentElement();
        String rootTag = root.getTagName();
        if (rootTag.equals("graphml")) {
            NodeList graphNodes = root.getElementsByTagName("graph");
            for (int i = 0; i < graphNodes.getLength(); i++) {
                Element elem = (Element) graphNodes.item(i);
                String graphName = elem.getAttribute("id");
                if (graphName == null) {
                    graphName = name;
                }
                addStructure(new GraphSpec(graphName));
                urlRefs.put(graphName, url);
            }
        } else if (rootTag.equals("tree")) {
            addStructure(new TreeSpec(name));
            urlRefs.put(name, url);
        } else {
            throw new IllegalArgumentException("Format of " + url + " not understood.");
        }
        inStream.close();
    }

    /**
     * Build the branch of a Tree. This method recursively adds all child
     * branches to a tree element created within this method. This method
     * parses any attribute values and stores them in the DataRecord associated
     * with the root node of the branch created by this method
     * 
     * @param attDecls the attribute declaration map, specifying the types for
     *            the various attributes
     * @param element the element The element which is the root of the branch.
     *            This can correspond to either a <code>branch</code> or
     *            <code>leaf</code> element.
     * @param nameTag the name tag The name of the field to be used as the
     *            record name.
     * @return the branch of the tree corresponding to this element
     */
    private Tree<DataRecord> buildBranch(Element element, Map<String, DataType> attDecls, String nameTag) {
        DataRecord rec = new DataRecord();
        Tree<DataRecord> tree = new Tree<DataRecord>(rec);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element elem = (Element) child;
                if (elem.getTagName().equals("attribute")) {
                    String name = elem.getAttribute("name");
                    String value = elem.getAttribute("value");
                    if (name.equals(nameTag)) {
                        rec.setName(value);
                    }
                    DataType type = attDecls.get(name);
                    try {
                        switch(type) {
                            case TEXT:
                                rec.setTextField(name, value);
                                break;
                            case INT:
                                int intVal = Integer.parseInt(value);
                                rec.setIntField(name, intVal);
                                break;
                            case MEASURE:
                                double dblVal = Double.parseDouble(value);
                                rec.setMeasureField(name, new Measure(dblVal, Unitless.RATIO));
                                break;
                            case TIME:
                                Date date = dateFormat.parse(value);
                                rec.setTimeField(name, new TimeSpan(date));
                        }
                    } catch (ParseException exc) {
                        throw new RuntimeException("Error Parsing field " + name + ", value: " + value + " as " + type, exc);
                    }
                } else if (elem.getTagName().equals("branch") || elem.getTagName().equals("leaf")) {
                    Tree<DataRecord> branch = buildBranch(elem, attDecls, nameTag);
                    tree.add(branch);
                }
            }
        }
        return tree;
    }

    /**
     * Create a DataRecord object for the graph. This method creates a new
     * DataRecord for the element, parses the data elements of the object -
     * either a node or edge - and loads the appropriate fields in the
     * DataRecord. It sets any default fields which are not overriden by the
     * specific data elements for this element
     * 
     * @param keyFields a map of the key fields, giving the field information
     * @param element the element to convert to a DataRecord
     * @param type the type of object, either <code>node</code> or
     *            <code>edge</code>
     * @return the data record created
     */
    private DataRecord createGraphObject(Element element, Map<String, FieldDesc<?>> keyFields, String type) {
        String id = element.getAttribute("id");
        DataRecord rec = new DataRecord(id);
        String link = element.getAttribute("xlink:href");
        if (link != null && link.length() > 0) {
            rec.setLink(link);
        }
        for (String key : keyFields.keySet()) {
            if (key.startsWith(type)) {
                FieldDesc<?> desc = keyFields.get(key);
                desc.storeDefaultIntoRecord(rec);
            }
        }
        NodeList dataList = element.getElementsByTagName("data");
        for (int i = 0; i < dataList.getLength(); i++) {
            Element elem = (Element) dataList.item(i);
            String value = ((Text) elem.getFirstChild()).getData();
            String key = elem.getAttribute("key");
            FieldDesc<?> desc = keyFields.get(type + "." + key);
            FieldHandler handler = getFieldHandler(desc.getFieldName());
            try {
                desc.parseValueIntoRecord(rec, value);
                if (handler != null) {
                    handler.handleField(desc.getFieldName(), value, rec);
                }
            } catch (Exception exc) {
                throw new RuntimeException("Invalid or unparsable value for " + desc.getFieldName() + ": " + value, exc);
            }
        }
        return rec;
    }

    /**
     * Retrieves the named dataset. Currently unsupported.
     * 
     * @param datasetName the name of the dataset
     * @return null
     */
    @Override
    public Set<DataRecord> getDataset(String datasetName) {
        return null;
    }

    /**
     * Retrieves the named graph. This method retrieves the graph data from the
     * corresponding URL which contains the information about the named graph.
     * It reconstructs the graph from a GraphML file. This method retrieves the
     * URL for the graph name, and then calls parseGraphML to retrieve the
     * current graph data.
     * 
     * @param graphName the name of the graph
     * @return the graph named, or null if there is no such graph
     */
    @Override
    public Graph<DataRecord, DataRecord> getGraph(String graphName) {
        URL url = urlRefs.get(graphName);
        Graph<DataRecord, DataRecord> graph = null;
        try {
            InputStream inStream = url.openStream();
            graph = parseGraphML(inStream, graphName);
            inStream.close();
        } catch (IOException exc) {
            throw new RuntimeException("Cannot access stream for " + graphName + " at " + url, exc);
        } catch (SAXException exc) {
            throw new RuntimeException("Cannot parse stream for " + graphName + " at " + url, exc);
        }
        return graph;
    }

    /**
     * Retrieves the named grid. Currently unsupported.
     * 
     * @param gridName the name of the grid
     * @return null
     */
    @Override
    public Grid<DataRecord> getGrid(String gridName) {
        return null;
    }

    /**
     * Retrieves the named tree. This method retrieves the tree data from the
     * corresponding URL which contains the information about the named tree.
     * It reconstructs the tree from a TreeML file. This method retrieves the
     * URL for the tree name, and then calls parseTreeML to retrieve the
     * current tree data.
     * 
     * @param treeName the name of the tree
     * @return the tree named, or null if there is no such tree
     */
    @Override
    public Tree<DataRecord> getTree(String treeName) {
        URL url = urlRefs.get(treeName);
        Tree<DataRecord> tree = null;
        try {
            InputStream inStream = url.openStream();
            parseTreeML(inStream);
            inStream.close();
        } catch (IOException exc) {
            throw new RuntimeException("Cannot access stream for " + treeName + " at " + url, exc);
        } catch (SAXException exc) {
            throw new RuntimeException("Cannot parse stream for " + treeName + " at " + url, exc);
        }
        return tree;
    }

    /**
     * Parse the base graph element for a GraphML file. This method extracts
     * the directionality information, and then finds all node elements to
     * build the nodes in the graph. It then extracts all edge elements to
     * build the edges of the graph.
     * 
     * @param graphElem the graph element
     * @param keyFields the map of fields corresponding to the key elements
     * @return the graph created from parsing the elements.
     */
    private Graph<DataRecord, DataRecord> parseGraphElement(Element graphElem, Map<String, FieldDesc<?>> keyFields) {
        String edgeDef = graphElem.getAttribute("edgedefault");
        Graph<DataRecord, DataRecord> graph = (edgeDef == null || edgeDef.equals("directed")) ? new DirectedGraph<DataRecord, DataRecord>() : new UndirectedGraph<DataRecord, DataRecord>();
        HashMap<String, DataRecord> nodeMap = new HashMap<String, DataRecord>();
        NodeList nodeList = graphElem.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element nodeElem = (Element) nodeList.item(i);
            String id = nodeElem.getAttribute("id");
            DataRecord node = createGraphObject(nodeElem, keyFields, "node");
            graph.add(node);
            nodeMap.put(id, node);
        }
        NodeList edgeList = graphElem.getElementsByTagName("edge");
        for (int i = 0; i < edgeList.getLength(); i++) {
            Element edgeElem = (Element) edgeList.item(i);
            String source = edgeElem.getAttribute("source");
            String target = edgeElem.getAttribute("target");
            DataRecord edge = createGraphObject(edgeElem, keyFields, "edge");
            graph.addEdge(edge, nodeMap.get(source), nodeMap.get(target));
        }
        return graph;
    }

    /**
     * Parses a stream containing a graphML format, and returns the graph with
     * the matching name. If the graphName is null, the top-level graph is
     * returned
     * 
     * @param inStream the input stream to read from
     * @param graphName the name of the graph
     * @return the graph created from the graphML data
     * @throws SAXException if there is a problem parsing the graphML
     * @throws IOException if there is a problem with the input stream
     */
    public Graph<DataRecord, DataRecord> parseGraphML(InputStream inStream, String graphName) throws SAXException, IOException {
        Graph<DataRecord, DataRecord> graph = null;
        Document doc = docBuild.parse(inStream);
        Element docElem = doc.getDocumentElement();
        NodeList keyList = docElem.getElementsByTagName("key");
        Map<String, FieldDesc<?>> keyFields = readGraphKeys(keyList);
        NodeList graphList = docElem.getElementsByTagName("graph");
        for (int i = 0; i < graphList.getLength(); i++) {
            Element graphElem = (Element) graphList.item(i);
            String graphId = graphElem.getAttribute("id");
            if (graphList.getLength() == 1 || (graphName == null && i == 0) || (graphId != null && graphId.equals(graphName))) {
                graph = parseGraphElement(graphElem, keyFields);
            }
        }
        return graph;
    }

    /**
     * Parses a stream containing a treeML format, and returns the tree
     * contained in the treeML data
     * 
     * @param inStream the input stream to read from
     * @return the tree created from the treeML data
     * @throws SAXException if there is a problem parsing the treeML
     * @throws IOException if there is a problem with the input stream
     */
    public Tree<DataRecord> parseTreeML(InputStream inStream) throws SAXException, IOException {
        String nameTag = "name";
        Document doc = docBuild.parse(inStream);
        Element docElem = doc.getDocumentElement();
        Map<String, DataType> attDecls = new HashMap<String, DataType>();
        NodeList attList = docElem.getElementsByTagName("attributeDecl");
        for (int i = 0; i < attList.getLength(); i++) {
            Element elem = (Element) attList.item(i);
            String name = elem.getAttribute("name");
            String type = elem.getAttribute("type");
            DataType dataType = DataType.TEXT;
            if (type.equals("Integer") || type.equals("Long")) {
                dataType = DataType.INT;
            } else if (type.equals("Float") || type.equals("Real")) {
                dataType = DataType.MEASURE;
            } else if (type.equals("Date")) {
                dataType = DataType.TIME;
            }
            attDecls.put(name, dataType);
            if (i == 0) {
                nameTag = name;
            }
        }
        Element rootElem = (Element) docElem.getLastChild();
        if (!rootElem.getTagName().equals("branch")) {
            throw new RuntimeException("root is not the last element");
        }
        return buildBranch(rootElem, attDecls, nameTag);
    }

    /**
     * Reads the graph key elements which define the various data fields, as
     * well as any default values
     * 
     * @param keyList the list of key elements from the GraphML file
     * @return a map of field descriptors corresponding to each key, keyed by
     *         the id.
     */
    private Map<String, FieldDesc<?>> readGraphKeys(NodeList keyList) {
        Map<String, FieldDesc<?>> keyMap = new HashMap<String, FieldDesc<?>>();
        for (int i = 0; i < keyList.getLength(); i++) {
            Element elem = (Element) keyList.item(i);
            String id = elem.getAttribute("id");
            String name = elem.getAttribute("attr.name");
            String forStr = elem.getAttribute("for");
            String type = elem.getAttribute("attr.type");
            NodeList defList = elem.getElementsByTagName("default");
            String defVal = null;
            if (defList != null && defList.getLength() > 0) {
                Element defElem = (Element) defList.item(0);
                defVal = ((Text) defElem.getFirstChild()).getData();
            }
            FieldDesc<?> desc = null;
            if (type.equals("int") || type.equals("long")) {
                IntFieldDesc intDesc = new IntFieldDesc(name);
                if (defVal != null) {
                    int defInt = Integer.parseInt(defVal);
                    intDesc.setDefaultValue(defInt);
                }
                desc = intDesc;
            } else if (type.equals("float") || type.equals("double")) {
                MeasureFieldDesc msrDesc = new MeasureFieldDesc(name, Unitless.RATIO);
                if (defVal != null) {
                    Double dblVal = Double.parseDouble(defVal);
                    msrDesc.setDefaultValue(new Measure(dblVal, Unitless.RATIO));
                }
                desc = msrDesc;
            } else {
                TextFieldDesc txtDesc = new TextFieldDesc(name);
                txtDesc.setDefaultValue(defVal);
                desc = txtDesc;
            }
            if (forStr.equals("all")) {
                keyMap.put("node." + id, desc);
                keyMap.put("edge." + id, desc);
            } else {
                keyMap.put(forStr + "." + id, desc);
            }
        }
        return keyMap;
    }

    /**
     * Stub implementation of retrieveRecords. Since this SemanticAccessor
     * retrieves different data structures from specific data files, this
     * method is never used.
     * 
     * @param template the DataTemplate
     * @return an empty set
     */
    @Override
    protected Set<DataRecord> retrieveRecords(DataTemplate template) {
        return new HashSet<DataRecord>();
    }

    /**
     * Set initialization parameters.
     * 
     * @param parameters initialization parameter map
     */
    public void setInitParameters(Map<String, String> parameters) {
    }

    /**
     * Set the query string used to retrieve records. This accessor ignors the
     * query string.
     * 
     * @param query the query string
     */
    @Override
    public void setQuery(String query) {
    }
}
