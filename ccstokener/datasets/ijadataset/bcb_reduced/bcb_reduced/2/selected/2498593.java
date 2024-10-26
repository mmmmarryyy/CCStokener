package org.exist.plugin;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import org.apache.log4j.Logger;
import org.exist.Database;
import org.exist.collections.Collection;
import org.exist.config.*;
import org.exist.config.annotation.*;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

/**
 * Plugins manager. 
 * It control search procedure, activation and de-actication (including runtime).
 * 
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 * 
 */
@ConfigurationClass("plugin-manager")
public class PluginsManagerImpl implements Configurable, PluginsManager {

    private static final Logger LOG = Logger.getLogger(PluginsManagerImpl.class);

    public static final XmldbURI PLUGINS_COLLETION_URI = XmldbURI.SYSTEM_COLLECTION_URI.append("plugins");

    public static final XmldbURI CONFIG_FILE_URI = XmldbURI.create("config.xml");

    private static final String MAIN_CLASS = "Main-Class";

    private static final String CLASS_PATH = "Class-Path";

    @ConfigurationFieldAsAttribute("version")
    private String version = "1.0";

    private List<Jack> jacks = new ArrayList<Jack>();

    private Configuration configuration = null;

    private Collection collection;

    private Database db;

    public PluginsManagerImpl(Database db, DBBroker broker) throws ConfigurationException {
        this.db = db;
        TransactionManager transaction = db.getTransactionManager();
        Txn txn = null;
        try {
            collection = broker.getCollection(PLUGINS_COLLETION_URI);
            if (collection == null) {
                txn = transaction.beginTransaction();
                collection = broker.getOrCreateCollection(txn, PLUGINS_COLLETION_URI);
                if (collection == null) return;
                collection.setPermissions(0770);
                broker.saveCollection(txn, collection);
                transaction.commit(txn);
            }
        } catch (Exception e) {
            transaction.abort(txn);
            e.printStackTrace();
            LOG.debug("loading configuration failed: " + e.getMessage());
        }
        Configuration _config_ = Configurator.parse(this, broker, collection, CONFIG_FILE_URI);
        configuration = Configurator.configure(this, _config_);
        try {
            for (Class<? extends Jack> plugin : listServices(Jack.class)) {
                try {
                    Constructor<? extends Jack> ctor = plugin.getConstructor(PluginsManager.class);
                    Jack plgn = ctor.newInstance(this);
                    jacks.add(plgn);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public Database getDatabase() {
        return db;
    }

    public void addSearchPath(String path) {
        System.out.println("addSearchPath = " + path);
    }

    private void addJavaClassPath() {
        String path = null;
        try {
            path = System.getProperty("java.class.path");
        } catch (Exception ex) {
            path = "";
            LOG.error("Unable to get class path", ex);
        }
        StringTokenizer tok = new StringTokenizer(path, File.pathSeparator);
        while (tok.hasMoreTokens()) add(new File(tok.nextToken()));
    }

    public boolean add(File file) {
        boolean added = false;
        if (Utils.fileCanContainClasses(file)) {
            String absPath = file.getAbsolutePath();
            added = true;
        }
        return added;
    }

    private void loadJarClass(File jarFile) {
        try {
            JarFile jar = new JarFile(jarFile);
            Manifest manifest = jar.getManifest();
            if (manifest == null) return;
            Attributes attrs = manifest.getMainAttributes();
            String value = (String) attrs.get(CLASS_PATH);
            if (value != null) {
                if (LOG.isDebugEnabled()) LOG.debug("Adding Class-Path from jar " + jar.getName());
                StringBuilder buf = new StringBuilder();
                StringTokenizer tok = new StringTokenizer(value);
                while (tok.hasMoreTokens()) {
                    buf.setLength(0);
                    String element = tok.nextToken();
                    String parent = jarFile.getParent();
                    if (parent != null) {
                        buf.append(parent);
                        buf.append(File.separator);
                    }
                    buf.append(element);
                }
                String element = buf.toString();
                if (LOG.isDebugEnabled()) LOG.debug("From " + jar.getName() + ": " + element);
                add(new File(element));
            }
            value = (String) attrs.get(MAIN_CLASS);
            if (value != null) {
            }
        } catch (IOException ex) {
            LOG.error("I/O error processing jar file '" + jarFile.getPath() + "'", ex);
        }
    }

    private <S> Iterable<Class<? extends S>> listServices(Class<S> ifc) throws Exception {
        ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> e = ldr.getResources("META-INF/services/" + ifc.getName());
        Set<Class<? extends S>> services = new HashSet<Class<? extends S>>();
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            InputStream is = url.openStream();
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while (true) {
                    String line = r.readLine();
                    if (line == null) break;
                    int comment = line.indexOf('#');
                    if (comment >= 0) line = line.substring(0, comment);
                    String name = line.trim();
                    if (name.length() == 0) continue;
                    Class<?> clz = Class.forName(name, true, ldr);
                    Class<? extends S> impl = clz.asSubclass(ifc);
                    services.add(impl);
                }
            } finally {
                is.close();
            }
        }
        return services;
    }

    @Override
    public boolean isConfigured() {
        return configuration != null;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}
