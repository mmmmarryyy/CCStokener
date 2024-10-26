package guestbook;

import javax.xml.parsers.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.URL;
import java.net.MalformedURLException;
import org.xml.sax.InputSource;
import org.xml.sax.*;
import java.nio.charset.Charset;
import guestbook.FDT_data;
import guestbook.PK_data;
import guestbook.RSS_data;
import guestbook.URLGrabber;

/**
 * 
 * Torino Cultura
 * http://www.torinocultura.it/eventi/index.shtml
 *
 * RSS index
 * http://www.torinocultura.it/rss/index.shtml
 *  
 * Eventi di Domani
 * http://www.torinocultura.it/servizionline/memento/rss.php?context=rss&action=rss&currDate=tomorrow&refProgetto=2
 * 
 * ToRSS - I feed RSS del sito della Citta'
 * http://www.comune.torino.it/torss/
 * 
 */
public class GetOnlineData {

    public static InputStream getInputStream(URL url) throws IOException {
        InputStream in = url.openStream();
        return in;
    }

    public static InputStream getInputStream(String url) throws MalformedURLException, IOException {
        System.out.println("String URL received");
        URL u = new URL(url);
        return getInputStream(u);
    }

    public static String getStringFromUrl(URL url) throws IOException {
        StringBuffer result = new StringBuffer();
        System.out.println("URL opened");
        InputStream in = getInputStream(url);
        String charset = "UTF-8";
        InputStreamReader isr = new InputStreamReader(in, charset);
        int c;
        while ((c = in.read()) != -1) result.append((char) c);
        String result_to_string = result.toString();
        return result_to_string;
    }

    public static String getStringFromUrl(String url) throws MalformedURLException, IOException {
        URL u = new URL(url);
        return getStringFromUrl(u);
    }

    public static Document stringToDocument(String xmlSource) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        StringReader string_reader = new StringReader(xmlSource);
        InputSource input_source = new InputSource(string_reader);
        Document document = builder.parse(input_source);
        return document;
    }

    public static Document retrieveDataAsDocument(String url) {
        try {
            System.out.println("Start RSS Parser");
            String data = getStringFromUrl(url);
            Document document = stringToDocument(data);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList rssNewsReader(String url) {
        try {
            System.out.println("Start RSS Data Parser");
            Document document = retrieveDataAsDocument(url);
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
            System.out.println("[0] - [" + node.getNodeName() + "]" + " with " + node_list.getLength() + " childs.");
            for (int i = 0; i < node_list.getLength(); i++) {
                a_node = node_list.item(i);
                if (a_node.getNodeType() == Node.ELEMENT_NODE) {
                    System.out.println("  [1] - [" + a_node.getNodeName() + "]" + " with " + a_node.getChildNodes().getLength() + " childs.");
                    inner_node_list = a_node.getChildNodes();
                    for (int j = 0; j < inner_node_list.getLength(); j++) {
                        an_inner_node = inner_node_list.item(j);
                        if ((!an_inner_node.getNodeName().equals("item")) && (!an_inner_node.getNodeName().equals("#text"))) {
                            System.out.println("	[2] " + an_inner_node.getNodeName() + ": " + an_inner_node.getTextContent());
                            if (an_inner_node.getNodeName().equals("title")) channel_title = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("link")) channel_link = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("description")) channel_description = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("copyright")) channel_copyright = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("pubDate")) channel_pubDate = an_inner_node.getTextContent();
                            if (an_inner_node.getNodeName().equals("category")) channel_category = an_inner_node.getTextContent();
                        }
                        if (an_inner_node.getNodeName().equals("item")) {
                            System.out.println("	[2] - [" + an_inner_node.getNodeName() + "]" + " with " + an_inner_node.getChildNodes().getLength() + " childs.");
                        }
                        if (an_inner_node.hasChildNodes() && (an_inner_node.getNodeName().equals("item"))) {
                            inner_inner_node_list = an_inner_node.getChildNodes();
                            for (int k = 0; k < inner_inner_node_list.getLength(); k++) {
                                an_inner_inner_node = inner_inner_node_list.item(k);
                                if (!(an_inner_inner_node.getTextContent().equals(""))) {
                                    if (!(an_inner_inner_node.getNodeName().equals("#text")) && !(an_inner_inner_node.getNodeName().equals("link"))) {
                                        System.out.println("		[3] " + an_inner_inner_node.getNodeName() + ": " + an_inner_inner_node.getTextContent());
                                        if (an_inner_inner_node.getNodeName().equals("title")) item_title = an_inner_inner_node.getTextContent();
                                        if (an_inner_inner_node.getNodeName().equals("pubDate")) item_pubDate = an_inner_inner_node.getTextContent();
                                        if (an_inner_inner_node.getNodeName().equals("description")) {
                                            if (an_inner_inner_node.getTextContent().length() >= 500) {
                                                String tmp_item_description = new String(an_inner_inner_node.getTextContent().substring(0, 400));
                                                item_description = new String(tmp_item_description.getBytes("UTF-8"), "UTF-8");
                                            } else {
                                                item_description = new String(an_inner_inner_node.getTextContent().getBytes("UTF-8"), "UTF-8");
                                            }
                                        }
                                    }
                                    if (an_inner_inner_node.getNodeName().equals("link")) {
                                        String tmp_link = an_inner_inner_node.getTextContent();
                                        System.out.println("		[3] " + an_inner_inner_node.getNodeName() + ": " + tmp_link);
                                        if (an_inner_inner_node.getNodeName().equals("link")) item_link = an_inner_inner_node.getTextContent();
                                        item_link = tmp_link;
                                    }
                                }
                            }
                            unit = "Unit data: [" + item_title + " - " + item_pubDate + " - " + item_description + " - " + item_link + "]";
                            rss_map.put(item_title, unit);
                            rss_unit = new RSS_data(item_title, item_link, "item_category", new String(item_description.getBytes("UTF-8"), "UTF-8"), new Double(lat), new Double(lng), date_format.parse(item_pubDate));
                            result.add(rss_unit);
                        }
                        System.out.println("--------------");
                    }
                }
                unit = " Info data: [" + channel_title + " " + channel_link + " " + channel_description + " " + channel_copyright + " " + channel_pubDate + " " + channel_category + " " + "]";
                rss_map.put("info", unit);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String argv[]) {
    }
}
