package org.easyrec.utils.rss;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Responsible for reading RSS feeds from URLs.
 * 
 * <p>
 * <b>Company:&nbsp;</b> SAT, Research Studios Austria
 * </p>
 * 
 * <p>
 * <b>Copyright:&nbsp;</b> (c) 2007
 * </p>
 * 
 * <p>
 * <b>last modified:</b><br/> $Author: sat-rsa $<br/> $Date: 2007-05-09
 * 11:36:52 +0200 (Mi, 09 Mai 2007) $<br/> $Revision: 4 $
 * </p>
 * 
 * @author David Mann
 */
public class RssReader {

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * Reads the RSS feed at the specified URL and returns an RssFeed instance
     * representing it.
     * 
     * @param url
     *            the URL of the RSS feed as a String
     * @return an RssFeed instance representing the feed
     */
    public RssFeed read(String url) {
        RssFeed rssFeed = new RssFeed();
        try {
            HttpClient httpClient = new DefaultHttpClient();
            httpClient.getParams().setIntParameter("http.connection.timeout", 5000);
            HttpGet getMethod = new HttpGet(url);
            HttpResponse resp = httpClient.execute(getMethod);
            int responseCode = resp.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                return rssFeed;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {

                public void warning(SAXParseException e) throws SAXException {
                    logger.warn("caught exception", e);
                    throw e;
                }

                public void error(SAXParseException e) throws SAXException {
                    logger.warn("caught exception", e);
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    logger.warn("caught exception", e);
                    throw e;
                }
            });
            InputStream in = resp.getEntity().getContent();
            Document doc = builder.parse(in);
            NodeList channels = doc.getElementsByTagName("channel");
            for (int i = 0; i < channels.getLength(); i++) {
                NodeList nodes = channels.item(i).getChildNodes();
                for (int j = 0; j < nodes.getLength(); j++) {
                    Node n = nodes.item(j);
                    if (n.getNodeName().equals("item")) {
                        RssItem rssItem = loadRssItem(n);
                        rssFeed.addItem(rssItem);
                    }
                }
            }
            if (rssFeed.getItems().size() == 0) {
                NodeList items = doc.getElementsByTagName("item");
                for (int i = 0; i < items.getLength(); i++) {
                    RssItem rssItem = loadRssItem(items.item(i));
                    rssFeed.addItem(rssItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rssFeed;
    }

    /**
     * Helper method to load an RSS item.
     * 
     * @param root
     *            the root Node describing the item
     * @throws Exception
     *             if the item can't be loaded
     */
    private RssItem loadRssItem(Node root) throws Exception {
        String title = null;
        String link = null;
        String pubDate = null;
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeName().equals("title")) {
                title = getTextValue(n);
            }
            if (n.getNodeName().equals("link")) {
                link = getTextValue(n);
            }
            if (n.getNodeName().equals("pubDate")) {
                pubDate = getTextValue(n);
            }
        }
        RssItem item = new RssItem();
        item.setTitle(title);
        item.setLink(link);
        item.setPubDate(pubDate);
        return item;
    }

    /**
     * Helper method to extract the text value from a given node.
     * 
     * @param node
     *            a Node
     * @return the text value, or an empty string if no text value available
     */
    private String getTextValue(Node node) {
        if (node.hasChildNodes()) {
            return node.getFirstChild().getNodeValue();
        } else {
            return "";
        }
    }
}
