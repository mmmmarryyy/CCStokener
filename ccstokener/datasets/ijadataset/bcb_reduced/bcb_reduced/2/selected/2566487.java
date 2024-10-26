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
public class ESRIml2out {

    public static int ToGSML(GeoSciML_Mapping mapping, String strTemplate, String strRequest, PrintWriter sortie, String requestedSRS) throws Exception {
        String level = "info.";
        if (ConnectorServlet.debug) level = "debug.";
        Log log = LogFactory.getLog(level + "fr.brgm.exows.gml2gsml.Gml2Gsml");
        log.debug(strRequest);
        String tagFeature = "FIELDS";
        URL url2Request = new URL(strRequest);
        URLConnection conn = url2Request.openConnection();
        Date dDebut = new Date();
        BufferedReader buffin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String strLine = null;
        int nbFeatures = 0;
        Template template = VelocityCreator.createTemplate("/fr/brgm/exows/gml2gsml/templates/" + strTemplate);
        while ((strLine = buffin.readLine()) != null) {
            if (strLine.indexOf(tagFeature) != -1) {
                nbFeatures++;
                GSMLFeatureGeneric feature = createGSMLFeatureFromGMLFeatureString(mapping, strLine);
                VelocityContext context = new VelocityContext();
                context.put("feature", feature);
                String outputFeatureMember = VelocityCreator.createXMLbyContext(context, template);
                sortie.println(outputFeatureMember);
            }
        }
        buffin.close();
        Date dFin = new Date();
        String output = "GEOSCIML : " + nbFeatures + " features handled - time : " + (dFin.getTime() - dDebut.getTime()) / 1000 + " [" + dDebut + " // " + dFin + "]";
        log.trace(output);
        return nbFeatures;
    }

    /**
	 * This function create an object <i>GSMLFeature</i> from a GML feature 
	 * @param config
	 * @param gml
	 * @return
	 */
    private static GSMLFeatureGeneric createGSMLFeatureFromGMLFeatureString(GeoSciML_Mapping mapping, String gml) {
        GSMLFeatureGeneric output = new GSMLFeatureGeneric();
        for (Entry entree : mapping.getEntries()) {
            String attribute = entree.getAttribute();
            String attributeValue = StringUtils.substringBetween(gml, attribute + "=\"", "\"");
            if (attributeValue != null && !attributeValue.equalsIgnoreCase("null") && !attributeValue.equalsIgnoreCase("")) {
                output.setGSMLxpath(entree.getGsml_uri(), attributeValue);
            }
        }
        for (Group groupe : mapping.getGroups()) {
            Hashtable<String, List<String>> GroupHashtable = new Hashtable<String, List<String>>();
            boolean GroupHashtableIsNotEmpty = false;
            for (Entry entree : groupe.getEntries()) {
                String attribute = entree.getAttribute();
                String attributeValue = StringUtils.substringBetween(gml, attribute + "=\"", "\"");
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

    public static void ToHTML(GeoSciML_Mapping mapping, String strTemplate, String strRequest, String lang, PrintWriter sortie) throws Exception {
        String level = "info.";
        if (ConnectorServlet.debug) level = "debug.";
        Log log = LogFactory.getLog(level + "fr.brgm.exows.gml2gsml.Gml2Gsml");
        String tagFeature = "FIELDS";
        URL url2Request = new URL(strRequest);
        URLConnection conn = url2Request.openConnection();
        Date dDebut = new Date();
        BufferedReader buffin = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String strLine = null;
        int nbFeatures = 0;
        Template template = VelocityCreator.createTemplate("/fr/brgm/exows/gml2gsml/templates/" + strTemplate);
        while ((strLine = buffin.readLine()) != null) {
            if (strLine.indexOf(tagFeature) != -1) {
                nbFeatures++;
                GSMLFeatureGeneric feature = createGSMLFeatureFromGMLFeatureString(mapping, strLine);
                VelocityContext context = new VelocityContext();
                context.put("feature", feature);
                context.put("lang", lang);
                context.put("vocabularies", new Vocabularies());
                String outputFeatureMember = VelocityCreator.createXMLbyContext(context, template);
                sortie.println(outputFeatureMember);
            }
        }
        buffin.close();
        Date dFin = new Date();
        String output = "GEOSCIML : " + nbFeatures + " features handled - time : " + (dFin.getTime() - dDebut.getTime()) / 1000 + " [" + dDebut + " // " + dFin + "]";
        log.trace(output);
    }
}
