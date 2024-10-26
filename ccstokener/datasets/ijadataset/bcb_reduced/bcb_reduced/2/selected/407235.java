package vrm.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import vrm.core.Player;

public class RugbyManiaPlayersXMLParser {

    private static final String HTML_AND = "&amp;";

    private static final String[] HTML_PUNCTUATION = { "&Agrave;", "&Aacute;", "&Acirc;", "&Atilde;", "&Auml;", "&Aring;", "&AElig;", "&Ccedil;", "&Egrave;", "&Eacute;", "&Ecirc;", "&Euml;", "&Igrave;", "&Iacute;", "&Icirc;", "&Iuml;", "&ETH;", "&Ntilde;", "&Ograve;", "&Oacute;", "&Ocirc;", "&Otilde;", "&Ouml;", "&times;", "&Oslash;", "&Ugrave;", "&Uacute;", "&Ucirc;", "&Uuml;", "&Yacute;", "&THORN;", "&szlig" };

    private static final String[] HTML_PUNCTUATION_SUBS = { "A", "A", "A", "A", "A", "A", "AE", "C", "E", "E", "E", "E", "I", "I", "I", "I", "D", "N", "O", "O", "O", "O", "O", "X", "O", "U", "U", "U", "U", "Y", "P", "B" };

    private Document doc;

    public RugbyManiaPlayersXMLParser(String id, String key) throws SAXException, IOException, ParserConfigurationException {
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.getRMPlayersXMLInputStream(id, key));
    }

    public RugbyManiaPlayersXMLParser(InputStream input) throws SAXException, IOException, ParserConfigurationException {
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
    }

    public RugbyManiaPlayersXMLParser(File xmlFile) throws SAXException, IOException, ParserConfigurationException {
        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
    }

    private String cleanString(String playerName) {
        playerName = playerName.replaceAll(HTML_AND, "&");
        int pos = 0;
        for (String punct : HTML_PUNCTUATION) {
            if (playerName.contains(punct)) {
                playerName = playerName.replaceAll(punct, HTML_PUNCTUATION_SUBS[pos]);
            } else if (playerName.contains(punct.toLowerCase())) {
                playerName = playerName.replaceAll(punct.toLowerCase(), HTML_PUNCTUATION_SUBS[pos].toLowerCase());
            }
            pos++;
        }
        return playerName;
    }

    public ArrayList<Player> getPlayerList() {
        ArrayList<Player> players = new ArrayList<Player>();
        NodeList nl = doc.getElementsByTagName("ROW");
        for (int i = 0; i < nl.getLength(); i++) {
            Node player = nl.item(i);
            NodeList attributes = player.getChildNodes();
            HashMap<RugbyManiaPlayersXMLTags, String> xmlPlayer = new HashMap<RugbyManiaPlayersXMLTags, String>();
            for (int k = 0; k < attributes.getLength(); k++) {
                Node attribute = attributes.item(k);
                String nodeName = attribute.getNodeName().trim();
                String textContent = attribute.getTextContent().trim();
                if (nodeName.toLowerCase().equals(RugbyManiaPlayersXMLTags.nom.toString().toLowerCase())) textContent = cleanString(textContent);
                if (nodeName.toLowerCase().equals(RugbyManiaPlayersXMLTags.prenom.toString().toLowerCase())) textContent = cleanString(textContent);
                if (nodeName.toLowerCase().equals(RugbyManiaPlayersXMLTags.country.toString().toLowerCase())) textContent = cleanString(textContent);
                xmlPlayer.put(RugbyManiaPlayersXMLTags.valueOf(nodeName), textContent);
            }
            players.add(new Player(xmlPlayer));
        }
        return players;
    }

    private InputStream getRMPlayersXMLInputStream(String id, String key) throws IOException {
        String sid = ResourceBundleManager.getString(ResourceBundleManager.PROPS, "ID");
        String skey = ResourceBundleManager.getString(ResourceBundleManager.PROPS, "KEY");
        String rmpurl = ResourceBundleManager.getString(ResourceBundleManager.PROPS, "PLAYERS");
        rmpurl = rmpurl.replaceAll(sid, id).replaceAll(skey, key);
        URL url = new URL(rmpurl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setReadTimeout(10000);
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = rd.readLine()) != null) {
            sb.append(line + '\n');
        }
        rd.close();
        byte[] bytes = sb.toString().getBytes("UTF-8");
        InputStream input = new ByteArrayInputStream(bytes);
        return input;
    }
}
