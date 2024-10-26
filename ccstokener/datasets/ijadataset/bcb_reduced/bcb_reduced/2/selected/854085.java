package guestbook;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.net.*;
import java.io.*;
import org.xml.sax.InputSource;
import guestbook.FDT_data;
import guestbook.PK_data;
import guestbook.RSS_data;
import guestbook.URLGrabber;

/**
 * 
 * Torino Cultura
 * http://www.torinocultura.it/eventi/index.shtml
 * 
 * Eventi di Domani
 * http://www.torinocultura.it/servizionline/memento/rss.php?context=rss&action=rss&currDate=tomorrow&refProgetto=2
 * 
 * ToRSS - I feed RSS del sito della Citta'
 * http://www.comune.torino.it/torss/
 * 
 */
public class XMLReader {

    public static void get_FDT_data() {
        try {
            FileWriter file_writer = new FileWriter("xml_data/FDT_data_dump.xml");
            BufferedWriter file_buffered_writer = new BufferedWriter(file_writer);
            URL fdt = new URL("http://opendata.5t.torino.it/get_fdt");
            URLConnection url_connection = fdt.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(url_connection.getInputStream()));
            String input_line;
            int num_lines = 0;
            while ((input_line = in.readLine()) != null) {
                file_buffered_writer.write(input_line + "\n");
                num_lines++;
            }
            System.out.println("FDT :: Writed " + num_lines + " lines.");
            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void get_PK_data() {
        try {
            FileWriter file_writer = new FileWriter("xml_data/PK_data_dump.xml");
            BufferedWriter file_buffered_writer = new BufferedWriter(file_writer);
            URL fdt = new URL("http://opendata.5t.torino.it/get_pk");
            URLConnection url_connection = fdt.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(url_connection.getInputStream()));
            String input_line;
            int num_lines = 0;
            while ((input_line = in.readLine()) != null) {
                file_buffered_writer.write(input_line + "\n");
                num_lines++;
            }
            System.out.println("Parking :: Writed " + num_lines + " lines.");
            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static FDT_data[] parse_FDT_data() {
        try {
            System.out.println("Start Data Parser");
            DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
            Document document = document_builder.parse(new InputSource(new StringReader(URLGrabber.getDocumentAsString("http://opendata.5t.torino.it/get_fdt"))));
            document.getDocumentElement().normalize();
            System.out.println("Root element : " + document.getDocumentElement().getNodeName());
            System.out.println("generation_time : " + document.getDocumentElement().getAttribute("generation_time"));
            System.out.println("start_time : " + document.getDocumentElement().getAttribute("start_time"));
            System.out.println("end_time : " + document.getDocumentElement().getAttribute("end_time"));
            NodeList node_list = document.getElementsByTagName("FDT_data");
            System.out.println("Information of FDT_data");
            int node_number = node_list.getLength();
            System.out.println("Found " + node_number + " elements.");
            FDT_data[] result = new FDT_data[node_number];
            FDT_data fdt_unit = null;
            String lcd1 = "";
            String road_LCD = "";
            String road_name = "";
            String offset = "";
            String direction = "";
            String lat = "";
            String lng = "";
            String accuracy = "";
            String period = "";
            String flow = "";
            String speed = "";
            Date date = new Date();
            for (int i = 0; i < node_number; i++) {
                Node node = node_list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    System.out.println("     Element TagName: " + element.getTagName());
                    lcd1 = element.getAttribute("lcd1");
                    road_LCD = element.getAttribute("Road_LCD");
                    road_name = element.getAttribute("Road_name");
                    offset = element.getAttribute("offset");
                    direction = element.getAttribute("direction");
                    lat = element.getAttribute("lat");
                    lng = element.getAttribute("lng");
                    accuracy = element.getAttribute("accuracy");
                    period = element.getAttribute("period");
                    System.out.println("	lcd1: " + lcd1);
                    System.out.println("	Road_LCD: " + road_LCD);
                    System.out.println("	Road_name: " + road_name);
                    System.out.println("	offset: " + offset);
                    System.out.println("	direction: " + direction);
                    System.out.println("	lat: " + lat);
                    System.out.println("	lng: " + lng);
                    System.out.println("	accuracy: " + accuracy);
                    System.out.println("	period: " + period);
                    NodeList element_list = element.getElementsByTagName("speedflow");
                    System.out.println("	Flow data:");
                    for (int j = 0; j < element_list.getLength(); j++) {
                        Node inner_node = element_list.item(j);
                        if (inner_node.getNodeType() == Node.ELEMENT_NODE) {
                            Element inner_element = (Element) inner_node;
                            System.out.println("	Element TagName: " + inner_element.getTagName());
                            flow = inner_element.getAttribute("flow");
                            speed = inner_element.getAttribute("speed");
                            System.out.println("		flow: " + flow);
                            System.out.println("		speed: " + speed);
                        }
                    }
                    fdt_unit = new FDT_data(Long.parseLong(lcd1), Long.parseLong(road_LCD), road_name, Long.parseLong(offset), direction, new Double(lat), new Double(lng), new Integer(accuracy), new Integer(period), new Double(flow), new Double(speed), date);
                    result[i] = fdt_unit;
                    System.out.println("	---------------------------");
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PK_data[] parse_PK_data() {
        try {
            System.out.println("Start Data Parser");
            DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
            Document document = document_builder.parse(new InputSource(new StringReader(URLGrabber.getDocumentAsString("http://opendata.5t.torino.it/get_pk"))));
            document.getDocumentElement().normalize();
            System.out.println("Root element : " + document.getDocumentElement().getNodeName());
            System.out.println("generation_time : " + document.getDocumentElement().getAttribute("generation_time"));
            System.out.println("start_time : " + document.getDocumentElement().getAttribute("start_time"));
            System.out.println("end_time : " + document.getDocumentElement().getAttribute("end_time"));
            NodeList node_list = document.getElementsByTagName("td:PK_data");
            System.out.println("Information of PK_data");
            int node_number = node_list.getLength();
            System.out.println("Found " + node_number + " elements.");
            PK_data[] result = new PK_data[node_number];
            PK_data pk_unit = null;
            String id = "0";
            String name = "0";
            String status = "0";
            String total = "0";
            String lat = "0";
            String lng = "0";
            String free = "0";
            String tendence = "0";
            Date date = new Date();
            for (int i = 0; i < node_number; i++) {
                Node node = node_list.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    System.out.println("     Element TagName: " + element.getTagName());
                    id = element.getAttribute("ID");
                    name = element.getAttribute("Name");
                    status = element.getAttribute("status");
                    total = element.getAttribute("Total");
                    lat = element.getAttribute("lat");
                    lng = element.getAttribute("lng");
                    if (!element.getAttribute("Free").equals("")) free = element.getAttribute("Free");
                    if (!element.getAttribute("tendence").equals("")) tendence = element.getAttribute("tendence");
                    System.out.println("	id: " + id);
                    System.out.println("	name: " + name);
                    System.out.println("	status: " + status);
                    System.out.println("	total: " + total);
                    System.out.println("	lat: " + lat);
                    System.out.println("	lng: " + lng);
                    System.out.println("	free: " + free);
                    System.out.println("	tendence: " + tendence);
                    pk_unit = new PK_data(Long.parseLong(id), name, new Integer(status), new Integer(total), new Double(lat), new Double(lng), new Integer(free), new Integer(tendence), date);
                    result[i] = pk_unit;
                    System.out.println("	---------------------------");
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList parse_RSS_data() {
        try {
            System.out.println("Start RSS Parser");
            DocumentBuilderFactory document_builder_factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document_builder = document_builder_factory.newDocumentBuilder();
            Document document = document_builder.parse(new InputSource(new StringReader(URLGrabber.getDocumentAsString("http://www.comune.torino.it/cgi-bin/torss/rssfeed.cgi?id=93"))));
            Node node;
            Node a_node;
            Node an_inner_node;
            Node an_inner_inner_node;
            NodeList node_list;
            NodeList inner_node_list;
            NodeList inner_inner_node_list;
            node = document.getFirstChild();
            node_list = node.getChildNodes();
            String channel_title = "";
            String channel_link = "";
            String channel_description = "";
            String channel_copyright = "";
            String channel_pubDate = "";
            String channel_category = "";
            String item_title = "";
            String item_link = "";
            String item_pubDate = "";
            String item_description = new String("".getBytes(), "UTF-8");
            String unit = "";
            String lat = "0";
            String lng = "0";
            RSS_data rss_unit = null;
            Hashtable rss_map = new Hashtable();
            ArrayList result = new ArrayList();
            DateFormat date_format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            System.out.println("[0] - [" + node.getNodeName() + "]");
            for (int i = 0; i < node_list.getLength(); i++) {
                a_node = node_list.item(i);
                if (a_node.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.println("  [1] - [" + a_node.getNodeName() + "]");
                    inner_node_list = a_node.getChildNodes();
                    for (int j = 0; j < inner_node_list.getLength(); j++) {
                        an_inner_node = inner_node_list.item(j);
                        if ((!an_inner_node.getNodeName().equals("item")) && (!an_inner_node.getNodeName().equals("#text"))) {
                            System.out.println("    [2] " + an_inner_node.getNodeName() + ": " + an_inner_node.getTextContent());
                            if (an_inner_node.getNodeName().equals("title")) channel_title = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("link")) channel_link = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("description")) channel_description = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("copyright")) channel_copyright = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("pubDate")) channel_pubDate = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("category")) channel_category = an_inner_node.getTextContent();
                        }
                        if (an_inner_node.getNodeName().equals("item")) {
                            System.out.println("    [2] - [" + an_inner_node.getNodeName() + "]");
                        }
                        if (an_inner_node.hasChildNodes() && (an_inner_node.getNodeName().equals("item"))) {
                            inner_inner_node_list = an_inner_node.getChildNodes();
                            for (int k = 0; k < inner_inner_node_list.getLength(); k++) {
                                an_inner_inner_node = inner_inner_node_list.item(k);
                                if (!(an_inner_inner_node.getTextContent().equals(""))) {
                                    if (!(an_inner_inner_node.getNodeName().equals("#text")) && !(an_inner_inner_node.getNodeName().equals("link"))) {
                                        System.out.println("        [3] " + an_inner_inner_node.getNodeName() + ": " + an_inner_inner_node.getTextContent());
                                        if (an_inner_inner_node.getNodeName().equals("title")) item_title = an_inner_inner_node.getTextContent();
                                        if (an_inner_inner_node.getNodeName().equals("pubDate")) item_pubDate = an_inner_inner_node.getTextContent();
                                        if (an_inner_inner_node.getNodeName().equals("description")) {
                                            an_inner_inner_node.getTextContent().replace("&quot;", "");
                                            if (an_inner_inner_node.getTextContent().length() >= 500) {
                                                String tmp_item_description = new String(an_inner_inner_node.getTextContent().substring(0, 400));
                                                item_description = new String(tmp_item_description.getBytes("ISO8859-2"), "UTF-8");
                                            } else {
                                                item_description = new String(an_inner_inner_node.getTextContent().getBytes("ISO8859-2"), "UTF-8");
                                            }
                                        }
                                    }
                                    if (an_inner_inner_node.getNodeName().equals("link")) {
                                        String tmp_link = an_inner_inner_node.getTextContent();
                                        System.out.println("        [3] " + an_inner_inner_node.getNodeName() + ": " + tmp_link);
                                        if (an_inner_inner_node.getNodeName().equals("link")) item_link = an_inner_inner_node.getTextContent();
                                        item_link = tmp_link;
                                    }
                                }
                            }
                            unit = "Unit data: [" + item_title + " - " + item_pubDate + " - " + item_description + " - " + item_link + "]";
                            rss_map.put(item_title, unit);
                            rss_unit = new RSS_data(item_title, item_link, "item_category", new String(item_description.getBytes("ISO8859-2"), "UTF-8"), new Double(lat), new Double(lng), date_format.parse(item_pubDate));
                            result.add(rss_unit);
                        }
                        System.out.println("--------------");
                    }
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String argv[]) {
        parse_FDT_data();
        parse_PK_data();
    }
}
