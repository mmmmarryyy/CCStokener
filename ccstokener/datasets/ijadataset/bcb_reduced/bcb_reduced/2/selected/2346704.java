package Bindings;

import Common.myXML;
import Message.AbstractMessage;
import Message.MessageFieldType;
import Message.PacketField;
import Monitoring.Exceptions.BindingException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import starlink.ApplicationAutomata.ActionMessage;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import starlink.AbstractMessage.Message;
import starlink.AbstractMessage.ProtocolMessage;
import starlink.AbstractMessage.XMLFactoring;
import starlink.ApplicationAutomata.Action;
import starlink.ApplicationAutomata.ActionMessageField;
import starlink.ApplicationAutomata.BindResults;
import starlink.ApplicationAutomata.BoundType;
import starlink.ApplicationAutomata.CallException;
import starlink.ApplicationAutomata.Inputs;
import starlink.ApplicationAutomata.StateType;
import starlink.ColouredAutomata.Bridge;
import starlink.ColouredAutomata.StateMachine;

/**
 * Operations to bind abstract application automata actions to concrete
 * protocol send and receive actions.
 *
 * @author Paul Grace
 */
public class BindingOperations {

    private XMLFactoring xml;

    private static String thisDir = System.getProperty("user.dir");

    ;

    private static String separator = System.getProperty("file.separator");

    ;

    private static String CAFolder = "AbstractMessageTemplate";

    private starlink.ColouredAutomata.ProtocolBridge colouredAutomataEngine;

    private static BindingOperations instance = null;

    /**
     * BindingOperations is a singleton instance that is created once per
     * Starlink instantiation.
     */
    protected BindingOperations() {
        xml = XMLFactoring.getInstance();
    }

    public static BindingOperations getInstance() {
        if (instance == null) {
            instance = new BindingOperations();
        }
        return instance;
    }

    public void setEngine(starlink.ColouredAutomata.ProtocolBridge ae) {
        this.colouredAutomataEngine = ae;
    }

    public Object castValue(String type, String Value) {
        if (type.equalsIgnoreCase("java.lang.double")) {
            return new Double(Value);
        } else return null;
    }

    /**
     *
     * @param AM
     * @param msg
     * @param firstState
     * @param binding
     * @return
     * @throws BindingException
     */
    public AbstractMessage bindAction(ActionMessage AM, String msg, starlink.ColouredAutomata.State firstState, Document binding) throws BindingException {
        try {
            AbstractMessage toCompose = (AbstractMessage) firstState.getQueue().get(msg);
            XPath xpa = XPath.newInstance("/Binding/abstractTemplate");
            Element elt = (Element) xpa.selectSingleNode(binding);
            String label = elt.getTextTrim();
            ProtocolMessage newMessage = xml.createMessages(new File(thisDir + separator + "DSL" + separator + CAFolder + separator + label));
            if (toCompose == null) {
                List<Message> m = newMessage.getMessage();
                for (int i = 0; i < m.size(); i++) {
                    Message m1 = m.get(i);
                    if (m1.getName().equalsIgnoreCase(msg)) toCompose = xml.constructPacket(m1);
                }
            }
            String xpathexpr = "/Binding/send/Action/field/xpath";
            xpa = XPath.newInstance(xpathexpr);
            elt = (Element) xpa.selectSingleNode(binding);
            String xpathBinding = elt.getTextTrim();
            if (!xpathBinding.equalsIgnoreCase("null")) {
                xml.setXPathValue(toCompose, xpathBinding, AM.getAction());
            }
            return toCompose;
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    /**
     *
     * @param mPacket
     * @param label
     * @param type
     * @param value
     * @param bType
     * @param flag
     * @param boundTypes
     * @throws BindingException
     */
    private void bindStruct(AbstractMessage mPacket, String label, String type, Object value, BoundType bType, int flag, HashMap<String, BoundType> boundTypes) throws BindingException {
        try {
            AbstractMessage array = mPacket;
            if (flag == 0) array = new AbstractMessage(label.toLowerCase());
            for (int l = 0; l < bType.elementType.size(); l++) {
                MessageFieldType mType = (MessageFieldType) bType.elementType.get(l);
                PacketField pf = new PacketField(mType.Type.toLowerCase(), mType.Encoding);
                BoundType bTypeInside = boundTypes.get(mType.Encoding.toLowerCase());
                if (mType.Type.equalsIgnoreCase("ArrayValue")) {
                    Vector resVal = new Vector();
                    if (mType.Encoding.equalsIgnoreCase("java.lang.integer")) {
                        int[] res = (int[]) value;
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    if (mType.Encoding.equalsIgnoreCase("java.lang.long")) {
                        long[] res = (long[]) value;
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    if (mType.Encoding.equalsIgnoreCase("java.lang.float")) {
                        float[] res = (float[]) value;
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    if (mType.Encoding.equalsIgnoreCase("java.lang.double")) {
                        double[] res = (double[]) value;
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    if (mType.Encoding.equalsIgnoreCase("java.lang.boolean")) {
                        boolean[] res = (boolean[]) value;
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    if (mType.Encoding.equalsIgnoreCase("java.lang.string")) {
                        String[] res = null;
                        if (value.getClass().getCanonicalName().contains("Vector")) {
                            res = new String[((Vector) value).size()];
                            for (int i = 0; i < ((Vector) value).size(); i++) {
                                res[i] = (String) ((Vector) value).get(i);
                            }
                        } else if (value.getClass().getCanonicalName().equalsIgnoreCase("Ljava.lang.string")) {
                            res = (String[]) value;
                        }
                        for (int i = 0; i < res.length; i++) {
                            resVal.add(res[i]);
                        }
                    }
                    Vector<AbstractMessage> hh = new Vector<AbstractMessage>(resVal.size());
                    for (int i = 0; i < resVal.size(); i++) {
                        AbstractMessage h = new AbstractMessage("i" + i);
                        bindStruct((AbstractMessage) h, "i" + i, mType.Encoding, resVal.get(i), bTypeInside, 1, boundTypes);
                        hh.add(h);
                    }
                    pf.Value = hh;
                    array.Fields.put(mType.Type.toLowerCase(), pf);
                } else {
                    xml.writePrimitiveField(array, mType.Type.toLowerCase(), type);
                    PacketField pp = array.Fields.get(mType.Type.toLowerCase());
                    if (pp != null) xml.setXPathValue(array, "/field/primitiveField[label='" + mType.Type.toLowerCase() + "']", value);
                }
            }
            if (flag == 0) mPacket.StructuredFields.put(label.toLowerCase(), array);
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    public String[] getLabels(URL input, String operationName) {
        try {
            URL url = new URL(input.toString() + "?wsdl");
            HttpURLConnection hConn = (HttpURLConnection) url.openConnection();
            hConn.setRequestMethod("GET");
            hConn.setDoOutput(true);
            hConn.setReadTimeout(10000);
            hConn.connect();
            BufferedReader rd = new BufferedReader(new InputStreamReader(hConn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            myXML wsdlDoc = new myXML(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sb.toString().getBytes()))));
            myXML parsingType = wsdlDoc.findElement("wsdl:definitions");
            String pAdd = "wsdl:";
            if (parsingType == null) pAdd = "";
            myXML portTypes = wsdlDoc.findElement(pAdd + "portType");
            String toLocation = null;
            for (int ops = 0; ops < portTypes.size(); ops++) {
                myXML inner = portTypes.getElement(ops);
                String method = inner.Attribute.find("name");
                if (method.equalsIgnoreCase(operationName)) {
                    myXML inputMess = inner.findElement(pAdd + "input");
                    toLocation = inputMess.Attribute.find("message");
                    break;
                }
            }
            if (toLocation.contains(":")) toLocation = toLocation.substring(toLocation.indexOf(":") + 1);
            String typeLocation = null;
            myXML inner = null;
            for (int ops = 0; ops < wsdlDoc.size(); ops++) {
                inner = wsdlDoc.getElement(ops);
                if (inner.getTag().equalsIgnoreCase(pAdd + "message")) {
                    String method = inner.Attribute.find("name");
                    if (method.equalsIgnoreCase(toLocation)) {
                        typeLocation = inner.getElement(0).Attribute.find("element");
                        break;
                    }
                }
            }
            String[] Labels = null;
            if (typeLocation == null) {
                Labels = new String[inner.size()];
                for (int i = 0; i < inner.size(); i++) {
                    myXML inner2 = inner.getElement(i);
                    Labels[i] = "ns:" + inner2.Attribute.find("name");
                    System.out.println(Labels[i]);
                }
            } else {
                if (typeLocation.contains(":")) typeLocation = typeLocation.substring(typeLocation.indexOf(":") + 1);
                myXML typeElements = wsdlDoc.findElement(pAdd + "types").getElement(0);
                myXML labelList = null;
                for (int ops = 0; ops < typeElements.size(); ops++) {
                    myXML inner2 = typeElements.getElement(ops);
                    String method = inner2.Attribute.find("name");
                    if (method.equalsIgnoreCase(typeLocation)) {
                        labelList = inner2.getElement(0).getElement(0);
                        break;
                    }
                }
                Labels = new String[labelList.size()];
                for (int i = 0; i < labelList.size(); i++) {
                    myXML inner2 = labelList.getElement(i);
                    Labels[i] = "ns:" + inner2.Attribute.find("name");
                    System.out.println(Labels[i]);
                }
            }
            return Labels;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     *
     * @param mPacket
     * @param amF
     */
    private void bindPrimitive(AbstractMessage mPacket, ActionMessageField amF, BoundType bt) {
        xml.writePrimitiveField(mPacket, amF.label, bt.typeLabel);
        xml.setXPathValue(mPacket, "/field/primitiveField[label='" + amF.label + "']", amF.value);
    }

    private BoundType reverseType(HashMap<String, BoundType> boundTypes, String type) {
        Iterator list = boundTypes.keySet().iterator();
        while (list.hasNext()) {
            BoundType bt = boundTypes.get(list.next());
            if (bt.typeLabel.equalsIgnoreCase(type)) {
                return bt;
            }
        }
        return null;
    }

    /**
     *
     * @param inputAction
     * @param msg
     */
    public void bindOutputValues(ActionMessage inputAction, AbstractMessage msg, Document binding, HashMap<String, BoundType> boundTypes) throws BindingException {
        try {
            XPath xpa = XPath.newInstance("/Binding/send/Output");
            Element root = (Element) xpa.selectSingleNode(binding);
            List<Element> outputBindings = root.getChildren();
            for (int j = 0; j < outputBindings.size(); j++) {
                Element outputBinding = (Element) outputBindings.get(j);
                if (outputBinding.getName().equalsIgnoreCase("list")) {
                    String xpathLabel = outputBinding.getChild("xpath").getTextTrim();
                    AbstractMessage outputList = msg.StructuredFields.remove(xml.getXPathFieldLabel(xpathLabel));
                    String toParse = "";
                    for (int i = 0; i < inputAction.getOutputFields().size(); i++) {
                        ActionMessageField amF = inputAction.getOutputFields().get(i);
                        BoundType bType = reverseType(boundTypes, amF.type.toLowerCase());
                        if (bType.primitive) {
                            bindPrimitive(outputList, amF, bType);
                        } else {
                            if (outputList == null) outputList = new AbstractMessage(xml.getXPathFieldLabel(xpathLabel));
                            bindStruct(outputList, amF.label, amF.type, amF.getValue(), bType, 0, boundTypes);
                            toParse += "<" + amF.label.toLowerCase() + ":" + amF.type + ":8>";
                        }
                    }
                    if (outputList != null) msg.addStructuredField(outputList, toParse);
                }
            }
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    /**
     * 
     * @param inps
     * @param am
     */
    public void bindInputValues(Inputs inps, ActionMessage am, Document binding, HashMap<String, BoundType> boundTypes) throws BindingException {
        try {
            XPath xpa = XPath.newInstance("/Binding/recv/Input");
            Element elt11 = (Element) xpa.selectSingleNode(binding);
            List nm = elt11.getChildren();
            Element elt = (Element) nm.get(0);
            if (elt.getName().equalsIgnoreCase("list")) {
                String pfVal = elt.getChild("xpath").getTextTrim();
                AbstractMessage seq = xml.getXPathStructuredField(am.origAbstractMessage, pfVal);
                for (int i = 0; i < inps.inputs.size(); i++) {
                    ActionMessageField amF = inps.get(i);
                    if (amF.type.equalsIgnoreCase("void")) {
                        return;
                    }
                    BoundType bType = boundTypes.get(amF.type.toLowerCase());
                    if (bType.primitive) {
                        try {
                            amF.setValue(seq.Fields.get(amF.label.toLowerCase()).Value);
                        } catch (Exception e) {
                            Iterator iit = seq.Fields.keySet().iterator();
                            int poi = 0;
                            while (iit.hasNext()) {
                                String key = (String) iit.next();
                                if (poi == i) {
                                    Object val = seq.Fields.get(key).Value;
                                    String tp = val.getClass().toString().substring(16);
                                    if (bType.typeLabel.equalsIgnoreCase("wsdlname")) {
                                        amF.setValue(castValue(amF.type, (String) val));
                                    } else if (tp.equalsIgnoreCase(bType.typeLabel)) {
                                        amF.setValue(val);
                                    } else {
                                        amF.setValue(castValue(bType.typeLabel, (String) val));
                                    }
                                }
                                poi++;
                            }
                        }
                        am.setInputField(amF);
                    } else {
                        AbstractMessage inner = seq.StructuredFields.get(amF.label.toLowerCase());
                        inner.DynamicParserContent = "<" + amF.label.toLowerCase() + ":" + amF.type + ":8>";
                        amF.setValue(inner.Fields.get(bType.typeValue.toLowerCase()).Value);
                        am.setInputField(amF);
                    }
                }
            }
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    public Action bindAction(Document xmlBinding, int type) throws BindingException {
        try {
            String statement = "/Binding";
            switch(type) {
                case 0:
                    statement = statement + "/recv";
                    break;
                case 1:
                    statement = statement + "/send";
                    break;
            }
            XPath xpa = XPath.newInstance(statement + "/Action/field/state");
            Element elt = (Element) xpa.selectSingleNode(xmlBinding);
            String label = elt.getTextTrim();
            xpa = XPath.newInstance(statement + "/Action/field/message");
            elt = (Element) xpa.selectSingleNode(xmlBinding);
            String msg = elt.getTextTrim();
            xpa = XPath.newInstance(statement + "/Action/field/xpath");
            elt = (Element) xpa.selectSingleNode(xmlBinding);
            String xpath = elt.getTextTrim();
            return new Action(label, msg, xpath);
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    public void bindLogic(Document xmlBinding, int type, starlink.ColouredAutomata.StateMachine LocalStateMachine) throws BindingException {
        try {
            String statement = "/Binding";
            switch(type) {
                case 0:
                    statement = statement + "/recv";
                    break;
                case 1:
                    statement = statement + "/send";
                    break;
            }
            XPath xpa = XPath.newInstance(statement + "/Action/logic");
            Element elt1 = (Element) xpa.selectSingleNode(xmlBinding);
            if (elt1 != null) {
                xpa = XPath.newInstance(statement + "/Action/logic/state");
                elt1 = (Element) xpa.selectSingleNode(xmlBinding);
                String stateLabel = elt1.getTextTrim();
                xpa = XPath.newInstance(statement + "/Action/logic/translationlogic");
                Element elt2 = (Element) xpa.selectSingleNode(xmlBinding);
                starlink.ColouredAutomata.NoActionState sTmp = (starlink.ColouredAutomata.NoActionState) LocalStateMachine.getState(stateLabel);
                if (sTmp.boundLogic == false) {
                    List<Element> lst = elt2.getChildren();
                    for (int i = 0; i < lst.size(); i++) {
                        Element lTmp = lst.get(i);
                        sTmp.addLogic(lTmp);
                    }
                }
                sTmp.boundLogic = true;
            }
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    private MessageFieldType getField(Element e) {
        try {
            XPath xpa = XPath.newInstance("./name");
            Element root = (Element) xpa.selectSingleNode(e);
            xpa = XPath.newInstance("./primitive");
            Element type = (Element) xpa.selectSingleNode(e);
            if (type == null) {
                xpa = XPath.newInstance("./element");
                type = (Element) xpa.selectSingleNode(e);
            }
            MessageFieldType mfT = new MessageFieldType(root.getTextTrim(), type.getTextTrim());
            return mfT;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void bindTypes(Document xmlBinding, HashMap<String, BoundType> boundTypes) {
        try {
            XPath xpa = XPath.newInstance("/Binding/types");
            Element root = (Element) xpa.selectSingleNode(xmlBinding);
            if (root != null) {
                List<Element> types = root.getChildren();
                for (int i = 0; i < types.size(); i++) {
                    Element type = types.get(i);
                    xpa = XPath.newInstance("./binding");
                    Element tmp = (Element) xpa.selectSingleNode(type);
                    String binding = tmp.getTextTrim();
                    xpa = XPath.newInstance("./label");
                    tmp = (Element) xpa.selectSingleNode(type);
                    String label = tmp.getTextTrim();
                    xpa = XPath.newInstance("./value");
                    tmp = (Element) xpa.selectSingleNode(type);
                    String value = tmp.getTextTrim();
                    xpa = XPath.newInstance("./Fields");
                    tmp = (Element) xpa.selectSingleNode(type);
                    Vector eType = new Vector();
                    if (tmp != null) {
                        List<Element> lst = tmp.getChildren();
                        for (int j = 0; j < lst.size(); j++) {
                            eType.add(getField(lst.get(j)));
                        }
                    }
                    boundTypes.put(label, new BoundType(binding, value, eType));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CallException bindExceptions(Document xmlBinding) {
        CallException ce = null;
        try {
            XPath xpa = XPath.newInstance("/Binding/error");
            Element root = (Element) xpa.selectSingleNode(xmlBinding);
            if (root != null) {
                Element eltInput = (Element) xpa.selectSingleNode(xmlBinding);
                xpa = XPath.newInstance("./message");
                Element elt = (Element) xpa.selectSingleNode(eltInput);
                String msg = elt.getTextTrim();
                xpa = XPath.newInstance("./exception");
                elt = (Element) xpa.selectSingleNode(eltInput);
                String ceType = elt.getTextTrim();
                xpa = XPath.newInstance("./value");
                elt = (Element) xpa.selectSingleNode(eltInput);
                String val = elt.getTextTrim();
                ce = new CallException(msg, ceType, val);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ce;
    }

    public void bindInputs(Element xmlBinding, Inputs boundInputs) {
        try {
            XPath xpa = XPath.newInstance("./transition/Inputs");
            Element eltInputs = (Element) xpa.selectSingleNode(xmlBinding);
            List<Element> states = eltInputs.getChildren();
            for (Element elt1 : states) {
                xpa = XPath.newInstance("./simpleType");
                Element elt = (Element) xpa.selectSingleNode(elt1);
                if (elt == null) {
                    xpa = XPath.newInstance("./complexType");
                    elt = (Element) xpa.selectSingleNode(elt1);
                }
                boundInputs.addInputField(new ActionMessageField(elt.getChildText("name"), elt.getChildText("type")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindOutputs(Element xmlBinding, Inputs boundOutputs) {
        try {
            XPath xpa = XPath.newInstance("./transition/Outputs");
            Element eltInputs = (Element) xpa.selectSingleNode(xmlBinding);
            if (eltInputs != null) {
                List<Element> states = eltInputs.getChildren();
                for (Element elt1 : states) {
                    xpa = XPath.newInstance("./simpleType");
                    Element elt = (Element) xpa.selectSingleNode(elt1);
                    if (elt == null) {
                        xpa = XPath.newInstance("./complexType");
                        elt = (Element) xpa.selectSingleNode(elt1);
                    }
                    boundOutputs.addInputField(new ActionMessageField(elt.getChildText("name"), elt.getChildText("type")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BindResults bindStates(StateType type, Document xmlBinding, starlink.ColouredAutomata.ProtocolBridge ae) throws BindingException {
        try {
            XPath xpa = XPath.newInstance("/Binding/protocol");
            Element elt = (Element) xpa.selectSingleNode(xmlBinding);
            String File = thisDir + separator + "DSL" + separator + "ColouredAutomata" + separator + elt.getTextTrim();
            Document binding = null;
            try {
                binding = jDomReadXmlStream(new FileInputStream(File));
            } catch (Exception e) {
                throw new BindingException(e.getMessage());
            }
            starlink.ColouredAutomata.StateMachine LocalSM = (StateMachine) ae.stms.get(File);
            if (LocalSM == null) {
                LocalSM = Bridge.createStateMachine(binding, new HashMap<String, StateMachine>());
                System.out.println("New SM: " + File);
                System.out.println("New SM Code: " + LocalSM.hashCode());
                ae.stms.put(File, LocalSM);
            }
            String xpathPointer = "/Binding";
            if (type == type.RECV) {
                xpathPointer += "/recv";
            } else {
                xpathPointer += "/send";
            }
            String startlabel = xpathPointer.concat("/start");
            xpa = XPath.newInstance(startlabel);
            elt = (Element) xpa.selectSingleNode(xmlBinding);
            startlabel = elt.getTextTrim();
            String endlabel = xpathPointer.concat("/end");
            xpa = XPath.newInstance(endlabel);
            elt = (Element) xpa.selectSingleNode(xmlBinding);
            endlabel = elt.getTextTrim();
            BindResults ret = new BindResults(startlabel, endlabel, LocalSM);
            return ret;
        } catch (Exception e) {
            throw new BindingException(e.getMessage());
        }
    }

    public Document jDomReadXmlStream(InputStream in) {
        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(in);
            Element root = doc.getRootElement();
            System.out.println("Root element is " + root.getName());
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}
