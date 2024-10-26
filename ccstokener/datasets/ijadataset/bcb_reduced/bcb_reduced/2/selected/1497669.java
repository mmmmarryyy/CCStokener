package servicesHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.io.StringReader;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CategoryListHandler {

    private HashMap<String, Integer> categories = new HashMap<String, Integer>();

    public CategoryListHandler(int langId) {
        try {
            URL url = new URL("http://eiffel.itba.edu.ar/hci/service/Catalog.groovy?method=GetCategoryList&language_id=" + langId);
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
            NodeList nl = dom.getElementsByTagName("category");
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                String id = e.getAttribute("id");
                NodeList name = e.getElementsByTagName("name");
                Element line = (Element) name.item(0);
                categories.put(getCharacterDataFromElement(line), Integer.valueOf(id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getCategories() {
        return categories;
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
