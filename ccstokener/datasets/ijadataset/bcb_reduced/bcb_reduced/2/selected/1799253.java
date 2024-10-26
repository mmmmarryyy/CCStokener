package com.romanenco.code.oscache;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

/**
 * osCache use sample.
 * System will not create CacheEntry object every time, but will use instance
 * from cache, until it expires.
 * 
 * Note, cache expires every 5 seconds, so cache will expire for second Sleep
 * 
 * @author Andrew Romanenco
 *
 */
public class SimpleUseCase {

    private static Logger logger = Logger.getLogger(SimpleUseCase.class);

    private GeneralCacheAdministrator admin;

    private Cache map;

    private int secondsExpire = 5;

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        logger.debug("SimpleUseCase started...");
        SimpleUseCase ins = new SimpleUseCase();
        ins.setup();
        logger.debug("Select from empty cache");
        ins.get("alfa");
        try {
            Thread.sleep(2000);
            ins.get("alfa");
            Thread.sleep(4000);
            ins.get("alfa");
        } catch (InterruptedException e) {
            logger.error("On sleep", e);
        }
    }

    public void setup() {
        Properties props = new Properties();
        URL url = ClassLoader.getSystemResource("oscache.properties");
        try {
            props.load(url.openStream());
        } catch (IOException e) {
            logger.error("Properties are not loaded. Working with defaults.");
        }
        admin = new GeneralCacheAdministrator(props);
        map = admin.getCache();
        if (props.get("item.expire") != null) {
            try {
                secondsExpire = Integer.parseInt((String) props.get("item.expire"));
            } catch (Exception e) {
                logger.warn("Wrong format for item.expire", e);
            }
        }
        logger.info("Cache expiration in seconds: " + secondsExpire);
    }

    public void put(String key, Object value) {
        map.putInCache(key, value);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, secondsExpire * 1000);
        map.flushAll(cal.getTime());
        logger.debug("Added key to cache: " + key);
    }

    public Object get(String key) {
        logger.debug("Reading from cache: " + key);
        try {
            Object o = map.getFromCache(key);
            logger.debug("Object read from cache with key: " + key);
            return o;
        } catch (NeedsRefreshException e) {
            logger.debug("NeedsRefreshException detected, re put entry.");
            CacheEntry entry = new CacheEntry();
            put(key, entry);
            return entry;
        }
    }
}
