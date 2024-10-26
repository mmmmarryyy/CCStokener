package org.chartsy.main.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.chartsy.main.managers.ProxyManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Viorel
 */
public class RSSFeedUtil {

    public static final String RSS = "rss";

    public static final String TITLE = "title";

    public static final String DESCRIPTION = "description";

    public static final String CHANNEL = "channel";

    public static final String LANGUAGE = "language";

    public static final String LINK = "link";

    public static final String ITEM = "item";

    public static final String PUB_DATE = "pubDate";

    public static final String GUID = "guid";

    public static RSSFeed parseRSSFeed(final String rssFeedURL, final String rssFeedName) {
        RSSFeed feed = null;
        HttpContext context = new BasicHttpContext();
        HttpGet method = new HttpGet(rssFeedURL);
        try {
            HttpResponse response = ProxyManager.httpClient.execute(method, context);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(stream);
            document.normalizeDocument();
            if (document != null) {
                Element rss = (Element) document.getElementsByTagName(RSS).item(0);
                Element channel = (Element) rss.getElementsByTagName(CHANNEL).item(0);
                String title = "";
                String link = "";
                String desc = "";
                String lang = "";
                NodeList nodeList = channel.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    if (element.getTagName().equals(TITLE)) {
                        title = element.getTextContent();
                    }
                    if (element.getTagName().equals(LINK)) {
                        link = element.getTextContent();
                    }
                    if (element.getTagName().equals(DESCRIPTION)) {
                        desc = element.getTextContent();
                    }
                    if (element.getTagName().equals(LANGUAGE)) {
                        lang = element.getTextContent();
                    }
                    if (element.getTagName().equals(ITEM)) {
                        break;
                    }
                }
                feed = new RSSFeed();
                feed.feedName = rssFeedName;
                feed.title = title;
                feed.link = link;
                feed.description = desc;
                feed.language = lang;
                List<RSSFeedMessage> feedMessages = new ArrayList<RSSFeedMessage>();
                nodeList = channel.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element item = (Element) nodeList.item(i);
                    if (item.getTagName().equals(ITEM)) {
                        RSSFeedMessage feedMessage = new RSSFeedMessage();
                        NodeList itemNodeList = item.getChildNodes();
                        for (int j = 0; j < itemNodeList.getLength(); j++) {
                            Element element = (Element) itemNodeList.item(j);
                            if (element.getTagName().equals(TITLE)) {
                                feedMessage.title = element.getTextContent();
                            }
                            if (element.getTagName().equals(GUID)) {
                                feedMessage.guid = element.getTextContent();
                            }
                            if (element.getTagName().equals(LINK)) {
                                feedMessage.link = element.getTextContent();
                            }
                            if (element.getTagName().equals(DESCRIPTION)) {
                                feedMessage.description = element.getTextContent();
                            }
                            if (element.getTagName().equals(PUB_DATE)) {
                                feedMessage.pubDate = element.getTextContent();
                            }
                        }
                        feedMessages.add(feedMessage);
                    }
                }
                feed.entries = feedMessages.toArray(new RSSFeedMessage[feedMessages.size()]);
                feedMessages = null;
            }
        } catch (Exception ex) {
            NotifyUtil.error("RSS parse error", "Could not parse " + rssFeedName + " RSS Feed", false);
            feed = null;
        } finally {
            method.abort();
        }
        return feed;
    }

    public static class RSSFeed {

        public String title;

        public String link;

        public String description;

        public String language;

        public RSSFeedMessage[] entries;

        public String feedName;

        public String toString() {
            return "Feed [description=" + description + ", language=" + language + ", link=" + link + ", title=" + title + "]";
        }
    }

    public static class RSSFeedMessage {

        public String title;

        public String description;

        public String link;

        public String guid;

        public String pubDate;

        public String toString() {
            return "FeedMessage [title=" + title + ", description=" + description + ", link=" + link + ", guid=" + guid + ", pubDate=" + pubDate + "]";
        }
    }
}
