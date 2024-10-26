package com.NiPlayer.Parser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ReadAndPrintXMLFile {

    ArrayList<String> actualLineList = new ArrayList<String>();

    ArrayList<String> timeList = new ArrayList<String>();

    public ReadAndPrintXMLFile(String fileName) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(fileName));
            doc.getDocumentElement().normalize();
            System.out.println("Root element of the doc is " + doc.getDocumentElement().getNodeName());
            NodeList listOfPersons = doc.getElementsByTagName("sentence");
            int totalPersons = listOfPersons.getLength();
            System.out.println("Total no of sentence : " + totalPersons);
            for (int s = 0; s < listOfPersons.getLength(); s++) {
                Node firstPersonNode = listOfPersons.item(s);
                if (firstPersonNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstPersonElement = (Element) firstPersonNode;
                    NodeList firstNameList = firstPersonElement.getElementsByTagName("time");
                    Element firstNameElement = (Element) firstNameList.item(0);
                    NodeList textFNList = firstNameElement.getChildNodes();
                    String strTime = "" + ((Node) textFNList.item(0)).getNodeValue().trim();
                    System.out.println("Time value : " + strTime);
                    timeList.add(strTime);
                    NodeList lastNameList = firstPersonElement.getElementsByTagName("actualline");
                    Element lastNameElement = (Element) lastNameList.item(0);
                    NodeList textLNList = lastNameElement.getChildNodes();
                    String strActualLine = "" + ((Node) textLNList.item(0)).getNodeValue().trim();
                    System.out.println("Actual Line : " + strActualLine);
                    actualLineList.add(strActualLine);
                }
            }
        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());
        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public ReadAndPrintXMLFile(URL url) {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(url.openStream());
            doc.getDocumentElement().normalize();
            System.out.println("Root element of the doc is " + doc.getDocumentElement().getNodeName());
            NodeList listOfPersons = doc.getElementsByTagName("sentence");
            int totalPersons = listOfPersons.getLength();
            System.out.println("Total no of sentence : " + totalPersons);
            for (int s = 0; s < listOfPersons.getLength(); s++) {
                Node firstPersonNode = listOfPersons.item(s);
                if (firstPersonNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstPersonElement = (Element) firstPersonNode;
                    NodeList firstNameList = firstPersonElement.getElementsByTagName("time");
                    Element firstNameElement = (Element) firstNameList.item(0);
                    NodeList textFNList = firstNameElement.getChildNodes();
                    String strTime = "" + ((Node) textFNList.item(0)).getNodeValue().trim();
                    System.out.println("Time value : " + strTime);
                    timeList.add(strTime);
                    NodeList lastNameList = firstPersonElement.getElementsByTagName("actualline");
                    Element lastNameElement = (Element) lastNameList.item(0);
                    NodeList textLNList = lastNameElement.getChildNodes();
                    String strActualLine = "" + ((Node) textLNList.item(0)).getNodeValue().trim();
                    System.out.println("Actual Line : " + strActualLine);
                    actualLineList.add(strActualLine);
                }
            }
        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());
        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public synchronized ArrayList<String> getActualLineList() {
        return actualLineList;
    }

    public synchronized ArrayList<String> getTimeList() {
        return timeList;
    }
}
