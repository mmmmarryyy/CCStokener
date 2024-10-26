package com.ryanhirsch.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

public class ParseXML extends DefaultHandler {

    private static XMLTag master;

    private static XMLTag cur;

    public ParseXML() {
        super();
    }

    public ParseXML(String path) {
        super();
        master = null;
        cur = null;
        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            ParseXML handler = new ParseXML();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            BufferedReader reader = null;
            if (path.contains("http://")) {
                reader = readURL(path);
            } else {
                reader = readFile(path);
            }
            xr.parse(new InputSource(reader));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Malformed XML ... exiting");
            System.exit(1);
        }
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    public void startElement(String uri, String name, String qName, Attributes atts) {
        XMLTag tag = new XMLTag(qName);
        if ("".equals(uri)) {
            if (atts.getLength() != 0) {
                for (int i = 0; i < atts.getLength(); i++) {
                    String attName = atts.getLocalName(i);
                    String attValue = atts.getValue(i);
                    tag.addAttribute(attName, attValue);
                }
            }
            tag.setName(qName);
            tag.setLocalName(name);
        } else {
            tag.setNamespace(uri);
        }
        if (master == null) {
            master = new XMLTag(tag);
            cur = master;
        } else {
            tag.setParent(cur);
            cur.addChild(tag);
            cur = tag;
        }
    }

    public void endElement(String uri, String name, String qName) {
        if ("".equals(uri)) {
        } else {
        }
        cur = cur.getParent();
    }

    public void characters(char ch[], int start, int length) {
        StringBuffer test = new StringBuffer();
        for (int i = start; i < start + length; i++) {
            test.append(ch[i]);
        }
        String blah = test.toString();
        if (!"".equals(blah.trim())) {
            if (cur.getValue() != null) {
                cur.setValue(cur.getValue() + blah.trim());
            } else {
                cur.setValue(blah.trim());
            }
        }
    }

    public XMLTag getMaster() {
        return master;
    }

    public XMLTag getCur() {
        return cur;
    }

    public static BufferedReader readURL(String url) throws Exception {
        return new BufferedReader(new InputStreamReader(new URL(url).openStream()));
    }

    public static BufferedReader readFile(String path) {
        FileReader blah;
        try {
            blah = new FileReader(new File(path));
            return new BufferedReader(blah);
        } catch (FileNotFoundException e) {
            System.out.println("File " + path + " not found");
            System.exit(1);
        }
        return null;
    }
}
