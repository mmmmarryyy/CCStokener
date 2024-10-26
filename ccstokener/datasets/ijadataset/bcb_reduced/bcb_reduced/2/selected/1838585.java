package org.gdi3d.xnavi.panels.directory;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import javax.media.ding3d.BranchGroup;
import javax.media.ding3d.CapabilityNotSetException;
import javax.media.ding3d.Group;
import javax.media.ding3d.Transform3D;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.media.ding3d.vecmath.Point3d;
import javax.media.ding3d.vecmath.Point3f;
import javax.media.ding3d.vecmath.Vector3d;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.gdi3d.xnavi.listeners.CheckListener;
import org.gdi3d.xnavi.listeners.ClickCoordinateListener;
import org.gdi3d.xnavi.listeners.PathNavigationListener;
import org.gdi3d.xnavi.navigation.OrbitBehavior;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.navigator.Waypoint;
import org.gdi3d.xnavi.services.eqs.ElevationQueryService;
import org.gdi3d.xnavi.services.transform.CoordinateTransformService;
import org.gdi3d.xnavi.services.w3ds.Style;
import org.gdi3d.xnavi.services.w3ds.W3DS_Layer;
import org.gdi3d.xnavi.swing.JCheckTree;
import org.gdi3d.xnavi.swing.TreeRadioBoxLeafNode;
import org.gdi3d.xnavi.viewer.FeatureInfoMessageBox;
import org.gdi3d.xnavi.viewer.Java3DViewer;
import org.gdi3d.xnavi.viewer.OnScreenObjectGroup;
import org.gdi3d.xnavi.viewer.RouteWayPointLabel;

public class OLS_DirectoryServicePanel extends JSplitPane implements ActionListener, ClickCoordinateListener, CheckListener, MouseListener {

    Navigator navigator;

    private JButton PickStartButton;

    private double xCor, yCor, zCor;

    private Java3DViewer viewer;

    private JButton OKButton;

    private JButton ClearButton;

    private final int dividerLocation = 140;

    private Point3d crs_coord;

    private Point3d coordEpsg31467;

    private JSlider sliderDistance;

    private String response;

    private InputStream is;

    private JCheckTree layersTree = null;

    private TreeRadioBoxLeafNode[] layerLeafNodes = null;

    private TreeRadioBoxLeafNode shops;

    private TreeRadioBoxLeafNode publicTransport;

    private TreeRadioBoxLeafNode amenity;

    private TreeRadioBoxLeafNode tourism;

    JList list;

    JTable table;

    JLabel errorPanel = new JLabel();

    JScrollPane bottomPanel;

    JScrollPane poiList;

    int listCount = 0;

    public OLS_DirectoryServicePanel() {
        super(JSplitPane.VERTICAL_SPLIT);
        this.setDividerSize(2);
        this.setVisible(true);
        JPanel jpanel = makePanel();
        jpanel.setPreferredSize(new Dimension(100, 100));
        this.add(jpanel, JSplitPane.TOP);
        this.setDividerLocation(dividerLocation);
        bottomPanel = setPanelAttributes();
        this.add(bottomPanel, JSplitPane.BOTTOM);
    }

    private JScrollPane setPanelAttributes() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.white);
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEFT);
        panel.setLayout(layout);
        DefaultMutableTreeNode Top_Node = new DefaultMutableTreeNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_CATEGORY"));
        shops = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_SHOPS"));
        publicTransport = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PUBLIC_TRANSPORT"));
        amenity = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_AMENITY"));
        tourism = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TOURISM"));
        layerLeafNodes = new TreeRadioBoxLeafNode[7];
        layerLeafNodes[0] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_ALL_PUBLIC_TRANSPORT"));
        layerLeafNodes[0].setWorkingName("all_public_tran");
        layerLeafNodes[1] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BUS_STOP"));
        layerLeafNodes[1].setWorkingName("bus_stop");
        layerLeafNodes[2] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BUS_STATION"));
        layerLeafNodes[2].setWorkingName("bus_station");
        layerLeafNodes[3] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_RAILWAY_STATION"));
        layerLeafNodes[3].setWorkingName("railway_station");
        layerLeafNodes[4] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_SUBWAY_ENTRANCE"));
        layerLeafNodes[4].setWorkingName("subway_entrance");
        layerLeafNodes[5] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TRAM_STOP"));
        layerLeafNodes[5].setWorkingName("tram_stop");
        layerLeafNodes[6] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PARKING"));
        layerLeafNodes[6].setWorkingName("parking");
        for (int i = 0; i < layerLeafNodes.length; i++) {
            publicTransport.add(layerLeafNodes[i]);
        }
        layerLeafNodes = new TreeRadioBoxLeafNode[31];
        layerLeafNodes[0] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_ALL_AMENITY"));
        layerLeafNodes[0].setWorkingName("all_amenity");
        layerLeafNodes[1] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_ATM"));
        layerLeafNodes[1].setWorkingName("atm");
        layerLeafNodes[2] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BANK"));
        layerLeafNodes[2].setWorkingName("bank");
        layerLeafNodes[3] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BUREAU_DE_CHANGE"));
        layerLeafNodes[3].setWorkingName("bureau_de_change");
        layerLeafNodes[4] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BIERGARTEN"));
        layerLeafNodes[4].setWorkingName("biergarten");
        layerLeafNodes[5] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BUS_STATION"));
        layerLeafNodes[5].setWorkingName("bus_station");
        layerLeafNodes[6] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_CAFE"));
        layerLeafNodes[6].setWorkingName("cafe");
        layerLeafNodes[7] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_CINEMA"));
        layerLeafNodes[7].setWorkingName("cinema");
        layerLeafNodes[8] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_COLLEGE"));
        layerLeafNodes[8].setWorkingName("college");
        layerLeafNodes[9] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_COURTHOUSE"));
        layerLeafNodes[9].setWorkingName("courthouse");
        layerLeafNodes[10] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_FAST_FOOD"));
        layerLeafNodes[10].setWorkingName("fast_food");
        layerLeafNodes[11] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_FUEL"));
        layerLeafNodes[11].setWorkingName("fuel");
        layerLeafNodes[12] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_HOSPITAL"));
        layerLeafNodes[12].setWorkingName("hospital");
        layerLeafNodes[13] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_LIBRARY"));
        layerLeafNodes[13].setWorkingName("libray");
        layerLeafNodes[14] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_NIGHTCLUB"));
        layerLeafNodes[14].setWorkingName("nightclub");
        layerLeafNodes[15] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PARKING"));
        layerLeafNodes[15].setWorkingName("parking");
        layerLeafNodes[16] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PHARMACY"));
        layerLeafNodes[16].setWorkingName("pharmacy");
        layerLeafNodes[17] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PLACE_OF_WORSHIP"));
        layerLeafNodes[17].setWorkingName("place_of_worship");
        layerLeafNodes[18] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_POLICE"));
        layerLeafNodes[18].setWorkingName("poilice");
        layerLeafNodes[19] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_POST_BOX"));
        layerLeafNodes[19].setWorkingName("post_box");
        layerLeafNodes[20] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_POST_OFFICE"));
        layerLeafNodes[20].setWorkingName("post_office");
        layerLeafNodes[21] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PUB"));
        layerLeafNodes[21].setWorkingName("pub");
        layerLeafNodes[22] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PUBLIC_BUILDING"));
        layerLeafNodes[22].setWorkingName("public_builing");
        layerLeafNodes[23] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_RESTAURANT"));
        layerLeafNodes[23].setWorkingName("restaurant");
        layerLeafNodes[24] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_SCHOOL"));
        layerLeafNodes[24].setWorkingName("school");
        layerLeafNodes[25] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TAXI"));
        layerLeafNodes[25].setWorkingName("taxi");
        layerLeafNodes[26] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TELEPHONE"));
        layerLeafNodes[26].setWorkingName("telephone");
        layerLeafNodes[27] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_THEATRE"));
        layerLeafNodes[27].setWorkingName("theatre");
        layerLeafNodes[28] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TOILETS"));
        layerLeafNodes[28].setWorkingName("toilets");
        layerLeafNodes[29] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_TOWNHALL"));
        layerLeafNodes[29].setWorkingName("townhall");
        layerLeafNodes[30] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_UNIVERSITY"));
        layerLeafNodes[30].setWorkingName("university");
        for (int i = 0; i < layerLeafNodes.length; i++) {
            amenity.add(layerLeafNodes[i]);
        }
        layerLeafNodes = new TreeRadioBoxLeafNode[6];
        layerLeafNodes[0] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_ALL_SHOPS"));
        layerLeafNodes[0].setWorkingName("all_shops");
        layerLeafNodes[1] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_SUPERMARKET"));
        layerLeafNodes[1].setWorkingName("supermaket");
        layerLeafNodes[2] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_CONVINIENCE"));
        layerLeafNodes[2].setWorkingName("convinience");
        layerLeafNodes[3] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BAKERY"));
        layerLeafNodes[3].setWorkingName("bakery");
        layerLeafNodes[4] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_BUTCHER"));
        layerLeafNodes[4].setWorkingName("butcher");
        layerLeafNodes[5] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_KIOSK"));
        layerLeafNodes[5].setWorkingName("kiosk");
        for (int i = 0; i < layerLeafNodes.length; i++) {
            shops.add(layerLeafNodes[i]);
        }
        layerLeafNodes = new TreeRadioBoxLeafNode[6];
        layerLeafNodes[0] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_ALL_TOURISM"));
        layerLeafNodes[0].setWorkingName("all_tourism");
        layerLeafNodes[1] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_INFORMATION"));
        layerLeafNodes[1].setWorkingName("information");
        layerLeafNodes[2] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_HOTEL"));
        layerLeafNodes[2].setWorkingName("hotel");
        layerLeafNodes[3] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_MOTEL"));
        layerLeafNodes[3].setWorkingName("motel");
        layerLeafNodes[4] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_GUESTHOUSE"));
        layerLeafNodes[4].setWorkingName("guesthouse");
        layerLeafNodes[5] = new TreeRadioBoxLeafNode(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_HOSTEL"));
        layerLeafNodes[5].setWorkingName("hostel");
        for (int i = 0; i < layerLeafNodes.length; i++) {
            tourism.add(layerLeafNodes[i]);
        }
        Top_Node.add(amenity);
        Top_Node.add(publicTransport);
        Top_Node.add(shops);
        Top_Node.add(tourism);
        layersTree = new JCheckTree(Top_Node);
        layersTree.setCheckListener(this);
        layersTree.setState(amenity, true);
        panel.add(layersTree);
        JScrollPane bottomScrollPane = new JScrollPane();
        bottomScrollPane.setViewportView(panel);
        bottomScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        bottomScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bottomScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return bottomScrollPane;
    }

    private JPanel makePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        this.setBackground(Color.WHITE);
        panel.setLayout(null);
        panel.setPreferredSize(new java.awt.Dimension(180, 455));
        int y = 0;
        JLabel GetCoordinates = new JLabel("Pick Start Position");
        GetCoordinates.setBounds(5, y = y + 10, 100, 20);
        panel.add(GetCoordinates);
        PickStartButton = new JButton(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_PICK_BUTTON"));
        PickStartButton.setBounds(120, y, 40, 20);
        PickStartButton.setMargin(new Insets(1, 1, 1, 1));
        PickStartButton.setEnabled(true);
        panel.add(PickStartButton);
        PickStartButton.addActionListener(this);
        JLabel DistanceLabel = new JLabel("Distance (m)");
        DistanceLabel.setBounds(5, y = y + 20, 100, 20);
        panel.add(DistanceLabel);
        sliderDistance = new JSlider(0, 5000);
        sliderDistance.setOrientation(0);
        sliderDistance.setBounds(00, y += 20, 200, 60);
        sliderDistance.setToolTipText("");
        sliderDistance.setValue(1000);
        sliderDistance.setMajorTickSpacing(1000);
        sliderDistance.setMinorTickSpacing(200);
        sliderDistance.setPaintTicks(true);
        sliderDistance.setBackground(Color.WHITE);
        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(0), new JLabel("0"));
        labelTable.put(new Integer(1000), new JLabel("1000"));
        labelTable.put(new Integer(2000), new JLabel("2000"));
        labelTable.put(new Integer(3000), new JLabel("3000"));
        labelTable.put(new Integer(4000), new JLabel("4000"));
        labelTable.put(new Integer(5000), new JLabel("5000"));
        sliderDistance.setLabelTable(labelTable);
        sliderDistance.setPaintLabels(true);
        panel.add(sliderDistance);
        OKButton = new JButton(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_OK_BUTTON"));
        OKButton.setBounds(10, y += 60, 50, 20);
        OKButton.setMargin(new Insets(1, 1, 1, 1));
        OKButton.setEnabled(true);
        panel.add(OKButton);
        OKButton.addActionListener(this);
        ClearButton = new JButton(Navigator.i18n.getString("OSM_DIRECTORY_SERVICE_CLEAR_BUTTON"));
        ClearButton.setBounds(70, y, 50, 20);
        ClearButton.setMargin(new Insets(1, 1, 1, 1));
        ClearButton.setEnabled(true);
        panel.add(ClearButton);
        ClearButton.addActionListener(this);
        return panel;
    }

    public String getPOICategory() {
        String poiType = "public_tran";
        if (layersTree.getState(shops) == 1) {
            poiType = "shop";
        }
        if (layersTree.getState(publicTransport) == 1) {
            poiType = "public_tran";
        }
        if (layersTree.getState(amenity) == 1) {
            poiType = "amenity";
        }
        if (layersTree.getState(tourism) == 1) {
            poiType = "tourism";
        }
        return poiType;
    }

    public String getPOIsubcategory(String searchCategory) {
        String selectedPOI = "empty";
        if (searchCategory == "shop") {
            for (int i = 0; i < shops.getChildCount(); i++) if (layersTree.getState(shops.getChildAt(i)) == 1) {
                selectedPOI = ((TreeRadioBoxLeafNode) shops.getChildAt(i)).getWorkingName();
            }
            if (selectedPOI.equals("empty")) {
                selectedPOI = "all_shops";
            }
        }
        if (searchCategory == "amenity") {
            for (int i = 0; i < amenity.getChildCount(); i++) if (layersTree.getState(amenity.getChildAt(i)) == 1) {
                selectedPOI = ((TreeRadioBoxLeafNode) amenity.getChildAt(i)).getWorkingName();
            }
            if (selectedPOI.equals("empty")) {
                selectedPOI = "all_amenity";
            }
        }
        if (searchCategory == "public_tran") {
            for (int i = 0; i < publicTransport.getChildCount(); i++) if (layersTree.getState(publicTransport.getChildAt(i)) == 1) {
                selectedPOI = ((TreeRadioBoxLeafNode) publicTransport.getChildAt(i)).getWorkingName();
            }
            if (selectedPOI.equals("empty")) {
                selectedPOI = "all_public_tran";
            }
        }
        if (searchCategory == "tourism") {
            for (int i = 0; i < tourism.getChildCount(); i++) if (layersTree.getState(tourism.getChildAt(i)) == 1) {
                selectedPOI = ((TreeRadioBoxLeafNode) tourism.getChildAt(i)).getWorkingName();
            }
            if (selectedPOI.equals("empty")) {
                selectedPOI = "all_tourism";
            }
        }
        return selectedPOI;
    }

    public String getPOISearchType(String searchCategory) {
        String searchType = null;
        for (int i = 0; i < shops.getChildCount(); i++) {
            if (layersTree.getState(shops.getChildAt(i)) == 1) {
                searchType = ((TreeRadioBoxLeafNode) shops.getChildAt(i)).getWorkingName();
            }
        }
        for (int i = 0; i < amenity.getChildCount(); i++) {
            if (layersTree.getState(amenity.getChildAt(i)) == 1) {
                searchType = ((TreeRadioBoxLeafNode) amenity.getChildAt(i)).getWorkingName();
            }
        }
        for (int i = 0; i < publicTransport.getChildCount(); i++) {
            if (layersTree.getState(publicTransport.getChildAt(i)) == 1) {
                searchType = ((TreeRadioBoxLeafNode) publicTransport.getChildAt(i)).getWorkingName();
            }
        }
        for (int i = 0; i < tourism.getChildCount(); i++) {
            if (layersTree.getState(tourism.getChildAt(i)) == 1) {
                searchType = ((TreeRadioBoxLeafNode) tourism.getChildAt(i)).getWorkingName();
            }
        }
        return searchType;
    }

    private void getElevation(Vector<POI> pois) {
        ElevationQueryService demService = new ElevationQueryService(Navigator.getPreferences().getEQSServiceEndPoint());
        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);
            Point2D point2d = poi.getPoint();
            if (point2d != null) {
                double z = demService.getHeight(point2d.getX(), point2d.getY(), 0);
                if (z == -1.0) System.out.println("demService returned -1 at " + point2d.getX() + ", " + point2d.getY());
                poi.setPointElevation(z);
            } else {
                System.out.println("OLS_DirectoryServicePanel.getElevation ERROR: point2d == null at poi " + i);
            }
        }
    }

    private void getDistance(Vector<POI> pois) {
        for (int i = 0; i < pois.size(); i++) {
            pois.get(i).setDistance((int) distance(pois.get(i), coordEpsg31467));
        }
    }

    public void sort(Vector<POI> poiListe) {
        if (poiListe != null) {
            int n = poiListe.size();
            if (n > 0) {
                quicksort(0, n - 1, poiListe);
            }
        }
    }

    private void quicksort(int lo, int hi, Vector<POI> poiListe) {
        int i = lo;
        int j = hi;
        int x = poiListe.get((lo + hi) / 2).getDistance();
        while (i <= j) {
            while (poiListe.get(i).getDistance() < x) i++;
            while (poiListe.get(j).getDistance() > x) {
                j--;
            }
            if (i <= j) {
                exchange(i, j, poiListe);
                i++;
                j--;
            }
        }
        if (lo < j) quicksort(lo, j, poiListe);
        if (i < hi) quicksort(i, hi, poiListe);
    }

    private void exchange(int i, int j, Vector<POI> poiListe) {
        POI p = new POI();
        p = poiListe.get(i);
        poiListe.set(i, poiListe.get(j));
        poiListe.set(j, p);
    }

    private String createRequest(String distance, String searchCategory) {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " + " \n " + " <xls:XLS xmlns:xls=\"http://www.opengis.net/xls\" xmlns:sch=\"http://www.ascc.net/xml/schematron\"  " + " \n " + " xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"  " + " \n " + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  " + " \n " + " xsi:schemaLocation=\"http://www.opengis.net/xls http://schemas.opengis.net/ols/1.1.0/DirectoryService.xsd\" version=\"1.1\"> " + " \n " + " 	<xls:RequestHeader srsName=\"EPSG:" + Navigator.getEpsg_code() + "\"/> " + " \n " + " 	<xls:Request methodName=\"DirectoryRequest\" requestID=\"123456789\" version=\"1.1\"> " + " \n " + " 		<xls:DirectoryRequest distanceUnit=\"M\" sortCriteria=\"Distance\"> " + " \n " + " 			<xls:POILocation> " + " \n " + " 				<xls:WithinDistance> " + " \n " + " 					<xls:Position> " + " \n " + " 						<gml:Point> " + " \n " + " 							<gml:pos>YKOORDINATE XKOORDINATE</gml:pos> " + " \n " + " 						</gml:Point> " + " \n " + " 					</xls:Position> " + " \n " + " 					<xls:MinimumDistance value=\"0\" uom=\"M\"/> " + " 					<xls:MaximumDistance value=\"DISTANCE\" uom=\"M\"/> " + " \n " + " 				</xls:WithinDistance> " + " \n " + " 			</xls:POILocation> " + " \n " + " 			<xls:POIProperties directoryType=\"OSM\"> " + " \n " + " 				<xls:POIProperty name=\"Keyword\" value=\"POITYPE\"/> " + " \n " + " 			</xls:POIProperties> " + " \n " + " 		</xls:DirectoryRequest> " + " \n " + " 	</xls:Request> " + " \n " + " </xls:XLS> " + " \n ";
        request = request.replace("POITYPE", searchCategory);
        request = request.replace("XKOORDINATE", String.valueOf(crs_coord.x));
        request = request.replace("YKOORDINATE", String.valueOf(crs_coord.y));
        request = request.replace("DISTANCE", distance);
        System.out.println("request " + request);
        return request;
    }

    private InputSource sendRequest(String f) {
        InputSource iSource = null;
        try {
            String serviceEndPoint = Navigator.getPreferences().getOpenLSDirectoryServiceEndPoint();
            if (Navigator.isVerbose()) {
                System.out.println("sending request to " + serviceEndPoint);
                System.out.println(f);
            }
            URL u = new URL(serviceEndPoint);
            HttpURLConnection urlc = (HttpURLConnection) u.openConnection();
            urlc.setReadTimeout(Navigator.TIME_OUT);
            urlc.setAllowUserInteraction(false);
            urlc.setRequestMethod("POST");
            urlc.setRequestProperty("Content-Type", "application/xml");
            urlc.setDoOutput(true);
            urlc.setDoInput(true);
            urlc.setUseCaches(false);
            PrintWriter xmlOut = null;
            xmlOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(urlc.getOutputStream())));
            xmlOut = new java.io.PrintWriter(urlc.getOutputStream());
            xmlOut.write(f);
            xmlOut.flush();
            xmlOut.close();
            is = urlc.getInputStream();
            response = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = rd.readLine()) != null) {
                response = response + line;
            }
            is.close();
            System.out.println(response);
            StringReader sReader = new StringReader(response);
            iSource = new InputSource(sReader);
        } catch (Exception e) {
            remove(bottomPanel);
            bottomPanel = errorPanel();
            setDividerLocation(dividerLocation);
            add(bottomPanel);
            e.printStackTrace();
        }
        return iSource;
    }

    private JScrollPane errorPanel() {
        JLabel jLabel = new JLabel(" OSM - Server down due to maintenance!!");
        JScrollPane jScrollPane = new JScrollPane(jLabel);
        return jScrollPane;
    }

    private Map<String, BufferedImage> loadedImages;

    private void filterPOIs(Vector<POI> pois, String searchPoiType) {
        Vector<POI> removePOIs = new Vector<POI>();
        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);
            String type = poi.getType();
            if (!type.equals(searchPoiType)) {
                removePOIs.add(poi);
            }
        }
        for (int i = 0; i < removePOIs.size(); i++) {
            pois.remove(removePOIs.get(i));
            System.out.println("filtered out " + removePOIs.get(i).getType() + " " + removePOIs.get(i).getName());
        }
    }

    private void displayPOIs(Vector<POI> pois) {
        if (loadedImages == null) {
            loadedImages = new HashMap<String, BufferedImage>();
        }
        BufferedImage defaultImage = Navigator.loadImageFromResources("resources/IconWaypoint001.png", 22, 22);
        Point3d j3d_startPoint = new Point3d(xCor, yCor + 20, zCor);
        Waypoint startWaypoint = new Waypoint(new Vector3d(j3d_startPoint), Double.POSITIVE_INFINITY, defaultImage, Waypoint.ALIGN_CENTER, Waypoint.VALIGN_MIDDLE, "Start", "id_directory_start");
        navigator.addWaypoint(startWaypoint);
        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);
            String type = poi.getType();
            Point3d crs_poiPoint = poi.getPoint3d();
            if (crs_poiPoint != null) {
                Point3d j3d_poiPoint = null;
                if (Navigator.globe) {
                    Transform3D latlonTrans = CoordinateTransformService.getLatLonTransform(crs_poiPoint);
                    Vector3d tmp = new Vector3d();
                    latlonTrans.get(tmp);
                    j3d_poiPoint = new Point3d(tmp);
                    tmp.normalize();
                    tmp.scale(20);
                    j3d_poiPoint.add(tmp);
                } else {
                    j3d_poiPoint = new Point3d((float) (crs_poiPoint.x), (float) (crs_poiPoint.z + 20), (float) (-crs_poiPoint.y));
                }
                BufferedImage bufferedImage = this.loadedImages.get(type);
                if (bufferedImage == null) {
                    String iconFile = this.getIconImageFile(type);
                    bufferedImage = loadImageFromResource(iconFile, 26, 26);
                    if (bufferedImage == null) {
                        bufferedImage = defaultImage;
                    } else loadedImages.put(type, bufferedImage);
                }
                String name = null;
                if (poi.getName().length() > 0) {
                    name = poi.getName();
                }
                Waypoint waypoint = new Waypoint(new Vector3d(j3d_poiPoint), Double.POSITIVE_INFINITY, bufferedImage, Waypoint.ALIGN_CENTER, Waypoint.VALIGN_MIDDLE, name, "id_poi_" + name);
                navigator.addWaypoint(waypoint);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object quelle = e.getSource();
        if (quelle == this.OKButton) {
            GetPOIsThread thread = new GetPOIsThread();
            thread.start();
        } else if (quelle == this.PickStartButton) {
            PickStartButton.setSelected(!PickStartButton.isSelected());
            Navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            Navigator.viewer.setClickCoordinateListener(this);
        } else if (quelle == this.ClearButton) {
            viewer.clearUserObjects();
            this.remove(bottomPanel);
            bottomPanel = setPanelAttributes();
            this.add(bottomPanel, JSplitPane.BOTTOM);
            this.setDividerLocation(dividerLocation);
        }
    }

    public void setViewer(Java3DViewer viewer) {
        this.viewer = viewer;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public void coordinateClicked(Point3d j3d_position) {
        xCor = j3d_position.x;
        yCor = j3d_position.y;
        zCor = j3d_position.z;
        if (Navigator.globe) {
            Vector3d tmp = CoordinateTransformService.getCRSPosition(new Vector3d(j3d_position));
            crs_coord = new Point3d(tmp);
        } else {
            crs_coord = new Point3d(xCor, -zCor, yCor);
        }
        coordEpsg31467 = new Point3d(crs_coord);
        Navigator.coordinateTransformService.transform("epsg:" + Navigator.getEpsg_code(), "epsg:4326", crs_coord);
        PickStartButton.setSelected(false);
        viewer.clearUserObjects();
        viewer.clearUserObjects();
        BranchGroup bg = new BranchGroup();
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        bg.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        bg.setCapability(BranchGroup.ALLOW_DETACH);
        Group pathLabels = new Group();
        BufferedImage defaultImage = Navigator.loadImageFromResources("resources/IconWaypoint001.png", 22, 22);
        Waypoint startWaypoint = new Waypoint(new Vector3d(j3d_position), Double.POSITIVE_INFINITY, defaultImage, Waypoint.ALIGN_CENTER, Waypoint.VALIGN_MIDDLE, "Start", "id_directory_start");
        navigator.addWaypoint(startWaypoint);
        bg.addChild(pathLabels);
        viewer.addUserObject(bg);
        Navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        Navigator.viewer.setClickCoordinateListener(null);
    }

    public void checkChange(TreeNode node, Integer state) {
        if (state == JCheckTree.STATE_SELECTED) {
            TreeNode parent = node.getParent();
            if (parent instanceof TreeRadioBoxLeafNode) {
                System.out.println("select " + parent);
                this.layersTree.setState(parent, true);
            }
        }
    }

    private class PoiListElement {

        private POI poi;

        public PoiListElement(POI poi) {
            this.poi = poi;
        }

        public POI getPOI() {
            return poi;
        }

        public String toString() {
            String string = "null";
            if (poi != null) {
                String name = poi.getName();
                if (name != null && !name.equals("null") && !name.equals("")) {
                    string = "" + name;
                } else {
                    String type = poi.getType();
                    if (type != null && !type.equals("null") && !type.equals("null")) {
                        string = "" + type;
                    }
                }
                int distance = poi.getDistance();
                string += " " + distance + " m";
            }
            return string;
        }
    }

    Vector<Integer> POI_List_Index;

    public JScrollPane listPOIs(Vector<POI> pois) {
        DefaultListModel model = new DefaultListModel();
        POI_List_Index = new Vector<Integer>();
        for (int i = 0; i < pois.size(); i++) {
            POI poi = pois.get(i);
            PoiListElement poile = new PoiListElement(poi);
            model.addElement(poile);
            POI_List_Index.add(i);
            listCount = listCount + 1;
        }
        list = new JList(model);
        list.addMouseListener(this);
        JScrollPane jScrollPane = new JScrollPane(list);
        jScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        return jScrollPane;
    }

    private class GetPOIsThread extends Thread {

        public void run() {
            try {
                String distance = String.valueOf(sliderDistance.getValue());
                String searchCategory = getPOICategory();
                if (Navigator.isVerbose()) System.out.println("OLS_DirectoryServicePanel searchCategory " + searchCategory);
                String searchPoiType = getPOISearchType(searchCategory);
                String request = createRequest(distance, searchCategory);
                InputSource iSource = sendRequest(request);
                XMLParser xmlP = new XMLParser(iSource);
                Vector<POI> pois = xmlP.getPois();
                if (searchPoiType != null) {
                    filterPOIs(pois, searchPoiType);
                }
                getElevation(pois);
                getDistance(pois);
                sort(pois);
                displayPOIs(pois);
                remove(bottomPanel);
                bottomPanel = listPOIs(pois);
                add(bottomPanel, JSplitPane.BOTTOM);
                setDividerLocation(dividerLocation);
            } catch (CapabilityNotSetException c) {
                c.printStackTrace();
            }
        }
    }

    public double distance(POI poi, Point3d point3d) {
        double value = 0.0;
        Point3d p3d = poi.getPoint3d();
        if (p3d != null) {
            double x = (p3d.x - point3d.x) * (p3d.x - point3d.x);
            double y = (p3d.y - point3d.y) * (p3d.y - point3d.y);
            value = Math.sqrt(x + y);
        } else {
            System.out.println("OLS_DirectoryServicePanel.distance ERROR: p3d == null");
        }
        return value;
    }

    private String getIconImageFile(String type) {
        String string = null;
        if (type.equalsIgnoreCase("start")) {
            string = "startPunkt.jpg";
        } else {
            string = type.trim() + ".jpg";
        }
        return string;
    }

    public BufferedImage loadImageFromResource(String imageFile, int width, int height) {
        BufferedImage newImage = null;
        System.out.println("loadImageFromResource " + imageFile);
        String resourceName = "resources/osm_symbols/" + imageFile;
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL url = classLoader.getResource(resourceName);
            ImageIcon img = new ImageIcon(url);
            int status = MediaTracker.LOADING;
            while (status == MediaTracker.LOADING) {
                status = img.getImageLoadStatus();
            }
            Image orgImage = img.getImage();
            newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D graphics2D = newImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(orgImage, 0, 0, width, height, null);
        } catch (Exception e) {
            System.out.println("unable to load " + resourceName);
        }
        return newImage;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            this.setDividerLocation(dividerLocation + 30);
            PoiListElement poile = (PoiListElement) list.getSelectedValue();
            POI poi = poile.getPOI();
            Point3d poiPoint3d = poi.getPoint3d();
            if (poiPoint3d != null) {
                Point3d p = new Point3d(poiPoint3d);
                Vector3d v = new Vector3d(0, 1, 0);
                Point3d lookTo = new Point3d(p.x, p.z, -p.y);
                Point3d lookFrom = new Point3d(lookTo);
                double fieldOfView = viewer.getFieldOfView();
                double distance = 200.0;
                Vector3d look = new Vector3d(0.0, 2.0, -1.0);
                look.normalize();
                look.scale((float) distance);
                lookFrom.x -= look.x;
                lookFrom.y += look.y;
                lookFrom.z -= look.z;
                v = new Vector3d(0, 1, 0);
                viewer.lookAt(lookFrom, lookTo, v, fieldOfView);
            } else {
                System.out.println("mouseClicked poiPoint3d == null");
            }
        }
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
    }
}
