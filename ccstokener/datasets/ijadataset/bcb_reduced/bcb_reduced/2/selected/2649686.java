package jp.riken.omicspace.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.String;
import java.lang.StringBuffer;
import java.lang.System;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jp.riken.omicspace.osml.Arrow;
import jp.riken.omicspace.osml.Candidate;
import jp.riken.omicspace.osml.Dataset;
import jp.riken.omicspace.osml.FunctionalClass;
import jp.riken.omicspace.osml.Graphics;
import jp.riken.omicspace.osml.OmicElement;
import jp.riken.omicspace.osml.OmicInteraction;
import jp.riken.omicspace.osml.Osml;
import jp.riken.omicspace.osml.OsmlBuilder;
import jp.riken.omicspace.osml.OsmlBuilderFactory;
import jp.riken.omicspace.osml.Position;
import jp.riken.omicspace.osml.Property;
import jp.riken.omicspace.osml.Shape;
import jp.riken.omicspace.osml.SuperClass;
import jp.riken.omicspace.osml.impl.OsmlBuilderImpl;
import jp.riken.omicspace.service.Area;
import jp.riken.omicspace.service.Chromosome;
import jp.riken.omicspace.service.SpeciesVersion;
import jp.riken.omicspace.service.CoordinateManager;
import jp.riken.omicspace.service.InitXmlParser;
import jp.riken.omicspace.service.LineMode;
import jp.riken.omicspace.service.ListMode;
import jp.riken.omicspace.service.Service;
import jp.riken.omicspace.service.Translator;
import jp.riken.omicspace.lib.XMLElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MenuXmlParser extends Object {

    public static Service[] parse(String s, String s1, String s2) {
        try {
            ArrayList arraylist;
            Service aservice[];
            arraylist = new ArrayList();
            URL url = new URL(s + "/gps/menu?alias=" + s1 + "&hCheck='" + s2.replace(" ", "%20") + "'");
            DocumentBuilderFactory documentbuilderfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentbuilder = documentbuilderfactory.newDocumentBuilder();
            Document document = documentbuilder.parse(url.openStream());
            NodeList nodelist = document.getElementsByTagName("entryset");
            for (int i = 0; i < nodelist.getLength(); i++) {
                Element element = (Element) nodelist.item(i);
                String s3 = element.getAttribute("name");
                String s4 = element.getAttribute("src");
                if (s4 == null || s4.equals("")) s4 = ""; else if (s4.length() < 7 || !s4.substring(0, 7).equals("http://")) s4 = s + s4;
                arraylist.add(new Service(s3, s1 + "." + s2 + "." + s3, s4));
                NodeList nodelist1 = element.getChildNodes();
                for (int j = 0; j < nodelist1.getLength(); j++) {
                    if (nodelist1.item(j).getNodeType() != 1 || !nodelist1.item(j).getNodeName().equals("entry")) continue;
                    Element element1 = (Element) nodelist1.item(j);
                    String s5 = element1.getAttribute("name");
                    String s6 = element1.getAttribute("src");
                    if (s6 == null || s6.equals("")) s6 = ""; else if (s6.length() < 7 || !s6.substring(0, 7).equals("http://")) s6 = s + s6;
                    arraylist.add(new Service(s5, s1 + "." + s2 + "." + s3 + "." + s5, s6));
                    NodeList nodelist2 = element1.getChildNodes();
                    for (int k = 0; k < nodelist2.getLength(); k++) {
                        if (nodelist2.item(k).getNodeType() != 1 || !nodelist2.item(k).getNodeName().equals("menu")) continue;
                        Element element2 = (Element) nodelist2.item(k);
                        String s7 = element2.getAttribute("name");
                        String s8 = element2.getAttribute("src");
                        if (s8 == null || s8.equals("")) s8 = ""; else if (s8.length() < 7 || !s8.substring(0, 7).equals("http://")) s8 = s + s8;
                        arraylist.add(new Service(s7, s1 + "." + s2 + "." + s3 + "." + s5 + "." + s7, s8));
                    }
                }
            }
            aservice = new Service[arraylist.size()];
            return (Service[]) arraylist.toArray(aservice);
        } catch (Exception ex) {
            System.err.println("---Error occured. MenuXmlParser.parse()---");
            System.out.println("Exception:" + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    public static void main(String args[]) {
        Service aservice[] = parse("http://omicspace.riken.jp/gps/menu", "Mm", "NCBIm36");
        if (aservice == null || aservice.length == 0) {
            System.out.println("Service is not found.");
        } else {
            for (int i = 0; i < aservice.length; i++) System.out.println(aservice[i].getPath());
        }
    }

    MenuXmlParser() {
        super();
        return;
    }
}
