package se.waltersson.wowarmory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.waltersson.wowarmory.page.CharacterSheet;
import se.waltersson.wowarmory.page.Reputation;

/**
 * 
 * @author Joakim Waltersson
 * @version $Revision: 15 $ $Date: 2010-08-30 14:26:42 -0400 (Mon, 30 Aug 2010) $
 */
public class ArmoryReader {

    private static final String euArmory = "http://eu.wowarmory.com/";

    private String baseUrl = euArmory;

    static final String charPage = "";

    public ArmoryReader() {
    }

    public void setArmoryEU() {
        baseUrl = euArmory;
    }

    public Reputation getReputation(String realm, String charName) {
        Reputation r = new Reputation(realm, charName);
        try {
            r.parse(getDocument(r.getQuery()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    public InputStream getCharacterPageStream(String realm, String charName) throws MalformedURLException, IOException {
        CharacterSheet cs = new CharacterSheet(realm, charName);
        return getPageStream(cs.getQuery());
    }

    public CharacterSheet getCharacterSheet(String realm, String charName) {
        CharacterSheet cs = new CharacterSheet(realm, charName);
        try {
            cs.parse(getDocument(cs.getQuery()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cs;
    }

    private Document getDocument(String query) throws IOException, SAXException, ParserConfigurationException {
        InputStream bis = getPageStream(query);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        builder = factory.newDocumentBuilder();
        Document doc = builder.parse(bis);
        return doc;
    }

    private InputStream getPageStream(String query) throws MalformedURLException, IOException {
        URL url = new URL(baseUrl + query + "&rhtml=no");
        URLConnection connection = url.openConnection();
        connection.connect();
        InputStream in = connection.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(in);
        return bis;
    }
}
