package net.sourceforge.ondex.parser.chemblactivity;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.core.AttributeName;
import net.sourceforge.ondex.core.ConceptAccession;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.event.type.GeneralOutputEvent;
import net.sourceforge.ondex.event.type.ParsingErrorEvent;
import net.sourceforge.ondex.parser.ONDEXParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for bio activity data from ChEMBL database
 * 
 * @author taubertj
 * 
 */
public class Parser extends ONDEXParser implements MetaData {

    DataSource dsCHEMBL, dsASSAY, dsTARGET;

    EvidenceType evidencetype;

    ConceptClass ccActivity, ccTarget;

    RelationType rtActivity, rtOccin;

    AttributeName anRef, anOrg, anType, anComment, anConf;

    Map<String, ONDEXConcept> targets = new HashMap<String, ONDEXConcept>();

    Map<String, AttributeName> anTypes = new HashMap<String, AttributeName>();

    /**
	 * Mapping of comment terms for normalization
	 */
    private static Map<String, String> mapping = new HashMap<String, String>();

    static {
        mapping.put("Inconclusive", "Inconclusive");
        mapping.put("inconclusive", "Inconclusive");
        mapping.put("Not Active", "Not Active");
        mapping.put("Inactive", "Not Active");
        mapping.put("inactive", "Not Active");
        mapping.put("Active", "Active");
        mapping.put("active", "Active");
    }

    /**
	 * initialise Ondex meta-data
	 */
    private void initMetaData() {
        dsCHEMBL = graph.getMetaData().getDataSource(DS_CHEMBL);
        evidencetype = graph.getMetaData().getEvidenceType(ET_IMPD);
        anRef = graph.getMetaData().getAttributeName(AN_REFERENCE);
        anConf = graph.getMetaData().getAttributeName(AN_CONF);
        rtOccin = graph.getMetaData().getRelationType(RT_OCCIN);
        ccActivity = graph.getMetaData().getConceptClass(CC_ACTIVITY);
        if (ccActivity == null) ccActivity = graph.getMetaData().getFactory().createConceptClass(CC_ACTIVITY);
        ccTarget = graph.getMetaData().getConceptClass(CC_TARGET);
        if (ccTarget == null) ccTarget = graph.getMetaData().getFactory().createConceptClass(CC_TARGET);
        rtActivity = graph.getMetaData().getRelationType(RT_ACTIVITY);
        if (rtActivity == null) rtActivity = graph.getMetaData().getFactory().createRelationType(RT_ACTIVITY);
        dsASSAY = graph.getMetaData().getDataSource(DS_CHEMBLASSAY);
        if (dsASSAY == null) dsASSAY = graph.getMetaData().getFactory().createDataSource(DS_CHEMBLASSAY);
        dsTARGET = graph.getMetaData().getDataSource(DS_CHEMBLTARGET);
        if (dsTARGET == null) dsTARGET = graph.getMetaData().getFactory().createDataSource(DS_CHEMBLTARGET);
        anOrg = graph.getMetaData().getAttributeName(AN_ORGANISM);
        if (anOrg == null) anOrg = graph.getMetaData().getFactory().createAttributeName(AN_ORGANISM, String.class);
        anType = graph.getMetaData().getAttributeName(AN_TYPE);
        if (anType == null) anType = graph.getMetaData().getFactory().createAttributeName(AN_TYPE, String.class);
        anComment = graph.getMetaData().getAttributeName(AN_COMMENT);
        if (anComment == null) anComment = graph.getMetaData().getFactory().createAttributeName(AN_COMMENT, String.class);
    }

    @Override
    public String getId() {
        return "chemblactivity";
    }

    @Override
    public String getName() {
        return "ChEMBL BioActivity";
    }

    @Override
    public String getVersion() {
        return "17.11.2011";
    }

    @Override
    public ArgumentDefinition<?>[] getArgumentDefinitions() {
        return new ArgumentDefinition<?>[0];
    }

    @Override
    public void start() throws Exception {
        initMetaData();
        Map<String, Set<ONDEXConcept>> accessions = new HashMap<String, Set<ONDEXConcept>>();
        for (ONDEXConcept c : graph.getConcepts()) {
            for (ConceptAccession ca : c.getConceptAccessions()) {
                if (ca.getElementOf().equals(dsCHEMBL) && !accessions.containsKey(ca.getAccession())) accessions.put(ca.getAccession(), new HashSet<ONDEXConcept>());
                accessions.get(ca.getAccession()).add(c);
            }
        }
        System.out.println(accessions);
        int count = 0;
        for (String accession : accessions.keySet()) {
            URL url = new URL("https://www.ebi.ac.uk/chemblws/compounds/" + accession + "/bioactivities");
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            int code = uc.getResponseCode();
            if (code != 200) {
                String response = uc.getResponseMessage();
                fireEventOccurred(new ParsingErrorEvent("HTTP/1.x " + code + " " + response, getCurrentMethodName()));
            } else {
                InputStream in = new BufferedInputStream(uc.getInputStream());
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(in);
                doc.getDocumentElement().normalize();
                NodeList nList = doc.getElementsByTagName("bioactivity");
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        ONDEXConcept activity = graph.getFactory().createConcept(getTagValue("assay__chemblid", eElement), getTagValue("assay__description", eElement), dsCHEMBL, ccActivity, evidencetype);
                        activity.createConceptAccession(getTagValue("assay__chemblid", eElement), dsASSAY, false);
                        activity.createAttribute(anRef, getTagValue("reference", eElement), true);
                        activity.createAttribute(anOrg, getTagValue("organism", eElement), true);
                        String type = getTagValue("bioactivity__type", eElement);
                        type = type.replace(" ", "_");
                        if (!anTypes.containsKey(type)) {
                            AttributeName an = graph.getMetaData().getFactory().createAttributeName(type, Double.class);
                            String units = getTagValue("units", eElement);
                            if (graph.getMetaData().getUnit(units) == null) graph.getMetaData().getFactory().createUnit(units);
                            an.setUnit(graph.getMetaData().getUnit(units));
                            anTypes.put(type, an);
                        }
                        String value = getTagValue("value", eElement);
                        try {
                            Double d = Double.valueOf(value);
                            activity.createAttribute(anTypes.get(type), d, false);
                        } catch (NumberFormatException nfe) {
                        }
                        String comment = getTagValue("activity__comment", eElement);
                        if (comment != null && comment.trim().length() > 0) {
                            if (mapping.containsKey(comment)) comment = mapping.get(comment);
                            activity.createAttribute(anComment, comment, true);
                        }
                        count++;
                        Set<ONDEXConcept> compounds = accessions.get(accession);
                        for (ONDEXConcept c : compounds) {
                            graph.getFactory().createRelation(c, activity, rtActivity, evidencetype);
                        }
                        String key = getTagValue("target__chemblid", eElement);
                        if (!targets.containsKey(key)) {
                            ONDEXConcept c = graph.getFactory().createConcept(key, dsCHEMBL, ccTarget, evidencetype);
                            c.createConceptName(getTagValue("target__name", eElement), true);
                            c.createConceptAccession(key, dsTARGET, false);
                            targets.put(key, c);
                        }
                        ONDEXConcept target = targets.get(key);
                        ONDEXRelation r = graph.getFactory().createRelation(activity, target, rtOccin, evidencetype);
                        r.createAttribute(anConf, Double.valueOf(getTagValue("target__confidence", eElement)), false);
                    }
                }
            }
        }
        fireEventOccurred(new GeneralOutputEvent("Total assays parsed:" + count, getCurrentMethodName()));
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }

    @Override
    public String[] requiresValidators() {
        return new String[0];
    }
}
