package org.gdi3d.xnavi.panels.bomb;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import javax.media.ding3d.BranchGroup;
import javax.swing.*;
import javax.media.ding3d.vecmath.Point3d;
import javax.media.ding3d.vecmath.Vector3d;
import javax.media.ding3d.loaders.Scene;
import org.gdi3d.xnavi.listeners.ClickCoordinateListener;
import org.gdi3d.xnavi.listeners.ProgressListener;
import org.gdi3d.xnavi.listeners.PathNavigationListener;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.viewer.Java3DViewer;

public class BombfindingPanel extends JSplitPane implements ActionListener, ProgressListener, MouseListener, ClickCoordinateListener {

    final int NAVIGATION_PANEL_HEIGHT = 68;

    final int SLIDER_INT_SIZE = 200;

    private boolean mousePressed = false;

    PathNavigationListener routeNavigationListener = null;

    JButton PickStartButton;

    String innerradius;

    String outerradius;

    String x_pos, y_pos, z_pos;

    double xmin, ymin, xmax, ymax;

    double xCor, yCor, zCor;

    Java3DViewer viewer;

    StringBuffer stringBuffer = new StringBuffer();

    JTextField InnerRadiusTextfield;

    JTextField ExplosiveForce;

    JTextField XCoordinatesTextfield;

    JTextField YCoordinatesTextfield;

    JTextField ZCoordinatesTextfield;

    JButton OKButton;

    JButton ClearButton;

    JSlider progressSlider;

    Integer dividerLocation = 250;

    String response = "";

    URL url;

    String request;

    public BombfindingPanel() {
        super(JSplitPane.VERTICAL_SPLIT);
        this.setName("BombThread SplitPane");
        this.setDividerSize(2);
        this.setVisible(true);
        JPanel jpanel = makePane();
        jpanel.setPreferredSize(new Dimension(205, 1000));
        this.add(jpanel, JSplitPane.TOP);
        this.setDividerLocation(dividerLocation);
        JPanel navigationPanel = makeNavigationPanel();
        this.add(navigationPanel, JSplitPane.BOTTOM);
    }

    public void request() {
        request = " <wps:Execute service=\"WPS\" version=\"0.4.0\" store=\"true\" status=\"false\"  " + " \n " + "     xmlns:wps=\"http://www.opengeospatial.net/wps\" xmlns:ows=\"http://www.opengeospatial.net/ows\"   " + " \n " + "     xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:gml=\"http://www.opengis.net/gml\" " + " \n " + "     xmlns:onsa=\"http://localhost:8080/osna\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + " \n " + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + " \n " + "     xsi:schemaLocation=\"http://www.opengeospatial.net/wps..\\wpsExecute.xsd\"> " + " \n " + "     <ows:Identifier>BombThreatScenario3D</ows:Identifier> " + " \n " + "     <wps:DataInputs> " + " \n " + "         <wps:Input> " + " \n " + "             <ows:Identifier>BombLocation</ows:Identifier> " + " \n " + "             <ows:Title>BombLocation</ows:Title> " + " \n " + "             <wps:ComplexValue format=\"text/xml\" encoding=\"UTF-8\" " + " \n " + "                 schema=\"http://schemas.opengis.net/gml/3.0.0/base/gml.xsd\"> " + " \n " + "                 <wfs:FeatureCollection " + " \n " + "                     xsi:schemaLocation=\"http://localhost:8080/osna http://localhost:8080/geoserver/wfs/DescribeFeatureType?typeName=onsa:bomb_finding_place http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd\"> " + " \n " + "                     <gml:boundedBy> " + " \n " + "                         <gml:Box srsName=\"http://www.opengis.net/gml/srs/epsg.xml#" + Navigator.getEpsg_code() + "\">  " + " \n " + "                             <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">x_koordinate,y_koordinate,z_koordinate</gml:coordinates> " + " \n " + "                         </gml:Box> " + " \n " + "                     </gml:boundedBy> " + " \n " + "                     <gml:featureMember> " + " \n " + "                         <onsa:bomb_finding_place fid=\"bomb_finding_place.1\"> " + " \n " + "                             <onsa:the_geom> " + " \n " + "                                 <gml:MultiPoint " + " \n " + "                                     srsName=\"http://www.opengis.net/gml/srs/epsg.xml#" + Navigator.getEpsg_code() + "\"> " + " \n " + "                                     <gml:pointMember> " + " \n " + "                                         <gml:Point> " + " \n " + "                                             <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">x_koordinate,y_koordinate,z_koordinate</gml:coordinates> " + " \n " + "                                         </gml:Point> " + " \n " + "                                     </gml:pointMember> " + " \n " + "                                 </gml:MultiPoint> " + " \n " + "                             </onsa:the_geom> " + " \n " + "                         </onsa:bomb_finding_place> " + " \n " + "                     </gml:featureMember> " + " \n " + "                 </wfs:FeatureCollection> " + " \n " + "             </wps:ComplexValue> " + " \n " + "         </wps:Input> " + " \n " + "         <wps:Input> " + " \n " + "             <ows:Identifier>BombRadius</ows:Identifier> " + " \n " + "             <ows:Title>BombRadius</ows:Title> " + " \n " + "             <wps:LiteralValue dataType=\"urn:ogc:def:dataType:OGC:1.0:Integer\">innerradius</wps:LiteralValue> " + " \n " + "         </wps:Input> " + " \n " + "         <wps:Input> " + " \n " + "             <ows:Identifier>BombRadius2</ows:Identifier> " + " \n " + "             <ows:Title>BombRadiu2s</ows:Title> " + " \n " + "             <wps:LiteralValue dataType=\"urn:ogc:def:dataType:OGC:1.0:Integer\">outerradius</wps:LiteralValue> " + " \n " + "         </wps:Input> " + " \n " + "     </wps:DataInputs> " + " \n " + "     <wps:OutputDefinitions> " + " \n " + "         <wps:Output> " + " \n " + "             <ows:Identifier>Spheres</ows:Identifier> " + " \n " + "             <ows:Title>Spheres</ows:Title> " + " \n " + "             <ows:Abstract>Spheres</ows:Abstract> " + " \n " + "             <!-- The data type of the process output --> " + " \n " + "             <wps:ComplexOutput defaultSchema=\"http://www.web3d.org/specifications/x3d-3.0.xsd\" " + " \n " + "             > </wps:ComplexOutput> " + " \n " + "         </wps:Output> " + " \n " + "     </wps:OutputDefinitions> " + " \n " + " </wps:Execute> " + " \n ";
    }

    public void setRouteNavigationListener(PathNavigationListener listener) {
        this.routeNavigationListener = listener;
    }

    public void createWPSrequest() {
        innerradius = String.valueOf((Integer.valueOf(outerradius) / 2));
        innerradius = String.valueOf((Integer.valueOf(outerradius)));
        {
            request = request.replace("x_koordinate", x_pos);
            request = request.replace("y_koordinate", y_pos);
            request = request.replace("z_koordinate", z_pos);
            request = request.replace("innerradius", innerradius);
            request = request.replace("outerradius", outerradius);
            request = request.replace("xmin", String.valueOf(xmin));
            request = request.replace("ymin", String.valueOf(ymin));
            request = request.replace("xmax", String.valueOf(xmax));
            request = request.replace("ymax", String.valueOf(ymax));
        }
    }

    public void receiveXML() {
    }

    public void loadVRML() {
    }

    public void calculateBoundingBox() {
        xmin = xCor - Double.valueOf(outerradius) / 2;
        ymin = yCor - Double.valueOf(outerradius) / 2;
        xmax = xCor + Double.valueOf(outerradius) / 2;
        ymax = yCor + Double.valueOf(outerradius) / 2;
    }

    private JPanel makeNavigationPanel() {
        ClassLoader cl = this.getClass().getClassLoader();
        JPanel panel = new JPanel();
        panel.setMinimumSize(new java.awt.Dimension(205, NAVIGATION_PANEL_HEIGHT));
        FlowLayout layout = new FlowLayout();
        layout.setHgap(2);
        panel.setLayout(layout);
        panel.setBorder(null);
        OKButton = new JButton("OK");
        OKButton.setEnabled(true);
        panel.add(OKButton);
        OKButton.addActionListener(this);
        ClearButton = new JButton("Clear");
        ClearButton.setEnabled(true);
        panel.add(ClearButton);
        ClearButton.addActionListener(this);
        return panel;
    }

    private JPanel makePane() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        this.setBackground(Color.WHITE);
        panel.setLayout(null);
        panel.setPreferredSize(new java.awt.Dimension(180, 455));
        int y = 0;
        JLabel OuterRadiusLabel = new JLabel("Outer Radius");
        OuterRadiusLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(OuterRadiusLabel);
        ExplosiveForce = new JTextField("200");
        ExplosiveForce.setBounds(100, y, 40, 20);
        panel.add(ExplosiveForce);
        JLabel OuterRadiusUnit = new JLabel("m");
        OuterRadiusUnit.setBounds(150, y, 40, 20);
        panel.add(OuterRadiusUnit);
        JLabel GetCoordinates = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 9px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">" + "Get Coordinates" + "</span></body></html>");
        GetCoordinates.setBounds(5, y = y + 60, 100, 20);
        panel.add(GetCoordinates);
        PickStartButton = new JButton(Navigator.i18n.getString("ROUTING_PANEL_PICK"));
        PickStartButton.setBounds(120, y = y + 20, 40, 20);
        PickStartButton.setMargin(new Insets(1, 1, 1, 1));
        PickStartButton.setEnabled(true);
        panel.add(PickStartButton);
        PickStartButton.addActionListener(this);
        JLabel XCoordinateLabel = new JLabel("x coordinate");
        XCoordinateLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(XCoordinateLabel);
        XCoordinatesTextfield = new JTextField();
        XCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(XCoordinatesTextfield);
        JLabel YCoordinateLabel = new JLabel("y coordinate");
        YCoordinateLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(YCoordinateLabel);
        YCoordinatesTextfield = new JTextField();
        YCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(YCoordinatesTextfield);
        ZCoordinatesTextfield = new JTextField();
        ZCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(ZCoordinatesTextfield);
        JLabel ZCoordinateLabel = new JLabel("z coordinate");
        ZCoordinateLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(ZCoordinateLabel);
        ZCoordinatesTextfield = new JTextField();
        ZCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(ZCoordinatesTextfield);
        return panel;
    }

    public void sendFile(String f) {
        try {
            URL u = new URL("http://129.206.229.148/deegree/services");
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
            InputStream is = urlc.getInputStream();
            response = "";
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = rd.readLine()) != null) {
                response = response + line;
            }
            int begin = (response.indexOf("ows:reference=")) + 15;
            int end = (response.indexOf(".wrl")) + 4;
            String wrl = "";
            if (begin > 0) {
                wrl = response.substring(begin, end);
            }
            try {
                url = new URL(wrl);
            } catch (MalformedURLException e) {
                System.err.println(e);
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object quelle = e.getSource();
        if (quelle == this.OKButton) {
            outerradius = ExplosiveForce.getText();
            request();
            createWPSrequest();
            sendFile(request);
            Scene scene = null;
            scene = viewer.loadSceneFromURL(url);
            if (scene != null) {
                BranchGroup bgroup = scene.getSceneGroup();
                if (bgroup != null) {
                    bgroup.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
                    CopyOfVRMLGrundrisse_To_Shapefile c2 = new CopyOfVRMLGrundrisse_To_Shapefile();
                    com.vividsolutions.jts.geom.Geometry avoidArea = c2.create_file(bgroup, 0.0, 0.0, 0.5);
                    viewer.importScene(scene, false);
                    Navigator.addAvoidArea(avoidArea);
                }
            }
        } else if (quelle == this.PickStartButton) {
            PickStartButton.setSelected(!PickStartButton.isSelected());
            Navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            Navigator.viewer.setClickCoordinateListener(this);
            outerradius = ExplosiveForce.getText();
        } else if (quelle == this.ClearButton) {
            viewer.clearUserObjects();
            Navigator.clearAvoidAreas();
        }
    }

    public void setViewer(Java3DViewer viewer) {
        this.viewer = viewer;
    }

    public void setProgress(float value) {
        float progress = value;
        if (progress > 1.0f) progress = 1.0f;
        if (progress < 0.0f) progress = 0.0f;
        int int_progress = (int) (progress * (float) SLIDER_INT_SIZE);
        if (!mousePressed) progressSlider.setValue(int_progress);
    }

    public void mouseReleased(MouseEvent e) {
        float progress = (float) progressSlider.getValue() / (float) SLIDER_INT_SIZE;
        if (routeNavigationListener != null) {
            routeNavigationListener.setPathProgress(progress);
        }
        mousePressed = false;
    }

    public void mouseClicked(MouseEvent e) {
        mousePressed = false;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mousePressed = false;
    }

    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    public void coordinateClicked(Point3d GKposition) {
        xCor = GKposition.x;
        yCor = GKposition.y;
        zCor = GKposition.z;
        x_pos = String.valueOf(xCor);
        y_pos = String.valueOf(yCor);
        z_pos = String.valueOf(zCor);
        String x_pos_ausgabe = x_pos.substring(0, 10);
        String y_pos_ausgabe = y_pos.substring(0, 10);
        String z_pos_ausgabe = z_pos.substring(0, 6);
        XCoordinatesTextfield.setText(x_pos_ausgabe);
        YCoordinatesTextfield.setText(y_pos_ausgabe);
        ZCoordinatesTextfield.setText(z_pos_ausgabe);
        PickStartButton.setSelected(false);
        Navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        Navigator.viewer.setClickCoordinateListener(null);
    }
}
