package prajna.semantic.accessor;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import prajna.data.*;
import prajna.semantic.*;
import prajna.util.*;

/**
 * Accessor which loads XML data files. The StructureReader reads the XML
 * format data that is written by the StructureWriter class.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 * @see prajna.semantic.writer.StructureWriter
 */
public class StructureReader extends SemanticAccessor {

    private HashSet<DataRecord> dataset = new HashSet<DataRecord>();

    private Graph<DataRecord, DataRecord> graph = null;

    private Tree<DataRecord> tree = null;

    private Grid<DataRecord> grid = null;

    private static LocationFieldDesc locDesc = new LocationFieldDesc();

    private String curDataset;

    private String curGraph;

    private String curGrid;

    private String curTree;

    private HashMap<String, MeasureFieldDesc> msrFields = new HashMap<String, MeasureFieldDesc>();

    private static TimeFieldDesc timDesc = new TimeFieldDesc();

    /**
     * Craete a DataRecord from an XML element. The XML element contains a
     * series of field definition elements. This method parses those elements
     * into fields within the DataRecord.
     * 
     * @param elem the element
     * @return the DataRecord created.
     */
    private DataRecord createRecord(Element elem) {
        String name = elem.getAttribute("name");
        DataRecord rec = new DataRecord(name);
        NodeList fieldElems = elem.getElementsByTagName("field");
        for (int i = 0; i < fieldElems.getLength(); i++) {
            Element fieldElem = (Element) fieldElems.item(i);
            String fieldName = fieldElem.getAttribute("fieldName");
            String fieldType = fieldElem.getAttribute("fieldType");
            String value = ((Text) fieldElem.getFirstChild()).getData();
            try {
                if (fieldType == null || fieldType.equals("text")) {
                    rec.addTextFieldValue(fieldName, value);
                } else if (fieldType.equals("enum")) {
                    rec.addEnumFieldValue(fieldName, value);
                } else if (fieldType.equals("location")) {
                    Location loc = locDesc.parse(value);
                    rec.addLocationFieldValue(fieldName, loc);
                } else if (fieldType.equals("measure")) {
                    String className = fieldElem.getAttribute("unitClass");
                    MeasureFieldDesc msrDesc = findMeasureField(fieldName, className);
                    Measure msr = msrDesc.parse(value);
                    rec.addMeasureFieldValue(fieldName, msr);
                } else if (fieldType.equals("int")) {
                    rec.addIntFieldValue(fieldName, Integer.parseInt(value));
                } else if (fieldType.equals("time")) {
                    TimeSpan span = timDesc.parse(value);
                    rec.addTimeFieldValue(fieldName, span);
                }
            } catch (ParseException exc) {
                System.err.println("error parsing element in record " + name + " with fieldName " + fieldName + "  type: " + fieldType + " Value=" + value);
            }
        }
        return rec;
    }

    /**
     * Find the MeasureField Descriptor for a particular field. If the
     * descriptor has not been created, initialize it with the given class. If
     * the class cannot be loaded, or is not an implementation of
     * prajna.data.Unit, the measure field is initialized as a Unitless field
     * 
     * @param fieldName the field name within the DataRecord and XML definition
     * @param className the name of the class to use for the Unit of Measure.
     *            THis class must implement the prajna.data.Unit interface
     * @return the MeasureField Descriptor for the field
     */
    private MeasureFieldDesc findMeasureField(String fieldName, String className) {
        MeasureFieldDesc desc = msrFields.get(fieldName);
        if (desc == null) {
            Class<? extends Unit> unitClass = prajna.data.Unitless.class;
            if (className != null) {
                try {
                    Class<?> cls = Class.forName(className);
                    unitClass = cls.asSubclass(prajna.data.Unit.class);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
            desc = new MeasureFieldDesc(fieldName, unitClass);
            msrFields.put(fieldName, desc);
        }
        return desc;
    }

    /**
     * Get the named dataset. If the dataset matches the dataset currently
     * loaded, this method simply returns it. Otherwise, it attempts to open
     * the dataset as a URL or file, and load it.
     * 
     * @param datasetName the named dataset, which should be a URL or file name
     * @return the data set
     */
    @Override
    public Set<DataRecord> getDataset(String datasetName) {
        if (!datasetName.equals(curDataset)) {
            InputStream inStream = openStream(datasetName);
            if (inStream != null) {
                loadData(inStream, datasetName);
            }
        }
        return (datasetName.equals(curDataset)) ? dataset : null;
    }

    /**
     * Get the named graph. If the graph matches the graph currently loaded,
     * this method simply returns it. Otherwise, it attempts to open the graph
     * as a URL or file, and load it.
     * 
     * @param graphName the named graph, which should be a URL or file name
     * @return the graph
     */
    @Override
    public Graph<DataRecord, DataRecord> getGraph(String graphName) {
        if (!graphName.equals(curGraph)) {
            InputStream inStream = openStream(graphName);
            if (inStream != null) {
                loadData(inStream, graphName);
            }
        }
        return (graphName.equals(curGraph)) ? graph : null;
    }

    /**
     * Get the named grid. If the grid matches the grid currently loaded, this
     * method simply returns it. Otherwise, it attempts to open the grid as a
     * URL or file, and load it.
     * 
     * @param gridName the named grid, which should be a URL or file name
     * @return the grid
     */
    @Override
    public Grid<DataRecord> getGrid(String gridName) {
        if (!gridName.equals(curGrid)) {
            InputStream inStream = openStream(gridName);
            if (inStream != null) {
                loadData(inStream, gridName);
            }
        }
        return (gridName.equals(curGrid)) ? grid : null;
    }

    /**
     * Get the named tree. If the tree matches the tree currently loaded, this
     * method simply returns it. Otherwise, it attempts to open the tree as a
     * URL or file, and load it.
     * 
     * @param treeName the named tree, which should be a URL or file name
     * @return the tree
     */
    @Override
    public Tree<DataRecord> getTree(String treeName) {
        if (!treeName.equals(curTree)) {
            InputStream inStream = openStream(treeName);
            if (inStream != null) {
                loadData(inStream, treeName);
            }
        }
        return (treeName.equals(curTree)) ? tree : null;
    }

    /**
     * Load data from an input stream. This method opens the stream, parses the
     * XML document, and determines which structure the file contains. It then
     * calls one of the loadStructure methods to load the actual structure from
     * the XML
     * 
     * @param inStream the input stream to read
     * @param name the name of the data structure. This should be the file
     *            name, or URL string, for the file
     */
    private void loadData(InputStream inStream, String name) {
        try {
            DocumentBuilder docBuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuild.parse(inStream);
            Element root = doc.getDocumentElement();
            NodeList elemList = root.getChildNodes();
            for (int i = 0; i < elemList.getLength(); i++) {
                if (elemList.item(i) instanceof Element) {
                    Element elem = (Element) elemList.item(i);
                    String tagName = elem.getTagName();
                    if (tagName.equals("graph")) {
                        loadGraph(elem);
                        curGraph = name;
                    } else if (tagName.equals("tree")) {
                        loadTree(elem);
                        curTree = name;
                    } else if (tagName.equals("grid")) {
                        loadGrid(elem);
                        curGrid = name;
                    } else {
                        loadDataset(elem);
                        curDataset = name;
                    }
                }
            }
        } catch (Exception exc) {
            throw new RuntimeException("Cannot initialize " + getClass().getName() + " from Stream", exc);
        }
    }

    /**
     * Set the configuration from a local file. This convenience method simply
     * opens a FileInputStream and calls <code>loadData(InputStream)</code>
     * 
     * @param dataFile The path to the configuration file
     * @throws FileNotFoundException if the file is not found
     */
    public void loadDataFile(String dataFile) throws FileNotFoundException {
        FileInputStream inStream = new FileInputStream(dataFile);
        loadData(inStream, dataFile);
    }

    /**
     * Set the configuration from a URL. This convenience method simply opens
     * an input stream to the URL, and calls <code>loadData(InputStream)</code>
     * 
     * @param dataUrl the configuration file as a URL
     * @throws IOException if there is a problem opening or reading from the
     *             URL
     */
    public void loadDataFile(URL dataUrl) throws IOException {
        loadData(dataUrl.openStream(), dataUrl.toString());
    }

    /**
     * Load the dataset from an XML element. This method parses the nodes, and
     * loads them into DataRecords.
     * 
     * @param elem the element to load the dataset from
     */
    private void loadDataset(Element elem) {
        NodeList kids = elem.getChildNodes();
        HashSet<DataRecord> recs = new HashSet<DataRecord>();
        for (int i = 0; i < kids.getLength(); i++) {
            Node node = kids.item(i);
            if (node instanceof Element) {
                DataRecord rec = createRecord((Element) node);
                if (rec != null) {
                    recs.add(rec);
                }
            }
        }
        dataset = recs;
    }

    /**
     * Load the graph from an XML element. This method parses the nodes and
     * edges, and loads them into DataRecords.
     * 
     * @param elem the element to load the graph from
     */
    private void loadGraph(Element elem) {
        NodeList nodeElems = elem.getElementsByTagName("node");
        HashMap<String, DataRecord> nodeMap = new HashMap<String, DataRecord>();
        String directStr = elem.getAttribute("directed");
        boolean directed = directStr != null && Boolean.parseBoolean(directStr);
        graph = (directed) ? new DirectedGraph<DataRecord, DataRecord>() : new UndirectedGraph<DataRecord, DataRecord>();
        for (int i = 0; i < nodeElems.getLength(); i++) {
            Element nodeElem = (Element) nodeElems.item(i);
            DataRecord rec = createRecord(nodeElem);
            if (rec.getName() != null) {
                nodeMap.put(rec.getName(), rec);
            }
            graph.add(rec);
        }
        NodeList edgeElems = elem.getElementsByTagName("edge");
        for (int i = 0; i < edgeElems.getLength(); i++) {
            Element edgeElem = (Element) edgeElems.item(i);
            DataRecord rec = createRecord(edgeElem);
            String from = edgeElem.getAttribute("from");
            String to = edgeElem.getAttribute("to");
            if (from != null && to != null) {
                DataRecord orig = nodeMap.get(from);
                DataRecord dest = nodeMap.get(to);
                graph.addEdge(rec, orig, dest);
            }
        }
    }

    /**
     * Load the grid from an XML element. This method parses the cell elements,
     * and loads them into DataRecords.
     * 
     * @param elem the element to load the grid from
     */
    private void loadGrid(Element elem) {
        NodeList cellElems = elem.getElementsByTagName("cell");
        String dimStr = elem.getAttribute("dimension");
        int dimCnt = Integer.parseInt(dimStr);
        String contStr = elem.getAttribute("continuous");
        boolean cont = contStr.equalsIgnoreCase("true");
        HashMap<double[], DataRecord> tmpGrid = new HashMap<double[], DataRecord>();
        for (int i = 0; i < cellElems.getLength(); i++) {
            Element cellElem = (Element) cellElems.item(i);
            DataRecord rec = createRecord(cellElem);
            String coordStr = cellElem.getAttribute("coord");
            String[] coords = coordStr.split(",");
            double[] newCoord = new double[dimCnt];
            for (int j = 0; j < dimCnt; j++) {
                newCoord[j] = Double.parseDouble(coords[j]);
            }
            tmpGrid.put(newCoord, rec);
        }
        if (!cont) {
            int[] max = new int[dimCnt];
            for (double[] coord : tmpGrid.keySet()) {
                for (int i = 0; i < dimCnt; i++) {
                    if (coord[i] > max[i]) {
                        max[i] = (int) Math.ceil(coord[i]);
                    }
                }
            }
            grid = new CellGrid<DataRecord>(max);
        } else {
            grid = new RealGrid<DataRecord>(dimCnt);
        }
        for (double[] coord : tmpGrid.keySet()) {
            DataRecord rec = tmpGrid.get(coord);
            grid.set(coord, rec);
        }
    }

    /**
     * Load the tree from an XML element. This method parses the tree nodes,
     * and loads them into DataRecords.
     * 
     * @param elem the element to load the tree from
     */
    private void loadTree(Element elem) {
        NodeList treeElems = elem.getElementsByTagName("treeNode");
        if (treeElems.getLength() > 1) {
            System.err.println("Malformed Tree Specification");
        }
        Element rootElem = (Element) treeElems.item(0);
        tree = parseTreeElement(rootElem);
    }

    /**
     * Open the named object as an input stream. This method first tries to
     * parse the string into a URL and open a stream to it. If that fails, it
     * tries to parse the string as a file, opening a FileInputStrea
     * 
     * @param dataName the URL or file to open
     * @return the InputStream that can be used to read data from the URL or
     *         file
     */
    private InputStream openStream(String dataName) {
        InputStream inStream = null;
        try {
            URL url = new URL(dataName);
            inStream = url.openStream();
        } catch (Exception exc) {
            File file = new File(dataName);
            if (file.exists() && file.canRead()) {
                try {
                    inStream = new FileInputStream(file);
                } catch (FileNotFoundException exc1) {
                }
            }
        }
        return inStream;
    }

    /**
     * Parse a tree XML element into a Tree object. This recursive method
     * creates a Tree by creating the root node, and then iterating over the
     * list of children.
     * 
     * @param elem the element containing the tree node
     * @return the tree created
     */
    private Tree<DataRecord> parseTreeElement(Element elem) {
        DataRecord rec = createRecord(elem);
        Tree<DataRecord> node = new Tree<DataRecord>(rec);
        NodeList childElems = elem.getElementsByTagName("children");
        Element childElem = (Element) childElems.item(0);
        NodeList nodeElems = childElem.getElementsByTagName("treeNode");
        for (int i = 0; i < nodeElems.getLength(); i++) {
            Element nodeElem = (Element) nodeElems.item(i);
            node.add(parseTreeElement(nodeElem));
        }
        return node;
    }

    /**
     * Retrieve a set of data records for a particular DataTemplate. This
     * method is used by the default implementation of the various
     * get<I>Structure</i> methods. However, since the StructureReader
     * overrides the getStructure methods, this method is not used. Therefore,
     * this implementation is a stub
     * 
     * @param template The data template specifying field mappings and
     *            descriptions
     * @return null
     */
    @Override
    protected Set<DataRecord> retrieveRecords(DataTemplate template) {
        return null;
    }

    /**
     * Set the initialization parameters.
     * 
     * @param parameters the initialization parameters
     */
    public void setInitParameters(Map<String, String> parameters) {
    }

    /**
     * Set the query string used to retrieve records. Currently stubbed
     */
    @Override
    public void setQuery(String query) {
    }
}
