package org.gdi3d.xnavi.panels.map;

import java.awt.Color;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.media.ding3d.vecmath.Point2d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.gdi3d.xnavi.xml.wms.BoundingBox;
import org.gdi3d.xnavi.xml.wms.LatLonBoundingBox;
import org.gdi3d.xnavi.xml.wms.Layer;
import org.gdi3d.xnavi.xml.wms.Style;
import org.gdi3d.xnavi.xml.wms.WMT_MS_Capabilities;
import org.gdi3d.xnavi.xml.wms.WebMapServer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.gdi3d.xnavi.listeners.CheckListener;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.panels.prefs.PreferencesFrame;
import org.gdi3d.xnavi.swing.JCheckTree;
import org.gdi3d.xnavi.swing.TreeCheckBoxLeafNode;
import org.gdi3d.xnavi.swing.TreeRadioBoxLeafNode;
import org.gdi3d.xnavi.viewer.Java3DViewer;

public class ManualWMSConfigurationPanel extends JFrame implements CheckListener, ActionListener, MouseListener {

    InputStream inputStream;

    private Java3DViewer viewer;

    String mapServiceEndpoint;

    String ausgabeFormat;

    DefaultMutableTreeNode Top_Node;

    DefaultMutableTreeNode General_Node;

    WMT_MS_Capabilities wmsC;

    InputStream urlIn;

    String contentType;

    JButton OKButton;

    JButton upButton;

    JButton downButton;

    JButton loadPreview;

    JButton setButton;

    JButton closeButton;

    JList list;

    DefaultListModel listModel = new DefaultListModel();

    WMSConfiguration newWmsConfiguration, currentWmsConfiguration;

    WMSConfiguration wmsConfigurations;

    int currentWMSConfigurationIndex = 0;

    JLabel jLabel;

    String[] currentlayers;

    ImageIcon icon;

    WebMapServer wms;

    Object[] srs;

    String[] currentLayers;

    String[] styles;

    JSlider mapScaleSlider;

    String currentLayersAndStyles;

    private TreeRadioBoxLeafNode styleBox;

    TreeRadioBoxLeafNode[] styleCheckBox;

    Vector<TreeRadioBoxLeafNode> selectedStyleBoxVector = new Vector();

    JFrame frame;

    Layer[] layers;

    JSplitPane split;

    boolean b = false;

    JCheckTree layersTree;

    TreeCheckBoxLeafNode[] layerLeafNodes;

    Vector<String> vector = new Vector();

    DefaultMutableTreeNode defaultMutableTreeNode_Formats;

    private JButton loadPreviewButton;

    private PreferencesFrame preferencesFrame;

    public ManualWMSConfigurationPanel(PreferencesFrame preferencesFrame, String mapServiceEndpoint) {
        super("WebMapServer");
        this.preferencesFrame = preferencesFrame;
        this.mapServiceEndpoint = mapServiceEndpoint;
        this.setSize(750, 580);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setVisible(true);
        parseResponse();
        JSplitPane jSplitPane = jSplitPanel();
        add(jSplitPane);
    }

    public JSplitPane jSplitPanel() {
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setVisible(true);
        split.setDividerLocation(300);
        split.setDividerSize(5);
        split.setBackground(Color.WHITE);
        JSplitPane lPanel = leftPanel();
        JPanel rPanel = rightPanel();
        split.add(lPanel, JSplitPane.LEFT);
        split.add(rPanel, JSplitPane.RIGHT);
        return (split);
    }

    public JSplitPane leftPanel() {
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jSplitPane.setDividerLocation(100);
        JScrollPane jSprollPane = new JScrollPane(chooseLayersPanel());
        jSprollPane.getVerticalScrollBar().setUnitIncrement(5);
        jSprollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jSprollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        JScrollPane jScprollPane_settings = new JScrollPane(globalPropertyPanel());
        jScprollPane_settings.getVerticalScrollBar().setUnitIncrement(5);
        jScprollPane_settings.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScprollPane_settings.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jSplitPane.add(jSprollPane, JSplitPane.BOTTOM);
        jSplitPane.add(jScprollPane_settings, JSplitPane.TOP);
        return jSplitPane;
    }

    public JPanel rightPanel() {
        JPanel panel = new JPanel();
        panel.setSize(300, 300);
        panel.setEnabled(true);
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);
        upButton = new JButton(Navigator.i18n.getString("ManualWMSConfigurationPanel_UP_Button"));
        upButton.setLocation(10, 50);
        upButton.setSize(100, 20);
        upButton.addActionListener(this);
        downButton = new JButton(Navigator.i18n.getString("ManualWMSConfigurationPanel_DOWN_Button"));
        downButton.addActionListener(this);
        downButton.setLocation(10, 80);
        downButton.setSize(100, 20);
        loadPreviewButton = new JButton(Navigator.i18n.getString("ManualWMSConfigurationPanel_LOAD_PREVIEW_Button"));
        loadPreviewButton.addActionListener(this);
        loadPreviewButton.setLocation(120, 130);
        loadPreviewButton.setSize(130, 20);
        setButton = new JButton(Navigator.i18n.getString("ManualWMSConfigurationPanel_SET_Button"));
        setButton.setLocation(290, 130);
        setButton.setSize(130, 20);
        setButton.addActionListener(this);
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setSize(300, 100);
        scrollPane.setLocation(120, 20);
        Border border;
        BorderFactory borderFactory;
        border = BorderFactory.createLineBorder(Color.BLACK);
        list.setBorder(border);
        panel.add(upButton);
        panel.add(downButton);
        panel.add(loadPreviewButton);
        panel.add(setButton);
        panel.add(scrollPane);
        icon = new ImageIcon();
        jLabel = new JLabel(icon);
        jLabel.setSize(300, 300);
        jLabel.setLocation(120, 180);
        jLabel.setBorder(border);
        panel.add(jLabel);
        mapScaleSlider = new JSlider(10, 1000);
        mapScaleSlider.setInverted(true);
        mapScaleSlider.setLocation(120, 485);
        mapScaleSlider.setSize(300, 40);
        mapScaleSlider.setToolTipText("");
        mapScaleSlider.setBackground(Color.WHITE);
        mapScaleSlider.setValue(1000);
        mapScaleSlider.setMajorTickSpacing(100);
        mapScaleSlider.setPaintTicks(true);
        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(0), new JLabel("+"));
        labelTable.put(new Integer(1000), new JLabel("-"));
        mapScaleSlider.setLabelTable(labelTable);
        mapScaleSlider.setPaintLabels(true);
        mapScaleSlider.addMouseListener(this);
        panel.add(mapScaleSlider);
        return panel;
    }

    public void load_preview() {
        URL url = null;
        InputStream is = null;
        Socket s = new Socket();
        try {
            url = createWmsRequestURL();
            if (url != null) {
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setReadTimeout(Navigator.TIME_OUT);
                urlc.connect();
                is = urlc.getInputStream();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        icon = new ImageIcon(url);
        jLabel.setIcon(icon);
    }

    public JPanel globalPropertyPanel() {
        JPanel panel = new JPanel();
        General_Node = new DefaultMutableTreeNode(Navigator.i18n.getString("ManualWMSConfigurationPanel_PROPERTIES"));
        TreeRadioBoxLeafNode[] formats_local = new TreeRadioBoxLeafNode[wms.getCapabilities().getRequest().getGetMap().getFormats().size()];
        for (int i = 0; i < wms.getCapabilities().getRequest().getGetMap().getFormats().size(); i++) {
            vector.add(wms.getCapabilities().getRequest().getGetMap().getFormats().get(i).toString());
        }
        if (wms.getCapabilities().getService().getTitle() != null) {
            DefaultMutableTreeNode defaultMutableTreeNodeTitle = new DefaultMutableTreeNode(Navigator.i18n.getString("ManualWMSConfigurationPanel_TITLE"));
            DefaultMutableTreeNode titleLeaf = new DefaultMutableTreeNode(wms.getCapabilities().getService().getTitle());
            defaultMutableTreeNodeTitle.add(titleLeaf);
            General_Node.add(defaultMutableTreeNodeTitle);
        }
        if (wms.getCapabilities().getService().get_abstract() != null) {
            DefaultMutableTreeNode defaultMutableTreeNode_abstracts = new DefaultMutableTreeNode(Navigator.i18n.getString("ManualWMSConfigurationPanel_ABSTRACT"));
            DefaultMutableTreeNode abstractLeaf = new DefaultMutableTreeNode(wms.getCapabilities().getService().get_abstract());
            defaultMutableTreeNode_abstracts.add(abstractLeaf);
            General_Node.add(defaultMutableTreeNode_abstracts);
        }
        if (wms.getCapabilities().getService().getKeywordList() != null) {
            DefaultMutableTreeNode defaultMutableTreeNodeKeywordList = new DefaultMutableTreeNode(Navigator.i18n.getString("ManualWMSConfigurationPanel_KEYWORDS"));
            DefaultMutableTreeNode abstractLeaf = new DefaultMutableTreeNode(wms.getCapabilities().getService().getKeywordList());
            defaultMutableTreeNodeKeywordList.add(abstractLeaf);
            General_Node.add(defaultMutableTreeNodeKeywordList);
        }
        if (wms.getCapabilities().getLayer().getBoundingBoxes().size() > 0) {
            DefaultMutableTreeNode defaultMutableTreeNode_BoundingBoxes = new DefaultMutableTreeNode("BoundingBoxes");
            Vector<BoundingBox> boundingBoxes = wms.getCapabilities().getLayer().getBoundingBoxes();
            DefaultMutableTreeNode boundingBoxeLeaf;
            for (int i = 0; i < wms.getCapabilities().getLayer().getBoundingBoxes().size(); i++) {
                boundingBoxeLeaf = new DefaultMutableTreeNode(boundingBoxes.get(i));
                defaultMutableTreeNode_BoundingBoxes.add(boundingBoxeLeaf);
            }
            General_Node.add(defaultMutableTreeNode_BoundingBoxes);
        }
        DefaultMutableTreeNode defaultMutableTreeNode_SRS = new DefaultMutableTreeNode("SRS");
        DefaultMutableTreeNode srsLeaf;
        for (int i = 0; i < srs.length; i++) {
            srsLeaf = new DefaultMutableTreeNode(srs[i]);
            defaultMutableTreeNode_SRS.add(srsLeaf);
        }
        General_Node.add(defaultMutableTreeNode_SRS);
        JCheckTree layersTree2 = new JCheckTree(General_Node);
        layersTree2.setCheckListener(this);
        panel.setBackground(Color.white);
        panel.add(layersTree2);
        return panel;
    }

    public JPanel chooseLayersPanel() {
        Layer layer = wmsC.getLayer();
        srs = layer.getSrss().toArray();
        Top_Node = new DefaultMutableTreeNode("Layer");
        layerLeafNodes = new TreeCheckBoxLeafNode[layers.length];
        for (int i = 1; i < layers.length; i++) {
            String[] string = { layers[i].getName(), layers[i].getName() };
            layerLeafNodes[i] = new TreeCheckBoxLeafNode(layers[i].getName());
            layerLeafNodes[i].setDisplayName(layers[i].getTitle());
            layerLeafNodes[i].setID(layers[i].getName());
            layerLeafNodes[i].setTitle(layers[i].getTitle());
            Top_Node.add(layerLeafNodes[i]);
            if (layers[i].getKeywords() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Keywords");
                String keywords = "";
                for (int j = 0; j < layers[i].getKeywords().length; j++) keywords = layers[i].getKeywords()[j] + ", " + keywords;
                DefaultMutableTreeNode l1 = new DefaultMutableTreeNode(keywords);
                treeNode.add(l1);
                layerLeafNodes[i].add(treeNode);
            }
            if (layers[i].get_abstract() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Abstract");
                DefaultMutableTreeNode defaultMutableLeaf = new DefaultMutableTreeNode((layers[i].get_abstract()));
                treeNode.add(defaultMutableLeaf);
                layerLeafNodes[i].add(treeNode);
            }
            if (layers[i].getLatLonBoundingBox() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("LatLonBoundingBox");
                LatLonBoundingBox bbox = layers[i].getLatLonBoundingBox();
                if (bbox != null) {
                    String s = bbox.getMinx() + "," + bbox.getMiny() + "," + bbox.getMaxx() + "," + bbox.getMaxy();
                    DefaultMutableTreeNode defaultMutableLeaf = new DefaultMutableTreeNode(s);
                    treeNode.add(defaultMutableLeaf);
                    layerLeafNodes[i].add(treeNode);
                }
            }
            if (layers[i].getSrss() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("SRS");
                DefaultMutableTreeNode defaultMutableLeaf = new DefaultMutableTreeNode(layers[i].getSrss());
                treeNode.add(defaultMutableLeaf);
                layerLeafNodes[i].add(treeNode);
            }
            if (layers[i].getStyles() != null && layers[i].getStyles().size() > 0) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("Styles");
                for (int j = 0; j < layers[i].getStyles().size(); j++) {
                    Style style = layers[i].getStyles().get(j);
                    styleBox = new TreeRadioBoxLeafNode(style.getName());
                    if (j == 0) {
                        styleBox.setSelected(true);
                        selectedStyleBoxVector.add(styleBox);
                    }
                    treeNode.add(styleBox);
                }
                layerLeafNodes[i].add(treeNode);
            }
            if (layers[i].getMinScaleDenominator() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("MinScaleDenominator");
                DefaultMutableTreeNode defaultMutableLeaf = new DefaultMutableTreeNode((layers[i].getMinScaleDenominator()));
                treeNode.add(defaultMutableLeaf);
                layerLeafNodes[i].add(treeNode);
            }
            if (layers[i].getMinScaleDenominator() != null) {
                DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode("MaxScaleDenominator");
                DefaultMutableTreeNode defaultMutableLeaf = new DefaultMutableTreeNode((layers[i].getMinScaleDenominator()));
                treeNode.add(defaultMutableLeaf);
                layerLeafNodes[i].add(treeNode);
            }
        }
        layersTree = new JCheckTree(Top_Node);
        layersTree.setCheckListener(this);
        for (int m = 0; m < selectedStyleBoxVector.size(); m++) {
            layersTree.setState(selectedStyleBoxVector.get(m), true);
        }
        JPanel panel = new JPanel();
        panel.setBackground(Color.white);
        panel.add(layersTree);
        return panel;
    }

    private URL createWmsRequestURL() {
        URL url = null;
        double minx = Double.POSITIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < listModel.getSize(); i++) {
            TreeCheckBoxLeafNode t = (TreeCheckBoxLeafNode) listModel.get(i);
            for (int l = 0; l < t.getChildCount(); l++) {
                if (t.getChildAt(l).toString() == "LatLonBoundingBox") {
                    StringTokenizer tok = new StringTokenizer(t.getChildAt(l).getChildAt(0).toString(), ",");
                    minx = Math.min(minx, Double.parseDouble(tok.nextToken()));
                    miny = Math.min(miny, Double.parseDouble(tok.nextToken()));
                    maxx = Math.max(maxx, Double.parseDouble(tok.nextToken()));
                    maxy = Math.max(maxy, Double.parseDouble(tok.nextToken()));
                }
            }
        }
        Point2d mapCenter = new Point2d((minx + maxx) / 2.0, (miny + maxy) / 2.0);
        double zoom = (double) (mapScaleSlider.getValue() / 1000.0);
        zoom = Math.pow(zoom, 2);
        double mapSize = Math.max(maxx - minx, maxy - miny) * zoom;
        Point2d coordmin = new Point2d(mapCenter.x - mapSize / 2.0, mapCenter.y - mapSize / 2.0);
        Point2d coordmax = new Point2d(mapCenter.x + mapSize / 2.0, mapCenter.y + mapSize / 2.0);
        try {
            if (currentWmsConfiguration != null) {
                String urlString = currentWmsConfiguration.getServiceEndPoint() + "?" + "bbox=" + coordmin.x + "," + coordmin.y + "," + coordmax.x + "," + coordmax.y + "&Format=" + currentWmsConfiguration.getFormat() + "&request=GetMap" + currentLayersAndStyles + "&width=" + 300 + "&height=" + 300 + "&srs=EPSG:4326";
                if (currentWmsConfiguration.getMap() != null) {
                    urlString += "&map=" + currentWmsConfiguration.getMap();
                }
                if (currentWmsConfiguration.getVersion() != null) {
                    urlString += "&version=" + currentWmsConfiguration.getVersion();
                }
                if (Navigator.isVerbose()) {
                }
                url = new URL(urlString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Navigator.isVerbose()) System.out.println("wms:  " + url);
        return url;
    }

    private void createCurrentLayersAndStyles() {
        currentLayersAndStyles = "";
        WMSConfiguration wmsConfiguration = newWmsConfiguration;
        if (wmsConfiguration.getLayers() != null && wmsConfiguration.getStyles() != null) {
            String[] layers = wmsConfiguration.getLayers();
            String[] styles = wmsConfiguration.getStyles();
            if (layers.length == styles.length) {
                currentLayersAndStyles += "&layers=";
                int numLayers = layers.length;
                for (int i = 0; i < numLayers; i++) {
                    currentLayersAndStyles += layers[i];
                    if (i < (numLayers - 1)) currentLayersAndStyles += ",";
                }
                currentLayersAndStyles += "&styles=";
                for (int i = 0; i < numLayers; i++) {
                    currentLayersAndStyles += styles[i];
                    if (i < (numLayers - 1)) currentLayersAndStyles += ",";
                }
            }
        }
    }

    private void swap(int a, int b) {
        Object aObject = listModel.getElementAt(a);
        Object bObject = listModel.getElementAt(b);
        listModel.set(a, bObject);
        listModel.set(b, aObject);
    }

    public void parseResponse() {
        wms = null;
        URL url = null;
        try {
            String urls = mapServiceEndpoint + "?REQUEST=GetCapabilities&VERSION=1.1.1&Service=WMS";
            System.out.println(urls);
            url = new URL(urls);
        } catch (MalformedURLException e) {
            finished(Navigator.i18n.getString("ManualWMSConfigurationPanel_INVALID_URL"));
        }
        try {
            wms = new WebMapServer(url);
        } catch (Exception e) {
            finished(Navigator.i18n.getString("ManualWMSConfigurationPanel_INVALID_WMS"));
        }
        wmsC = wms.getCapabilities();
        Layer rootLayer = wmsC.getLayer();
        List layerList = wmsC.getLayerList();
        layers = new Layer[layerList.size()];
        for (int i = 0; i < layers.length; i++) {
            layers[i] = (Layer) layerList.get(i);
        }
    }

    public void checkChange(TreeNode node, Integer state) {
        if (node instanceof TreeCheckBoxLeafNode) {
            TreeCheckBoxLeafNode t = (TreeCheckBoxLeafNode) node;
            if (t.getState() == false) t.setState(true); else t.setState(false);
            if (state == 1) {
                listModel.addElement(t);
            }
            if (state == 3) {
                for (int i = 0; i < listModel.size(); i++) {
                    if (node == listModel.get(i)) listModel.remove(i);
                }
            }
        }
    }

    public void finished(String text) {
        setSize(400, 200);
        LayoutManager manager = null;
        this.setLayout(manager);
        this.getContentPane().removeAll();
        JLabel closeLabel = new JLabel(text);
        closeLabel.setSize(350, 30);
        closeLabel.setLocation(50, 20);
        add(closeLabel);
        closeButton = new JButton(Navigator.i18n.getString("ManualWMSConfigurationPanel_CLOSE_WINDOW"));
        closeButton.addActionListener(this);
        closeButton.setSize(200, 30);
        closeButton.setLocation(50, 50);
        add(closeButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listModel.size() != 0) {
            if (e.getSource() == upButton) {
                int moveMe = list.getSelectedIndex();
                if (moveMe != 0) {
                    swap(moveMe, moveMe - 1);
                    list.setSelectedIndex(moveMe - 1);
                    list.ensureIndexIsVisible(moveMe - 1);
                }
            }
            if (e.getSource() == downButton) {
                int moveMe = list.getSelectedIndex();
                if (moveMe != listModel.getSize() - 1) {
                    swap(moveMe, moveMe + 1);
                    list.setSelectedIndex(moveMe + 1);
                    list.ensureIndexIsVisible(moveMe + 1);
                }
            }
        }
        if (e.getSource() == loadPreviewButton) {
            createCurrentWmsConfiguration();
            createCurrentLayersAndStyles();
            load_preview();
        }
        if (e.getSource() == setButton) {
            if (currentLayers == null) {
                createCurrentWmsConfiguration();
                createCurrentLayersAndStyles();
            }
            Vector<MapConfiguration> mapConfigurations = preferencesFrame.getMapConfigurations();
            String title = wms.getCapabilities().getService().getTitle().trim();
            String checkTitle = new String(title);
            int count = 1;
            boolean check = true;
            while (check) {
                boolean titleExists = false;
                for (int t = 0; t < mapConfigurations.size(); t++) {
                    if (checkTitle.equals(mapConfigurations.get(t).getTitle().trim())) {
                        checkTitle = title + "[" + count++ + "]";
                        titleExists = true;
                        break;
                    }
                }
                if (!titleExists) {
                    title = checkTitle;
                    check = false;
                }
            }
            WMSConfiguration wmsConfiguration = new WMSConfiguration();
            wmsConfiguration.setTitle(title);
            wmsConfiguration.setServiceEndPoint(mapServiceEndpoint.toString());
            wmsConfiguration.setLayers(currentLayers);
            wmsConfiguration.setStyles(styles);
            wmsConfiguration.setFormat(ausgabeFormat);
            wmsConfiguration.setVersion(wmsC.getVersion());
            mapConfigurations.add(wmsConfiguration);
            preferencesFrame.getMapsComboBox().addItem(wmsConfiguration.getTitle());
            this.dispose();
        }
        if (e.getSource() == closeButton) {
            this.dispose();
        }
    }

    void createCurrentWmsConfiguration() {
        currentLayers = new String[listModel.size()];
        styles = new String[listModel.size()];
        TreeCheckBoxLeafNode t;
        int num = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            t = (TreeCheckBoxLeafNode) listModel.get(i);
            currentLayers[listModel.size() - i - 1] = t.getID().toString();
            for (int l = 0; l < t.getChildCount(); l++) {
                if (t.getChildAt(l).toString() == "Styles") {
                    num = l;
                }
            }
            for (int l = 0; l < t.getChildAt(num).getChildCount(); l++) {
                if (layersTree.getState(t.getChildAt(num).getChildAt(l)) == 1) {
                    styles[listModel.size() - i - 1] = t.getChildAt(num).getChildAt(l).toString();
                }
            }
            if (styles[listModel.size() - i - 1] == null) {
                styles[listModel.size() - i - 1] = "";
            }
        }
        for (int i = 0; i < vector.size(); i++) {
            if (vector.get(i).toString().contains("jpeg")) {
                ausgabeFormat = "image/jpeg";
            }
        }
        if (ausgabeFormat == "") ausgabeFormat = "image/png";
        newWmsConfiguration = new WMSConfiguration();
        newWmsConfiguration.setFormat(ausgabeFormat);
        newWmsConfiguration.setLayers(currentLayers);
        newWmsConfiguration.setMap("");
        newWmsConfiguration.setServiceEndPoint(mapServiceEndpoint);
        newWmsConfiguration.setStyles(styles);
        newWmsConfiguration.setTitle(wms.getCapabilities().getService().getTitle());
        newWmsConfiguration.setVersion(wmsC.getVersion());
        currentWmsConfiguration = newWmsConfiguration;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        createCurrentWmsConfiguration();
        createCurrentLayersAndStyles();
        load_preview();
    }
}
