package com.twilight.SofaStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.xml.sax.helpers.DefaultHandler;
import com.twilight.SofaStream.EpisodeList.updateThread;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class RSSHandler extends DefaultHandler {

    private boolean inItem = false;

    private boolean inTitle = false;

    private boolean inLink = false;

    private boolean inPubDate = false;

    private boolean inSummary = false;

    private boolean inDescription = false;

    private String articleTitle = "";

    private String articleUrl = "";

    private String articleFile = "";

    private String articleFileSize = "";

    private String articleText = "";

    private String articleDesc = "";

    private String articlePubdate = "";

    private String feedTitle = "";

    private RSSFeed mFeed;

    private RSSDB db;

    EpisodeList.updateThread wrapper;

    RSSHandler(EpisodeList.updateThread w) {
        wrapper = w;
    }

    private static final int ARTICLES_LIMIT = 15;

    public void startElement(String uri, String name, String qName, Attributes atts) {
        if (name.trim().equals("title")) inTitle = true; else if (name.trim().equals("item")) inItem = true; else if (name.trim().equals("link")) inLink = true; else if (name.trim().equals("pubDate")) inPubDate = true; else if (name.trim().equals("summary")) inSummary = true; else if (name.trim().equals("description")) inDescription = true; else if (name.trim().equals("enclosure")) {
            articleFile = atts.getValue("url");
            articleFileSize = atts.getValue("length");
        }
    }

    public void endElement(String uri, String name, String qName) throws SAXException {
        if (name.trim().equals("title")) inTitle = false; else if (name.trim().equals("link")) inLink = false; else if (name.trim().equals("pubDate")) inPubDate = false; else if (name.trim().equals("summary")) inSummary = false; else if (name.trim().equals("description")) inDescription = false; else if (name.trim().equals("item")) {
            inItem = false;
            RSSItem currentItem = new RSSItem();
            try {
                currentItem.url = new URL(articleUrl);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                currentItem.url = null;
            }
            try {
                currentItem.remotefile = new URL(articleFile);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                currentItem.remotefile = null;
            }
            SimpleDateFormat df = new SimpleDateFormat();
            df.applyPattern("E, dd MMM yyyy HH:mm:ss Z");
            try {
                currentItem.pubdate = df.parse(articlePubdate);
            } catch (ParseException e) {
                Log.e("GeneriCast", e.toString());
            }
            currentItem.downloadstate = RSSItem.downloadstates.UNDOWNLOADED;
            currentItem.listened = false;
            currentItem.title = articleTitle;
            if (articleText.equals("")) {
                currentItem.text = articleDesc;
            } else {
                currentItem.text = articleText;
            }
            try {
                currentItem.filesize = Long.parseLong(articleFileSize);
            } catch (NumberFormatException e) {
                currentItem.filesize = 0;
            }
            currentItem.downloadprogress = 0;
            RSSItem duplicate = db.findExistingArticle(currentItem);
            if (duplicate != null) {
                boolean urlIdentical = compareUrls(duplicate.url, currentItem.url);
                boolean remotefileIdentical = compareUrls(duplicate.remotefile, currentItem.remotefile);
                if ((duplicate.title.compareTo(currentItem.title) != 0) || !urlIdentical || !remotefileIdentical) {
                    ContentValues content = new ContentValues();
                    content.put("title", articleTitle);
                    content.put("url", articleUrl);
                    if (duplicate.remotefile.equals(currentItem.remotefile)) {
                        content.put("remotefile", articleFile);
                    }
                    content.put("listened", false);
                    db.updateArticle(duplicate.id, content);
                    Log.d("GeneriCast", String.format("Duplicate found for %s, updating", articleTitle));
                } else {
                    Log.d("GeneriCast", String.format("Duplicate found for %s - no update needed", articleTitle));
                    throw new SAXException("Done parsing");
                }
            } else {
                long result = db.insertArticle(mFeed.id, currentItem);
                Log.d("GeneriCast", String.format("Inserted %s, %d", articleTitle, result));
            }
            articleTitle = "";
            articleText = "";
            articleDesc = "";
            articleUrl = "";
            articleFile = "";
            articleFileSize = "";
            articlePubdate = "";
            wrapper.notifyItemDone();
        }
    }

    public boolean compareUrls(URL a, URL b) {
        if (a == null && b == null) return true;
        if ((a == null && b != null) || (a == null && b != null)) return false;
        return a.equals(b);
    }

    public void characters(char ch[], int start, int length) {
        String chars = (new String(ch).substring(start, start + length));
        if (!inItem) {
            if (inTitle) feedTitle += chars;
        } else {
            if (inLink) {
                articleUrl += chars;
            }
            if (inTitle) articleTitle += chars;
            if (inSummary) {
                articleText += chars;
            }
            if (inDescription) {
                articleDesc += chars;
            }
            if (inPubDate) {
                articlePubdate += chars;
            }
        }
    }

    public void storeArticles(Context ctx, RSSFeed feed) throws RSSHandlerError {
        try {
            mFeed = feed;
            db = new RSSDB(ctx);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);
            InputStream stream = feed.url.openStream();
            InputSource source = new InputSource(stream);
            xr.parse(source);
        } catch (IOException e) {
            Log.e("GeneriCast", e.toString());
            throw new RSSHandlerError("IOError");
        } catch (SAXException e) {
            Log.e("GeneriCast", e.toString());
            throw new RSSHandlerError("ParsingError");
        } catch (ParserConfigurationException e) {
            Log.e("GeneriCast", e.toString());
            throw new RSSHandlerError("ParsingError");
        }
    }
}
