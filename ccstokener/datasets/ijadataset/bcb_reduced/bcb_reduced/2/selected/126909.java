package com.danga.memcached;

import java.lang.reflect.Method;
import java.util.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.danga.memcached.tag.CacheTag;

/**
 * Original please visit <a href="http://sourceforge.net/projects/memcachetaglib">memcachetaglib in sf</a><p/>
 * This is the manager of cache, includes cache in memcached server / in memory / in buffer.<p/>
 * Usage Examples:
 * <pre><code>
 * CacheManager admin = CacheManager.getInstance();
 * </code></pre>
 * <p/>
 * All property will be found in file <b>memcached.properties</b>, it should be found
 * in classes file path. <p/>
 *
 *
 * @author <a href="mailto:cytown@gmail.com">Cytown</a>
 * @version 1.09
 */
public class CacheManager {

    private static final transient Log log = LogFactory.getLog(CacheManager.class);

    /**
     * the properties file name
     */
    private static final String PROPERTIES_FILENAME = "/memcached";

    private static final String PROPERTIES_FILENAMEEXT = ".properties";

    /**
     * default name
     */
    private static String DEFAULTNAME = "default";

    private static final String CACHELIST = "cache_list";

    private static final int OPDELETE = -1;

    private static final int OPUPDATE = 1;

    private static final int UPDATEINTERVAL = 3;

    private static Map<String, CacheManager> adminPool = new HashMap<String, CacheManager>();

    private static Set<URLClassLoader> loaders = new HashSet<URLClassLoader>();

    private static URLClassLoader myLoader = new URLClassLoader(new URL[] {}, ClassLoader.getSystemClassLoader());

    public static final int INITCONN = 5;

    public static final int MINCONN = 5;

    public static final int MAXCONN = 50;

    public static final int MAINTSLEEP = 100;

    private final Object ramcachelock = new Object();

    private SockIOPool pool;

    private Properties properties = null;

    private String name;

    private boolean iscompress;

    private boolean encodeKey;

    private boolean hasAdmin;

    private int compressThreshold;

    private int maxcachesize = 100;

    private int secondcachetime = 5;

    private int tagTime = 0;

    private int randomRate = 0;

    private Map<String, Cache> ramCacheList = new WeakHashMap<String, Cache>(10);

    private Map<String, Cache> secondCacheList = new HashMap<String, Cache>(10);

    private static AdminThread adminThread = null;

    private static final String ADMINPOOL = "admin";

    private static Method addURL;

    static {
        try {
            addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        } catch (Exception e) {
        }
        addURL.setAccessible(true);
    }

    /**
     * Construction in default init.
     * @throws IllegalStateException init fail
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    private CacheManager() throws IllegalStateException {
        this(null, null);
    }

    /**
     * Construction in default init.
     * @param name pool name
     * @throws IllegalStateException init fail
     */
    private CacheManager(String name) throws IllegalStateException {
        this(null, name);
    }

    /**
     * Construction.
     *
     * @param p the properties to initial.
     * @param name pool name
     * @throws IllegalStateException init fail
     */
    private CacheManager(Properties p, String name) throws IllegalStateException {
        if (log.isDebugEnabled()) {
            log.debug("MemCache: Config called");
        }
        if (p == null) {
            this.properties = loadProperties(name, "the default configuration");
        } else {
            this.properties = p;
        }
        if (properties == null || properties.isEmpty()) {
            throw new IllegalStateException("No or Error properties file!");
        }
        this.name = name;
        init();
        if ("default".equals(DEFAULTNAME) && !"admin".equals(get("name"))) DEFAULTNAME = get("name");
    }

    private void init() throws IllegalStateException {
        String s = get("serverlist");
        if (name == null || "default".equals(name)) name = get("name");
        if (name.equals("")) {
            log.error("server name can not be empty!");
            return;
        }
        int initconn = parseInt(get("initconn"), INITCONN);
        int minconn = parseInt(get("minconn"), MINCONN);
        int maxconn = parseInt(get("maxconn"), MAXCONN);
        int maintsleep = parseInt(get("maintsleep"), MAINTSLEEP);
        secondcachetime = parseInt(get("secondcachetime"), secondcachetime);
        tagTime = parseInt(get("tagTime"), tagTime);
        randomRate = parseInt(get("randomRate"), randomRate);
        boolean alivecheck = "true".equals(get("alivecheck"));
        boolean nagle = "true".equals(get("nagle"));
        iscompress = "true".equals(get("compressEnable"));
        encodeKey = "true".equals(get("encodeKey"));
        if (!name.equals(ADMINPOOL)) hasAdmin = "true".equals(get("admin"));
        compressThreshold = parseInt(get("compressThreshold"), -1);
        String[] serverlist = s.split("\\s");
        if (serverlist.length == 0) {
            log.error("serverlist can not be null!");
            return;
        }
        s = get("weights");
        Integer[] weights = new Integer[serverlist.length];
        String[] sweights = new String[] {};
        if (!s.equals("")) sweights = s.split("\\s");
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (i >= sweights.length) ? 1 : parseInt(sweights[i], 1);
        }
        pool = SockIOPool.getInstance(name);
        pool.setServers(serverlist);
        pool.setWeights(weights);
        pool.setInitConn(name.equals(ADMINPOOL) ? 1 : initconn);
        pool.setMinConn(name.equals(ADMINPOOL) ? 1 : minconn);
        pool.setMaxConn(name.equals(ADMINPOOL) ? 5 : maxconn);
        pool.setMaintSleep(maintsleep);
        pool.setAliveCheck(alivecheck);
        pool.setNagle(nagle);
        pool.initialize();
        if (hasAdmin) initThread();
        maxcachesize = parseInt(get("maxcachesize"), maxcachesize);
    }

    private static int parseInt(String s, int iDefault) {
        if (s == null || s.equals("")) return iDefault;
        try {
            s = s.replaceAll(",", "");
            int l = s.indexOf(".");
            if (l > 0) s = s.substring(0, l);
            return Integer.parseInt(s);
        } catch (Exception e) {
            return iDefault;
        }
    }

    /**
     * Check if it has the admin thread.
     * @return true if has.
     */
    public boolean isHasAdmin() {
        return hasAdmin;
    }

    /**
     * Get the admin pool list
     * @return the pool list
     */
    public Set<String> getAdminPool() {
        return adminPool.keySet();
    }

    /**
     * Get the cached key list in RAM.
     * @return the cache key set.
     */
    public Set<String> getRamCacheKeys() {
        return ramCacheList.keySet();
    }

    /**
     * Get the cache list in RAM.
     * @return the ram cache map.
     */
    public Map<String, Cache> getRamCacheList() {
        return Collections.unmodifiableMap(ramCacheList);
    }

    /**
     * Check whether it's connected.
     * @return true for connected.
     */
    public boolean isConnected() {
        return pool != null && pool.isInitialized();
    }

    /**
     * Get the manager name.
     * @return the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the tag time
     * @return the tagtime
     */
    public int getTagTime() {
        return tagTime;
    }

    /**
     * return the random rate to force reload the tag
     * @return random rate
     */
    public int getRandomRate() {
        return randomRate;
    }

    /**
     * Get all the cache list in memcache server.
     * @return the cache list map.
     */
    public Map<String, Long> getCacheList() {
        if (!hasAdmin) return new HashMap<String, Long>();
        initThread();
        try {
            return adminThread.getCacheList();
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Long> forceUpdateCacheList() {
        if (!hasAdmin) return new HashMap<String, Long>();
        Map<String, Long> ret = new HashMap<String, Long>();
        initThread();
        adminThread.updateCacheList(ret);
        return ret;
    }

    private synchronized void checkCache(int operation, Cache cache) {
        if (!hasAdmin) return;
        if (operation != OPDELETE && operation != OPUPDATE) return;
        initThread();
        adminThread.addQueue(new CacheQueue(operation, cache));
    }

    /**
     * shut down the pool.
     */
    public void shutdown() {
        if (pool != null) pool.shutDown();
        pool = null;
    }

    /**
     * shut down all the pools.
     */
    public void shutdownAll() {
        for (CacheManager c : adminPool.values()) {
            c.shutdown();
        }
    }

    private static void addLoader(Class clazz) {
        URLClassLoader cl = (URLClassLoader) clazz.getClassLoader();
        if (addLoader(cl)) {
            addLoader((URLClassLoader) clazz.getClassLoader().getParent());
        }
    }

    private static boolean addLoader(URLClassLoader cl) {
        if (cl == null || loaders.contains(cl)) {
            return false;
        }
        URL[] classPath = cl.getURLs();
        for (URL url : classPath) {
            try {
                addURL.invoke(myLoader, url);
            } catch (Exception e) {
            }
        }
        loaders.add(cl);
        return true;
    }

    /**
     * get the MemCachedClient from the pool.
     * @return MemCachedClient
     */
    public MemCachedClient getClient() {
        MemCachedClient mc = new MemCachedClient(name);
        mc.setCompressEnable(iscompress);
        mc.setSanitizeKeys(encodeKey);
        if (compressThreshold >= 0) mc.setCompressThreshold((long) compressThreshold * 1024);
        return mc;
    }

    private void checkRamCache() {
        for (Iterator<Map.Entry<String, Cache>> iter = ramCacheList.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, Cache> entry = iter.next();
            if (entry.getValue().isTimeOut()) {
                iter.remove();
            }
        }
        if (ramCacheList.size() > maxcachesize) {
            ramCacheList.remove(ramCacheList.keySet().iterator().next());
        }
    }

    /**
     * Set the cache in memcached server or ram.
     * @param cache cache to be stored or updated.
     */
    public void setCache(Cache cache) {
        setCache(cache, 4, false);
    }

    /**
     * Set the cache in memcached server or ram.
     * @param cache cache to be stored or updated
     * @param type the type, 4 is application, 3 is in ram
     */
    public void setCache(Cache cache, int type) {
        setCache(cache, type, false);
    }

    /**
     * Set the cache in memcached server or ram.
     * @param cache cache to be stored or updated
     * @param type the type, 4 is application, 3 is in ram
     * @param secondcache whether second cache enabled, only valid for type 4.
     */
    public void setCache(Cache cache, int type, boolean secondcache) {
        if (type == 4) {
            MemCachedClient mc = getClient();
            mc.set(name + cache.getCacheKey(), cache.getObject(), cache.getExpireTime());
            cache.setPrekey(name);
            if (!cache.getKey().startsWith(CacheTag.TAG_TIME_PREFIX)) {
                checkCache(OPUPDATE, cache);
            }
            if (secondcache) {
                cache.setTime(secondcachetime);
                setCache(cache, 3, true);
            }
        } else {
            synchronized (ramcachelock) {
                if (!secondcache) {
                    ramCacheList.put(cache.getCacheKey(), cache);
                    checkRamCache();
                } else {
                    secondCacheList.put(cache.getCacheKey(), cache);
                }
            }
        }
    }

    /**
     * Get the cache from memcached server or ram.
     * @param cache the cache to be retrived.
     * @return the cache.
     */
    public Cache getCache(Cache cache) {
        return getCache(cache, 4, false);
    }

    /**
     * Get the cache from memcached server or ram.
     * @param cache the cache to be retrived.
     * @param type the type, 4 is application, 3 is in ram
     * @return the cache.
     */
    public Cache getCache(Cache cache, int type) {
        return getCache(cache, type, false);
    }

    /**
     * Get the cache from memcached server or ram.
     * @param cache the cache to be retrived.
     * @param type the type, 4 is application, 3 is in ram
     * @param secondcache whether second cache enabled, only valid for type 4.
     * @return the cache.
     */
    public Cache getCache(Cache cache, int type, boolean secondcache) {
        if (type == 4) {
            if (secondcache) {
                Cache c = getCache(cache, 3, true);
                if (c != null) return c;
            }
            String poolname = cache.getPrekey() != null ? cache.getPrekey() : name;
            MemCachedClient mc = CacheManager.getInstance(poolname).getClient();
            if (cache.getClassLoader() != null) mc.setClassLoader(cache.getClassLoader());
            Serializable o = (Serializable) mc.get(poolname + cache.getCacheKey());
            if (o == null) return null;
            cache.setObject(o);
            if (secondcache) {
                cache.setTime(secondcachetime);
                setCache(cache, 3, true);
            }
            return cache;
        } else {
            Cache c;
            if (!secondcache) {
                c = ramCacheList.get(cache.getCacheKey());
            } else {
                c = secondCacheList.get(cache.getCacheKey());
            }
            if (c == null) return null;
            if (c.isTimeOut()) {
                synchronized (ramcachelock) {
                    ramCacheList.remove(cache.getCacheKey());
                }
                return null;
            }
            return c;
        }
    }

    private String get(Object key) {
        Object o = properties.get(key);
        if (o == null) return "";
        return o.toString();
    }

    private static Properties loadProperties(URL url, String info) {
        if (log.isInfoEnabled()) {
            log.info("MemCache: Getting properties from URL " + url + " for " + info);
        }
        Properties properties = new Properties();
        InputStream in = null;
        try {
            in = url.openStream();
            properties.load(in);
            if (log.isInfoEnabled()) {
                log.info("MemCache: Properties read " + properties);
            }
        } catch (Exception e) {
            log.error("MemCache: Error reading from " + url, e);
            log.error("MemCache: Ensure the properties information in " + url + " is readable and in your classpath.");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.warn("MemCache: IOException while closing InputStream: " + e.getMessage());
            }
        }
        return properties;
    }

    private static Properties loadProperties(String filename, String info) {
        URL url = null;
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        filename = PROPERTIES_FILENAME + "-" + filename + PROPERTIES_FILENAMEEXT;
        if (threadContextClassLoader != null) {
            url = threadContextClassLoader.getResource(filename);
            if (url == null) url = threadContextClassLoader.getResource(PROPERTIES_FILENAME + PROPERTIES_FILENAMEEXT);
        }
        if (url == null) {
            url = CacheManager.class.getResource(filename);
            if (url == null) {
                url = CacheManager.class.getResource(PROPERTIES_FILENAME + PROPERTIES_FILENAMEEXT);
                if (url == null) {
                    log.warn("MemCache: No properties file found in the classpath by filename " + PROPERTIES_FILENAME + PROPERTIES_FILENAMEEXT);
                    return new Properties();
                }
            }
        }
        return loadProperties(url, info);
    }

    /**
     * get the instance in default name
     * @return cache manager
     */
    public static CacheManager getInstance() {
        return getInstance(null);
    }

    /**
     * get the instance in target name, if not exists, it will create new.
     * @param name the name of pool
     * @return cache manager
     * @throws IllegalStateException if create fail.
     */
    public static CacheManager getInstance(String name) throws IllegalStateException {
        if (name == null) name = DEFAULTNAME;
        CacheManager admin = adminPool.get(name);
        if (admin == null || !admin.isConnected()) {
            try {
                admin = new CacheManager(name);
            } catch (IllegalStateException e) {
                log.error(name + "==>" + e.getMessage());
                throw e;
            }
            adminPool.put(admin.name, admin);
        }
        return admin;
    }

    /**
     * delete the cache in memcached server or ram
     * @param cache the cache to be deleted
     */
    public void deleteCache(Cache cache) {
        deleteCache(cache, 4, false);
    }

    /**
     * delete the cache in memcached server or ram
     * @param cache the cache to be deleted
     * @param type the type, 4 is application, 3 is in ram
     */
    public void deleteCache(Cache cache, int type) {
        deleteCache(cache, type, false);
    }

    /**
     * delete the cache in memcached server or ram
     * @param cache the cache to be deleted
     * @param type the type, 4 is application, 3 is in ram
     * @param secondcache whether second cache enabled, only valid for type 4.
     */
    public void deleteCache(Cache cache, int type, boolean secondcache) {
        if (type == 4) {
            String poolname = cache.getPrekey() != null ? cache.getPrekey() : name;
            MemCachedClient mc = CacheManager.getInstance(poolname).getClient();
            mc.delete(poolname + cache.getCacheKey());
            checkCache(OPDELETE, cache);
            if (secondcache) secondCacheList.remove(cache.getCacheKey());
        } else {
            synchronized (ramcachelock) {
                ramCacheList.remove(cache.getCacheKey());
                checkRamCache();
            }
        }
    }

    private void initThread() {
        if (adminThread == null) {
            adminThread = new AdminThread(CacheManager.getInstance(ADMINPOOL));
            ThreadUtil.uniqueThreadStart(adminThread, "CacheManagerAdminThread");
        } else if (!adminThread.isAlive()) {
            adminThread = new AdminThread(CacheManager.getInstance(ADMINPOOL));
            adminThread.setName("CacheManagerAdminThread");
            adminThread.setDaemon(true);
            adminThread.start();
        }
    }

    private static class AdminThread extends Thread {

        static volatile List<CacheQueue> queueList = new ArrayList<CacheQueue>();

        CacheManager admin;

        static final Object lock = new Object();

        AdminThread(CacheManager admin) {
            this.admin = admin;
        }

        public void addQueue(CacheQueue q) {
            synchronized (lock) {
                queueList.add(q);
            }
        }

        public Map<String, Long> getCacheList() throws Exception {
            return (Map<String, Long>) getCacheList(admin.getClient()).getObject();
        }

        private GetsObject getCacheList(MemCachedClient mc) throws Exception {
            long currentTime = new Date().getTime();
            try {
                GetsObject ret = mc.gets(DEFAULTNAME + "/" + CACHELIST);
                if (ret.getCascode() <= 0) return new GetsObject(0, new HashMap<String, Long>());
                Map<String, Long> set = (Map<String, Long>) ret.getObject();
                for (Iterator<Map.Entry<String, Long>> iter = set.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<String, Long> entry = iter.next();
                    if (entry.getValue() < currentTime) {
                        iter.remove();
                    }
                }
                return ret;
            } catch (NullPointerException e) {
                return new GetsObject(0, new HashMap<String, Long>());
            }
        }

        public boolean updateCacheList(Map<String, Long> cacheList) {
            return updateCacheList(new ArrayList<CacheQueue>(), cacheList);
        }

        private boolean updateCacheList(List<CacheQueue> copylist, Map<String, Long> cacheList) {
            boolean finish = false;
            if (log.isInfoEnabled()) {
                log.info("AdminThread: update " + copylist.size() + " start");
            }
            Map<String, Long> set = null;
            do {
                MemCachedClient mc = admin.getClient();
                GetsObject gets;
                try {
                    gets = getCacheList(mc);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    break;
                }
                set = (Map<String, Long>) gets.getObject();
                for (CacheQueue cq : copylist) {
                    if (cq.operate == OPDELETE) {
                        set.remove(cq.cache.getFullKey());
                    } else if (cq.operate == OPUPDATE) {
                        set.put(cq.cache.getFullKey(), cq.cache.getExpireTime().getTime());
                    }
                }
                if (gets.getCascode() <= 0) {
                    finish = mc.set(DEFAULTNAME + "/" + CACHELIST, set);
                } else {
                    finish = mc.cas(DEFAULTNAME + "/" + CACHELIST, set, gets.getCascode());
                }
                if (!finish) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } while (!finish);
            if (finish) {
                if (cacheList != null && cacheList.size() == 0) {
                    cacheList.putAll(set);
                }
                if (log.isInfoEnabled()) {
                    log.info("AdminThread: update end");
                }
            }
            return finish;
        }

        public void run() {
            if (log.isInfoEnabled()) {
                log.info("AdminThread: start");
            }
            try {
                while (true) {
                    Thread.yield();
                    if (queueList.size() > 0) {
                        final List<CacheQueue> copylist;
                        synchronized (lock) {
                            copylist = new ArrayList<CacheQueue>(queueList.size());
                            for (Iterator<CacheQueue> it = queueList.iterator(); it.hasNext(); ) {
                                copylist.add(it.next());
                                it.remove();
                            }
                        }
                        if (copylist.size() == 0) continue;
                        boolean finish = updateCacheList(copylist, null);
                        if (!finish) {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(UPDATEINTERVAL * 1000);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                admin.shutdownAll();
                if (log.isInfoEnabled()) {
                    log.info("AdminThread: quit!");
                }
            }
        }
    }

    class CacheQueue {

        int operate;

        Cache cache;

        public CacheQueue(int operate, Cache cache) {
            this.operate = operate;
            this.cache = cache.copy();
        }

        public String toString() {
            return "CacheQueue{" + "operate=" + operate + ", cache=" + cache + '}';
        }
    }
}
