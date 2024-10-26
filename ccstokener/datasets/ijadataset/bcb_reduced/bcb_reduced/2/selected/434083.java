package org.kablink.teaming.applets.workflowviewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JApplet;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import samples.graph.ShortestPathDemo.MyEdgePaintFunction;
import samples.graph.ShortestPathDemo.MyEdgeStrokeFunction;
import samples.graph.ShortestPathDemo.MyVertexPaintFunction;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.AbstractEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.transform.Transformer;

public class WorkflowViewer extends JApplet implements ActionListener {

    /**
     * the graph
     */
    private SparseGraph g;

    private Map vertexName = new HashMap();

    private Map vertexCaption = new HashMap();

    private Map nameVertex = new HashMap();

    private Map appletData = new HashMap();

    private Document workflowDoc;

    private Layout layout;

    private JTextArea popup;

    protected PluggableRenderer pr;

    protected VertexStringer vs;

    protected VertexStringer vs_none;

    protected EdgeStringer es;

    protected EdgeStringer es_none;

    protected VisualizationViewer vv;

    protected PopupGraphMouse gm;

    protected Transformer affineTransformer;

    private static final Object COLORKEY = "COLORKEY";

    private static final Object THICKNESSKEY = "THICKNESSKEY";

    public void init() {
    }

    public void start() {
        getContentPane().add(startFunction());
    }

    public WorkflowViewer() {
    }

    public static void main(String[] s) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel jp = new WorkflowViewer().startFunction();
        jf.getContentPane().add(jp);
        jf.pack();
        jf.show();
    }

    public JPanel startFunction() {
        setupAppletData();
        g = new SparseGraph();
        readWorkflowData();
        Vertex[] v = createVertices();
        createEdges(v);
        pr = new PluggableRenderer();
        layout = new FRLayout(g);
        vv = new VisualizationViewer(layout, pr);
        pr.setVertexPaintFunction(new WorkflowVertexPaintFunction());
        pr.setEdgePaintFunction(new WorkflowEdgePaintFunction());
        pr.setEdgeStrokeFunction(new WorkflowEdgeStrokeFunction());
        pr.setVertexStringer(new VertexNodeNameStringer(v));
        JPanel jp = new JPanel();
        jp.setLayout(new BorderLayout());
        addTopControls(jp);
        vv.setBackground(Color.white);
        GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(vv);
        jp.add(scrollPane);
        gm = new PopupGraphMouse();
        vv.setGraphMouse(gm);
        gm.setMode(Mode.PICKING);
        double dX = 100;
        double dY = 50;
        if (v != null) {
            for (int i = 0; i < v.length; i++) {
                String stateName = (String) vertexName.get(v[i]);
                Element stateNameProperty = (Element) workflowDoc.getRootElement().selectSingleNode("//item[@name='state']/properties/property[@name='name' and @value='" + stateName + "']");
                if (stateNameProperty != null) {
                    Element state = stateNameProperty.getParent().getParent();
                    String sX = state.attributeValue("x", String.valueOf(dX));
                    String sY = state.attributeValue("y", "");
                    if (sY.equals("")) {
                        sY = String.valueOf(dY);
                        dY += 50;
                    }
                    layout.forceMove(v[i], (double) Double.parseDouble(sX), (double) Double.parseDouble(sY));
                } else {
                    layout.forceMove(v[i], dX, dY);
                    dY += 50;
                }
            }
        }
        return jp;
    }

    /**
     * @param jp    panel to which controls will be added
     */
    protected void addTopControls(final JPanel jp) {
        final JPanel control_panel = new JPanel();
        jp.add(control_panel, BorderLayout.NORTH);
        control_panel.setLayout(new BorderLayout());
        final Box vertex_panel = Box.createVerticalBox();
        vertex_panel.setBorder(BorderFactory.createTitledBorder("Vertices"));
        final Box edge_panel = Box.createVerticalBox();
        edge_panel.setBorder(BorderFactory.createTitledBorder("Edges"));
        final Box both_panel = Box.createVerticalBox();
        control_panel.add(both_panel, BorderLayout.CENTER);
        if (!appletData.get("nltSaveLayout").equals("")) {
            JButton saveChanges = new JButton((String) appletData.get("nltSaveLayout"));
            saveChanges.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateWorkflowStateLayout();
                    Map postData = new HashMap();
                    URL postUrl;
                    try {
                        postUrl = new URL((String) appletData.get("xmlPostUrl"));
                        if (postUrl == null || postUrl.equals("")) return;
                    } catch (MalformedURLException em) {
                        return;
                    }
                    postData.put("saveLayout", "saveLayout");
                    postData.put("xmlData", workflowDoc.asXML());
                    uploadToUrl(postUrl, postData);
                }
            });
            both_panel.add(saveChanges);
        }
    }

    public void actionPerformed(ActionEvent e) {
        AbstractButton source = (AbstractButton) e.getSource();
        if (false) {
        }
        vv.repaint();
    }

    /**
     * a subclass of DefaultModalGraphMouse that offers popup
     * menu support
     */
    protected class PopupGraphMouse extends DefaultModalGraphMouse {

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
            } else {
                super.mousePressed(e);
            }
        }

        /**
         * if this is the popup trigger, process here, otherwise
         * defer to the superclass
         */
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                handlePopup(e);
            } else {
                super.mouseReleased(e);
            }
        }

        /**
         * If this event is over a Vertex, pop up a menu to
         * allow the user to increase/decrease the voltage
         * attribute of this Vertex
         * @param e
         */
        private void handlePopup(MouseEvent e) {
            Point2D p = vv.inverseViewTransform(e.getPoint());
            PickSupport pickSupport = vv.getPickSupport();
            if (pickSupport != null) {
                final Vertex v = pickSupport.getVertex(p.getX(), p.getY());
                if (v != null) {
                }
            }
        }
    }

    public class WorkflowEdgePaintFunction extends AbstractEdgePaintFunction {

        /**
		 * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getDrawPaint(edu.uci.ics.jung.graph.Edge)
		 */
        public Paint getDrawPaint(Edge e) {
            Color k = (Color) e.getUserDatum(COLORKEY);
            if (k != null) return k;
            return Color.BLACK;
        }
    }

    public class WorkflowEdgeStrokeFunction implements EdgeStrokeFunction {

        protected final Stroke THIN = new BasicStroke(1);

        protected final Stroke THICK = new BasicStroke(1);

        protected final Stroke DOTTED = PluggableRenderer.DOTTED;

        public Stroke getStroke(Edge e) {
            Stroke s = (Stroke) e.getUserDatum(THICKNESSKEY);
            if (s != null) return s;
            return THICK;
        }
    }

    public class WorkflowVertexPaintFunction implements VertexPaintFunction {

        public Paint getDrawPaint(Vertex v) {
            return Color.black;
        }

        public Paint getFillPaint(Vertex v) {
            Color k = (Color) v.getUserDatum(COLORKEY);
            if (k != null) return k;
            return Color.BLUE;
        }
    }

    /**
     * create some vertices
     * @return the Vertices in an array
     */
    private Vertex[] createVertices() {
        Element workflowRoot = workflowDoc.getRootElement();
        Element workflowProcess = (Element) workflowRoot.selectSingleNode("//item[@name='workflowProcess']");
        List states = workflowRoot.selectNodes("//item[@name='state']");
        if (workflowProcess == null || states == null) return null;
        int nodeCount = states.size();
        System.out.println("node count is: " + String.valueOf(nodeCount).toString());
        Vertex[] v = new Vertex[nodeCount];
        Iterator itStates = states.iterator();
        int i = 0;
        while (itStates.hasNext()) {
            Element state = (Element) itStates.next();
            Element stateName = (Element) state.selectSingleNode("properties/property[@name='name']");
            Element stateCaption = (Element) state.selectSingleNode("properties/property[@name='caption']");
            if (stateName != null) {
                String name = stateName.attributeValue("value", "???");
                String caption = stateCaption.attributeValue("value", "");
                if (name.equals("")) name = "???";
                if (!name.equals("")) {
                    System.out.println("State: " + name + ", vertex number: " + String.valueOf(i).toString());
                    v[i] = g.addVertex(new SparseVertex());
                    vertexName.put(v[i], name);
                    vertexCaption.put(v[i], caption);
                    nameVertex.put(name, v[i]);
                    Element initialState = (Element) workflowProcess.selectSingleNode("properties/property[@name='initialState']");
                    if (initialState != null && initialState.attributeValue("value", "").equals(name)) {
                        v[i].setUserDatum(COLORKEY, Color.YELLOW, UserData.REMOVE);
                    } else {
                        v[i].setUserDatum(COLORKEY, Color.BLUE, UserData.REMOVE);
                    }
                    Iterator endStates = workflowProcess.selectNodes("properties/property[@name='endState']").iterator();
                    while (endStates.hasNext()) {
                        Element endState = (Element) endStates.next();
                        if (endState != null && endState.attributeValue("value", "").equals(name)) {
                            v[i].setUserDatum(COLORKEY, Color.RED, UserData.REMOVE);
                        }
                    }
                    i++;
                }
            }
        }
        return v;
    }

    /**
     * create edges for this graph
     * @param v an array of Vertices to connect
     */
    private void createEdges(Vertex[] v) {
        Element workflowRoot = workflowDoc.getRootElement();
        List states = workflowRoot.selectNodes("//item[@name='state']");
        if (states == null) return;
        Iterator itStates = states.iterator();
        while (itStates.hasNext()) {
            Element state = (Element) itStates.next();
            Element stateName = (Element) state.selectSingleNode("properties/property[@name='name']");
            System.out.println("Creating edges, name: " + stateName);
            if (stateName != null) {
                String name = stateName.attributeValue("value", "");
                String transitionsPath = "item[@name='transitions']/item[@type='transition']/properties/property[@name='toState']";
                Iterator itTransitions = state.selectNodes(transitionsPath).iterator();
                while (itTransitions.hasNext()) {
                    Element transition = (Element) itTransitions.next();
                    String toState = transition.attributeValue("value", "");
                    if (!toState.equals("")) {
                        if (nameVertex.containsKey(name) && nameVertex.containsKey(toState)) {
                            Edge newEdge = g.addEdge(new DirectedSparseEdge((Vertex) nameVertex.get(name), (Vertex) nameVertex.get(toState)));
                            String itemName = transition.getParent().getParent().attributeValue("name", "");
                            if (!itemName.equals("transitionManual")) {
                                newEdge.setUserDatum(COLORKEY, Color.ORANGE, UserData.REMOVE);
                            }
                        }
                    }
                }
                String startThreadPath = "item[@name='onEntry']/item[@name='startParallelThread']/properties/property[@name='name']";
                itTransitions = state.selectNodes(startThreadPath).iterator();
                while (itTransitions.hasNext()) {
                    Element thread = (Element) itTransitions.next();
                    String threadName = thread.attributeValue("value", "");
                    System.out.println("startParallelThread name: " + threadName);
                    if (!threadName.equals("")) {
                        Element pThread = (Element) workflowRoot.selectSingleNode("//item[@name='parallelThread']/" + "properties/property[@name='name' and @value='" + threadName + "']");
                        if (pThread != null) {
                            pThread = pThread.getParent().getParent();
                            Element sThreadStartState = (Element) pThread.selectSingleNode("./properties/property[@name='startState']");
                            if (sThreadStartState != null) {
                                String toState = sThreadStartState.attributeValue("value", "");
                                if (!toState.equals("")) {
                                    if (nameVertex.containsKey(name) && nameVertex.containsKey(toState)) {
                                        Edge newEdge = g.addEdge(new DirectedSparseEdge((Vertex) nameVertex.get(name), (Vertex) nameVertex.get(toState)));
                                        newEdge.setUserDatum(COLORKEY, Color.GREEN, UserData.REMOVE);
                                        newEdge.setUserDatum(THICKNESSKEY, PluggableRenderer.DOTTED, UserData.REMOVE);
                                    }
                                }
                            }
                        }
                    }
                }
                startThreadPath = "item[@name='onExit']/item[@name='startParallelThread']/properties/property[@name='name']";
                itTransitions = state.selectNodes(startThreadPath).iterator();
                while (itTransitions.hasNext()) {
                    Element thread = (Element) itTransitions.next();
                    String threadName = thread.attributeValue("value", "");
                    System.out.println("startParallelThread name: " + threadName);
                    if (!threadName.equals("")) {
                        Element pThread = (Element) workflowRoot.selectSingleNode("//item[@name='parallelThread']/" + "properties/property[@name='name' and @value='" + threadName + "']");
                        if (pThread != null) {
                            pThread = pThread.getParent().getParent();
                            Element sThreadStartState = (Element) pThread.selectSingleNode("./properties/property[@name='startState']");
                            if (sThreadStartState != null) {
                                String toState = sThreadStartState.attributeValue("value", "");
                                if (!toState.equals("")) {
                                    if (nameVertex.containsKey(name) && nameVertex.containsKey(toState)) {
                                        Edge newEdge = g.addEdge(new DirectedSparseEdge((Vertex) nameVertex.get(name), (Vertex) nameVertex.get(toState)));
                                        newEdge.setUserDatum(COLORKEY, Color.GREEN, UserData.REMOVE);
                                        newEdge.setUserDatum(THICKNESSKEY, PluggableRenderer.DOTTED, UserData.REMOVE);
                                    }
                                }
                            }
                        }
                    }
                }
                String waitThreadPath = "item[@name='transitions']/item[@name='waitForParallelThread']";
                itTransitions = state.selectNodes(waitThreadPath).iterator();
                while (itTransitions.hasNext()) {
                    String threadName = "";
                    Element waitEle = (Element) itTransitions.next();
                    Iterator itWaits = waitEle.selectNodes("properties/property[@name='name']").iterator();
                    while (itWaits.hasNext()) {
                        Element thread = (Element) itWaits.next();
                        threadName = thread.attributeValue("value", "");
                        System.out.println("waitForParallelThread name: " + threadName + ", to state:" + name);
                        if (!threadName.equals("")) {
                            Element pThread = (Element) workflowRoot.selectSingleNode("//item[@name='parallelThread']/" + "properties/property[@name='name' and @value='" + threadName + "']");
                            if (pThread != null) {
                                pThread = pThread.getParent().getParent();
                                Iterator itThreadEndStates = pThread.selectNodes("./properties/property[@name='endState']").iterator();
                                while (itThreadEndStates.hasNext()) {
                                    Element sThreadEndState = (Element) itThreadEndStates.next();
                                    if (sThreadEndState != null) {
                                        String endState = sThreadEndState.attributeValue("value", "");
                                        if (!endState.equals("")) {
                                            if (nameVertex.containsKey(name) && nameVertex.containsKey(endState)) {
                                                Edge newEdge = g.addEdge(new DirectedSparseEdge((Vertex) nameVertex.get(endState), (Vertex) nameVertex.get(name)));
                                                newEdge.setUserDatum(COLORKEY, Color.PINK, UserData.REMOVE);
                                                newEdge.setUserDatum(THICKNESSKEY, PluggableRenderer.DOTTED, UserData.REMOVE);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Element waitToStateEle = (Element) waitEle.selectSingleNode("properties/property[@name='toState']");
                    String toState = "";
                    if (waitToStateEle != null) toState = waitToStateEle.attributeValue("value", "");
                    if (!threadName.equals("") && !toState.equals("")) {
                        if (nameVertex.containsKey(name) && nameVertex.containsKey(toState)) {
                            Edge newEdge = g.addEdge(new DirectedSparseEdge((Vertex) nameVertex.get(name), (Vertex) nameVertex.get(toState)));
                            newEdge.setUserDatum(COLORKEY, Color.RED, UserData.REMOVE);
                        }
                    }
                }
            }
        }
    }

    private void updateWorkflowStateLayout() {
        Element workflowRoot = workflowDoc.getRootElement();
        List states = workflowRoot.selectNodes("//item[@name='state']");
        if (states == null) return;
        Iterator itStates = states.iterator();
        while (itStates.hasNext()) {
            Element state = (Element) itStates.next();
            Element stateName = (Element) state.selectSingleNode("properties/property[@name='name']");
            if (stateName != null) {
                String name = stateName.attributeValue("value", "");
                if (nameVertex.containsKey(name)) {
                    String x = String.valueOf(layout.getX((Vertex) nameVertex.get(name)));
                    x = x.substring(0, x.indexOf("."));
                    String y = String.valueOf(layout.getY((Vertex) nameVertex.get(name)));
                    y = y.substring(0, y.indexOf("."));
                    state.addAttribute("x", x);
                    state.addAttribute("y", y);
                }
            }
        }
    }

    public String downloadFromUrl(URL url) {
        BufferedReader dis;
        String content = "";
        HttpURLConnection urlConn = null;
        try {
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            urlConn.setAllowUserInteraction(false);
            dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line;
            while ((line = dis.readLine()) != null) {
                content = content.concat(line);
                content = content.concat("\n");
            }
        } catch (MalformedURLException ex) {
            System.err.println(ex + " (downloadFromUrl) " + url);
        } catch (java.io.IOException iox) {
            System.out.println(iox + " (downloadFromUrl) " + url);
        } catch (Exception generic) {
            System.out.println(generic.toString() + " (downloadFromUrl) " + url);
        } finally {
        }
        return content;
    }

    public boolean uploadToUrl(URL url, Map postData) {
        boolean success = false;
        OutputStream oStream;
        HttpURLConnection urlConn = null;
        try {
            urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);
            urlConn.setAllowUserInteraction(false);
            String parametersAsString = "";
            Iterator itParams = postData.entrySet().iterator();
            while (itParams.hasNext()) {
                Map.Entry param = (Map.Entry) itParams.next();
                parametersAsString += (String) param.getKey() + "=" + URLEncoder.encode((String) param.getValue(), "UTF-8") + "&";
            }
            byte[] parameterAsBytes = parametersAsString.getBytes();
            urlConn.setRequestProperty("Content=length", String.valueOf(parameterAsBytes.length));
            oStream = urlConn.getOutputStream();
            oStream.write(parameterAsBytes);
            oStream.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                System.err.println("Response: " + line);
            }
            oStream.close();
            rd.close();
            success = true;
        } catch (MalformedURLException ex) {
            System.err.println(ex + " (uploadToUrl)");
            success = false;
        } catch (java.io.IOException iox) {
            System.out.println(iox + " (uploadToUrl)");
            success = false;
        } catch (Exception generic) {
            System.out.println(generic.toString() + " (uploadToUrl)");
            success = false;
        } finally {
            success = false;
        }
        return success;
    }

    private void readWorkflowData() {
        URL url;
        try {
            url = new URL((String) appletData.get("xmlGetUrl"));
        } catch (MalformedURLException e) {
            System.out.println("Invalid url for workflow XML file: " + e.toString());
            return;
        }
        SAXReader xIn = new SAXReader();
        String name = "wfp";
        String caption = "Workflow process";
        String type = "2";
        try {
            workflowDoc = xIn.read(url);
        } catch (DocumentException e) {
            String workflowDefinitionXML = downloadFromUrl(url);
            try {
                workflowDoc = DocumentHelper.parseText(workflowDefinitionXML);
            } catch (Exception e2) {
                workflowDoc = DocumentHelper.createDocument();
                Element ntRoot = workflowDoc.addElement("definition");
                ntRoot.addAttribute("name", name);
                ntRoot.addAttribute("caption", caption);
                ntRoot.addAttribute("type", type);
            }
        }
    }

    public class VertexNodeNameStringer implements VertexStringer {

        public VertexNodeNameStringer(Vertex[] vertices) {
        }

        /**
	     * @see edu.uci.ics.jung.graph.decorators.EdgeStringer#getLabel(ArchetypeEdge)
	     */
        public String getLabel(ArchetypeVertex v) {
            String caption = (String) vertexCaption.get(v);
            String x = String.valueOf(layout.getX((Vertex) v));
            x = x.substring(0, x.indexOf("."));
            String y = String.valueOf(layout.getY((Vertex) v));
            y = y.substring(0, y.indexOf("."));
            return caption;
        }
    }

    private void setupAppletData() {
        String xmlGetUrl;
        String xmlPostUrl;
        String nltSaveLayout;
        try {
            xmlGetUrl = getParameter("xmlGetUrl");
            xmlPostUrl = getParameter("xmlPostUrl");
            nltSaveLayout = getParameter("nltSaveLayout");
        } catch (Exception e) {
            xmlGetUrl = "file:///ss/ptest1";
            xmlPostUrl = "";
            nltSaveLayout = "Save layout";
        }
        if (xmlGetUrl == null) xmlGetUrl = "file:///ss/ptest1";
        if (xmlPostUrl == null) xmlPostUrl = "";
        if (nltSaveLayout == null) nltSaveLayout = "Save layout";
        appletData.put("xmlGetUrl", xmlGetUrl);
        appletData.put("xmlPostUrl", xmlPostUrl);
        appletData.put("nltSaveLayout", nltSaveLayout);
    }
}
