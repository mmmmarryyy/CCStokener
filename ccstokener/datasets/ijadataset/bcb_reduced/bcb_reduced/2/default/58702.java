import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Parser {

    private ArrayList elements = new ArrayList();

    private HashMap operations = new HashMap();

    private ArrayList messages = new ArrayList();

    private HashMap bindings = new HashMap();

    public Parser() {
        super();
    }

    public void parseWSDL(URL WSDLurl) throws IOException, XMLStreamException {
        InputStream in = WSDLurl.openStream();
        XMLInputFactory factory = null;
        factory = XMLInputFactory.newInstance();
        XMLStreamReader parser = factory.createXMLStreamReader(in);
    }

    public void parseFile(File wsdl) throws IOException, XMLStreamException {
        InputStream in = new FileInputStream(wsdl);
        XMLInputFactory factory = null;
        factory = XMLInputFactory.newInstance();
        XMLStreamReader parser = factory.createXMLStreamReader(in);
        boolean operation = false;
        boolean portType = false;
        boolean message = false;
        boolean simpleType = false;
        WSDLOperation wsdlOp = null;
        WSDLMessage wsdlMessage = null;
        String outerElem = null;
        Element elem = null;
        int tagCount = 0;
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.END_DOCUMENT) {
                parser.close();
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ((parser.getLocalName().equals("element") || parser.getLocalName().equals("simpleType") || parser.getLocalName().equals("complexType"))) {
                    tagCount++;
                    if (parser.getAttributeCount() >= 1) {
                        elem = new Element();
                        if (parser.getLocalName().equals("simpleType")) {
                            simpleType = true;
                            if (parser.getAttributeCount() > 0 && parser.getAttributeName(0).equals("name")) for (int i = 0; i < elements.size(); i++) {
                                Element el = (Element) elements.get(i);
                                if (el.getType() != null && el.getType().equals(parser.getAttributeValue(0))) {
                                    elem = el;
                                    System.out.println("Caught it!!" + elem.getName());
                                    break;
                                }
                            }
                            System.out.println("hit continue.....");
                            continue;
                        }
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals("name")) {
                                elem.setName(parser.getAttributeValue(i));
                                if (outerElem == null && tagCount == 1) {
                                    outerElem = parser.getAttributeValue(i);
                                }
                                elem.setParentElement(outerElem);
                            } else if (parser.getAttributeName(i).equals("type")) {
                                elem.setType(stripPrefix(parser.getAttributeValue(i)));
                            } else if (parser.getAttributeName(i).equals("minOccurs")) {
                                elem.setMinOccurs(parser.getAttributeValue(i));
                            } else if (parser.getAttributeName(i).equals("maxOccurs")) {
                                elem.setMaxOccurs(parser.getAttributeValue(i));
                            } else if (parser.getAttributeName(i).equals("base")) {
                                elem.setBase(parser.getAttributeValue(i));
                            } else if (parser.getAttributeName(i).equals("nillable")) {
                                if (parser.getAttributeValue(i).equals("true")) elem.setNillable(true); else if (parser.getAttributeValue(i).equals("false")) elem.setNillable(false);
                            }
                        }
                        elements.add(elem);
                    }
                } else if (parser.getLocalName().equals("restriction") && simpleType) {
                    if (parser.getAttributeName(0).equals("base")) {
                        if (elem != null) {
                            elem.setRestricted(true);
                            elem.setRestrictionBase(parser.getAttributeValue(0));
                            simpleType = false;
                        }
                    }
                } else if (parser.getLocalName().equals("enumeration")) {
                    if (parser.getAttributeName(0).equals("value")) {
                        if (elem != null) {
                            System.out.println("Adding enumeration value: " + parser.getAttributeValue(0));
                            elem.addRestrictionValue(parser.getAttributeValue(0));
                        }
                    }
                } else if (parser.getLocalName().equals("portType") && parser.getAttributeName(0).equals("name")) {
                    System.out.println("Setting port type...");
                    portType = true;
                } else if (portType && parser.getLocalName().equals("operation") && parser.getAttributeName(0).equals("name")) {
                    System.out.println("New WSDL operation.... : " + parser.getAttributeValue(0));
                    wsdlOp = new WSDLOperation();
                    wsdlOp.setOperationName(parser.getAttributeValue(0));
                    operation = true;
                } else if (portType && operation && parser.getLocalName().equals("input") && parser.getAttributeName(0).equals("message")) {
                    wsdlOp.setOperationInput(parser.getAttributeValue(0));
                } else if (portType && operation && parser.getLocalName().equals("output") && parser.getAttributeName(0).equals("message")) {
                    wsdlOp.setOperationOutput(parser.getAttributeValue(0));
                } else if (parser.getLocalName().equals("message") && parser.getAttributeName(0).equals("name")) {
                    message = true;
                    wsdlMessage = new WSDLMessage();
                    wsdlMessage.setMessageName(parser.getAttributeValue(0));
                } else if (message && parser.getLocalName().equals("part") && (parser.getAttributeName(1).equals("element") || parser.getAttributeName(1).equals("type"))) {
                    wsdlMessage.setPartName(parser.getAttributeValue(0));
                    wsdlMessage.setElementName(parser.getAttributeValue(1));
                    messages.add(wsdlMessage);
                }
            }
            if (event == XMLStreamConstants.END_ELEMENT) {
                if (parser.getLocalName().equals("element") || parser.getLocalName().equals("simpleType") || parser.getLocalName().equals("complexType")) {
                    tagCount--;
                }
                if (tagCount == 0) outerElem = null;
                if (parser.getLocalName().equals("portType")) {
                    portType = false;
                    System.out.println("PortType closed....");
                } else if (portType && operation && parser.getLocalName().equals("operation")) {
                    operation = false;
                    operations.put(wsdlOp.getOperationName(), wsdlOp);
                    wsdlOp = null;
                } else if (message && parser.getLocalName().equals("message")) {
                    message = false;
                    wsdlMessage = null;
                }
            }
        }
        clearElementsPrefix(elements);
        printElements(elements);
        printOperations(operations);
        printMessages(messages);
        storeElementMaps(elements);
        constructMessageXML("GetDtcSoapIn");
    }

    public void clearElementsPrefix(ArrayList operations) {
        for (int i = 0; i < operations.size(); i++) {
            Element elem = (Element) operations.get(i);
            if (elem.getType() != null) elem.setType(stripPrefix(elem.getType()));
            if (elem.getRestrictionBase() != null) elem.setRestrictionBase(stripPrefix(elem.getRestrictionBase()));
            operations.set(i, elem);
        }
    }

    public String stripPrefix(String type) {
        String value = null;
        String prefix = null;
        StringTokenizer st = new StringTokenizer(type, ":");
        if (st.countTokens() > 0) {
            if (st.hasMoreTokens()) {
                prefix = st.nextToken();
            }
            if (st.hasMoreTokens()) {
                value = st.nextToken();
                return value;
            }
        }
        return type;
    }

    public StringBuffer createXML(String methodName) {
        StringBuffer xmlString = new StringBuffer();
        boolean methodMatch = false;
        for (int i = 0; i < elements.size(); i++) {
            Element elem = (Element) elements.get(i);
            if (elem.getName().equals(methodName)) {
                xmlString.append("<");
                xmlString.append(methodName);
                xmlString.append(">\n");
                methodMatch = true;
            } else if (methodMatch) {
                xmlString.append("<");
                xmlString.append(elem.getName());
                xmlString.append(">\n");
            }
        }
        return xmlString;
    }

    public void createXML(String methodName, String[] values, String[] tags) {
    }

    public void constructMessageXML(String message) {
        StringBuffer outputXML = null;
        ArrayList xmlList = null;
        String parentElem = null;
        for (int i = 0; i < messages.size(); i++) {
            WSDLMessage wsdlMsg = (WSDLMessage) messages.get(i);
            if (wsdlMsg.getMessageName().equals(message)) {
                outputXML = new StringBuffer();
                xmlList = new ArrayList();
                parentElem = stripPrefix(wsdlMsg.getElementName());
                System.out.println("xml: " + outputXML);
            }
        }
        for (int i = 0; i < elements.size(); i++) if (((Element) elements.get(i)).getName().equals(parentElem)) {
            xmlList.add(elements.get(i));
            break;
        }
        xmlList = parseElements(xmlList);
        System.out.println("Construct XML ......");
        String parentEle = null;
        for (int i = 0; i < xmlList.size(); i++) {
            System.out.println(((Element) xmlList.get(i)).getName());
            Element ele = ((Element) xmlList.get(i));
            if (ele.getName().equals(ele.getParentElement())) {
                outputXML.append("<" + ele.getName() + ">\n");
                parentEle = ele.getName();
            } else if (ele.getParentElement().equals(parentEle) && !isPrimitive(ele.getType())) {
                outputXML.append("<" + ele.getName() + ">\n");
                parentEle = ele.getName();
            } else if (ele.getType() != null && ele.isRestricted() == false && !isPrimitive(ele.getType())) outputXML.append("<" + ele.getName() + ">\n"); else if (ele.getMaxOccurs() != null && ele.getMaxOccurs().equals("unbounded")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "[]" + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("string")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("float")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("double")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("int")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("dateTime")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.getType().equals("base64Binary")) outputXML.append("<" + ele.getName() + ">" + ele.getType() + "</" + ele.getName() + ">\n"); else if (ele.isRestricted()) outputXML.append("<" + ele.getName() + ">" + ele.getRestrictionList() + "</" + ele.getName() + ">\n");
        }
        for (int i = xmlList.size() - 1; i >= 0; i--) {
            Element temp = (Element) xmlList.get(i);
            if (outputXML.indexOf("</" + temp.getName() + ">") > 0) ; else outputXML.append("</" + temp.getName() + ">\n");
        }
        System.out.println();
        System.out.println(outputXML);
        xmlListIndex = -1;
    }

    public boolean isPrimitive(String type) {
        if (type.equals("int")) return true; else if (type.equals("string")) return true; else if (type.equals("float")) return true; else if (type.equals("double")) return true; else if (type.equals("dateTime")) return true; else if (type.equals("boolean")) return true; else if (type.equals("base64Binary")) return true;
        return false;
    }

    int xmlListIndex = -1;

    public ArrayList parseElements(ArrayList xmlList) {
        Element parentElem = (Element) xmlList.get(++xmlListIndex);
        System.out.println("xmlListIndex : " + xmlListIndex + " , " + xmlList.size() + " , " + parentElem.getName());
        boolean added = false;
        for (int i = 0; i < elements.size(); i++) {
            added = false;
            Element elem = (Element) elements.get(i);
            if (elem.getParentElement().equals(parentElem.getName())) {
                if (!added && !elem.getParentElement().equals(elem.getName())) {
                    xmlList.add(elem);
                    System.out.println("????a " + elem.getName() + " , xmlList size:" + xmlList.size());
                    added = true;
                }
            }
            if (!added && parentElem.getType() != null && parentElem.getType().equals(elem.getParentElement())) {
                if (!elem.getParentElement().equals(elem.getName())) {
                    xmlList.add(elem);
                    System.out.println("????b " + elem.getName() + " , xmlList size:" + xmlList.size());
                    added = true;
                }
            }
        }
        if (xmlListIndex >= xmlList.size() - 1) return xmlList; else xmlList = parseElements(xmlList);
        return xmlList;
    }

    public void readElementMaps() {
        ObjectInput input = null;
        try {
            InputStream file = new FileInputStream("quarks.ser");
            InputStream buffer = new BufferedInputStream(file);
            input = new ObjectInputStream(buffer);
            ArrayList recoveredQuarks = (ArrayList) input.readObject();
            Iterator quarksItr = recoveredQuarks.iterator();
            while (quarksItr.hasNext()) {
                System.out.println((String) quarksItr.next());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void storeElementMaps(ArrayList list) {
        ObjectOutput output = null;
        try {
            OutputStream file = new FileOutputStream("quarks.ser");
            OutputStream buffer = new BufferedOutputStream(file);
            output = new ObjectOutputStream(buffer);
            output.writeObject(list);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void printElements(ArrayList list) {
        Element elem = null;
        System.out.println("Name\t" + "Type\t" + "MinOccurs\t" + "MaxOccurs\t" + "Base\t" + "Nillable\t" + "Parent\t" + "SimpleType\t" + "RestrictionBase\t" + "EnumerationValues");
        for (int i = 0; i < list.size(); i++) {
            elem = (Element) list.get(i);
            if (!elem.isRestricted()) System.out.println(elem.getName() + "\t" + elem.getType() + "\t" + elem.getMinOccurs() + "\t" + elem.getMaxOccurs() + "\t" + elem.getBase() + "\t" + elem.getNillable() + "\t" + elem.getParentElement()); else {
                System.out.print(elem.getName() + "\t" + elem.getType() + "\t" + elem.getMinOccurs() + "\t" + elem.getMaxOccurs() + "\t" + elem.getBase() + "\t" + elem.getNillable() + "\t" + elem.getParentElement() + "\t");
                System.out.print(elem.isRestricted() + "\t" + elem.getRestrictionBase() + "\t" + elem.getRestrictionList() + "\n");
            }
        }
    }

    public void printMessages(ArrayList list) {
        WSDLMessage wsdlMsg = null;
        System.out.println("----------Messages--------------");
        for (int i = 0; i < list.size(); i++) {
            wsdlMsg = (WSDLMessage) list.get(i);
            System.out.println(wsdlMsg.getMessageName() + " - " + wsdlMsg.getPartName() + " - " + wsdlMsg.getElementName() + " - " + wsdlMsg.getMethodName());
        }
    }

    public void printOperations(Map map) {
        WSDLOperation wsdlOp = null;
        System.out.println("----------Operations--------------");
        Set operationSet = operations.entrySet();
        for (Iterator i = operationSet.iterator(); i.hasNext(); ) {
            Map.Entry me = (Map.Entry) i.next();
            String ok = (String) me.getKey();
            WSDLOperation ov = (WSDLOperation) me.getValue();
            System.out.print(me + "\t");
            System.out.print(ok + "\t");
            System.out.println(ov.getOperationName() + " - " + ov.getOperationInput() + " - " + ov.getOperationOutput());
        }
    }

    public static void main(String[] args) throws Exception {
        File file = new File("res/bbservice.wsdl");
        if (file.exists()) System.out.println("File found by parser.");
        new Parser().parseFile(file);
    }
}
