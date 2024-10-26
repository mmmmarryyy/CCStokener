package org.sensorweb.core.scs.cache;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.sensorweb.GlobalConstant;
import org.sensorweb.core.ObjectFactory;
import org.sensorweb.SensorQuery;

/**
 * The Class CacheManager. It is the entrance to all caches related services
 * from a user's point of view
 */
public final class CacheManager {

    /** The instance. */
    private static CacheManager instance = null;

    /**
	 * Gets the single instance of CacheManager.
	 * 
	 * @return single instance of CacheManager
	 */
    public static synchronized CacheManager getInstance(SensorQuery query) {
        if (instance == null) try {
            init(query);
        } catch (Exception e) {
            System.out.println("Error in cache manager: " + e);
            return null;
        }
        return instance;
    }

    public void killThreads() {
        cache.destroy();
    }

    /**
	 * Initial.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    private static void init(SensorQuery query) throws Exception {
        Properties prop = new Properties();
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("cache.conf");
            if (url == null) {
                url = new URL(GlobalConstant.SensorTypeConstants.CACHE_CONF_FILE_URL);
            }
            System.out.println("URL: " + url);
            prop.load(url.openStream());
        } catch (IOException e) {
            System.err.println("can not initial cache manager");
            throw e;
        }
        instance = new CacheManager(prop);
    }

    /** The cache. */
    private Cache cache = null;

    /** The rule engine. */
    private RuleEngine ruleEngine = null;

    /**
	 * Instantiates a new cache manager.
	 * 
	 * @param prop
	 *            the prop
	 */
    private CacheManager(Properties prop) {
        initRuleEngine(prop);
        composeCacheChain(prop);
    }

    /**
	 * Feed back. value will be stored into cache and feedback into RuleEngine
	 * 
	 * @param id
	 *            the id
	 * @param key
	 *            the key
	 * @param duration
	 *            the duration
	 * @param value
	 *            the value
	 */
    public void feedBack(Integer id, SensorQuery key, Long duration, Object value) {
        System.err.println("IN feedback");
        Cache curr = cache;
        Long t = System.currentTimeMillis();
        while (curr != null) {
            curr.add(key, value);
            curr = curr.next();
        }
        if (ruleEngine != null) {
            ruleEngine.feedback(key, t, value);
        }
    }

    /**
	 * Feed back. this is the version without duration
	 * 
	 * @param id
	 *            the id
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 */
    public void feedBack(Integer id, SensorQuery key, Object value) {
        feedBack(id, key, -1L, value);
    }

    /**
	 * Gets the cached object. If there's no suitable cache value or RuleEngine
	 * decides to let client access sensor network, null is returned.
	 * 
	 * @param id
	 *            the id
	 * @param key
	 *            the key
	 * 
	 * @return the cached object
	 */
    public Object getCachedObject(Integer id, SensorQuery key) {
        Long currTime = System.currentTimeMillis();
        Long lastTime = null;
        Cache curr = cache;
        while (curr != null) {
            lastTime = curr.getLastUpdateTime(key);
            if (lastTime != null) break;
            curr = curr.next();
        }
        if (lastTime != null) {
            if (ruleEngine != null && ruleEngine.bypassCache(key, lastTime, currTime)) {
                System.err.println("DON'T ACCESS CACHE!");
                return null;
            }
        }
        System.err.println("OK ACCESS CACHE");
        Object obj = null;
        curr = cache;
        List<Cache> emptyHits = new ArrayList<Cache>();
        while (curr != null) {
            obj = curr.get(key);
            if (obj != null) {
                break;
            } else {
                emptyHits.add(curr);
                curr = curr.next();
            }
        }
        if (obj != null) {
            Iterator<Cache> iter = emptyHits.iterator();
            while (iter.hasNext()) iter.next().add(key, obj);
        }
        return obj;
    }

    /**
	 * Gets the cached object. This is the one with duration, but not
	 * implemented yet
	 * 
	 * @param id
	 *            the id
	 * @param key
	 *            the key
	 * @param duration
	 *            the duration
	 * 
	 * @return the cached object
	 */
    public Object getCachedObject(Integer id, SensorQuery key, Long duration) throws RuntimeException {
        throw new RuntimeException("This is not implemented yet.");
    }

    public Integer getThreshold() {
        return ruleEngine == null ? null : ruleEngine.getThreshold();
    }

    /**
	 * Compose cache chain.
	 * 
	 * @param prop
	 *            the prop
	 */
    private void composeCacheChain(Properties prop) {
        String chain = prop.getProperty("cache.chain");
        if (chain == null) return;
        String[] chainParts = chain.split(";");
        ObjectFactory factory = ObjectFactory.newInstance();
        Cache obj = null;
        for (int i = chainParts.length - 1; i >= 0; i--) {
            String part = chainParts[i];
            if (i == chainParts.length - 1) obj = (Cache) factory.createObject(part); else obj = (Cache) factory.createObject(part, new Class[] { Cache.class }, new Object[] { obj });
        }
        cache = obj;
    }

    /**
	 * Inits the rule engine.
	 * 
	 * @param prop
	 *            the prop
	 */
    private void initRuleEngine(Properties prop) {
        Boolean useRuleEngine = new Boolean(prop.getProperty("useRuleEngine"));
        if (useRuleEngine) ruleEngine = new RuleEngine(); else ruleEngine = null;
    }
}
