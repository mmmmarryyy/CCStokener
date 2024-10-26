package de.planethold.sevenloadj;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import de.planethold.sevenloadj.Item.TYPE;

public class Sevenload {

    private final String USERNAME;

    private final String PASSWORD;

    private final String ENDPOINT;

    private Token token = new Token();

    private final boolean debug = true;

    public Sevenload(String username, String password) {
        this.USERNAME = username;
        this.PASSWORD = password;
        this.ENDPOINT = "http://api.sevenload.com/rest/1.0";
    }

    public Sevenload(String username, String password, String endpoint) {
        this.USERNAME = username;
        this.PASSWORD = password;
        this.ENDPOINT = endpoint;
    }

    private Element makeRequest(String link) {
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            InputStream in = conn.getInputStream();
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(in);
            Element element = document.getDocumentElement();
            element.normalize();
            if (checkRootTag(element)) {
                return element;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkRootTag(Element element) {
        if (!element.getNodeName().equals("sl:sevenload-api")) {
            System.out.println("checkRootTag: NodeName should: sl-sevenload-api is:" + element.getNodeName());
            return false;
        }
        if (!element.getAttribute("major-version").equals("1")) {
            System.out.println("checkRootTag: major-version should: 1 is: " + element.getAttribute("major-version"));
            return false;
        }
        if (!element.getAttribute("minor-version").equals("0")) {
            System.out.println("checkRootTag: minor-version should: 0 is: " + element.getAttribute("minor-version"));
            return false;
        }
        if (!element.getAttribute("response").equals("success")) {
            System.out.println("checkRootTag: should: success is: " + element.getAttribute("response"));
            return false;
        }
        return true;
    }

    /**
	 * Creating a Token
	 *
	 */
    public void tokenCreate() {
        String link = ENDPOINT + "/token/create?username=" + USERNAME + "&password=" + PASSWORD;
        if (debug) System.out.println(link);
        token = new Token(makeRequest(link));
    }

    /**
	 *  Get an Item
	 * @param id
	 * @return
	 */
    public Item getItem(String id) {
        if (token.isValid()) {
            String link = ENDPOINT + "/item/" + id + "?username=" + USERNAME + "&token-id=" + token.getId();
            if (debug) System.out.println(link);
            return new Item((Element) makeRequest(link).getElementsByTagName("item").item(0));
        } else {
            System.out.println("Token not valid");
            return null;
        }
    }

    public Album getAlbum(String id) {
        if (token.isValid()) {
            String link = ENDPOINT + "/album/" + id + "/items?username=" + USERNAME + "&token-id=" + token.getId();
            if (debug) System.out.println(link);
            return new Album(makeRequest(link));
        } else {
            System.out.println("Token not valid");
            return null;
        }
    }

    public Token getToken() {
        return token;
    }
}
