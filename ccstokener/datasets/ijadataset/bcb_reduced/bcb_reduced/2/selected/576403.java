package mwt.xml.xdbforms.cache.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import mwt.xml.xdbforms.cache.CacheService;
import mwt.xml.xdbforms.cache.CacheServiceFactory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Progetto Master Web Technology
 * @author Gianfranco Murador, Cristian Castiglia, Matteo Ferri
 * implementa la classe astratta CacheServiceFactory
 * @see CacheServiceFactory
 */
public class CacheServiceFactoryImpl extends CacheServiceFactory {

    private CacheManager cacheManager;

    public CacheServiceFactoryImpl() {
        @SuppressWarnings("static-access") URL url = this.getClass().getClassLoader().getResource("mwt/xml/xdbforms/configuration/ehcache.xml");
        InputStream is;
        try {
            is = url.openStream();
            cacheManager = CacheManager.create(is);
        } catch (IOException ex) {
            System.err.println("NOn riesco ad aprire il file di configurazione ehcache.xml");
        }
    }

    /**
     * Gli oggetti nella cache dei documenti xforms
     * vengono memorizzati in un BufferedOutputStream
     * @return
     */
    @Override
    public CacheService newXFormCacheService() {
        Cache cache = cacheManager.getCache("xform-cache");
        return new XFormCacheService(cache);
    }

    /**
     * @see CacheServiceFactory
     * @return
     */
    @Override
    public CacheService newXSchemaCacheService() {
        Cache cache = cacheManager.getCache("xschema-cache");
        return new XSchemaCacheService(cache);
    }

    /**
     * @see CacheServiceFactory
     * @return
     */
    @Override
    public CacheService newDataInstanceCacheService() {
        Cache cache = cacheManager.getCache("datainstance-cache");
        return new DataInstanceCacheService(cache);
    }

    @Override
    protected void finalize() throws Throwable {
    }
}
