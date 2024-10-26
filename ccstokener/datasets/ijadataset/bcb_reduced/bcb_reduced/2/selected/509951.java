package backend.reos.eoffice.rodiks;

import java.io.*;
import java.lang.annotation.Documented;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.XMLFormatter;
import java.util.prefs.NodeChangeEvent;
import org.xml.sax.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.*;

public class ReOSExchangeRates implements Runnable {

    protected Thread ThreadExThread;

    private static final String BASE_URL = "http://www.nationalbank.kz/rss/rates_all.xml";

    private static Document doc;

    public ReOSExchangeRates() {
        ThreadExThread = new Thread(this, "ReOSCurrancyThread");
        ThreadExThread.start();
    }

    public void run() {
        while (true) {
            try {
                getRemoteContent();
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                ReOSApplication.log.Write(e);
            }
        }
    }

    private static void getRemoteContent() {
        try {
            ReOSApplication.log.Write("Trying get exchange currancy rate...");
            URL url = new URL(BASE_URL);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            Object object = huc.getContent();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse((InputStream) object);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("item");
            String title, pubDate, desc, quant, index;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node fstNode = nodeList.item(i);
                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element) fstNode;
                    NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("title");
                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    title = ((Node) fstNm.item(0)).getNodeValue();
                    NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("pubDate");
                    Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
                    NodeList lstNm = lstNmElmnt.getChildNodes();
                    pubDate = ((Node) lstNm.item(0)).getNodeValue();
                    lstNmElmntLst = fstElmnt.getElementsByTagName("description");
                    lstNmElmnt = (Element) lstNmElmntLst.item(0);
                    lstNm = lstNmElmnt.getChildNodes();
                    desc = ((Node) lstNm.item(0)).getNodeValue();
                    lstNmElmntLst = fstElmnt.getElementsByTagName("quant");
                    lstNmElmnt = (Element) lstNmElmntLst.item(0);
                    lstNm = lstNmElmnt.getChildNodes();
                    quant = ((Node) lstNm.item(0)).getNodeValue();
                    lstNmElmntLst = fstElmnt.getElementsByTagName("index");
                    lstNmElmnt = (Element) lstNmElmntLst.item(0);
                    lstNm = lstNmElmnt.getChildNodes();
                    index = ((Node) lstNm.item(0)).getNodeValue();
                    ReOSApplication.db.StoreCurrencyData(title, pubDate, desc, quant, index);
                    fstNmElmntLst = null;
                    fstNmElmnt = null;
                    fstNm = null;
                    lstNmElmntLst = null;
                    lstNmElmnt = null;
                    lstNm = null;
                    huc = null;
                    url = null;
                    doc = null;
                    object = null;
                    db = null;
                    dbf = null;
                }
            }
        } catch (UnknownHostException e) {
            ReOSApplication.log.Write(e);
        } catch (IOException e) {
            ReOSApplication.log.Write(e);
        } catch (Exception e) {
            ReOSApplication.log.Write(e);
        }
    }
}
