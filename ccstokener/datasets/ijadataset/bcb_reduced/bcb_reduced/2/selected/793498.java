package org.devtcg.rssreader.parser;

import org.devtcg.rssreader.dao.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.devtcg.rssreader.provider.RSSReader;
import org.devtcg.rssreader.util.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.ContentURI;
import android.os.Handler;
import android.util.Log;

/**
 * Clase que refresca los feeds. Es la solucion recomendada para operaciones
 * largas. Consiste en arrancar un thread distinto y al final actualizar el
 * thread de la UI con los resultados
 * Para el posicionamiento del feed utilizamos http://www.georss.org/simple
 * @author gcristofol
 *
 */
public class ChannelRefresh extends DefaultHandler {

    private static final String TAG = "RSSChannelRefresh";

    private long mID;

    private String mRSSURL;

    private ContentResolver mContent;

    private ChannelPostHolder mPostBuf;

    private int mState;

    private static final int STATE_IN_ITEM = (1 << 2);

    private static final int STATE_IN_ITEM_TITLE = (1 << 3);

    private static final int STATE_IN_ITEM_LINK = (1 << 4);

    private static final int STATE_IN_ITEM_DESC = (1 << 5);

    private static final int STATE_IN_ITEM_DATE = (1 << 6);

    private static final int STATE_IN_ITEM_AUTHOR = (1 << 7);

    private static final int STATE_IN_TITLE = (1 << 8);

    private static final int STATE_IN_ITEM_ENCLOSURE = (1 << 9);

    private static final int STATE_IN_ITEM_GEORSS_POS = (1 << 10);

    private static final int STATE_IN_ITEM_ITUNES_IMAGE = (1 << 11);

    private static HashMap<String, Integer> mStateMap;

    static {
        mStateMap = new HashMap<String, Integer>();
        mStateMap.put("item", new Integer(STATE_IN_ITEM));
        mStateMap.put("entry", new Integer(STATE_IN_ITEM));
        mStateMap.put("title", new Integer(STATE_IN_ITEM_TITLE));
        mStateMap.put("link", new Integer(STATE_IN_ITEM_LINK));
        mStateMap.put("description", new Integer(STATE_IN_ITEM_DESC));
        mStateMap.put("content", new Integer(STATE_IN_ITEM_DESC));
        mStateMap.put("content:encoded", new Integer(STATE_IN_ITEM_DESC));
        mStateMap.put("dc:date", new Integer(STATE_IN_ITEM_DATE));
        mStateMap.put("updated", new Integer(STATE_IN_ITEM_DATE));
        mStateMap.put("pubDate", new Integer(STATE_IN_ITEM_DATE));
        mStateMap.put("dc:author", new Integer(STATE_IN_ITEM_AUTHOR));
        mStateMap.put("author", new Integer(STATE_IN_ITEM_AUTHOR));
        mStateMap.put("enclosure", new Integer(STATE_IN_ITEM_ENCLOSURE));
        mStateMap.put("georss:point", new Integer(STATE_IN_ITEM_GEORSS_POS));
        mStateMap.put("itunes:image", new Integer(STATE_IN_ITEM_ITUNES_IMAGE));
    }

    public ChannelRefresh(ContentResolver resolver) {
        super();
        mContent = resolver;
    }

    public long syncDB(Handler h, long id, String rssurl) throws Exception {
        mID = id;
        mRSSURL = rssurl;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();
        xr.setContentHandler(this);
        xr.setErrorHandler(this);
        URL url = new URL(mRSSURL);
        URLConnection c = url.openConnection();
        c.setRequestProperty("User-Agent", "Android/m3-rc37a");
        xr.parse(new InputSource(c.getInputStream()));
        return mID;
    }

    public boolean updateFavicon(long id, String iconUrl) throws MalformedURLException {
        return updateFavicon(id, new URL(iconUrl));
    }

    public boolean updateFavicon(long id, URL iconUrl) {
        InputStream stream = null;
        OutputStream ico = null;
        boolean r = false;
        ContentURI feedUri = RSSReader.Channels.CONTENT_URI.addId(id);
        ContentURI iconUri = feedUri.addPath("icon");
        try {
            stream = iconUrl.openStream();
            ico = mContent.openOutputStream(iconUri);
            byte[] b = new byte[1024];
            int n;
            while ((n = stream.read(b)) != -1) ico.write(b, 0, n);
            r = true;
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        } finally {
            try {
                if (stream != null) stream.close();
                if (ico != null) ico.close();
            } catch (IOException e) {
            }
        }
        return r;
    }

    public void startElement(String uri, String name, String qName, Attributes attrs) {
        Log.d(TAG, "Inicio elemento " + qName);
        if (mID == -1 && qName.equals("title") && (mState & STATE_IN_ITEM) == 0) {
            mState |= STATE_IN_TITLE;
            return;
        }
        if (qName.equals("itunes:image") && (mState & STATE_IN_ITEM) == 0) {
            String href = attrs.getValue("href");
            if (href != null) ChannelPostHolder.logotype = href;
        }
        Integer state = mStateMap.get(qName);
        if (state != null) {
            mState |= state.intValue();
            if (state.intValue() == STATE_IN_ITEM) mPostBuf = new ChannelPostHolder(); else if ((mState & STATE_IN_ITEM) != 0 && state.intValue() == STATE_IN_ITEM_LINK) {
                String href = attrs.getValue("href");
                if (href != null) mPostBuf.link = href;
            } else if ((mState & STATE_IN_ITEM) != 0 && state.intValue() == STATE_IN_ITEM_ENCLOSURE) {
                String url = attrs.getValue("url");
                if (url != null) mPostBuf.enclosure = url;
            }
        }
    }

    public void endElement(String uri, String name, String qName) {
        Integer state = mStateMap.get(qName);
        if (state != null) {
            mState &= ~(state.intValue());
            if (state.intValue() == STATE_IN_ITEM) {
                if (mID == -1) {
                    Log.d(TAG, "Oops, </item> found before feed title and our parser sucks too much to deal.");
                    return;
                }
                String[] dupProj = new String[] { RSSReader.Posts._ID };
                ContentURI listURI = RSSReader.Posts.CONTENT_URI_LIST.addId(mID);
                Cursor dup = mContent.query(listURI, dupProj, "title = ? AND url = ?", new String[] { mPostBuf.title, mPostBuf.link }, null);
                Log.d(TAG, "Post: " + mPostBuf.title);
                Log.d(TAG, "Insert del elemento con posicion = " + mPostBuf.georsspoint);
                Log.d(TAG, "Insert del elemento con enclosure = " + mPostBuf.enclosure);
                Log.d(TAG, "Insert del elemento con logotype = " + mPostBuf.logotype);
                if (dup.count() == 0) {
                    ContentValues values = new ContentValues();
                    values.put(RSSReader.Posts.CHANNEL_ID, mID);
                    values.put(RSSReader.Posts.TITLE, mPostBuf.title);
                    values.put(RSSReader.Posts.URL, mPostBuf.link);
                    values.put(RSSReader.Posts.AUTHOR, mPostBuf.author);
                    values.put(RSSReader.Posts.DATE, mPostBuf.getDate());
                    values.put(RSSReader.Posts.BODY, mPostBuf.desc);
                    values.put(RSSReader.Posts.ENCLOSURE, mPostBuf.enclosure);
                    values.put(RSSReader.Posts.GEORSSPOINT, mPostBuf.georsspoint);
                    values.put(RSSReader.Posts.LOGOTYPE, mPostBuf.logotype);
                    mContent.insert(RSSReader.Posts.CONTENT_URI, values);
                }
                dup.close();
            }
        }
    }

    public void characters(char ch[], int start, int length) {
        if (mID == -1 && (mState & STATE_IN_TITLE) != 0) {
            ContentValues values = new ContentValues();
            values.put(RSSReader.Channels.TITLE, new String(ch, start, length));
            values.put(RSSReader.Channels.URL, mRSSURL);
            ContentURI added = mContent.insert(RSSReader.Channels.CONTENT_URI, values);
            mID = Long.parseLong(added.getPathSegment(1));
            mState &= ~STATE_IN_TITLE;
            return;
        }
        if ((mState & STATE_IN_ITEM) == 0) return;
        switch(mState) {
            case STATE_IN_ITEM | STATE_IN_ITEM_TITLE:
                mPostBuf.title = new String(ch, start, length);
                break;
            case STATE_IN_ITEM | STATE_IN_ITEM_DESC:
                mPostBuf.desc = new String(ch, start, length);
                break;
            case STATE_IN_ITEM | STATE_IN_ITEM_LINK:
                mPostBuf.link = new String(ch, start, length);
                break;
            case STATE_IN_ITEM | STATE_IN_ITEM_DATE:
                mPostBuf.setDate(new String(ch, start, length));
                break;
            case STATE_IN_ITEM | STATE_IN_ITEM_AUTHOR:
                mPostBuf.author = new String(ch, start, length);
                break;
            case STATE_IN_ITEM | STATE_IN_ITEM_GEORSS_POS:
                mPostBuf.georsspoint = new String(ch, start, length);
                break;
            default:
        }
    }
}
