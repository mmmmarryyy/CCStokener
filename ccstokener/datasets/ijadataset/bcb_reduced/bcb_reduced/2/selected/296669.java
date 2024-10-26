package com.skillworld.webapp.web.test.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TestPost {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            testTranslateTutorial();
        } catch (UnsupportedEncodingException e) {
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getXml(String lang) {
        String xml = "<tutorial>" + "<locale>" + lang + "</locale>" + "<title>Teste1</title>" + "<abstract></abstract>" + "<description><![CDATA[<TEXTFORMAT LEADING=\"2\"><P ALIGN=\"LEFT\"><FONT FACE=\"Arial\" SIZE=\"12\" COLOR=\"#000000\" LETTERSPACING=\"0\" KERNING=\"0\"><B>Teste</B></FONT></P></TEXTFORMAT>]]></description>" + "<difficulty>EASY</difficulty>" + "<duration>200000</duration>" + "<urlfile></urlfile>" + "<photos>" + "<url>" + "http://a1.sphotos.ak.fbcdn.net/hphotos-ak-snc7/s720x720/394825_144181495693031_100003035446901_195371_950015822_n.jpg" + "</url>" + "</photos>" + "<video></video>" + "</tutorial>";
        return xml;
    }

    public static void testCreateTutorial() throws UnsupportedEncodingException, MalformedURLException, IOException {
        String cont = getXml("en");
        String data = URLEncoder.encode("dept", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
        data += "&" + URLEncoder.encode("doc", "UTF-8") + "=" + URLEncoder.encode(cont, "UTF-8");
        URL url = new URL("http://localhost:9090/skillworld/rest/tutorial/CreateTutorial?" + data);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        System.out.println(conn.toString());
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        wr.close();
        rd.close();
    }

    public static void testTranslateTutorial() throws UnsupportedEncodingException, MalformedURLException, IOException {
        String cont = getXml("gl");
        String data = URLEncoder.encode("tut", "UTF-8") + "=" + URLEncoder.encode("2", "UTF-8");
        data += "&" + URLEncoder.encode("doc", "UTF-8") + "=" + URLEncoder.encode(cont, "UTF-8");
        URL url = new URL("http://localhost:9090/skillworld/rest/tutorial/TranslateTutorial?" + data);
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        System.out.println(conn.toString());
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        }
        wr.close();
        rd.close();
    }

    public static Element getDocumentElement(File fileName) throws Exception {
        try {
            Document doc = getDocumentBuilder().parse(fileName);
            return doc.getDocumentElement();
        } catch (Exception se) {
            return null;
        }
    }

    /**
   * Returns a default DocumentBuilder instance or throws an
   * ExceptionInInitializerError if it can't be created.
   *
   * @return a default DocumentBuilder instance.
   */
    public static DocumentBuilder getDocumentBuilder() throws Exception {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setCoalescing(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setValidating(false);
            return dbf.newDocumentBuilder();
        } catch (Exception exc) {
            throw new Exception(exc.getMessage());
        }
    }

    public static Document newDocumentFromInputStream(InputStream in) {
        DocumentBuilderFactory factory = null;
        DocumentBuilder builder = null;
        Document ret = null;
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            ret = builder.parse(new InputSource(in));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
