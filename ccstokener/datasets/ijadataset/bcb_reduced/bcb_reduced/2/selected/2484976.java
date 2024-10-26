package org.dcm4chee.xero.metadata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class knows how to provide static meta-data information. This meta-data
 * typically comes from something such as: 1. Seam Theme 2. Static property file
 * on a per-WAR/EAR/EJB JAR file.
 * 
 * @author bwallace
 * 
 */
public class StaticMetaData {

    private static Logger log = LoggerFactory.getLogger(StaticMetaData.class);

    static Map<URL, MetaDataBean> metaDataByUrl = new ConcurrentHashMap<URL, MetaDataBean>();

    /**
	 * Gets the meta data information by name, from the given loader. This will
	 * cause it to be loaded if possible. Seam instances are identified by
	 * ${...} and MUST be application level objects. Simple proprety name
	 * instances are just identified by name. It is possible to have two or more
	 * instances with the same name in separate class loaders. They might have
	 * the same content, but they will be distinct in terms of the actual
	 * objects.
	 * 
	 * @param url is the url to load as a property file into a meta-data object.
	 * @return A cached or new meta-data object for the given location.
	 */
    @SuppressWarnings("unchecked")
    public static synchronized MetaDataBean getMetaDataByUrl(URL url) {
        if (url == null) throw new IllegalArgumentException("Properties url for meta data is null");
        MetaDataBean mdb = metaDataByUrl.get(url);
        if (mdb != null) return mdb;
        log.info("Loading metadata " + url);
        Properties properties = new Properties();
        try {
            properties.load(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mdb = new MetaDataBean((Map) properties);
        metaDataByUrl.put(url, mdb);
        mdb.instanceValue = url.toString();
        return mdb;
    }

    /**
	 * Get the value form the current thread class loader as a string name.
	 * 
	 * @param surl is a string based URL - this will be converted into a URL based version using the class/thread resources.
	 * @return A cached or new meta data bean containing the meta-data.
	 * @throws IllegalArgumentException if surl is null, or if no resources is found by that name.
	 */
    public static MetaDataBean getMetaData(String surl) {
        if (surl == null) throw new IllegalArgumentException("Null string provided for meta-data name.");
        MetaDataBean mdb;
        if (surl.indexOf(":") >= 0) {
            try {
                mdb = getMetaDataByUrl(new URL(surl));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL url = cl.getResource(surl);
            if (url == null) {
                log.debug("Trying static class getReource for " + surl);
                url = StaticMetaData.class.getResource(surl);
            }
            if (url == null) throw new IllegalArgumentException("URL for meta-data name " + surl + " is null.");
            mdb = getMetaDataByUrl(url);
            mdb.instanceValue = surl;
        }
        return mdb;
    }
}
