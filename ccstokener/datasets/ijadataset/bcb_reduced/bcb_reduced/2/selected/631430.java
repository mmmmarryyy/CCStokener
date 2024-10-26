package siena;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import siena.logging.SienaLogger;
import siena.logging.SienaLoggerFactory;

/**
 * This is the class responsible of loading
 * <code>PersistenceManager</code> implementations.
 * Each group of persistent classes must be in the same
 * package. That package must have a <code>siena.properties</code>
 * file containing the information of the <code>PersistenceManager</code>
 * to be used.
 *
 * @author gimenete
 *
 */
public class PersistenceManagerFactory {

    private static PersistenceManagerFactory singleton;

    /**
	 * Logger for PersistenceManagerFactory.
	 */
    protected static SienaLogger logger = SienaLoggerFactory.getLogger(PersistenceManagerFactory.class);

    private Map<String, PersistenceManager> configuration;

    public PersistenceManagerFactory() {
        configuration = new ConcurrentHashMap<String, PersistenceManager>();
    }

    public static PersistenceManager getPersistenceManager(Class<?> clazz) {
        return getInstance().get(clazz);
    }

    private static PersistenceManagerFactory getInstance() {
        if (singleton == null) singleton = new PersistenceManagerFactory();
        return singleton;
    }

    private PersistenceManager get(Class<?> clazz) {
        String pack = getPackage(clazz);
        PersistenceManager pm = configuration.get(pack);
        if (pm != null) return pm;
        URL url = clazz.getResource("siena.properties");
        if (url == null) throw new SienaException("Cannot load siena.properties file for package: " + pack);
        Properties p = new Properties();
        try {
            p.load(url.openStream());
        } catch (IOException e) {
            throw new SienaException(e);
        }
        String prefix = "siena." + pack + ".";
        Properties sysprops = System.getProperties();
        for (Map.Entry<Object, Object> entry : sysprops.entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(prefix)) {
                String value = entry.getValue().toString();
                p.setProperty(key.substring(prefix.length()), value);
            }
        }
        String impl = p.getProperty("implementation");
        if (impl == null) throw new SienaException("key 'implementation' not found at " + url);
        try {
            pm = (PersistenceManager) Class.forName(impl).newInstance();
            pm.init(p);
        } catch (Exception e) {
            throw new SienaException("Error while creating instance of: " + impl, e);
        }
        configuration.put(pack, pm);
        return pm;
    }

    public static void install(PersistenceManager pm, Class<?> clazz) {
        getInstance().put(pm, getPackage(clazz));
    }

    private void put(PersistenceManager pm, String pack) {
        configuration.put(pack, pm);
    }

    private static String getPackage(Class<?> clazz) {
        String clazzName = clazz.getName();
        return clazzName.substring(0, clazzName.lastIndexOf('.'));
    }
}
