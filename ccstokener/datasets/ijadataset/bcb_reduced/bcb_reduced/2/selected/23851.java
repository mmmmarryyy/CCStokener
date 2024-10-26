package com.jedox.etl.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XMLUtil {

    public static String jdomToString(Element element) throws IOException {
        Element root = (Element) element.clone();
        StringWriter writer = new StringWriter();
        Document document = new Document();
        document.setRootElement(root);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(document, writer);
        return writer.toString();
    }

    public static Element stringTojdom(String xmlString) throws IOException, JDOMException {
        Document doc = new SAXBuilder().build(new StringReader(xmlString));
        return doc.getRootElement();
    }

    public static Document readDocument(String filename) throws IOException, JDOMException {
        try {
            URL url = new URL(filename);
            return readDocument(url);
        } catch (Exception e) {
        }
        ;
        File f = new File(filename);
        return readDocument(f.toURI().toURL());
    }

    public static Document readDocument(URL url) throws IOException, JDOMException {
        Reader r = new BufferedReader(new InputStreamReader(url.openStream(), "UTF8"));
        Document document = new SAXBuilder().build(r);
        return document;
    }
}
