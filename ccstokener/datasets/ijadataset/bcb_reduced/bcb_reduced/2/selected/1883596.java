package org.jmonit.config;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import org.jmonit.spi.Factory;
import org.jmonit.spi.FeatureManager;

/**
 * @author ndeloof
 */
public class Configuration {

    /**
     * @throws IOException
     */
    private void read() throws IOException {
        Properties props = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        Enumeration<URL> modules = cl.getResources("META-INF/jmonit.properties");
        while (modules.hasMoreElements()) {
            URL url = (URL) modules.nextElement();
            props.load(url.openStream());
        }
        InputStream config = cl.getResourceAsStream("jmonit.properties");
        if (config != null) {
            props.load(config);
        }
    }

    private void registerFeatures(Properties config, FeatureManager manager) throws Exception {
        String features = config.getProperty("features");
        if (features != null) {
            StringTokenizer tokenizer = new StringTokenizer(features, ",");
            while (tokenizer.hasMoreTokens()) {
                String feature = tokenizer.nextToken();
                String factory = config.getProperty(feature + ".factory");
                int dash = factory.indexOf("#");
                String field = null;
                if (dash > 0) {
                    field = factory.substring(dash + 1).trim();
                    factory = factory.substring(0, dash);
                }
                Class factoryClass = Class.forName(factory);
                if (field != null) {
                    Field f = factoryClass.getField(field);
                    if (!Factory.class.isAssignableFrom(f.getType())) {
                    }
                    manager.registerFeature((Factory) f.get(factoryClass));
                } else {
                    if (!Factory.class.isAssignableFrom(factoryClass)) {
                    }
                    manager.registerFeature((Factory) factoryClass.newInstance());
                }
            }
        }
    }
}
