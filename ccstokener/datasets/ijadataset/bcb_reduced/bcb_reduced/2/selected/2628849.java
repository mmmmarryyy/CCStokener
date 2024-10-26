package servicesHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import utils.ProductFullBook;

public class BookDetailHandler {

    public ProductFullBook product;

    public BookDetailHandler(String id) throws Exception {
        try {
            product = new ProductFullBook();
            URL url = new URL("http://eiffel.itba.edu.ar/hci/service/Catalog.groovy?method=GetProduct&product_id=" + id);
            URLConnection urlc = url.openConnection();
            urlc.setDoOutput(false);
            urlc.setAllowUserInteraction(false);
            BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String str;
            StringBuffer sb = new StringBuffer();
            while ((str = br.readLine()) != null) {
                sb.append(str);
                sb.append("\n");
            }
            br.close();
            String response = sb.toString();
            if (response == null) {
                return;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document dom = db.parse(is);
            NodeList nl = dom.getElementsByTagName("product");
            Element e = (Element) nl.item(0);
            NodeList name = e.getElementsByTagName("name");
            Element line = (Element) name.item(0);
            product.name = getCharacterDataFromElement(line);
            e = (Element) nl.item(0);
            name = e.getElementsByTagName("sales_rank");
            line = (Element) name.item(0);
            product.rank = getCharacterDataFromElement(line);
            e = (Element) nl.item(0);
            name = e.getElementsByTagName("price");
            line = (Element) name.item(0);
            product.price = getCharacterDataFromElement(line);
            name = e.getElementsByTagName("image_url");
            line = (Element) name.item(0);
            product.url = getCharacterDataFromElement(line);
            name = e.getElementsByTagName("authors");
            line = (Element) name.item(0);
            product.authors = getCharacterDataFromElement(line);
            e = (Element) nl.item(0);
            name = e.getElementsByTagName("publisher");
            line = (Element) name.item(0);
            product.publisher = getCharacterDataFromElement(line);
            e = (Element) nl.item(0);
            name = e.getElementsByTagName("published_date");
            line = (Element) name.item(0);
            product.publish_date = getCharacterDataFromElement(line);
            e = (Element) nl.item(0);
            name = e.getElementsByTagName("language");
            line = (Element) name.item(0);
            product.language = getCharacterDataFromElement(line);
        } catch (Exception e) {
            throw e;
        }
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "?";
    }
}
