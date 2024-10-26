package fi.arcusys.qnet.common.dao;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create {@link ResourceFileStorage} instances based on the current 
 * configuration.
 * 
 * <p>The configuration is defined in resources <code>/setup.properties</code>
 * and <code>/setup.local.properties</code> (the latter overrides the first).
 * If properties are defined in files, JVM system properties with equal names
 * are used.
 * Following properties are read:</p>
 * 
 * <ul>
 * <li><code>qnet.ResourceFileStorage.class</code> defines the 
 *     {@link ResourceFileStorage} implementation class to instantiate</li>
 * <li><code>qnet.ResourceFileStorage.&lt;property name&gt;</code> defines
 *     properties for the storage implementation instance</li>
 * </ul>
 * 
 * 
 * 
 * @author mikko
 * @version 1.0 $Rev: 553 $
 */
public class ResourceFileStorageFactory {

    private static final Log log = LogFactory.getLog(ResourceFileStorageFactory.class);

    private static ResourceFileStorageFactory instance;

    public static final String PROPERTIES_RESOURCE = "/setup.properties";

    public static final String LOCAL_PROPERTIES_RESOURCE = "/setup.local.properties";

    public static final String PROPERTY_PREFIX = "qnet.ResourceFileStorage.";

    public static final String PROPERTY_CLASS = PROPERTY_PREFIX + "class";

    private String className;

    private Map<String, String> initProperties;

    private static void loadProperties(Properties props, String res, boolean warnIfNotFound) throws IOException {
        log.debug("Reading properties from resource " + res);
        URL url = ResourceFileStorageFactory.class.getResource(res);
        if (null == url) {
            if (warnIfNotFound) {
                log.warn("Resource " + res + " was not found");
            } else {
                log.debug("Resource " + res + " was not found");
            }
        } else {
            InputStream in = url.openStream();
            try {
                props.load(in);
            } finally {
                in.close();
            }
        }
    }

    private ResourceFileStorageFactory() throws IOException {
        log.debug("Initializing");
        Properties sysProps = System.getProperties();
        Properties props = new Properties(sysProps);
        loadProperties(props, PROPERTIES_RESOURCE, true);
        loadProperties(props, LOCAL_PROPERTIES_RESOURCE, false);
        for (Object ko : sysProps.keySet()) {
            String k = ko.toString();
            if (k.startsWith(PROPERTY_PREFIX)) {
                String v = sysProps.getProperty(k);
                props.put(k, v);
            }
        }
        this.className = props.getProperty(PROPERTY_CLASS);
        if (log.isDebugEnabled()) {
            log.debug("qnet.ResourceFileStorage.class = " + className);
        }
        if (null == className || 0 == className.length()) {
            throw new IOException("Invalid class name for 'qnet.ResourceFileStorage.class': " + className);
        }
        props.remove(PROPERTY_CLASS);
        initProperties = new HashMap<String, String>();
        for (Object ko : props.keySet()) {
            String k = ko.toString();
            if (k.startsWith(PROPERTY_PREFIX)) {
                String val = props.getProperty(k);
                k = k.substring(PROPERTY_PREFIX.length());
                initProperties.put(k, val);
                if (log.isDebugEnabled()) {
                    log.debug("Found init property: " + k + " = " + val);
                }
            }
        }
    }

    public static synchronized ResourceFileStorageFactory getInstance() {
        if (null == instance) {
            try {
                instance = new ResourceFileStorageFactory();
            } catch (IOException ex) {
                log.error("Failed to initialize instance", ex);
            }
        }
        return instance;
    }

    public synchronized ResourceFileStorage openResourceFileStorage() throws IOException {
        ResourceFileStorage rfs = null;
        if (log.isDebugEnabled()) {
            log.debug("Trying to create instance of class: " + className);
        }
        try {
            Class<?> cls = Class.forName(className);
            if (!ResourceFileStorage.class.isAssignableFrom(cls)) {
                String msg = "Class '" + className + "' does not implement ResourceFileStorage interface";
                log.error(msg);
                throw new IOException(msg);
            }
            ResourceFileStorage o = (ResourceFileStorage) cls.newInstance();
            log.debug("Configuring created ResourceFileStorage");
            o.initialize(Collections.unmodifiableMap(initProperties));
            rfs = o;
        } catch (ClassNotFoundException ex) {
            String msg = "ResourceFileStorage class not found: " + className;
            log.error(msg);
            throw new IOException(msg);
        } catch (IllegalAccessException ex) {
            String msg = "ResourceFileStorage class is not accessible: " + className;
            log.error(msg);
            throw new IOException(msg);
        } catch (InstantiationException ex) {
            String msg = "ResourceFileStorage class can not be instantiated: " + className;
            log.error(msg);
            throw new IOException(msg);
        }
        return rfs;
    }
}
