package nl.utwente.ewi.stream.network.provenance;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeNode;
import nl.utwente.ewi.stream.network.AbstractPE;
import nl.utwente.ewi.stream.network.Configuration;
import nl.utwente.ewi.stream.network.TupeloProxy;
import nl.utwente.ewi.stream.network.provenance.datamodel.AnnotationTriple;
import nl.utwente.ewi.stream.network.provenance.datamodel.GeneratesTriple;
import nl.utwente.ewi.stream.network.provenance.datamodel.ProcessingElementTriple;
import nl.utwente.ewi.stream.network.provenance.datamodel.UsesTriple;
import nl.utwente.ewi.stream.network.provenance.datamodel.ViewTriple;
import nl.utwente.ewi.stream.utils.KeyValuePair;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.ParentMap;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphModelFacade;
import com.jgraph.layout.tree.JGraphTreeLayout;

/**
 * The Tupleo2 provenance provider (http://dlt.ncsa.illinois.edu/wiki/index.php/Tupelo_2)
 * is a wrapper to add course grained provenance information. The Tupelo2 server has to 
 * be started indipently as it is provided in the tupleo-opm subproject of this project.
 * The provider is configured with a single parameter <i>tupelo-servlet-url</i> indicating the 
 * url of the tupelo servlet.
 * @author wombachera
 *
 */
public class TupeloProvenance extends AbstractProvenanceProvider {

    public TupeloProvenance(Configuration config) {
    }

    @Override
    public boolean checkConnection() {
        int status = 0;
        try {
            URL url = new URL(TupeloProxy.endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            status = conn.getResponseCode();
        } catch (Exception e) {
            logger.severe("Connection test failed with code:" + status);
            e.printStackTrace();
        }
        return status > 199 && status < 400;
    }

    @Override
    public void sendProvenance(TripleCollection triples) {
        Collection<Triple> col = triples.getCollection();
        String pe_name = "", pe_uri = "", pe_endpoint = "";
        ArrayList<AbstractPE> parentPEs = new ArrayList<AbstractPE>();
        ArrayList<KeyValuePair> annotations = new ArrayList<KeyValuePair>();
        AbstractPE pe;
        for (Triple tr : col) {
            switch(tr.getType()) {
                case ProcessingElementTriple.TYPE:
                    pe = ((ProcessingElementTriple) tr).getSubject();
                    pe_name = pe.name;
                    pe_uri = pe.getUri();
                    pe_endpoint = ((ProcessingElementTriple) tr).getObject();
                    break;
                case ViewTriple.TYPE:
                    break;
                case AnnotationTriple.TYPE:
                    annotations.add(((AnnotationTriple) tr).getObject());
                    break;
                case UsesTriple.TYPE:
                    parentPEs.add(((UsesTriple) tr).getObject());
                    break;
                case GeneratesTriple.TYPE:
                    break;
                default:
                    logger.info("the provided triple could not be interpreted :" + tr.toString());
            }
        }
        String outputUri = pe_uri + ":view";
        TupeloProxy.newProcess(pe_name, pe_endpoint);
        TupeloProxy.newArtifact(pe_name + "_view", outputUri);
        TupeloProxy.newGeneratedBy(outputUri, pe_uri);
        if (parentPEs != null) for (AbstractPE parent : parentPEs) TupeloProxy.newUsedBy(pe_uri, parent.getUri() + ":view");
        if (annotations != null) for (KeyValuePair kv : annotations) {
            TupeloProxy.annotateProcess(pe_uri, kv.getKey(), kv.getValue());
        }
    }

    @Override
    public String getProvenanceRelations(AbstractPE node) {
        return TupeloProxy.getProvenanceGraph(node.getUri());
    }

    @Override
    public byte[] getProvenanceGraph(AbstractPE node, Map<String, String> options) {
        String graph = TupeloProxy.getProvenanceGraph(node.getUri());
        if (graph != null) {
            return drawGraph(graph, options);
        }
        return null;
    }

    @Override
    public int getType() {
        return ProvenanceManager.TUPELO;
    }

    /**
	   * Draws the graph and returns the PNG image as a byte array.
	   * @param tupeloGraph - the plan grap representation
	   * @param options - some options, see handleRequest().
	   * @return  png image as a byte array
	   */
    private static byte[] drawGraph(String tupeloGraph, Map<String, String> options) {
        Properties p = new Properties(System.getProperties());
        p.put("java.awt.headless", "true");
        System.setProperties(p);
        GraphModel model = new DefaultGraphModel();
        JGraph graph = new JGraph(model);
        GraphLayoutCache cache = graph.getGraphLayoutCache();
        Pattern ptn = Pattern.compile("\"(.+?)\"");
        Map<String, DefaultGraphCell> vertices = new HashMap<String, DefaultGraphCell>();
        List<DefaultGraphCell> edges = new ArrayList<DefaultGraphCell>();
        String[] cellz = tupeloGraph.split(";");
        for (String cell : cellz) {
            boolean isEdge = cell.contains("->");
            if (!isEdge) {
                Matcher m = ptn.matcher(cell);
                ArrayList<String> strings = new ArrayList<String>();
                while (m.find()) strings.add(m.group(1));
                String urn = strings.get(0);
                String type = strings.get(1);
                String name = strings.get(2);
                StringBuffer sb = new StringBuffer();
                if (strings.size() > 3) {
                    for (int i = 3; i < strings.size(); i++) sb.append("- <i>").append(strings.get(i)).append(" = ").append(strings.get(++i)).append("</i><br>");
                }
                vertices.put(urn, createVertex(name, type, urn, sb.toString(), 20, 20, 60, 30));
            }
        }
        Map<String, DefaultGraphCell> roots = new HashMap<String, DefaultGraphCell>();
        roots.putAll(vertices);
        for (String cell : cellz) {
            boolean isEdge = cell.contains("->");
            if (isEdge) {
                Matcher m = ptn.matcher(cell);
                ArrayList<String> strings = new ArrayList<String>();
                while (m.find()) strings.add(m.group(1));
                String sourceUrn = strings.get(0);
                String targetUrn = strings.get(1);
                String label = strings.get(2);
                edges.add(createEdge(label, vertices.get(sourceUrn).getChildAt(0), vertices.get(targetUrn).getChildAt(0)));
                roots.remove(targetUrn);
            }
        }
        List<DefaultGraphCell> verticesAndEdges = new ArrayList<DefaultGraphCell>();
        verticesAndEdges.addAll(vertices.values());
        verticesAndEdges.addAll(edges);
        insert(model, verticesAndEdges.toArray());
        JGraphFacade facade = new JGraphModelFacade(model, roots.values().toArray());
        facade.setDirected(true);
        JGraphTreeLayout layout = new JGraphTreeLayout();
        String orientation = options == null ? null : options.get("orientation");
        if (orientation == null) layout.setOrientation(SwingConstants.WEST); else layout.setOrientation(valueOfOrientation(orientation));
        layout.setLevelDistance(150);
        layout.run(facade);
        Map nested = facade.createNestedMap(true, true);
        cache.edit(nested);
        JPanel panel = new JPanel();
        panel.setDoubleBuffered(false);
        panel.add(graph);
        panel.setVisible(true);
        panel.setEnabled(true);
        panel.addNotify();
        panel.validate();
        BufferedImage img = graph.getImage(graph.getBackground(), 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException ex) {
            return "Failed to write provenance graph image. Please try again.".getBytes();
        }
        return out.toByteArray();
    }

    /**
	   * Creates a vertex. Note that the dimensions nor the location of the vertex is essential, since JGraphLayout will
	   * automatically layout the graph.
	   *
	   * @param name - the name of the vertex
	   * @param type - the type of the vertex, which is added behind the name (e.g. 'Artifact', 'Process')
	   * @param uri - the URI of the vertex
	   * @param annotations - A string containing any annotations belonging to this vertex
	   * @param x - x coordinate
	   * @param y - y coordinate
	   * @param w - vertex width
	   * @param h - vertex height
	   * @return a graph cell
	   */
    public static DefaultGraphCell createVertex(String name, String type, String uri, String annotations, double x, double y, double w, double h) {
        StringBuffer label = new StringBuffer("<html>");
        label.append("<b>").append(name).append("</b>");
        label.append(" <i>(").append(type).append(")</i>");
        label.append("<br>").append(uri);
        if (!annotations.trim().equals("")) label.append("<br>").append(annotations);
        label.append("</html>");
        DefaultGraphCell cell = new DefaultGraphCell(label.toString());
        AttributeMap map = cell.getAttributes();
        GraphConstants.setBounds(map, new Rectangle2D.Double(x, y, w, h));
        GraphConstants.setInset(map, 5);
        GraphConstants.setAutoSize(map, true);
        if (type.equals("process")) GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black, 2)); else GraphConstants.setBorder(map, BorderFactory.createLineBorder(Color.black));
        cell.addPort();
        return cell;
    }

    /**
	   * Draws an edge
	   * @param label - the label of the edge
	   * @param source - the source vertex
	   * @param target - the target vertex
	   * @return - a DefaultEdge instance
	   */
    public static DefaultEdge createEdge(String label, TreeNode source, TreeNode target) {
        DefaultEdge edge = new DefaultEdge();
        edge.setSource(source);
        edge.setTarget(target);
        AttributeMap map = edge.getAttributes();
        GraphConstants.setLineStyle(map, GraphConstants.STYLE_SPLINE);
        GraphConstants.setLineEnd(map, GraphConstants.ARROW_TECHNICAL);
        GraphConstants.setEndFill(map, true);
        GraphConstants.setLabelAlongEdge(map, true);
        GraphConstants.setExtraLabelPositions(map, new Point2D[] { new Point2D.Double(GraphConstants.PERMILLE / 2, -5) });
        GraphConstants.setExtraLabels(map, new Object[] { label });
        return edge;
    }

    /**
	   * Inserts the specified cells into the graph model. This method is a
	   * general implementation of cell insertion. If the source and target port
	   * are null, then no connection set is created. The method uses the
	   * attributes from the specified edge and the egdge's children to construct
	   * the insert call. This example shows how to insert an edge with a special
	   * arrow between two known vertices:
	   *
	   * <pre>
	   * Object source = graph.getDefaultPortForCell(sourceVertex).getCell();
	   * Object target = graph.getDefaultPortForCell(targetVertex).getCell();
	   * DefaultEdge edge = new DefaultEdge(&quot;Hello, world!&quot;);
	   * edge.setSource(source);
	   * edge.setTarget(target);
	   * Map attrs = edge.getAttributes();
	   * GraphConstants.setLineEnd(attrs, GraphConstants.ARROW_TECHNICAL);
	   * graph.getGraphLayoutCache().insert(edge);
	   * </pre>
	   */
    public static void insert(GraphModel model, Object[] cells) {
        insert(model, cells, new Hashtable(), new ConnectionSet(), new ParentMap());
    }

    /**
	   * Variant of the insert method that allows to pass a default connection set
	   * and parent map and nested map.
	   */
    public static void insert(GraphModel model, Object[] cells, Map nested, ConnectionSet cs, ParentMap pm) {
        if (cells != null) {
            if (nested == null) {
                nested = new Hashtable();
            }
            if (cs == null) {
                cs = new ConnectionSet();
            }
            if (pm == null) {
                pm = new ParentMap();
            }
            for (int i = 0; i < cells.length; i++) {
                int childCount = model.getChildCount(cells[i]);
                for (int j = 0; j < childCount; j++) {
                    Object child = model.getChild(cells[i], j);
                    pm.addEntry(child, cells[i]);
                    AttributeMap attrs = model.getAttributes(child);
                    if (attrs != null) {
                        nested.put(child, attrs);
                    }
                }
                Map attrsTmp = (Map) nested.get(cells[i]);
                Map attrs = model.getAttributes(cells[i]);
                if (attrsTmp != null) {
                    attrs.putAll(attrsTmp);
                }
                nested.put(cells[i], attrs);
                Object sourcePort = model.getSource(cells[i]);
                if (sourcePort != null) {
                    cs.connect(cells[i], sourcePort, true);
                }
                Object targetPort = model.getTarget(cells[i]);
                if (targetPort != null) {
                    cs.connect(cells[i], targetPort, false);
                }
            }
            cells = DefaultGraphModel.getDescendants(model, cells).toArray();
            model.insert(cells, nested, cs, pm, null);
        }
    }

    /**
	   * Returns the SwingConstant for a given orientation string.
	   * @param orientation - the orientation: north, south, east, west
	   * @return the SwingConstant, default is SwingConstants.WEST
	   */
    public static int valueOfOrientation(String orientation) {
        String value = orientation.trim().toUpperCase();
        if (value.equals("NORTH")) return SwingConstants.NORTH; else if (value.equals("EAST")) return SwingConstants.EAST; else if (value.equals("SOUTH")) return SwingConstants.SOUTH; else return SwingConstants.WEST;
    }
}
