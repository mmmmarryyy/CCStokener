package de.psisystems.dmachinery.caches;

import net.sf.ehcache.*;
import net.sf.ehcache.CacheManager;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import de.psisystems.dmachinery.core.exeptions.PrintException;

/**
 * Created by IntelliJ IDEA.
 * User: stefanpudig
 * Date: Jul 28, 2009
 * Time: 6:49:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class EHCacheImpl implements Cache {

    private net.sf.ehcache.Cache cache;

    public EHCacheImpl() {
        URL cfg = EHCacheImpl.class.getResource("/ehcache.xml");
        net.sf.ehcache.CacheManager cacheManager = new CacheManager(cfg);
        cache = cacheManager.getCache("EHCacheImpl");
        if (cache == null) {
            throw new IllegalStateException("no cache configured for EHCacheImpl");
        }
    }

    public synchronized Object get(URL key) throws CacheException {
        long modified = getVersion(key);
        Element ce = cache.get(key);
        if (ce != null && modified != ce.getVersion()) {
            ce = null;
            cache.remove(ce);
        }
        return ce != null ? ce.getObjectValue() : null;
    }

    private long getVersion(URL url) throws CacheException {
        URLConnection con;
        try {
            con = url.openConnection();
        } catch (IOException e) {
            throw new CacheException(e);
        }
        long modified = con.getLastModified();
        return modified;
    }

    public synchronized void add(URL url, Object value) throws CacheException {
        long modified = getVersion(url);
        Element element = new Element(url, value);
        element.setVersion(modified);
        cache.put(element);
    }

    public synchronized void remove(URL url) {
        cache.remove(url);
    }

    public synchronized void refresh() {
        cache.removeAll();
    }
}
