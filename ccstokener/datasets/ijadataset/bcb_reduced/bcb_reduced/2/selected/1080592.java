package de.zeitfuchs.networkfinder.core.internal.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import de.zeitfuchs.networkfinder.core.IPlugin;

public class PluginLoader<P extends IPlugin> {

    private Map<String, P> loadedPlugins;

    private String pluginConfigFile;

    private File pluginDirectory;

    private String pluginFactory;

    public PluginLoader(String pluginConfigFile, String pluginFactory, File pluginDirectory) {
        this.pluginFactory = pluginFactory;
        this.pluginConfigFile = pluginConfigFile;
        this.pluginDirectory = pluginDirectory;
    }

    public Collection<P> getPlugins() {
        if (loadedPlugins == null) {
            loadedPlugins = new HashMap<String, P>();
            loadPlugins();
        }
        return loadedPlugins.values();
    }

    private void addPlugins(P[] plugins) {
        for (int i = 0; i < plugins.length; i++) {
            P plugin = plugins[i];
            String name = plugin.getUniqueName();
            if (!loadedPlugins.containsKey(name)) loadedPlugins.put(name, plugin);
        }
    }

    private ClassLoader createJarClassLoader(File jar) {
        try {
            return new URLClassLoader(new URL[] { jar.toURL() });
        } catch (Exception ex) {
            return null;
        }
    }

    private P[] loadFactoryPlugins(String factoryName) {
        return loadFactoryPlugins(factoryName, getClass().getClassLoader());
    }

    private P[] loadFactoryPlugins(String factoryName, ClassLoader loader) {
        try {
            Class factoryClass = loader.loadClass(factoryName);
            IPluginFactory<P> factory = (IPluginFactory<P>) factoryClass.newInstance();
            return factory.createPlugins();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void loadPlugins() {
        try {
            Enumeration e = getClass().getClassLoader().getResources(pluginConfigFile);
            while (e.hasMoreElements()) {
                Properties properties = new Properties();
                URL url = (URL) e.nextElement();
                InputStream is = url.openStream();
                try {
                    properties.load(is);
                    String factoryName = properties.getProperty(pluginFactory);
                    if (factoryName != null) {
                        P[] plugins = loadFactoryPlugins(factoryName);
                        if (plugins != null) addPlugins(plugins);
                    }
                } finally {
                    is.close();
                }
            }
            File pluginPath = pluginDirectory;
            File[] jars = pluginPath.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    String fileName = file.getName().toLowerCase();
                    return (file.isFile() && fileName.endsWith(".jar"));
                }
            });
            for (int i = 0; i < jars.length; i++) {
                JarFile jarFile = new JarFile(jars[i]);
                try {
                    ZipEntry entry = jarFile.getEntry(pluginConfigFile);
                    if (entry != null) {
                        Properties properties = new Properties();
                        InputStream is = jarFile.getInputStream(entry);
                        try {
                            properties.load(is);
                            String factoryName = properties.getProperty(pluginFactory);
                            if (factoryName != null) {
                                P[] plugins = loadFactoryPlugins(factoryName, createJarClassLoader(jars[i]));
                                if (plugins != null) addPlugins(plugins);
                            }
                        } finally {
                            is.close();
                        }
                    }
                } finally {
                    jarFile.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
