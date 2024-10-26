package org.gdi3d.xnavi.services.geocode;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.media.ding3d.vecmath.Point2d;
import javax.media.ding3d.vecmath.Point3d;
import org.apache.xmlbeans.XmlException;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.panels.finfo.FeatureInfoPanel;
import org.gdi3d.xnavi.panels.search.SearchAddressRequest;
import org.gdi3d.xnavi.services.w3ds.ByteBuf;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.PointType;
import net.opengis.xls.*;

public class OpenLSGeocoder {

    private String serviceEndPoint;

    public OpenLSGeocoder(String serviceEndPoint) {
        this.serviceEndPoint = serviceEndPoint;
    }

    public GeocodeResponse getGKCoordinateFromAddress(SearchAddressRequest searchAddressRequest) {
        GeocodeResponse result = null;
        String adress = null;
        if (searchAddressRequest.getAdressTextField() != null) adress = searchAddressRequest.getAdressTextField().getText();
        if (adress == null || adress.length() == 0) adress = " ";
        String postRequest = "";
        postRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" + "<xls:XLS xmlns:xls=\"http://www.opengis.net/xls\" xmlns:sch=\"http://www.ascc.net/xml/schematron\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/xls \n" + "http://gdi3d.giub.uni-bonn.de:8080/openls-lus/schemas/LocationUtilityService.xsd\" version=\"1.1\"> \n" + "	<xls:RequestHeader srsName=\"EPSG:" + Navigator.getEpsg_code() + "\"/> \n" + "	<xls:Request methodName=\"GeocodeRequest\" requestID=\"123456789\" version=\"1.1\"> \n" + "		<xls:GeocodeRequest> \n" + "			<xls:Address countryCode=\"DE\"> \n" + "				<xls:freeFormAddress>" + adress + "</xls:freeFormAddress> \n" + "			</xls:Address> \n" + "		</xls:GeocodeRequest> \n" + "	</xls:Request> \n" + "</xls:XLS> \n";
        if (Navigator.isVerbose()) {
            System.out.println("OpenLSGeocoder postRequest " + postRequest);
        }
        String errorMessage = "";
        try {
            System.out.println("contacting " + serviceEndPoint);
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
            xmlOut = new java.io.PrintWriter(urlc.getOutputStream());
            xmlOut.write(postRequest);
            xmlOut.flush();
            xmlOut.close();
            InputStream is = urlc.getInputStream();
            result = new GeocodeResponse();
            XLSDocument xlsResponse = XLSDocument.Factory.parse(is);
            XLSType xlsTypeResponse = xlsResponse.getXLS();
            Node node0 = xlsTypeResponse.getDomNode();
            NodeList nodes1 = node0.getChildNodes();
            for (int i = 0; i < nodes1.getLength(); i++) {
                Node node1 = nodes1.item(i);
                NodeList nodes2 = node1.getChildNodes();
                for (int j = 0; j < nodes2.getLength(); j++) {
                    Node node2 = nodes2.item(j);
                    NodeList nodes3 = node2.getChildNodes();
                    for (int k = 0; k < nodes3.getLength(); k++) {
                        Node node3 = nodes3.item(k);
                        String nodeName = node3.getNodeName();
                        if (nodeName.equalsIgnoreCase("xls:GeocodeResponseList")) {
                            net.opengis.xls.GeocodeResponseListDocument gcrld = net.opengis.xls.GeocodeResponseListDocument.Factory.parse(node3);
                            net.opengis.xls.GeocodeResponseListType geocodeResponseList = gcrld.getGeocodeResponseList();
                            result.setGeocodeResponseList(geocodeResponseList);
                        }
                    }
                }
            }
            is.close();
        } catch (java.net.ConnectException ce) {
            JOptionPane.showMessageDialog(null, "no connection to geocoder", "Connection Error", JOptionPane.ERROR_MESSAGE);
        } catch (SocketTimeoutException ste) {
            ste.printStackTrace();
            errorMessage += "<p>Time Out Exception, Server is not responding</p>";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            errorMessage += "<p>IO Exception</p>";
        } catch (XmlException xmle) {
            xmle.printStackTrace();
            errorMessage += "<p>Error occured during parsing the XML response</p>";
        }
        if (!errorMessage.equals("")) {
            System.out.println("\nerrorMessage: " + errorMessage + "\n\n");
            JLabel label1 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">Geocoder Error</span></body></html>");
            JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + errorMessage + "<br>" + "<p>please check Java console. If problem persits, please report to system manager</p>" + "</span></body></html>");
            Object[] objects = { label1, label2 };
            JOptionPane.showMessageDialog(null, objects, "Error Message", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return result;
    }

    public AddressType[] getAdressFromCRSCoordinate(Point3d crs_position) {
        AddressType[] result = null;
        String postRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" + "<xls:XLS xmlns:xls=\"http://www.opengis.net/xls\" xmlns:sch=\"http://www.ascc.net/xml/schematron\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/xls \n" + "	http://gdi3d.giub.uni-bonn.de/lus/schemas/LocationUtilityService.xsd\" version=\"1.1\"> \n" + "	<xls:RequestHeader srsName=\"EPSG:" + Navigator.getEpsg_code() + "\"/> \n" + "	<xls:Request methodName=\"ReverseGeocodeRequest\" requestID=\"123456789\" version=\"1.1\"> \n" + "		<xls:ReverseGeocodeRequest> \n" + "			<xls:Position> \n" + "				<gml:Point srsName=\"" + Navigator.getEpsg_code() + "\"> \n" + "					<gml:pos>" + crs_position.x + " " + crs_position.y + "</gml:pos> \n" + "				</gml:Point> \n" + "			</xls:Position> \n" + "			<xls:ReverseGeocodePreference>StreetAddress</xls:ReverseGeocodePreference> \n" + "		</xls:ReverseGeocodeRequest> \n" + "	</xls:Request> \n" + "</xls:XLS> \n";
        try {
            if (Navigator.isVerbose()) {
                System.out.println("contacting " + serviceEndPoint + ":\n" + postRequest);
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
            xmlOut = new java.io.PrintWriter(urlc.getOutputStream());
            xmlOut.write(postRequest);
            xmlOut.flush();
            xmlOut.close();
            InputStream is = urlc.getInputStream();
            XLSDocument xlsResponse = XLSDocument.Factory.parse(is);
            is.close();
            XLSType xlsTypeResponse = xlsResponse.getXLS();
            AbstractBodyType abBodyResponse[] = xlsTypeResponse.getBodyArray();
            ResponseType response = (ResponseType) abBodyResponse[0].changeType(ResponseType.type);
            AbstractResponseParametersType respParam = response.getResponseParameters();
            if (respParam == null) {
                return null;
            }
            ReverseGeocodeResponseType drResp = (ReverseGeocodeResponseType) respParam.changeType(ReverseGeocodeResponseType.type);
            net.opengis.xls.ReverseGeocodedLocationType[] types = drResp.getReverseGeocodedLocationArray();
            int num = types.length;
            if (num > 2) {
                return null;
            }
            result = new AddressType[num];
            for (int i = 0; i < num; i++) {
                String addressDescription = "<b>";
                net.opengis.xls.ReverseGeocodedLocationType type = types[i];
                result[i] = type.getAddress();
            }
        } catch (java.net.ConnectException ce) {
            JOptionPane.showMessageDialog(null, "no connection to reverse geocoder", "Connection Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getAdressFromGKCoordinateAsHTML(Point3d GKposition) {
        AddressType[] addresss = getAdressFromCRSCoordinate(GKposition);
        if (addresss == null) return "";
        String html = "";
        int num = addresss.length;
        for (int i = 0; i < num; i++) {
            AddressType address = addresss[i];
            String text = addressToText(address);
            html += "<b>" + text + "</b><br>";
        }
        return html;
    }

    public String addressToText(AddressType address) {
        String addressDescription = "";
        StreetAddressType streetAddress = address.getStreetAddress();
        StreetNameType[] streetNames = streetAddress.getStreetArray();
        for (int j = 0; j < streetNames.length; j++) {
            StreetNameType streetName = streetNames[j];
            String officialName = streetName.getOfficialName();
            if (officialName != null && !officialName.equals("null")) {
                if (j > 0) addressDescription += ",";
                addressDescription += officialName + " ";
            }
        }
        AbstractStreetLocatorType abstractStreetLocatorType = streetAddress.getStreetLocation();
        if (abstractStreetLocatorType instanceof BuildingLocatorType) {
            BuildingLocatorType buildingLocatorType = (BuildingLocatorType) abstractStreetLocatorType;
            String buildingName = buildingLocatorType.getBuildingName();
            String number = buildingLocatorType.getNumber();
            String subdivision = buildingLocatorType.getSubdivision();
            if (number != null && !number.equals("null")) {
                addressDescription += number + ", ";
            }
            if (buildingName != null && !buildingName.equals("null")) {
                addressDescription += buildingName + ", ";
            }
        }
        String postalCode = address.getPostalCode();
        if (postalCode != null && !postalCode.equals("null")) {
            addressDescription += postalCode + " ";
        }
        NamedPlaceType[] namedPlaceTypes = address.getPlaceArray();
        String municipality = null;
        String countrySubdivision = null;
        for (int j = 0; j < namedPlaceTypes.length; j++) {
            NamedPlaceType namedPlaceType = namedPlaceTypes[j];
            String value = namedPlaceType.getStringValue();
            if (namedPlaceType.getType() == NamedPlaceClassification.MUNICIPALITY) {
                municipality = value;
            }
            if (namedPlaceType.getType() == NamedPlaceClassification.COUNTRY_SUBDIVISION) {
                countrySubdivision = value;
            }
        }
        if (municipality != null && !municipality.equals("null")) {
            addressDescription += ", " + municipality;
        }
        if (countrySubdivision != null && !countrySubdivision.equals("null")) {
            addressDescription += ", " + countrySubdivision;
        }
        String countryCode = address.getCountryCode();
        if (countryCode != null && !countryCode.equals("null")) {
            addressDescription += ", " + countryCode.toUpperCase();
        }
        return addressDescription;
    }

    public Point2d assertAddress(JTextField adressTextField, String errorMessage) {
        Point2d point = null;
        String warningMessage = "";
        SearchAddressRequest searchAddressRequest = new SearchAddressRequest();
        searchAddressRequest.setAdressTextField(adressTextField);
        GeocodeResponse geocodeResponse = getGKCoordinateFromAddress(searchAddressRequest);
        if (geocodeResponse != null) {
            GeocodeResponseListType geocodeResponseListType = geocodeResponse.getGeocodeResponseList();
            net.opengis.xls.GeocodedAddressType[] geocodedAddressTypes = geocodeResponseListType.getGeocodedAddressArray();
            int numGeocodedAddressTypes = geocodedAddressTypes.length;
            if (numGeocodedAddressTypes > 0) {
                int selectedIndex = 0;
                if (numGeocodedAddressTypes > 1) {
                    selectedIndex = -1;
                    Vector<String> addressDescriptionVector = new Vector<String>();
                    for (int i = 0; i < numGeocodedAddressTypes; i++) {
                        net.opengis.xls.GeocodedAddressType geocodedAddressType = geocodedAddressTypes[i];
                        AddressType address = geocodedAddressType.getAddress();
                        String text = addressToText(address);
                        addressDescriptionVector.add(text);
                    }
                    JPanel panel = new JPanel();
                    GridBagLayout layout = new GridBagLayout();
                    panel.setLayout(layout);
                    DefaultListModel model = new DefaultListModel();
                    for (int i = 0; i < addressDescriptionVector.size(); i++) {
                        model.addElement(addressDescriptionVector.get(i));
                    }
                    JList list = new JList(model);
                    list.setSelectedIndex(0);
                    JScrollPane jScrollPane = new JScrollPane(list);
                    jScrollPane.getVerticalScrollBar().setUnitIncrement(5);
                    jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    panel.add(jScrollPane);
                    JLabel label1 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: bold;}--></style></head><body><span class=\"Stil2\">" + Navigator.i18n.getString("ROUTING_PANEL_MESSAGE4") + "</span></body></html>");
                    Object[] objects = { label1, panel };
                    if (Navigator.defaultMouseControlsFrame != null && Navigator.defaultMouseControlsFrame.isVisible()) {
                        Navigator.defaultMouseControlsFrame.dispose();
                    }
                    int option = JOptionPane.showConfirmDialog(null, objects, Navigator.i18n.getString("INFO_MESSAGE"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (option == 0) {
                        selectedIndex = list.getSelectedIndex();
                        if (selectedIndex < 0) selectedIndex = 0;
                    } else return null;
                }
                net.opengis.xls.GeocodedAddressType geocodedAddressType = geocodedAddressTypes[selectedIndex];
                PointType pointType = geocodedAddressType.getPoint();
                DirectPositionType directPositionType = pointType.getPos();
                String stringValue = directPositionType.getStringValue();
                StringTokenizer tok = new StringTokenizer(stringValue, " ", false);
                double x = Double.parseDouble(tok.nextToken());
                double y = Double.parseDouble(tok.nextToken());
                point = new Point2d(x, y);
                AddressType address = geocodedAddressType.getAddress();
                String freeFormAddress = address.getFreeFormAddress();
                String street = null;
                String house_no = null;
                String zip = null;
                String city = null;
                String country = null;
                StreetAddressType streetAddress = address.getStreetAddress();
                StreetNameType[] streetNames = streetAddress.getStreetArray();
                if (streetNames.length > 0) {
                    StreetNameType streetName = streetNames[0];
                    street = streetName.getOfficialName();
                }
                AbstractStreetLocatorType abstractStreetLocatorType = streetAddress.getStreetLocation();
                if (abstractStreetLocatorType instanceof BuildingLocatorType) {
                    BuildingLocatorType buildingLocatorType = (BuildingLocatorType) abstractStreetLocatorType;
                    String buildingName = buildingLocatorType.getBuildingName();
                    house_no = buildingLocatorType.getNumber();
                }
                zip = address.getPostalCode();
                NamedPlaceType[] namedPlaceTypes = address.getPlaceArray();
                String municipality = null;
                String countrySubdivision = null;
                for (int j = 0; j < namedPlaceTypes.length; j++) {
                    NamedPlaceType namedPlaceType = namedPlaceTypes[j];
                    String value = namedPlaceType.getStringValue();
                    if (namedPlaceType.getType() == NamedPlaceClassification.MUNICIPALITY) {
                        municipality = value;
                    }
                    if (namedPlaceType.getType() == NamedPlaceClassification.COUNTRY_SUBDIVISION) {
                        countrySubdivision = value;
                    }
                }
                city = municipality;
                String countryCode = address.getCountryCode();
                country = countryCode.toUpperCase();
                String adressText = "";
                if (street != null) adressText += street;
                if (house_no != null) adressText += " " + house_no;
                if (zip != null) adressText += " " + zip;
                if (city != null) adressText += " " + city;
                if (country != null) adressText += ", " + country;
                adressTextField.setText(adressText);
            } else warningMessage += Navigator.i18n.getString("ADDRESS_NOT_FOUND") + "</p>";
        } else errorMessage += "<p>OpenLSGeocoder.assertAddress point = " + point + "</p>";
        if (!warningMessage.equals("")) {
            JLabel label2 = new JLabel("<html><head><style type=\"text/css\"><!--.Stil2 {font-size: 10px;font-weight: normal;}--></style></head><body><span class=\"Stil2\">" + "<br>" + warningMessage + "<br>" + "</span></body></html>");
            Object[] objects = { label2 };
            JOptionPane.showMessageDialog(null, objects, Navigator.i18n.getString("WARNING_MESSAGE"), JOptionPane.WARNING_MESSAGE);
        }
        return point;
    }
}
