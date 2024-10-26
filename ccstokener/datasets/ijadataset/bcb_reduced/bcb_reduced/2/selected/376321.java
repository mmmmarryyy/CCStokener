package net.dromard.common.rss;

import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.dromard.common.rss.feed.Channel;
import net.dromard.common.rss.feed.Enclosure;
import net.dromard.common.rss.feed.Item;
import net.dromard.common.rss.feed.RSS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RSSFeedReader extends RSSReader {

    public RSS load(final URL url) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(url.openStream());
        RSS rss = new RSS();
        NodeList feedNodes = doc.getElementsByTagName("rss");
        for (int f = 0; f < feedNodes.getLength(); f++) {
            Element element = (Element) feedNodes.item(f);
            rss.setVersion(element.getAttribute("version"));
            NodeList channelNodes = element.getElementsByTagName("channel");
            if (channelNodes.getLength() >= 1) {
                element = (Element) channelNodes.item(0);
                Channel channel = new Channel();
                channel.setTitle(getElementValue(element, "title"));
                channel.setDescription(getElementValue(element, "description"));
                channel.setLink(getElementValue(element, "link"));
                channel.setAuthor(getElementValue(element, "author"));
                NodeList items = element.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    Element itemElmt = ((Element) items.item(i));
                    Item item = new Item();
                    item.setAuthor(getElementValue(itemElmt, "author"));
                    item.setCategory(getElementValue(itemElmt, "category"));
                    item.setDescription(getElementValue(itemElmt, "description"));
                    item.setLink(getElementValue(itemElmt, "link"));
                    item.setGuid(getElementValue(itemElmt, "guid"));
                    item.setPublished(getElementValue(itemElmt, "pubDate"));
                    item.setTitle(getElementValue(itemElmt, "title"));
                    NodeList enclosureNode = itemElmt.getElementsByTagName(Enclosure.ENCLOSURE);
                    if (enclosureNode.getLength() == 1) {
                        Enclosure enclosure = new Enclosure();
                        Element enclosureElmt = ((Element) enclosureNode.item(0));
                        String length = enclosureElmt.getAttribute(Enclosure.LENGTH);
                        if (length.length() > 0) {
                            try {
                                enclosure.setLength(Integer.parseInt(length));
                            } catch (NumberFormatException e) {
                            }
                        }
                        enclosure.setType(enclosureElmt.getAttribute(Enclosure.TYPE));
                        enclosure.setUrl(enclosureElmt.getAttribute(Enclosure.URL));
                        item.setEnclosure(enclosure);
                    }
                    channel.addItem(item);
                }
                rss.setChannel(channel);
            }
        }
        return rss;
    }

    public static void main(String[] args) throws Exception {
        RSSFeedReader reader = new RSSFeedReader();
        URL u = new URL("http://picasaweb.google.fr/data/feed/base/user/laurentetsylvie75/albumid/5264008816855671777?alt=rss&kind=photo&authkey=OJ0rnRRHaLA&hl=fr");
        RSS rss = reader.load(u);
        System.out.println(rss.toXML());
    }
}
