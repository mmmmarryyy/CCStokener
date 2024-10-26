package prajna.semantic;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import prajna.util.Graph;
import prajna.util.Grid;
import prajna.util.Tree;

/**
 * Class which interacts with server-side DataAccessors. The DataAccessorClient
 * connects to a {@link prajna.servlet.SemanticAccessorServlet}, and retrieves
 * the various data through servlet invocations. The data received from the
 * servlet is transmitted as serialized objects to enhance performance.
 * 
 * @author <a href="http://www.ganae.com/edswing">Edward Swing</a>
 * @param <N> The node class for this DataAccessor. Must extend Serializable.
 * @param <E> The edge class for this DataAccessor. Must extend Serializable.
 */
public class DataAccessorClient<N extends Serializable, E extends Serializable> implements DataAccessor<N, E> {

    private static DocumentBuilder builder;

    private URL docBase;

    private Map<String, DataStructureType> typeMap = new HashMap<String, DataStructureType>();

    static {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Creates a new DataAccessorClient object. This constructor needs a
     * document base so that it will know where to communicate with the server.
     * 
     * @param documentBase The document base
     */
    public DataAccessorClient(URL documentBase) {
        setDocBase(documentBase);
    }

    /**
     * Extend the given graph around the specified node. This method uses the
     * basic graph retrieval, but adds the node's name as an additional
     * argument attached to the graph name
     * 
     * @param graphName The name of the graph
     * @param graph The graph to extend
     * @param node The node to use as an extension point.
     * @return true if more data was added to the graph, false otherwise.
     */
    public boolean extendGraph(String graphName, Graph<N, E> graph, N node) {
        Graph<N, E> subGraph = getGraph(graphName + "&node=" + node.toString());
        int nodeSize = graph.order();
        int edgeSize = graph.size();
        graph.addAll(subGraph);
        return (nodeSize != graph.order()) && (edgeSize != graph.size());
    }

    /**
     * Extend the given tree around the specified node. This method adds any
     * children which are not already part of the tree. This method uses the
     * basic tree retrieval, but adds the node's name as an additional argument
     * attached to the tree name
     * 
     * @param treeName The name of the graph
     * @param tree The graph to extend
     * @param node The node to use as an extension point.
     * @return true if more data was added to the tree, false otherwise.
     */
    public boolean extendTree(String treeName, Tree<N> tree, N node) {
        Tree<N> newRoot = getTree(treeName + "&node=" + node.toString());
        int kidCount = tree.getChildCount();
        for (Tree<N> kid : newRoot.getChildTrees()) {
            tree.add(kid.getData(), node);
        }
        return (kidCount != tree.getChildCount());
    }

    /**
     * Retrieve the dataset from the underlying data source. This method
     * queries the servlet to retrieve the named dataset. The dataset is
     * retrieved from the servlet in a serialized form.
     * 
     * @param datasetName the name of the dataset
     * @return the dataset
     */
    @SuppressWarnings("unchecked")
    public Set<N> getDataset(String datasetName) {
        Set<N> dataSet = null;
        try {
            URL getUrl = new URL(docBase, "?serial=true&name=" + datasetName);
            ObjectInputStream inStream = new ObjectInputStream(getUrl.openStream());
            dataSet = (Set<N>) inStream.readObject();
            inStream.close();
        } catch (Exception exc) {
            throw new RuntimeException("Error retrieving dataset at " + docBase + "?serial=true&name=" + datasetName, exc);
        }
        return dataSet;
    }

    /**
     * Get the names of the available datasets from this accessor. This method
     * issues a servlet call to retrieve the dataset names. If there is a
     * problem retrieving data, an empty set is returned.
     * 
     * @return the set of dataset names
     */
    public Set<String> getDatasetNames() {
        Set<String> list = null;
        try {
            URL getUrl = new URL(docBase, "?query=dataset");
            list = parseXmlList(getUrl, "Dataset", DataStructureType.DATASET);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot retrieve dataset names from " + docBase, exc);
        }
        return list;
    }

    /**
     * Retrieve the graph from the underlying data source. This method queries
     * the servlet to retrieve the named graph. The graph is retrieved from the
     * servlet in a serialized form.
     * 
     * @param graphName the name of the graph
     * @return the graph
     */
    @SuppressWarnings("unchecked")
    public Graph<N, E> getGraph(String graphName) {
        Graph<N, E> graph = null;
        try {
            URL getUrl = new URL(docBase, "?serial=true&name=" + graphName);
            ObjectInputStream inStream = new ObjectInputStream(getUrl.openStream());
            graph = (Graph<N, E>) inStream.readObject();
            inStream.close();
        } catch (Exception exc) {
            throw new RuntimeException("Error retrieving graph at " + docBase + "?serial=true&name=" + graphName, exc);
        }
        return graph;
    }

    /**
     * Get the names of the available graphs from this accessor. This method
     * issues a servlet call to retrieve the graph names. If there is a problem
     * retrieving data, an empty set is returned.
     * 
     * @return the set of graph names
     */
    public Set<String> getGraphNames() {
        Set<String> list = null;
        try {
            URL getUrl = new URL(docBase, "?query=graph");
            list = parseXmlList(getUrl, "Graph", DataStructureType.GRAPH);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot retrieve graph names from " + docBase, exc);
        }
        return list;
    }

    /**
     * Retrieve the grid from the underlying data source. This method queries
     * the servlet to retrieve the named grid. The grid is retrieved from the
     * servlet in a serialized form.
     * 
     * @param gridName the name of the grid
     * @return the grid
     */
    @SuppressWarnings("unchecked")
    public Grid<N> getGrid(String gridName) {
        Grid<N> grid = null;
        try {
            URL getUrl = new URL(docBase, "?serial=true&name=" + gridName);
            ObjectInputStream inStream = new ObjectInputStream(getUrl.openStream());
            grid = (Grid<N>) inStream.readObject();
            inStream.close();
        } catch (Exception exc) {
            throw new RuntimeException("Error retrieving grid at " + docBase + "?serial=true&name=" + gridName, exc);
        }
        return grid;
    }

    /**
     * Get the names of the available grids from this accessor.This method
     * issues a servlet call to retrieve the grid names. If there is a problem
     * retrieving data, an empty set is returned.
     * 
     * @return the set of grid names
     */
    public Set<String> getGridNames() {
        Set<String> list = null;
        try {
            URL getUrl = new URL(docBase, "?query=grid");
            list = parseXmlList(getUrl, "Grid", DataStructureType.GRID);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot retrieve grid names from " + docBase, exc);
        }
        return list;
    }

    /**
     * Get the data type for the specified dataName.
     * 
     * @param dataName the name of the data
     * @return the data type
     */
    public DataStructureType getStructureType(String dataName) {
        return typeMap.get(dataName);
    }

    /**
     * Retrieve the tree from the underlying data source. This method queries
     * the servlet to retrieve the named tree. The tree is retrieved from the
     * servlet in a serialized form.
     * 
     * @param treeName the name of the tree
     * @return the tree
     */
    @SuppressWarnings("unchecked")
    public Tree<N> getTree(String treeName) {
        Tree<N> root = null;
        try {
            URL getUrl = new URL(docBase, "?serial=true&name=" + treeName);
            ObjectInputStream inStream = new ObjectInputStream(getUrl.openStream());
            root = (Tree<N>) inStream.readObject();
            inStream.close();
        } catch (Exception exc) {
            throw new RuntimeException("Error retrieving tree at " + docBase + "?serial=true&name=" + treeName, exc);
        }
        return root;
    }

    /**
     * Get the names of the available trees from this accessor. This method
     * issues a servlet call to retrieve the tree names. If there is a problem
     * retrieving data, an empty set is returned.
     * 
     * @return the set of tree names
     */
    public Set<String> getTreeNames() {
        Set<String> list = new TreeSet<String>();
        try {
            URL getUrl = new URL(docBase, "?query=tree");
            list = parseXmlList(getUrl, "Tree", DataStructureType.TREE);
        } catch (Exception exc) {
            throw new RuntimeException("Cannot retrieve tree names from " + docBase, exc);
        }
        return list;
    }

    /**
     * Reads the list of data names from a servlet response. The response from
     * the servlet should be a skeleton of an accessor configuration file,
     * which specifies the data structures matching the desired type.
     * 
     * @param url the Servlet URL
     * @param tagName the tag name for the data elements to be extracted
     * @param type The data type
     * @return
     * @throws SAXException if there is a problem parsing the XML
     * @throws IOException if there is a problem accessing the data from the
     *             servlet
     */
    private Set<String> parseXmlList(URL url, String tagName, DataStructureType type) throws SAXException, IOException {
        InputStream inStream = url.openStream();
        Document doc = builder.parse(inStream);
        Element root = doc.getDocumentElement();
        TreeSet<String> strSet = new TreeSet<String>();
        NodeList nodeList = root.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            String name = ((Element) nodeList.item(i)).getAttribute("name");
            strSet.add(name);
            typeMap.put(name, type);
        }
        inStream.close();
        return strSet;
    }

    /**
     * Set the configuration information from an input stream. The input stream
     * should contain any required initialization parameters. This method
     * should initialize the data accessor if necessary. The format of the
     * stream is implementation-dependent.
     * 
     * @param inStream Input stream which is the source of the configuration
     *            information
     */
    public void setConfig(InputStream inStream) {
    }

    /**
     * Set the document base for this DataAccessorClient.
     * 
     * @param documentBase The document base
     */
    public void setDocBase(URL documentBase) {
        if (docBase == null) {
            throw new IllegalArgumentException("DocBase cannot be null");
        }
        this.docBase = documentBase;
        getDatasetNames();
        getGraphNames();
        getGridNames();
        getTreeNames();
    }

    /**
     * Set any initialization parameters required by this data accessor. The
     * DataAccessorClient looks for the <code>docBase</code> parameter, and
     * initializes itself to the specified
     * 
     * @param parameters a map of initialization parameters
     */
    public void setInitParameters(Map<String, String> parameters) {
        String docBaseStr = parameters.get("docBase");
        if (docBaseStr != null && docBase == null) {
            throw new IllegalArgumentException("Cannot determine docBase");
        }
        try {
            setDocBase(new URL(docBaseStr));
        } catch (MalformedURLException exc) {
            throw new IllegalArgumentException("Invalid docBase: " + docBaseStr);
        }
    }
}
