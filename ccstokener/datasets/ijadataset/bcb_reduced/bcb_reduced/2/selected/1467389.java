package servicesHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import utils.ProductShort;

public class ProductListBySubCatHandler {

    public ArrayList<ProductShort> list = new ArrayList<ProductShort>();

    public ProductListBySubCatHandler(int cathegory, int subcategory, int langId) {
        try {
            URL url = new URL("http://eiffel.itba.edu.ar/hci/service/Catalog.groovy?method=GetProductListBySubcategory&category_id=" + cathegory + "&subcategory_id=" + subcategory + "&language_id=" + langId);
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
            for (int i = 0; i < nl.getLength(); i++) {
                Element nodes = (Element) nl.item(i);
                String id = nodes.getAttribute("id").toString();
                NodeList name = nodes.getElementsByTagName("name");
                NodeList rank2 = nodes.getElementsByTagName("sales_rank");
                NodeList price = nodes.getElementsByTagName("price");
                NodeList url2 = nodes.getElementsByTagName("image_url");
                String nameS = getCharacterDataFromElement((Element) name.item(0));
                String rank2S = getCharacterDataFromElement((Element) rank2.item(0));
                String priceS = getCharacterDataFromElement((Element) price.item(0));
                String url2S = getCharacterDataFromElement((Element) url2.item(0));
                list.add(new ProductShort(id, nameS, rank2S, priceS, url2S));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
