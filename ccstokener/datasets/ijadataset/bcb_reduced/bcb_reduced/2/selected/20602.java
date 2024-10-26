package nl.multimedian.eculture.ak;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nl.multimedian.eculture.annocultor.xconverter.api.Environment;
import nl.multimedian.eculture.annocultor.xconverter.api.Environment.PARAMETERS;
import nl.multimedian.eculture.annocultor.xconverter.impl.XmlElementForVelocity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * 
 * @author Borys Omelayenko
 *
 */
public class Scraper {

    public static void main(String... params) throws Exception {
        Environment environment = new Environment();
        File scrapedXmlFile = new File(environment.getParameter(PARAMETERS.inputDir), "ak.xml");
        if (!scrapedXmlFile.exists()) {
            Document trgDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element trgElement = trgDoc.createElement("records");
            trgDoc.appendChild(trgElement);
            DOMParser p = new DOMParser();
            p.parse(new InputSource(new URL("http://www.aziatischekeramiek.nl/get?site=ak&id=i000077&types=object").openStream()));
            Map<String, String> namespaces = new HashMap<String, String>();
            namespaces.put("ak", "http://www.sitemaps.org/schemas/sitemap/0.9");
            XmlElementForVelocity root = new XmlElementForVelocity(p.getDocument().getDocumentElement(), namespaces);
            XmlElementForVelocity[] links = root.getChildren("url");
            for (int i = 0; i < links.length; i++) {
                String url = links[i].getFirstChild("loc").getValue();
                String id = url.substring(url.lastIndexOf("&") + "&id=".length());
                url += "&version=xml";
                System.out.println(url);
                DOMParser work = new DOMParser();
                work.parse(new InputSource(new URL(url).openStream()));
                Node workNode = trgDoc.importNode(work.getDocument().getDocumentElement(), true);
                Element workId = trgDoc.createElement("id");
                workId.appendChild(trgDoc.createTextNode(id));
                workNode.appendChild(workId);
                trgElement.appendChild(workNode);
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(scrapedXmlFile);
            DOMSource source = new DOMSource(trgDoc);
            transformer.transform(source, result);
        }
    }
}
