package com.sun.portal.rssportlet;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.InputSource;

/**
 * This class manages a cache of ROME feeds.
 */
@SuppressWarnings("unchecked")
public class FeedHelper {

    private static FeedHelper feedHelper = new FeedHelper();

    private Map feeds = Collections.synchronizedMap(new HashMap());

    /**
     * This class is the cached representation of a ROME feed.
     */
    private static final class FeedElement {

        private SyndFeed feed = null;

        private long cacheTime;

        private long timeout;

        public FeedElement(SyndFeed feed, int timeout) {
            this.feed = feed;
            this.cacheTime = System.currentTimeMillis();
            this.timeout = timeout * 1000;
        }

        public SyndFeed getFeed() {
            return feed;
        }

        public boolean isExpired() {
            if (timeout < 0) {
                return false;
            }
            if ((cacheTime + timeout) < System.currentTimeMillis()) {
                return true;
            }
            return false;
        }
    }

    private FeedHelper() {
    }

    /**
	 * Get the feed handler singleton instance.
	 */
    public static FeedHelper getInstance() {
        return feedHelper;
    }

    /**
     * Get the ROME SyndFeed object for the specified feed. The object may come
     * from a cache; the data in the feed may not be read at the time
     * this method is called.
     *
     * The <code>RssPortletBean</code> object is used to identify the feed
     * of interest, and the timeout value to be used when managing this
     * feed's cached value.
     *
     * @param bean an <code>RssPortletBean</code> object that describes
     * the feed of interest, and the cache timeout value for the feed.
     * @return a ROME <code>SyndFeed</code> object encapsulating the
     * feed specified by the URL.
     */
    public SyndFeed getFeed(SettingsBean bean, String selectedFeed) throws IOException, FeedException {
        SyndFeed feed = null;
        FeedElement feedElement = (FeedElement) feeds.get(selectedFeed);
        if (feedElement != null && !feedElement.isExpired()) {
            feed = feedElement.getFeed();
        } else {
            URL feedUrl = new URL(selectedFeed);
            URLConnection urlc = feedUrl.openConnection();
            urlc.connect();
            SyndFeedInput input = new SyndFeedInput();
            InputSource src = new InputSource(urlc.getInputStream());
            feed = input.build(src);
            int timeout = bean.getCacheTimeout();
            if (timeout != 0) {
                putFeed(selectedFeed, feed, timeout);
            }
        }
        return feed;
    }

    /**
	 * Get the ROME SyndFeed object for the feed specified by the 
	 * SettingsBean's selectedFeed field.
	 */
    public SyndFeed getFeed(SettingsBean bean) throws IOException, FeedException {
        return getFeed(bean, bean.getSelectedFeed());
    }

    /**
     * Put a ROME feed into the cache.
     * This method must be called from within a synchronzied block.
     */
    private void putFeed(String url, SyndFeed feed, int timeout) {
        FeedElement feedElement = new FeedElement(feed, timeout);
        feeds.put(url, feedElement);
    }
}
