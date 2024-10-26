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

public class Finder {

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

    public static Document getLocation(String url) {
        try {
            return retrieveDataAsDocument(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String argv[]) {
        System.out.println(getLocation("http://www.torinocultura.it/eventi/index.shtml"));
    }
}
