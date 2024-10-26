package org.gdi3d.xnavi.panels.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.media.ding3d.Transform3D;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.media.ding3d.vecmath.Matrix3d;
import javax.media.ding3d.vecmath.Matrix4d;
import javax.media.ding3d.vecmath.Point2d;
import javax.media.ding3d.vecmath.Point3d;
import javax.media.ding3d.vecmath.Vector3d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.opengis.gml.PointDocument;
import net.opengis.gml.PosDocument;
import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import de.i3mainz.sort.SortAlgorithm;
import de.i3mainz.sort.SortItem;
import org.gdi3d.xnavi.listeners.UpdateListener;
import org.gdi3d.xnavi.navigation.NavigationBehavior;
import org.gdi3d.xnavi.navigation.OrbitBehavior;
import org.gdi3d.xnavi.navigation.PathFollowingBehavior;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.viewer.Java3DViewer;
import org.gdi3d.xnavi.viewer.myCanvas3D;

public class MapPanel extends JPanel implements UpdateListener, ActionListener, MouseListener, MouseMotionListener {

    /** Einheit CRS in Metern */
    private double metersPerUnit_CRS_X = 1.0;

    private double metersPerUnit_CRS_Y = 1.0;

    /** Breite des Kartenfensters (pixel) */
    private static final int MAP_WIDTH = 200;

    /** H�he des Kartenfensters (pixel) */
    private static final int MAP_HEIGHT = 300;

    private static final double WMS_TILEMATRIX_MIN_X = 0.0;

    private static final double WMS_TILEMATRIX_MAX_Y = 0.0;

    private TileMatrix tileMatrix;

    private int center_tile_raster_i;

    private int center_tile_raster_j;

    private double center_tile_center_x;

    private double center_tile_center_y;

    private double move_x_meters;

    private double move_y_meters;

    private int move_x_pixels;

    private int move_y_pixels;

    /** Ma�stab in Meter pro Pixel */
    double mapScaleX;

    double mapScaleY;

    public static GeoImageMeta meta = new GeoImageMeta();

    private JLabel[][] lab = new JLabel[3][3];

    private Point3d currentLocationInMapSpace = new Point3d();

    private boolean b = true;

    private Vector3d entryPoint;

    private ImageIcon icon;

    private Java3DViewer viewer;

    private int scaleSlider_minValue = 0;

    private int scaleSlider_maxValue;

    /** Gr��e der einzelnen Karten-Kacheln */
    private double tileSpanX = -1;

    private double tileSpanY = -1;

    private static boolean reload = true;

    private int compass_size;

    private double heading;

    private JLabel compassAndScalebar = new JLabel();

    private JLabel viewPort = new JLabel();

    private Transform3D currentTransform = new Transform3D();

    private Matrix3d currentTransformMatrix = new Matrix3d();

    private MapOverviewThread mapOverviewThread;

    private Vector<MapConfiguration> mapConfigurations;

    private int currentMapConfigurationIndex = 0;

    private MapConfiguration currentMapConfiguration;

    ReloadTilesThread reloadTilesThread;

    int counter = 0;

    JButton button;

    private JComboBox mapsComboBox;

    boolean bool = true;

    public boolean reqwer = true;

    int count = 0;

    int countWMS = 0;

    private JSlider scaleSlider = new JSlider();

    private Transform3D virtualToMapTransform = new Transform3D();

    public MapPanel(Vector<MapConfiguration> mapConfigurations) {
        super();
        this.mapConfigurations = mapConfigurations;
        currentMapConfigurationIndex = -1;
        if (mapConfigurations.size() > 0) {
            currentMapConfigurationIndex = 0;
            currentMapConfiguration = mapConfigurations.get(currentMapConfigurationIndex);
            prepareCurrentMapConfiguration();
        }
    }

    public void makeMapWindow() {
        try {
            this.removeAll();
            this.setLayout(null);
            manual_settings();
            viewPort.setLocation(0, 0);
            viewPort.addMouseListener(this);
            viewPort.setSize(MAP_WIDTH, MAP_HEIGHT);
            viewPort.setVisible(true);
            viewPort.setName("viewPort");
            add(viewPort);
            currentLocationInMapSpace.x = entryPoint.x;
            currentLocationInMapSpace.y = entryPoint.y;
            virtualToMapTransform.transform(currentLocationInMapSpace);
            mapScaleX = 1.0;
            mapScaleY = 1.0;
            if (tileMatrix != null) {
                mapScaleX = tileSpanX / tileMatrix.getTileWidth();
                mapScaleY = tileSpanY / tileMatrix.getTileHeight();
            }
            compassAndScalebar = new JLabel();
            compassAndScalebar.setUI(new DirectionalArrow(heading, compass_size, mapScaleX, true, MAP_WIDTH, MAP_HEIGHT));
            compassAndScalebar.setSize(MAP_WIDTH, MAP_HEIGHT);
            compassAndScalebar.setLocation(0, 0);
            viewPort.add(compassAndScalebar, 0);
            viewPort.addMouseMotionListener(this);
            int scaleSliderValue = scaleSlider_maxValue / 2;
            if (Navigator.startConfig.getMapZoomLevel() != null) {
                scaleSliderValue = Navigator.startConfig.getMapZoomLevel().intValue();
                if (scaleSliderValue < scaleSlider_minValue) scaleSliderValue = scaleSlider_minValue;
                if (scaleSliderValue > scaleSlider_maxValue) scaleSliderValue = scaleSlider_maxValue;
            }
            scaleSlider.setValue(scaleSliderValue);
            scaleSlider.setMinorTickSpacing(1);
            scaleSlider.setPaintTicks(true);
            scaleSlider.setSnapToTicks(true);
            Hashtable labelTable = new Hashtable();
            labelTable.put(new Integer(0), new JLabel("+"));
            labelTable.put(new Integer(scaleSlider_maxValue), new JLabel("-"));
            scaleSlider.setLabelTable(labelTable);
            scaleSlider.setPaintLabels(true);
            scaleSlider.addMouseListener(this);
            add(scaleSlider);
            String[] wms = new String[mapConfigurations.size()];
            for (int i = 0; i < mapConfigurations.size(); i++) {
                MapConfiguration mapConfiguration = mapConfigurations.get(i);
                wms[i] = mapConfigurations.get(i).getTitle();
            }
            mapsComboBox = new JComboBox();
            configureMapComboBox(mapConfigurations, mapConfigurations.size() - 1);
            mapsComboBox.repaint();
            mapsComboBox.setBounds(20, 350, 170, 20);
            add(mapsComboBox);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void configureMapComboBox(Vector<MapConfiguration> mapConfigurations, int selectedIndex) {
        mapsComboBox.removeActionListener(this);
        mapsComboBox.removeAllItems();
        for (int i = 0; i < mapConfigurations.size(); i++) {
            mapsComboBox.addItem(mapConfigurations.get(i).getTitle());
        }
        if (currentMapConfigurationIndex != selectedIndex) {
            if (selectedIndex >= 0) {
                currentMapConfiguration = mapConfigurations.get(selectedIndex);
                currentMapConfigurationIndex = selectedIndex;
                prepareCurrentMapConfiguration();
            } else currentMapConfiguration = null;
            mapsComboBox.setSelectedIndex(selectedIndex);
            if (mapOverviewThread != null) {
                mapOverviewThread.reload2();
                mapOverviewThread.wakeUp();
            }
        }
        mapsComboBox.addActionListener(this);
    }

    public void manual_settings() {
        compass_size = 50;
    }

    private ImageIcon getImageIcon(int size, double meter_size, double x_min, double y_min) {
        ImageIcon imageIcon = null;
        try {
            meta.extent_x1 = x_min;
            meta.extent_y1 = y_min;
            meta.extent_x2 = meta.extent_x1 + meter_size;
            meta.extent_y2 = meta.extent_y1 + meter_size;
            meta.image_width = size;
            meta.image_height = size;
            URL url = createMapRequestURL();
            if (Navigator.isVerbose()) System.out.println("map " + url);
            if (url != null) {
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setReadTimeout(100000);
                InputStream is = urlc.getInputStream();
                is.close();
                imageIcon = new ImageIcon(url);
                imageIcon.setDescription(x_min + "," + y_min);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageIcon;
    }

    public void setEntryPoint(Vector3d entryPoint) {
        this.entryPoint = entryPoint;
    }

    private void moveMap() {
        int tileWidth = this.tileMatrix.getTileWidth();
        int tileHeight = this.tileMatrix.getTileHeight();
        int mapCenterX = (int) (0.5 * this.MAP_WIDTH - this.move_x_pixels);
        int mapUpperLeftX = (int) (mapCenterX - 1.5 * tileWidth);
        int mapCenterY = (int) (0.5 * this.MAP_HEIGHT + this.move_y_pixels);
        int mapUpperLeftY = (int) (mapCenterY - 1.5 * tileHeight);
        lab[1][2].setLocation(mapUpperLeftX + tileWidth, mapUpperLeftY + 2 * tileHeight);
        lab[1][0].setLocation(mapUpperLeftX + tileWidth, mapUpperLeftY);
        lab[1][1].setLocation(mapUpperLeftX + tileWidth, mapUpperLeftY + tileHeight);
        lab[0][1].setLocation(mapUpperLeftX, mapUpperLeftY + tileHeight);
        lab[0][0].setLocation(mapUpperLeftX, mapUpperLeftY);
        lab[0][2].setLocation(mapUpperLeftX, mapUpperLeftY + 2 * tileHeight);
        lab[2][1].setLocation(mapUpperLeftX + 2 * tileWidth, mapUpperLeftY + tileHeight);
        lab[2][0].setLocation(mapUpperLeftX + 2 * tileWidth, mapUpperLeftY);
        lab[2][2].setLocation(mapUpperLeftX + 2 * tileWidth, mapUpperLeftY + 2 * tileHeight);
    }

    private void prepareCurrentMapConfiguration() {
        if (mapConfigurations != null && this.currentMapConfigurationIndex < mapConfigurations.size()) {
            MapConfiguration mapConfiguration = mapConfigurations.get(currentMapConfigurationIndex);
            if (mapConfiguration instanceof WMSConfiguration) {
                String layersAndStylesString = "";
                WMSConfiguration wmsConfiguration = (WMSConfiguration) mapConfiguration;
                if (wmsConfiguration.getLayers() != null && wmsConfiguration.getStyles() != null) {
                    String[] layers = wmsConfiguration.getLayers();
                    String[] styles = wmsConfiguration.getStyles();
                    if (layers.length == styles.length) {
                        layersAndStylesString += "&layers=";
                        int numLayers = layers.length;
                        for (int i = 0; i < numLayers; i++) {
                            layersAndStylesString += layers[i];
                            if (i < (numLayers - 1)) layersAndStylesString += ",";
                        }
                        layersAndStylesString += "&styles=";
                        for (int i = 0; i < numLayers; i++) {
                            layersAndStylesString += styles[i];
                            if (i < (numLayers - 1)) layersAndStylesString += ",";
                        }
                    }
                    wmsConfiguration.setLayersAndStylesString(layersAndStylesString);
                }
                metersPerUnit_CRS_X = 1.0;
                metersPerUnit_CRS_Y = 1.0;
                virtualToMapTransform.setIdentity();
                scaleSlider_maxValue = wmsConfiguration.getWMS_NUM_ZOOM_LEVELS() - 1;
                int scaleSliderValue = scaleSlider_maxValue / 2;
                if (Navigator.startConfig.getMapZoomLevel() != null) {
                    scaleSliderValue = Navigator.startConfig.getMapZoomLevel().intValue();
                    if (scaleSliderValue < scaleSlider_minValue) scaleSliderValue = scaleSlider_minValue;
                    if (scaleSliderValue > scaleSlider_maxValue) scaleSliderValue = scaleSlider_maxValue;
                }
                double scaleDenominator = wmsConfiguration.getWMS_minScaleDenominator() * Math.pow(2.0, scaleSliderValue);
                tileMatrix = new TileMatrix();
                tileMatrix.setIdentifier("" + scaleSliderValue);
                tileMatrix.setScaleDenominator(scaleDenominator);
                tileMatrix.setTopLeftPoint(new Point2d(WMS_TILEMATRIX_MIN_X, WMS_TILEMATRIX_MAX_Y));
                tileMatrix.setTileWidth(wmsConfiguration.getWMS_TILE_WIDTH());
                tileMatrix.setTileHeight(wmsConfiguration.getWMS_TILE_HEIGHT());
                tileMatrix.setMatrixWidth(Integer.MAX_VALUE);
                tileMatrix.setMatrixHeight(Integer.MAX_VALUE);
                initLabels(wmsConfiguration.getWMS_TILE_WIDTH(), wmsConfiguration.getWMS_TILE_HEIGHT());
                scaleSlider.setInverted(true);
                scaleSlider.setOrientation(0);
                scaleSlider.setBounds(00, 310, 205, 40);
                scaleSlider.setToolTipText("");
                scaleSlider.setMinimum(scaleSlider_minValue);
                scaleSlider.setMaximum(scaleSlider_maxValue);
            } else if (mapConfiguration instanceof WMTSConfiguration) {
                WMTSConfiguration wmtsConfiguration = (WMTSConfiguration) mapConfiguration;
                String serviceEndPoint = wmtsConfiguration.getServiceEndPoint().trim();
                String version = wmtsConfiguration.getVersion();
                if (!serviceEndPoint.endsWith("/")) serviceEndPoint += "/";
                String getCapabilitiesUrlString = serviceEndPoint + version + "/WMTSCapabilities.xml";
                System.out.println("WMTS getCapabilitiesUrlString " + getCapabilitiesUrlString);
                TileMatrixSet found_tileMatrixSet = null;
                URL url = null;
                try {
                    InputStream urlIn;
                    url = new URL(getCapabilitiesUrlString);
                    URLConnection urlc = url.openConnection();
                    urlc.setReadTimeout(Navigator.TIME_OUT);
                    urlIn = urlc.getInputStream();
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(urlIn);
                    NodeList tileMatrixSet_NodeList = document.getElementsByTagName("TileMatrixSet");
                    NodeList tileMatrixSet_NodeList2 = document.getElementsByTagName("*");
                    NodeList tileMatrixSet_NodeList3 = document.getElementsByTagNameNS("*", "TileMatrixSet");
                    int numTileMatrixSets = tileMatrixSet_NodeList.getLength();
                    int numTileMatrixSets2 = tileMatrixSet_NodeList2.getLength();
                    int numTileMatrixSets3 = tileMatrixSet_NodeList3.getLength();
                    List<Node> tileMatrixSet_NodeList4 = new Vector<Node>();
                    for (int n = 0; n < numTileMatrixSets2; n++) {
                        Node node = tileMatrixSet_NodeList2.item(n);
                        if (nodeName(node).equals("TileMatrixSet")) {
                            tileMatrixSet_NodeList4.add(node);
                        }
                    }
                    int numTileMatrixSets4 = tileMatrixSet_NodeList4.size();
                    for (int a = 0; a < numTileMatrixSets4; a++) {
                        TileMatrixSet tileMatrixSet = new TileMatrixSet();
                        tileMatrixSet.setTileMatrixArray(new ArrayList<TileMatrix>());
                        Node TileMatrixSet_Node = tileMatrixSet_NodeList4.get(a);
                        NodeList TileMatrixSet_Children = TileMatrixSet_Node.getChildNodes();
                        for (int b = 0; b < TileMatrixSet_Children.getLength(); b++) {
                            Node node = TileMatrixSet_Children.item(b);
                            if (nodeName(node).equals("Identifier")) {
                                String identifier = node.getTextContent().trim();
                                tileMatrixSet.setIdentifier(identifier);
                                if (identifier.equals(wmtsConfiguration.getTileMatrixSetName())) {
                                    found_tileMatrixSet = tileMatrixSet;
                                } else {
                                    continue;
                                }
                            } else if (nodeName(node).equals("SupportedCRS")) {
                                String srid = node.getTextContent().trim();
                                tileMatrixSet.setSrid(srid);
                            } else if (nodeName(node).equals("TileMatrix")) {
                                TileMatrix tileMatrix = parseTileMatrix(node);
                                tileMatrixSet.getTileMatrixArray().add(tileMatrix);
                            }
                        }
                        Vector<SortItem> sortList = new Vector<SortItem>();
                        ArrayList<TileMatrix> tileMatrixArray = tileMatrixSet.getTileMatrixArray();
                        for (int s = 0; s < tileMatrixArray.size(); s++) {
                            TileMatrix tm = tileMatrixArray.get(s);
                            SortItem item = new SortItem((float) tm.getScaleDenominator(), tm, s);
                            sortList.add(item);
                        }
                        SortAlgorithm algorithm = (SortAlgorithm) Class.forName("de.i3mainz.sort.FastQSortAlgorithm4").newInstance();
                        algorithm.sort(sortList);
                        ArrayList<TileMatrix> reverse_tileMatrixArray = new ArrayList<TileMatrix>();
                        for (int s = tileMatrixArray.size() - 1; s >= 0; s--) {
                            reverse_tileMatrixArray.add(tileMatrixArray.get(s));
                        }
                        tileMatrixSet.setTileMatrixArray(reverse_tileMatrixArray);
                    }
                    urlIn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                wmtsConfiguration.setTileMatrixSet(found_tileMatrixSet);
                metersPerUnit_CRS_X = 111111.1111;
                metersPerUnit_CRS_Y = 111111.1111;
                Transform3D scale_T3D = new Transform3D();
                scale_T3D.setScale(new Vector3d(1.0 / metersPerUnit_CRS_X, 1.0 / metersPerUnit_CRS_Y, 1.0));
                Transform3D trans_T3D = new Transform3D();
                trans_T3D.setTranslation(new Vector3d(-208145.399, -3318531.999, 0.0));
                Transform3D trans2_T3D = new Transform3D();
                trans2_T3D.setTranslation(new Vector3d(-90.024209, 29.962976, 0.0));
                virtualToMapTransform.set(trans2_T3D);
                virtualToMapTransform.mul(scale_T3D);
                virtualToMapTransform.mul(trans_T3D);
                if (found_tileMatrixSet != null) {
                    int numZoomLevels = found_tileMatrixSet.getTileMatrixArray().size();
                    scaleSlider_minValue = 0;
                    scaleSlider_maxValue = numZoomLevels - 1;
                    int value = scaleSlider.getValue();
                    if (value < scaleSlider_minValue) scaleSlider.setMinimum(scaleSlider_minValue);
                    if (value > scaleSlider_maxValue) scaleSlider.setValue(scaleSlider_maxValue);
                    tileMatrix = found_tileMatrixSet.getTileMatrixArray().get(scaleSlider.getValue());
                    initLabels(tileMatrix.getTileWidth(), tileMatrix.getTileHeight());
                    scaleSlider.setInverted(true);
                    scaleSlider.setOrientation(0);
                    scaleSlider.setBounds(00, 310, 205, 40);
                    scaleSlider.setToolTipText("");
                    scaleSlider.setMinimum(scaleSlider_minValue);
                    scaleSlider.setMaximum(scaleSlider_maxValue);
                } else {
                    System.out.println("MapPanel.prepareMapConfiguration: TileMatrixSet " + wmtsConfiguration.getTileMatrixSetName() + " not found in WMTS");
                }
            }
        }
    }

    private String nodeName(Node node) {
        String nodeName = null;
        StringTokenizer tok = new StringTokenizer(node.getNodeName(), ":");
        while (tok.hasMoreTokens()) nodeName = tok.nextToken();
        return nodeName;
    }

    private TileMatrix parseTileMatrix(Node node) throws XmlException, ParseException {
        TileMatrix tileMatrix = new TileMatrix();
        NodeList tileMatrix_Children = node.getChildNodes();
        for (int b = 0; b < tileMatrix_Children.getLength(); b++) {
            Node childNode = tileMatrix_Children.item(b);
            String nodeName = nodeName(childNode);
            if (nodeName.equals("Identifier")) {
                tileMatrix.setIdentifier(childNode.getTextContent().trim());
            } else if (nodeName.equals("ScaleDenominator")) {
                double scaleDenominator = Double.parseDouble(childNode.getTextContent().trim());
                tileMatrix.setScaleDenominator(scaleDenominator);
            } else if (nodeName.equals("TopLeftCorner")) {
                StringTokenizer tok = new StringTokenizer(childNode.getTextContent().trim(), " ", false);
                double x = Double.parseDouble(tok.nextToken());
                double y = Double.parseDouble(tok.nextToken());
                Point2d topLeftPoint = new Point2d(x, y);
                tileMatrix.setTopLeftPoint(topLeftPoint);
            } else if (nodeName.equals("TileWidth")) {
                int tileWidth = Integer.parseInt(childNode.getTextContent().trim());
                tileMatrix.setTileWidth(tileWidth);
            } else if (nodeName.equals("TileHeight")) {
                int tileHeight = Integer.parseInt(childNode.getTextContent().trim());
                tileMatrix.setTileHeight(tileHeight);
            } else if (nodeName.equals("MatrixWidth")) {
                int matrixWidth = Integer.parseInt(childNode.getTextContent().trim());
                tileMatrix.setMatrixWidth(matrixWidth);
            } else if (nodeName.equals("MatrixHeight")) {
                int matrixHeight = Integer.parseInt(childNode.getTextContent().trim());
                tileMatrix.setMatrixHeight(matrixHeight);
            }
        }
        return tileMatrix;
    }

    private URL createMapRequestURL() {
        URL url = null;
        counter++;
        try {
            if (currentMapConfiguration != null && currentMapConfiguration instanceof WMSConfiguration) {
                WMSConfiguration wmsConfiguration = (WMSConfiguration) currentMapConfiguration;
                int srid = 31467;
                if (Navigator.startConfig.getSrid() != null) {
                    int s = Navigator.startConfig.getSrid().intValue();
                    if (s > 0) {
                        srid = s;
                    }
                }
                meta.resolution_x = (meta.extent_x2 - meta.extent_x1) / meta.image_width;
                meta.resolution_y = (meta.extent_y2 - meta.extent_y1) / meta.image_height;
                String urlString = wmsConfiguration.getServiceEndPoint() + "?" + "bbox=" + meta.extent_x1 + "," + meta.extent_y1 + "," + meta.extent_x2 + "," + meta.extent_y2 + "&Format=" + wmsConfiguration.getFormat() + "&request=GetMap" + wmsConfiguration.getLayersAndStylesString() + "&width=" + meta.image_width + "&height=" + meta.image_height + "&srs=EPSG:" + srid;
                if (wmsConfiguration.getMap() != null) {
                    urlString += "&map=" + wmsConfiguration.getMap();
                }
                if (wmsConfiguration.getVersion() != null) {
                    urlString += "&version=" + wmsConfiguration.getVersion();
                }
                if (Navigator.isVerbose()) {
                }
                url = new URL(urlString);
            } else if (currentMapConfiguration != null && currentMapConfiguration instanceof WMTSConfiguration) {
                WMTSConfiguration wmtsConfiguration = (WMTSConfiguration) currentMapConfiguration;
                int srid = 31467;
                if (Navigator.startConfig.getSrid() != null) {
                    int s = Navigator.startConfig.getSrid().intValue();
                    if (s > 0) {
                        srid = s;
                    }
                }
                double tileCenterX = (meta.extent_x1 + meta.extent_x2) / 2.0;
                double tileCenterY = (meta.extent_y1 + meta.extent_y2) / 2.0;
                meta.resolution_x = (meta.extent_x2 - meta.extent_x1) / meta.image_width;
                meta.resolution_y = (meta.extent_y2 - meta.extent_y1) / meta.image_height;
                Point2d topleft = tileMatrix.getTopLeftPoint();
                double d_raster_i = ((tileCenterX - topleft.x - 0.5 * tileSpanX) / (double) tileSpanX + 0.5);
                double d_raster_j = -((tileCenterY - topleft.y - 0.5 * tileSpanY) / (double) tileSpanY + 0.5);
                if (d_raster_i < 0) d_raster_i -= 1.0;
                if (d_raster_j < 0) d_raster_j -= 1.0;
                int raster_i = (int) (d_raster_i);
                int raster_j = (int) (d_raster_j);
                if (raster_i >= 0 && raster_i < tileMatrix.getMatrixWidth() && raster_j >= 0 && raster_j < tileMatrix.getMatrixHeight()) {
                    String format = ".png";
                    if (wmtsConfiguration.getFormat().equals("image/png")) {
                        format = ".png";
                    } else if (wmtsConfiguration.getFormat().equals("image/gif")) {
                        format = ".gif";
                    }
                    String urlString = wmtsConfiguration.getServiceEndPoint();
                    if (!urlString.endsWith("/")) urlString += "/";
                    urlString += wmtsConfiguration.getLayer() + "/" + wmtsConfiguration.getTileMatrixSetName() + "/" + tileMatrix.getIdentifier() + "/" + raster_j + "/" + raster_i + format;
                    url = new URL(urlString);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public void makeMapWindow2() {
        makeMapWindow();
    }

    private void initLabels(int tileWidth, int tileHeight) {
        if (viewPort != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (lab[i][j] != null) viewPort.remove(lab[i][j]);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                lab[i][j] = new JLabel();
                lab[i][j].setSize(tileWidth, tileHeight);
            }
        }
        if (viewPort != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    viewPort.add(lab[i][j], -1);
                }
            }
        }
    }

    public void reloadMap() {
        lab[0][0].setIcon(null);
        lab[0][1].setIcon(null);
        lab[0][2].setIcon(null);
        lab[1][0].setIcon(null);
        lab[1][1].setIcon(null);
        lab[1][2].setIcon(null);
        lab[2][0].setIcon(null);
        lab[2][1].setIcon(null);
        lab[2][2].setIcon(null);
        moveValues();
        moveMap();
        int tileWidth = this.tileMatrix.getTileWidth();
        int tileHeight = this.tileMatrix.getTileHeight();
        icon = getImageIcon(tileWidth, tileSpanX, this.center_tile_center_x - tileSpanX / 2, center_tile_center_y - tileSpanY / 2);
        lab[1][1].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y + tileSpanY / 2);
        lab[0][0].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y - tileSpanY / 2);
        lab[0][1].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y - 1.5 * tileSpanY);
        lab[0][2].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - tileSpanX / 2, center_tile_center_y + tileSpanY / 2);
        lab[1][0].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - tileSpanX / 2, center_tile_center_y - 1.5 * tileSpanY);
        lab[1][2].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x + tileSpanX / 2, center_tile_center_y + tileSpanY / 2);
        lab[2][0].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x + tileSpanX / 2, center_tile_center_y - tileSpanY / 2);
        lab[2][1].setIcon(icon);
        icon = getImageIcon(tileWidth, tileSpanX, center_tile_center_x + tileSpanX / 2, center_tile_center_y - 1.5 * tileSpanY);
        lab[2][2].setIcon(icon);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof JComboBox) {
            JComboBox combo = (JComboBox) source;
            currentMapConfigurationIndex = combo.getSelectedIndex();
            currentMapConfiguration = mapConfigurations.get(currentMapConfigurationIndex);
            prepareCurrentMapConfiguration();
        }
        if (mapOverviewThread != null) {
            mapOverviewThread.reload2();
            mapOverviewThread.wakeUp();
        }
    }

    private void updateArrow() {
        compassAndScalebar.setUI(new DirectionalArrow(heading, compass_size, mapScaleX * metersPerUnit_CRS_X, true, MAP_WIDTH, MAP_HEIGHT));
        repaint();
    }

    private void checkMap() {
        moveValues();
        int tileWidth = this.tileMatrix.getTileWidth();
        int tileHeight = this.tileMatrix.getTileHeight();
        if ((move_x_meters > tileSpanX / 2.0)) {
            if (move_x_meters > tileSpanX * 3.0) {
                computePositionInRaster();
                reloadMap();
            } else {
                int new_center_tile_raster_i = center_tile_raster_i + 1;
                double new_center_tile_center_x = center_tile_center_x + tileSpanX;
                Icon icon1 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x + tileSpanX / 2, center_tile_center_y - tileSpanY / 2);
                Icon icon2 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x + tileSpanX / 2, center_tile_center_y + tileSpanY / 2);
                Icon icon3 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x + tileSpanX / 2, center_tile_center_y - 1.5 * tileSpanY);
                lab[0][1].setIcon(lab[1][1].getIcon());
                lab[1][1].setIcon(lab[2][1].getIcon());
                lab[2][1].setIcon(icon1);
                lab[0][0].setIcon(lab[1][0].getIcon());
                lab[1][0].setIcon(lab[2][0].getIcon());
                lab[2][0].setIcon(icon2);
                lab[0][2].setIcon(lab[1][2].getIcon());
                lab[1][2].setIcon(lab[2][2].getIcon());
                lab[2][2].setIcon(icon3);
                center_tile_raster_i = new_center_tile_raster_i;
                center_tile_center_x = new_center_tile_center_x;
                moveValues();
                moveMap();
            }
        }
        if ((move_x_meters < -tileSpanX / 2.0)) {
            if (move_x_meters < -tileSpanX * 3.0) {
                computePositionInRaster();
                reloadMap();
            } else {
                int new_center_tile_raster_i = center_tile_raster_i - 1;
                double new_center_tile_center_x = center_tile_center_x - tileSpanX;
                Icon icon1 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y - tileSpanY / 2);
                Icon icon2 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y + tileSpanY / 2);
                Icon icon3 = getImageIcon(tileWidth, tileSpanX, new_center_tile_center_x - 1.5 * tileSpanX, center_tile_center_y - 1.5 * tileSpanY);
                lab[2][1].setIcon(lab[1][1].getIcon());
                lab[1][1].setIcon(lab[0][1].getIcon());
                lab[0][1].setIcon(icon1);
                lab[2][0].setIcon(lab[1][0].getIcon());
                lab[1][0].setIcon(lab[0][0].getIcon());
                lab[0][0].setIcon(icon2);
                lab[2][2].setIcon(lab[1][2].getIcon());
                lab[1][2].setIcon(lab[0][2].getIcon());
                lab[0][2].setIcon(icon3);
                center_tile_raster_i = new_center_tile_raster_i;
                center_tile_center_x = new_center_tile_center_x;
                moveValues();
                moveMap();
            }
        }
        if ((move_y_meters > tileSpanY / 2.0)) {
            if (move_y_meters > tileSpanY * 3.0) {
                computePositionInRaster();
                reloadMap();
            } else {
                int new_center_tile_raster_j = center_tile_raster_j - 1;
                double new_center_tile_center_y = center_tile_center_y + tileSpanX;
                Icon icon1 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - tileSpanX / 2, new_center_tile_center_y + tileSpanY / 2);
                Icon icon2 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - 1.5 * tileSpanX, new_center_tile_center_y + tileSpanY / 2);
                Icon icon3 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x + tileSpanX / 2, new_center_tile_center_y + tileSpanY / 2);
                lab[1][2].setIcon(lab[1][1].getIcon());
                lab[1][1].setIcon(lab[1][0].getIcon());
                lab[1][0].setIcon(icon1);
                lab[0][2].setIcon(lab[0][1].getIcon());
                lab[0][1].setIcon(lab[0][0].getIcon());
                lab[0][0].setIcon(icon2);
                lab[2][2].setIcon(lab[2][1].getIcon());
                lab[2][1].setIcon(lab[2][0].getIcon());
                lab[2][0].setIcon(icon3);
                center_tile_raster_j = new_center_tile_raster_j;
                center_tile_center_y = new_center_tile_center_y;
                moveValues();
                moveMap();
            }
        }
        if ((move_y_meters < -tileSpanY / 2.0)) {
            if (move_y_meters < -tileSpanY * 3.0) {
                computePositionInRaster();
                reloadMap();
            } else {
                int new_center_tile_raster_j = center_tile_raster_j + 1;
                double new_center_tile_center_y = center_tile_center_y - tileSpanX;
                Icon icon1 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - tileSpanX / 2, new_center_tile_center_y - 1.5 * tileSpanY);
                Icon icon2 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x + tileSpanX / 2, new_center_tile_center_y - 1.5 * tileSpanY);
                Icon icon3 = getImageIcon(tileWidth, tileSpanX, center_tile_center_x - 1.5 * tileSpanX, new_center_tile_center_y - 1.5 * tileSpanY);
                lab[1][0].setIcon(lab[1][1].getIcon());
                lab[1][1].setIcon(lab[1][2].getIcon());
                lab[1][2].setIcon(icon1);
                lab[2][0].setIcon(lab[2][1].getIcon());
                lab[2][1].setIcon(lab[2][2].getIcon());
                lab[2][2].setIcon(icon2);
                lab[0][0].setIcon(lab[0][1].getIcon());
                lab[0][1].setIcon(lab[0][2].getIcon());
                lab[0][2].setIcon(icon3);
                center_tile_raster_j = new_center_tile_raster_j;
                center_tile_center_y = new_center_tile_center_y;
                moveValues();
                moveMap();
            }
        }
    }

    private void computePositionInRaster() {
        Point2d topleft = tileMatrix.getTopLeftPoint();
        center_tile_raster_i = (int) ((currentLocationInMapSpace.x - topleft.x) / tileSpanX);
        center_tile_raster_j = -(int) ((currentLocationInMapSpace.y - topleft.y) / tileSpanY);
        center_tile_center_x = topleft.x + center_tile_raster_i * tileSpanX + 0.5 * tileSpanX;
        center_tile_center_y = topleft.y - center_tile_raster_j * tileSpanY + 0.5 * tileSpanY;
    }

    private double getHeading() {
        viewer.navigation.getCurrentCRSTransform3D(currentTransform);
        currentTransform.get(currentTransformMatrix);
        double m00 = Math.asin(currentTransformMatrix.m00);
        double m20 = Math.asin(currentTransformMatrix.m20);
        double lon = m20;
        if (m00 < 0.0) lon = Math.PI - lon;
        return lon;
    }

    private void moveValues() {
        move_x_meters = currentLocationInMapSpace.x - center_tile_center_x;
        move_y_meters = currentLocationInMapSpace.y - center_tile_center_y;
        move_x_pixels = (int) (move_x_meters / mapScaleX);
        move_y_pixels = (int) (move_y_meters / mapScaleY);
    }

    int update_interval = 10;

    int update_counter = 0;

    public void update() {
        if (update_counter == update_interval) {
            if (mapOverviewThread != null) mapOverviewThread.wakeUp();
            update_counter = 0;
        } else {
            update_counter++;
        }
    }

    public void setViewer(Java3DViewer viewer) {
        this.viewer = viewer;
    }

    public void setComboBox(MapConfiguration ConfiguratnewtempWmsConfigurationion) {
        Vector<MapConfiguration> tempWmsConfiguration = new Vector<MapConfiguration>();
        for (int i = 0; i < mapConfigurations.size(); i++) {
            tempWmsConfiguration.add(mapConfigurations.get(i));
        }
        tempWmsConfiguration.set(tempWmsConfiguration.size() - 1, ConfiguratnewtempWmsConfigurationion);
        mapConfigurations = tempWmsConfiguration;
        String[] wms = new String[mapConfigurations.size()];
        countWMS = 0;
        for (int i = 0; i < mapConfigurations.size(); i++) {
            MapConfiguration wmsConfiguration = mapConfigurations.get(i);
            wms[i] = mapConfigurations.get(i).getTitle();
            for (int j = 0; j < mapConfigurations.size(); j++) {
                if (wms[j] != null) if (wms[i].toString().equals(wms[j].toString()) & (i != j)) {
                    countWMS = countWMS + 1;
                    wms[i] = wms[i] + "_" + countWMS;
                }
            }
        }
        remove(mapsComboBox);
        mapsComboBox = new JComboBox(wms);
        mapsComboBox.setBounds(20, 350, 170, 20);
        mapsComboBox.addActionListener(this);
        add(mapsComboBox);
        currentMapConfiguration = mapConfigurations.get(currentMapConfigurationIndex);
        prepareCurrentMapConfiguration();
        if (mapOverviewThread != null) {
            mapOverviewThread.reload();
            mapOverviewThread.wakeUp();
        }
        mapsComboBox.setSelectedIndex(currentMapConfigurationIndex);
    }

    public void setCurrentWMSConfigurationIndex(int index) {
        currentMapConfigurationIndex = index;
    }

    public void startThread() {
        mapOverviewThread = new MapOverviewThread();
        reloadTilesThread = new ReloadTilesThread();
        mapOverviewThread.start();
        reloadTilesThread.start();
    }

    public void stopThread() {
        if (mapOverviewThread != null) {
            mapOverviewThread.running = false;
        }
    }

    private class ReloadTilesThread extends Thread {

        private boolean active = true;

        private boolean checkZoom = false;

        public boolean running = true;

        public void checkZoom() {
            this.checkZoom = true;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public void setRunningTrue() {
            running = true;
        }

        public void setRunningFalse() {
            running = false;
        }

        public void run() {
            while (running) {
                if (active && tileMatrix != null) {
                    if (checkZoom) {
                        checkZoomLevel();
                        checkZoom = false;
                    }
                    checkMap();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MapOverviewThread extends Thread {

        Object lock = new Object();

        public boolean running = true;

        private boolean reload = true;

        private boolean reload2 = false;

        private Map<String, Boolean> WMS_Connections = new HashMap<String, Boolean>();

        public void run() {
            while (running) {
                NavigationBehavior nb = viewer.navigation;
                Vector3d platform_vec = nb.getViewPos();
                heading = getHeading();
                currentLocationInMapSpace.x = platform_vec.x;
                currentLocationInMapSpace.y = -(platform_vec.z);
                virtualToMapTransform.transform(currentLocationInMapSpace);
                if (b == true) {
                    currentLocationInMapSpace.x = entryPoint.x;
                    currentLocationInMapSpace.y = entryPoint.y;
                    virtualToMapTransform.transform(currentLocationInMapSpace);
                    reloadTilesThread.checkZoom();
                    b = false;
                }
                if (tileMatrix != null) {
                    moveValues();
                    updateArrow();
                    moveMap();
                }
                if (reload) {
                    assertConnection();
                    current_scaleSliderValue = -1;
                    reload = false;
                }
                if (reload2) {
                    assertConnection();
                    current_scaleSliderValue = -1;
                    checkZoomLevel();
                    reload2 = false;
                }
                goToSleep();
            }
        }

        private void assertConnection() {
            URL url = createMapRequestURL();
            if (url != null) {
                try {
                    if (!WMS_Connections.containsKey(url.toString())) {
                        HttpURLConnection urlc;
                        urlc = (HttpURLConnection) url.openConnection();
                        urlc.setReadTimeout(Navigator.TIME_OUT);
                        InputStream is = urlc.getInputStream();
                        is.close();
                        WMS_Connections.put(url.toString(), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        void goToSleep() {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setRunningTrue() {
            running = true;
        }

        public void setRunningFalse() {
            running = false;
        }

        void wakeUp() {
            synchronized (this) {
                notifyAll();
            }
        }

        public void reload() {
            MapPanel.reload = true;
            this.reload = true;
        }

        public void reload2() {
            this.reload2 = true;
        }
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

    double pressedX;

    double pressedY;

    @Override
    public void mousePressed(MouseEvent e) {
        xOld = e.getX();
        yOld = e.getY();
        yStart = e.getY();
        xStart = e.getX();
        startX = currentLocationInMapSpace.x;
        startY = currentLocationInMapSpace.y;
        versetzSumX = 0;
        versetzSumY = 0;
        Navigator.impl.exitPathAnimation();
        if ((Navigator.viewer.navigation instanceof OrbitBehavior)) {
            OrbitBehavior orbitBehavior = (OrbitBehavior) Navigator.viewer.navigation;
            startOrbitX = orbitBehavior.getCRSCenter().x;
            startOrbitY = -orbitBehavior.getCRSCenter().z;
            startOrbitZ = orbitBehavior.getCRSCenter().y;
        }
    }

    private int current_scaleSliderValue = -1;

    private int c = 0;

    private double versetzeX2 = 0;

    private double versetzeY2 = 0;

    private double xOld = this.getX();

    private double yOld = this.getY();

    private double versetzeX;

    private double versetzeY;

    private int yStart;

    private int xStart;

    private double versetzSumX = 0;

    private double versetzSumY = 0;

    private void checkZoomLevel() {
        int sliderValue = scaleSlider.getValue();
        if (sliderValue != current_scaleSliderValue) {
            current_scaleSliderValue = sliderValue;
            if (this.currentMapConfiguration instanceof WMSConfiguration) {
                WMSConfiguration wmsConfiguration = (WMSConfiguration) this.currentMapConfiguration;
                double scaleDenominator = wmsConfiguration.getWMS_minScaleDenominator() * Math.pow(2.0, sliderValue);
                double pixelSpanX = scaleDenominator * Navigator.STANDARDIZED_RENDERING_PIXEL_SIZE / metersPerUnit_CRS_X;
                double pixelSpanY = scaleDenominator * Navigator.STANDARDIZED_RENDERING_PIXEL_SIZE / metersPerUnit_CRS_Y;
                double new_tileSpanX = (wmsConfiguration.getWMS_TILE_WIDTH() * pixelSpanX);
                double new_tileSpanY = (wmsConfiguration.getWMS_TILE_HEIGHT() * pixelSpanY);
                tileMatrix = new TileMatrix();
                tileMatrix.setIdentifier("" + sliderValue);
                tileMatrix.setScaleDenominator(scaleDenominator);
                tileMatrix.setTopLeftPoint(new Point2d(WMS_TILEMATRIX_MIN_X, WMS_TILEMATRIX_MAX_Y));
                tileMatrix.setTileWidth(wmsConfiguration.getWMS_TILE_WIDTH());
                tileMatrix.setTileHeight(wmsConfiguration.getWMS_TILE_HEIGHT());
                tileMatrix.setMatrixWidth(Integer.MAX_VALUE);
                tileMatrix.setMatrixHeight(Integer.MAX_VALUE);
                tileSpanX = new_tileSpanX;
                tileSpanY = new_tileSpanY;
                mapScaleX = (double) (tileSpanX) / (double) (wmsConfiguration.getWMS_TILE_WIDTH());
                mapScaleY = (double) (tileSpanY) / (double) (wmsConfiguration.getWMS_TILE_HEIGHT());
            } else if (this.currentMapConfiguration instanceof WMTSConfiguration) {
                WMTSConfiguration wMTSConfiguration = (WMTSConfiguration) currentMapConfiguration;
                tileMatrix = wMTSConfiguration.getTileMatrixSet().getTileMatrixArray().get(sliderValue);
                double pixelSpanX = tileMatrix.getScaleDenominator() * Navigator.STANDARDIZED_RENDERING_PIXEL_SIZE / metersPerUnit_CRS_X;
                double pixelSpanY = tileMatrix.getScaleDenominator() * Navigator.STANDARDIZED_RENDERING_PIXEL_SIZE / metersPerUnit_CRS_Y;
                System.out.println("pixelSpan " + pixelSpanX + " " + pixelSpanY);
                double new_tileSpanX = (tileMatrix.getTileWidth() * pixelSpanX);
                double new_tileSpanY = (tileMatrix.getTileHeight() * pixelSpanY);
                tileSpanX = new_tileSpanX;
                tileSpanY = new_tileSpanY;
                System.out.println("tileSpan " + tileSpanX + " " + tileSpanY);
                mapScaleX = (double) (tileSpanX) / (double) (tileMatrix.getTileWidth());
                mapScaleY = (double) (tileSpanY) / (double) (tileMatrix.getTileHeight());
                System.out.println("mapScale " + mapScaleX + " " + mapScaleY);
            }
            updateArrow();
            computePositionInRaster();
            reloadMap();
            moveMap();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        reloadTilesThread.checkZoom();
    }

    public Transform3D getTransform(double posX, double posY) {
        Transform3D current_crs_transform = new Transform3D();
        current_crs_transform.setIdentity();
        Navigator.viewer.navigation.getCurrentCRSTransform3D(current_crs_transform);
        double[] matrix = new double[16];
        current_crs_transform.get(matrix);
        matrix[3] = posX;
        matrix[11] = -posY;
        current_crs_transform.set(matrix);
        return current_crs_transform;
    }

    double startX = 0;

    double startY = 0;

    double startOrbitX = 0;

    double startOrbitY = 0;

    double startOrbitZ = 0;

    double newPosY;

    double newPosX;

    public void mouseDragged(MouseEvent e) {
        double xNew = e.getX();
        double yNew = e.getY();
        double moveX = 0;
        double moveY = 0;
        if (xNew != xOld) moveX = xOld - xNew;
        if (yNew != yOld) moveY = yOld - yNew;
        if (((e.getX() > 0) & (e.getY() > 0)) & ((e.getX() < 201) & (e.getY() < 301))) {
            versetzeX = ((moveX) * mapScaleX * metersPerUnit_CRS_X);
            versetzeY = -((moveY) * mapScaleY * metersPerUnit_CRS_Y);
            versetzSumX += versetzeX;
            versetzSumY += versetzeY;
            double newPosX = startX + versetzSumX;
            double newPosY = startY + versetzSumY;
            this.newPosX = newPosX;
            this.newPosY = newPosY;
            Transform3D current_transform = getTransform(newPosX, newPosY);
            double[] matrix = new double[16];
            current_transform.get(matrix);
            if ((Navigator.viewer.navigation instanceof OrbitBehavior)) {
                OrbitBehavior orbitBehavior = (OrbitBehavior) Navigator.viewer.navigation;
                double newOrbitX = startOrbitX + versetzSumX;
                double newOrbitY = startOrbitY + versetzSumY;
                double newOrbitZ = startOrbitZ;
                orbitBehavior.setCRSCenter(new Vector3d(newOrbitX, newOrbitZ, -newOrbitY));
            } else {
                Navigator.viewer.navigation.setCRSTransform3D(current_transform);
            }
        }
        xOld = xNew;
        yOld = yNew;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
