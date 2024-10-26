package metso.dal;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * Classe helper per l'istanziazione di un connection manager.
 */
public class ConnectionManagerHelper {

    private static Logger log = Logger.getLogger(ConnectionManagerHelper.class);

    private static String configurationPath = "";

    ;

    private static ConnectionManager connectionManager;

    /**
	 * Istanzia e restituisce il connection manager specificato su file di
	 * configurazione.
	 * 
	 * @return ConnectionManager
	 * @throws ConnectionManagerException
	 */
    @SuppressWarnings("unchecked")
    public static ConnectionManager getConnectionManager() throws ConnectionManagerException {
        if (connectionManager == null) {
            Properties conf = loadConfiguration();
            String className = conf.getProperty("connectionmanager.class");
            try {
                Class<ConnectionManager> connectionManagerClass = (Class<ConnectionManager>) Class.forName(className);
                connectionManager = connectionManagerClass.newInstance();
            } catch (Throwable cnfe) {
                log.error("Errore nell'istanziazione del connectionmanager: ", cnfe);
            }
        }
        return connectionManager;
    }

    private static Properties loadConfiguration() {
        log.debug("Caricamento configurazione");
        Properties properties = null;
        log.debug("Caricamento configurazione non riuscito, ricerca file nel working folder.");
        if (properties == null) {
            properties = new Properties();
            File file = new File((configurationPath.equals("") ? "" : configurationPath + "/") + "configuration.properties");
            try {
                Reader reader = new FileReader(file);
                properties.load(reader);
                reader.close();
            } catch (Throwable e) {
                log.error("Caricamento della configurazione dal working folder non riuscito", e);
            }
        }
        return properties;
    }

    public static Properties loadProperties(String name) {
        String suffisso = ".properties";
        if (name.startsWith("/")) name = name.substring(1);
        if (name.endsWith(suffisso)) name = name.substring(0, name.length() - suffisso.length());
        log.debug("Caricamento come resourcebundle");
        Properties result = loadPropertiesAsResourceBundle(name);
        log.debug("Caricamento come resource stream");
        if (result == null) {
            result = loadPropertiesFromResourceAsStream(name, suffisso);
        }
        log.debug("Caricamento dal classpath");
        if (result == null) {
            result = loadPropertiesFromClassPath(name, suffisso);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Properties loadPropertiesAsResourceBundle(String name) {
        Properties result = null;
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        ResourceBundle rb = null;
        try {
            rb = ResourceBundle.getBundle(name, Locale.getDefault(), loader);
        } catch (Exception e) {
            log.error("Errore nel caricamento come resourcebundle: ", e);
            return null;
        }
        result = new Properties();
        for (Enumeration keys = rb.getKeys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            String value = rb.getString(key);
            result.put(key, value);
        }
        return result;
    }

    private static Properties loadPropertiesFromResourceAsStream(String name, String suffisso) {
        InputStream in = null;
        Properties result = null;
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            name = name.replace('.', '/');
            if (!name.endsWith(suffisso)) name = name.concat(suffisso);
            in = loader.getResourceAsStream(name);
            log.debug("Inputstream caricato");
            if (in != null) {
                result = new Properties();
                log.debug("Caricamento della configurazione dallo stream");
                result.load(in);
            }
        } catch (Throwable e) {
            log.error("Errore nel caricamento come resource stream: ", e);
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    private static Properties loadPropertiesFromClassPath(String name, String suffisso) {
        Properties props = null;
        try {
            props = new Properties();
            URL url = ClassLoader.getSystemResource(name + suffisso);
            props.load(url.openStream());
            return props;
        } catch (Exception e) {
            log.error("Errore nel caricamento come system resource: ", e);
        }
        return null;
    }

    public static void setConfigurationPath(String configurationPath) {
        ConnectionManagerHelper.configurationPath = configurationPath;
    }

    public static String getConfigurationPath() {
        return configurationPath;
    }
}
