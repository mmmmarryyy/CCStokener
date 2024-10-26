package servicesHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RegisterHandler {

    @SuppressWarnings("deprecation")
    public static int UseRegister(String username, String name, String password, String email, String birthDate) {
        try {
            URL url = new URL("http://eiffel.itba.edu.ar/hci/service/Security.groovy?method=CreateAccount");
            String ret = "<account>" + "<username>" + username + "</username>" + "<name>" + name + "</name>" + "<password>" + password + "</password>" + "<email>" + email + "</email>" + "<birth_date>" + birthDate + "</birth_date>" + "</account>";
            URLConnection urlc = url.openConnection();
            urlc.setDoOutput(true);
            urlc.setAllowUserInteraction(false);
            String encodedPost = URLEncoder.encode(ret);
            PrintStream ps = new PrintStream(urlc.getOutputStream());
            ps.print("account=" + encodedPost);
            ps.close();
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
                return -1;
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(response));
            Document dom = db.parse(is);
            NodeList nl = dom.getElementsByTagName("response");
            String status = ((Element) nl.item(0)).getAttributes().item(0).getTextContent();
            if (status.equalsIgnoreCase("ok")) {
                return 0;
            } else {
                nl = dom.getElementsByTagName("error");
                String code = ((Element) nl.item(0)).getAttributes().item(0).getTextContent();
                if (code.toString().equals("201")) {
                    return 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
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
