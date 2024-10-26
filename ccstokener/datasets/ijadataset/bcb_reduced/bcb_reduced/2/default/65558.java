import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Markus Plessing
 */
public class XMLProcessor {

    private static StringBuffer XmlIn;

    private static StringBuffer XmlOut;

    private static String server = "https://www.ups.com/ups.app/xml/";

    private static Hashtable errorCodes = new Hashtable();

    private static Hashtable response = new Hashtable();

    private static String internTrack = "";

    private static boolean saveImage = false, saveTrack = false;

    /**
     * invisible Constructor
     * all methods are static
     */
    private XMLProcessor() {
    }

    protected static String validate(String text) {
        text = text.replaceAll(">", "&lt;");
        text = text.replaceAll("<", "&gt;");
        text = text.replaceAll("&", "&amp;");
        text = text.replaceAll("\"", "&quot;");
        text = text.replaceAll("'", "&apos;");
        return text;
    }

    /**
     * isServiceAvailable(): check if requested Service is online
     * @param service String: the Service-Name
     * @return boolean Status of the service (available)?true:false
     */
    protected static final boolean isServiceAvailable(String service) {
        try {
            URL url = new URL(server + service);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String header = connection.getHeaderField(null);
            connection.disconnect();
            String[] fields = header.split(" ");
            if (fields.length > 1) {
                if (fields[1].equals("200")) {
                    return true;
                }
                return false;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * readResponse(): get the Hashtable with the label informations 
     * @return Hashtable : Hash with information to the labels 
     */
    protected static final Hashtable readResponse() {
        return response;
    }

    /**
     * 
     *
     */
    public static final void resetValues() {
        saveImage = false;
        saveTrack = false;
        internTrack = "";
    }

    /**
     * 
     *
     */
    public static final void clearResponse() {
        response.clear();
    }

    /**
     * 
     *
     */
    public static final void clearBuffers() {
        if (XmlIn != null) XmlIn.delete(0, XmlIn.length());
        if (XmlOut != null) XmlOut.delete(0, XmlIn.length());
    }

    /**
     * readXMLDocument(): read a File into a DOM-Tree
     * @param file File: the file for reading
     * @return Document: the DOM-document
     * @throws Exception : Errors reading the file
     */
    protected static final Document readXMLDocument(File file) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory dfactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
        org.w3c.dom.Document _doc = docBuilder.parse(file);
        return _doc;
    }

    /**
     * createXMLDocument(): create a new XMLDocument
     * @param rootName String: Name of the root - Element
     * @param attrName String: the Attributes-Name to append to the rootElement 
     * @param attrValue String: the Attributes-Value to append to the rootElement
     * @return Document: the finished DOM-Document
     * @throws Exception: Parsing Errors
     */
    protected static final Document createXMLDocument(String rootName, String attrName, String attrValue) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory dfactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);
        javax.xml.parsers.DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
        org.w3c.dom.Document _doc = docBuilder.newDocument();
        Element root = _doc.createElement(rootName);
        if (attrName != null && attrValue != null) {
            root.setAttribute(attrName, attrValue);
        }
        _doc.appendChild(root);
        return _doc;
    }

    /**
     * retrieveGraphics(): read the graphics out of the DOM-Document
     * @param node the Node to check
     */
    protected static void retrieveGraphics(Node node) {
        int type = node.getNodeType();
        switch(type) {
            case Node.ELEMENT_NODE:
                {
                    if (node.getNodeName().equals("TrackingNumber")) {
                        saveTrack = true;
                    }
                    if (node.getNodeName().equals("GraphicImage")) {
                        saveImage = true;
                    }
                    break;
                }
            case Node.TEXT_NODE:
                {
                    if (saveTrack) {
                        internTrack = node.getNodeValue();
                        saveTrack = false;
                    }
                    if (saveImage) {
                        response.put(internTrack, getLabel(node.getNodeValue().getBytes(), internTrack));
                        internTrack = "";
                        saveImage = false;
                    }
                    break;
                }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            retrieveGraphics(child);
        }
    }

    /**
     * Dom2Buf(): convert a DOM-Tree to a StringBuffer
     * @param node Node: the starting Node
     * @param buff StringBuffer: the Buffer to read the document into
     * @return StringBuffer: the filled StringBuffer
     */
    protected static StringBuffer Dom2Buf(Node node, StringBuffer buff) {
        int type = node.getNodeType();
        switch(type) {
            case Node.DOCUMENT_NODE:
                {
                    buff.append("<?xml version=\"1.0\" encoding=\"" + "UTF-8" + "\"?>\n");
                    break;
                }
            case Node.ELEMENT_NODE:
                {
                    buff.append('<' + node.getNodeName());
                    NamedNodeMap nnm = node.getAttributes();
                    if (nnm != null) {
                        int len = nnm.getLength();
                        Attr attr;
                        for (int i = 0; i < len; i++) {
                            attr = (Attr) nnm.item(i);
                            buff.append(' ' + attr.getNodeName() + "=\"" + attr.getNodeValue() + '"');
                        }
                    }
                    buff.append('>');
                    break;
                }
            case Node.ENTITY_REFERENCE_NODE:
                {
                    buff.append('&' + node.getNodeName() + ';');
                    break;
                }
            case Node.CDATA_SECTION_NODE:
                {
                    buff.append("<![CDATA[" + node.getNodeValue() + "]]>");
                    break;
                }
            case Node.TEXT_NODE:
                {
                    buff.append(node.getNodeValue());
                    break;
                }
            case Node.PROCESSING_INSTRUCTION_NODE:
                {
                    buff.append("<?" + node.getNodeName());
                    String data = node.getNodeValue();
                    if (data != null && data.length() > 0) {
                        buff.append(' ');
                        buff.append(data);
                    }
                    buff.append("?>");
                    break;
                }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            buff = Dom2Buf(child, buff);
        }
        if (type == Node.ELEMENT_NODE) {
            buff.append("</" + node.getNodeName() + ">");
        }
        return buff;
    }

    /**
	 * Reads an input file to a StringBuffer
	 * @return java.lang.StringBuffer
	 * @param file java.lang.String
	 * @throws Exception
	 */
    protected static final StringBuffer readInputFile(String file) throws Exception {
        StringBuffer xml = new StringBuffer();
        InputStreamReader in = new java.io.InputStreamReader(new FileInputStream(file), "UTF-8");
        LineNumberReader lineReader = new LineNumberReader(in);
        String line_xml = null;
        while ((line_xml = lineReader.readLine()) != null) {
            xml.append(line_xml);
        }
        lineReader.close();
        return xml;
    }

    /**
     * writeOutputFile(): write a StringBuffer to a file
     * @param str StringBuffer: the StringBuffer to write to the File
     * @param file String: Stringrepresentation of the filePath
     * @throws Exception: Errors writing to the file
     */
    protected static final void writeOutputFile(StringBuffer str, String file) throws Exception {
        FileOutputStream stream = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
        writer.write(str.toString());
        writer.close();
        stream.close();
    }

    /**
     * getXMLOut(): get the Output-StringBuffer
     * @return StringBuffer the Output-StringBuffer
     */
    protected static final StringBuffer getXMLOut() {
        return XmlOut;
    }

    /**
     * getXMLIn(): get the Input-StringBuffer
     * @return StringBuffer the Input-StringBuffer
     */
    protected static final StringBuffer getXMLIn() {
        return XmlIn;
    }

    /**
     * setXMLOut(): set the Output-StringBuffer
     * @param out StringBuffer the Output-Stringbuffer
     */
    protected static final void setXMLOut(StringBuffer out) {
        XmlOut = out;
    }

    /**
     * setXMLIn(): set the Input-Stringbuffer
     * @param in StringBuffer the Input-StringBuffer
     */
    protected static final void setXMLIn(StringBuffer in) {
        XmlIn = in;
    }

    /**
     * This method is used to send the XmlTransmitter information to a designated service.  
     * @param service java.lang.String: The Service-Name
     * @throws java.lang.Exception: Connection-Errors
     */
    protected static final void contactService(String service) throws Exception {
        HttpURLConnection connection;
        URL url;
        try {
            java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            System.getProperties().put("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
            url = new URL(server + service);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            String queryString = getXMLIn().toString();
            OutputStream out = connection.getOutputStream();
            out.write(queryString.getBytes());
            out.close();
            String data = "";
            try {
                data = readURLConnection(connection);
            } catch (Exception e) {
                System.out.println("Error in reading URL Connection" + e.getMessage());
                throw e;
            }
            setXMLOut(new StringBuffer(data));
        } catch (Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
    }

    /**
     * This method read all of the data from a URL conection to a String
     * @param uc URLConnection the connection to read
     * @throws Exception: Connection/Reading - Errors
     * @return String the String read from the connection
     */
    private static String readURLConnection(URLConnection uc) throws Exception {
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            int letter = 0;
            while ((letter = reader.read()) != -1) buffer.append((char) letter);
        } catch (Exception e) {
            System.out.println("Cannot read from URL" + e.toString());
            throw e;
        } finally {
            try {
                reader.close();
            } catch (java.io.IOException io) {
                System.out.println("Error closing URLReader!");
                throw io;
            }
        }
        return buffer.toString();
    }

    /**
    * getLabel(): Decode the BASE64-Encoded Labels 
    * @param base64image String: encoded Label represented by this String
    * @param transactionNumber String: Number of the Transaction
    * @return String the path to the img-Label
    */
    public static String getLabel(byte[] base64image, String transactionNumber) {
        String imgPath = System.getProperty("java.io.tmpdir") + "/label" + transactionNumber + ".gif";
        try {
            byte[] buf = Base64.base64Decode(base64image);
            FileOutputStream fos = new FileOutputStream(new File(imgPath));
            fos.write(buf);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
        return imgPath;
    }

    /**
     * getHTML(): Decode the BASE64-Encoded HTML-File for displaying the Labels
     * @param base64html String: encoded HTML represented by this String
     * @param transactionNumber String: Number of the Transaction
     * @return String the path to the img-Label
     */
    public static String getHTML(String base64html, String transactionNumber) {
        String imgPath = System.getProperty("java.io.tmpdir") + "/view" + transactionNumber + ".html";
        try {
            byte[] buf = Base64.base64Decode(base64html.getBytes());
            FileOutputStream fos = new FileOutputStream(new File(imgPath));
            fos.write(buf);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            new ErrorHandler(ex);
        }
        return imgPath;
    }

    /**
     * addNode(): append a Node to the given Document
     * @param _doc Document: the document to append the Node
     * @param strParentTag String: the Name of the Parent-Tag
     * @param newTagName String: the Name of the new Tag
     * @param textString String: the Value of the new Tag
     * @param attrName String: AttributeName of the new Tag
     * @param attrValue String: AttributeValue of the new Tag
     */
    public static void addNode(Document _doc, String strParentTag, String newTagName, String textString, String attrName, String attrValue) {
        Node parentTag = _doc.getDocumentElement();
        NodeList nl = _doc.getDocumentElement().getElementsByTagName(strParentTag);
        parentTag = nl.item(nl.getLength() - 1);
        if (newTagName != null) {
            Element item = _doc.createElement(newTagName);
            try {
                if (textString != null) {
                    if (!textString.startsWith("#RM") && !textString.startsWith("#rm")) {
                        item.appendChild(_doc.createTextNode(validate(textString)));
                        parentTag.appendChild(item);
                    }
                } else {
                    if (attrName != null && attrValue != null) {
                        item.setAttribute(attrName, attrValue);
                    }
                    parentTag.appendChild(item);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                new ErrorHandler(ex);
            }
        } else {
            if (textString != null) {
                if (!textString.startsWith("#RM") && !textString.startsWith("#rm")) {
                    parentTag.appendChild(_doc.createTextNode(validate(textString)));
                }
            }
        }
    }

    /**
	 * This method builds an part of the DOM tree and attaches it to the document using recursion
	 * @param _doc Document: the document to append the Subtree 
	 * @param strParentTag String: Name of the parent Tag
	 * @param nodeName String: name of the node
	 * @param data Vector: the vectorized Tree to append
	 */
    public static void buildNode(Document _doc, String strParentTag, String nodeName, Vector data) {
        addNode(_doc, strParentTag, nodeName, null, null, null);
        Enumeration e = data.elements();
        while (e.hasMoreElements()) {
            Vector v = (Vector) e.nextElement();
            if (v.elementAt(1) instanceof Vector) buildNode(_doc, nodeName, v.elementAt(0).toString(), (Vector) v.elementAt(1)); else addNode(_doc, nodeName, v.elementAt(0).toString(), v.elementAt(1).toString(), null, null);
        }
    }

    /**
	 * setErrorCodes(): fill the passed codes to the Error-Hash
	 * @param code String: ErrorCode
	 * @param desc String: ErrorDescription
	 */
    protected static void setErrorCodes(String code, String desc) {
        errorCodes.put(code, desc);
    }

    /**
	 * getErrorCodes(): get the Hashtable with the Errors
	 * @return Hashtable: the Errors-Hash 
	 */
    protected static Hashtable getErrorCodes() {
        Hashtable retCodes = new Hashtable();
        retCodes.putAll(errorCodes);
        return retCodes;
    }

    protected static void clearErrors() {
        errorCodes.clear();
    }

    /**
	 * Method to get a node that is a child of parent
	 * @return Node the node found
	 * @param parent of the node you are looking for
	 * @param nodeName the name of the node you are looking for
	 */
    protected static Node getChildNode(Node parent, String nodeName) {
        if (parent == null) return null;
        org.w3c.dom.Node child = null;
        org.w3c.dom.Node found = null;
        for (child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeName().equals(nodeName)) {
                found = child;
                break;
            }
        }
        return found;
    }

    /**
	 * Method to get a value of a node based on the node's name and its parent
	 * @return String the value of the found node
	 * @param node the parent of the node you are looking for
	 * @param fieldName the name of the node you are looking for
	 */
    protected static String getChildNodeValue(Node node, String fieldName) {
        if (node == null) return "";
        String value = "";
        Node found = null;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeName().equals(fieldName)) {
                found = child;
                value = getNodeValue(child);
                break;
            }
        }
        if (found == null) return "";
        return value.trim();
    }

    /**
	 * Method to get the value (text) of a node
	 * @return String the value
	 * @param node the node to get the value from
	 */
    protected static String getNodeValue(Node node) {
        String output = new String("");
        NodeList nl = null;
        if (node != null) nl = node.getChildNodes();
        if ((nl != null) && (nl.getLength() > 0)) {
            Node value = nl.item(0);
            if (value != null) output = value.getNodeValue();
        }
        output = output.trim();
        return output;
    }

    /**
	 * Convert the given LinkedHashMap to an Vector
	 * @param elements Sorted hashMap containing the elements for this dimension of the vector
	 * @return Vector the created Vector
	 */
    public static Vector createElementVector(LinkedHashMap elements) {
        Vector[] element = new Vector[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            element[i] = new Vector();
            Object[] keys = elements.keySet().toArray();
            Object[] values = elements.values().toArray();
            element[i].add(keys[i]);
            element[i].add(values[i]);
        }
        Vector retVec = new Vector();
        int a = 0;
        while (a < elements.size()) {
            retVec.add(element[a++]);
        }
        return retVec;
    }

    /**
     * only for testing purposes
     * @param args
     */
    public static void main(String[] args) {
    }
}
