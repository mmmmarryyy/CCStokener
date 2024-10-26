package no.uio.edd.model.geo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import no.uio.edd.model.geo.calc.*;
import no.uio.edd.utils.datautils.EddDataUtils;
import no.uio.edd.utils.datautils.EddTableModel;
import no.uio.edd.utils.datautils.ExtendableObjectList;
import no.uio.edd.utils.datautils.ExtendableStringList;
import no.uio.edd.utils.datautils.MessageViewer;
import no.uio.edd.utils.datautils.TableCallbackInterface;
import no.uio.edd.utils.fileutils.FileUtils;
import no.uio.edd.utils.geoutils.GeoLocationException;
import no.uio.edd.utils.menuutils.MenuInterface;
import no.uio.edd.utils.menuutils.MenuStorage;
import no.uio.edd.utils.misc.TimeUtils;
import no.uio.edd.utils.xmlutils.DomUtils;
import no.uio.edd.utils.xmlutils.ExtendableNodeList;

/**
 * The main runner for the GeoModelText application.
 * 
 * @author oeide
 * 
 */
public class GeoModelRunner extends JTabbedPane implements MenuInterface, TableCallbackInterface {

    private static final long serialVersionUID = 1L;

    JFrame mainFrame;

    private JSplitPane spTextModel, spTableView;

    private NodeListView myNodeListView;

    /**
	 * Includes all paragraph level nodes, that is, p and table nodes.
	 */
    private ExtendableNodeList listOfNodes;

    private int activeNodeNumber;

    private int numberOfNodes;

    private GeoModelTableViewEntities myGeoModelTableViewEntities;

    private GeoModelTableViewProperties myGeoModelTableViewProperties;

    private boolean showTags = false;

    private GeoModelPersonReg myGeoModelPersonReg;

    private GeoModelPlaceReg myGeoModelPlaceReg;

    private Document geoModelDoc;

    private Map<String, GeoModelElement> geoModelDocNodeIndex = new HashMap<String, GeoModelElement>();

    private Map<String, Integer> geoModelRefNodeIdNodeIndex = new HashMap<String, Integer>();

    private GeoModelLinkSet myGeoModelLinkSet;

    private AddedNodeSet myAddedNodeSet;

    private GeoModelCoref myGeoModelCoref;

    private GeoModelProperties myGeoModelProperties;

    private GeoModelAddedNodes myGeoModelAddedNodes;

    private Map<String, String> geoModelNodeIdToPageNum = new HashMap<String, String>();

    private GeoModelNetworkVisualise myGeoModelNetworkVisualise;

    private GraphTraversal myGraphTraversal;

    /**
	 * Start a runner for the GeoModelText application.
	 * 
	 * @param inFrame
	 *            The frame we put things into.
	 */
    public GeoModelRunner(JFrame inFrame) {
        super(SwingConstants.LEFT);
        mainFrame = inFrame;
        myGeoModelCoref = new GeoModelCoref(this);
        String schnitlerTeiFile = GeoModel.PATH_STORED_FILES + "schnitler-tei-no-formats.xml";
        File fileTest = new File(schnitlerTeiFile);
        if (fileTest.canRead()) geoModelDoc = DomUtils.parseXMLFile(schnitlerTeiFile); else {
            String schnitlerTeiUrl = GeoModel.URL_STORED_FILES + "schnitler-tei-no-formats.xml";
            geoModelDoc = DomUtils.parseXMLUrl(schnitlerTeiUrl);
        }
        populateSchnitlerNodeIndex();
        String xmlMenu = "<?xml version='1.0' encoding='UTF-8'?>" + "<eddMenu>" + "<menu name='File'>" + "<menuItem name='Save' description='Save to RDF file' event='quit' keyStroke='control S'/>" + "<menuItem name='Quit' description='Quit the application' event='quit' keyStroke='control Q'/>" + "</menu>" + "<menu name='Text'>" + "<menuItem name='Next' description='Next paragraph' event='nextText' keyStroke='control shift N'/>" + "<menuItem name='Prev' description='Previous paragraph' event='prevText' keyStroke='control shift P'/>" + "<menuItem name='Goto number' description='Goto the paragraph with this numeric part of its ID' event='gotoText' keyStroke='control shift G'/>" + "<menuItem name='Toggle Show Tags' description='Turn XML tags display on/off' event='toggleShowTags' keyStroke='control shift T'/>" + "</menu>" + "<menu name='Table'>" + "<menuItem name='New Entity Line' description='Add a new line in the entity table' event='quit' keyStroke='control shift E'/>" + "<menuItem name='New Link Line' description='Add a new line in the link table' event='quit' keyStroke='control shift L'/>" + "<menuItem name='Delete Entity' description='Delete the active entity line' event='deleteEnt' keyStroke=''/>" + "<menuItem name='Delete Property' description='Delete the active property line' event='deleteProp' keyStroke=''/>" + "<menuItem name='Get coref info' description='Get information about the coref value of the selected entity' event='getCorefInfo' keyStroke='control shift I'/>" + "</menu>" + "<menu name='Persons'>" + "<menuItem name='List persons' description='List all person elements' event='listPersons' keyStroke=''/>" + "<menuItem name='List persons with paragraphs' description='List all person elements if the person is speaker of one or more paragraphs' event='listPersonsWithPara' keyStroke=''/>" + "<menuItem name='Show persons speaking' description='Show all persons which are speakers of one or more paragraphs, with the text.' event='listPersonsSpeaker' keyStroke=''/>" + "<menuItem name='Show one person' description='Show the information about one person, with the text.' event='showOnePerson' keyStroke=''/>" + "<menuItem name='Person table' description='Show a table with modelling statistics for each person who is a speaker.' event='showPersonTable' keyStroke=''/>" + "</menu>" + "<menu name='Places'>" + "<menuItem name='List place elements' description='List all place elements from the TEI file' event='listPlaceElements' keyStroke=''/>" + "<menuItem name='List places' description='List all place objects' event='listPlaces' keyStroke=''/>" + "<menuItem name='Add place description' description='Add a description to the place in the coref field of the entity table' event='addPlaceDescr' keyStroke=''/>" + "<menuItem name='Get GML' description='Get a GML describing all places with vector data' event='getGML' keyStroke=''/>" + "</menu>" + "<menu name='Coref'>" + "<menuItem name='Load place names' description='Load place name elements' event='loadPlaceNames' keyStroke=''/>" + "<menuItem name='Coref selected' description='Coref selected place name rows with selcted register row' event='corefRows' keyStroke='control shift R'/>" + "<menuItem name='Insert text' description='Include the paragraph text on selected rows' event='textIntoRows' keyStroke=''/>" + "<menuItem name='Popup text untagged' description='Show untagged full text of the first marked row' event='popupText' keyStroke=''/>" + "<menuItem name='Popup text tagged' description='Show tagged full text of the first marked row' event='popupTextTagged' keyStroke=''/>" + "<menuItem name='Coref statistics' description='Some statistical information about co-reference' event='CorefStat' keyStroke=''/>" + "</menu>" + "<menu name='Visualisation'>" + "<menuItem name='Traverse graph' description='Traverse graph' event='traverseGraph' keyStroke=''/>" + "<menuItem name='Traverse speaker' description='Traverse all graphs owned by one speaker' event='traverseGraphSpeaker' keyStroke=''/>" + "<menuItem name='Traverse speaker paragraphs separately' description='Traverse all paragraphs spoken by one speaker separately' event='traverseGraphSpeakerSeparate' keyStroke=''/>" + "</menu>" + "<menu name='Node types'>" + "<menuItem name='List added node types' description='List added node types, content and formalised types' event='AddedNodeTypes' keyStroke=''/>" + "<menuItem name='List link types' description='List link types, content and formalised types' event='LinkTypes' keyStroke=''/>" + "</menu>" + "<menu name='Paras'>" + "<menuItem name='List paragraphs' description='List all paragraph type elements with added nodes or links.' event='listParas' keyStroke=''/>" + "<menuItem name='List paragraphs for a speaker' description='List all paragraph type elements with added nodes or links for one speaker.' event='listParasSpeaker' keyStroke=''/>" + "<menuItem name='List paragraphs for a set of paras' description='List all paragraph type elements with added nodes or links for some paragraphs.' event='listParasParas' keyStroke=''/>" + "<menuItem name='Paragraph summary' description='Summary of one paragraph.' event='onePara' keyStroke=''/>" + "<menuItem name='Speaker paragraph summaries' description='Paragraph summeried for one speaker.' event='oneSpeakerParaSum' keyStroke=''/>" + "</menu>" + "</eddMenu>";
        MenuStorage myMenu = new MenuStorage(xmlMenu, this);
        mainFrame.setJMenuBar(myMenu.getMenuBar());
        Document personDoc;
        String schnitlerPersonFile = GeoModel.PATH_STORED_FILES + "dokub-navnereg-no-dtd.xml";
        fileTest = new File(schnitlerPersonFile);
        if (fileTest.canRead()) personDoc = DomUtils.parseXMLFile(schnitlerPersonFile); else {
            schnitlerPersonFile = GeoModel.URL_STORED_FILES + "dokub-navnereg-no-dtd.xml";
            personDoc = DomUtils.parseXMLFile(schnitlerPersonFile);
        }
        fileTest = new File(schnitlerTeiFile);
        if (fileTest.canRead()) geoModelDoc = DomUtils.parseXMLFile(schnitlerTeiFile); else {
            String schnitlerTeiUrl = GeoModel.URL_STORED_FILES + "schnitler-tei-no-formats.xml";
            geoModelDoc = DomUtils.parseXMLUrl(schnitlerTeiUrl);
        }
        NodeList taxonomyList = personDoc.getElementsByTagName("taxonomy");
        GeoModelPersonTitCat mySchnitlerPersonTitCat;
        try {
            mySchnitlerPersonTitCat = new GeoModelPersonTitCat(taxonomyList);
        } catch (DatatypeConfigurationException e1) {
            printErrMessage(e1.toString());
            mySchnitlerPersonTitCat = null;
        }
        Element thisDiv, linkGrpElem;
        Node divChild;
        String linkGrpType;
        NodeList divChildren;
        NodeList divNodes = personDoc.getElementsByTagName("div");
        for (int i = 0; i < divNodes.getLength(); i++) {
            thisDiv = (Element) divNodes.item(i);
            divChildren = thisDiv.getChildNodes();
            for (int j = 0; j < divChildren.getLength(); j++) {
                divChild = divChildren.item(j);
                if (divChild.getNodeType() == 1 && divChild.getNodeName().equals("listPerson")) try {
                    myGeoModelPersonReg = new GeoModelPersonReg(this, (Element) divChild, mySchnitlerPersonTitCat);
                } catch (NullPointerException e) {
                    printErrMessage(e.toString());
                } else if (divChild.getNodeType() == 1 && divChild.getNodeName().equals("linkGrp")) {
                    linkGrpElem = (Element) divChild;
                    linkGrpType = linkGrpElem.getAttribute("type");
                    if (linkGrpType.equals("elements_in_text")) try {
                        myGeoModelPersonReg.addLinksPersonregNamenodes(linkGrpElem);
                    } catch (DatatypeConfigurationException e) {
                        printErrMessage(e.toString());
                    } else if (linkGrpType.equals("speaker_of_paragraphs")) try {
                        myGeoModelPersonReg.addSpeakersOfParagraphs(linkGrpElem);
                    } catch (DatatypeConfigurationException e) {
                        printErrMessage(e.toString());
                    }
                }
            }
        }
        Document placeDoc;
        String schnitlerPlaceFile = GeoModel.PATH_STORED_FILES + "schnitler438-473.xml";
        fileTest = new File(schnitlerPlaceFile);
        if (fileTest.canRead()) placeDoc = DomUtils.parseXMLFile(schnitlerPlaceFile); else {
            schnitlerPlaceFile = GeoModel.URL_STORED_FILES + "schnitler438-473.xml";
            placeDoc = DomUtils.parseXMLFile(schnitlerPlaceFile);
        }
        NodeList bodyNodes = placeDoc.getElementsByTagName("body");
        if (bodyNodes == null || bodyNodes.getLength() != 1) System.err.println("Feil i stedsregister!"); else {
            Element bodyElem = (Element) bodyNodes.item(0);
            myGeoModelPlaceReg = new GeoModelPlaceReg(this, bodyElem);
        }
        myAddedNodeSet = new AddedNodeSet(this);
        myGeoModelLinkSet = new GeoModelLinkSet(this);
        String rdfStored = readRDF();
        Document rdfDoc;
        NodeList nodesForInsertion;
        if (rdfStored != null && rdfStored.length() > 0) {
            rdfDoc = DomUtils.parseStringAsXML(rdfStored);
            nodesForInsertion = rdfDoc.getElementsByTagName("entity");
            myAddedNodeSet.insertAddedNodes(nodesForInsertion);
            nodesForInsertion = rdfDoc.getElementsByTagName("property");
            myGeoModelLinkSet.insertLinks(nodesForInsertion);
            nodesForInsertion = rdfDoc.getElementsByTagName("personCoref");
            myGeoModelPersonReg.insertCorefs(nodesForInsertion);
            nodesForInsertion = rdfDoc.getElementsByTagName("elementCoref");
            loadElementCorefsFromFile(nodesForInsertion);
            nodesForInsertion = rdfDoc.getElementsByTagName("placeInfo");
            try {
                myGeoModelPlaceReg.setGeoModelPlacesWithGeoInfo(nodesForInsertion);
            } catch (GeoModelException e) {
                printErrMessage("Something wrong in the description for a place.\n" + e.getMessage());
            } catch (GeoLocationException e) {
                printErrMessage("Something wrong in the parsing of a coordinate.\n" + e.getMessage());
            }
            readAndSetActiveNodeNumber(rdfDoc);
        }
        modelText(geoModelDoc);
        populateGeoModelRefNodeIdNodeIndex();
        readPageNums();
        myGeoModelProperties = new GeoModelProperties(this);
        addTab("Property tool", myGeoModelProperties);
        myGeoModelAddedNodes = new GeoModelAddedNodes(this);
        addTab("Added nodes tool", myGeoModelAddedNodes);
        myGeoModelNetworkVisualise = new GeoModelNetworkVisualise(this);
        addTab("Visualise tool", myGeoModelNetworkVisualise);
        myGraphTraversal = new GraphTraversal(this);
        if (GeoModel.DEBUG > 0) spitOutDebug();
    }

    /**
	 * Read the XML DOM document and create the text window and the two tables
	 * on the screen: entities and attributes. This is ran only once.
	 * 
	 * @param doc
	 *            The XML DOM document object based on the main TEI file .
	 */
    private void modelText(Document doc) {
        myNodeListView = new NodeListView(this);
        myGeoModelTableViewEntities = new GeoModelTableViewEntities(this);
        myGeoModelTableViewProperties = new GeoModelTableViewProperties(this);
        spTableView = new JSplitPane(JSplitPane.VERTICAL_SPLIT, myGeoModelTableViewEntities, myGeoModelTableViewProperties);
        spTableView.setDividerLocation(GeoModel.TABLE_DIVIDER);
        spTextModel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, myNodeListView, spTableView);
        spTextModel.setDividerLocation(GeoModel.MAIN_DIVIDER);
        addTab("Model tool", spTextModel);
        addTab("Coref tool", myGeoModelCoref);
        String[] nodeNameList = { "p", "table" };
        listOfNodes = DomUtils.getElementsByTagNames(doc, nodeNameList);
        numberOfNodes = listOfNodes.getNextFree();
        if (numberOfNodes < 1) {
            printErrMessage("To few nodes!");
        } else {
            Node thisNode = listOfNodes.item(activeNodeNumber);
            Element thisElement = (Element) thisNode;
            myNodeListView.renewText(thisNode.getTextContent(), thisElement.getAttribute("xml:id"));
        }
        populateNodeTable();
        myGeoModelCoref.loadPlaceRegTable();
    }

    /**
	 * Increase the active node number (that is, go to the next paragraph type
	 * node) and return a string containing the text of the active node. An XML
	 * fragment or clean text is returned depending on the status of the
	 * showTags flag.
	 * 
	 * @return The paragraph text string.
	 */
    String getTextFromNextNode() {
        if (activeNodeNumber < numberOfNodes - 1) activeNodeNumber++; else printErrMessage("No next node.");
        return getTextFromThisNode();
    }

    /**
	 * Decrease the active node number (that is, go to the previous paragraph
	 * type node) and return a string containing the text of the active node. An
	 * XML fragment or clean text is returned depending on the status of the
	 * showTags flag.
	 * 
	 * @return The paragraph text string.
	 */
    String getTextFromPrevNode() {
        if (activeNodeNumber > 0) activeNodeNumber--; else printErrMessage("No previous node.");
        return getTextFromThisNode();
    }

    /**
	 * Set the active node number (that is, go to the selected paragraph type
	 * node) and return a string containing the text of the active node. An XML
	 * fragment or clean text is returned depending on the status of the
	 * showTags flag.
	 * 
	 * @return The paragraph text string.
	 */
    String getTextFromSelectedNode(int nodeNumber) {
        if (nodeNumber >= 0 && nodeNumber < numberOfNodes) activeNodeNumber = nodeNumber; else printErrMessage("Cannot select node number: " + nodeNumber);
        return getTextFromThisNode();
    }

    /**
	 * Return a string containing the text of the node with this number. An XML
	 * fragment or clean text is returned depending on the tagged parameter.
	 * 
	 * @param nodeNumber
	 *            The xml:id of the node.
	 * @param tagged
	 *            True if tags to be included.
	 * @return The paragraph text string.
	 */
    String getTextFromNodeNumber(String nodeId, boolean tagged) {
        String paraNodeId = getIdOfSurroundingPara(nodeId);
        int paraTypeNodeNumber = geoModelRefNodeIdNodeIndex.get(paraNodeId);
        if (tagged == true) {
            try {
                return DomUtils.xmlNodeContents(listOfNodes.item(paraTypeNodeNumber));
            } catch (IOException e) {
                printErrMessage("getTextFromThisNode: " + e.toString());
                return e.toString();
            }
        } else {
            return listOfNodes.item(paraTypeNodeNumber).getTextContent();
        }
    }

    /**
	 * Return a string containing the text of the active node. An XML fragment
	 * or clean text is returned depending on the status of the showTags flag.
	 * 
	 * @return The paragraph text string.
	 */
    String getTextFromThisNode() {
        if (showTags == true) {
            try {
                return DomUtils.xmlNodeContents(listOfNodes.item(activeNodeNumber));
            } catch (IOException e) {
                printErrMessage("getTextFromThisNode: " + e.toString());
                return e.toString();
            }
        } else {
            return listOfNodes.item(activeNodeNumber).getTextContent();
        }
    }

    /**
	 * Get the xml:id of the active node.
	 * 
	 * @return The xml:id of the active node.
	 */
    String getIdFromThisNode() {
        Element thisElement = (Element) listOfNodes.item(activeNodeNumber);
        return thisElement.getAttribute("xml:id");
    }

    /**
	 * Find a node with a specified xml:id value. Uses the first 4 characters of
	 * the ID to dispatch on node type to look for.
	 * 
	 * @param id
	 *            The xmk:id value
	 * @return the node typed according to the linkable interface.
	 */
    public GeoModelLinkable findNodeWithId(String id) {
        if (id != null && id.length() >= 4) {
            String nodeTypeId = id.substring(0, 4);
            if (nodeTypeId.equals("Schn")) return geoModelDocNodeIndex.get(id); else if (nodeTypeId.equals("pers")) return myGeoModelPersonReg.getGeoModelPersonFromId(id); else if (nodeTypeId.equals("node")) return myAddedNodeSet.getAddedNode(id); else if (nodeTypeId.equals("link")) return myGeoModelLinkSet.getLink(id); else if (nodeTypeId.equals("Sc1P")) return myGeoModelPlaceReg.getGeoModelPlace(id); else {
                printErrMessage("findNodeWithId: Wrong type of ID: " + id);
                return null;
            }
        } else {
            printErrMessage("findNodeWithId: Wrong ID: " + id);
            return null;
        }
    }

    /**
	 * Fint the object with a specified ID value.
	 * 
	 * @param id
	 *            The ID identifying the object.
	 * @return The object according to the interface.
	 * @throws GeoModelLocationException
	 */
    public GeoModelLocation findGeoModelLocation(String id) throws GeoModelLocationException {
        if (id != null && id.length() >= 4) {
            String nodeTypeId = id.substring(0, 4);
            if (nodeTypeId.equals("Sc1P")) return myGeoModelPlaceReg.getGeoModelPlace(id); else throw new GeoModelLocationException("findGeoModelLocation: Wrong type of ID: " + id);
        } else throw new GeoModelLocationException("findGeoModelLocation: Wrong ID: " + id);
    }

    /**
	 * Send in the node id, find a number in the node list for the paragraph.
	 * 
	 * @param nodeId
	 *            A node ID string.
	 * @return The paragraph number (that is, in the listOfNodes nodeList)
	 *         found, or -1 if none found.
	 */
    int findNodeNumberFromNodeId(String nodeId) {
        Integer retInt = geoModelRefNodeIdNodeIndex.get(nodeId);
        if (retInt == null) return -1; else return retInt.intValue();
    }

    /**
	 * Inserts links from the xml:id value to the node for all elements in the
	 * XML DOM.
	 */
    private void populateSchnitlerNodeIndex() {
        Element thisElem;
        NodeList allNodes = geoModelDoc.getElementsByTagName("*");
        for (int i = 0; i < allNodes.getLength(); i++) {
            thisElem = (Element) allNodes.item(i);
            geoModelDocNodeIndex.put(thisElem.getAttribute("xml:id"), new GeoModelElement(this, thisElem));
        }
    }

    /**
	 * Inserts links from all existing node ID values to the corresponding item
	 * in the listOfNodes.
	 */
    private void populateGeoModelRefNodeIdNodeIndex() {
        for (int i = 0; i < numberOfNodes; i++) {
            Node thisNode = listOfNodes.item(i);
            Element thisElem = (Element) thisNode;
            String thisNodeId = thisElem.getAttribute("xml:id");
            geoModelRefNodeIdNodeIndex.put(thisNodeId, i);
        }
    }

    /**
	 * Finish working on entity and attribute tables for one paragraph type
	 * element. Inserts new data from the tables into the various objects, and
	 * clears the tables to make them ready for the next paragraph type element.
	 */
    void leaveNodeTable() {
        String newProperties = myGeoModelTableViewProperties.dumpTable();
        if (GeoModel.DEBUG >= 2) System.out.println("From prop table: " + newProperties);
        Document linksDoc = DomUtils.parseStringAsXML(newProperties);
        myGeoModelLinkSet.insertLinks(linksDoc);
        String newEntities = myGeoModelTableViewEntities.dumpTable();
        if (GeoModel.DEBUG >= 2) System.out.println("From ent table: " + newEntities);
        try {
            Document nodesDoc = DomUtils.parseStringAsXML(newEntities);
            myAddedNodeSet.insertAddedNodes(nodesDoc);
            updateChangedCorefs(nodesDoc);
        } catch (NullPointerException e) {
            printErrMessage(e.toString());
        }
        myGeoModelTableViewProperties.clearTable();
        myGeoModelTableViewEntities.clearTable();
    }

    private void loadElementCorefsFromFile(NodeList nodesForInsertion) {
        Element rowElem;
        String elementId, corefId;
        GeoModelElement thisGeoModelElement;
        boolean setResult;
        for (int i = 0; i < nodesForInsertion.getLength(); i++) {
            rowElem = (Element) nodesForInsertion.item(i);
            elementId = rowElem.getElementsByTagName("elementId").item(0).getTextContent();
            thisGeoModelElement = geoModelDocNodeIndex.get(elementId);
            if (thisGeoModelElement != null) {
                corefId = rowElem.getElementsByTagName("corefId").item(0).getTextContent();
                setResult = thisGeoModelElement.setCorefId(corefId);
                if (GeoModel.DEBUG > 1) System.out.println("Set coref success on " + elementId + ": " + setResult);
            } else if (GeoModel.DEBUG >= 1) printErrMessage("What? No GeoModelLinkable object? " + elementId);
        }
    }

    private void updateChangedCorefs(Document nodesDoc) {
        NodeList rows = nodesDoc.getElementsByTagName("row");
        Element rowElem;
        String nodeId, corefId;
        GeoModelLinkable thisGeoModelLinkable;
        boolean setResult;
        for (int i = 0; i < rows.getLength(); i++) {
            rowElem = (Element) rows.item(i);
            nodeId = rowElem.getElementsByTagName("nodeId").item(0).getTextContent();
            thisGeoModelLinkable = findNodeWithId(nodeId);
            if (thisGeoModelLinkable != null) {
                corefId = rowElem.getElementsByTagName("corefId").item(0).getTextContent();
                setResult = thisGeoModelLinkable.setCorefId(corefId);
                if (GeoModel.DEBUG > 1) System.out.println("Set coref success on " + nodeId + ": " + setResult);
            } else printErrMessage("What? No GeoModelLinkable object? " + nodeId);
        }
    }

    /**
	 * Insert values from the various objects connected to one paragraph type
	 * element into the entity and property tables.
	 */
    void populateNodeTable() {
        Element activeNode = (Element) listOfNodes.item(activeNodeNumber);
        String mainNodeId = activeNode.getAttribute("xml:id");
        GeoModelPerson speaker = myGeoModelPersonReg.getGeoModelPersonFromSpeakerNodeId(mainNodeId);
        if (speaker != null) {
            myGeoModelTableViewEntities.putLine(speaker.getId(), "speaker", speaker.getMainName(), speaker.getCorefId(), "");
        }
        Element nameElem;
        NodeList listNodes = activeNode.getElementsByTagName("name");
        for (int i = 0; i < listNodes.getLength(); i++) {
            nameElem = (Element) listNodes.item(i);
            String nodeId = nameElem.getAttribute("xml:id");
            String nameType = nameElem.getAttribute("type");
            String corefId = nodeId;
            String placeInfo = "";
            GeoModelElement thisElementObj = geoModelDocNodeIndex.get(nodeId);
            if (thisElementObj != null) corefId = thisElementObj.getCorefId(); else if (GeoModel.DEBUG >= 1) System.err.println("No GeoModelElement for this node: " + nodeId);
            try {
                placeInfo = findGeoModelLocation(corefId).getGeographicalDescription();
            } catch (GeoModelLocationException e) {
                if (GeoModel.DEBUG >= 3) System.err.println("An element without geographical description in coref place: " + corefId);
            }
            String nodeString = nameElem.getTextContent();
            myGeoModelTableViewEntities.putLine(nodeId, "name: " + nameType, nodeString, corefId, placeInfo);
        }
        listNodes = activeNode.getElementsByTagName("date");
        for (int i = 0; i < listNodes.getLength(); i++) {
            nameElem = (Element) listNodes.item(i);
            String nodeId = nameElem.getAttribute("xml:id");
            String dateReg = nameElem.getAttribute("when");
            String nodeString = nameElem.getTextContent();
            myGeoModelTableViewEntities.putLine(nodeId, "date: " + dateReg, nodeString, nodeId, "");
        }
        String idNumThisNode = myNodeListView.getIdNumThisNode();
        if (GeoModel.DEBUG > 1) System.out.println("Idnum this node: " + idNumThisNode);
        AddedNode[] addedNodes = myAddedNodeSet.getAddedNodeByParent(idNumThisNode);
        if (addedNodes != null) {
            AddedNode thisAddedNode;
            String addedNodeId, addedNodeCorefId;
            String placeInfo = "";
            for (int i = 0; i < addedNodes.length; i++) {
                thisAddedNode = addedNodes[i];
                if (thisAddedNode != null) {
                    addedNodeId = thisAddedNode.getId();
                    addedNodeCorefId = thisAddedNode.getCorefId();
                    try {
                        placeInfo = findGeoModelLocation(addedNodeCorefId).getGeographicalDescription();
                    } catch (GeoModelLocationException e) {
                        if (GeoModel.DEBUG >= 3) System.err.println("An element without geographical description in coref place: " + addedNodeCorefId);
                    }
                    myGeoModelTableViewEntities.putLine(addedNodeId, thisAddedNode.getType(), thisAddedNode.getContent(), addedNodeCorefId, placeInfo);
                }
            }
        }
        if (GeoModel.DEBUG >= 3) myAddedNodeSet.dump();
        String[] entIds = myGeoModelTableViewEntities.getAllNodeIds();
        for (int i = 0; i < entIds.length; i++) addLinkToTable(entIds[i]);
        if (GeoModel.DEBUG >= 3) myGeoModelLinkSet.dump();
    }

    /**
	 * Insert stored links into the entities tables.
	 * 
	 * @param nodeId
	 *            The entity that may be in either end of the link.
	 */
    private void addLinkToTable(String nodeId) {
        GeoModelLink[] linksFrom = myGeoModelLinkSet.getLinkFrom(nodeId);
        if (linksFrom != null) {
            GeoModelLink thisLink;
            String linkId;
            for (int i = 0; i < linksFrom.length; i++) {
                thisLink = linksFrom[i];
                if (thisLink != null) {
                    linkId = thisLink.getLinkIdString();
                    if (myGeoModelTableViewProperties.linkInTable(linkId) == false) {
                        if (GeoModel.DEBUG > 1) System.out.println("Link from: " + thisLink.getType());
                        myGeoModelTableViewProperties.putLine(linkId, thisLink.getType(), thisLink.getFrom().getId(), thisLink.getTo().getId());
                    }
                }
            }
        }
        GeoModelLink[] linksTo = myGeoModelLinkSet.getLinkTo(nodeId);
        if (linksTo != null) {
            GeoModelLink thisLink;
            String linkId;
            for (int i = 0; i < linksTo.length; i++) {
                thisLink = linksTo[i];
                if (thisLink != null) {
                    linkId = thisLink.getLinkIdString();
                    if (myGeoModelTableViewProperties.linkInTable(linkId) == false) {
                        if (GeoModel.DEBUG > 1) System.out.println("Link to: " + thisLink.getType());
                        myGeoModelTableViewProperties.putLine(linkId, thisLink.getType(), thisLink.getFrom().getId(), thisLink.getTo().getId());
                    }
                }
            }
        }
    }

    public void menuChoice(String choice) {
        if (GeoModel.DEBUG >= 2) System.out.println("Menu choice: " + choice);
        if (choice.equals("Next")) {
            myNodeListView.nextPara();
        } else if (choice.equals("Prev")) {
            myNodeListView.prevPara();
        } else if (choice.equals("Goto number")) {
            String numString = MessageViewer.getInput(mainFrame, "Number from paragraph xml:id:");
            try {
                int numInt = Integer.parseInt(numString);
                myNodeListView.gotoPara(numInt);
            } catch (NumberFormatException e1) {
                printErrMessage("Wrong number node ID: " + numString);
            } catch (IllegalArgumentException e2) {
                printErrMessage(e2.getMessage() + ": " + numString);
            }
        } else if (choice.equals("Toggle Show Tags")) {
            if (showTags == true) showTags = false; else showTags = true;
            myNodeListView.refresh();
        } else if (choice.equals("New Entity Line")) {
            String newId = myAddedNodeSet.createEmptyAddedNode();
            myGeoModelTableViewEntities.putLine(newId, "", "", "", "");
        } else if (choice.equals("New Link Line")) {
            String newId = myGeoModelLinkSet.createEmptyLink();
            myGeoModelTableViewProperties.putLine(newId, "", "", "");
        } else if (choice.equals("Delete Entity")) {
            removeFirstSelectedTableRow("Entities");
        } else if (choice.equals("Delete Property")) {
            removeFirstSelectedTableRow("Properties");
        } else if (choice.equals("Get coref info")) {
            String corefId = myGeoModelTableViewEntities.getCorefIdSelectedRow();
            if (corefId != null && !corefId.equals("")) MessageViewer.longMessage(this.mainFrame, findNodeWithId(corefId).getInfoHtml(), "Information", "text/html", 100, 100, 700, 600);
        } else if (choice.equals("Save")) {
            if (GeoModel.DEBUG > 0) TimeUtils.stampTime("Save chosen");
            myNodeListView.nextPara();
            myNodeListView.prevPara();
            saveRDF(getAllRdf());
            if (GeoModel.DEBUG > 0) TimeUtils.stampTime("Save finished");
        } else if (choice.equals("List persons")) {
            MessageViewer.longMessage(mainFrame, myGeoModelPersonReg.FormatAllPersonsFull(), "All persons", "text/plain", 100, 100, 700, 600);
        } else if (choice.equals("List persons with paragraphs")) {
            MessageViewer.longMessage(mainFrame, myGeoModelPersonReg.FormatAllPersonsWithParagraphsFull(), "All persons with paragrpahs", "text/plain", 100, 100, 700, 600);
        } else if (choice.equals("Show persons speaking")) {
            MessageViewer.longMessage(mainFrame, myGeoModelPersonReg.FormatAllSpeakersWithText(), "All persons who are speakers", "text/plain", 100, 100, 700, 600);
        } else if (choice.equals("Show one person")) {
            String numString = MessageViewer.getInput(mainFrame, "Speaker number:");
            try {
                int numInt = Integer.parseInt(numString);
                GeoModelPerson printPerson = myGeoModelPersonReg.getGeoModelPersonFromId(numInt);
                if (printPerson == null) printErrMessage("Person does not exist: " + numString); else {
                    MessageViewer.longMessage(mainFrame, printPerson.formatPersonFullWithTextHtml(), "All persons who are speakers", "text/html", 100, 100, 700, 600);
                }
            } catch (NumberFormatException e) {
                printErrMessage("Wrong number for person: " + numString);
            }
        } else if (choice.equals("Person table")) {
            MessageViewer.longMessage(mainFrame, myGeoModelPersonReg.tableOfSpeakingPersons(), "All speaking persons", "text/html", 100, 100, 700, 600);
        } else if (choice.equals("List paragraphs")) {
            String paraStat = getStatForAllParas();
            MessageViewer.longMessage(mainFrame, paraStat, "Added data to paragraph level nodes", "text/html", 100, 100, 1000, 600);
        } else if (choice.equals("List paragraphs for a speaker")) {
            String numString = MessageViewer.getInput(mainFrame, "Speaker number:");
            try {
                int numInt = Integer.parseInt(numString);
                String paraStat = getStatForAllParas(numInt);
                MessageViewer.longMessage(mainFrame, paraStat, "Added data to paragraph level nodes", "text/html", 100, 100, 1000, 600);
            } catch (NumberFormatException e) {
                printErrMessage("Wrong number for person: " + numString);
            }
        } else if (choice.equals("List paragraphs for a set of paras")) {
            String firstPara = MessageViewer.getInput(mainFrame, "First paragraph (full ID): ");
            String lastPara = MessageViewer.getInput(mainFrame, "Last paragraph (full ID): ");
            try {
                int firstParaIndex = findNodeNumberFromNodeId(firstPara);
                int lastParaIndex = findNodeNumberFromNodeId(lastPara);
                String[] paraIds = new String[lastParaIndex - firstParaIndex + 1];
                int j = 0;
                Element thisElem;
                for (int i = firstParaIndex; i <= lastParaIndex; i++) {
                    thisElem = (Element) listOfNodes.getElem(i);
                    paraIds[j++] = (thisElem.getAttribute("xml:id"));
                }
                String paraStat = getStatForAllParas(paraIds);
                MessageViewer.longMessage(mainFrame, paraStat, "Added data to paragraph level nodes", "text/html", 100, 100, 1000, 600);
            } catch (Exception e) {
                printErrMessage("Wrong IDs for paragraphs: " + firstPara + ", " + lastPara);
            }
        } else if (choice.equals("Paragraph summary")) {
            String[] paraIdArr = new String[1];
            try {
                paraIdArr[0] = "Schn1_" + Integer.parseInt(MessageViewer.getInput(mainFrame, "Paragraph id:"));
                String paraStat = getStatForParaList(paraIdArr);
                MessageViewer.longMessage(mainFrame, paraStat, "All about paragraphs", "text/html", 100, 100, 1000, 600);
            } catch (NumberFormatException e) {
                printErrMessage("Wrong number for paragraph: " + paraIdArr[0]);
            }
        } else if (choice.equals("Speaker paragraph summaries")) {
            String numString = MessageViewer.getInput(mainFrame, "Speaker number:");
            try {
                int numInt = Integer.parseInt(numString);
                GeoModelPerson printPerson = myGeoModelPersonReg.getGeoModelPersonFromId(numInt);
                if (printPerson == null) printErrMessage("Person does not exist: " + numString); else {
                    Object[] ParaElemsArr = printPerson.getParasWhereIAmSpeaker().getListNoNulls();
                    String[] paraIdArr = new String[ParaElemsArr.length];
                    GeoModelElement thisElem;
                    for (int i = 0; i < ParaElemsArr.length; i++) {
                        thisElem = (GeoModelElement) ParaElemsArr[i];
                        paraIdArr[i] = thisElem.getId();
                    }
                    String path = "/Users/oeide_loc/Documents/dg/schnitlerMap/layerMaps/";
                    String summaries = getStatForParaList(paraIdArr);
                    MessageViewer.longMessage(mainFrame, summaries, "All paragraphs for person " + numInt, "text/html", 100, 100, 700, 600);
                    PrintWriter pw = new PrintWriter(path + "speakerParas.html", "UTF-8");
                    pw.print(summaries);
                    pw.close();
                }
            } catch (NumberFormatException e) {
                printErrMessage("Wrong number for person: " + numString);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (choice.equals("List place elements")) {
            MessageViewer.longMessage(mainFrame, "Test", "Added data to paragraph level nodes", "text/html", 100, 100, 1000, 600);
        } else if (choice.equals("List places")) {
            String htmlVersion = myGeoModelPlaceReg.getInfoHtml();
            MessageViewer.longMessage(mainFrame, htmlVersion, "The place register", "text/html", 100, 100, 1000, 600);
        } else if (choice.equals("Add place description")) {
            String idPlaceObject = myGeoModelTableViewEntities.getCorefIdSelectedRow();
            try {
                GeoModelLocation thisLocation = findGeoModelLocation(idPlaceObject);
                thisLocation.setGeographicalDescription(MessageViewer.getInput(mainFrame, "Give a geographical description for place " + idPlaceObject, thisLocation.getGeographicalDescription()));
            } catch (GeoModelLocationException e) {
                MessageViewer.errMessage(mainFrame, "Could not add a description to this one.\n" + e.getMessage());
            } catch (GeoModelException e) {
                printErrMessage("Something wrong in the description for a place.\n" + e.getMessage());
            }
        } else if (choice.equals("Get GML")) {
            MessageViewer.longMessage(mainFrame, myGeoModelPlaceReg.asGML(), "GML of place register", "text/plain", 100, 100, 1000, 600);
        } else if (choice.equals("Load place names")) {
            myGeoModelCoref.getMyGeoModelCorefDashboard().loadPlaceNames();
        } else if (choice.equals("Coref selected")) {
            myGeoModelCoref.getMyGeoModelCorefDashboard().corefRows();
        } else if (choice.equals("Insert text")) {
            myGeoModelCoref.getMyGeoModelCorefDashboard().setTextIntoRows();
        } else if (choice.equals("Popup text untagged")) {
            myGeoModelCoref.getMyGeoModelCorefDashboard().popupText(false);
        } else if (choice.equals("Popup text tagged")) {
            myGeoModelCoref.getMyGeoModelCorefDashboard().popupText(true);
        } else if (choice.equals("Coref statistics")) {
            MessageViewer.longMessage(mainFrame, myGeoModelCoref.toString(), "Coref statistics", "text/plain", 100, 100, 500, 600);
        } else if (choice.equals("Traverse graph")) {
            int firstPara = Integer.parseInt(MessageViewer.getInput(mainFrame, "First paragraph (number only): "));
            int lastPara = Integer.parseInt(MessageViewer.getInput(mainFrame, "Last paragraph (number only): "));
            String rdf = "";
            try {
                rdf = myGraphTraversal.traverseAllLinks(firstPara, lastPara);
            } catch (GeoModelLocationException e) {
                printErrMessage(e.toString());
            }
            MessageViewer.longMessage(mainFrame, rdf, "Tabular version of graph based on paragraph " + firstPara + " through " + lastPara + ".", "text/plain", 100, 100, 1000, 600);
        } else if (choice.equals("Traverse speaker")) {
            String speakerId = MessageViewer.getInput(mainFrame, "Restrict to speaker: ");
            String rdf = "";
            try {
                rdf = myGraphTraversal.traverseAllLinks(speakerId);
            } catch (GeoModelLocationException e) {
                printErrMessage(e.toString());
            }
            MessageViewer.longMessage(mainFrame, rdf, "Tabular version of graph based on speaker " + speakerId + ".", "text/plain", 100, 100, 1000, 600);
        } else if (choice.equals("Traverse speaker paragraphs separately")) {
            String speakerId = MessageViewer.getInput(mainFrame, "Restrict to speaker: ");
            String[] paras = myGeoModelPersonReg.getGeoModelPersonFromId(Integer.parseInt(speakerId)).getIdToParasWhereIAmSpeaker().getListNoNulls();
            try {
                for (int i = 0; i < paras.length; i++) myGraphTraversal.traverseAllLinks(Integer.parseInt(paras[i].substring(6)), Integer.parseInt(paras[i].substring(6)));
            } catch (GeoModelLocationException e) {
                printErrMessage(e.toString());
            }
            MessageViewer.infoMessage(mainFrame, "OK. See output files for results.");
        } else if (choice.equals("List added node types")) {
            MessageViewer.longMessage(mainFrame, myAddedNodeSet.getAddedNodeTypesHtmlTable(), "Added node types", "text/html", 100, 100, 1000, 600);
        } else if (choice.equals("List link types")) {
            MessageViewer.longMessage(mainFrame, myGeoModelLinkSet.getLinkTypesHtmlTable(), "Link types", "text/html", 100, 100, 1000, 600);
        } else if (choice.equals("Quit")) {
            int answer = MessageViewer.question(mainFrame, "Save?");
            if (answer == 0) {
                myNodeListView.nextPara();
                myNodeListView.prevPara();
                saveRDF(getAllRdf());
                System.exit(0);
            } else if (answer == 1) {
                System.exit(0);
            }
        } else {
            System.err.println("Wrong menu choice: " + choice);
        }
    }

    private String getAllRdf() {
        if (GeoModel.DEBUG > 0) TimeUtils.stampTime("getAllRdf starts. ");
        String rdfSet = myGeoModelLinkSet.getLinkSet() + myAddedNodeSet.getAddedNodeSet() + myGeoModelPersonReg.getGeoModelPersonsWithChangedCoref() + myGeoModelPlaceReg.getGeoModelPlacesWithGeoInfo();
        if (GeoModel.DEBUG > 0) TimeUtils.stampTime("getAllRdf will add values from the Elements. ");
        Collection<GeoModelElement> collNodes = geoModelDocNodeIndex.values();
        Iterator<GeoModelElement> callNodesIter = collNodes.iterator();
        StringBuffer rdfSetElem = new StringBuffer("");
        while (callNodesIter.hasNext()) {
            rdfSetElem.append(callNodesIter.next().getRdf());
        }
        rdfSet = rdfSet + rdfSetElem.toString();
        if (GeoModel.DEBUG > 0) TimeUtils.stampTime("getAllRdf ends. ");
        return rdfSet;
    }

    /**
	 * Removes the selected row in one of the tables. If more than one line is
	 * selected, the first one is removed.
	 * 
	 * @param tableType
	 *            The table type from which a row is to be deleted: "Entities"
	 *            or "Properties".
	 */
    private void removeFirstSelectedTableRow(String tableType) {
        int rowNum = -1;
        String idEnt = null, idProp = null;
        int yesOrNo;
        AddedNode addedNodeToBeRemoved;
        GeoModelLink linkToBeRemoved;
        boolean success;
        if (tableType.equals("Entities")) {
            rowNum = myGeoModelTableViewEntities.getSelectedRow();
            if (rowNum >= 0) idEnt = myGeoModelTableViewEntities.getNodeIdInRow(rowNum);
        } else if (tableType.equals("Properties")) {
            rowNum = myGeoModelTableViewProperties.getSelectedRow();
            if (rowNum >= 0) idProp = myGeoModelTableViewProperties.getNodeIdInRow(rowNum);
        }
        if (tableType.equals("Entities") && idEnt != null) {
            yesOrNo = MessageViewer.question(mainFrame, "Delete entity line with ID " + idEnt + "?");
            if (yesOrNo == 0) {
                addedNodeToBeRemoved = myAddedNodeSet.getAddedNode(idEnt);
                if (addedNodeToBeRemoved == null) MessageViewer.infoMessage(mainFrame, "Not able to remove this row."); else {
                    if (myGeoModelLinkSet.getLinkFrom(idEnt) != null || myGeoModelLinkSet.getLinkTo(idEnt) != null) MessageViewer.infoMessage(mainFrame, "Not able to remove this row: Link(s) exist."); else {
                        success = addedNodeToBeRemoved.removeObject();
                        if (!success) MessageViewer.infoMessage(mainFrame, "Not able to remove this row."); else myGeoModelTableViewEntities.removeTableRow(rowNum);
                    }
                }
            }
        } else if (tableType.equals("Properties") && idProp != null) {
            yesOrNo = MessageViewer.question(mainFrame, "Delete property line with ID " + idProp + "?");
            if (yesOrNo == 0) {
                linkToBeRemoved = myGeoModelLinkSet.getLink(idProp);
                if (linkToBeRemoved == null) MessageViewer.infoMessage(mainFrame, "Not able to remove this row."); else {
                    success = linkToBeRemoved.removeObject();
                    if (!success) MessageViewer.infoMessage(mainFrame, "Not able to remove this row."); else myGeoModelTableViewProperties.removeTableRow(rowNum);
                }
            }
        } else MessageViewer.infoMessage(mainFrame, "No row selected. Nothing is removed.");
    }

    /**
	 * Getter for the NodeListView object.
	 * 
	 * @return the object.
	 */
    NodeListView getNodeListView() {
        return myNodeListView;
    }

    /**
	 * Save the data added by the operator to disk. TODO: Should be RDF.
	 * 
	 * @param rdfString
	 *            The string containing data from the object, to be encapsulated
	 *            in a root XML element.
	 */
    private void saveRDF(String rdfString) {
        try {
            rdfString = "<table activeNodeNumber='" + getActiveNodeNumber() + "'>\n" + rdfString + "</table>";
            FileWriter fileStream = new FileWriter(GeoModel.PATH_STORED_FILES + "rdfVersion.xml");
            BufferedWriter writeBuf = new BufferedWriter(fileStream);
            writeBuf.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            writeBuf.write(rdfString);
            writeBuf.close();
            File savedFile = new File(GeoModel.PATH_STORED_FILES + "rdfVersion.xml");
            File backupFile = new File(GeoModel.PATH_STORED_FILES + "rdfOldSaves/rdfBck" + new Date().getTime() + ".xml");
            boolean fileIsNew = backupFile.createNewFile();
            if (fileIsNew == true) FileUtils.copy(savedFile, backupFile); else printErrMessage("Saving: Backup file already exists!");
            printInfoMessage("File saved.");
        } catch (IOException e) {
            printErrMessage("Could not save file!");
        }
    }

    /**
	 * Read the data added by the operator in previous sessions from disk.
	 * 
	 * TODO: Should be RDF.
	 * 
	 * If file is not found, it is assumed this is a web based demo session, and
	 * this fact is added to the title of the frame.
	 * 
	 * @return
	 */
    private String readRDF() {
        try {
            FileReader fileStream = new FileReader(GeoModel.PATH_STORED_FILES + "rdfVersion.xml");
            BufferedReader readBuf = new BufferedReader(fileStream);
            StringBuilder contents = new StringBuilder();
            String line;
            while ((line = readBuf.readLine()) != null) {
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
            readBuf.close();
            return contents.toString();
        } catch (IOException e) {
            MessageViewer.infoMessage(mainFrame, "You are running a demo version using data files fetched from the web.\nResults cannot be saved.");
            mainFrame.setTitle(mainFrame.getTitle() + ". You are running a demo version using data files fetched from the web. Results cannot be saved.");
            URL urlRdf;
            try {
                urlRdf = new URL(GeoModel.URL_STORED_FILES + "rdfVersion.xml");
                URLConnection urlConnRdf = urlRdf.openConnection();
                BufferedReader urlReader = new BufferedReader(new InputStreamReader(urlConnRdf.getInputStream()));
                StringBuilder contents = new StringBuilder();
                String line;
                while ((line = urlReader.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
                urlReader.close();
                return contents.toString();
            } catch (Exception e1) {
                return null;
            }
        }
    }

    /**
	 * Return the node ID value from a specific line in the entity table.
	 * 
	 * @param rowNum
	 *            The row number in the table.
	 * @return The ID value of the record in this row. If the low number is out
	 *         of bonds, null is returned.
	 */
    String getNodeIdFromEntTable(int rowNum) {
        return myGeoModelTableViewEntities.getNodeIdInRow(rowNum);
    }

    public void rowSelected(String rowValue, String tableType) {
        if (GeoModel.DEBUG >= 3) System.out.println("Row selected: " + rowValue);
    }

    public void tableEscape() {
        if (GeoModel.DEBUG >= 3) System.out.println("Table escaped");
    }

    public void cellLeft(int column, int row) {
        EddTableModel thisTableModel = myGeoModelTableViewProperties.getTableDialog().getTableModel();
        String rowNumString = (String) thisTableModel.getValueAt(row, column);
        try {
            int rowNum = Integer.parseInt(rowNumString);
            String id = getNodeIdFromEntTable(rowNum);
            thisTableModel.setValueAt(id, row, column);
        } catch (NumberFormatException e) {
            if (GeoModel.DEBUG >= 2) System.out.println("GeoModelRunner.cellLeft: Not row number: " + rowNumString + ". We consider this as a real id value.");
        }
    }

    /**
	 * Prints an error message in a popup window.
	 * 
	 * @param mess
	 *            The error message.
	 */
    private void printErrMessage(String mess) {
        MessageViewer.errMessage(mainFrame, mess);
    }

    /**
	 * Prints an info message in a popup window.
	 * 
	 * @param mess
	 *            The info message.
	 */
    private void printInfoMessage(String mess) {
        MessageViewer.infoMessage(mainFrame, mess);
    }

    /**
	 * Find the active node number from a stored XML file being retrieved, and
	 * set it.
	 * 
	 * @param loadedDoc
	 *            The XML DOM version of the retrieved file.
	 */
    private void readAndSetActiveNodeNumber(Document loadedDoc) {
        Element tableElem = (Element) loadedDoc.getElementsByTagName("table").item(0);
        String nodeNumStr = tableElem.getAttribute("activeNodeNumber");
        try {
            activeNodeNumber = Integer.parseInt(nodeNumStr);
        } catch (NumberFormatException e) {
            printErrMessage("readAndSetActiveNodeNumber: Could not read active node number: " + nodeNumStr);
        }
    }

    /**
	 * Getter for the active node number.
	 * 
	 * @return The current active node number.
	 */
    private int getActiveNodeNumber() {
        return activeNodeNumber;
    }

    /**
	 * Get information about all paragraph type elements with added nodes or
	 * links.
	 * 
	 * @return The overview in HTML format.
	 */
    private String getStatForAllParas() {
        String tableHeaders = "<tr><th>Para num</th>" + "<th>Characters</th>" + "<th>Speaker</th>" + "<th>Person names</th>" + "<th>Place names</th>" + "<th>Dates</th>" + "<th>Added nodes</th>" + "<th>Links from</th>" + "<th>Links to</th>" + "<th>Text excerpt</th></tr>\n";
        String returnString = DomUtils.getXhtmlHeader("Added data to paragraph level nodes") + "<table border='1'>\n" + tableHeaders;
        AddedNode[] addedNodes;
        GeoModelLink[] linksFrom, linksTo;
        Element paraElem, childElem;
        String parentNodeId, textExcerpt, childNodeId, nameType;
        ExtendableObjectList listOfChildIds;
        int countAddedNodes, countLinksFrom, countLinksTo, stringLength, countPersNames, countPlaceNames, countDates, lineCount = 0;
        String speakerId;
        for (int i = 0; i < listOfNodes.getNextFree(); i++) {
            paraElem = (Element) listOfNodes.item(i);
            parentNodeId = paraElem.getAttribute("xml:id");
            countAddedNodes = 0;
            countLinksFrom = 0;
            countLinksTo = 0;
            speakerId = "";
            countPersNames = 0;
            countPlaceNames = 0;
            countDates = 0;
            listOfChildIds = new ExtendableObjectList();
            GeoModelPerson speaker = myGeoModelPersonReg.getGeoModelPersonFromSpeakerNodeId(parentNodeId);
            if (speaker != null) {
                listOfChildIds.addElem(speaker.getId());
                speakerId = speaker.getId();
            }
            NodeList listNodes = paraElem.getElementsByTagName("name");
            for (int k = 0; k < listNodes.getLength(); k++) {
                childElem = (Element) listNodes.item(k);
                childNodeId = childElem.getAttribute("xml:id");
                listOfChildIds.addElem(childNodeId);
                nameType = childElem.getAttribute("type");
                if (nameType.equals("person")) countPersNames++; else if (nameType.equals("place")) countPlaceNames++; else printErrMessage("getStatForAllParas: Wrong type for name! XML:ID: " + parentNodeId);
            }
            listNodes = paraElem.getElementsByTagName("date");
            for (int k = 0; k < listNodes.getLength(); k++) {
                childElem = (Element) listNodes.item(k);
                childNodeId = childElem.getAttribute("xml:id");
                listOfChildIds.addElem(childNodeId);
                countDates++;
            }
            addedNodes = myAddedNodeSet.getAddedNodeByParent(parentNodeId);
            if (addedNodes != null) {
                AddedNode thisAddedNode;
                for (int k = 0; k < addedNodes.length; k++) {
                    thisAddedNode = addedNodes[k];
                    if (thisAddedNode != null) {
                        childNodeId = thisAddedNode.getId();
                        listOfChildIds.addElem(childNodeId);
                        countAddedNodes++;
                    }
                }
            }
            for (int j = 0; j < listOfChildIds.getNextFree(); j++) {
                if (listOfChildIds.getElem(j) != null) {
                    childNodeId = (String) listOfChildIds.getElem(j);
                    linksFrom = myGeoModelLinkSet.getLinkFrom(childNodeId);
                    countLinksFrom += EddDataUtils.countArrayNonnull(linksFrom);
                    linksTo = myGeoModelLinkSet.getLinkTo(childNodeId);
                    countLinksTo += EddDataUtils.countArrayNonnull(linksTo);
                }
            }
            if (countAddedNodes > 0 || countLinksFrom > 0 || countLinksTo > 0) {
                textExcerpt = paraElem.getTextContent();
                stringLength = textExcerpt.length();
                if (stringLength > 50) textExcerpt = textExcerpt.substring(0, 48) + "...";
                returnString = returnString + "<tr align='right'><td>" + parentNodeId + "</td><td>" + stringLength + "</td><td>" + speakerId + "</td><td>" + countPersNames + "</td><td>" + countPlaceNames + "</td><td>" + countDates + "</td><td>" + countAddedNodes + "</td><td>" + countLinksFrom + "</td><td>" + countLinksTo + "</td><td align='left'>" + textExcerpt + "</td></tr>\n";
                if (lineCount++ > 20) {
                    returnString = returnString + tableHeaders;
                    lineCount = 0;
                }
            }
        }
        return returnString + DomUtils.getXhtmlFooter();
    }

    /**
	 * Get information about all paragraph type elements with added nodes or
	 * links. Includes grand totals.
	 * 
	 * @param personId
	 *            The speaker to include the paragraphs of.
	 * @return The overview in HTML format.
	 */
    private String getStatForAllParas(int personId) {
        ExtendableStringList listOfParas = (ExtendableStringList) myGeoModelPersonReg.getGeoModelPersonFromId(personId).getIdToParasWhereIAmSpeaker();
        return getStatForAllParas(listOfParas.getListNoNulls());
    }

    /**
	 * Get information about all paragraph type elements with added nodes or
	 * links. Includes grand totals.
	 * 
	 * @param parasToList
	 *            A list of the paragraph level elements to be used.
	 * @return The overview in HTML format.
	 */
    private String getStatForAllParas(String[] parasToList) {
        String tableHeaders = "<tr><th>Para num</th>" + "<th>Characters</th>" + "<th>Speaker</th>" + "<th>Person names</th>" + "<th>Place names</th>" + "<th>Dates</th>" + "<th>Added nodes</th>" + "<th>Links from</th>" + "<th>Links to</th>" + "<th>Text excerpt</th></tr>\n";
        String returnString = DomUtils.getXhtmlHeader("Added data to paragraph level nodes") + "<table border='1'>\n" + tableHeaders;
        AddedNode[] addedNodes;
        GeoModelLink[] linksFrom, linksTo;
        Element paraElem, childElem;
        String parentNodeId, textExcerpt, childNodeId, nameType;
        ExtendableObjectList listOfChildIds;
        int countAddedNodes, countLinksFrom, countLinksTo, stringLength, countPersNames, countPlaceNames, countDates, lineCount = 0;
        int totalAddedNodes = 0, totalLinksFrom = 0, totalLinksTo = 0, totalStringLength = 0, totalPersNames = 0, totalPlaceNames = 0, totalDates = 0;
        ExtendableStringList listOfAllPlaceIds = new ExtendableStringList();
        String speakerId;
        for (int i = 0; i < parasToList.length; i++) {
            paraElem = (Element) listOfNodes.getElem(findNodeNumberFromNodeId(parasToList[i]));
            parentNodeId = parasToList[i];
            countAddedNodes = 0;
            countLinksFrom = 0;
            countLinksTo = 0;
            stringLength = 0;
            speakerId = "";
            countPersNames = 0;
            countPlaceNames = 0;
            countDates = 0;
            listOfChildIds = new ExtendableObjectList();
            GeoModelPerson speaker = myGeoModelPersonReg.getGeoModelPersonFromSpeakerNodeId(parentNodeId);
            if (speaker != null) {
                listOfChildIds.addElem(speaker.getId());
                speakerId = speaker.getId();
            }
            NodeList listNodes = paraElem.getElementsByTagName("name");
            for (int k = 0; k < listNodes.getLength(); k++) {
                childElem = (Element) listNodes.item(k);
                childNodeId = childElem.getAttribute("xml:id");
                listOfChildIds.addElem(childNodeId);
                nameType = childElem.getAttribute("type");
                if (nameType.equals("person")) countPersNames++; else if (nameType.equals("place")) {
                    countPlaceNames++;
                    listOfAllPlaceIds.addElem(childNodeId);
                } else printErrMessage("getStatForAllParas: Wrong type for name! XML:ID: " + parentNodeId);
            }
            listNodes = paraElem.getElementsByTagName("date");
            for (int k = 0; k < listNodes.getLength(); k++) {
                childElem = (Element) listNodes.item(k);
                childNodeId = childElem.getAttribute("xml:id");
                listOfChildIds.addElem(childNodeId);
                countDates++;
            }
            addedNodes = myAddedNodeSet.getAddedNodeByParent(parentNodeId);
            if (addedNodes != null) {
                AddedNode thisAddedNode;
                for (int k = 0; k < addedNodes.length; k++) {
                    thisAddedNode = addedNodes[k];
                    if (thisAddedNode != null) {
                        childNodeId = thisAddedNode.getId();
                        listOfChildIds.addElem(childNodeId);
                        countAddedNodes++;
                    }
                }
            }
            for (int j = 0; j < listOfChildIds.getNextFree(); j++) {
                if (listOfChildIds.getElem(j) != null) {
                    childNodeId = (String) listOfChildIds.getElem(j);
                    linksFrom = myGeoModelLinkSet.getLinkFrom(childNodeId);
                    countLinksFrom += EddDataUtils.countArrayNonnull(linksFrom);
                    linksTo = myGeoModelLinkSet.getLinkTo(childNodeId);
                    countLinksTo += EddDataUtils.countArrayNonnull(linksTo);
                }
            }
            textExcerpt = paraElem.getTextContent();
            stringLength = textExcerpt.length();
            if (stringLength > 50) textExcerpt = textExcerpt.substring(0, 48) + "...";
            returnString = returnString + "<tr align='right'><td>" + parentNodeId + "</td><td>" + stringLength + "</td><td>" + speakerId + "</td><td>" + countPersNames + "</td><td>" + countPlaceNames + "</td><td>" + countDates + "</td><td>" + countAddedNodes + "</td><td>" + countLinksFrom + "</td><td>" + countLinksTo + "</td><td align='left'>" + textExcerpt + "</td></tr>\n";
            if (lineCount++ > 20) {
                returnString = returnString + tableHeaders;
                lineCount = 0;
            }
            totalAddedNodes += countAddedNodes;
            totalLinksFrom += countLinksFrom;
            totalLinksTo += countLinksTo;
            totalStringLength += stringLength;
            totalPersNames += countPersNames;
            totalPlaceNames += countPlaceNames;
            totalDates += countDates;
        }
        returnString = returnString + "<tr><th>Para count</th>" + "<th>Characters</th>" + "<th>Person names</th>" + "<th>Place names</th>" + "<th>Coref place names</th>" + "<th>Dates</th>" + "<th>Added nodes</th>" + "<th>Links from</th>" + "<th>Links to</th>" + "<th></th></tr>\n";
        returnString = returnString + "<tr align='right'><td>" + parasToList.length + "</td><td>" + totalStringLength + "</td><td>" + totalPersNames + "</td><td>" + totalPlaceNames + "</td><td>" + myGeoModelPlaceReg.removeCorefDuplicates(listOfAllPlaceIds).length + "</td><td>" + totalDates + "</td><td>" + totalAddedNodes + "</td><td>" + totalLinksFrom + "</td><td>" + totalLinksTo + "</td><td align='left'>" + "</td></tr>\n";
        return returnString + DomUtils.getXhtmlFooter();
    }

    private String getStatForParaList(String[] paraNodeIds) {
        StringBuffer returnStringBuffer = new StringBuffer(DomUtils.getXhtmlHeader("All about paragraphs"));
        for (int i = 0; i < paraNodeIds.length; i++) returnStringBuffer.append(getStatForOnePara(paraNodeIds[i]));
        return returnStringBuffer.toString() + DomUtils.getXhtmlFooter();
    }

    private String getStatForOnePara(String paraNodeId) {
        StringBuffer returnStringBuffer = new StringBuffer("<h2>Paragraph: " + paraNodeId + "</h2>");
        String personString, placeString, dateString, addedNodeString;
        AddedNode[] addedNodes;
        Element paraElem, childElem;
        String childNodeId, nameType, name;
        ExtendableObjectList listOfChildIds;
        int nodeNumber = findNodeNumberFromNodeId(paraNodeId);
        paraElem = (Element) listOfNodes.item(nodeNumber);
        listOfChildIds = new ExtendableObjectList();
        returnStringBuffer.append("<h3>Text content</h3><p>" + paraElem.getTextContent() + "</p>");
        GeoModelPerson speaker = myGeoModelPersonReg.getGeoModelPersonFromSpeakerNodeId(paraNodeId);
        if (speaker != null) {
            returnStringBuffer.append("<h3>Speaker: " + speaker.getName() + " (" + speaker.getId() + ")</h3>");
            returnStringBuffer.append(formatLinksHtml(speaker.getId()));
            listOfChildIds.addElem(speaker.getId());
        }
        returnStringBuffer.append("<table><tr cellspacing='30'><td valign='top'>");
        personString = "";
        placeString = "";
        NodeList listNodes = paraElem.getElementsByTagName("name");
        for (int k = 0; k < listNodes.getLength(); k++) {
            childElem = (Element) listNodes.item(k);
            childNodeId = childElem.getAttribute("xml:id");
            listOfChildIds.addElem(childNodeId);
            nameType = childElem.getAttribute("type");
            name = childElem.getTextContent();
            if (nameType.equals("person")) {
                personString = personString + "<li>" + name + " (" + childNodeId + ")" + formatLinksHtml(childNodeId) + "</li>";
            } else if (nameType.equals("place")) {
                placeString = placeString + "<li>" + name + " (" + childNodeId + ")" + formatLinksHtml(childNodeId) + "</li>";
            } else printErrMessage("getStatForAllParas: Wrong type for name! XML:ID: " + paraNodeId);
        }
        if (personString.length() > 0) returnStringBuffer.append("<h3>Persons</h3><ul>" + personString + "</ul>");
        if (placeString.length() > 0) returnStringBuffer.append("<h3>Places</h3><ul>" + placeString + "</ul>");
        returnStringBuffer.append("</td><td valign='top'>");
        dateString = "";
        listNodes = paraElem.getElementsByTagName("date");
        for (int k = 0; k < listNodes.getLength(); k++) {
            childElem = (Element) listNodes.item(k);
            childNodeId = childElem.getAttribute("xml:id");
            listOfChildIds.addElem(childNodeId);
            dateString = dateString + "<li>" + childElem.getTextContent() + " (" + childNodeId + ")</li>";
            dateString = dateString + formatLinksHtml(childNodeId);
        }
        if (dateString.length() > 0) returnStringBuffer.append("<h3>Dates</h3><ul>" + dateString + "</ul>");
        addedNodeString = "";
        addedNodes = myAddedNodeSet.getAddedNodeByParent(paraNodeId);
        if (addedNodes != null) {
            AddedNode thisAddedNode;
            for (int k = 0; k < addedNodes.length; k++) {
                thisAddedNode = addedNodes[k];
                if (thisAddedNode != null) {
                    childNodeId = thisAddedNode.getId();
                    listOfChildIds.addElem(childNodeId);
                    addedNodeString = addedNodeString + "<li>" + thisAddedNode.getType() + ": " + thisAddedNode.getContent() + " (" + childNodeId + ")</li>";
                    addedNodeString = addedNodeString + formatLinksHtml(childNodeId);
                }
            }
        }
        if (addedNodeString.length() > 0) returnStringBuffer.append("<h3>Added nodes</h3><ul>" + addedNodeString + "</ul>");
        returnStringBuffer.append("</td></tr></table>");
        return returnStringBuffer.toString();
    }

    private String formatLinksHtml(String nodeId) {
        StringBuffer sb = new StringBuffer();
        int i;
        GeoModelLink thisLink, thisLinkLink;
        GeoModelLink[] links = myGeoModelLinkSet.getLinkFrom(nodeId);
        GeoModelLink[] linksFromLink, linksToLink;
        if (links != null) {
            sb.append("<ul>");
            for (i = 0; i < links.length; i++) {
                thisLink = links[i];
                if (thisLink != null) {
                    sb.append("<li>" + thisLink.getType() + ": " + thisLink.toString() + " (" + thisLink.getTo().getId() + ")</li>");
                    linksFromLink = myGeoModelLinkSet.getLinkFrom(thisLink.getId());
                    linksToLink = myGeoModelLinkSet.getLinkTo(thisLink.getId());
                    if (linksFromLink != null || linksToLink != null) {
                        sb.append("<ul>");
                        if (linksFromLink != null) for (int j = 0; j < linksFromLink.length; j++) {
                            thisLinkLink = linksFromLink[j];
                            if (thisLinkLink != null) sb.append("<li>--> " + thisLinkLink.getType() + ": " + thisLinkLink.toString() + " (" + thisLinkLink.getTo().getId() + ")</li>");
                        }
                        if (linksToLink != null) for (int j = 0; j < linksToLink.length; j++) {
                            thisLinkLink = linksToLink[j];
                            if (thisLinkLink != null) sb.append("<li><-- " + thisLinkLink.getType() + ": " + thisLinkLink.toString() + " (" + thisLinkLink.getTo().getId() + ")</li>");
                        }
                        sb.append("</ul>\n");
                    }
                }
            }
            sb.append("</ul>\n");
        }
        return sb.toString();
    }

    public GeoModelLinkSet getMyGeoModelLinkSet() {
        return myGeoModelLinkSet;
    }

    public GeoModelCoref getMyGeoModelCoref() {
        return myGeoModelCoref;
    }

    public Document getTeiRootNode() {
        return geoModelDoc;
    }

    public GeoModelCorefTableView getMyGeoModelCorefTableView() {
        return myGeoModelCoref.getMyGeoModelCorefTableView();
    }

    /**
	 * Search for paragraphs with the given numeric part of the xml:id. Will
	 * look for paragraphs with ID numbers previous to the requested, but gives
	 * up after having tried 1000.
	 * 
	 * @param nodeId
	 *            The node ID to start with.
	 * @param tagged
	 *            True of XML tags to be included.
	 * @return The string with the paragraph text, or null if none found.
	 */
    String getTextFromSurroundingPara(String nodeId, boolean tagged) {
        return myNodeListView.getTextFromSurroundingPara(nodeId, tagged);
    }

    /**
	 * Search for paragraphs with the given numeric part of the xml:id. Will
	 * look for paragraphs with ID numbers previous to the requested, but gives
	 * up after having tried 1000.
	 * 
	 * @param nodeId
	 *            The node ID to start with.
	 * @return The string with the ID of the paragraph type element, or null if
	 *         none found.
	 */
    public String getIdOfSurroundingPara(String nodeId) {
        return myNodeListView.getIdOfSurroundingPara(nodeId);
    }

    private void readPageNums() {
        NodeList pageNodes = geoModelDoc.getElementsByTagName("pb");
        Element thisPageNode;
        String nodeId, pageNum;
        for (int i = 0; i < pageNodes.getLength(); i++) {
            thisPageNode = (Element) pageNodes.item(i);
            nodeId = thisPageNode.getAttribute("xml:id");
            pageNum = thisPageNode.getAttribute("n");
            geoModelNodeIdToPageNum.put(nodeId, pageNum);
        }
    }

    /**
	 * Find the page on which the start tag of an element is located.
	 * 
	 * @param nodeId
	 *            The element whose page to find.
	 * @return The number of the page on which the node starts. Null on failure.
	 */
    String findPageNumForNode(String nodeId) {
        String pageNum = null;
        if (nodeId != null && nodeId.length() > 6) {
            int nodeIdNum = Integer.parseInt(nodeId.substring(6));
            while (pageNum == null && --nodeIdNum > 0) {
                pageNum = geoModelNodeIdToPageNum.get("Schn1_" + nodeIdNum);
            }
        }
        return pageNum;
    }

    GeoModelPlaceReg getMyGeoModelPlaceReg() {
        return myGeoModelPlaceReg;
    }

    GeoModelProperties getMyGeoModelProperties() {
        return myGeoModelProperties;
    }

    AddedNodeSet getMyAddedNodeSet() {
        return myAddedNodeSet;
    }

    public GeoModelPersonReg getMyGeoModelPersonReg() {
        return myGeoModelPersonReg;
    }

    private void spitOutDebug() {
        System.out.println(myAddedNodeSet.getAddedNodeSet());
    }

    public GeoModelElement[] getElements() {
        GeoModelElement[] elements = new GeoModelElement[geoModelDocNodeIndex.size()];
        Set<String> keys = geoModelDocNodeIndex.keySet();
        Iterator<String> keyIter = keys.iterator();
        String key;
        int i = 0;
        while (keyIter.hasNext()) {
            key = keyIter.next();
            elements[i++] = geoModelDocNodeIndex.get(key);
        }
        return elements;
    }
}
