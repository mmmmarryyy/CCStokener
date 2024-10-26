package org.jbrix.xml;

import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.*;

/**
 *   This abstract class provides the foundation around which the
 *   Xybrix XML utilities operate. To obtain an instance of this
 *   class, you should typically call XMLDriver.getDefaultDriver().
 *   From this you can parse files in a way that takes full advantage
 *   of the utilities and features provided by this package. Currently
 *   the only concrete implementation is for the Xerces parser, but
 *   it should be possible to make a driver for any other w3c DOM-based
 *   parser.
 */
public abstract class XMLDriver {

    public static final String NS_URI_XML_UTILITY = "http://www.jbrix.org/XMLUtilitySchema";

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>";

    private static XMLDriver defaultDriver = null;

    private static Hashtable editedDocuments = new Hashtable();

    private static Hashtable driverStacksByThread = new Hashtable();

    private static boolean parsing = false;

    private Class defaultElementClass = null;

    /**
	 *   This method returns the default XMLDriver instance that is
	 *   shared throughout an application. This is typically how an
	 *   XMLDriver instance is acquired, as opposed to manually
	 *   instantiating a concrete subclass.
	 *   
	 *   @return the default XMLDriver object.
	 */
    public static XMLDriver getDefaultDriver() {
        if (defaultDriver == null) {
            defaultDriver = new XercesXMLDriver();
        }
        return defaultDriver;
    }

    public void setDefaultElementClass(Class cls) {
        defaultElementClass = cls;
    }

    public Class getDefaultElementClass() {
        return defaultElementClass;
    }

    public static XMLDriver getNewDriver() {
        return new XercesXMLDriver();
    }

    /**
	 *   This method indicates whether or not document has been edited in
	 *   any way since it was initially parsed, or since the document
	 *   edited flag was last called through the setDocumentEdited
	 *   method.
	 *   
	 *   @see #setDocumentEdited
	 *   @param document the document to be tested for edits.
	 *   @return true if the document has been edited since it was first
	 *   parsed or since the last call to setDocumentEdited clearing any
	 *   edits.
	 */
    public static boolean isDocumentEdited(Document document) {
        return editedDocuments.containsKey(document);
    }

    /**
	 *   If flag is true, this method marks the document as having been
	 *   edited; if flag is false, it clears the document from being
	 *   marked as edited.
	 *   
	 *   @see #isDocumentEdited
	 *   @param document the document for which edits will be marked or
	 *   cleared.
	 *   @param flag true to mark document as edited, false to clear any
	 *   edits.
	 */
    public static void setDocumentEdited(Document document, boolean flag) {
        if (document == null) return;
        if (flag) {
            editedDocuments.put(document, document);
        } else {
            editedDocuments.remove(document);
            if (document instanceof XMLDocument) {
                ((XMLDocument) document).clearModificationStacks();
            }
        }
    }

    /**
	 *   This method attempts to parse the file fileName, and return the
	 *   resulting DOM Document object.
	 *   
	 *   @see #parse
	 *   @param fileName the absolute path of the XML file to
	 *   parse.
	 *   @exception IOException if there is an IO problem reading the file.
	 *   @exception SAXException if there is a problem parsing the file.
	 *   @return a DOM Document object of the XML file.
	 */
    public Document parseFile(String fileName) throws IOException, SAXException {
        return parseFile(new File(fileName));
    }

    public Document parseResource(String resourceName) throws IOException, SAXException {
        InputStream in = XMLDriver.class.getResourceAsStream(resourceName);
        return parse(in);
    }

    /**
	 *   This method attempts to parse the file fileName, and return the
	 *   resulting DOM Document object.
	 *   
	 *   @see #parse
	 *   @param file the XML file to
	 *   parse.
	 *   @exception IOException if there is an IO problem reading the file.
	 *   @exception SAXException if there is a problem parsing the file.
	 *   @return a DOM Document object of the XML file.
	 */
    public Document parseFile(File file) throws IOException, SAXException {
        try {
            parsingFile = file.getAbsolutePath();
            FileInputStream inputStream = new FileInputStream(file);
            Document doc = parse(inputStream);
            return doc;
        } finally {
            parsingFile = null;
        }
    }

    String parsingFile = null;

    /**
	 *   This method parses the given xml string and returns is
	 *   representation as a DOM Document object. There should be exactly
	 *   one root element in the xml fragment, which will be the document
	 *   element object of the returned document. The xml may, but does
	 *   not need to include a standard XML header.
	 *   
	 *   @param xml the XML fragment to be parsed.
	 *   @exception SAXException if there is a problem parsing the fragment.
	 *   @return a DOM Document object representing the XML fragment.
	 */
    public Document parseXML(String xml) throws IOException, SAXException {
        if (!xml.toLowerCase().trim().startsWith("<?xml")) {
            xml = XML_HEADER + xml;
        }
        return parse(new StringReader(xml));
    }

    /**
	 *   Parses the XML located at url, and returns the resulting DOM
	 *   Document object.
	 *   
	 *   @param url the location of the XML to parse.
	 *   @exception IOException if there is an IO problem reading the url.
	 *   @exception SAXException if there is a problem parsing the document.
	 *   @return a DOM Document object.
	 */
    public Document parse(URL url) throws IOException, SAXException {
        return parse(url.openStream());
    }

    public Document parse(StringReader reader) throws IOException, SAXException {
        return parse(new InputSource(reader));
    }

    public Document parse(InputStream inputStream) throws IOException, SAXException {
        return parse(new InputSource(inputStream));
    }

    static XMLDriver getCurrentXMLDriver() {
        synchronized (driverStacksByThread) {
            Vector stack = (Vector) (driverStacksByThread.get(Thread.currentThread()));
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            return (XMLDriver) (stack.lastElement());
        }
    }

    public Document parse(InputSource inputSource) throws IOException, SAXException {
        parsing = true;
        Vector stack;
        synchronized (driverStacksByThread) {
            stack = (Vector) (driverStacksByThread.get(Thread.currentThread()));
            if (stack == null) {
                stack = new Vector();
                driverStacksByThread.put(Thread.currentThread(), stack);
            }
            stack.addElement(this);
        }
        Document document = parseInput(inputSource);
        synchronized (driverStacksByThread) {
            if (stack.lastElement() == this) {
                stack.removeElementAt(stack.size() - 1);
            } else {
                System.err.println("WARNING: Unexpected XMLDriver found on stack...");
            }
            if (stack.isEmpty()) {
                driverStacksByThread.remove(Thread.currentThread());
            }
        }
        parsing = false;
        if (document != null) XMLDriver.setDocumentEdited(document, false);
        return document;
    }

    /**
	 *   Indicates whether or not the driver is currently parsing a
	 *   document.
	 *   
	 *   @return true if the driver is currently parsing a document, false
	 *   otherwise.
	 */
    static boolean isParsing() {
        return parsing;
    }

    public static void addMapping(String tagName, String className) {
        XMLDocument.addMapping(tagName, className);
    }

    public static void addMapping(String nsURI, String tagName, String className) {
        XMLDocument.addMapping(nsURI, tagName, className);
    }

    protected abstract Document parseInput(InputSource inputSource) throws IOException, SAXException;

    public abstract String toXmlString(Document d);

    public abstract Document getNewDocument();

    public abstract DocumentFragment parseFragment(Document parentDocument, String fragment);

    /**
	 *   This is a parser-independent method for creating a new DOM
	 *   Document object. The document contains an empty document element
	 *   with docTag as its tag.
	 *   
	 *   @param docTag the desired tag name for the document element of
	 *   the document to be created.
	 *   @return a DOM Document object.
	 */
    public Document getNewDocument(String docTag) {
        Document doc = getNewDocument();
        Element element = doc.createElement(docTag);
        doc.appendChild(element);
        return doc;
    }

    private static String makeParserSafe(String text) {
        text = ExpressionUtilities.substitute(text, "&", "&amp;");
        text = ExpressionUtilities.substitute(text, "\"", "&quot;");
        text = ExpressionUtilities.substitute(text, "<", "&lt;");
        text = ExpressionUtilities.substitute(text, ">", "&gt;");
        text = ExpressionUtilities.substitute(text, "\'", "&apos;");
        return text;
    }

    public String toPrettyXmlString(Document doc) {
        StringBuffer buffer = new StringBuffer(200);
        toPrettyXmlString(doc.getDocumentElement(), 0, buffer);
        return buffer.toString();
    }

    private void toPrettyXmlString(Node elmnt, int level, StringBuffer buffer) {
        String piece;
        int i;
        for (i = 0; i < level; i++) buffer.append("    ");
        buffer.append("<");
        String prefix;
        if (elmnt instanceof XMLElement) {
            prefix = ((XMLElement) elmnt).getNamespacePrefix();
            if (prefix != null && ((XMLElement) elmnt).explicitPrefix) {
                buffer.append(prefix + ":");
            }
        }
        buffer.append(elmnt.getNodeName());
        NamedNodeMap atts = elmnt.getAttributes();
        Node node;
        for (i = 0; i < atts.getLength(); i++) {
            node = atts.item(i);
            piece = " " + node.getNodeName() + "=\"" + makeParserSafe(node.getNodeValue()) + "\"";
            if ((buffer.length() - (buffer.toString().lastIndexOf('\n') + 1)) + piece.length() > 78) {
                buffer.append("\n");
                for (int j = 0; j < (level + 1); j++) buffer.append("    ");
            }
            buffer.append(piece);
        }
        NodeList kids = elmnt.getChildNodes();
        int n = kids.getLength();
        Node kid;
        boolean needIndent = false;
        buffer.append(n < 1 ? "/>" : ">");
        if (!(elmnt.getFirstChild() instanceof Text)) {
        }
        for (i = 0; i < n; i++) {
            kid = kids.item(i);
            if (kid instanceof Element) {
                needIndent = true;
                buffer.append("\n");
                toPrettyXmlString((Element) kid, level + 1, buffer);
            } else if (kid instanceof Text) {
                buffer.append(makeParserSafe(kid.getNodeValue()).trim());
            } else if (kid instanceof Comment) {
                if (i > 0 && kids.item(i - 1) instanceof Text) {
                    if (kids.item(i - 1).getNodeValue().trim().length() < 1) {
                        buffer.append(kids.item(i - 1).getNodeValue());
                    }
                }
                buffer.append("<!--" + kid.getNodeValue() + "-->");
            } else {
                System.out.println(kid.getClass().getName());
            }
        }
        if (n > 0) {
            if (n > 1 || (n == 1 && !(elmnt.getFirstChild() instanceof Text))) buffer.append("\n");
            if (needIndent) for (i = 0; i < level; i++) buffer.append("    ");
            buffer.append("</");
            if (elmnt instanceof XMLElement) {
                prefix = ((XMLElement) elmnt).getNamespacePrefix();
                if (prefix != null && ((XMLElement) elmnt).explicitPrefix) {
                    buffer.append(prefix + ":");
                }
            }
            buffer.append(elmnt.getNodeName() + ">");
        }
    }
}
