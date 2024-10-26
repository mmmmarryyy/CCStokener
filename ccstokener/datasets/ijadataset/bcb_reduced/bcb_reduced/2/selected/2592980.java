package com.abich.eve.evecalc.alloys;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.sun.org.apache.xerces.internal.impl.xs.opti.DefaultDocument;

public class Items {

    private static Items instance;

    private static final String HOURS = "168";

    public static final String PLUSH = "11725";

    public static final String CONDENSED = "11739";

    public static final String DARKCOMP = "11735";

    public static final String GLOSSY = "11724";

    public static final String LUSTERING = "11736";

    public static final String OPULENT = "11734";

    public static final String PRECIOUS = "11737";

    public static final String CRYSTAL = "11741";

    public static final String GLEAMING = "11740";

    public static final String LUCENT = "11738";

    public static final String MOTLEY = "11733";

    public static final String SHEEN = "11732";

    public static final String TRITANIUM = "34";

    public static final String ISOGEN = "37";

    public static final String MEGACYTE = "40";

    public static final String MEXCALLON = "36";

    public static final String MORPHITE = "11399";

    public static final String NOCXIUM = "38";

    public static final String PYERITE = "35";

    public static final String ZYDRINE = "39";

    HashMap<String, String> items = new HashMap<String, String>();

    private HashMap<String, String> reverseItems = new HashMap<String, String>();

    HashMap<String, PriceList> pricelists = new HashMap<String, PriceList>();

    private Items() {
        items.put("Condensed Alloy", CONDENSED);
        items.put("Dark Compound", DARKCOMP);
        items.put("Glossy Compound", GLOSSY);
        items.put("Lustering Alloy", LUSTERING);
        items.put("Opulent Compound", OPULENT);
        items.put("Precious Alloy", PRECIOUS);
        items.put("Crystal Compound", CRYSTAL);
        items.put("Gleaming Alloy", GLEAMING);
        items.put("Lucent Compound", LUCENT);
        items.put("Motley Compound", MOTLEY);
        items.put("Plush Compound", PLUSH);
        items.put("Sheen Compound", SHEEN);
        items.put("Tritanium", TRITANIUM);
        items.put("Isogen", ISOGEN);
        items.put("Megacyte", MEGACYTE);
        items.put("Mexcallon", MEXCALLON);
        items.put("Morphite", MORPHITE);
        items.put("Nocxium", NOCXIUM);
        items.put("Pyerite", PYERITE);
        items.put("Zydrine", ZYDRINE);
        for (Iterator<String> iter = items.keySet().iterator(); iter.hasNext(); ) {
            String item = iter.next();
            reverseItems.put(items.get(item), item);
        }
    }

    public static Items getInstance() {
        String running = "";
        synchronized (running) {
            if (instance == null) {
                instance = new Items();
            }
        }
        return instance;
    }

    public void loadRegionPrices(Region region) {
        synchronized (pricelists) {
            if (!pricelists.containsKey(region.getName())) {
                System.out.println(pricelists.keySet().toString());
                PriceList pricelist = new PriceList(region.getName());
                Document doc = getXMLDoc(region);
                XPathFactory factory = XPathFactory.newInstance();
                XPath xpath = factory.newXPath();
                NodeList nodes = null;
                try {
                    XPathExpression expr = xpath.compile("//type/sell/min/text()");
                    Object xpathResult = expr.evaluate(doc, XPathConstants.NODESET);
                    nodes = (NodeList) xpathResult;
                } catch (XPathExpressionException e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    Element parentNode = (Element) node.getParentNode().getParentNode().getParentNode();
                    String typeID = parentNode.getAttribute("id");
                    String buyMaxPriceString = "";
                    try {
                        String xpathquery = "//type[@id='" + typeID + "']/buy/max";
                        XPathExpression expr = xpath.compile(xpathquery);
                        Object xpathResult = expr.evaluate(doc, XPathConstants.STRING);
                        buyMaxPriceString = (String) xpathResult;
                    } catch (XPathExpressionException e) {
                        e.printStackTrace();
                    }
                    String sellMinPriceString = node.getNodeValue();
                    Double sellMinPrice = new Double(0);
                    try {
                        sellMinPrice = new Double(sellMinPriceString);
                    } catch (NumberFormatException e) {
                        sellMinPrice = new Double(0);
                    }
                    Double buyMaxPrice;
                    try {
                        buyMaxPrice = new Double(buyMaxPriceString);
                    } catch (NumberFormatException e) {
                        buyMaxPrice = new Double(0);
                    }
                    if (sellMinPrice.longValue() == 0) {
                        if (buyMaxPrice == 0) {
                            sellMinPrice = new Double(0);
                        } else {
                            sellMinPrice = buyMaxPrice;
                        }
                    }
                    pricelist.put(typeID, sellMinPrice);
                    System.out.println(reverseItems.get(typeID) + ": " + typeID + ": " + sellMinPrice);
                }
                System.out.println(pricelist.toString());
                pricelists.put(region.getName(), pricelist);
            }
        }
    }

    /**
	 * @param region
	 * @param alloys.getTypes
	 * @return
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
    private Document getXMLDoc(Region region) {
        Document doc;
        try {
            InputStream stream;
            URL url = new URL("http://eve-central.com/api/marketstat?hours=" + HOURS + "&" + getTypes() + "&regionlimit=" + region.getTypeID());
            System.out.println(url.toString());
            stream = url.openStream();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            doc = parser.parse(stream);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            doc = new DefaultDocument();
        } catch (SAXException e) {
            e.printStackTrace();
            doc = new DefaultDocument();
        } catch (IOException e) {
            e.printStackTrace();
            doc = new DefaultDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            doc = new DefaultDocument();
        }
        return doc;
    }

    public String getTypes() {
        String queryString = "";
        Collection<String> ids = items.values();
        for (Iterator iter = ids.iterator(); iter.hasNext(); ) {
            String element = (String) iter.next();
            queryString += "&typeid=" + element;
        }
        return queryString.substring(1);
    }

    public PriceList getPriceList(Region region) {
        return pricelists.get(region.getName());
    }
}
