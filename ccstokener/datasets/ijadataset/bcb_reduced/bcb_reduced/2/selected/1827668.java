package org.jtweet.util;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

public class ImageManager {

    private static ImageManager instance;

    private Map<String, Image> images;

    private Map<String, Long> imageTimes;

    private boolean isDisposed;

    private int cacheTimeMinutes;

    private ImageManager() {
        images = new Hashtable<String, Image>();
        imageTimes = new Hashtable<String, Long>();
        isDisposed = false;
        cacheTimeMinutes = 2;
    }

    public static ImageManager get() {
        if (instance == null) {
            instance = new ImageManager();
        }
        return instance;
    }

    public Image getImage(String image) {
        if (!images.containsKey(image)) {
            images.put(image, new Image(null, new ImageData(image)));
        }
        return images.get(image);
    }

    public Image getURLImage(String url) {
        if (!images.containsKey(url)) {
            try {
                URL img = new URL(url);
                images.put(url, new Image(null, img.openStream()));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage() + ": " + url);
            }
        }
        imageTimes.put(url, System.currentTimeMillis());
        return images.get(url);
    }

    public void dispose() {
        Iterator<Image> e = images.values().iterator();
        while (e.hasNext()) {
            e.next().dispose();
        }
        images.clear();
        imageTimes.clear();
        isDisposed = true;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public void setCacheTime(int minutes) {
        cacheTimeMinutes = minutes <= 1 ? 1 : minutes;
    }

    public void removeOldImages() {
        Iterator<Entry<String, Long>> ek = imageTimes.entrySet().iterator();
        Entry<String, Long> e;
        long end = System.currentTimeMillis();
        long time, cache;
        long total = 1000 * 60 * cacheTimeMinutes;
        String key;
        while (ek.hasNext()) {
            e = ek.next();
            time = e.getValue();
            key = e.getKey();
            cache = end - time;
            if (cache >= total) {
                images.get(key).dispose();
                images.remove(key);
                ek.remove();
            }
        }
    }
}
