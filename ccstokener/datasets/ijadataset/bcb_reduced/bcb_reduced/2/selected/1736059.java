package de.wadndadn.commons.rss;

import static de.wadndadn.commons.rss.RssUtils.loadRssItem;
import static org.apache.commons.io.IOUtils.closeQuietly;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
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
 * <p>
 * Based on <a href="http://www.sitepoint.com/rss-feeds-jsp-based-web-apps/">Use Custom Tags to
 * Aggregate RSS Feeds into JSP-Based Web Apps</a>.
 * 
 * @author Simon Brown
 * @author TODO
 * 
 * @since TODO
 */
public final class RssReader {

    /**
     * The url to the feed.
     */
    private final URL url;

    /**
     * TODO Document.
     */
    private final HttpClient httpClient;

    /**
     * Constructor.
     * 
     * @param url
     *            TODO Document
     */
    public RssReader(final URL url) {
        this(url, new DefaultHttpClient());
    }

    /**
     * Constructor.
     * 
     * @param url
     *            TODO Document
     * @param httpClient
     *            TODO Document
     */
    public RssReader(final URL url, final HttpClient httpClient) {
        this.url = url;
        this.httpClient = httpClient;
    }

    /**
     * Reads the RSS feed and returns an {@link RssFeed} instance representing it.
     * 
     * @return an {@link RssFeed} instance representing the feed
     */
    public RssFeed read() {
        List<RssItem> items = new ArrayList<RssItem>();
        try {
            HttpGet getMethod = new HttpGet(url.toString());
            HttpResponse response = httpClient.execute(getMethod);
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {

                public void warning(SAXParseException e) throws SAXException {
                    System.out.println(e);
                    throw e;
                }

                public void error(SAXParseException e) throws SAXException {
                    System.out.println(e);
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    System.out.println(e);
                    throw e;
                }
            });
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            try {
                Document doc = builder.parse(in);
                NodeList channels = doc.getElementsByTagName("channel");
                for (int i = 0; i < channels.getLength(); i++) {
                    NodeList nodes = channels.item(i).getChildNodes();
                    for (int j = 0; j < nodes.getLength(); j++) {
                        Node n = nodes.item(j);
                        if (n.getNodeName().equals("item")) {
                            RssItem rssItem = loadRssItem(n);
                            items.add(rssItem);
                        }
                    }
                }
            } finally {
                closeQuietly(in);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpClient != null) {
                httpClient.getConnectionManager().shutdown();
            }
        }
        return new RssFeed(url, items);
    }
}
