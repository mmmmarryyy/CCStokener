package info.metlos.plugin;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * This implementation of {@link IPluginLoader} can load {@link IPlugin}s from
 * .jar files.
 * 
 * @author metlos
 * 
 * @version $Id: JarPluginLoader.java 90 2007-06-18 21:33:31Z metlos $
 * 
 */
public class JarPluginLoader implements IPluginLoader {

    private static final Logger logger = LogManager.getLogger(JarPluginLoader.class);

    private static class JarClassLoader extends ClassLoader {

        private static final Logger logger = LogManager.getLogger(JarPluginLoader.JarClassLoader.class);

        private static final String CLASS_SUFFIX = ".class";

        private static final int SUFFIX_LEN = 6;

        private final JarInputStream stream;

        public JarClassLoader(JarInputStream stream, ClassLoader parent) {
            super(parent);
            this.stream = stream;
        }

        public Set<Class<?>> getClasses() {
            final Set<Class<?>> ret = new HashSet<Class<?>>();
            try {
                JarEntry entry;
                while ((entry = stream.getNextJarEntry()) != null) {
                    final String name = entry.getName();
                    if (!entry.isDirectory() && name.endsWith(CLASS_SUFFIX)) {
                        final String className = name.substring(0, name.length() - SUFFIX_LEN);
                        final byte[] bytes = getBytes(stream);
                        ret.add(defineClass(className, bytes, 0, bytes.length));
                    }
                }
            } catch (IOException e) {
                ret.clear();
                logger.warn("Exception while loading classes.", e);
            }
            return ret;
        }

        private byte[] getBytes(final InputStream inStream) throws IOException {
            final byte[] buffer = new byte[2048];
            final ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            int cnt;
            while ((cnt = inStream.read(buffer)) != -1) {
                out.write(buffer, 0, cnt);
            }
            return out.toByteArray();
        }
    }

    private Set<IPlugin> plugins;

    private ClassLoader parentLoader;

    /**
	 * Creates a new instance initialized with the class loader of JarPluginLoader class.
	 * @see JarPluginLoader#JarPluginLoader(ClassLoader)
	 */
    public JarPluginLoader() {
        this(JarPluginLoader.class.getClassLoader());
    }

    /**
	 * Creates a new instance and sets given class loader as the parent class loader for the
	 * classes loaded from the jar files.
	 * 
	 * @param parentClassLoader the parent class loader for the jars
	 */
    public JarPluginLoader(ClassLoader parentClassLoader) {
        parentLoader = parentClassLoader;
    }

    public Set<IPlugin> getPlugins() {
        if (plugins == null) {
            plugins = new HashSet<IPlugin>();
        }
        return plugins;
    }

    public void loadJar(final String fileName) throws IOException {
        loadJar(new JarInputStream(new FileInputStream(fileName)));
    }

    public void loadJar(final URL url) throws IOException {
        loadJar(new JarInputStream(url.openStream()));
    }

    public void loadJar(final JarInputStream inputStream) {
        final JarClassLoader loader = AccessController.doPrivileged(new PrivilegedAction<JarClassLoader>() {

            public JarClassLoader run() {
                return new JarClassLoader(inputStream, parentLoader);
            }
        });
        for (Class<?> clazz : loader.getClasses()) {
            if (PluginUtil.isPluginClass(clazz)) {
                final IPlugin plugin = PluginUtil.getInstance(clazz);
                if (plugin == null) {
                    logger.warn("Failed to instantiate plugin of class " + clazz.getName());
                } else {
                    getPlugins().add(plugin);
                }
            }
        }
    }
}
