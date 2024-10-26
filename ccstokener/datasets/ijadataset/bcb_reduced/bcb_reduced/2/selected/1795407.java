package de.lastfm.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
import de.lastfm.db.DB_Neighbours;
import de.lastfm.gui.Gui;

/**
 * @author User
 *
 */
public class Neighbours {

    static void getNeighbourDetails(String username) {
        try {
            System.out.println("getNeighbourDetails");
            Gui.getBalken().setValue(50);
            int a = Gui.getBalken().getValue();
            a++;
            System.out.println("BaLKEN HAT WERT : " + a);
            Gui.getBalken().setValue(a);
            Gui.getBalken().setString("crawling Neighbours");
            Gui.getBalken().paint(Gui.getBalken().getGraphics());
            URL url = new URL("http://ws.audioscrobbler.com/1.0/user/" + username + "/neighbours.xml");
            URLConnection con = url.openConnection();
            InputStream is = con.getInputStream();
            Document document = null;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(is);
            } catch (SAXParseException error) {
                System.out.print("1");
            } catch (ParserConfigurationException pce) {
                System.out.print("2");
            } catch (IOException ioe) {
                System.out.print("3");
            } catch (Throwable t) {
                System.out.print("44");
            }
            NodeList taglist_user = document.getElementsByTagName("user");
            NodeList taglist_match = document.getElementsByTagName("match");
            for (int i = 0; i < taglist_user.getLength(); i++) {
                Node tag_user = taglist_user.item(i);
                Node tag_match = taglist_match.item(i);
                System.out.println(tag_match.getFirstChild().getNodeValue());
                String match = tag_match.getFirstChild().getNodeValue();
                NamedNodeMap attributes_map = tag_user.getAttributes();
                Node user_node = attributes_map.item(0);
                System.out.println(match);
                User.getUserProfile_Stop(user_node.getNodeValue().replace(" ", "%20"));
                DB_Neighbours.addNeighbourRelation(username, user_node.getNodeValue().replace(" ", "%20"), match);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }
}
