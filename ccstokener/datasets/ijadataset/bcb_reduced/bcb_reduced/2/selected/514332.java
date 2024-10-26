package org.yaoqiang.bpmn.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yaoqiang.bpmn.model.BPMNModelParsingErrors.ErrorMessage;
import org.yaoqiang.bpmn.model.elements.XMLComplexElement;
import org.yaoqiang.bpmn.model.elements.XMLElement;
import org.yaoqiang.bpmn.model.elements.activities.Activity;
import org.yaoqiang.bpmn.model.elements.activities.CallActivity;
import org.yaoqiang.bpmn.model.elements.activities.SubProcess;
import org.yaoqiang.bpmn.model.elements.artifacts.Association;
import org.yaoqiang.bpmn.model.elements.artifacts.Category;
import org.yaoqiang.bpmn.model.elements.artifacts.CategoryValue;
import org.yaoqiang.bpmn.model.elements.bpmndi.BPMNEdge;
import org.yaoqiang.bpmn.model.elements.bpmndi.BPMNShape;
import org.yaoqiang.bpmn.model.elements.choreography.Choreography;
import org.yaoqiang.bpmn.model.elements.choreographyactivities.ChoreographyActivity;
import org.yaoqiang.bpmn.model.elements.choreographyactivities.ChoreographyTask;
import org.yaoqiang.bpmn.model.elements.choreographyactivities.SubChoreography;
import org.yaoqiang.bpmn.model.elements.collaboration.Collaboration;
import org.yaoqiang.bpmn.model.elements.collaboration.Participant;
import org.yaoqiang.bpmn.model.elements.core.common.BPMNError;
import org.yaoqiang.bpmn.model.elements.core.common.FlowElement;
import org.yaoqiang.bpmn.model.elements.core.common.FlowElementsContainer;
import org.yaoqiang.bpmn.model.elements.core.common.ItemDefinition;
import org.yaoqiang.bpmn.model.elements.core.common.Message;
import org.yaoqiang.bpmn.model.elements.core.common.Resource;
import org.yaoqiang.bpmn.model.elements.core.common.ResourceParameter;
import org.yaoqiang.bpmn.model.elements.core.common.SequenceFlow;
import org.yaoqiang.bpmn.model.elements.core.foundation.BaseElement;
import org.yaoqiang.bpmn.model.elements.core.foundation.Documentation;
import org.yaoqiang.bpmn.model.elements.core.infrastructure.Definitions;
import org.yaoqiang.bpmn.model.elements.data.DataObject;
import org.yaoqiang.bpmn.model.elements.data.DataObjectReference;
import org.yaoqiang.bpmn.model.elements.data.DataStore;
import org.yaoqiang.bpmn.model.elements.data.DataStoreReference;
import org.yaoqiang.bpmn.model.elements.events.BoundaryEvent;
import org.yaoqiang.bpmn.model.elements.events.CatchEvent;
import org.yaoqiang.bpmn.model.elements.events.Event;
import org.yaoqiang.bpmn.model.elements.events.ThrowEvent;
import org.yaoqiang.bpmn.model.elements.process.BPMNProcess;
import org.yaoqiang.bpmn.model.elements.process.Lane;
import org.yaoqiang.bpmn.model.elements.process.LaneSet;

/**
 * BPMNModelUtils
 * 
 * @author Shi Yaoqiang(shi_yaoqiang@yahoo.com)
 */
public class BPMNModelUtils {

    public static Definitions getDefinitions(XMLElement el) {
        if (el == null) return null;
        while (!(el instanceof Definitions)) {
            el = el.getParent();
            if (el == null) break;
        }
        return (Definitions) el;
    }

    public static boolean hasAttachments(BaseElement el) {
        boolean hasAttachments = false;
        for (XMLElement doc : el.getDocumentations().getXMLElements()) {
            String format = ((Documentation) doc).getTextFormat();
            if (format.length() != 0 && !format.equals("text/plain")) {
                return true;
            }
        }
        return hasAttachments;
    }

    public static void refreshTypes(Definitions defs) {
        defs.resetTypes();
        for (XMLElement el : defs.getItemDefinitions()) {
            ItemDefinition itemDefinition = (ItemDefinition) el;
            defs.addType(itemDefinition.getStructureRef());
        }
    }

    public static List<Object> getAllItemDefinitions(XMLElement el) {
        List<Object> items = new ArrayList<Object>();
        Definitions defs = getDefinitions(el);
        if (defs == null) {
            return items;
        } else {
            items.addAll(Arrays.asList(new String[] { "xsd:string", "xsd:boolean", "xsd:int", "xsd:integer", "xsd:long", "xsd:double", "xsd:float" }));
            items.addAll(defs.getItemDefinitions());
        }
        return items;
    }

    public static String getCategoryValueId(Definitions defs, String label) {
        String[] catvalue = label.split(":");
        if (catvalue.length != 2) {
            catvalue = new String[] { "", label };
        }
        for (XMLElement category : defs.getCategories()) {
            String catName = ((Category) category).getName();
            if (catvalue[0].equals(catName)) {
                for (XMLElement categoryValue : ((Category) category).getCategoryValueList()) {
                    String catValue = ((CategoryValue) categoryValue).getValue();
                    if (catvalue[1].equals(catValue)) {
                        return ((CategoryValue) categoryValue).getId();
                    }
                }
            }
        }
        return "";
    }

    public static Object[] getAllCategoryValues(Definitions defs) {
        List<String> categories = new ArrayList<String>();
        for (XMLElement category : defs.getCategories()) {
            String catName = ((Category) category).getName();
            for (XMLElement categoryValue : ((Category) category).getCategoryValueList()) {
                String catValue = ((CategoryValue) categoryValue).getValue();
                if (catName.length() == 0) {
                    categories.add(catValue);
                } else {
                    categories.add(catName + ":" + catValue);
                }
            }
        }
        return categories.toArray();
    }

    public static String getCategoryValueString(Definitions defs, String categoryValueId) {
        CategoryValue categoryValue = defs.getCategoryValue(categoryValueId);
        String catName = ((Category) categoryValue.getParent().getParent()).getName();
        String catValue = categoryValue.getValue();
        if (catName.length() == 0) {
            return catValue;
        } else {
            return catName + ":" + catValue;
        }
    }

    public static List<String> getCategoryValueList(Definitions defs, String flowElementId) {
        List<String> categoryValues = new ArrayList<String>();
        for (String id : getCachedCategoryValueIds(defs, flowElementId)) {
            categoryValues.add(getCategoryValueString(defs, id));
        }
        return categoryValues;
    }

    public static Set<String> getAllCategoryValueIds(Category category) {
        Set<String> ids = new HashSet<String>();
        for (XMLElement categoryValue : category.getCategoryValueList()) {
            ids.add(((CategoryValue) categoryValue).getId());
        }
        return ids;
    }

    public static Set<String> getCachedCategoryValueIds(Definitions defs, String flowElementId) {
        Set<String> categoryValues = new HashSet<String>();
        for (XMLElement cat : defs.getCategories()) {
            for (XMLElement catValue : ((Category) cat).getCategoryValueList()) {
                for (XMLElement el : ((CategoryValue) catValue).getCategorizedFlowElements(false)) {
                    if (((FlowElement) el).getId().equals(flowElementId)) categoryValues.add(((CategoryValue) catValue).getId());
                }
            }
        }
        return categoryValues;
    }

    public static List<XMLElement> getCategorizedFlowElements(CategoryValue categoryValue) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        String id = categoryValue.getId();
        Definitions defs = getDefinitions(categoryValue);
        for (XMLElement rootElement : defs.getRootElementList()) {
            flowElements.addAll(getFlowElements(rootElement, id, null));
        }
        return flowElements;
    }

    public static List<XMLElement> getAllDataObjects(Definitions defs, String itemId) {
        List<XMLElement> result = new ArrayList<XMLElement>();
        List<XMLElement> dataObjects = new ArrayList<XMLElement>();
        for (XMLElement p : getAllNoneEmptyProcesses(defs)) {
            BPMNProcess process = (BPMNProcess) p;
            dataObjects.addAll(process.getDataInOuts());
            for (XMLElement f : getFlowElements(process, null, null)) {
                if (f instanceof DataObject) {
                    dataObjects.add(f);
                } else if (f instanceof Activity) {
                    dataObjects.addAll(((Activity) f).getDataInOuts());
                } else if (f instanceof CatchEvent) {
                    dataObjects.addAll(((CatchEvent) f).getDataOutputList());
                } else if (f instanceof ThrowEvent) {
                    dataObjects.addAll(((ThrowEvent) f).getDataInputList());
                }
            }
        }
        if (itemId != null && itemId.length() != 0) {
            for (XMLElement o : dataObjects) {
                String itemRef = ((XMLComplexElement) o).get("itemSubjectRef").toValue();
                if (itemRef.length() == 0) {
                    continue;
                }
                int index = itemRef.indexOf(":");
                if (index != -1) {
                    itemRef = itemRef.substring(index + 1);
                }
                index = itemId.indexOf(":");
                if (index != -1) {
                    itemId = itemId.substring(index + 1);
                }
                if (itemRef.equals(itemId)) {
                    result.add(o);
                }
            }
        } else {
            return dataObjects;
        }
        return result;
    }

    public static Object[] getAllDataStores(Definitions defs) {
        List<String> dataStores = new ArrayList<String>();
        for (XMLElement dataStore : defs.getDataStores()) {
            dataStores.add(((DataStore) dataStore).getName());
        }
        return dataStores.toArray();
    }

    public static String getDataStoreId(Definitions defs, String label) {
        for (XMLElement dataStore : defs.getDataStores()) {
            if (((DataStore) dataStore).getName().equals(label)) {
                return ((DataStore) dataStore).getId();
            }
        }
        return "";
    }

    public static List<XMLElement> getAllFlowElements(XMLElement element) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        Definitions defs = getDefinitions(element);
        for (XMLElement rootElement : defs.getRootElementList()) {
            flowElements.addAll(getFlowElements(rootElement, null, null));
        }
        return flowElements;
    }

    public static List<XMLElement> getAllEvents(XMLElement element) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        Definitions defs = getDefinitions(element);
        for (XMLElement rootElement : defs.getRootElementList()) {
            flowElements.addAll(getFlowElements(rootElement, null, Event.class));
        }
        return flowElements;
    }

    public static List<XMLElement> getAllDataStoreRefs(XMLElement element) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        Definitions defs = getDefinitions(element);
        for (XMLElement rootElement : defs.getRootElementList()) {
            flowElements.addAll(getFlowElements(rootElement, null, DataStoreReference.class));
        }
        return flowElements;
    }

    public static List<XMLElement> getAllDataObjectRefs(XMLElement element) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        Definitions defs = getDefinitions(element);
        for (XMLElement rootElement : defs.getRootElementList()) {
            flowElements.addAll(getFlowElements(rootElement, null, DataObjectReference.class));
        }
        return flowElements;
    }

    public static List<XMLElement> getFlowElements(XMLElement element, String id, Class<?> type) {
        List<XMLElement> flowElements = new ArrayList<XMLElement>();
        if (element instanceof FlowElementsContainer) {
            for (XMLElement el : ((FlowElementsContainer) element).getFlowElements().getXMLElements()) {
                FlowElement flowElement = (FlowElement) el;
                if (id == null && type == null || type != null && type.isAssignableFrom(flowElement.getClass()) || flowElement.getCategoryValueRefSet().contains(id)) {
                    flowElements.add(flowElement);
                }
                if (flowElement instanceof FlowElementsContainer) {
                    flowElements.addAll(getFlowElements(flowElement, id, type));
                }
            }
        }
        return flowElements;
    }

    public static List<XMLElement> getDataInOuts(XMLElement actOrEvent, String type) {
        if (actOrEvent instanceof Activity) {
            if ("selectDataInput".equals(type)) {
                return ((Activity) actOrEvent).getIoSpecification().getDataInputList();
            } else {
                return ((Activity) actOrEvent).getIoSpecification().getDataOutputList();
            }
        } else if (actOrEvent instanceof ThrowEvent) {
            return ((ThrowEvent) actOrEvent).getDataInputList();
        } else if (actOrEvent instanceof CatchEvent) {
            return ((CatchEvent) actOrEvent).getDataOutputList();
        } else {
            return new ArrayList<XMLElement>();
        }
    }

    public static int getEventDefinitionRefNumbers(XMLElement element, String ref) {
        int count = 0;
        for (XMLElement el : getAllEvents(element)) {
            if (((Event) el).hasEventDefinitionRef(ref)) {
                count++;
            }
        }
        return count;
    }

    public static ResourceParameter getResourceParameter(Definitions defs, String paramRef) {
        ResourceParameter param = null;
        for (XMLElement el : defs.getResources()) {
            param = ((Resource) el).getResourceParameter(paramRef);
            if (param != null) {
                return param;
            }
        }
        return param;
    }

    public static BaseElement getDefaultFlowElementsContainer(Definitions defs) {
        BaseElement container = getChoreography(defs);
        if (container == null) {
            container = BPMNModelUtils.getCollaboration(defs);
            if (container == null) {
                container = BPMNModelUtils.getDefaultProcess(defs);
            }
        }
        return container;
    }

    public static Collaboration getCollaboration(Definitions defs) {
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof Collaboration && !(root instanceof Choreography)) {
                return (Collaboration) root;
            }
        }
        return null;
    }

    public static Choreography getChoreography(Definitions defs) {
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof Choreography) {
                return (Choreography) root;
            }
        }
        return null;
    }

    public static Choreography getParentChoreography(XMLElement el) {
        if (el == null) return null;
        while (!(el instanceof Choreography)) {
            el = el.getParent();
            if (el == null) break;
        }
        return (Choreography) el;
    }

    public static List<XMLElement> getAllNoneEmptyProcesses(Definitions defs) {
        List<XMLElement> processes = new ArrayList<XMLElement>();
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof BPMNProcess && !((BPMNProcess) root).isEmptyProcess()) {
                processes.add(root);
            }
        }
        return processes;
    }

    public static BPMNProcess getDefaultProcess(Definitions defs) {
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof BPMNProcess) {
                String participantId = getParticipantByProcessId(((BPMNProcess) root).getId(), defs);
                if (participantId.length() == 0 && !hasProcessRef(defs, ((BPMNProcess) root).getId())) {
                    return (BPMNProcess) root;
                }
            }
        }
        return null;
    }

    public static BPMNProcess getProcess(Definitions defs, String id) {
        if (id == null || id.length() == 0) return null;
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof BPMNProcess) {
                if (id.equals(((BPMNProcess) root).getId())) {
                    return (BPMNProcess) root;
                }
            }
        }
        return null;
    }

    public static BPMNProcess getParentProcess(XMLElement el) {
        if (el == null) return null;
        while (!(el instanceof BPMNProcess)) {
            el = el.getParent();
            if (el == null) break;
        }
        return (BPMNProcess) el;
    }

    public static String getParticipantByProcessId(String processId, Definitions defs) {
        for (XMLElement root : defs.getRootElementList()) {
            if (root instanceof Collaboration) {
                for (XMLElement part : ((Collaboration) root).getParticipantList()) {
                    String processRef = ((Participant) part).getProcessRef();
                    if (processRef == null || processRef.length() == 0) {
                        continue;
                    }
                    int index = processRef.indexOf(":");
                    if (index > 0) {
                        processRef = processRef.substring(index + 1);
                    }
                    if (processRef.equals(processId)) {
                        return ((Participant) part).getId();
                    }
                }
            }
        }
        return "";
    }

    public static Message getMessageByStructure(String type, Definitions defs) {
        for (XMLElement msg : defs.getMessages()) {
            String itemId = ((Message) msg).getItemRef();
            XMLElement item = defs.getRootElement(itemId);
            if (item != null) {
                if (((ItemDefinition) item).getStructureRef().equals(type)) {
                    return (Message) msg;
                }
            }
        }
        return null;
    }

    public static BPMNError getErrorByStructure(String type, Definitions defs) {
        for (XMLElement err : defs.getErrors()) {
            String itemId = ((BPMNError) err).getStructureRef();
            XMLElement item = defs.getRootElement(itemId);
            if (item != null) {
                if (((ItemDefinition) item).getStructureRef().equals(type)) {
                    return (BPMNError) err;
                }
            }
        }
        return null;
    }

    public static ItemDefinition getItemDefinitionByStructure(String type, Definitions defs) {
        for (XMLElement id : defs.getItemDefinitions()) {
            if (((ItemDefinition) id).getStructureRef().equals(type)) {
                return (ItemDefinition) id;
            }
        }
        return null;
    }

    public static boolean hasItemDefinition(String type, Definitions defs) {
        for (XMLElement id : defs.getItemDefinitions()) {
            if (((ItemDefinition) id).getStructureRef().equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasMessageFlowRef(FlowElementsContainer container, String messageFlowId) {
        boolean has = false;
        for (XMLElement flowElement : container.getFlowElements().getXMLElements()) {
            if (flowElement instanceof ChoreographyTask) {
                for (XMLElement mfRef : ((ChoreographyTask) flowElement).getMessageFlowRefList()) {
                    if (messageFlowId.equals(mfRef.toValue())) {
                        return true;
                    }
                }
            } else if (flowElement instanceof SubChoreography) {
                has = hasMessageFlowRef((SubChoreography) flowElement, messageFlowId);
            }
        }
        return has;
    }

    public static boolean hasChoreographyActivity(Definitions defs) {
        Choreography choreography = BPMNModelUtils.getChoreography(defs);
        if (choreography == null) {
            return false;
        }
        for (XMLElement flowElement : choreography.getFlowElementList()) {
            if (flowElement instanceof ChoreographyActivity) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasParticipantRef(XMLElement owner, String participantId) {
        Choreography choreography = BPMNModelUtils.getChoreography(BPMNModelUtils.getDefinitions(owner));
        if (choreography == null) {
            return false;
        }
        Set<ChoreographyActivity> acts = getChoreographyActivityByParticipantRef(choreography, participantId);
        acts.remove(owner);
        if (acts.size() > 0) {
            return true;
        }
        return false;
    }

    public static boolean hasProcessRef(Definitions defs, String id) {
        boolean has = false;
        for (XMLElement el : getAllNoneEmptyProcesses(defs)) {
            has = hasProcessRef((FlowElementsContainer) el, id);
        }
        return has;
    }

    public static boolean hasProcessRef(FlowElementsContainer container, String id) {
        boolean has = false;
        for (XMLElement f : container.getFlowElements().getXMLElements()) {
            if (f instanceof CallActivity) {
                if (((CallActivity) f).getCalledElement().equals(id)) {
                    return true;
                }
            } else if (f instanceof SubProcess) {
                has = hasProcessRef((FlowElementsContainer) f, id);
            }
        }
        return has;
    }

    public static Set<ChoreographyActivity> getChoreographyActivityByParticipantRef(Definitions defs, String participantId) {
        Set<ChoreographyActivity> acts = new HashSet<ChoreographyActivity>();
        Choreography choreography = BPMNModelUtils.getChoreography(defs);
        if (choreography == null) {
            return acts;
        }
        acts.addAll(getChoreographyActivityByParticipantRef(choreography, participantId));
        return acts;
    }

    public static Set<ChoreographyActivity> getChoreographyActivityByParticipantRef(FlowElementsContainer container, String participantId) {
        Set<ChoreographyActivity> acts = new HashSet<ChoreographyActivity>();
        for (XMLElement flowElement : container.getFlowElements().getXMLElements()) {
            if (flowElement instanceof ChoreographyTask) {
                for (String participant : ((ChoreographyTask) flowElement).getParticipantList()) {
                    if (participantId.equals(participant)) {
                        acts.add((ChoreographyTask) flowElement);
                    }
                }
            } else if (flowElement instanceof SubChoreography) {
                for (String participant : ((SubChoreography) flowElement).getParticipantList()) {
                    if (participantId.equals(participant)) {
                        acts.add((SubChoreography) flowElement);
                    }
                }
                acts.addAll(getChoreographyActivityByParticipantRef((SubChoreography) flowElement, participantId));
            }
        }
        return acts;
    }

    public static Set<XMLElement> getLanes(LaneSet laneSet) {
        Set<XMLElement> lanes = new HashSet<XMLElement>();
        if (laneSet == null) {
            return lanes;
        }
        for (XMLElement lane : laneSet.getLaneList()) {
            LaneSet childLaneSet = ((Lane) lane).getChildLaneSet();
            if (childLaneSet.getLaneList().size() == 0) {
                lanes.add(lane);
            } else {
                lanes.addAll(getLanes(((Lane) lane).getChildLaneSet()));
            }
        }
        return lanes;
    }

    public static void generateBPMNDI(FlowElementsContainer container, Map<String, XMLElement> bpmnElementMap, List<XMLElement> shapes, List<XMLElement> edges) {
        int num = 2;
        List<XMLElement> elements = container.getFlowElements().getXMLElements();
        elements.addAll(container.getArtifacts().getXMLElements());
        for (XMLElement e : elements) {
            BaseElement element = (BaseElement) e;
            if (element instanceof FlowElementsContainer) {
                generateBPMNDI((FlowElementsContainer) element, bpmnElementMap, shapes, edges);
            }
            String id = element.getId();
            if (id.length() == 0) {
                id = "_" + num;
                while (bpmnElementMap.containsKey(id)) {
                    id = "_" + ++num;
                }
                element.setId(id);
                bpmnElementMap.put(id, element);
            }
            if (element instanceof SequenceFlow || element instanceof Association) {
                BPMNEdge edge = new BPMNEdge(null);
                edge.setBpmnElement(id);
                edges.add(edge);
            } else if (!(element instanceof DataObject)) {
                BPMNShape shape = new BPMNShape(null);
                shape.setBpmnElement(id);
                if (element instanceof BoundaryEvent) {
                    shapes.add(shape);
                } else {
                    shapes.add(0, shape);
                }
            }
        }
    }

    public static Document parseDocument(Object toParse, boolean isFile, List<ErrorMessage> errorMessages) {
        Document document = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(true);
            docBuilderFactory.setNamespaceAware(true);
            docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
            docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", "BPMN20.xsd");
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            BPMNModelParsingErrors pErrors = new BPMNModelParsingErrors();
            docBuilder.setErrorHandler(pErrors);
            docBuilder.setEntityResolver(new BPMNModelEntityResolver());
            if (isFile) {
                String filepath = toParse.toString();
                File f = new File(filepath);
                if (!f.exists()) {
                    URL url = BPMNModelUtils.class.getResource(filepath);
                    if (url == null) {
                        if (filepath.startsWith("http") || filepath.startsWith("ftp")) {
                            url = new URL(filepath);
                        }
                    }
                    if (url != null) {
                        document = docBuilder.parse(url.openStream());
                    }
                } else {
                    if (filepath.endsWith(".gz")) {
                        document = docBuilder.parse(new GZIPInputStream(new FileInputStream(f)));
                    } else {
                        document = docBuilder.parse(new FileInputStream(f));
                    }
                }
            } else {
                if (toParse instanceof String) {
                    document = docBuilder.parse(new InputSource(new StringReader(toParse.toString())));
                } else if (toParse instanceof InputStream) {
                    document = docBuilder.parse((InputStream) toParse);
                }
            }
            errorMessages.addAll(pErrors.getErrorMessages());
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static String getShortClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf(".");
        if (lastDot >= 0) {
            return fullClassName.substring(lastDot + 1, fullClassName.length());
        }
        return fullClassName;
    }

    public static Node getChildByName(Node parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getLocalName() != null && child.getLocalName().equals(childName)) {
                return child;
            }
        }
        return null;
    }

    public static String getChildNodesContent(Node node) {
        String txt = "";
        if (node != null) {
            if (node.hasChildNodes()) {
                Node fc = node.getFirstChild();
                if (fc.getNodeType() == Node.CDATA_SECTION_NODE) {
                    return ((CDATASection) fc).getData();
                }
                txt = BPMNModelUtils.getContent(node, true);
                try {
                    String fcnc = BPMNModelUtils.getContent(fc, true);
                    String closedTag = "</" + node.getNodeName() + ">";
                    if (fcnc.trim().length() > 0) {
                        fcnc = fcnc.trim();
                    }
                    int i1, i2;
                    i1 = txt.lastIndexOf(fcnc);
                    i2 = txt.lastIndexOf(closedTag);
                    txt = txt.substring(i1, i2).trim();
                    if (txt.startsWith("<![CDATA[") && txt.endsWith("]]>")) {
                        txt = txt.substring(9, txt.lastIndexOf("]]>"));
                    }
                } catch (Exception ex) {
                    NodeList nl = node.getChildNodes();
                    txt = "";
                    try {
                        for (int i = 0; i < nl.getLength(); i++) {
                            Node sn = nl.item(i);
                            if (sn instanceof Element) {
                                txt += BPMNModelUtils.getContent(sn, true);
                            } else {
                                String nv = sn.getNodeValue();
                                if (i > 0) {
                                    txt += nv.substring(1);
                                } else if (i == 0 && nv.trim().length() == 0) {
                                    continue;
                                } else {
                                    txt += nv;
                                }
                            }
                        }
                    } catch (Exception ex2) {
                    }
                }
            }
        }
        return txt;
    }

    public static String getContent(Node node, boolean omitXMLDeclaration) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty("encoding", "UTF-8");
            if (omitXMLDeclaration) {
                transformer.setOutputProperty("omit-xml-declaration", "yes");
            }
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
            String cont = baos.toString("UTF8");
            baos.close();
            return cont;
        } catch (Exception ex) {
            return "";
        }
    }
}
