package fr.upmfgrenoble.rssreader.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.sun.syndication.io.impl.DateParser;
import fr.upmfgrenoble.rssreader.database.RssReader.RssFeed;
import fr.upmfgrenoble.rssreader.database.RssReader.RssFeed.RssItem;

public class RssHandler extends DefaultHandler {

    private static final String[] PROJECTION = { RssFeed.MODIFIED_DATE };

    private static final String TAG = "ArticleHandler";

    private ContentResolver resolver;

    private Uri feed;

    private final ContentValues values = new ContentValues();

    private String feedTitle = null;

    private Date oldUpdate = null;

    private Date newest = null;

    private byte[] img = null;

    private StringBuffer current;

    private static enum Elements {

        channel(1), item(2), title(4), description(8), link(16), rss(32), pubDate(64), image(128), url(256), _other(1 << 31);

        private final int value;

        Elements(int v) {
            value = v;
        }
    }

    ;

    private final int FEED_TITLE = Elements.rss.value | Elements.channel.value | Elements.title.value;

    private final int FEED_ICON = Elements.rss.value | Elements.channel.value | Elements.image.value | Elements.url.value;

    private final int ITEM = Elements.rss.value | Elements.channel.value | Elements.item.value;

    private final int ITEM_TITLE = ITEM | Elements.title.value;

    private final int ITEM_DESCRIPTION = ITEM | Elements.description.value;

    private final int ITEM_LINK = ITEM | Elements.link.value;

    private final int ITEM_PUBDATE = ITEM | Elements.pubDate.value;

    private int path = 0;

    private int nbOther = 0;

    String titreArticle;

    String description;

    URL linkArticle;

    Date pubDate;

    /**
	 * Le contexte (l'activit�) et l'Uri de type content://fr.upmfgrenoble.miam.provider.RSSReader/feeds/#
	 * ou # est l'id du fil a mettre � jour
	 * @param c
	 * @param feed
	 */
    public RssHandler(Context c, Uri feed) {
        this.resolver = c.getContentResolver();
        Cursor cursor = resolver.query(feed, PROJECTION, null, null, null);
        cursor.moveToFirst();
        oldUpdate = new Date(cursor.getLong(0));
        newest = oldUpdate;
        cursor.close();
        this.feed = feed;
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (path == ITEM) {
            Uri itemUri = Uri.withAppendedPath(feed, RssFeed.RssItem.CONTENT_DIRECTORY);
            if (oldUpdate.before(pubDate)) {
                Log.v(TAG, "new item...");
                values.clear();
                values.put(RssItem.TITLE, titreArticle);
                values.put(RssItem.FEED_ID, feed.getLastPathSegment());
                values.put(RssItem.DESCRIPTION, description);
                values.put(RssItem.URL, linkArticle.toString());
                values.put(RssItem.MODIFIED_DATE, pubDate.getTime());
                resolver.insert(itemUri, values);
            }
            if (newest.before(pubDate)) newest = pubDate;
            titreArticle = null;
            description = null;
            linkArticle = null;
        } else if (path == ITEM_TITLE) {
            titreArticle = current.toString();
        } else if (path == ITEM_DESCRIPTION) {
            description = current.toString();
        } else if (path == ITEM_LINK) {
            try {
                Log.v(TAG, current.toString());
                linkArticle = new URL(current.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else if (path == ITEM_PUBDATE) {
            pubDate = DateParser.parseDate(current.toString());
        } else if (path == FEED_TITLE) {
            feedTitle = current.toString();
        } else if (path == FEED_ICON) {
            try {
                URI imgUrl = URI.create(current.toString());
                HttpClient client = new DefaultHttpClient();
                HttpGet req = new HttpGet(imgUrl);
                HttpResponse r = client.execute(req);
                img = new byte[0];
                byte[] buf = new byte[1024];
                int read;
                InputStream is = r.getEntity().getContent();
                while ((read = is.read(buf)) != -1) {
                    byte[] newImg = new byte[img.length + read];
                    System.arraycopy(img, 0, newImg, 0, img.length);
                    System.arraycopy(buf, 0, newImg, img.length, read);
                    img = newImg;
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            path ^= Elements.valueOf(localName).value;
        } catch (IllegalArgumentException e) {
            if (--nbOther == 0) path ^= Elements._other.value;
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        try {
            path |= Elements.valueOf(localName).value;
            current = new StringBuffer();
        } catch (IllegalArgumentException e) {
            nbOther++;
            path |= Elements._other.value;
            current = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (current != null) current.append(ch, start, length).toString();
    }

    @Override
    public void endDocument() throws SAXException {
        values.clear();
        values.put(RssFeed.MODIFIED_DATE, newest.getTime());
        if (img != null) values.put(RssFeed.ICON, img);
        resolver.update(feed, values, null, null);
    }
}
