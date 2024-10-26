package com.bbn.vessel.core.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.jdom.DataConversionException;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.bbn.vessel.core.logging.Logger;
import com.bbn.vessel.core.logging.LoggerBase;

/**
 * helper class for xml parsing and generation
 *
 * @author jostwald
 *
 */
public class XMLHelper {

    private static final Logger logger = LoggerBase.getLogger(XMLHelper.class);

    private static final Set<String> truth = new TreeSet<String>();

    static {
        String[] trueValues = { "t", "true", "y", "yes", "on", "1" };
        for (String t : trueValues) {
            truth.add(t);
        }
    }

    static final SAXBuilder saxBuilder = new SAXBuilder(false);

    static {
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    /**
     * Utility method for performing getChildren operation and suppress the
     * unchecked warning
     *
     * @param element
     *            the element whose children we are getting
     * @param name
     *            the tag name
     * @return list of children
     */
    @SuppressWarnings("unchecked")
    public static List<Element> getChildren(Element element, String name) {
        return element.getChildren(name);
    }

    /**
     * Utility method for performing getChildren operation and suppress the
     * unchecked warning
     *
     * @param element
     *            the element whose children we are getting
     * @return list of children
     */
    @SuppressWarnings("unchecked")
    public static List<Element> getChildren(Element element) {
        return element.getChildren();
    }

    /**
     * opens a stream from the URL and builds a jdom root element from the
     * content
     *
     * @param urlString
     *            describing the location of xml
     * @param docTypes allowed DocTypes if not empty
     * @return jdom element representing the root of the xml
     */
    public static Element getDocRoot(String urlString, DocType... docTypes) {
        try {
            URL url = new URL(urlString);
            InputStream stream = url.openStream();
            try {
                Element root = getRoot(stream, docTypes);
                return root;
            } finally {
                stream.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * adds a subElement to element containing only text
     *
     * @param element
     *            the parent element
     * @param tag
     *            the tag for the new subelement
     * @param value
     *            the text. If null, the element is not added
     * @return the element that was added or null if the value was null
     */
    public static Element addStringElement(Element element, String tag, String value) {
        if (value != null) {
            Element subElement = new Element(tag);
            subElement.setText(value);
            element.addContent(subElement);
            return subElement;
        }
        return null;
    }

    /**
     * method (mostly for debugging) to get an element as an xml stirng
     *
     * @param elt
     *            the element
     * @return the string
     * @throws IOException
     *             if the outputstream does.
     */
    public static String elementToString(Element elt) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(elt);
        XMLOutputter xmlOutputter = new XMLOutputter(getFormat());
        xmlOutputter.output(doc, baos);
        return baos.toString();
    }

    /**
     * @param elt the element
     * @param attr the attribute name
     * @return the int value of the given attribute
     */
    public static int getIntAttribute(Element elt, String attr) {
        try {
            return elt.getAttribute(attr).getIntValue();
        } catch (DataConversionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param elt the element
     * @param attr the attribute name
     * @return the Integer value of the given attribute or null if the attribute is missing
     */
    public static Integer getIntegerAttribute(Element elt, String attr) {
        try {
            return elt.getAttribute(attr).getIntValue();
        } catch (DataConversionException e) {
            return null;
        }
    }

    /**
     * Gets an attribute as a boolean value
     * @param elt the element
     * @param attr the attribute name
     * @return true if the value is present and any of the listed true words
     */
    public static boolean getBooleanAttribute(Element elt, String attr) {
        String attrValue = elt.getAttributeValue(attr);
        return attrValue != null && truth.contains(attrValue.toLowerCase());
    }

    /**
     * @param <T> the enum type
     * @param elt the element to find the attribute in
     * @param cls the enum class object
     * @param attr the attribute name
     * @return the enum value
     */
    public static <T extends Enum<T>> T getEnumAttribute(Element elt, Class<T> cls, String attr) {
        String s = elt.getAttributeValue(attr);
        if (s == null) {
            return null;
        }
        return T.valueOf(cls, s);
    }

    /**
     * Get the root element from an XML file.
     * @param is FileInputStream through which the file will come.
     * @param docTypes the allowed DocTypes if empty, allow any DocType
     * @return The XML document's root element.
     * @throws JDOMException if parse error.
     * @throws IOException if file IO error.
     */
    public static Element getRoot(InputStream is, DocType... docTypes) throws JDOMException, IOException {
        Document doc = saxBuilder.build(is);
        Element rootElement = detachRootAndCheckDocType(doc, docTypes);
        return rootElement;
    }

    /**
     * Get the root element from an XML file.
     * @param reader FileInputStream through which the file will come.
     * @param docTypes the allowed DocTypes if not empty
     * @return The XML document's root element.
     * @throws JDOMException if parse error.
     * @throws IOException if file IO error.
     */
    public static Element getRoot(Reader reader, DocType... docTypes) throws JDOMException, IOException {
        Document doc = saxBuilder.build(reader);
        Element rootElement = detachRootAndCheckDocType(doc);
        return rootElement;
    }

    /**
     * Test if the Document DocType is acceptable, meaning:
     *   No allowed DocTypes have been specified
     *   There is no DocType in the document
     *   The DocType has no PUBLIC id
     *   The PUBLIC id matches one the allowed doctypes
     *
     * @param doc The document to check
     * @param docTypes allowed types if not empty
     * @return the root element of the Document
     * @throws JDOMException if the DocType is not allowed
     */
    public static Element detachRootAndCheckDocType(Document doc, DocType... docTypes) throws JDOMException {
        if (docTypes.length > 0) {
            DocType documentDocType = doc.getDocType();
            if (documentDocType != null) {
                String systemId = documentDocType.getSystemID();
                if (systemId != null) {
                    boolean ok = false;
                    for (DocType docType : docTypes) {
                        if (systemId.equals(docType.getSystemID())) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        throw new JDOMException("DocType mismatch");
                    }
                }
            }
        }
        return doc.detachRootElement();
    }

    /**
     * @param file the File to read
     * @param docTypes the allowed DocTypes if not empty
     * @return the root element of the doc
     * @throws IOException if the file cannot be read
     * @throws JDOMException if there is a problem with the XML
     */
    public static Element getRoot(File file, DocType... docTypes) throws IOException, JDOMException {
        Reader reader = new FileReader(file);
        Element root = getRoot(reader, docTypes);
        reader.close();
        return root;
    }

    /**
     * Write element as XML.
     * @param element the root element to write
     * @param os where to write
     * @param docType the DocType of the document
     */
    public static void write(Element element, OutputStream os, DocType docType) {
        write(element, new OutputStreamWriter(os), docType);
    }

    /**
     * Write element as XML.
     * @param element the root element to write
     * @param writer where to write
     * @param docType the DocType of the document
     */
    public static void write(Element element, Writer writer, DocType docType) {
        Document doc = new Document(element, docType);
        Format prettyFormat = getFormat();
        XMLOutputter outputter = new XMLOutputter(prettyFormat);
        try {
            outputter.output(doc, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static Format getFormat() {
        Format prettyFormat = Format.getPrettyFormat();
        prettyFormat.setTextMode(Format.TextMode.PRESERVE);
        return prettyFormat;
    }
}
