package org.maestroframework.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class RssFeedParser extends DefaultHandler {

    private static final Logger LOG = Logger.getLogger(RssFeedParser.class);

    private String urlString;

    private RssFeed rssFeed;

    private StringBuilder text;

    private Item item;

    private boolean imgStatus;

    private SimpleDateFormat dateFmt;

    private SimpleDateFormat altDateFmt;

    public RssFeedParser(String url) {
        this.urlString = url;
        this.text = new StringBuilder();
        dateFmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        altDateFmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    }

    public void parse() {
        InputSource urlInputStream = null;
        SAXParserFactory spf = null;
        SAXParser sp = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(this.urlString);
            _setProxy();
            conn = (HttpURLConnection) url.openConnection();
            urlInputStream = new InputSource(StreamUtils.inputStreamToReader(conn.getInputStream()));
            spf = SAXParserFactory.newInstance();
            if (spf != null) {
                sp = spf.newSAXParser();
                sp.parse(urlInputStream, this);
            }
        } catch (Exception e) {
            if (conn != null) {
                if (conn.getHeaderField("X-RateLimit-Limit") != null) {
                    String rateLimit = conn.getHeaderField("X-RateLimit-Limit");
                    String rateRemaining = conn.getHeaderField("X-RateLimit-Remaining");
                    long rateReset = Long.valueOf(conn.getHeaderField("X-RateLimit-Reset")) * 1000;
                    LOG.warn("Possible rate limits?  LIMIT:" + rateLimit + "  REMAINING:" + rateRemaining + "  RESET:" + new Date(rateReset));
                }
            }
            e.printStackTrace();
            LOG.warn("error parsing rss feed", e);
        } finally {
        }
    }

    public RssFeed getFeed() {
        return (this.rssFeed);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equalsIgnoreCase("channel")) this.rssFeed = new RssFeed(); else if (qName.equalsIgnoreCase("item") && (this.rssFeed != null)) {
            this.item = new Item();
            this.rssFeed.addItem(this.item);
        } else if (qName.equalsIgnoreCase("image") && (this.rssFeed != null)) {
            this.imgStatus = true;
        } else if (qName.equalsIgnoreCase("enclosure") && (this.item != null)) {
            String type = attributes.getValue("type");
            if (type != null && type.startsWith("image")) {
                this.item.icon = attributes.getValue("url");
            }
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (this.rssFeed == null) return;
        if (qName.equalsIgnoreCase("item")) this.item = null; else if (qName.equalsIgnoreCase("image")) this.imgStatus = false; else if (qName.equalsIgnoreCase("title")) {
            if (this.item != null) this.item.title = this.text.toString().trim(); else if (this.imgStatus) this.rssFeed.imageTitle = this.text.toString().trim(); else this.rssFeed.title = this.text.toString().trim();
        } else if (qName.equalsIgnoreCase("link")) {
            if (this.item != null) this.item.link = this.text.toString().trim(); else if (this.imgStatus) this.rssFeed.imageLink = this.text.toString().trim(); else this.rssFeed.link = this.text.toString().trim();
        } else if (qName.equalsIgnoreCase("description")) {
            if (this.item != null) this.item.description = this.text.toString().trim(); else this.rssFeed.description = this.text.toString().trim();
        } else if (qName.equalsIgnoreCase("url") && this.imgStatus) this.rssFeed.imageUrl = this.text.toString().trim(); else if (qName.equalsIgnoreCase("language")) this.rssFeed.language = this.text.toString().trim(); else if (qName.equalsIgnoreCase("generator")) this.rssFeed.generator = this.text.toString().trim(); else if (qName.equalsIgnoreCase("copyright")) this.rssFeed.copyright = this.text.toString().trim(); else if (qName.equalsIgnoreCase("pubDate") && (this.item != null)) try {
            this.item.pubDate = dateFmt.parse(this.text.toString().trim());
        } catch (ParseException e) {
            try {
                this.item.pubDate = altDateFmt.parse(this.text.toString().trim());
            } catch (ParseException e1) {
            }
        } else if (qName.equalsIgnoreCase("category") && (this.item != null)) {
            String category = this.text.toString().trim();
            this.rssFeed.addItem(category, this.item);
            this.item.categories.add(category);
            if (LOG.isTraceEnabled()) LOG.trace("ADDING CATEGORY: " + category + " to Item: " + this.item.title);
        } else if (qName.equalsIgnoreCase("dc:creator")) {
            this.item.creator = this.text.toString().trim();
        } else if (qName.equalsIgnoreCase("icon") && (this.item != null)) {
            this.item.icon = this.text.toString().trim();
        } else {
            if (item != null) this.item.unhandled.put(qName, this.text.toString().trim());
        }
        this.text.setLength(0);
    }

    public void characters(char[] ch, int start, int length) {
        this.text.append(ch, start, length);
    }

    public static void _setProxy() throws IOException {
        Properties sysProperties = System.getProperties();
        sysProperties.put("proxyHost", "<Proxy IP Address>");
        sysProperties.put("proxyPort", "<Proxy Port Number>");
        System.setProperties(sysProperties);
    }

    public static class RssFeed {

        public String creator;

        public String title;

        public String description;

        public String link;

        public String language;

        public String generator;

        public String copyright;

        public String imageUrl;

        public String imageTitle;

        public String imageLink;

        private List<String> categories = new ArrayList<String>();

        private ArrayList<Item> items;

        private HashMap<String, ArrayList<Item>> category;

        public void addItem(Item item) {
            if (this.items == null) this.items = new ArrayList<Item>();
            this.items.add(item);
        }

        public void addItem(String category, Item item) {
            if (this.category == null) this.category = new HashMap<String, ArrayList<Item>>();
            if (!this.category.containsKey(category)) this.category.put(category, new ArrayList<Item>());
            this.category.get(category).add(item);
        }

        public List<Item> getItems() {
            return this.items;
        }
    }

    public static class Item {

        public String creator;

        public String title;

        public String description;

        public String link;

        public String icon;

        public Date pubDate;

        public List<String> categories = new ArrayList<String>();

        public MultiMap<String, String> unhandled = new MultiMap<String, String>();

        public String toString() {
            return (this.title + ": " + this.pubDate + "n" + this.description);
        }
    }
}
