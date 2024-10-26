package org.tridentproject.repository.fedora.mgmt;

import org.apache.log4j.Logger;
import java.io.InputStream;
import java.io.IOException;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.MalformedURLException;
import org.jaxen.SimpleNamespaceContext;

/**
 * ItemImpl is the basic implementation class of the Item interface.
 * This class will implement methods that are common across all Item types
 * and can therefore be abstracted from those classes into this one.
 *
 * This class interacts with a repository through the RepositoryClient class.
 *
 * @author David Kennedy
 *
 **/
public class ItemImpl extends DataObjectImpl implements Item {

    private static Logger log = Logger.getLogger(ItemImpl.class);

    private DocumentFactory df = DocumentFactory.getInstance();

    private HashMap URImap = new HashMap();

    /**
     * Class constructor
     *
     * @param repoClient    the RepositoryClient
     *
     **/
    public ItemImpl(RepositoryClient repoClient) {
        super(repoClient);
        init();
    }

    /**
     * Class constructor
     *
     * @param repoClient     the RepositoryClient
     * @param existingPid    existing Pid
     *
     **/
    public ItemImpl(RepositoryClient repoClient, String existingPid) {
        super(repoClient, existingPid);
        init();
    }

    private void init() {
        URImap.put("mfd", "http://lib.duke.edu/fedora/metadataFormDefinition");
    }

    /**
     * Create a basic Item object in the Fedora Repository.
     *
     * @throws FedoraAPIException
     *
     **/
    public void createDataObject() throws FedoraAPIException {
        super.createDataObject();
        ItemProperties props = createProperties();
        setProperties(props);
        Struct struct = createStruct();
        setStruct(struct);
    }

    protected Workflow initiateWorkflow(Workflow workflow) {
        log.debug("ItemImpl: initiateWorkflow");
        workflow.updateProcessStatus("publication", null);
        workflow.updateProcessStatus("component-validation", null);
        workflow.updateProcessStatus("metadata-validation", null);
        return workflow;
    }

    /**
     * Marks item as deleted.  Purge all components.  Remove all item-item rels from rels.
     * set status to Deleted
     *
     * @throws FedoraAPIException in the event of a failure to mark the item as deleted
     *
     **/
    public void delete() throws FedoraAPIException {
        List comps = getComponents();
        Iterator<Component> compsIter = comps.iterator();
        while (compsIter.hasNext()) purgeComponent(compsIter.next().getPid());
        Rels rels = getRels();
        rels.removeAllCategories();
        rels.removeAllCollections();
        rels.removeAllMAPs();
        rels.removeAllParts();
        setRels(rels);
        setStatus("Deleted");
        setType("DeletedItem");
    }

    /**
     * Purge an item and its components
     *
     * @throws FedoraAPIException in the event of a failure to purge the item or its components
     *
     **/
    public void purgeItemAndComponents() throws FedoraAPIException {
        List comps = getComponents();
        Iterator<Component> compsIter = comps.iterator();
        while (compsIter.hasNext()) purgeComponent(compsIter.next().getPid());
        purge();
    }

    /**
     * Creates initial RELS-EXT document
     *
     * @throws FedoraAPIException if error creating rels 
     *
     **/
    public Rels createRels() throws FedoraAPIException {
        Rels rels = super.createRels();
        rels.addModel("duke-cm:Item");
        return rels;
    }

    /**
     * Sets the Properties datastream of the item
     * to the supplied Document.
     *
     * @param doc           the properties document
     *
     * @throws FedoraAPIException in the event of a failure to set the datastream
     *
     **/
    public void setProperties(Document doc) throws FedoraAPIException {
        if (hasProperties()) {
            modifyDatastream("Properties", doc, null);
        } else {
            addDatastream("Properties", "Item Properties", true, "text/xml", "X", doc, null);
        }
    }

    /**
     * Sets the Properties datastream of the item with
     * the supplied ItemProperties object
     *
     * @param props         the properties object
     *
     * @throws FedoraAPIException in the event of a failure to set the datastream
     *
     **/
    public void setProperties(ItemProperties props) throws FedoraAPIException {
        Document doc = props.getProperties();
        if (hasProperties()) {
            modifyDatastream("Properties", doc, null);
        } else {
            addDatastream("Properties", "Item Properties", true, "text/xml", "X", doc, null);
        }
    }

    /**
     * Returns the Properties datastream as a Document
     *
     * @throws FedoraAPIException if an error occurs in retrieving the datastream
     *
     **/
    public Document getPropertiesDoc() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getPropertiesDoc: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getPropertiesDoc: repository client not initialized");
        }
        return getXMLDissemination("duke-sDef:properties", "getProperties", null);
    }

    /**
     * Returns the Properties datastream as a Properties object
     *
     * @throws FedoraAPIException if an error occurs in retrieving the datastream
     *
     **/
    public ItemProperties getProperties() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getProperties: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getProperties: repository client not initialized");
        }
        return new DukeItemProperties(getXMLDissemination("duke-sDef:properties", "getProperties", null));
    }

    /**
     * Returns whether or not Properties exists for the Item
     *
     * @return boolean indicator of Properties existence
     *
     **/
    public boolean hasProperties() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in hasProperties: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in hasProperties: repository client not initialized");
        }
        return hasDatastream("Properties");
    }

    /**
     * Creates an ItemProperties object for the Item
     *
     * @return the properties object
     *
     * @throws FedoraAPIException if there is an error creating item properties
     *
     **/
    public ItemProperties createProperties() throws FedoraAPIException {
        ItemProperties props = new DukeItemProperties();
        props.createProperties("DukeItem", "Incomplete");
        return props;
    }

    /**
     * Sets the appropriate datastream of the item
     * to the supplied Document.
     *
     * @param doc           the Struct document
     *
     * @throws FedoraAPIException in the event of a failure to set the datastream
     *
     **/
    public void setStruct(Document doc) throws FedoraAPIException {
        if (hasStruct()) {
            modifyDatastream("METS", doc, null);
        } else {
            addDatastream("METS", "Structural Metadata - METS", true, "text/xml", "X", doc, null);
        }
        checkAndUpdateStatus(null);
        checkAndUpdateComponentValidation();
    }

    /**
     * Sets the appropriate datastream of the item with
     * the supplied Struct object
     *
     * @param struct         the Struct object
     *
     * @throws FedoraAPIException in the event of a failure to set the datastream
     *
     **/
    public void setStruct(Struct struct) throws FedoraAPIException {
        Document doc = struct.getStruct();
        if (hasStruct()) {
            modifyDatastream("METS", doc, null);
        } else {
            addDatastream("METS", "Structural Metadata - METS", true, "text/xml", "X", doc, null);
        }
        checkAndUpdateStatus(null);
        checkAndUpdateComponentValidation();
    }

    /**
     * Returns the appropriate Struct datastream as a Document
     *
     * @throws FedoraAPIException if an error occurs in retrieving the datastream
     *
     **/
    public Document getStructDoc() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getStructDoc: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getStructDoc: repository client not initialized");
        }
        return getXMLDatastream("METS");
    }

    /**
     * Returns the appropriate Struct datastream as a Struct object
     *
     * @throws FedoraAPIException if an error occurs in retrieving the datastream
     *
     **/
    public Struct getStruct() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getStructDoc: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getStructDoc: repository client not initialized");
        }
        return new MetsStruct(getXMLDatastream("METS"));
    }

    /**
     * Returns whether or not Struct exists for the Item
     *
     * @return boolean indicator of Struct existence
     *
     **/
    public boolean hasStruct() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in hasStruct: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in hasStruct: repository client not initialized");
        }
        return hasDatastream("METS");
    }

    /**
     * Creates a Struct object for the Item
     *
     * @return the Struct object
     *
     * @throws FedoraAPIException if there is an error creating struct
     *
     **/
    public Struct createStruct() throws FedoraAPIException {
        Struct struct = new MetsStruct();
        struct.createStruct(pid);
        return struct;
    }

    /**
     * Checks and updates component validation.  Updates Workflow accordingly.
     *
     * @throws FedoraAPIException if there are any errors
     *
     **/
    private void checkAndUpdateComponentValidation() throws FedoraAPIException {
        Workflow workflow = getWorkflow();
        String strOldStatus = workflow.getProcessStatus("component-validation");
        String strNewStatus = "Error";
        if (hasEnoughContent()) {
            strNewStatus = "Complete";
        }
        if (!(strNewStatus.equals(strOldStatus))) {
            workflow.updateComponentValidation(strNewStatus);
            setWorkflow(workflow);
        }
    }

    /**
     * Checks and updates metadata validation.  Updates Workflow accordingly.
     *
     * @throws FedoraAPIException if there are any errors
     *
     **/
    private void checkAndUpdateMetadataValidation() throws FedoraAPIException {
        Workflow workflow = getWorkflow();
        String strOldStatus = workflow.getProcessStatus("metadata-validation");
        String strNewStatus = "Error";
        if (hasDMDService() && validateDMD().isEmpty()) {
            strNewStatus = "Complete";
        }
        if (!(strNewStatus.equals(strOldStatus))) {
            workflow.updateMetadataValidation(strNewStatus);
            setWorkflow(workflow);
        }
    }

    /**
     * Gets the type of the item
     *
     * @return the type of the item
     *
     * @throws FedoraAPIException if there is an error retrieving the type
     **/
    public String getType() throws FedoraAPIException {
        return getProperties().getType();
    }

    /**
     * Sets the type of the item
     *
     * @param type         the type to be set for the item
     *
     * @throws FedoraAPIException if there is an error setting the type
     *
     **/
    public void setType(String type) throws FedoraAPIException {
        ItemProperties props = getProperties();
        props.updateType(type);
        setProperties(props);
    }

    /**
     * Gets the type of the item in a human consumable text string
     *
     * @return the type of the item
     *
     * @throws FedoraAPIException if there is an error retrieving the type
     **/
    public String getTypeText() throws FedoraAPIException {
        return "Generic Item";
    }

    /**
     * Gets the status of the item
     *
     * @return the status of the item
     *
     * @throws FedoraAPIException if tehre is an error retrieving the status
     *
     **/
    public String getStatus() throws FedoraAPIException {
        return getProperties().getStatus();
    }

    /**
     * Sets the status of the item
     *
     * @param status      string representation of the status to be set
     *
     * @throws FedoraAPIException if there is an error setting the status
     *
     **/
    public void setStatus(String status) throws FedoraAPIException {
        ItemProperties props = getProperties();
        props.updateStatus(status);
        setProperties(props);
    }

    /**
     * Gets the edit groups for the item
     *
     * @return List of edit groups
     *
     * @throws FedoraAPIException if there is an error retrieving the edit groups
     *
     **/
    public List<String> getEditGroups() throws FedoraAPIException {
        return getProperties().getEditGroups();
    }

    /**
     * Sets the edit groups of the item
     *
     * @param editGroups  the list of edit groups
     *
     * @throws FedoraAPIException if there is an error setting the edit group
     *
     **/
    public void setEditGroups(List<String> editGroups) throws FedoraAPIException {
        ItemProperties props = getProperties();
        props.setEditGroups(editGroups);
        setProperties(props);
    }

    /**
     * Sets the DMD of the item
     * to the supplied Document.
     *
     * @param doc           the DMD document
     *
     * @throws FedoraAPIException in the event of a failure to set the DMD
     *
     **/
    public void setDMD(Document doc) throws FedoraAPIException {
        if (hasDMD()) {
            modifyDatastream("DMD", doc, null);
        } else {
            addDatastream("DMD", "Descriptive Metadata Document", true, "text/xml", "X", doc, null);
        }
        checkAndUpdateStatus(null);
        checkAndUpdateMetadataValidation();
    }

    /**
     * Sets the DMD of the item with
     * the supplied DMD object
     *
     * @param dmd         the DMD object
     *
     * @throws FedoraAPIException in the event of a failure to set the DMD
     *
     **/
    public void setDMD(DMD dmd) throws FedoraAPIException {
        Document doc = dmd.getDMD();
        if (hasDMD()) {
            modifyDatastream("DMD", doc, null);
        } else {
            addDatastream("DMD", "Descriptive Metadata Document", true, "text/xml", "X", doc, null);
        }
        checkAndUpdateStatus(null);
        checkAndUpdateMetadataValidation();
    }

    /**
     * Returns a Document representation of the DMD
     *
     * @throws FedoraAPIException if an error occurs in retrieving the DMD
     *
     **/
    public Document getDMDDoc() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getDMDDoc: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getDMDDoc: repository client not initialized");
        }
        return getXMLDissemination("duke-sDef:dmd", "getDMD", null);
    }

    /**
     * Returns the DMD as a DMD object
     *
     * @throws FedoraAPIException if an error occurs in retrieving the DMD
     *
     **/
    public DMD getDMD() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getDMDDoc: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getDMDDoc: repository client not initialized");
        }
        return new DukeCore(getXMLDissemination("duke-sDef:dmd", "getDMD", null));
    }

    /**
     * Returns whether or not DMD exists for the Item
     *
     * @return boolean indicator of DMD existence
     *
     **/
    public boolean hasDMD() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in hasDMD: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in hasDMD: repository client not initialized");
        }
        return hasDatastream("DMD");
    }

    /**
     * Return whether or not DMD service exists for the item
     *
     * @return boolean indicator of DMD service existence
     *
     * @throws FedoraAPIException
     *
     **/
    public boolean hasDMDService() throws FedoraAPIException {
        return hasService("duke-sDef:dmd");
    }

    /**
     * Creates a DMD object for the Item
     *
     * @return the DMD object
     *
     * @throws FedoraAPIException if there is an error creating DMD
     *
     **/
    public DMD createDMD() throws FedoraAPIException {
        DMD dmd = new DukeCore();
        dmd.createDMD();
        return dmd;
    }

    /**
     * Indicates whether or not DMD is editable.  In some item types,
     * DMD will be derived instead of directly editable.
     *
     * @return boolean indicator of whether or not DMD is editable
     *
     **/
    public boolean isDMDEditable() {
        return true;
    }

    /**
     * Returns a Document containing the last modified date of the DMD datastream
     *
     * @return Document
     *
     **/
    public Document getDMDLastModified() throws FedoraAPIException {
        if (pid == null) {
            throw new FedoraAPIException("Error in getDMDLastModified: pid cannot be null");
        } else if (rc == null) {
            throw new FedoraAPIException("Error in getDMDLastModified: repository client not initialized");
        }
        return getXMLDissemination("duke-sDef:dmd", "getModifyDate", null);
    }

    /**
     * Returns a transformed DMD doc in the specified schema
     *
     * @param schema      the return schema of the DMD
     *
     * @return document containing the transformed DMD
     *
     * @throws FedoraAPIException if an error occurs retrieving the doc or transforming it
     *
     **/
    public Document getTransformedDMDDoc(String schema) throws FedoraAPIException {
        String defaultSchema = getDefaultSchema();
        Document defaultDMDDoc = getDMDDoc();
        XSLTransformer dmdTransformer = new XSLTransformer();
        if (defaultSchema != null && defaultSchema.equals(schema)) {
            return defaultDMDDoc;
        } else {
            InputStream xslin = getTransformXslIS(defaultSchema, schema);
            return dmdTransformer.transform(defaultDMDDoc, xslin);
        }
    }

    /**
     * Returns a MetadataForm derived from the DMD and the MAP
     *
     * @return document representation of the MetadataForm
     *
     * @throws FedoraAPIException if an error occurs retrieving the DMD,
     * transforming it, or applying validation errors
     * @throws MAPNotFoundException if MAP is not retrieved
     *
     **/
    public Document getMetadataForm() throws FedoraAPIException, MAPNotFoundException {
        log.debug("getMetadataForm");
        Document mdfDoc = getTransformedDMDDoc("MDF");
        List<ValidationRule> errors = validateDMD();
        log.debug("got " + errors.size() + " errors");
        Element rootNode = mdfDoc.getRootElement();
        Element errorsNode = rootNode.addElement("errors");
        for (ValidationRule error : errors) {
            log.debug("in error loop");
            Element errorNode = errorsNode.addElement("error");
            errorNode.addElement("message").addText(error.getError());
            errorNode.addElement("field_group").addText(error.getFieldGroup());
        }
        log.debug("finished adding errors to MetadataForm");
        Document mdfdDoc = getMetadataFormDefinition();
        log.debug("successfully retrieved metadata form definition");
        if (mdfdDoc != null) {
            log.debug("got metadataFormDefinition");
            log.debug(mdfdDoc.asXML());
            XPath xpathSelector;
            xpathSelector = getXPath("/metadata_form/fields/field_group");
            List fieldGroupList = xpathSelector.selectNodes(mdfDoc);
            for (Iterator fieldGroupIter = fieldGroupList.iterator(); fieldGroupIter.hasNext(); ) {
                Element fieldGroupNode = (Element) fieldGroupIter.next();
                String fieldGroupName = fieldGroupNode.attributeValue("name");
                log.debug("testing " + fieldGroupName);
                String strXpath = "/mfd:metadataFormDefinition/mfd:fields/mfd:field_group[@name='" + fieldGroupName + "']";
                log.debug("evaluating " + strXpath);
                xpathSelector = getXPath(strXpath);
                Element fieldGroupDefinitionNode = (Element) xpathSelector.selectSingleNode(mdfdDoc);
                if (fieldGroupDefinitionNode != null) {
                    log.debug("found corresponding definition for " + fieldGroupName);
                    List fgdnAttributes = fieldGroupDefinitionNode.attributes();
                    for (Iterator fgdnAttrList = fgdnAttributes.iterator(); fgdnAttrList.hasNext(); ) {
                        Attribute attr = (Attribute) fgdnAttrList.next();
                        String strAttrName = attr.getQName().getName();
                        if (!strAttrName.equals("name")) {
                            fieldGroupNode.addAttribute(strAttrName, attr.getValue());
                        }
                    }
                    xpathSelector = getXPath("field");
                    List fieldList = xpathSelector.selectNodes(fieldGroupNode);
                    if (!fieldList.isEmpty()) {
                        for (Iterator fieldIter = fieldList.iterator(); fieldIter.hasNext(); ) {
                            Element fieldNode = (Element) fieldIter.next();
                            xpathSelector = getXPath("element");
                            List elemList = xpathSelector.selectNodes(fieldNode);
                            for (Iterator elemIter = elemList.iterator(); elemIter.hasNext(); ) {
                                Element elementNode = (Element) elemIter.next();
                                String elementName = elementNode.attributeValue("name");
                                log.debug("testing " + elementName);
                                xpathSelector = getXPath("mfd:field/mfd:element[@name='" + elementName + "']");
                                Element elementDefinitionNode = (Element) xpathSelector.selectSingleNode(fieldGroupDefinitionNode);
                                if (elementDefinitionNode != null) {
                                    List elemAttrs = elementDefinitionNode.attributes();
                                    for (Iterator elemAttrList = elemAttrs.iterator(); elemAttrList.hasNext(); ) {
                                        Attribute attr = (Attribute) elemAttrList.next();
                                        String strAttrName = attr.getQName().getName();
                                        if (!strAttrName.equals("name")) {
                                            elementNode.addAttribute(strAttrName, attr.getValue());
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Element fieldNode = fieldGroupNode.addElement("field");
                        xpathSelector = getXPath("mfd:field/mfd:element");
                        List elemDefList = xpathSelector.selectNodes(fieldGroupDefinitionNode);
                        for (Iterator elemDefIter = elemDefList.iterator(); elemDefIter.hasNext(); ) {
                            Element elementDefinitionNode = (Element) elemDefIter.next();
                            Element elementNode = fieldNode.addElement("element");
                            List elemAttrs = elementDefinitionNode.attributes();
                            for (Iterator elemAttrList = elemAttrs.iterator(); elemAttrList.hasNext(); ) {
                                Attribute attr = (Attribute) elemAttrList.next();
                                String strAttrName = attr.getQName().getName();
                                elementNode.addAttribute(strAttrName, attr.getValue());
                            }
                        }
                    }
                } else {
                    fieldGroupNode.addAttribute("label", fieldGroupName);
                    xpathSelector = getXPath("field");
                    List fieldList = xpathSelector.selectNodes(fieldGroupNode);
                    for (Iterator fieldIter = fieldList.iterator(); fieldIter.hasNext(); ) {
                        Element fieldNode = (Element) fieldIter.next();
                        xpathSelector = getXPath("element");
                        List elemList = xpathSelector.selectNodes(fieldNode);
                        for (Iterator elemIter = elemList.iterator(); elemIter.hasNext(); ) {
                            Element elementNode = (Element) elemIter.next();
                            String elementName = elementNode.attributeValue("name");
                            elementNode.addAttribute("label", elementName);
                        }
                    }
                }
            }
        }
        for (ValidationRule error : errors) {
            String strFieldGroupName = error.getFieldGroup();
            log.debug("attempting to update field_group element with error: " + strFieldGroupName);
            Element fieldGroupNode = (Element) getXPath("//field_group[@name='" + strFieldGroupName + "']").selectSingleNode(mdfDoc);
            if (fieldGroupNode != null) {
                String strDisplay = fieldGroupNode.attributeValue("display");
                if (strDisplay == null) strDisplay = "error"; else strDisplay = strDisplay + " error";
                fieldGroupNode.addAttribute("display", strDisplay);
            }
        }
        return mdfDoc;
    }

    /**
     * Returns a list of pids of items for which an isMemberOfCollection
     * relationship is defined in this item.  This implementation will
     * return all pids that are defined with the isMemberOfCollection
     * relationship in the RELS-EXT datastream.
     *
     * @return List of pids representing collection items
     *
     * @throws FedoraAPIException if error occurs in retrieving collection relationships
     *
     **/
    public List<String> getCollections() throws FedoraAPIException {
        Rels rels = getRels();
        return rels.getCollections();
    }

    /**
     * Sets the isMemberOfCollection relationships to the pids that are
     * supplied in the collections parameter.
     *
     * @param collections      List of Strings representing pids of collections
     *
     * @throws FedoraAPIException if error occurs in setting collection relationships
     *
     **/
    public void setCollections(List<String> collections) throws FedoraAPIException {
        Iterator<String> collectionIter = collections.iterator();
        Rels rels = getRels();
        rels.removeAllCollections();
        while (collectionIter.hasNext()) rels.addToCollection(collectionIter.next());
        setRels(rels);
    }

    /**
     * Returns a list of pids of items for which an isMemberOfCategory
     * relationship is defined in this item.  This implementation will
     * return all pids that are defined with the isMemberOfCategory
     * relationship in the RELS-EXT datastream.
     *
     * @return List of pids representing category items
     *
     * @throws FedoraAPIException if error occurs in retrieving category relationships
     *
     **/
    public List<String> getCategories() throws FedoraAPIException {
        Rels rels = getRels();
        return rels.getCategories();
    }

    /**
     * Sets the isMemberOfCategory relationships to the pids that are
     * supplied in the categories parameter.
     *
     * @param categories      List of Strings representing pids of categories
     *
     * @throws FedoraAPIException if error occurs in setting category relationships
     *
     **/
    public void setCategories(List<String> categories) throws FedoraAPIException {
        Iterator<String> categoryIter = categories.iterator();
        Rels rels = getRels();
        rels.removeAllCategories();
        while (categoryIter.hasNext()) rels.addToCategory(categoryIter.next());
        setRels(rels);
    }

    /**
     * Returns a HashMap of valid Component types that can be added to this
     * item.  For entries (key:value pairs) in the HashMap, the Key represents the component type
     * and the Value is a textual representation of the component type (ie: DukeImage : Image)
     *
     * @return a HashMap of valid component types
     *
     **/
    public HashMap getValidComponentTypes() {
        return new HashMap();
    }

    /**
     * Returns the pid of the MAP for this item, or null if not defined
     *
     * @return the pid of the MAP for this item
     *
     * @throws FedoraAPIException in the case of errors in retrieving the MAP pid
     *
     **/
    public String getMAP() throws FedoraAPIException {
        log.debug("getMAP()");
        Rels rels = getRels();
        return rels.getMAP();
    }

    /**
     * Returns the pid of the MAP for this item, or null if not defined
     *
     * @param mapPid               the pid of the MAP for this item
     *
     * @throws FedoraAPIException in the case of errors in retrieving the MAP pid
     *
     **/
    public void setMAP(String mapPid) throws FedoraAPIException {
        Rels rels = getRels();
        rels.addMAP(mapPid);
        setRels(rels);
    }

    /**
     * Returns a Document containing the URLs of thumbnails for the item
     *
     * @return a Document containing the URLS of thumbnails
     *
     * @throws FedoraAPIException if an error occurs retrieving the thumbnail urls
     *
     **/
    public Document getThumbnailInfo() throws FedoraAPIException {
        InputStream is = getDissemination("duke-sDef:thumbnail", "getThumbnailInfo", null);
        Document doc = null;
        try {
            SAXReader reader = new SAXReader();
            doc = reader.read(is);
        } catch (DocumentException e) {
            throw new FedoraAPIException("Unable to retrieve thumbnail information", e);
        }
        return doc;
    }

    /**
     * Returns a list of supported DMD schemas
     *
     * @return List of supported DMD schemas
     *
     * @throws FedoraAPIException if an error occurs retrieving the schemas
     *
     **/
    public List<String> getSupportedSchemas() throws FedoraAPIException {
        List<String> schemas = new ArrayList<String>();
        schemas.add("DukeCore");
        schemas.add("MDF");
        schemas.add("Title");
        return schemas;
    }

    /**
     * Returns the default schema
     *
     * @return the default schema for the dmd for the item
     *
     * @throws FedoraAPIException if an error occurs in retrieving the schema
     *
     **/
    public String getDefaultSchema() throws FedoraAPIException {
        return "DukeCore";
    }

    /**
     * Attaches a Component to the Item.  If no pid
     * is specified, will create a new Component based on the 
     * type parameter.
     * <p>
     * One of the following parameters is required: type, pid.
     *
     * @param    type      the type of the Component
     * @param    pid       the PID of the Component, if it already exists
     * @param    section   the section of the Component in the Struct
     * @param    order     the order of the Component in the Struct
     * @param    label     the label of the Component in the Struct
     *
     * @return   the new Component
     */
    public Component addComponent(String type, String pid, String section, String order, String label) throws FedoraAPIException {
        return addComponent(type, pid, new ComponentMap(section, pid, order, label));
    }

    /**
     * Attaches a Component to the Item.  If no pid
     * is specified, will create a new Component based on the 
     * type parameter.
     * <p>
     * One of the following parameters is required: type, pid.
     *
     * @param    type      the type of the Component
     * @param    pid       the PID of the Component, if it already exists
     * @param    map       ComponentMap for updating the Struct
     *
     * @return   the new Component
     */
    public Component addComponent(String type, String pid, ComponentMap map) throws FedoraAPIException {
        if (type == null && pid == null) {
            throw new FedoraAPIException("Error adding Component: requires pid or type");
        }
        if (!getValidComponentTypes().containsKey(type)) {
            throw new FedoraAPIException(type + " is not a valid component type for this item.");
        }
        Component comp = null;
        RepositoryFactory rf = new RepositoryFactory();
        log.debug("in addComponent");
        if (pid == null) {
            comp = rf.newComponent(rc, type);
            pid = comp.getPid();
            map.setPid(pid);
        } else {
            comp = rf.getComponent(rc, pid);
            type = comp.getType();
        }
        Struct struct = getStruct();
        struct.addComponent(map);
        setStruct(struct);
        Rels rels = getRels();
        rels.addPart(pid);
        setRels(rels);
        return comp;
    }

    /**
     * Attaches a Component to the Item.  Attaches given
     * content item as datastream(s) to the Component.
     *
     * @param    is             InputStream representing content to be added
     * @param    map            ComponentMap for updating the Struct
     * @param    componentType  String representing component type
     * @param    mimeType       String representing content's MIME type
     *
     * @return   the new Component
     */
    public Component uploadContentItem(InputStream is, ComponentMap map, String componentType, String mimeType) throws FedoraAPIException {
        log.debug("Item:uploadContentItem(is, " + map + ", " + componentType + ", " + mimeType + ")");
        Component comp = addComponent(componentType, null, map);
        comp.uploadContentItem(is, mimeType);
        return comp;
    }

    /**
     * Purges a Component as well as dissociates the Component from
     * the Item
     * <p>
     * The following parameter is required: pid.
     *
     * @param    pid       the PID of the Component
     */
    public void purgeComponent(String pid) throws FedoraAPIException {
        Component comp = RepositoryFactory.getComponent(rc, pid);
        log.debug("purging Component with pid: " + pid);
        comp.purge();
        Struct struct = getStruct();
        log.debug("removing pid (" + pid + ") from struct");
        struct.removePid(pid);
        log.debug(struct.getStruct().asXML());
        setStruct(struct);
        Rels rels = getRels();
        rels.removePart(pid);
        setRels(rels);
    }

    /**
     * Returns a List of Components
     *
     * @return   a List of Components
     */
    public List<Component> getComponents() throws FedoraAPIException {
        List<Component> objs = new ArrayList<Component>();
        try {
            List<ComponentMap> cmaps = getStruct().getComponents();
            Iterator<ComponentMap> cmapsIter = cmaps.iterator();
            while (cmapsIter.hasNext()) {
                ComponentMap cmap = cmapsIter.next();
                Component comp = RepositoryFactory.getComponent(rc, cmap.getPid());
                objs.add(comp);
            }
        } catch (FedoraAPIException e) {
            throw new FedoraAPIException("Unable to get list of Components", e);
        } finally {
            return objs;
        }
    }

    /**
     * Checks for digital object having enough content.  Checks for at least
     * an image in the mets with copy=DISPLAY and order=1
     *
     * @return   boolean value, true for enough, false for not enough
     */
    public boolean hasEnoughContent() {
        return true;
    }

    /**
     * validates descriptive metadata document.  Returns a list of ValidationRules
     * that the descriptive metadata document did not pass.  If the list is empty,
     * then there are no errors, and the descriptive metadata document has
     * passed validation.
     *
     * @return list of ValidationRules
     *
     * @throws FedoraAPIException
     * @throws MAPNotFoundException
     *
     **/
    public List<ValidationRule> validateDMD() throws FedoraAPIException, MAPNotFoundException {
        List<ValidationRule> rules = getDMDValidationRules();
        DMD dmd = getDMD();
        return dmd.validate(rules);
    }

    /**
     * Returns the corresponding MAP for this Item
     *
     * @return MAP
     *
     * @throws FedoraAPIException
     * @throws MAPNotFoundException
     *
     **/
    private MAP getThisItemsMAP() throws MAPNotFoundException {
        log.debug("getThisItemsMAP()");
        String mapPid = null;
        try {
            mapPid = getMAP();
        } catch (FedoraAPIException e) {
            throw new MAPNotFoundException("Error obtaining pid of MAP", e);
        }
        log.debug("MAP pid = " + mapPid);
        MAP map = null;
        if (mapPid != null) {
            RepositoryFactory rf = new RepositoryFactory();
            map = rf.getMAP(rc, mapPid);
        }
        return map;
    }

    /**
     * Returns the list of ValidationRules that can be applied to the descriptive
     * metadata document for the item.  
     *
     * @return list of ValidationRules
     *
     * @throws MAPNotFoundException
     *
     **/
    public List<ValidationRule> getDMDValidationRules() throws FedoraAPIException, MAPNotFoundException {
        log.debug("getDMDValidationRules");
        List<ValidationRule> rules = null;
        MAP map = getThisItemsMAP();
        if (map != null) {
            log.debug("got MAP");
            rules = map.getValidationRules();
            log.debug("got validation rules");
        }
        return rules;
    }

    /**
     * Returns the MetadataFormDefinition for this Item
     *
     * @return Document representation of the MetadataFormDefinition
     *
     * @throws MAPNotFoundException
     *
     **/
    public Document getMetadataFormDefinition() throws MAPNotFoundException {
        log.debug("getMetadataFormDefinition");
        Document doc = null;
        try {
            MAP map = getThisItemsMAP();
            log.debug("got this items map");
            if (map != null) {
                log.debug("got MAP");
                doc = map.getMetadataFormDefinitionDoc();
                log.debug("got metadata form definition doc");
            }
        } catch (FedoraAPIException e) {
            log.debug("Unable to retrieve MetadataFormDefinition: " + e.getMessage());
            throw new FedoraAPIException("Unable to retrieve MetadataFormDefinition", e);
        } finally {
            return doc;
        }
    }

    /**
     * Checks for completeness of digital object.
     *
     * @return   boolean value, true for complete, false for not complete
     */
    public boolean isComplete() {
        try {
            if (hasDMDService()) {
                log.debug("has DMD Service");
                if (hasEnoughContent()) {
                    log.debug("has enough content");
                    if (validateDMD().isEmpty()) {
                        log.debug("validate DMD is empty");
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * Checks for completeness and updates status accordingly
     * 
     * @param    newStatus    String representing the new proposed status
     *
     * @return   boolean      whether or not the status was updated
     *
     * @throws FedoraAPIException
     *
     **/
    public boolean checkAndUpdateStatus(String newStatus) throws FedoraAPIException {
        String currentStatus = getStatus();
        if (newStatus == null) {
            if (isComplete()) {
                if (currentStatus.equals("Incomplete")) setStatus("Complete");
            } else if (currentStatus.equals("Complete") || currentStatus.equals("Published")) {
                setStatus("Incomplete");
            }
        } else {
            if (newStatus.equals("Published") || newStatus.equals("Complete")) {
                if (isComplete()) setStatus(newStatus); else return false;
            } else {
                if (newStatus.equals("Incomplete")) {
                    if (!currentStatus.equals("Incomplete")) {
                        setStatus("Incomplete");
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Performs a workflow action.
     *
     * @param process_id          the workflow process identifier
     * @param action_id           the action identifier
     *
     * @return boolean            indicates whether or not the action was completed successfully
     *
     **/
    public boolean doAction(String process_id, String action_id) {
        if (process_id == null || action_id == null) return false;
        if (process_id.equals("publication")) {
            if (action_id.equals("publish")) {
                return publish();
            } else if (action_id.equals("unpublish")) {
                return unpublish();
            }
        }
        return false;
    }

    private boolean publish() {
        try {
            Workflow workflow = getWorkflow();
            String strCompStatus = workflow.getProcessStatus("component-validation");
            String strMetaStatus = workflow.getProcessStatus("metadata-validation");
            String strPubStatus = workflow.getProcessStatus("publication");
            if (strCompStatus == null || strMetaStatus == null || strPubStatus == null) return false;
            if (strCompStatus.equals("Complete") && strMetaStatus.equals("Complete")) {
                if (!strPubStatus.equals("Complete")) {
                    workflow.publish();
                    setWorkflow(workflow);
                }
                return true;
            }
        } catch (FedoraAPIException e) {
            log.debug("Error publishing: " + e.getMessage());
        }
        return false;
    }

    private boolean unpublish() {
        try {
            Workflow workflow = getWorkflow();
            workflow.unpublish();
            setWorkflow(workflow);
            return true;
        } catch (FedoraAPIException e) {
            log.debug("Error unpublishing: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns an input stream containing the XSL to transform a document
     * from the sourceSchema into the targetSchema
     *
     * @param sourceSchema     the schema for the source document
     * @param targetSchema     the schema for the target document
     *
     * @return inputstream containing the xsl
     *
     * @throws FedoraAPIException if there is an error obtaining the xsl
     *
     **/
    public InputStream getTransformXslIS(String sourceSchema, String targetSchema) throws FedoraAPIException {
        try {
            String strUrl = "http://" + rc.getFedoraHost() + ":" + rc.getFedoraPort() + "/services/" + sourceSchema + "_to_" + targetSchema + ".xsl";
            URL url = new URL(strUrl);
            return url.openStream();
        } catch (MalformedURLException mue) {
            throw new FedoraAPIException("Unable to get xsl transformation", mue);
        } catch (IOException ioe) {
            throw new FedoraAPIException("Unable to get xsl transformation", ioe);
        }
    }

    public String toString() {
        Document d = null;
        try {
            d = export();
        } catch (FedoraAPIException e) {
            return null;
        }
        return d.asXML();
    }

    private XPath getXPath(String s) {
        XPath xpathSelector = df.createXPath(s);
        xpathSelector.setNamespaceContext(new SimpleNamespaceContext(URImap));
        return xpathSelector;
    }
}
