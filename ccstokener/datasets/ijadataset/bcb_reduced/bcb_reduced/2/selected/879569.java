package net.jwde.util;

import java.io.*;
import java.net.*;
import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.*;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.validation.ValidatorHandler;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.jdom.transform.*;
import org.xml.sax.*;
import org.w3c.tidy.*;

/**
 * XMLHelper is a class designed to provide some generic utility functions for
 * working with HTML, XHTML, XML, and XSL. All methods contained herein are
 * static, so no instantiation of this class is ever necessary. The methods deal
 * with parsing, input/output, retrieving files from the network, and
 * transformation and clean-up of documents.
 * 
 * @author Jared Jackson, Email: <a
 *         mailto="jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see XMLHelperException
 */
public class XMLHelper {

    /**
	 * This method creates a default XML document. The document is empty except
	 * for a single root element, with tag name as specified by the parameter.
	 * 
	 * @param rootName
	 *            The name of the root element of the XML document. If
	 *            <CODE>null</CODE> or empty, no root element is added to the
	 *            document.
	 * @return An empty XML document, save possibly a single root node.
	 */
    public static Document createXml(String rootName) {
        Document doc = new Document();
        if (rootName == null || rootName.trim().equals("")) return doc;
        doc.setRootElement(new Element(rootName));
        return doc;
    }

    /**
	 * Given an <CODE>URL</CODE> as a <CODE>String</CODE>, this method
	 * retrieves the file located at that URL, and attempts to parse it as XML.
	 * 
	 * @param url
	 *            A URL encoding such as "http://www.ibm.com/someXML.xml" of the
	 *            target document
	 * @return A parsed XML document found at the given URL
	 * @exception XMLHelperException
	 *                Thrown if the URL is malformed, the file at the given URL
	 *                can not be obtained, or the file found is not valid XML.
	 */
    public static Document parseXMLFromURLString(String url) throws XMLHelperException {
        return parseXMLFromURL(convertStringToURL(url));
    }

    /**
	 * Given an <CODE>URL</CODE>, this method retrieves the file located at
	 * that URL, and attempts to parse it as XML.
	 * 
	 * @param url
	 *            A <CODE>URL</CODE> java class instantiation of the target
	 *            document
	 * @return A parsed XML document found at the given URL
	 * @exception XMLHelperException
	 *                Thrown if the URL is malformed, the file at the given URL
	 *                can not be obtained, or the file found is not valid XML.
	 */
    public static Document parseXMLFromURL(URL url) throws XMLHelperException {
        try {
            URLConnection inConnection = url.openConnection();
            InputSource is = new InputSource(inConnection.getInputStream());
            return parseXMLFromInputSource(is);
        } catch (IOException ioe) {
            throw new XMLHelperException("Unable to read from source string", ioe);
        }
    }

    /**
	 * Given an XML document currently unparsed in the form of a
	 * <CODE>String</CODE>, this method attempts to parse the content of that
	 * <CODE>String</CODE> as XML.
	 * 
	 * @param source
	 *            A <CODE>String</CODE> encoding of a XML document.
	 * @return A parsed XML document
	 * @exception XMLHelperException
	 *                Thrown if the string given is not valid XML.
	 */
    public static Document parseXMLFromString(String source) throws XMLHelperException {
        InputSource is = new InputSource(new StringReader(source));
        return parseXMLFromInputSource(is);
    }

    /**
	 * Given an XML document pointed to by a <CODE>File</CODE> object, this
	 * method attemps to read the file and parse it as XML.
	 * 
	 * @param sourceFile
	 *            A <CODE>File</CODE> object referencing an XML file.
	 * @return A parsed XML document
	 * @exception XMLHelperException
	 *                Thrown if the file is unreadable or the file does not
	 *                contain a valid XML document
	 */
    public static Document parseXMLFromFile(File sourceFile) throws XMLHelperException {
        Document doc = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            doc = saxBuilder.build(sourceFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
	 * Given an XML document pointed to by a file path expression, this method
	 * attemps to read the file and parse it as XML.
	 * 
	 * @param sourceFile
	 *            An absolute or relative file path expression.
	 * @return A parsed XML document
	 * @exception XMLHelperException
	 *                Thrown if the file is unreadable or the file does not
	 *                contain a valid XML document
	 */
    public static Document parseXMLFromFile(String sourceFile) throws XMLHelperException {
        Document doc = null;
        File f1 = new File(sourceFile);
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            doc = saxBuilder.build(f1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    private static Document parseXMLFromInputSource(InputSource is) throws XMLHelperException {
        Document doc = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            doc = saxBuilder.build(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
	 * Given two XML documents, one the target XML file and one an XSL file,
	 * this method applies an XSL transform defined by the XSL file on the XML
	 * file and returns the resulting document.
	 * 
	 * @param xmlDoc
	 *            The source XML file
	 * @param xslDoc
	 *            An XML file that also follows the XSL transformation language
	 *            specification
	 * @return The document resulting from applying xslDoc to xmlDoc.
	 * @exception XMLHelperException
	 *                Thrown if the XSL document is either poorly formed as XSL
	 *                or if it encounters an error during transformation.
	 */
    public static Document transformXML(Document xmlDoc, Document xslDoc) throws XMLHelperException {
        String result = null;
        Document resultDoc = null;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new JDOMSource(xslDoc));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(new JDOMSource(xmlDoc), new StreamResult(baos));
            baos.close();
            result = baos.toString();
            resultDoc = parseXMLFromString(result);
            return resultDoc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultDoc;
    }

    /**
	 * Implement this using document instead of string
	 * 
	 * @param xmlDoc
	 * @param xslDoc
	 * @return
	 * @throws XMLHelperException
	 */
    public static Document transformXML(String xmlDoc, String xslDoc) {
        String result = null;
        try {
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslDoc));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(xmlDoc), new StreamResult(baos));
            baos.close();
            result = baos.toString();
            return parseXMLFromString(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Given an XML document, a pretty (tab delimited and with line breaks)
	 * representation is sent to the specified <CODE>PrintStream</CODE>
	 * object. This is the most convenient way to output an XML document to
	 * standard out.
	 * 
	 * @param doc
	 *            The XML document to output
	 * @param stream
	 *            The stream to send the result to. (e.g.
	 *            <CODE>System.out</CODE> or <CODE>System.err</CODE>)
	 * @exception XMLHelperException
	 *                Thrown in the event of an I/O error.
	 */
    public static void outputXML(Document doc, PrintStream stream) throws XMLHelperException {
        try {
            Format format = Format.getPrettyFormat();
            format.setTextMode(Format.TextMode.NORMALIZE);
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            xmlOutputter.output(doc, stream);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new XMLHelperException("Unable to write to the given print stream", ioe);
        }
    }

    /**
	 * Given an XML document and a relative or absolute path name for a file,
	 * writes the XML document to that file location. The format of the written
	 * XML document will be tab delimited and line breaked. The file name will
	 * need to use the system dependent separator character(s) for directory
	 * navigation.
	 * 
	 * @param doc
	 *            The XML document to output.
	 * @param fileName
	 *            A file name either relative to the running Java virtual
	 *            machine, or absolute.
	 * @exception XMLHelperException
	 *                Thrown if an I/O error occurs.
	 */
    public static void outputXMLToFile(Document doc, String fileName) throws XMLHelperException {
        try {
            File f1 = new File(fileName);
            FileWriter fout = new FileWriter(f1);
            Format format = Format.getPrettyFormat();
            format.setTextMode(Format.TextMode.NORMALIZE);
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            xmlOutputter.output(doc, fout);
            fout.close();
        } catch (IOException ioe) {
            throw new XMLHelperException("Unable to write to the given file", ioe);
        }
    }

    public static void outputXMLToURL(Document doc, URL url) throws XMLHelperException {
        try {
            File f1 = new File(url.getFile());
            FileWriter fout = new FileWriter(f1);
            Format format = Format.getPrettyFormat();
            format.setTextMode(Format.TextMode.NORMALIZE);
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            xmlOutputter.output(doc, fout);
            fout.close();
        } catch (IOException ioe) {
            throw new XMLHelperException("Unable to write to the given file", ioe);
        }
    }

    /**
	 * A utility method for converting an XML document to a <CODE>String</CODE>
	 * object. This method is included in case the user would like to do their
	 * own I/O in a way not specified in this class.
	 * 
	 * @param doc
	 *            The XML document to be encoded as a <CODE>String</CODE>.
	 * @return The XML document as text in a <CODE>String</CODE>.
	 */
    public static String convertXMLToString(Document doc) throws XMLHelperException {
        try {
            Format format = Format.getPrettyFormat();
            format.setTextMode(Format.TextMode.NORMALIZE);
            XMLOutputter xmlOutputter = new XMLOutputter(format);
            return xmlOutputter.outputString(doc);
        } catch (Exception ioe) {
            ioe.printStackTrace();
            throw new XMLHelperException("Unable to write to the string", ioe);
        }
    }

    public static Document tidyHTML(File file) throws XMLHelperException {
        return tidyHTML(convertFileToURL(file));
    }

    public static URL convertFileToURL(File f1) {
        try {
            return f1.toURL();
        } catch (MalformedURLException me) {
            me.printStackTrace();
        }
        return null;
    }

    /**
	 * Retrieves an HTML page from a URL encoded as a <CODE>String</CODE> and
	 * attempts to clean up the source of that HTML to remove author errors. If
	 * successful, the resulting document is converted to XHTML and returned as
	 * an XML document.
	 * 
	 * @param url
	 *            A <CODE>String</CODE> encoding of a URL (e.g.
	 *            "http://www.ibm.com/index.html").
	 * @return an XML document representing the XHTML of the source of the HTML
	 *         file.
	 * @exception XMLHelperException
	 *                Thrown if the URL is malformed, the HTML source can not be
	 *                obtained, or the tool is unable to convert the source to
	 *                XML.
	 */
    public static Document tidyHTML(String url) throws XMLHelperException {
        return tidyHTML(convertStringToURL(url));
    }

    /**
	 * Retrieves an HTML page from a java <CODE>URL</CODE> object and attempts
	 * to clean up the source of that HTML to remove author errors. If
	 * successful, the resulting document is converted to XHTML and returned as
	 * an XML document.
	 * 
	 * @param url
	 *            A <CODE>URL</CODE> object hopefully pointing to an HTML
	 *            file.
	 * @return an XML document representing the XHTML of the source of the HTML
	 *         file.
	 * @exception XMLHelperException
	 *                Thrown if the HTML source can not be obtained or the tool
	 *                is unable to convert the source to XML.
	 */
    public static Document tidyHTML(URL url) throws XMLHelperException {
        try {
            URLConnection inConnection = url.openConnection();
            if (inConnection.getContentType().startsWith("text/xml") || inConnection.getContentType().startsWith("text/xhtml")) {
                return parseXMLFromURL(url);
            } else if (inConnection.getContentType().startsWith("text/html")) {
                InputStream is = inConnection.getInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int totalBytes = 0;
                byte[] buffer = new byte[65536];
                while (true) {
                    int bytesRead = is.read(buffer, 0, buffer.length);
                    if (bytesRead < 0) break;
                    for (int i = 0; i < bytesRead; i++) {
                        byte b = buffer[i];
                        if (b < 32 && b != 10 && b != 13 && b != 9) b = 32;
                        buffer[i] = b;
                    }
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                is.close();
                out.close();
                String outContent = out.toString();
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                Tidy tidy = new Tidy();
                tidy.setShowWarnings(false);
                tidy.setXmlOut(true);
                tidy.setXmlPi(false);
                tidy.setDocType("omit");
                tidy.setXHTML(false);
                tidy.setRawOut(true);
                tidy.setNumEntities(true);
                tidy.setQuiet(true);
                tidy.setFixComments(true);
                tidy.setIndentContent(true);
                tidy.setCharEncoding(org.w3c.tidy.Configuration.ASCII);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                org.w3c.dom.Document tNode = (org.w3c.dom.Document) tidy.parseDOM(in, baos);
                String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + baos.toString();
                int startIndex = 0;
                int endIndex = 0;
                if ((startIndex = result.indexOf("<!DOCTYPE")) >= 0) {
                    endIndex = result.indexOf(">", startIndex);
                    result = result.substring(0, startIndex) + result.substring(endIndex + 1, result.length());
                }
                while ((startIndex = result.indexOf("<script")) >= 0) {
                    endIndex = result.indexOf("</script>");
                    result = result.substring(0, startIndex) + result.substring(endIndex + 9, result.length());
                }
                in.close();
                baos.close();
                return parseXMLFromString(result);
            } else {
                throw new XMLHelperException("Unable to tidy content type: " + inConnection.getContentType());
            }
        } catch (IOException ioe) {
            throw new XMLHelperException("Unable to perform input/output", ioe);
        }
    }

    private static URL convertStringToURL(String url) throws XMLHelperException {
        try {
            return new URL(url);
        } catch (MalformedURLException murle) {
            throw new XMLHelperException(url + " is not a well formed URL", murle);
        }
    }

    public static org.w3c.dom.Document JDOM2DOM(Document doc) throws JDOMException {
        org.w3c.dom.Document dom = null;
        DOMOutputter outputter = new DOMOutputter();
        try {
            if (doc != null) dom = outputter.output(doc); else return dom;
        } catch (JDOMException jdomEx) {
            throw jdomEx;
        }
        return dom;
    }

    /**
	 * This method checks a XML file and validates it against XML schema.
	 * This uses SAX to reduce memory footprint and for fast validation.
	 * It uses the JAXP API for compatibility with other schema processors.
	 * @param xmlURL
	 * @return boolean true if it validates correct
	 */
    public static void validate(URL xmlURL) throws SAXException, IOException {
        try {
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema();
            Validator validator = schema.newValidator();
            URLConnection inConnection = xmlURL.openConnection();
            InputStream is = inConnection.getInputStream();
            validator.validate(new SAXSource(new InputSource(is)));
        } catch (SAXException saxEx) {
            throw saxEx;
        } catch (IOException ioEx) {
            throw ioEx;
        }
    }
}
