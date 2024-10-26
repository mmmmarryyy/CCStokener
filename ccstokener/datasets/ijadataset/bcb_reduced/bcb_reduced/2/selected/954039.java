package hypergraph.applications.hexplorer;

import hypergraph.graphApi.Graph;
import hypergraph.graphApi.GraphSystem;
import hypergraph.graphApi.GraphSystemFactory;
import hypergraph.graphApi.Node;
import hypergraph.graphApi.algorithms.GraphUtilities;
import hypergraph.graphApi.io.GraphWriter;
import hypergraph.graphApi.io.GraphXMLWriter;
import hypergraph.graphApi.io.SAXReader;
import hypergraph.visualnet.ArrowLineRenderer;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import org.xml.sax.SAXException;

/**
 * @author Jens Kanschik
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class HExplorerApplet extends JApplet {

    /** Stores the graph that the applet shows. */
    private GraphPanel graphPanel;

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    /**@inheritDoc */
    public void init() {
        String file = getParameter("file");
        GraphSystem graphSystem = null;
        try {
            graphSystem = GraphSystemFactory.createGraphSystem("hypergraph.graph.GraphSystemImpl", null);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(8);
        }
        Graph graph = null;
        URL url = null;
        try {
            url = new URL(getCodeBase(), file);
            SAXReader reader = new SAXReader(graphSystem, url);
            ContentHandlerFactory ch = new ContentHandlerFactory();
            ch.setBaseUrl(getCodeBase());
            reader.setContentHandlerFactory(ch);
            graph = reader.parse();
        } catch (FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(null, "Could not find file " + url.getFile() + ". \n" + "Start applet with default graph", "File not found", JOptionPane.ERROR_MESSAGE);
            System.out.println("Exception : " + fnfe);
            fnfe.printStackTrace(System.out);
        } catch (SAXException saxe) {
            JOptionPane.showMessageDialog(null, "Error while parsing file" + url.getFile() + ". \n" + "Exception : " + saxe + ". \n" + "Start applet with default graph", "Parsing error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Exception : " + saxe);
            saxe.getException().printStackTrace();
            saxe.printStackTrace(System.out);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "General error while reading file " + url + ". \n" + "Exception : " + e + ". \n" + "Start applet with default graph", "General error", JOptionPane.ERROR_MESSAGE);
            System.out.println(url);
            System.out.println("Exception : " + e);
            e.printStackTrace(System.out);
        }
        if (graph == null) {
            graph = GraphUtilities.createTree(graphSystem, 2, 3);
        }
        graphPanel = new GraphPanel(graph, this);
        file = getParameter("properties");
        if (file != null) try {
            url = new URL(getCodeBase(), file);
            graphPanel.loadProperties(url.openStream());
        } catch (FileNotFoundException fnfe) {
            JOptionPane.showMessageDialog(null, "Could not find propertyfile " + url.getFile() + ". \n" + "Start applet with default properties", "File not found", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "General error while reading file " + url.getFile() + ". \n" + "Exception : " + e + ". \n" + "Start applet with default properties", "General error", JOptionPane.ERROR_MESSAGE);
            System.out.println(url);
            System.out.println("Exception : " + e);
            e.printStackTrace(System.out);
        }
        String center = getParameter("center");
        if (center != null) {
            Node n = (Node) graph.getElement(center);
            if (n != null) graphPanel.centerNode(n);
        }
        graphPanel.setLineRenderer(new ArrowLineRenderer());
        getContentPane().add(graphPanel);
    }

    public String getGraphXML() {
        try {
            OutputStream os = new ByteArrayOutputStream();
            GraphWriter graphWriter = new GraphXMLWriter(new OutputStreamWriter(os));
            graphWriter.write(getGraphPanel().getGraph());
            return os.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return ioe.toString();
        }
    }
}
