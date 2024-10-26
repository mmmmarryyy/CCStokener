package org.gdi3d.xnavi.panels.track;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.media.ding3d.vecmath.Point3d;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import x0.oasisNamesTcEbxmlRegrepXsdRim3.ServiceDocument;
import com.sun.org.apache.bcel.internal.generic.DDIV;
import com.sun.xml.internal.fastinfoset.QualifiedName;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.gdi3d.xnavi.navigator.Navigator;
import org.gdi3d.xnavi.panels.sensor.ObservationResult;
import net.opengis.gml.AbstractFeatureCollectionType;
import net.opengis.gml.AbstractFeatureType;
import net.opengis.gml.AbstractGeometryType;
import net.opengis.gml.AbstractTimeObjectType;
import net.opengis.gml.CoordinatesDocument;
import net.opengis.gml.CoordinatesType;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.FeaturePropertyType;
import net.opengis.gml.PointDocument;
import net.opengis.gml.PointType;
import net.opengis.gml.PosDocument;
import net.opengis.gml.TimePositionType;
import net.opengis.gml.impl.FeaturePropertyTypeImpl;
import net.opengis.gml.impl.GeometryPropertyTypeImpl;
import net.opengis.gml.impl.PointTypeImpl;
import net.opengis.gml.impl.TimeInstantTypeImpl;
import net.opengis.om.x10.ObservationCollectionDocument;
import net.opengis.om.x10.ObservationCollectionType;
import net.opengis.om.x10.ObservationPropertyType;
import net.opengis.om.x10.ObservationType;
import net.opengis.om.x10.impl.GeometryObservationTypeImpl;
import net.opengis.sos.x10.GetObservationDocument;
import net.opengis.sos.x10.GetObservationDocument.GetObservation;
import net.opengis.sos.x10.GetObservationDocument.GetObservation.FeatureOfInterest;
import net.opengis.swe.x101.DataArrayDocument;
import net.opengis.swe.x101.SimpleDataRecordType;
import net.opengis.swe.x101.TimeObjectPropertyType;

public class TrackingGetObservation {

    /**
	 *
	 * @param url
	 * @param servicename
	 * @param srsname
	 * @param version
	 * @param offering
	 * @param observed_property
	 * @param responseFormat
	 * @return
	 */
    public ObservationResult[] call(String url, String servicename, String srsname, String version, String offering, String observed_property, String responseFormat) {
        System.out.println("GetObservationBasic.call url " + url);
        URL service = null;
        URLConnection connection = null;
        ArrayList<ObservationResult> obsList = new ArrayList<ObservationResult>();
        boolean isDataArrayRead = false;
        try {
            service = new URL(url);
            connection = service.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try {
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            GetObservationDocument getobDoc = GetObservationDocument.Factory.newInstance();
            GetObservation getob = getobDoc.addNewGetObservation();
            getob.setService(servicename);
            getob.setVersion(version);
            getob.setSrsName(srsname);
            getob.setOffering(offering);
            getob.setObservedPropertyArray(new String[] { observed_property });
            getob.setResponseFormat(responseFormat);
            String request = URLEncoder.encode(getobDoc.xmlText(), "UTF-8");
            out.writeBytes(request);
            out.flush();
            out.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            URL observation_url = new URL("file:///E:/Temp/Observation.xml");
            URLConnection urlc = observation_url.openConnection();
            urlc.connect();
            InputStream observation_url_is = urlc.getInputStream();
            ObservationCollectionDocument obsCollDoc = ObservationCollectionDocument.Factory.parse(observation_url_is);
            ObservationCollectionType obsColl = obsCollDoc.getObservationCollection();
            ObservationPropertyType[] aObsPropType = obsColl.getMemberArray();
            for (ObservationPropertyType observationPropertyType : aObsPropType) {
                ObservationType observation = observationPropertyType.getObservation();
                if (observation != null) {
                    System.out.println("observation " + observation.getClass().getName());
                    ObservationResult obsResult = new ObservationResult();
                    if (observation instanceof GeometryObservationTypeImpl) {
                        GeometryObservationTypeImpl geometryObservation = (GeometryObservationTypeImpl) observation;
                        TimeObjectPropertyType samplingTime = geometryObservation.getSamplingTime();
                        TimeInstantTypeImpl timeInstant = (TimeInstantTypeImpl) samplingTime.getTimeObject();
                        TimePositionType timePosition = timeInstant.getTimePosition();
                        String time = (String) timePosition.getObjectValue();
                        StringTokenizer date_st;
                        String day = new StringTokenizer(time, "T").nextToken();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date d = sdf.parse(day);
                        String timetemp = null;
                        date_st = new StringTokenizer(time, "T");
                        while (date_st.hasMoreElements()) timetemp = date_st.nextToken();
                        sdf = new SimpleDateFormat("HH:mm:ss");
                        Date ti = sdf.parse(timetemp.substring(0, timetemp.lastIndexOf(':') + 2));
                        d.setHours(ti.getHours());
                        d.setMinutes(ti.getMinutes());
                        d.setSeconds(ti.getSeconds());
                        obsResult.setDatetime(d);
                        String textValue = "null";
                        FeaturePropertyType featureOfInterest = (FeaturePropertyType) geometryObservation.getFeatureOfInterest();
                        Node fnode = featureOfInterest.getDomNode();
                        NodeList childNodes = fnode.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node cnode = childNodes.item(j);
                            if (cnode.getNodeName().equals("n52:movingObject")) {
                                NamedNodeMap att = cnode.getAttributes();
                                Node id = att.getNamedItem("gml:id");
                                textValue = id.getNodeValue();
                                obsResult.setTextValue(textValue);
                                obsResult.setIsTextValue(true);
                            }
                        }
                        XmlObject result = geometryObservation.getResult();
                        if (result instanceof GeometryPropertyTypeImpl) {
                            GeometryPropertyTypeImpl geometryPropertyType = (GeometryPropertyTypeImpl) result;
                            AbstractGeometryType geometry = geometryPropertyType.getGeometry();
                            String srsName = geometry.getSrsName();
                            StringTokenizer st = new StringTokenizer(srsName, ":");
                            String epsg = null;
                            while (st.hasMoreElements()) epsg = st.nextToken();
                            int sri = Integer.parseInt(epsg);
                            if (geometry instanceof PointTypeImpl) {
                                PointTypeImpl point = (PointTypeImpl) geometry;
                                Node node = point.getDomNode();
                                PointDocument pointDocument = PointDocument.Factory.parse(node);
                                PointType point2 = pointDocument.getPoint();
                                XmlCursor cursor = point.newCursor();
                                cursor.toFirstChild();
                                CoordinatesDocument coordinatesDocument = CoordinatesDocument.Factory.parse(cursor.xmlText());
                                CoordinatesType coords = coordinatesDocument.getCoordinates();
                                StringTokenizer tok = new StringTokenizer(coords.getStringValue(), " ,;", false);
                                double x = Double.parseDouble(tok.nextToken());
                                double y = Double.parseDouble(tok.nextToken());
                                double z = 0;
                                if (tok.hasMoreTokens()) {
                                    z = Double.parseDouble(tok.nextToken());
                                }
                                x += 207561;
                                y += 3318814;
                                z += 20;
                                Point3d center = new Point3d(x, y, z);
                                obsResult.setCenter(center);
                                GeometryFactory fact = new GeometryFactory();
                                Coordinate coordinate = new Coordinate(x, y, z);
                                Geometry g1 = fact.createPoint(coordinate);
                                g1.setSRID(sri);
                                obsResult.setGeometry(g1);
                                String href = observation.getProcedure().getHref();
                                obsResult.setProcedure(href);
                                obsList.add(obsResult);
                            }
                        }
                    }
                }
            }
            observation_url_is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ObservationResult[] ar = new ObservationResult[obsList.size()];
        return obsList.toArray(ar);
    }
}
