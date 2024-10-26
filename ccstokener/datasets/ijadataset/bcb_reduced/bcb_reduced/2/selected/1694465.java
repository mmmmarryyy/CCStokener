package com.mudderman.marmas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TimerTask;
import com.mudderman.marmas.model.Feed;
import com.mudderman.marmas.model.Settings;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

@SuppressWarnings("unchecked")
public class FeedUpdater extends TimerTask {

    private Feed feed;

    private URL url;

    public FeedUpdater(Feed feed) {
        if (feed == null) {
            throw new IllegalArgumentException("Feed cannot be null");
        }
        try {
            this.url = new URL(feed.getUrl());
        } catch (MalformedURLException e) {
            Server.print("Could not find url: \"" + feed.getUrl() + "\"");
        }
        this.feed = feed;
    }

    /**
	 * Update the feed 
	 */
    public void update() {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null!");
        }
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("User-Agent", Settings.INSTANCE.getUserAgent());
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed syndFeed = input.build(new XmlReader(url));
            Channel channel = (Channel) syndFeed.createWireFeed(syndFeed.getFeedType());
            long lastModified = urlConnection.getLastModified();
            if (feed.getLastModified() != lastModified) {
                Server.print("Updating: " + feed.getName());
                feed.setLastModified(lastModified);
                ArrayList<String> cachedItems = getCachedItems();
                List<Item> items = channel.getItems();
                if (items.isEmpty()) {
                    return;
                }
                if (cachedItems.isEmpty()) {
                    Database.INSTANCE.addItems(feed, items);
                } else {
                    for (Item item : items) {
                        if (!cachedItems.contains(item.getTitle())) {
                            Database.INSTANCE.addItem(feed, item);
                        }
                    }
                }
                cacheItems(items);
                Settings.INSTANCE.persist();
                Server.print("Done updating: " + feed.getName());
            }
        } catch (ConnectException e) {
            Server.print("Could not connect to \"" + feed.getName() + "\"");
        } catch (SocketException e) {
            Server.print("Could not connect to \"" + feed.getName() + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Get a list of the titles from the last update.
	 * @return list with strings, where each string represents a &lt;title&gt;
	 */
    private ArrayList<String> getCachedItems() {
        ArrayList<String> cachedItems = new ArrayList<String>();
        try {
            this.createCacheFolder();
            Scanner sc = new Scanner(new File("./cache/" + feed.getName()));
            while (sc.hasNextLine()) {
                cachedItems.add(sc.nextLine().trim());
            }
            sc.close();
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            Server.print("Exception in getCachedItems: " + e.getMessage());
        }
        return cachedItems;
    }

    /**
	 * Save the current &lt;title&gt; from the feed.
	 * @param items feed items
	 */
    private void cacheItems(List<Item> items) {
        if (items == null) {
            return;
        }
        try {
            this.createCacheFolder();
            FileWriter fr = new FileWriter(new File("./cache/" + feed.getName()));
            for (Item item : items) {
                fr.write(item.getTitle());
                fr.write("\n");
            }
            fr.flush();
            fr.close();
        } catch (FileNotFoundException e) {
            Server.print("Could not find cache for \"" + feed.getName() + "\", created.");
        } catch (Exception e) {
            Server.print("Exception in cacheItems: " + e.getMessage());
        }
    }

    /**
	 * Creates the folder for the cache
	 */
    private void createCacheFolder() {
        try {
            File folder = new File("./cache");
            if (!folder.exists()) {
                folder.createNewFile();
            }
        } catch (Exception e) {
            Server.print("Exception in createCacheFolder: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        this.update();
    }
}
