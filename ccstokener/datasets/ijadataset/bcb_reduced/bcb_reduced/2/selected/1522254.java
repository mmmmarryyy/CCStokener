package edu.ycp.android.bowlingconcepts;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HttpListener {

    private URL feedurl;

    private URLConnection conn;

    private HttpURLConnection httpConn;

    private LinkedList<ArrayList<String>> parsedXML;

    public HttpListener(URL url) {
        this.feedurl = url;
        parsedXML = new LinkedList<ArrayList<String>>();
    }

    public void readSite() {
        try {
            conn = feedurl.openConnection();
            httpConn = (HttpURLConnection) conn;
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConn.getInputStream();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();
                NodeList n1 = docEle.getElementsByTagName("item");
                if (n1 != null && n1.getLength() > 0) {
                    for (int i = 0; i < n1.getLength(); i++) {
                        Element entry = (Element) n1.item(i);
                        Element title = (Element) entry.getElementsByTagName("title").item(0);
                        Element link = (Element) entry.getElementsByTagName("link").item(0);
                        parsedXML.add(new ArrayList<String>());
                        parsedXML.getLast().add(title.getFirstChild().getNodeValue());
                        parsedXML.getLast().add(link.getFirstChild().getNodeValue());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public LinkedList<ArrayList<String>> getSiteText() {
        return parsedXML;
    }
}
