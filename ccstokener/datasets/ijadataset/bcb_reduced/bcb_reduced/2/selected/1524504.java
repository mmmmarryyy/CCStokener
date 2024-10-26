package org.gdi3d.xnavi.panels.emission;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.media.ding3d.vecmath.Point3d;
import org.gdi3d.xnavi.listeners.ClickCoordinateListener;
import org.gdi3d.xnavi.listeners.ProgressListener;
import org.gdi3d.xnavi.listeners.PathNavigationListener;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.viewer.Java3DViewer;

public class EmissionScenario extends JSplitPane implements ActionListener, ProgressListener, ClickCoordinateListener {

    final int NAVIGATION_PANEL_HEIGHT = 68;

    final int SLIDER_INT_SIZE = 200;

    private boolean mousePressed = false;

    Navigator navigator;

    PathNavigationListener routeNavigationListener = null;

    JButton PickStartButton;

    String x_pos = "3476600.9189221356";

    String y_pos = "5474889.122483674";

    String z_pos = "105.09172339489092";

    double xCor, yCor, zCor;

    Java3DViewer viewer;

    StringBuffer stringBuffer = new StringBuffer();

    JTextField XCoordinatesTextfield;

    JTextField YCoordinatesTextfield;

    JTextField ZCoordinatesTextfield;

    JTextField WindDirectionTextfield;

    JTextField WindStrengthTextfield;

    JButton OKButton;

    JButton ClearButton;

    JButton startButton;

    JSlider progressSlider;

    Integer dividerLocation = 200;

    String response = "";

    URL url;

    String request;

    String windrichtung = null;

    public EmissionScenario() {
        super(JSplitPane.VERTICAL_SPLIT);
        this.setName("BombThread SplitPane");
        this.setDividerSize(2);
        this.setVisible(true);
        JPanel jpanel = makePanel();
        jpanel.setPreferredSize(new Dimension(205, 1000));
        this.add(jpanel, JSplitPane.TOP);
        this.setDividerLocation(dividerLocation);
        JPanel emssionScenatioPanel = makeEmssionScenarioPanel();
        this.add(emssionScenatioPanel, JSplitPane.BOTTOM);
    }

    public void createWPSRequest() {
        request = "  <wps:Execute service=\"WPS\" version=\"0.4.0\" store=\"true\" status=\"false\"   " + " \n " + "       xmlns:wps=\"http://www.opengeospatial.net/wps\" xmlns:ows=\"http://www.opengeospatial.net/ows\"   " + " \n " + "       xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:gml=\"http://www.opengis.net/gml\"   " + " \n " + "       xmlns:onsa=\"http://localhost:8080/osna\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"   " + " \n " + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"   " + " \n " + "       xsi:schemaLocation=\"http://www.opengeospatial.net/wps..\\wpsExecute.xsd\">   " + " \n " + "       <ows:Identifier>ToxicGasScenario3D</ows:Identifier>   " + " \n " + "       <wps:DataInputs>   " + " \n " + "           <wps:Input>   " + " \n " + "               <ows:Identifier>GasLeakageLocation</ows:Identifier>   " + " \n " + "               <ows:Title>GasLeakageLocation</ows:Title>   " + " \n " + "               <wps:ComplexValue format=\"text/xml\" encoding=\"UTF-8\"   " + " \n " + "                   schema=\"http://schemas.opengis.net/gml/3.0.0/base/gml.xsd\">   " + " \n " + "                   <wfs:FeatureCollection   " + " \n " + "                       xsi:schemaLocation=\"http://localhost:8080/osna http://localhost:8080/geoserver/wfs/DescribeFeatureType?typeName=onsa:bomb_finding_place http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-basic.xsd\">   " + " \n " + "                       <gml:boundedBy>   " + " \n " + "                           <gml:Box srsName=\"http://www.opengis.net/gml/srs/epsg.xml#" + Navigator.getEpsg_code() + "\">   " + " \n " + "                               <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">x_koordinate,y_koordinate,z_koordinate</gml:coordinates>   " + " \n " + "                           </gml:Box>   " + " \n " + "                       </gml:boundedBy>   " + " \n " + "                       <gml:featureMember>   " + " \n " + "                           <onsa:bomb_finding_place fid=\"bomb_finding_place.1\">   " + " \n " + "                               <onsa:the_geom>   " + " \n " + "                                   <gml:MultiPoint   " + " \n " + "                                       srsName=\"http://www.opengis.net/gml/srs/epsg.xml#31467\">   " + " \n " + "                                       <gml:pointMember>   " + " \n " + "                                           <gml:Point>   " + " \n " + "                                               <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">x_koordinate,y_koordinate,z_koordinate</gml:coordinates>   " + " \n " + "                                           </gml:Point>   " + " \n " + "                                       </gml:pointMember>   " + " \n " + "                                   </gml:MultiPoint>   " + " \n " + "                               </onsa:the_geom>   " + " \n " + "                           </onsa:bomb_finding_place>   " + " \n " + "                       </gml:featureMember>   " + " \n " + "                   </wfs:FeatureCollection>   " + " \n " + "               </wps:ComplexValue>   " + " \n " + "           </wps:Input>  " + " \n " + "           </wps:DataInputs>   " + " \n " + "         <wps:OutputDefinitions>   " + " \n " + "<wps:Output>   " + " \n " + "               <ows:Identifier>GasCloud</ows:Identifier>   " + " \n " + "               <ows:Title>GasCloud</ows:Title>   " + " \n " + "               <ows:Abstract>Sphere</ows:Abstract>   " + " \n " + "               <!-- The data type of the process output -->   " + " \n " + "               <wps:ComplexOutput defaultSchema=\"http://www.web3d.org/specifications/x3d-3.0.xsd\"> </wps:ComplexOutput>   " + " \n " + "</wps:Output>" + "         " + "  <wps:Output>   " + " \n " + "               <ows:Identifier>Windspeed</ows:Identifier>   " + " \n " + "               <ows:Title>Windspeed</ows:Title>   " + " \n " + "               <!-- The data type of the process output -->   " + " \n " + "               <wps:LiteralOutput defaultSchema=\"http://www.web3d.org/specifications/x3d-3.0.xsd\"> </wps:LiteralOutput>   " + " \n " + "</wps:Output>" + "  <wps:Output>   " + " \n " + "               <ows:Identifier>Winddirection</ows:Identifier>   " + " \n " + "               <ows:Title>Winddirection</ows:Title>   " + " \n " + "               <!-- The data type of the process output -->   " + " \n " + "               <wps:LiteralOutput defaultSchema=\"http://www.web3d.org/specifications/x3d-3.0.xsd\"> </wps:LiteralOutput>   " + " \n " + "</wps:Output>" + "       </wps:OutputDefinitions>   " + " \n " + " </wps:Execute>  ";
        request = request.replace("x_koordinate", x_pos);
        request = request.replace("y_koordinate", "-" + z_pos);
        request = request.replace("z_koordinate", y_pos);
        System.out.println("x: " + x_pos);
        System.out.println("y: " + y_pos);
        System.out.println("z: " + z_pos);
    }

    public void setRouteNavigationListener(PathNavigationListener listener) {
        this.routeNavigationListener = listener;
    }

    private JPanel makeEmssionScenarioPanel() {
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

    private JPanel makePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        this.setBackground(Color.WHITE);
        panel.setLayout(null);
        panel.setPreferredSize(new java.awt.Dimension(180, 455));
        int y = 0;
        JLabel GetCoordinates = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 9px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">" + "Get Coordinates" + "</span></body></html>");
        GetCoordinates.setBounds(5, y = y + 20, 100, 20);
        panel.add(GetCoordinates);
        PickStartButton = new JButton(Navigator.i18n.getString("ROUTING_PANEL_PICK"));
        PickStartButton.setBounds(120, y, 40, 20);
        PickStartButton.setMargin(new Insets(1, 1, 1, 1));
        PickStartButton.setEnabled(true);
        panel.add(PickStartButton);
        PickStartButton.addActionListener(this);
        JLabel XCoordinateLabel = new JLabel("X - Coordinate");
        XCoordinateLabel.setBounds(5, y = y + 40, 90, 20);
        panel.add(XCoordinateLabel);
        XCoordinatesTextfield = new JTextField();
        XCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(XCoordinatesTextfield);
        JLabel ZCoordinateLabel = new JLabel("Y - Coordinate");
        ZCoordinateLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(ZCoordinateLabel);
        ZCoordinatesTextfield = new JTextField();
        ZCoordinatesTextfield.setBounds(90, y, 80, 20);
        panel.add(ZCoordinatesTextfield);
        WindDirectionTextfield = new JTextField();
        WindDirectionTextfield.setBounds(90, y + 40, 80, 20);
        panel.add(WindDirectionTextfield);
        JLabel WindDirectionLabel = new JLabel("Wind Direction");
        WindDirectionLabel.setBounds(5, y = y + 40, 90, 20);
        panel.add(WindDirectionLabel);
        WindStrengthTextfield = new JTextField();
        WindStrengthTextfield.setBounds(90, y + 25, 80, 20);
        panel.add(WindStrengthTextfield);
        JLabel WindStrengthLabel = new JLabel("Wind Strength");
        WindStrengthLabel.setBounds(5, y = y + 25, 90, 20);
        panel.add(WindStrengthLabel);
        return panel;
    }

    public void sendFile(String f) {
        try {
            URL u = new URL("http://129.206.229.148/deegreeWPS/all");
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
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readParameters() {
        int begin = (response.indexOf("urn:ogc:def:dataType:OGC:1.0:Double") + 37);
        int end = begin + 4;
        String wd = response.substring(begin, end);
        if (wd.charAt(3) == '<') {
            wd = wd.substring(0, 3);
        }
        WindStrengthTextfield.setText(wd);
        begin = (response.indexOf("urn:ogc:def:dataType:OGC:1.0:String") + 37);
        end = begin + 4;
        String wr = response.substring(begin, end);
        if (wd.charAt(3) == '<') {
            wr = wd.substring(1, 3);
        } else if (wd.charAt(2) == '<') {
            wr = wd.substring(1, 2);
        }
        WindDirectionTextfield.setText(wr);
        begin = (response.indexOf("ows:reference=")) + 15;
        end = (response.indexOf(".wrl")) + 4;
        String wrl = "";
        if (begin > 0) {
            wrl = response.substring(begin, end);
        }
        try {
            url = new URL(wrl);
        } catch (MalformedURLException e) {
            System.err.println(e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object quelle = e.getSource();
        if (quelle == this.OKButton) {
            createWPSRequest();
            sendFile(request);
            readParameters();
            try {
                viewer.importURL(url);
            } catch (HeadlessException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        } else if (quelle == this.PickStartButton) {
            PickStartButton.setSelected(!PickStartButton.isSelected());
            navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            navigator.viewer.setClickCoordinateListener(this);
        } else if (quelle == this.ClearButton) {
            viewer.clearUserObjects();
        }
    }

    public void setViewer(Java3DViewer viewer) {
        this.viewer = viewer;
    }

    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
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

    public void coordinateClicked(Point3d GKposition) {
        xCor = GKposition.x;
        yCor = GKposition.y;
        zCor = GKposition.z;
        x_pos = String.valueOf(xCor);
        y_pos = String.valueOf(yCor);
        z_pos = String.valueOf(-zCor);
        String x_pos_ausgabe = "" + (((int) (xCor * 100.0)) / 100.0);
        String z_pos_ausgabe = "" + (((int) (-zCor * 100.0)) / 100.0);
        XCoordinatesTextfield.setText(x_pos_ausgabe);
        ZCoordinatesTextfield.setText(z_pos_ausgabe);
        PickStartButton.setSelected(false);
        navigator.viewer.getCanvas().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        navigator.viewer.setClickCoordinateListener(null);
    }
}
