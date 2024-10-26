package net.aetherial.gis.garmin;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.w3c.dom.svg.*;
import javax.xml.parsers.*;
import net.aetherial.gis.garmin.*;

public final class GarminGMLDoc {

    static Document d = null;

    static Element root = null;

    static SVGDocument svgDoc = null;

    public GarminGMLDoc() {
    }

    public static Document getDocument() {
        try {
            if (d == null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                d = db.newDocument();
                root = d.createElement("GarminDataCollection");
                d.appendChild(root);
            }
            d.setNodeValue("garminDataCollection");
        } catch (Exception e) {
            System.out.println(e);
        }
        return d;
    }

    public static void resetGMLDoc() {
        d = null;
    }

    public static Element addWaypoint(Waypoint w) {
        Position p = w.getPosition();
        String name = w.getIdentifier();
        if (GarminConfiguration.addWaypointToLog(name)) {
            double lon, lat;
            Element e = d.createElement("waypoint");
            lat = p.getDoubleDegreeLatitude();
            lon = p.getDoubleDegreeLongitude();
            if (p.getLongOrientChar() == 'W') lon = 0 - lon;
            if (p.getLatOrientChar() == 'N') lat = 0 - lat;
            Element f = newGMLPoint(lon, lat, name);
            e.appendChild(f);
            root.appendChild(e);
            return e;
        } else return null;
    }

    public static Element addTrack(String name) {
        if (d == null) getDocument();
        if (GarminConfiguration.addTrackToLog(name)) {
            Element e = d.createElement("track");
            Element ename = d.createElement("gml:name");
            Text t = d.createTextNode(name);
            ename.appendChild(t);
            e.appendChild(ename);
            Element eline = d.createElement("gml:LineString");
            e.appendChild(eline);
            root.appendChild(e);
            return eline;
        } else return null;
    }

    public static Element addTrackPoint(Element e, TrackPoint tp) {
        if (e == null) return null;
        Position p = tp.getPosition();
        float lon = p.getFloatDegreeLongitude();
        float lat = p.getFloatDegreeLatitude();
        if (p.getLongOrientChar() == 'W') lon = 0 - lon;
        if (p.getLatOrientChar() == 'N') lat = 0 - lat;
        Element c = d.createElement("gml:coord");
        Element x = d.createElement("X");
        Text t = d.createTextNode(Float.toString(lon));
        x.appendChild(t);
        Element y = d.createElement("Y");
        t = d.createTextNode(Float.toString(lat));
        y.appendChild(t);
        c.appendChild(x);
        c.appendChild(y);
        e.appendChild(c);
        return c;
    }

    public static String printDoc() {
        String n = "";
        n = walk(d);
        return n;
    }

    private static Element newGMLPoint(double lon, double lat, String n) {
        Element e = d.createElement("gml:Point");
        Element ename = d.createElement("gml:name");
        Text t = d.createTextNode(n);
        ename.appendChild(t);
        e.appendChild(ename);
        Element epos = d.createElement("gml:coord");
        Element ex = d.createElement("X");
        t = d.createTextNode(Double.toString(lon));
        ex.appendChild(t);
        Element ey = d.createElement("Y");
        t = d.createTextNode(Double.toString(lat));
        ey.appendChild(t);
        epos.appendChild(ex);
        epos.appendChild(ey);
        e.appendChild(epos);
        return e;
    }

    private static String walk(Node node) {
        String n = "";
        if (node == null) return n;
        int type = node.getNodeType();
        switch(type) {
            case Node.DOCUMENT_NODE:
                {
                    n = n + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
                    break;
                }
            case Node.ELEMENT_NODE:
                {
                    n = n + "<" + node.getNodeName();
                    NamedNodeMap nnm = node.getAttributes();
                    if (nnm != null) {
                        int len = nnm.getLength();
                        Attr attr;
                        for (int i = 0; i < len; i++) {
                            attr = (Attr) nnm.item(i);
                            n = n + " " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"";
                        }
                    }
                    n = n + ">";
                    break;
                }
            case Node.ENTITY_REFERENCE_NODE:
                {
                    n = n + "&" + node.getNodeName() + ";";
                    break;
                }
            case Node.CDATA_SECTION_NODE:
                {
                    n = n + "<![CDATA[" + node.getNodeValue() + "]]>";
                    break;
                }
            case Node.TEXT_NODE:
                {
                    n = n + node.getNodeValue();
                    break;
                }
            case Node.PROCESSING_INSTRUCTION_NODE:
                {
                    n = n + "<?" + node.getNodeName();
                    String data = node.getNodeValue();
                    if (data != null && data.length() > 0) {
                        n = n + " " + data;
                    }
                    n = n + "?>";
                    break;
                }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            n = n + walk(child);
        }
        if (type == Node.ELEMENT_NODE) {
            n = n + "</" + node.getNodeName() + ">\n";
        }
        return n;
    }

    private static void addWaypointToSVG(float x, float y, String n) {
        if (svgDoc == null) svgDoc = GarminConfiguration.getSVGDoc();
        Element r = svgDoc.getRootElement();
        Element e = svgDoc.createElement("circle");
        e.setAttribute("style", "fill:blue;stroke:red;");
        e.setAttribute("cx", Float.toString(x));
        e.setAttribute("cy", Float.toString(y));
        e.setAttribute("r", "10");
        r.appendChild(e);
    }

    public static String updateSVGFile(String fname) {
        try {
            String tmp = GarminConfiguration.getTmpFile();
            if (tmp == null) tmp = fname;
            File f = new File(tmp);
            if (f.exists() && !(fname.startsWith("jar"))) f.delete();
            Date d = new Date();
            int hc = d.hashCode();
            if (hc < 0) hc = 0 - hc;
            fname = System.getProperty("user.dir") + System.getProperty("file.separator") + Integer.toString(hc) + ".svg";
            File ftmp = new File(fname);
            ftmp.createNewFile();
            GarminConfiguration.setTmpFile(fname);
            PrintWriter fout = new PrintWriter(new FileOutputStream(ftmp));
            URL url = new URL("jar:file:net.aetherial.gis.garmin.jar!/net/aetherial/gis/garmin/defaultMap.svg");
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            BufferedReader bin = new BufferedReader(new InputStreamReader(jarConnection.getInputStream()));
            String line = null;
            for (line = bin.readLine(); !(line.startsWith("</svg>")) && line != null; line = bin.readLine()) {
                fout.println(line);
            }
            NodeList nl = root.getElementsByTagName("waypoint");
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                String name = "no name";
                String x = "0";
                String y = "0";
                NodeList nln = e.getElementsByTagName("gml:name");
                for (int j = 0; nln != null && j < nln.getLength(); j++) {
                    if (nln.item(j) != null) {
                        NodeList nlc = nln.item(j).getChildNodes();
                        name = nlc.item(0).getNodeValue();
                    }
                }
                nln = e.getElementsByTagName("X");
                for (int j = 0; nln != null && j < nln.getLength(); j++) {
                    if (nln.item(j) != null) {
                        NodeList nlc = nln.item(j).getChildNodes();
                        x = nlc.item(0).getNodeValue();
                    }
                }
                nln = e.getElementsByTagName("Y");
                for (int j = 0; nln != null && j < nln.getLength(); j++) {
                    if (nln.item(j) != null) {
                        NodeList nlc = nln.item(j).getChildNodes();
                        y = nlc.item(0).getNodeValue();
                    }
                }
                fout.println("<circle style=\"fill:gray;stroke:none;opacity:0.25\" cx=\"" + x + "\" cy=\"" + y + "\" r=\"1\"/>");
                fout.println("<circle style=\"fill:black;stroke:none;opacity:1.0\" cx=\"" + x + "\" cy=\"" + y + "\" r=\"0.001\"/>");
                x = Double.toString(Double.parseDouble(x) + 0.0015);
                y = Double.toString(Double.parseDouble(y) + 0.0015);
                fout.println("<text x=\"" + x + "\" y=\"" + y + "\" style=\"font-size:0.01px;font-family:san-serif;\">" + name + "</text>");
            }
            nl = root.getElementsByTagName("track");
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                String name = "no name";
                String path = "M ";
                NodeList nln = e.getElementsByTagName("gml:name");
                for (int j = 0; nln != null && j < nln.getLength(); j++) {
                    if (nln.item(j) != null) {
                        NodeList nlc = nln.item(j).getChildNodes();
                        name = nlc.item(0).getNodeValue();
                    }
                }
                nln = e.getElementsByTagName("gml:coord");
                String text = "";
                for (int j = 0; nln != null && j < nln.getLength(); j++) {
                    String x = "0";
                    String y = "0";
                    NodeList nln2 = ((Element) nln.item(j)).getElementsByTagName("X");
                    for (int k = 0; nln2 != null && k < nln2.getLength(); k++) {
                        if (nln2.item(k) != null) {
                            NodeList nlc = nln2.item(k).getChildNodes();
                            x = nlc.item(0).getNodeValue();
                        }
                    }
                    nln2 = ((Element) nln.item(j)).getElementsByTagName("Y");
                    for (int k = 0; nln2 != null && k < nln2.getLength(); k++) {
                        if (nln2.item(k) != null) {
                            NodeList nlc = nln2.item(k).getChildNodes();
                            y = nlc.item(0).getNodeValue();
                        }
                    }
                    if (j != (nln.getLength() - 1)) path = path + x + " " + y + " L "; else path = path + x + " " + y;
                    if (j == 0) {
                        fout.println("<circle style=\"fill:gray;stroke:none;opacity:0.25\" cx=\"" + x + "\" cy=\"" + y + "\" r=\"1\"/>");
                        text = "<text x=\"" + x + "\" y=\"" + y + "\" style=\"font-size:0.25;font-family:san-serif;\">" + name + "</text>";
                    }
                }
                fout.println("<path d=\"" + path + "\" style=\"stroke:red;stroke-width:0.0100;fill:none;opacity:0.25;\"/>");
                fout.println("<path d=\"" + path + "\" style=\"stroke:black;stroke-width:0.00100;fill:none;\"/>");
                fout.println(text);
            }
            fout.println("</svg>");
            fout.close();
            bin.close();
            fname = "file:///" + System.getProperty("user.dir") + System.getProperty("file.separator") + Integer.toString(hc) + ".svg";
        } catch (IOException ex) {
            System.out.println(ex);
        }
        return fname;
    }
}
