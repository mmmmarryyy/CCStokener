package de.uni_leipzig.lots.common.plugin;

import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Diese Klasse läd Pluginklassen aus Service Dateien.
 * <p/>
 * Das Pluginsystem besteht aus drei Teilen. </p> <ul> <li>Interface welches die einzelnen Plugins
 * implementieren</li> <li>Servicedatei dieses Interfaces</li> <li>mehrere Plugins</li> </ul>
 * <p/>
 * Die Plugins, welche das Interface implementieren, sind in einer Servicedatei aufgeführt. Diese Datei muss
 * im Classpath des zu implementierenden Interfaces unter <tt>META-INF/services/&lt;classname des
 * interfaces&gt;.service</tt> liegen. Genauer gesagt muss der Classloader des Interfaces die Servicedatei
 * laden können. </p>
 * <p/>
 * Als Anregung für die Implementierung wurde die Klasse <tt>org.radeox.util.Service</tt> der Render Engine
 * Radeox genommen. </p>
 *
 * @author Matthias L. Jugel
 * @author Alexander Kiel
 * @version $Id: ServiceFileLoader.java,v 1.9 2007/10/23 06:30:34 mai99bxd Exp $
 */
public class ServiceFileLoader implements PluginLoader {

    protected static final Logger logger = Logger.getLogger(ServiceFileLoader.class.getName());

    private static ServiceFileLoader instance;

    private boolean hasNew;

    public static synchronized ServiceFileLoader getInstance() {
        if (null == instance) {
            instance = new ServiceFileLoader();
        }
        return instance;
    }

    public ServiceFileLoader() {
        hasNew = true;
    }

    @Nullable
    public Collection<Class<? extends Plugin>> load(Class<? extends Plugin> pluginType) {
        ClassLoader classLoader = pluginType.getClassLoader();
        String resource = "META-INF/services/" + pluginType.getName() + ".service";
        logger.fine("try to use plugins from resource: " + resource);
        try {
            Collection<Class<? extends Plugin>> pluginClasses = loadFromResource(classLoader, resource);
            return pluginClasses;
        } catch (IOException e) {
            logger.log(Level.WARNING, "IOException while loading plugins from type " + pluginType.getName() + ".", e);
        }
        return null;
    }

    public boolean hasNew() {
        return hasNew;
    }

    private Collection<Class<? extends Plugin>> loadFromResource(ClassLoader classLoader, String resource) throws IOException {
        Collection<Class<? extends Plugin>> pluginClasses = new HashSet<Class<? extends Plugin>>();
        Enumeration providerFiles = classLoader.getResources(resource);
        if (!providerFiles.hasMoreElements()) {
            logger.warning("Can't find the resource: " + resource);
            return pluginClasses;
        }
        do {
            URL url = (URL) providerFiles.nextElement();
            InputStream stream = url.openStream();
            BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            } catch (IOException e) {
                continue;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                int index = line.indexOf('#');
                if (index != -1) {
                    line = line.substring(0, index);
                }
                line = line.trim();
                if (line.length() > 0) {
                    Class pluginClass;
                    try {
                        pluginClass = classLoader.loadClass(line);
                    } catch (ClassNotFoundException e) {
                        logger.log(Level.WARNING, "Can't use the Pluginclass with the name " + line + ".", e);
                        continue;
                    }
                    if (Plugin.class.isAssignableFrom(pluginClass)) {
                        pluginClasses.add((Class<? extends Plugin>) pluginClass);
                    } else {
                        logger.warning("The Pluginclass with the name " + line + " isn't a subclass of Plugin.");
                    }
                }
            }
            reader.close();
            stream.close();
        } while (providerFiles.hasMoreElements());
        return pluginClasses;
    }
}
