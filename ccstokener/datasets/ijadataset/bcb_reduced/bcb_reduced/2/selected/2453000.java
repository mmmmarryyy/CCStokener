package org.cyberaide.info;

import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.List;
import java.util.LinkedList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.cyberaide.core.CoGObject;
import org.cyberaide.core.CoGObjectsUtil;

public class InfoService {

    private CoGObject[] allService = null;

    private static String webmdsEndpoint;

    private static String strQueryLeft;

    private static String strQueryRight;

    private static String tgOutagesUrl;

    static {
        try {
            List<CoGObject> urls = CoGObjectsUtil.readObject("teragrid.infoservice.url", "ascii");
            webmdsEndpoint = urls.get(0).get("webmdsendpoint");
            strQueryLeft = urls.get(0).get("strqueryleft");
            strQueryRight = urls.get(0).get("strqueryright");
            tgOutagesUrl = urls.get(0).get("tgoutageurl");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * return the array containing info fetched from teragrid
     */
    public CoGObject[] getServices(String serviceType) {
        allService = null;
        try {
            String strQueryService = "//Service[Type='" + serviceType + "']";
            String strQuery = webmdsEndpoint + strQueryLeft + strQueryRight + strQueryService;
            URL url = new URL(strQuery);
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer content = new StringBuffer();
            String str;
            while ((str = in.readLine()) != null) {
                content.append(str);
                content.append("\n");
            }
            in.close();
            toCoGObject(new String(content));
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return allService;
    }

    /**
     * parse the returned xml raw data and store the info into array
     */
    private String toCoGObject(String content) {
        Document origDoc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(true);
            origDoc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes()));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        Node root = origDoc.getFirstChild();
        NodeList nodes = root.getChildNodes();
        int numServices = 0;
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node aNode = nodes.item(i);
            if (aNode.getNodeName().equals("Service")) {
                ++numServices;
            }
        }
        allService = new CoGObject[numServices];
        for (int i = 0; i < nodes.getLength(); ++i) {
            Node aNode = nodes.item(i);
            if (aNode.getNodeName().equals("Service")) {
                NodeList info = aNode.getChildNodes();
                allService[i] = new CoGObject();
                for (int j = 0; j < info.getLength(); ++j) {
                    String key = info.item(j).getNodeName();
                    try {
                        if (!key.equals("#text")) {
                            String value = info.item(j).getFirstChild().getNodeValue();
                            allService[i].set(key, value);
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * get tg-outages information from TeraGrid Information Services
     */
    public static List<String> getOutageSites() {
        List<String> outageSites = new LinkedList<String>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(tgOutagesUrl);
            NodeList nlist = doc.getElementsByTagName("ResourceID");
            for (int i = 0; i < nlist.getLength(); i++) {
                outageSites.add(nlist.item(i).getTextContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outageSites;
    }
}
