package CurrencyConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Agellous
 */
public class XMLParser {

    private static Document dom;

    private static ArrayList currencies;

    public static void FindXML() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            String url = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.parse(new URL(url).openStream());
        } catch (ParserConfigurationException pce) {
        } catch (SAXException se) {
        } catch (IOException ioe) {
        }
        parseCurrencies();
    }

    private static ArrayList parseCurrencies() {
        Element docEle = dom.getDocumentElement();
        Element el = null;
        NodeList nl = docEle.getElementsByTagName("Cube");
        currencies = new ArrayList();
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                el = (Element) nl.item(i);
                if (el.hasAttribute("currency")) {
                    currencies.add(el);
                }
            }
        }
        el = dom.createElement("Cube");
        el.setAttribute("currency", "EUR");
        el.setAttribute("rate", "1");
        currencies.add(0, el);
        return currencies;
    }

    public static String getDate() {
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName("Cube");
        Element el = (Element) nl.item(1);
        return (el.getAttribute("time"));
    }

    public static ArrayList getCurrencies() {
        return currencies;
    }
}
