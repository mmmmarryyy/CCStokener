package bop.xml;

import java.net.*;
import java.util.*;
import java.io.*;
import org.xml.sax.*;

/**
 * WARNING -- AElfred (and other SAX drivers) _may_ break large 
 * stretches of unmarked content into smaller chunks and call 
 * characters() for each smaller chunk
 * CURRENT IMPLEMENTATION DOES NOT DEAL WITH THIS 
 * COULD CAUSE PROBLEM WHEN READING IN SEQUENCE RESIDUES
 * haven't seen a problem yet though -- GAH 6-15-98
 */
public class XML_Converter implements DocumentHandler {

    org.xml.sax.Parser xml_parser = null;

    String default_parser_name = "com.microstar.xml.SAXDriver";

    protected XMLElement root_element = null;

    private XMLElement current_element = null;

    int element_count = 0;

    Stack element_chain;

    public XML_Converter(int initial_count) {
        super();
        element_count = initial_count;
    }

    public XMLElement getRootElement() {
        return root_element;
    }

    public boolean readXML(String doc_url_string) {
        URL doc_url = null;
        boolean read_it = false;
        try {
            doc_url = new URL(doc_url_string);
            read_it = readXML(doc_url);
        } catch (Exception ex1) {
            try {
                InputStream xml_stream = new FileInputStream(doc_url_string);
                BufferedInputStream bis = new BufferedInputStream(xml_stream);
                read_it = readXML(bis);
            } catch (Exception ex2) {
                System.err.println("caught Exception in readXML(doc_url_string): ");
                System.out.println(ex2.getMessage());
                ex2.printStackTrace();
            }
        }
        return read_it;
    }

    /**
   * Parse an XML document -- GAH 5-12-98
   */
    public boolean readXML(URL doc_url) {
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            is = doc_url.openStream();
        } catch (Exception ex) {
            return false;
        }
        try {
            bis = new BufferedInputStream(is);
        } catch (Exception ex) {
            System.err.println("caught Exception in BufferedInputStream for " + doc_url + ": " + ex);
            return false;
        }
        try {
            return (readXML(bis));
        } catch (Exception ex) {
            System.err.println("Failed to read XML from " + doc_url + ": " + ex);
            return false;
        }
    }

    /** 
   *  reads an XML document from an InputStream and 
   *  returns an hierarchy of XMLElements derived from the XML document
   */
    public boolean readXML(InputStream istream) {
        element_chain = new Stack();
        try {
            if (xml_parser == null) {
                xml_parser = (org.xml.sax.Parser) Class.forName(default_parser_name).newInstance();
            }
        } catch (Exception e) {
            System.err.println("Fatal Error in xml_parser new: " + e.getMessage());
            return false;
        }
        try {
            xml_parser.setDocumentHandler(this);
        } catch (Exception e) {
            System.err.println("Fatal Error in xml_parser.setDocumentHandler: " + e.getMessage());
            return false;
        }
        try {
            xml_parser.parse(new InputSource(istream));
        } catch (Exception e) {
            System.err.println("Fatal Error near element # " + element_count + ", " + " : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void setParser(org.xml.sax.Parser xml_parser) {
        this.xml_parser = xml_parser;
    }

    public void startElement(String name, AttributeList atts) {
        try {
            XMLElement parent_element = current_element;
            current_element = new XMLElement(name);
            if (parent_element == null) {
                root_element = current_element;
                System.err.println("Starting game parse");
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

    public void endElement(String name) {
        if (!element_chain.empty()) {
            current_element = (XMLElement) element_chain.pop();
        }
    }

    public void characters(char ch[], int start, int length) {
        String char_data;
        if ((current_element.getType().equals("Residues")) || (current_element.getType().equals("residues"))) {
            char_data = XML_util.filterWhiteSpace(ch, start, length);
        } else {
            char_data = XML_util.trimWhiteSpace(ch, start, length);
        }
        if (char_data != null && !char_data.equals("")) {
            current_element.setCharData(char_data);
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

    public void setDocumentLocator(Locator locator) {
    }

    public void printAttributes(AttributeList atts) {
        int max = atts.getLength();
        String allatts = "     Attributes: ";
        String aname, special, curatt;
        if (max == 0) {
            System.err.println("     [Attributes not available]");
        }
        for (int i = 0; i < max; i++) {
            aname = atts.getName(i);
            special = atts.getType(i);
            if (special == null) {
                special = "";
            }
            curatt = (aname + "=\"" + atts.getValue(aname) + '"' + special);
            allatts = allatts + curatt + ", ";
        }
        System.err.println(allatts);
    }
}
