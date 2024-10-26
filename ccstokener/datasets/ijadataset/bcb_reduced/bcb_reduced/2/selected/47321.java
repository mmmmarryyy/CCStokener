package cn.edu.nju.software.xyz.pim.rss;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import android.content.Context;
import cn.edu.nju.software.xyz.pim.util.Log;

/**
 * @author savio 2008-4-8 下午09:39:13 Parses RSS feed and saves what we need from
 *         them
 */
public class RSSHandler extends DefaultHandler {

    private boolean inItem = false;

    private boolean inTitle = false;

    private boolean inLink = false;

    private boolean inDiscription = false;

    private boolean inPubdate = false;

    private final Article currentArticle = new Article();

    private Feed currentFeed = new Feed();

    private int articlesAdded = 0;

    private static final int ARTICLES_LIMIT = 200;

    private static final int TARGET_FEED = 0;

    private static final int TARGET_ARTICLES = 1;

    private int targetFlag;

    private NewsDroidDB droidDB = null;

    private static RSSHandler ins;

    private RSSHandler() {
    }

    public static synchronized RSSHandler getInstance() {
        if (null == ins) ins = new RSSHandler();
        return ins;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        if (name.trim().equals("title")) inTitle = true; else if (name.trim().equals("item")) inItem = true; else if (name.trim().equals("link")) inLink = true; else if (name.trim().equals("pubDate")) inPubdate = true; else if (name.trim().equals("description")) inDiscription = true;
    }

    @Override
    public void endElement(String uri, String name, String qName) throws SAXException {
        if (name.trim().equals("title")) inTitle = false; else if (name.trim().equals("item")) inItem = false; else if (name.trim().equals("link")) inLink = false; else if (name.trim().equals("pubDate")) inPubdate = false; else if (name.trim().equals("description")) inDiscription = false;
        if (targetFlag == TARGET_FEED && currentFeed.Url != null && currentFeed.Title != null) {
            droidDB.insertFeed(currentFeed.Title, currentFeed.Url);
            Log.i(currentFeed.Title);
            Log.i(currentFeed.Url.toString());
            throw new SAXException();
        }
        if (targetFlag == TARGET_ARTICLES && currentArticle.Url != null && currentArticle.Title != null && currentArticle.date != null && currentArticle.Discription != null) {
            droidDB.insertArticle(currentFeed.FeedId, currentArticle.Title, currentArticle.Url, currentArticle.date, currentArticle.Discription);
            currentArticle.Title = null;
            currentArticle.Url = null;
            currentArticle.Discription = null;
            currentArticle.date = null;
            articlesAdded++;
            if (articlesAdded >= ARTICLES_LIMIT) throw new SAXException();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        String chars = (new String(ch).substring(start, start + length));
        try {
            if (!inItem) {
                if (inTitle) currentFeed.Title = chars;
            } else {
                if (inLink) currentArticle.Url = new URL(chars);
                if (inTitle) currentArticle.Title = chars;
                if (inPubdate) currentArticle.date = chars;
                if (inDiscription) currentArticle.Discription = chars;
            }
        } catch (MalformedURLException e) {
            Log.e(e.getMessage());
        }
    }

    public void createFeed(Context ctx, URL url) {
        try {
            targetFlag = TARGET_FEED;
            droidDB = NewsDroidDB.getInstance(ctx);
            currentFeed.Url = url;
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            xr.parse(new InputSource(url.openStream()));
        } catch (IOException e) {
            Log.e(e.getMessage());
        } catch (SAXException e) {
            Log.e(e.getMessage());
        } catch (ParserConfigurationException e) {
            Log.e(e.getMessage());
        }
    }

    public void updateArticles(Context ctx, Feed feed) {
        try {
            targetFlag = TARGET_ARTICLES;
            droidDB = NewsDroidDB.getInstance(ctx);
            currentFeed = feed;
            droidDB.deleteAricles(feed.FeedId);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            xr.parse(new InputSource(currentFeed.Url.openStream()));
        } catch (IOException e) {
            Log.e(e.getMessage());
        } catch (SAXException e) {
            Log.e(e.getMessage());
        } catch (ParserConfigurationException e) {
            Log.e(e.getMessage());
        }
    }
}
