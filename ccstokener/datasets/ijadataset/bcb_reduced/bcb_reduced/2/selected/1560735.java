package org.ifies.android.sax;

import java.io.IOException;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.transdroid.util.FakeSocketFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RssParser extends DefaultHandler {

    /**
     * The constructor for the RSS Parser
     * @param url
     */
    public RssParser(String url) {
        this.urlString = url;
        this.text = new StringBuilder();
    }

    /**
     * Returns the feed as a RssFeed, which is a ListArray
     * @return RssFeed rssFeed
     */
    public Channel getChannel() {
        return (this.channel);
    }

    public void parse() throws ParserConfigurationException, SAXException, IOException {
        DefaultHttpClient httpclient = initialise();
        HttpResponse result = httpclient.execute(new HttpGet(urlString));
        SAXParserFactory spf = SAXParserFactory.newInstance();
        if (spf != null) {
            SAXParser sp = spf.newSAXParser();
            sp.parse(result.getEntity().getContent(), this);
        }
    }

    /**
	 * Instantiates an HTTP client that can be used for all Torrentflux-b4rt requests.
	 * @param connectionTimeout The connection timeout in milliseconds
	 * @return 
	 * @throws DaemonException On conflicting or missing settings
	 */
    private DefaultHttpClient initialise() {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new PlainSocketFactory(), 80));
        registry.register(new Scheme("https", new FakeSocketFactory(), 443));
        HttpParams httpparams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpparams, 5000);
        HttpConnectionParams.setSoTimeout(httpparams, 5000);
        DefaultHttpClient httpclient = new DefaultHttpClient(new ThreadSafeClientConnManager(httpparams, registry), httpparams);
        return httpclient;
    }

    /**
     * By default creates a standard Item (with title, description and links), which
     * may to overriden to add more data.
     * @return A possibly decorated Item instance
     */
    protected Item createNewItem() {
        return new Item();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (localName.equalsIgnoreCase("channel")) {
            this.channel = new Channel();
        }
        if (localName.equalsIgnoreCase("item") && (this.channel != null)) {
            this.item = createNewItem();
            this.channel.addItem(this.item);
        }
        if (localName.equalsIgnoreCase("image") && (this.channel != null)) {
            this.imgStatus = true;
        }
        if (localName.equalsIgnoreCase("enclosure")) {
            if (this.item != null && attributes != null && attributes.getLength() > 0) {
                if (attributes.getValue("url") != null) {
                    this.item.setEnclosureUrl(parseLink(attributes.getValue("url")));
                }
                if (attributes.getValue("type") != null) {
                    this.item.setEnclosureType(attributes.getValue("type"));
                }
                if (attributes.getValue("length") != null) {
                    this.item.setEnclosureLength(Long.parseLong(attributes.getValue("length")));
                }
            }
        }
    }

    /**
     * This is where we actually parse for the elements contents
     */
    public void endElement(String uri, String localName, String qName) {
        if (this.channel == null) {
            return;
        }
        if (localName.equalsIgnoreCase("item")) {
            this.item = null;
        }
        if (localName.equalsIgnoreCase("image")) this.imgStatus = false;
        if (localName.equalsIgnoreCase("title")) {
            if (this.item != null) {
                this.item.setTitle(this.text.toString().trim());
            } else {
                this.channel.setTitle(this.text.toString().trim());
            }
        }
        if (localName.equalsIgnoreCase("link")) {
            if (this.item != null) {
                this.item.setLink(parseLink(this.text.toString()));
            } else if (this.imgStatus) {
                this.channel.setImage(parseLink(this.text.toString()));
            } else {
                this.channel.setLink(parseLink(this.text.toString()));
            }
        }
        if (localName.equalsIgnoreCase("description")) {
            if (this.item != null) {
                this.item.setDescription(this.text.toString().trim());
            } else {
                this.channel.setDescription(this.text.toString().trim());
            }
        }
        if (localName.equalsIgnoreCase("pubDate")) {
            if (this.item != null) {
                try {
                    this.item.setPubdate(new Date(Date.parse(this.text.toString().trim())));
                } catch (Exception e) {
                }
            } else {
                try {
                    this.channel.setPubDate(new Date(Date.parse(this.text.toString().trim())));
                } catch (Exception e) {
                }
            }
        }
        if (localName.equalsIgnoreCase("category") && (this.item != null)) {
            this.channel.addCategory(this.text.toString().trim());
        }
        addAdditionalData(localName, this.item, this.text.toString());
        this.text.setLength(0);
    }

    /**
     * May be overridden to add additional data from tags that are not standard in RSS. 
     * Not used by this default RSS style parser.
     * @param localName The tag name
     * @param item The Item we are currently parsing
     * @param text The new text content
     */
    protected void addAdditionalData(String localName, Item item, String text) {
    }

    public void characters(char[] ch, int start, int length) {
        this.text.append(ch, start, length);
    }

    private String parseLink(String string) {
        return string.trim();
    }

    private String urlString;

    private Channel channel;

    private StringBuilder text;

    private Item item;

    private boolean imgStatus;
}
