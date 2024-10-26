package xtom.parser.examples.rss;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import xtom.parser.Element;
import xtom.parser.Parser;
import xtom.parser.XMLTree;

/**
 * This is an RSS Reader. It support RSS version 2.0 Only. 
 * This Reader was tested with the JRoller RSS feed.
 * @author taras
 * @version $Revision: 1.1 $
 * @since
 */
public class RSSReader {

    String xml = "";

    /** 
	 * Reads the URL for the rss feed. 
	 * @param url The url of the RSS Feed.
	 */
    public RSSReader(URL url) {
        if (url == null) throw new IllegalArgumentException("URL cannot be NULL");
        try {
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) xml += inputLine;
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
	 * Parses the RSS Feed, and returns the RSS Feed document
	 * containing the RSS data.
	 * @return RSS Feed Document.
	 * @throws RuntimeException if the XML read from the URL is not RSS 2.0
	 */
    public Feed parse() {
        Parser p = new Parser(xml);
        XMLTree tree = p.parse();
        Element root = tree.getRootElement();
        if (root.getName() != "rss") throw new RuntimeException("The root element was not <rss>. Can't parse this RSS Feed");
        return parseFeed(root);
    }

    /**
	 * Goes through all elements and maps them onto a RSS holder
	 * classes.
	 * @param root The root elment.
	 * @return an RSS feed.
	 */
    private Feed parseFeed(Element root) {
        Feed feed = new Feed();
        if (root.getAttribute("version") != null) feed.setVersion(root.getAttribute("version").getValue());
        Element ch = root.getElementByPath("channel");
        Channel channel = new Channel(root.getElementByPath("channel/title").getValue());
        channel.setCopyright(ch.getElementByPath("copyright").getValue());
        channel.setDescription(ch.getElementByPath("description").getValue());
        channel.setGenerator(ch.getElementByPath("generator").getValue());
        channel.setLastBuildDate(ch.getElementByPath("lastBuildDate").getValue());
        channel.setLink(ch.getElementByPath("link").getValue());
        channel.setManagingEditor(ch.getElementByPath("managingEditor").getValue());
        channel.setWebMaster(ch.getElementByPath("webMaster").getValue());
        Element[] items = ch.getElementsByPath("item");
        LinkedList itemsList = new LinkedList();
        for (int i = 0; i < items.length; i++) {
            Item item = new Item(items[i].getElementByPath("title").getValue());
            item.setCategory(items[i].getElementByPath("category").getValue());
            item.setDescription(items[i].getElementByPath("description").getValue());
            item.setGuid(items[i].getElementByPath("guid").getValue());
            item.setPermaLink(items[i].getElementByPath("guid").getAttribute("isPermaLink").getValueAsBoolean());
            item.setPubDate(items[i].getElementByPath("pubDate").getValue());
            itemsList.add(item);
        }
        channel.setItems(itemsList);
        feed.setChannel(channel);
        return feed;
    }

    /**
	 * Test this feed
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        URL url = new URL("http://jroller.com/rss");
        RSSReader reader = new RSSReader(url);
        Feed feed = reader.parse();
        System.out.println("Feed version: " + feed.getVersion());
        System.out.println("Channel title: " + feed.getChannel().getTitle());
        System.out.println("Channel description: " + feed.getChannel().getDescription());
        LinkedList list = (LinkedList) feed.getChannel().getItems();
        for (int i = 0; i < list.size(); i++) {
            Item item = (Item) list.get(i);
            System.out.println("Item title: " + item.getTitle());
            System.out.println("Item category: " + item.getCategory());
            System.out.println("Item description: " + item.getDescription());
        }
    }
}
