package com.totalchange.wtframework.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.totalchange.wtframework.tester.WtfTesterFactory;

/**
 * Responsible for loading and managing plugins for the various parts of WTF
 * which require them.
 * 
 * @author Ralph Jones
 */
public final class WtfPluginManager {

    private static final String PLUGIN_PROPERTIES = "META-INF/wtframework.properties";

    private static final Logger logger = LoggerFactory.getLogger(WtfPluginManager.class);

    private static WtfPluginManager instance = new WtfPluginManager();

    private List<WtfPlugin> plugins = new ArrayList<WtfPlugin>();

    ;

    /**
     * Grabs configuration information about plugins.
     */
    private WtfPluginManager() {
        Enumeration<URL> pluginPropertyUrls = null;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = this.getClass().getClassLoader();
            }
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            logger.debug("Looking for all available plugins ({}) on " + "class path using class loader {}", PLUGIN_PROPERTIES, classLoader);
            pluginPropertyUrls = classLoader.getResources(PLUGIN_PROPERTIES);
        } catch (IOException ioEx) {
            logger.error("Problem loading plugins", ioEx);
            throw new RuntimeException("Problem loading plugins", ioEx);
        }
        if (pluginPropertyUrls != null) {
            while (pluginPropertyUrls.hasMoreElements()) {
                URL url = pluginPropertyUrls.nextElement();
                try {
                    addPlugin(url);
                } catch (IOException ioEx) {
                    logger.error("Failed to load plugin from URL " + url + " with error " + ioEx.getMessage(), ioEx);
                }
            }
        }
    }

    /**
     * Adds a plugin for a given URL.
     * 
     * @param url
     *            the URL of the properties file describing the plugin.
     * @throws IOException
     *             if fail to read the properties from the URL.
     */
    private void addPlugin(URL url) throws IOException {
        logger.debug("Adding plugin with URL {}", url);
        InputStream in = url.openStream();
        try {
            Properties properties = new Properties();
            properties.load(in);
            plugins.add(new WtfPlugin(properties));
        } finally {
            in.close();
        }
    }

    /**
     * Gets all the tester factories exposed via plugins.
     * 
     * @return all the tester factories.
     */
    public List<WtfTesterFactory> getTesterFactories() {
        List<WtfTesterFactory> factories = new ArrayList<WtfTesterFactory>();
        for (WtfPlugin plugin : plugins) {
            if (plugin.getTesterFactory() != null) {
                factories.add(plugin.getTesterFactory());
            }
        }
        return factories;
    }

    /**
     * Gets the plugin manager instance.
     * 
     * @return the plugin manager instance.
     */
    public static WtfPluginManager getInstance() {
        return instance;
    }
}
