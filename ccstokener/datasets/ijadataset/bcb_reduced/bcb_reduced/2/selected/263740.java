package org.mitre.mrald.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import org.mitre.mrald.util.Config;
import org.mitre.mrald.util.DBMetaData;
import org.mitre.mrald.util.Link;
import org.mitre.mrald.util.MetaData;
import org.mitre.mrald.util.MraldException;
import org.mitre.mrald.util.TableMetaData;
import org.mitre.mrald.util.TypeUtils;
import edu.berkeley.guir.prefuse.graph.DefaultEdge;
import edu.berkeley.guir.prefuse.graph.DefaultGraph;
import edu.berkeley.guir.prefuse.graph.DefaultNode;
import edu.berkeley.guir.prefuse.graph.DefaultTreeNode;
import edu.berkeley.guir.prefuse.graph.Edge;
import edu.berkeley.guir.prefuse.graph.Graph;
import edu.berkeley.guir.prefuse.graph.Node;
import edu.berkeley.guir.prefuse.graph.io.AbstractGraphReader;
import edu.berkeley.guir.prefuse.graph.io.GraphReader;
import edu.berkeley.guir.prefuse.graph.io.XMLGraphReader;

/**
 *  <p>
  *  This class supports setting the node type to use when building the graph from the Database.
 *  For example, one should set the node type to DefaultTreeNode.class if one
 *  wants to impose tree structures on the input graph.</p>
 *
 *@author     ghamilton
 *@created    March 30, 2005
 *@version    1.0
 */
public class DBGraphReader extends AbstractGraphReader implements GraphReader {

    /**
	 *  Description of the Field
	 */
    protected Class NODE_TYPE = DefaultNode.class;

    private String configFile = "";

    /**
	 *@param  is               Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 *@see                     edu.berkeley.guir.prefuse.graph.io.GraphReader#loadGraph(java.io.InputStream)
	 */
    public Graph loadGraph(InputStream is) throws IOException {
        return loadGraph("temp");
    }

    /**
	 *@param  dbName           Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 *@see                     edu.berkeley.guir.prefuse.graph.io.GraphReader#loadGraph(java.io.InputStream)
	 */
    public Graph loadGraph(String fileName) throws IOException {
        try {
            System.out.print("DBGraphReader: loadGraph: configFile: " + configFile);
            System.out.print("DBGraphReader: loadGraph: fileName: " + fileName);
            DBGraphHandler handler = new DBGraphHandler();
            return handler.getGraph(fileName);
        } catch (MraldException se) {
            se.printStackTrace();
        }
        return null;
    }

    /**
	 *@param  dbName           Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 *@see                     edu.berkeley.guir.prefuse.graph.io.GraphReader#loadGraph(java.io.InputStream)
	 */
    public Graph loadGraph(URL urlFileName) throws IOException {
        try {
            System.out.print("DBGraphReader: loading Graph from URL");
            DBGraphHandler handler = new DBGraphHandler();
            return handler.getGraph(urlFileName);
        } catch (MraldException se) {
            se.printStackTrace();
        }
        return null;
    }

    /**
	 *@param  dbName           Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 *@see                     edu.berkeley.guir.prefuse.graph.io.GraphReader#loadGraph(java.io.InputStream)
	 */
    public Graph loadGraph() throws IOException {
        try {
            System.out.print("DBGraphReader: loadGraph: configFile: " + configFile);
            System.out.print("DBGraphReader: loadGraph: no fileName ");
            DBGraphHandler handler = new DBGraphHandler();
            handler.init();
            return handler.getGraph();
        } catch (MraldException se) {
            se.printStackTrace();
        }
        return null;
    }

    /**
	 *  Return the class type used for representing nodes.
	 *
	 *@return    the Class of new DefaultNode instances.
	 */
    public Class getNodeType() {
        return NODE_TYPE;
    }

    /**
	 *  Set the type to be used for node instances. All created nodes will be
	 *  instances of the input type. The input class should be a descendant class
	 *  of edu.berkeley.guir.prefuse.graph.DefaultNode.
	 *
	 *@param  type  the class type to use for node instances.
	 */
    public void setNodeType(Class type) {
        NODE_TYPE = type;
    }

    /**
	 *  Helper class that performs XML parsing of graph files using SAX (the Simple
	 *  API for XML).
	 *
	 *@author     gail
	 *@created    March 30, 2005
	 */
    public class DBGraphHandler {

        /**
		 *  Description of the Field
		 */
        public static final String NODE = "node";

        /**
		 *  Description of the Field
		 */
        public static final String EDGE = "edge";

        /**
		 *  Description of the Field
		 */
        public static final String ATT = "att";

        /**
		 *  Description of the Field
		 */
        public static final String TABLEID = "id";

        /**
		 *  Description of the Field
		 */
        public static final String LABEL = "label";

        /**
		 *  Description of the Field
		 */
        public static final String TYPE = "type";

        /**
		 *  Description of the Field
		 */
        public static final String NAME = "name";

        /**
		 *  Description of the Field
		 */
        public static final String VALUE = "value";

        /**
		 *  Description of the Field
		 */
        public static final String LIST = "list";

        /**
		 *  Description of the Field
		 */
        public static final String WEIGHT = "weight";

        /**
		 *  Description of the Field
		 */
        public static final String GRAPH = "graph";

        /**
		 *  Description of the Field
		 */
        public static final String DIRECTED = "directed";

        public static final String COLUMN = "column";

        private Graph m_graph = null;

        private HashMap<String, Node> m_nodeMap = new HashMap<String, Node>();

        private boolean m_directed = false;

        /**
		 *  Description of the Method
		 */
        public void init() throws MraldException {
            System.out.print("DBGraphReader: init: configFile: " + configFile);
            int idx = -1;
            m_directed = (idx >= 0);
            m_graph = new DefaultGraph(m_directed);
            loadDB();
            m_nodeMap.clear();
        }

        private void loadDB() throws MraldException {
            System.out.print("DBGraphReader: loadDB: configFile: " + configFile);
            String config_dir = configFile;
            if (Config.getFinalPropertiesLocation() == null) Config.init(config_dir);
            MetaData.reload();
        }

        /**
		 *  Gets the dBMetaData attribute of the DBGraphHandler object
		 *
		 *@return    The dBMetaData value
		 */
        public DBMetaData getDBMetaData() {
            return MetaData.getDbMetaData("main");
        }

        /**
		 *  Gets the graph attribute of the DBGraphHandler object
		 *
		 *@return    The graph value
		 */
        public Graph getGraph() throws MraldException {
            System.out.print("DBGraphReader: gettingGraph using DB ");
            DBMetaData dbmd = getDBMetaData();
            getNodes(dbmd);
            getEdges(dbmd);
            return m_graph;
        }

        /**
		 *  Gets the graph attribute of the DBGraphHandler object
		 *
		 *@param  dbmd  Description of the Parameter
		 *@return       The graph value
		 */
        public Graph getGraph(DBMetaData dbmd) throws MraldException {
            System.out.print("DBGraphReader: gettingGraph using DB ");
            getNodes(dbmd);
            getEdges(dbmd);
            return m_graph;
        }

        /**
		 *  Gets the graph attribute of the DBGraphHandler object
		 *
		 *@param  dbmd  Description of the Parameter
		 *@return       The graph value
		 */
        public Graph getGraph(URL urlFilename) throws MraldException {
            try {
                System.out.print("DBGraphReader: gettingGraph using url");
                InputStream inUrl = urlFilename.openStream();
                XMLGraphReader gr = new XMLGraphReader();
                gr.setNodeType(DefaultTreeNode.class);
                Graph graph = gr.loadGraph(inUrl);
                return graph;
            } catch (java.io.FileNotFoundException e) {
                throw new MraldException(e.getMessage());
            } catch (java.io.IOException e) {
                throw new MraldException(e.getMessage());
            }
        }

        /**
		 *  Gets the graph attribute of the DBGraphHandler object
		 *
		 *@param  dbmd  Description of the Parameter
		 *@return       The graph value
		 */
        public Graph getGraph(String filename) throws MraldException {
            try {
                System.out.print("DBGraphReader: gettingGraph using file " + filename);
                XMLGraphReader gr = new XMLGraphReader();
                gr.setNodeType(DefaultTreeNode.class);
                Graph graph = gr.loadGraph(filename);
                return graph;
            } catch (java.io.FileNotFoundException e) {
                throw new MraldException(e.getMessage());
            } catch (java.io.IOException e) {
                throw new MraldException(e.getMessage());
            }
        }

        /**
		 *  Gets the nodes attribute of the DBGraphHandler object
		 *
		 *@param  dbmd                Description of the Parameter
		 *@exception  MraldException  Description of the Exception
		 */
        public void getNodes(DBMetaData dbmd) throws MraldException {
            try {
                Collection tables = dbmd.getAllTableMetaData();
                Iterator tableIter = tables.iterator();
                HashMap keys = DBMetaData.setLinks(dbmd);
                while (tableIter.hasNext()) {
                    int fkeyCount = 0;
                    TableMetaData table = (TableMetaData) tableIter.next();
                    Node n = (Node) NODE_TYPE.newInstance();
                    n.setAttribute(TABLEID, table.getName());
                    m_nodeMap.put(table.getName(), n);
                    Collection columns = table.getColumnNames();
                    Iterator colIter = columns.iterator();
                    int i = 0;
                    while (colIter.hasNext()) {
                        i++;
                        String label = COLUMN + i;
                        String label2 = "type" + i;
                        String fieldName = colIter.next().toString();
                        n.setAttribute(label, fieldName);
                        n.setAttribute(label2, TypeUtils.getSqlType(table.getFieldType(fieldName).intValue()));
                        boolean isPk = table.isPrimaryKey(fieldName);
                        if (isPk) n.setAttribute("pkey", fieldName);
                        boolean isFk = DBMetaData.isFkey(table.getName(), fieldName, keys);
                        if (isFk) {
                            fkeyCount++;
                            n.setAttribute("fkey" + fkeyCount, fieldName);
                        }
                    }
                    m_graph.addNode(n);
                    n.setAttribute(LABEL, table.getName());
                    n.setAttribute(WEIGHT, "0");
                }
            } catch (InstantiationException e) {
                throw new MraldException(e);
            } catch (IllegalAccessException e) {
                throw new MraldException(e);
            }
        }

        /**
		 *  Gets the edges attribute of the DBGraphHandler object
		 *
		 *@param  dbmd  Description of the Parameter
		 */
        public void getEdges(DBMetaData dbmd) {
            Collection links = dbmd.getLinkList();
            Iterator linkIter = links.iterator();
            while (linkIter.hasNext()) {
                Link link = (Link) linkIter.next();
                String source = link.getPtable();
                String target = link.getFtable();
                Node s = m_nodeMap.get(source);
                Node t = m_nodeMap.get(target);
                Edge e = new DefaultEdge(s, t, m_directed);
                e.setAttribute(LABEL, link.getPcolumn());
                m_graph.addEdge(e);
            }
        }
    }
}
