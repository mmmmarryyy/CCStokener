package org.acs.elated.fed;

import java.io.*;
import java.util.*;
import java.net.*;
import org.acs.elated.app.DStream;
import org.acs.elated.commons.*;
import org.acs.elated.exception.*;
import org.apache.log4j.Logger;
import com.hp.hpl.jena.rdf.model.*;

/**
 * <p>
 * Manages Fedora-based object to object relationships, including collection membership
 * and other information.  Uses the ontologies indedxed in the RELS-EXT datastreams.
 * </p>
 */
public class FedoraRelationshipMgr {

    private static Logger logger = (Logger) Logger.getInstance(FedoraRelationshipMgr.class);

    /**
	 * Adds a relationship between 2 items
	 * @param itemPID the place to store the relationship
	 * @param item1PID the 'subject' item
	 * @param relationship the relationship
	 * @param item2PID the 'object' item
	 */
    public static void addRelationship(String itemPID, String item1PID, String relationship, String item2PID) throws Exception {
        logger.debug("entering addRelationship()");
        Model model = getModel(itemPID);
        Property prop = model.createProperty(FedoraInterface.FEDORA_NS + relationship);
        Resource res1 = model.createResource("info:fedora/" + item1PID);
        Resource res2 = model.createResource("info:fedora/" + item2PID);
        res1.addProperty(prop, res2);
        Statement statement = model.createStatement(res1, prop, res2);
        model.add(statement);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        model.write(pw);
        logger.debug("RDF/XML:" + sw.toString());
        DStream dStream = new DStream(itemPID, FedoraInterface.RELS_EXT, FedoraInterface.FEDORA_RELATIONSHIP_RDF_XML, FedoraInterface.TEXT_XML, null);
        dStream.setData(sw.toString().getBytes());
        if (MaAPI.datastreamExists(itemPID, FedoraInterface.RELS_EXT)) {
            MaAPI.updateDatastreamByValue(dStream);
        } else {
            MaAPI.addDatastreamByValue(dStream, FedoraInterface.CONTROL_GROUP_XML);
        }
    }

    /**
	 * Removes a relationship between 2 items
	 * @param itemPID the item where the relationship is stored
	 * @param item1PID the 'subject' item
	 * @param relationship the relationship
	 * @param item2PID the 'object' item
	 * @return whether or not the relationship was found and removed
	 */
    public static boolean removeRelationship(String itemPID, String item1PID, String relationship, String item2PID) throws Exception {
        logger.debug("entering addRelationship()");
        Model model = getModel(itemPID);
        Property prop = model.createProperty(FedoraInterface.FEDORA_NS + relationship);
        Resource res1 = model.createResource("info:fedora/" + item1PID);
        Resource res2 = model.createResource("info:fedora/" + item2PID);
        res1.addProperty(prop, res2);
        Statement statement = model.createStatement(res1, prop, res2);
        if (model.contains(statement)) {
            model.remove(statement);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            model.write(pw);
            logger.debug("RDF/XML:" + sw.toString());
            DStream dStream = new DStream(itemPID, FedoraInterface.RELS_EXT, FedoraInterface.FEDORA_RELATIONSHIP_RDF_XML, FedoraInterface.TEXT_XML, null);
            dStream.setData(sw.toString().getBytes());
            if (MaAPI.datastreamExists(itemPID, FedoraInterface.RELS_EXT)) {
                MaAPI.updateDatastreamByValue(dStream);
            } else {
                MaAPI.addDatastreamByValue(dStream, FedoraInterface.CONTROL_GROUP_XML);
            }
            return true;
        }
        return false;
    }

    /**
	 * <p>
	 * Queries a given item to ask about its relationships to other items.
	 * </p>
	 * <p>
	 * IMPORTANT: if an item has just been added, Fedora needs a few seconds
	 * to index; use the alternative queryItemRelationship() in that event.
	 * </p>
	 * @param itemPID
	 * @param relationship
	 * @param relationshipNS
	 * @return a list of PIDs of objects having the relationship
	 * @throws Exception
	 */
    public static List queryItemRelationship(String itemPID, String relationship, String relationshipNS) throws Exception {
        logger.debug("entering queryItemRelationship(3-arg)");
        if (StringUtils.isEmpty(relationship) || StringUtils.isEmpty(itemPID)) {
            return new ArrayList();
        }
        String itqlQuery = " select $x " + " from <#ri> " + " where  " + " <info:fedora/" + itemPID.trim() + "> " + " <" + relationshipNS + ":" + relationship.trim() + "> " + " $x";
        logger.debug("ITQL query: " + itqlQuery);
        return runITQLQuery(itqlQuery);
    }

    /**
	 * Queries relationships based on the model held in a single item.
	 * @param hostingItemPID the object where the RDF model is held
	 * @param itemPID the item
	 * @param relationship the raltionship
	 * @param relationshipNS the namespace for the realtionship
	 * @return a list of PIDs of objects having the relationship
	 * @throws Exception
	 */
    public static List queryItemRelationship(String hostingItemPID, String itemPID, String relationship, String relationshipNS) throws Exception {
        logger.debug("entering queryItemRelationship(4-arg)");
        Model model = getModel(itemPID);
        RDFNode rdfNode = null;
        StmtIterator iter = model.listStatements(model.createResource("info:fedora/" + itemPID), model.createProperty(FedoraInterface.FEDORA_NS + relationship), rdfNode);
        ArrayList arrayList = new ArrayList();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            RDFNode node = stmt.getObject();
            String s = node.toString().trim();
            logger.info("relationship : " + s);
            String pid = s.substring(s.indexOf("/") + 1);
            if (pid.length() > 0) {
                arrayList.add(pid);
                logger.debug("found relationship to item: " + pid);
            }
        }
        logger.debug("num relationships found: " + arrayList.size());
        return arrayList;
    }

    /**
	 * Queries the relationships, returning a list of PIDs for items having
	 * the given relationship to the item requested
	 * @param relationship the relationship
	 * @param the relationshipNS namespace
	 * @param itemPID the predicate item to which the relationship refers
	 * @return a list of PIDs of objects having the relationship
	 */
    public static List queryRelationshipToItem(String relationship, String relationshipNS, String itemPID) throws Exception {
        logger.debug("entering queryRelationshipToItem()");
        if (StringUtils.isEmpty(relationship) || StringUtils.isEmpty(itemPID)) {
            return new ArrayList();
        }
        String itqlQuery = "select $member " + " from <#ri> " + " where  $member <" + relationshipNS + ":" + relationship.trim() + "> <info:fedora/" + itemPID.trim() + ">";
        logger.debug("ITQL query: " + itqlQuery);
        return runITQLQuery(itqlQuery);
    }

    /**
	 * Runs an ITQL query and returns a list of objects
	 * @param query the query to run
	 * @return a list of item PIDs
	 */
    private static List runITQLQuery(String itqlQuery) throws Exception {
        String escapedItqlQuery = URLEncoder.encode(itqlQuery, "UTF-8");
        String url = "http://" + Config.getProperty("FEDORA_SOAP_HOST") + ":" + Config.getProperty("FEDORA_SOAP_ACCESS_PORT") + "/fedora/risearch?type=tuples" + "&lang=iTQL" + "&format=CSV" + "&distinct=on" + "&stream=on" + "&query=" + escapedItqlQuery;
        logger.debug("url for risearch query: " + url);
        URL urlObject = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObject.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        logger.debug("response code: " + con.getResponseCode());
        if (con.getResponseCode() != 200 && con.getResponseCode() != 302) {
            throw new FedoraAccessException("Could not access the risearch service at url: " + url);
        }
        ArrayList arrayList = new ArrayList();
        String inputLine;
        int counter = 0;
        while ((inputLine = br.readLine()) != null) {
            logger.debug("reading line:" + inputLine);
            if (inputLine.indexOf("<html>") >= 0) {
                logger.error("problem quering the relationship");
                throw new Exception("Problem querying relationships; probably a bad ITQL query:" + itqlQuery);
            }
            if (counter >= 1 && inputLine.indexOf("/") >= 0 && inputLine.trim().length() > 0) {
                logger.debug("adding line:" + inputLine);
                inputLine = inputLine.substring(inputLine.indexOf("/") + 1);
                arrayList.add(inputLine);
                logger.debug("found relationship to item: " + inputLine);
            }
            counter++;
        }
        br.close();
        logger.debug("num relationships found: " + arrayList.size());
        return arrayList;
    }

    /**
	 * Gets the RDF graph for the item
	 * @param itemPID
	 * @return the model, or an empty model if no model exists yet
	 */
    private static Model getModel(String itemPID) throws Exception {
        Model model = ModelFactory.createDefaultModel();
        if (MaAPI.datastreamExists(itemPID, FedoraInterface.RELS_EXT)) {
            byte[] bytes = MaAPI.getDatastreamData(itemPID, FedoraInterface.RELS_EXT);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            model.read(bais, "");
        } else {
            model.setNsPrefix("fedora", FedoraInterface.FEDORA_NS);
        }
        return model;
    }
}
