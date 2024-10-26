package fr.brgm.exows.gml2gsml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import com.vividsolutions.jts.geom.Coordinate;
import fr.brgm.exows.ConnectorServlet;
import fr.brgm.exows.configuration.Entry;
import fr.brgm.exows.configuration.GeoSciML_Mapping;
import fr.brgm.exows.configuration.Group;
import fr.brgm.exows.vocabularies.Vocabularies;
import fr.brgm.utilities.VelocityCreator;

/**
 * @author BRGM
 * @version $Id$
 */
public class Gml2out {

    public static int ToGSML(GeoSciML_Mapping mapping, String strTemplate, String strRequest, String tagFeature, PrintWriter sortie, String requestedSRS) throws Exception {
        return Gml2out.ToGSML("", mapping, strTemplate, strRequest, tagFeature, sortie, requestedSRS);
    }

    public static int ToGSML(String prefix, GeoSciML_Mapping mapping, String strTemplate, String strRequest, String tagFeature, PrintWriter sortie, String requestedSRS) throws Exception {
        String level = "info.";
        if (ConnectorServlet.debug) level = "debug.";
        Log log = LogFactory.getLog(level + "fr.brgm.exows.gml2gsml.Gml2out");
        log.info(strRequest);
        URL url2Request = new URL(strRequest);
        URLConnection conn = url2Request.openConnection();
        Date dDebut = new Date();
        log.debug(dDebut);
        BufferedReader buffin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer strBuffer = new StringBuffer();
        String strLine = null;
        boolean exceptionRaised = false;
        Template template = VelocityCreator.createTemplate("/templates/" + strTemplate);
        boolean newFeature = false;
        strBuffer = new StringBuffer("");
        int nbFeatures = 0;
        strLine = buffin.readLine();
        String strTmp;
        int index;
        int length = tagFeature.length() + 1;
        while (strLine != null) {
            if (strLine.indexOf("<ServiceExceptionReport") != -1) {
                exceptionRaised = true;
            }
            if (exceptionRaised) {
                strBuffer.append(strLine);
                strBuffer.append("\n");
            }
            index = strLine.indexOf(tagFeature);
            if (index != -1) {
                if (newFeature) {
                    nbFeatures++;
                    VelocityContext context = new VelocityContext();
                    strBuffer.append(strLine.substring(0, index - 2));
                    strBuffer.append("</" + tagFeature + ">\n");
                    GSMLFeatureGeneric feature = createGSMLFeatureFromGMLFeatureString(prefix, mapping, strBuffer.toString(), requestedSRS);
                    context.put("feature", feature);
                    String outputFeatureMember = VelocityCreator.createXMLbyContext(context, template);
                    sortie.println(outputFeatureMember);
                    strBuffer = new StringBuffer();
                    newFeature = false;
                } else {
                    newFeature = true;
                }
            }
            strTmp = strLine;
            strLine = buffin.readLine();
            if (strLine == null) {
                if (!exceptionRaised) {
                    try {
                        if (strTmp.length() > index + length) strLine = strTmp.substring(index + length);
                    } catch (Exception e) {
                    }
                    if (newFeature) {
                        strBuffer.append("<" + tagFeature + ">\n");
                    }
                }
            } else {
                if (newFeature) {
                    strBuffer.append(strLine);
                    strBuffer.append("\n");
                }
            }
        }
        if (exceptionRaised) {
            sortie.print(strBuffer);
        } else {
        }
        buffin.close();
        Date dFin = new Date();
        String output = "GEOSCIML : " + nbFeatures + " features handled - time : " + (dFin.getTime() - dDebut.getTime()) / 1000 + " sec [" + dDebut + " // " + dFin + "] - Exception: " + Boolean.toString(exceptionRaised);
        log.trace(output);
        return nbFeatures;
    }

    /**
	 * Create an object <i>GSMLFeature</i> froma GML feature 
	 * @param config
	 * @param gml
	 * @return
	 */
    private static GSMLFeatureGeneric createGSMLFeatureFromGMLFeatureString(GeoSciML_Mapping mapping, String gml, String requestedSRS) {
        return Gml2out.createGSMLFeatureFromGMLFeatureString("", mapping, gml, requestedSRS);
    }

    private static GSMLFeatureGeneric createGSMLFeatureFromGMLFeatureString(String prefixe, GeoSciML_Mapping mapping, String gml, String requestedSRS) {
        if (requestedSRS == null) requestedSRS = "EPSG:4326";
        GSMLFeatureGeneric output = new GSMLFeatureGeneric();
        for (Entry entree : mapping.getEntries()) {
            String attribute = prefixe + entree.getAttribute();
            String attributeValue = StringUtils.substringBetween(gml, "<" + attribute + ">", "</" + attribute + ">");
            if (attributeValue == null || attributeValue.equalsIgnoreCase("null") || attributeValue.equalsIgnoreCase("")) {
                attributeValue = StringUtils.substringBetween(gml, "<ms:" + attribute + ">", "</ms:" + attribute + ">");
            }
            if (StringUtils.contains(entree.getGsml_uri(), "shape")) {
                String srs = StringUtils.substringBetween(attributeValue, "srsName=\"", "\"");
                if (srs == null) srs = StringUtils.substringBetween(attributeValue, "srsName='", "'");
                try {
                    if (srs != null && (!srs.equalsIgnoreCase(requestedSRS))) {
                        CoordinateReferenceSystem sourceCRS = CRS.decode(srs);
                        CoordinateReferenceSystem targetCRS = CRS.decode(requestedSRS);
                        Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);
                        MathTransform transform = ReferencingFactoryFinder.getCoordinateOperationFactory(hints).createOperation(sourceCRS, targetCRS).getMathTransform();
                        String[] split = StringUtils.splitByWholeSeparator(attributeValue, "</gml:coordinates>");
                        for (int j = 0; j < split.length; j++) {
                            String coordinates = StringUtils.substringAfterLast(split[j], "<gml:coordinates>");
                            String[] coordinatesArray = StringUtils.split(coordinates, " ");
                            String coordinatesTransformed = "";
                            for (int i = 0; i < coordinatesArray.length; i++) {
                                String[] coord = StringUtils.split(coordinatesArray[i], ",");
                                Coordinate cIn = new Coordinate(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
                                Coordinate cOut = new Coordinate();
                                JTS.transform(cIn, cOut, transform);
                                coordinatesTransformed += cOut.x + "," + cOut.y + " ";
                            }
                            attributeValue = StringUtils.replaceOnce(attributeValue, "<gml:coordinates>" + coordinates + "</gml:coordinates>", "<gml:coordinates>" + coordinatesTransformed + "</gml:coordinates>");
                        }
                        attributeValue = StringUtils.replace(attributeValue, srs, requestedSRS);
                    }
                } catch (Exception e) {
                }
            }
            if (attributeValue != null && !attributeValue.equalsIgnoreCase("null") && !attributeValue.equalsIgnoreCase("")) {
                output.setGSMLxpath(entree.getGsml_uri(), attributeValue);
            }
        }
        for (Group groupe : mapping.getGroups()) {
            Hashtable<String, List<String>> GroupHashtable = new Hashtable<String, List<String>>();
            boolean GroupHashtableIsNotEmpty = false;
            for (Entry entree : groupe.getEntries()) {
                String attribute = prefixe + entree.getAttribute();
                String attributeValue = StringUtils.substringBetween(gml, "<" + attribute + ">", "</" + attribute + ">");
                if (attributeValue == null || attributeValue.equalsIgnoreCase("null") || attributeValue.equalsIgnoreCase("")) {
                    attributeValue = StringUtils.substringBetween(gml, "<ms:" + attribute + ">", "</ms:" + attribute + ">");
                }
                if (StringUtils.contains(entree.getGsml_uri(), "shape")) {
                    String srs = StringUtils.substringBetween(attributeValue, "srsName=\"", "\"");
                    if (!srs.equalsIgnoreCase("EPSG:4326")) {
                        try {
                            CoordinateReferenceSystem sourceCRS = CRS.decode(srs);
                            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
                            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
                            String[] split = StringUtils.splitByWholeSeparator(attributeValue, "</gml:coordinates>");
                            for (int j = 0; j < split.length; j++) {
                                String coordinates = StringUtils.substringAfterLast(split[j], "<gml:coordinates>");
                                String[] coordinatesArray = StringUtils.split(coordinates, " ");
                                String coordinatesTransformed = "";
                                for (int i = 0; i < coordinatesArray.length; i++) {
                                    String[] coord = StringUtils.split(coordinatesArray[i], ",");
                                    Coordinate cIn = new Coordinate(Double.parseDouble(coord[0]), Double.parseDouble(coord[1]));
                                    Coordinate cOut = new Coordinate();
                                    JTS.transform(cIn, cOut, transform);
                                    coordinatesTransformed += cOut.x + "," + cOut.y + " ";
                                }
                                attributeValue = StringUtils.replaceOnce(attributeValue, "<gml:coordinates>" + coordinates + "</gml:coordinates>", "<gml:coordinates>" + coordinatesTransformed + "</gml:coordinates>");
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                if (attributeValue != null && !attributeValue.equalsIgnoreCase("null") && !attributeValue.equalsIgnoreCase("")) {
                    List<String> fieldsList = GroupHashtable.get(entree.getGsml_uri());
                    if (fieldsList != null) {
                        fieldsList.add(attributeValue);
                    } else {
                        fieldsList = new ArrayList<String>();
                        fieldsList.add(attributeValue);
                        GroupHashtable.put(entree.getGsml_uri(), fieldsList);
                    }
                    GroupHashtableIsNotEmpty = true;
                }
            }
            if (GroupHashtableIsNotEmpty) output.setGSMLxpathGroup(groupe.getGsml_path(), GroupHashtable);
        }
        return output;
    }

    public static void ToHTML(GeoSciML_Mapping mapping, String strTemplate, String strRequest, String tagFeature, String lang, PrintWriter sortie) throws Exception {
        Gml2out.ToHTML("", mapping, strTemplate, strRequest, tagFeature, lang, sortie);
    }

    public static void ToHTML(String prefix, GeoSciML_Mapping mapping, String strTemplate, String strRequest, String tagFeature, String lang, PrintWriter sortie) throws Exception {
        String level = "info.";
        if (ConnectorServlet.debug) level = "debug.";
        Log log = LogFactory.getLog(level + "fr.brgm.exows.gml2gsml.Gml2out");
        strRequest = StringUtils.replace(strRequest, "text/html", "application/vnd.ogc.gml");
        URL url2Request = new URL(strRequest);
        URLConnection conn = url2Request.openConnection();
        Date dDebut = new Date();
        BufferedReader buffin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer strBuffer = new StringBuffer();
        String strLine = null;
        Template template = VelocityCreator.createTemplate("/fr/brgm/exows/gml2gsml/templates/" + strTemplate);
        boolean newFeature = false;
        strBuffer = new StringBuffer("");
        int nbFeatures = 0;
        strLine = buffin.readLine();
        String strTmp;
        int index;
        int length = tagFeature.length() + 1;
        while (strLine != null) {
            index = strLine.indexOf(tagFeature);
            if (index != -1) {
                if (newFeature) {
                    nbFeatures++;
                    VelocityContext context = new VelocityContext();
                    strBuffer.append(strLine.substring(0, index - 2));
                    strBuffer.append("</" + tagFeature + ">\n");
                    GSMLFeatureGeneric feature = createGSMLFeatureFromGMLFeatureString(prefix, mapping, strBuffer.toString(), null);
                    context.put("feature", feature);
                    context.put("lang", lang);
                    context.put("vocabularies", new Vocabularies());
                    String outputFeatureMember = VelocityCreator.createXMLbyContext(context, template);
                    sortie.println(outputFeatureMember);
                    strBuffer = new StringBuffer();
                    newFeature = false;
                } else {
                    newFeature = true;
                }
            }
            strTmp = strLine;
            strLine = buffin.readLine();
            if (strLine == null) {
                try {
                    if (strTmp.length() > index + length) strLine = strTmp.substring(index + length);
                } catch (Exception e) {
                }
                if (newFeature) {
                    strBuffer.append("<" + tagFeature + ">\n");
                }
            } else {
                if (newFeature) {
                    strBuffer.append(strLine);
                    strBuffer.append("\n");
                }
            }
        }
        buffin.close();
        Date dFin = new Date();
        String output = "GEOSCIML : " + nbFeatures + " features handled - time : " + (dFin.getTime() - dDebut.getTime()) / 1000 + " [" + dDebut + " // " + dFin + "]";
        log.trace(output);
    }
}
