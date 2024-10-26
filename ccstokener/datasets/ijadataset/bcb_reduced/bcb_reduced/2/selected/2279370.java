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

public class LoginHandler {

    public ArrayList<String> datos = new ArrayList<String>();

    public LoginHandler(String username, String password) {
        try {
            URL url = new URL("http://eiffel.itba.edu.ar/hci/service/Security.groovy?method=SignIn&username=" + username + "&password=" + password);
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
                datos = null;
                return;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document dom = db.parse(is);
            NodeList nl = dom.getElementsByTagName("response");
            String status = ((Element) nl.item(0)).getAttributes().item(0).getTextContent();
            if (status.toString().equals("fail")) {
                nl = dom.getElementsByTagName("error");
                String code = ((Element) nl.item(0)).getAttributes().item(0).getTextContent();
                if (code.toString().equals("104")) {
                    datos.add("-1");
                }
                return;
            }
            nl = dom.getElementsByTagName("token");
            Element line = (Element) nl.item(0);
            datos.add(getCharacterDataFromElement(line));
            nl = dom.getElementsByTagName("user");
            datos.add(nl.item(0).getAttributes().getNamedItem("id").getNodeValue());
            datos.add(nl.item(0).getAttributes().getNamedItem("username").getNodeValue());
            datos.add(nl.item(0).getAttributes().getNamedItem("name").getNodeValue());
            datos.add(nl.item(0).getAttributes().getNamedItem("last_login_date").getNodeValue());
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        datos = null;
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
