package games.midhedava.client;

import games.midhedava.client.sprite.SpriteStore;
import games.midhedava.common.Debug;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;
import marauroa.common.Configuration;
import marauroa.common.io.Persistence;
import marauroa.common.net.TransferContent;
import org.apache.log4j.Logger;

/**
 * <p>Manages a two level cache which one or both levels are optional:</p>
 *
 * <p>The first level is prefilled readonly cache in a .jar file on class path.
 * At the time of writing we use this for Webstart because we are unsure
 * how large the webstart PersistenceService may grow.</p>
 *
 * <p>The second level is a normal cache on filesystem.</p>
 */
public class Cache {

    private static Logger logger = Logger.getLogger(Cache.class);

    private Configuration cacheManager;

    private Properties prefilledCacheManager;

    /**
	 * inits the cache
	 */
    public void init() {
        try {
            prefilledCacheManager = new Properties();
            URL url = SpriteStore.get().getResourceURL("cache/midhedava.cache");
            if (url != null) {
                InputStream is = url.openStream();
                prefilledCacheManager.load(is);
                is.close();
            }
            if (!Debug.WEB_START_SANDBOX) {
                File file = new File(System.getProperty("user.home") + "/" + midhedava.MIDHEDAVA_FOLDER);
                if (!file.exists() && !file.mkdir()) {
                    logger.error("Can't create " + file.getAbsolutePath() + " folder");
                } else if (file.exists() && file.isFile()) {
                    if (!file.delete() || !file.mkdir()) {
                        logger.error("Can't removing file " + file.getAbsolutePath() + " and creating a folder instead.");
                    }
                }
                file = new File(System.getProperty("user.home") + midhedava.MIDHEDAVA_FOLDER + "cache/");
                if (!file.exists() && !file.mkdir()) {
                    logger.error("Can't create " + file.getAbsolutePath() + " folder");
                }
                String cacheFile = System.getProperty("user.home") + midhedava.MIDHEDAVA_FOLDER + "cache/midhedava.cache";
                new File(cacheFile).createNewFile();
            }
            Configuration.setConfigurationFile(true, midhedava.MIDHEDAVA_FOLDER, "cache/midhedava.cache");
            cacheManager = Configuration.getConfiguration();
        } catch (Exception e) {
            logger.error("cannot create Midhedava Client", e);
        }
    }

    private InputStream getItemFromPrefilledCache(TransferContent item) {
        String name = "cache/" + item.name;
        String timestamp = prefilledCacheManager.getProperty(item.name);
        if ((timestamp != null) && (Integer.parseInt(timestamp) == item.timestamp)) {
            URL url = SpriteStore.get().getResourceURL(name);
            if (url != null) {
                try {
                    logger.debug("Content " + item.name + " is in prefilled cache.");
                    return url.openStream();
                } catch (IOException e) {
                    logger.error(e, e);
                }
            }
        }
        return null;
    }

    private InputStream getItemFromCache(TransferContent item) {
        if (cacheManager.has(item.name) && (Integer.parseInt(cacheManager.get(item.name)) == item.timestamp)) {
            logger.debug("Content " + item.name + " is on cache. We save transfer");
            try {
                return Persistence.get().getInputStream(true, midhedava.MIDHEDAVA_FOLDER + "cache/", item.name);
            } catch (IOException e) {
                logger.error(e, e);
            }
        }
        return null;
    }

    /**
	 * Gets an item from cache
	 *
	 * @param item key
	 * @return InputStream or null if not in cache
	 */
    public InputStream getItem(TransferContent item) {
        InputStream is = getItemFromPrefilledCache(item);
        if (is == null) {
            is = getItemFromCache(item);
        }
        return is;
    }

    /**
	 * Stores an item in cache
	 *
	 * @param item key
	 * @param data data
	 */
    public void store(TransferContent item, byte[] data) {
        try {
            OutputStream os = Persistence.get().getOutputStream(true, midhedava.MIDHEDAVA_FOLDER + "cache/", item.name);
            os.write(data);
            os.close();
            logger.debug("Content " + item.name + " cached now. Timestamp: " + Integer.toString(item.timestamp));
            cacheManager.set(item.name, Integer.toString(item.timestamp));
        } catch (java.io.IOException e) {
            logger.error("store", e);
        }
    }
}
