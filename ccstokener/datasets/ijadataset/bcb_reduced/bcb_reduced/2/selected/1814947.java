package org.merlotxml.merlot.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.merlotxml.merlot.MerlotConstants;
import org.merlotxml.merlot.MerlotDebug;
import org.merlotxml.merlot.MerlotResource;
import org.merlotxml.merlot.XMLEditor;
import org.merlotxml.merlot.XMLEditorSettings;
import org.merlotxml.merlot.plugin.action.ActionPluginConfig;
import org.merlotxml.merlot.plugin.dtd.DTDPluginConfig;
import org.merlotxml.merlot.plugin.nodeAction.NodeActionPluginConfig;
import org.merlotxml.util.FileUtil;
import org.merlotxml.util.xml.DOMLiaisonImplException;
import org.merlotxml.util.xml.ValidDocument;
import org.tolven.logging.TolvenLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Merlot Plugin Manager		<p>
 * 
 * Responsible for locating and loading all Merlot plugins,
 * and keeping track of them.<P>
 * 
 * Plugin initialization is a two-stage process. The first stage is loading the plugin 
 * configuration. This retrieves information from the plugin.xml file. The second
 * stage is initialization which is when the plugin's resources are initialized and 
 * classes can be loaded. These two stages allow for plugins to name dependencies on
 * other plugins, and their classloaders are linked together. However since plugins
 * are read in filesystem order, the dependencies can't be resolved until all configs
 * have been read.<p>
 *
 * Stage 1 is implemented by the {@link #loadPlugins} method. Stage 2 is implemented by
 * the {@link #initPlugins} method.<P>
 *
 * NOTE: Circular plugin dependencies are not checked for, and will cause a fatal
 * error in classloading if they exist.<P>
 * 
 * @author Tim McCune
 * @author Kelly Campbell
 */
public class PluginManager {

    protected static final String ERR_PLUGIN_NOT_RECOGNIZED = "Unrecognized plugin format";

    protected static final String ERR_DUP_PLUGIN = "Plugin named {0} already loaded";

    public static final String PLUGIN_CONFIG_FILE = "plugin.xml";

    private static final String TMP_PLUGIN_PREFIX = "merlotPlugin";

    private static final String TMP_PLUGIN_SUFFIX = ".jar";

    private static final Object mutex = new Object();

    protected static PluginManager instance;

    private String currentFilePath;

    /** Map of plugin configs keyed by the name */
    private Map<String, PluginConfig> _plugins;

    public static PluginManager getInstance() {
        if (instance != null) {
            return instance;
        } else {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new PluginManager();
                }
                return instance;
            }
        }
    }

    protected PluginManager() {
        _plugins = new HashMap<String, PluginConfig>();
    }

    public List<PluginConfig> getPlugins() {
        return new ArrayList<PluginConfig>(_plugins.values());
    }

    public PluginConfig getPlugin(String name) {
        return (PluginConfig) _plugins.get(name);
    }

    public void addPluginfolder(File dir) {
    }

    public String getCurrentFilePath() {
        return currentFilePath;
    }

    /**
	 * @exception PluginManagerException Thrown if a plugin's config file is in
	 *		an unrecognized format
	 * @exception IOException Thrown if a plugin couldn't be read
	 * @exception InstantiationException Thrown if there was a problem creating
	 *		an XML parser
	 * @exception IllegalAccessException Thrown if there was a problem creating
	 *		an XML parser
	 * @exception ClassNotFoundException Thrown if there was a problem creating
	 *		an XML parser
	 * @exception DOMLiaisonImplException Thrown if there was a problem creating
	 *		an XML parser
	 * @exception MalformedURLException Thrown if a plugin provided a malformed
	 *		URL in its config file
	 * @exception SAXException Thrown if the plugin config file contains malformed XML
	 * @exception PluginConfigException Thrown if there was a plugin-specific error
	 */
    public void loadPlugins() throws PluginManagerException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, DOMLiaisonImplException, MalformedURLException, SAXException, PluginConfigException {
        String msg = MerlotResource.getString(MerlotConstants.UI, "splash.loadingPlugins.msg");
        XMLEditorSettings.getSharedInstance().showSplashStatus(msg);
        List<File> path = XMLEditorSettings.getSharedInstance().getPluginPath();
        if (path != null) {
            for (File f : path) {
                try {
                    searchForPlugins(f);
                } catch (Exception ex) {
                    MerlotDebug.exception(ex);
                }
            }
        }
        List<URL> urls = XMLEditorSettings.getSharedInstance().getPluginURLs();
        for (URL u : urls) {
            XMLEditorSettings.getSharedInstance().showSplashStatus(msg + " " + u);
            File f = downloadURL(u);
            if (f != null) {
                initPlugin(f);
            }
        }
        java.util.List<URL> p_list = XMLEditorSettings.getSharedInstance().getPluginFiles();
        for (URL u : p_list) {
            File f = downloadURL(u);
            if (f != null) {
                initPlugin(f);
            }
        }
        resolveDependencies();
        initPlugins();
    }

    public void searchForPlugins(File dir) throws PluginManagerException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, DOMLiaisonImplException, MalformedURLException, SAXException, PluginConfigException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    initPlugin(files[i]);
                }
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    initPlugin(files[i]);
                }
            }
        }
    }

    private void initPlugin(File pluginFile) throws PluginManagerException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, DOMLiaisonImplException, MalformedURLException, SAXException, PluginConfigException {
        PluginClassLoader cl;
        File configFile;
        int j;
        PluginConfig pc = null;
        String s;
        ZipEntry configEntry;
        ZipFile nextZipFile;
        cl = new PluginClassLoader(pluginFile);
        pluginFile = cl.getPluginDir();
        if (pluginFile.isDirectory()) {
            configFile = new File(pluginFile, PLUGIN_CONFIG_FILE);
            if (configFile.exists() && configFile.canRead() && configFile.isFile()) {
                try {
                    currentFilePath = pluginFile.getPath();
                    pc = createPluginConfig(new FileInputStream(configFile), pluginFile, cl);
                } catch (Exception e) {
                    MerlotDebug.exception(e);
                }
            }
        } else {
            try {
                nextZipFile = new ZipFile(pluginFile);
            } catch (ZipException e) {
                return;
            }
            s = pluginFile.getAbsolutePath();
            if ((j = s.indexOf(".jar")) == -1) {
                j = s.indexOf(".zip");
            }
            if (j > -1) {
                s = s.substring(0, j) + File.separator + PLUGIN_CONFIG_FILE;
                if (new File(s).exists()) {
                    return;
                }
            }
            if ((configEntry = nextZipFile.getEntry(PLUGIN_CONFIG_FILE)) != null) {
                try {
                    currentFilePath = nextZipFile.getName();
                    pc = createPluginConfig(nextZipFile.getInputStream(configEntry), pluginFile, cl);
                } catch (Exception e) {
                    MerlotDebug.exception(e);
                }
            }
            nextZipFile.close();
        }
        if (pc != null && !_plugins.containsKey(pc.getName())) {
            _plugins.put(pc.getName(), pc);
        } else {
            TolvenLogger.info("Duplicate plugin definition [" + pc.getName() + "] found. Accepting first instance encountered.", PluginManager.class);
        }
    }

    protected PluginConfig createPluginConfig(InputStream input, File source, ClassLoader cl) throws PluginManagerException, InstantiationException, IllegalAccessException, ClassNotFoundException, DOMLiaisonImplException, MalformedURLException, SAXException, PluginConfigException {
        Document doc = null;
        try {
            ValidDocument vDoc = XMLEditor.getSharedInstance().getDOMLiaison().parseValidXMLStream(input, source.toString());
            doc = vDoc.getDocument();
            Node firstElement;
            PluginConfig rtn;
            String nodeName;
            if ((firstElement = (Element) doc.getDocumentElement()) != null) {
                nodeName = firstElement.getNodeName();
            } else {
                nodeName = "";
            }
            if (nodeName.equals("action-plugin")) {
                rtn = new ActionPluginConfig(this, cl, source);
            } else if (nodeName.equals("dtd-plugin")) {
                rtn = new DTDPluginConfig(this, cl, source);
            } else if (nodeName.equals("node-action-plugin")) {
                rtn = new NodeActionPluginConfig(this, cl, source);
            } else {
                throw new PluginManagerException(ERR_PLUGIN_NOT_RECOGNIZED + ": \"" + nodeName + "\"");
            }
            rtn.parse(doc);
            return rtn;
        } catch (Exception ex) {
            TolvenLogger.info("Exception loading plugin document: " + ex, PluginManager.class);
            throw new PluginConfigException(ex);
        }
    }

    private File downloadContent(URLConnection connection, File cacheFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            File outFile = File.createTempFile(TMP_PLUGIN_PREFIX, TMP_PLUGIN_SUFFIX);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int i;
            while ((i = in.read()) != -1) {
                out.write(i);
            }
            out.flush();
            out.close();
            in.close();
            FileUtil.copyFile(outFile, cacheFile);
            outFile.delete();
            return cacheFile;
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        return null;
    }

    private File downloadURL(URL url) {
        MerlotDebug.msg("Downloading URL: " + url);
        String filename = url.getFile();
        if (filename.indexOf('/') >= 0) {
            filename = filename.substring(filename.lastIndexOf('/') + 1);
        }
        File userPluginsDir = new File(XMLEditorSettings.USER_MERLOT_DIR, "plugins");
        File cache = new File(userPluginsDir, filename);
        try {
            if (!userPluginsDir.exists()) {
                userPluginsDir.mkdirs();
            }
            URLConnection connection = url.openConnection();
            if (cache.exists() && cache.canRead()) {
                connection.connect();
                long remoteTimestamp = connection.getLastModified();
                if (remoteTimestamp == 0 || remoteTimestamp > cache.lastModified()) {
                    cache = downloadContent(connection, cache);
                } else {
                    MerlotDebug.msg("Using cached version for URL: " + url);
                }
            } else {
                cache = downloadContent(connection, cache);
            }
        } catch (IOException ex) {
            MerlotDebug.exception(ex);
        }
        if (cache != null && cache.exists()) {
            return cache;
        } else {
            return null;
        }
    }

    /** 
	 * Go through all the plugins and tell each to resolve its dependencies
	 */
    private void resolveDependencies() {
        for (PluginConfig config : _plugins.values()) {
            try {
                config.resolveDependencies();
            } catch (PluginConfigException ex) {
                MerlotDebug.exception(ex);
            }
        }
    }

    /** 
	 * Go through all the plugins and tell each to resolve its dependencies
	 */
    private void initPlugins() {
        List<PluginConfig> badPlugins = new ArrayList<PluginConfig>();
        for (PluginConfig config : _plugins.values()) {
            try {
                config.init();
            } catch (PluginConfigException ex) {
                TolvenLogger.error("Plugin " + config.getName() + " could not be loaded due to an error", PluginManager.class);
                badPlugins.add(config);
                ex.printStackTrace();
            } catch (Throwable t) {
                TolvenLogger.error("Plugin " + config.getName() + " could not be loaded due to an error", PluginManager.class);
                badPlugins.add(config);
                t.printStackTrace();
            }
        }
        for (PluginConfig config : badPlugins) {
            _plugins.remove(config.getName());
        }
    }

    /**
	 * Tester
	 */
    public static void main(String[] args) {
        try {
            PluginManager pm = new PluginManager();
            pm.searchForPlugins(new File(args[0]));
            TolvenLogger.info(pm.getPlugins(), PluginManager.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
