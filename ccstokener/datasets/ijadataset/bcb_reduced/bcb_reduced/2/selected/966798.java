package apollo.dataadapter;

import java.net.URL;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.bdgp.xml.XMLElement;
import org.bdgp.xml.XML_util;

/**
 * WARNING -- AElfred (and other SAX drivers) _may_ break large
 * stretches of unmarked content into smaller chunks and call
 * characters() for each smaller chunk
 * CURRENT IMPLEMENTATION DOES NOT DEAL WITH THIS
 * COULD CAUSE PROBLEM WHEN READING IN SEQUENCE RESIDUES
 * haven't seen a problem yet though -- GAH 6-15-98
 */
public class XMLParser implements ContentHandler {

    org.xml.sax.XMLReader xml_reader = null;

    String default_parser_name = "";

    protected XMLElement root_element = null;

    private XMLElement current_element = null;

    int element_count = 0;

    Stack element_chain;

    public XMLParser() {
        super();
    }

    public XMLParser(int initial_count) {
        super();
        element_count = initial_count;
    }

    public XMLElement getRootElement() {
        return root_element;
    }

    public XMLElement readXML(String doc_url_string) {
        URL doc_url = null;
        try {
            doc_url = new URL(doc_url_string);
            readXML(doc_url);
        } catch (Exception ex1) {
            try {
                InputStream xml_stream = new FileInputStream(doc_url_string);
                BufferedInputStream bis = new BufferedInputStream(xml_stream);
                readXML(bis);
            } catch (Exception ex2) {
                System.err.println("caught Exception in readXML(doc_url_string): ");
                System.out.println(ex2.getMessage());
                ex2.printStackTrace();
                clean();
            }
        }
        return root_element;
    }

    /**
   * Parse an XML document -- GAH 5-12-98
   */
    public XMLElement readXML(URL doc_url) {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = doc_url.openStream();
            try {
                bis = new BufferedInputStream(is);
                try {
                    readXML(bis);
                } catch (Exception ex) {
                    System.err.println("Failed to read XML from " + doc_url + ": " + ex);
                    clean();
                }
            } catch (Exception ex) {
                System.err.println("caught Exception in BufferedInputStream for " + doc_url + ": " + ex);
                clean();
            }
        } catch (Exception ex) {
            clean();
        }
        return root_element;
    }

    /**
   *  reads an XML document from an InputStream and
   *  returns an hierarchy of XMLElements derived from the XML document
   */
    public XMLElement readXML(InputStream istream) {
        element_chain = new Stack();
        clean();
        try {
            if (xml_reader == null) {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setValidating(false);
                SAXParser saxParser;
                try {
                    saxParser = spf.newSAXParser();
                } catch (ParserConfigurationException e) {
                    throw new IOException("Couldn't load parser");
                }
                xml_reader = saxParser.getXMLReader();
                try {
                    xml_reader.setContentHandler(this);
                    try {
                        xml_reader.parse(new InputSource(istream));
                    } catch (Exception e) {
                        System.err.println("Fatal Error near element # " + element_count + ", " + " : " + e.getMessage());
                        e.printStackTrace();
                        clean();
                    }
                } catch (Exception e) {
                    System.err.println("Fatal Error in xml_reader.setDocumentHandler: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal Error in xml_reader new: " + e.getMessage());
        }
        return root_element;
    }

    public void setParser(org.xml.sax.XMLReader xml_reader) {
        this.xml_reader = xml_reader;
    }

    public void startElement(String uri, String local_name, String name, Attributes atts) {
        try {
            XMLElement parent_element = current_element;
            current_element = new XMLElement(name);
            if (parent_element == null) {
                root_element = current_element;
                System.err.println("Starting XML parse");
            } else {
                element_chain.push(parent_element);
                current_element.setParent(parent_element);
            }
            element_count++;
            current_element.setAttributes(atts, element_count);
        } catch (Exception ex) {
            System.err.println("Exception parsing " + name + " " + ex.getMessage());
            ex.printStackTrace();
            System.exit(0);
        }
    }

    public void endElement(String uri, String local_name, String name) {
        if (current_element != null && current_element.getCharData() != null) {
            current_element.setCharData(current_element.getCharData().trim());
        }
        if (!element_chain.empty()) {
            current_element = (XMLElement) element_chain.pop();
        }
    }

    public void characters(char ch[], int start, int length) {
        String char_data;
        if ((current_element.getType().equalsIgnoreCase("residues"))) {
            char_data = XML_util.filterWhiteSpace(ch, start, length);
            char_data = char_data.toUpperCase();
        } else {
            char_data = new String(ch, start, length);
        }
        if (char_data != null && !char_data.equals("")) {
            current_element.appendCharData(char_data);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) {
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    public void doctype(String name, String publicID, String systemID) {
    }

    public void processingInstruction(String name, String remainder) {
    }

    public void skippedEntity(String name) throws SAXException {
        System.out.println("Skipped XML entity " + name);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void printAttributes(Attributes atts) {
        int max = atts.getLength();
        String allatts = "     Attributes: ";
        String aname, special, curatt;
        if (max == 0) {
            System.err.println("     [Attributes not available]");
        }
        for (int i = 0; i < max; i++) {
            aname = atts.getLocalName(i);
            special = atts.getType(i);
            if (special == null) {
                special = "";
            }
            curatt = (aname + "=\"" + atts.getValue(aname) + '"' + special);
            allatts = allatts + curatt + ", ";
        }
        System.err.println(allatts);
    }

    public void clean() {
        current_element = null;
        root_element = null;
        System.gc();
    }
}
