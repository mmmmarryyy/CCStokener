package com.digg55.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/***

* @author hwp

* 6/11/2010 16:58

* DomXMLString.java

* 概述：纯java方式访问远程WebService接口返回的xml格式的数据保存在本地

*/
public class DomXMLString {

    private static String SERVICES_HOST = "www.webxml.com.cn";

    private static String NETDATA_URL = "http://webservice.webxml.com.cn/WebServices/WeatherWS.asmx/getRegionProvince";

    private static String LOCAL_PC_SAVEFILE_URL = "c:\\netDataToLocalFile.xml";

    private DomXMLString() {
    }

    public static void main(String[] args) throws Exception {
        Document document = getProvinceCode(NETDATA_URL);
        helloOK(document, LOCAL_PC_SAVEFILE_URL);
    }

    public static Document getProvinceCode(String netXMLDataURL) {
        Document document = null;
        DocumentBuilderFactory documentBF = DocumentBuilderFactory.newInstance();
        documentBF.setNamespaceAware(true);
        try {
            DocumentBuilder documentB = documentBF.newDocumentBuilder();
            InputStream inputStream = getSoapInputStream(netXMLDataURL);
            document = documentB.parse(inputStream);
            inputStream.close();
        } catch (DOMException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return document;
    }

    public static InputStream getSoapInputStream(String url) {
        InputStream inputStream = null;
        try {
            URL urlObj = new URL(url);
            URLConnection urlConn = urlObj.openConnection();
            urlConn.setRequestProperty("Host", SERVICES_HOST);
            urlConn.connect();
            inputStream = urlConn.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    public static void helloOK(Document document, String savaFileURL) {
        TransformerFactory transF = TransformerFactory.newInstance();
        try {
            Transformer transformer = transF.newTransformer();
            DOMSource source = new DOMSource(document);
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "YES");
            PrintWriter pw = new PrintWriter(new FileOutputStream(savaFileURL));
            StreamResult result = new StreamResult(pw);
            transformer.transform(source, result);
            System.out.println("生成xml文件成功!");
        } catch (TransformerConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (TransformerException e) {
            System.out.println(e.getMessage());
        }
    }
}
