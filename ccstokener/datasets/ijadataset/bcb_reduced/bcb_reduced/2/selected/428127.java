package org.qfirst.batavia.model;

import java.net.*;
import org.apache.commons.lang.*;
import java.util.*;
import org.qfirst.batavia.*;
import org.apache.log4j.*;
import java.io.*;
import org.qfirst.batavia.model.loader.*;
import org.qfirst.batavia.ui.options.*;
import org.xml.sax.*;
import javax.swing.JMenu;
import org.qfirst.batavia.event.*;
import org.qfirst.vfs.*;
import org.qfirst.batavia.service.transfer.*;
import org.qfirst.i18n.*;
import javax.swing.*;
import org.qfirst.options.ui.*;
import org.qfirst.options.loader.*;
import org.qfirst.options.*;
import org.qfirst.batavia.actions.*;
import org.qfirst.batavia.ui.*;
import org.qfirst.options.components.*;
import org.qfirst.batavia.plugin.*;

/**
 *  The plugin manager. Loads, unloads, starts, stops, etc plugins.
 *
 * @author     francisdobi
 * @created    May 16, 2004
 */
public class PluginManager {

    /**
	 *  Description of the Field
	 */
    protected Logger logger = Logger.getLogger(getClass());

    private List preparedPlugins = new ArrayList();

    private List runningPlugins = new ArrayList();

    private static PluginManager instance = new PluginManager();

    private String sep = SystemUtils.FILE_SEPARATOR;

    private String baseDir = SystemUtils.USER_HOME + sep + ".batavia";

    private String pluginsBaseDir = baseDir + sep + "plugins";

    private String pluginURLBase = "http://batavia.sourceforge.net";

    private List listeners = new ArrayList();

    /**
	 *  Gets the instance attribute of the PluginManager class
	 *
	 * @return    The instance value
	 */
    public static PluginManager getInstance() {
        return instance;
    }

    /**
	 *  Constructor for the PluginManager object
	 */
    private PluginManager() {
    }

    /**
	 *  Description of the Method
	 */
    public void refreshInstalled() {
        preparePlugins();
    }

    /**
	 *  Prepares plugins from the plugin directory. Loads the plugin property file.
	 *  Does not instantiate any plugin class, so does not start plugin.
	 */
    public void preparePlugins() {
        File pluginDirs[] = new File(pluginsBaseDir).listFiles();
        if (pluginDirs != null) {
            for (int i = 0; i < pluginDirs.length; i++) {
                PluginInfo pluginInfo = preparePlugin(pluginDirs[i].getAbsolutePath());
            }
        }
    }

    /**
	 *  Prepares a plugin. Loads its properties from the plugin.properties file.
	 *  Adds it to the pluginList.
	 *
	 * @param  pDir  Description of the Parameter
	 * @return
	 */
    public PluginInfo preparePlugin(String pDir) {
        InputStream is = null;
        try {
            logger.debug("Preparing: " + pDir);
            File jars[] = new File(pDir).listFiles(jarFilter);
            URL urls[] = new URL[jars.length];
            for (int i = 0; i < urls.length; i++) {
                urls[i] = new URL("file:" + jars[i].getAbsolutePath());
                logger.debug("urls[i]: " + urls[i]);
            }
            PluginInfo pluginInfo = new PluginInfo();
            URLClassLoader cl = new URLClassLoader(urls, Plugin.class.getClassLoader());
            Properties prop = new Properties();
            is = cl.getResourceAsStream("plugin.properties");
            if (is != null) {
                prop.load(is);
                pluginInfo.setId(prop.getProperty("plugin.id"));
                if (findPluginById(pluginInfo.getId()) != null) {
                    logger.debug("Plugin already prepareds:" + pluginInfo.getId() + ' ' + pDir);
                    return null;
                }
                pluginInfo.setLoadMode(prop.getProperty("plugin.loadmode", PluginInfo.LOAD_WHEN_FIRST_INVOKED));
                pluginInfo.setClassLoader(cl);
                pluginInfo.setPluginClassName(prop.getProperty("plugin.classname"));
                pluginInfo.setVersion(prop.getProperty("plugin.version"));
                if (prop.getProperty("plugin.option.path") != null) {
                    pluginInfo.setOptionPath(prop.getProperty("plugin.option.path"));
                }
                if (prop.getProperty("plugin.action.path") != null) {
                    pluginInfo.setActionPath(prop.getProperty("plugin.action.path"));
                }
                if (prop.getProperty("plugin.menu.path") != null) {
                    pluginInfo.setMenuPath(prop.getProperty("plugin.menu.path"));
                }
                pluginInfo.setShortDescription(prop.getProperty("plugin.short.description"));
                pluginInfo.setLongDescription(prop.getProperty("plugin.long.description"));
                pluginInfo.setName(prop.getProperty("plugin.name"));
                pluginInfo.setInstallDirectory(pDir);
                if (pluginInfo.validate()) {
                    loadPluginActions(pluginInfo);
                    loadPluginMenus(pluginInfo);
                    preparedPlugins.add(pluginInfo);
                    pluginInfo.setLoadState(pluginInfo.getLoadState() | PluginInfo.PREPARED);
                    firePluginPrepared(pluginInfo);
                    return pluginInfo;
                } else {
                    logger.info("Invalid plugin:" + pDir);
                    return null;
                }
            } else {
                logger.debug("p.props:" + cl.getResource("plugin.properties"));
                logger.info("Not a plugin:" + pDir);
            }
        } catch (MalformedURLException mex) {
            logger.error(mex, mex);
        } catch (IOException ioe) {
            logger.error(ioe, ioe);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe) {
            }
        }
        return null;
    }

    /**
	 *  Loads all plugins which have load mode equals parameter when.
	 *
	 * @param  when  Description of the Parameter
	 */
    public void loadAndStartPlugins(String when) {
        for (int i = 0; i < preparedPlugins.size(); i++) {
            PluginInfo p = (PluginInfo) preparedPlugins.get(i);
            if (p.getLoadMode().equals(when)) {
                loadAndStartPlugin(p);
            }
        }
    }

    /**
	 *  Loads all plugins.
	 */
    public void loadAndStartPlugins() {
        for (int i = 0; i < preparedPlugins.size(); i++) {
            PluginInfo p = (PluginInfo) preparedPlugins.get(i);
            loadAndStartPlugin(p);
        }
    }

    /**
	 *  Loads a plugin and calls its start method.
	 *
	 * @param  pluginInfo  Description of the Parameter
	 */
    public void loadAndStartPlugin(PluginInfo pluginInfo) {
        if ((pluginInfo.getLoadState() & PluginInfo.PREPARED) == 0) {
            logger.warn("Not starting plugin, because plugin is not prepared.");
            return;
        }
        if ((pluginInfo.getLoadState() & PluginInfo.STARTED) != 0) {
            logger.warn("Not starting plugin, because plugin is already started.");
            return;
        }
        try {
            ClassLoader loader;
            if (pluginInfo.getClassLoader() == null) {
                File jars[] = new File(pluginInfo.getInstallDirectory()).listFiles(jarFilter);
                URL urls[] = new URL[jars.length];
                for (int i = 0; i < urls.length; i++) {
                    urls[i] = new URL("file:" + jars[i].getAbsolutePath());
                }
                loader = new URLClassLoader(urls, Plugin.class.getClassLoader());
            } else {
                loader = pluginInfo.getClassLoader();
            }
            Plugin plugin = (Plugin) loader.loadClass(pluginInfo.getPluginClassName()).newInstance();
            pluginInfo.setPlugin(plugin);
            pluginInfo.setOptionPersister(new DefaultOptionPersister(plugin.getClass()));
            logger.debug("Starting plugin: " + pluginInfo.getName());
            plugin.start();
            pluginInfo.setLoadState(pluginInfo.getLoadState() | PluginInfo.STARTED);
            try {
                registerActions(pluginInfo);
            } catch (Exception ex) {
                logger.warn("Obsolote plugin? " + pluginInfo.getName(), ex);
            }
            firePluginStarted(pluginInfo);
        } catch (ClassNotFoundException ex) {
            logger.warn("Could not start plugin: " + pluginInfo.getName(), ex);
        } catch (InstantiationException ex) {
            logger.warn("Could not start plugin: " + pluginInfo.getName(), ex);
        } catch (IllegalAccessException ex) {
            logger.warn("Could not start plugin: " + pluginInfo.getName(), ex);
        } catch (Throwable t) {
            logger.error("Could not start plugin: " + pluginInfo.getName(), t);
        }
    }

    private void unregisterActions(PluginInfo pluginInfo) {
        Map map = pluginInfo.getActionMap();
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String id = (String) iter.next();
            KeyboardPrefixManager.getInstance().removeAction("pluginshortcut." + pluginInfo.getId() + "." + id);
        }
        JMenu menu = pluginInfo.getJMenu();
        if (menu != null) {
            int count = menu.getItemCount();
            for (int i = 0; i < count; i++) {
                JMenuItem item = menu.getItem(i);
                if (item != null) {
                    AbstractPluginAction action = (AbstractPluginAction) item.getAction();
                    String id = action.getId();
                    KeyboardPrefixManager.getInstance().removeAction("pluginshortcut." + pluginInfo.getId() + ".JMenuItem." + id);
                }
            }
        }
    }

    private void registerActions(PluginInfo pluginInfo) {
        Map map = pluginInfo.getActionMap();
        Iterator iter = map.keySet().iterator();
        OptionManager man = Batavia.getOptionManager();
        while (iter.hasNext()) {
            String id = (String) iter.next();
            Action a = (Action) map.get(id);
            ShortcutAction shortcutAction = new ShortcutAction();
            shortcutAction.setAction(a);
            shortcutAction.setOption(man.getOption("pluginshortcut." + pluginInfo.getId() + "." + id, true));
            KeyboardPrefixManager.getInstance().addAction(shortcutAction);
        }
        JMenu menu = pluginInfo.getJMenu();
        if (menu != null) {
            int count = menu.getItemCount();
            for (int i = 0; i < count; i++) {
                JMenuItem item = menu.getItem(i);
                if (item != null) {
                    AbstractPluginAction action = (AbstractPluginAction) item.getAction();
                    String id = action.getId();
                    ShortcutAction shortcutAction = new ShortcutAction();
                    shortcutAction.setAction(action);
                    shortcutAction.setOption(man.getOption("pluginshortcut." + pluginInfo.getId() + ".JMenuItem." + id, true));
                    KeyboardPrefixManager.getInstance().addAction(shortcutAction);
                }
            }
        }
    }

    /**
	 *  Loads plugins option structure
	 */
    public void loadPluginsOptions() {
        for (int i = 0; i < allPlugins.getSize(); i++) {
            PluginInfo p = (PluginInfo) allPlugins.getElementAt(i);
            if ((p.getLoadState() & PluginInfo.STARTED) == 0) {
                loadAndStartPlugin(p);
            }
            loadPluginOptions(p);
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  pluginInfo  Description of the Parameter
	 */
    protected void loadPluginOptions(PluginInfo pluginInfo) {
        if ((pluginInfo.getLoadState() & PluginInfo.STARTED) == 0) {
            logger.warn("Not loading options, because plugin not started.");
            return;
        }
        OptionLoader optLoader = new OptionLoader(new DefaultOptionPersister(pluginInfo.getPlugin().getClass()), Message.getInstance(), pluginInfo.getClassLoader(), Batavia.getLocale());
        InputStream optionStream = null;
        try {
            optionStream = pluginInfo.getClassLoader().getResourceAsStream(pluginInfo.getOptionPath());
            if (optionStream == null) {
                logger.debug("No options for plugin: " + pluginInfo.getName());
            } else {
                OptionTreeNode node = optLoader.loadStructure(optionStream);
                pluginInfo.setOptionTreeNode(node);
            }
            pluginInfo.setLoadState(pluginInfo.getLoadState() | PluginInfo.OPTION_LOADED);
        } catch (SAXException ex) {
            logger.warn("Parse error during loading options for plugin: " + pluginInfo.getName(), ex);
        } catch (IOException ex) {
            logger.warn("IO error occured while loading options for plugin: " + pluginInfo.getName(), ex);
        } catch (Throwable t) {
            logger.warn("Unknow error occured while loading options for plugin: " + pluginInfo.getName(), t);
        } finally {
            if (optionStream != null) {
                try {
                    optionStream.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
	 *  Loads plugins menu structure
	 */
    public void loadPluginsMenus() {
        for (int i = 0; i < preparedPlugins.size(); i++) {
            PluginInfo p = (PluginInfo) preparedPlugins.get(i);
            loadPluginMenus(p);
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  pluginInfo  Description of the Parameter
	 * @return             Description of the Return Value
	 */
    public JMenu loadPluginMenus(PluginInfo pluginInfo) {
        if (pluginInfo.getJMenu() != null) {
            logger.debug("JMenu already loaded.");
        }
        if (!pluginInfo.hasJMenu()) {
            logger.debug("Plugin has no menu defined.");
            return null;
        }
        MenuLoader menuLoader = new MenuLoader(pluginInfo.getClassLoader());
        InputStream menuStream = null;
        try {
            menuStream = pluginInfo.getClassLoader().getResourceAsStream(pluginInfo.getMenuPath());
            if (menuStream == null) {
                logger.debug("No menu for plugin: " + pluginInfo.getName());
            } else {
                JMenu menu = menuLoader.loadMenus(menuStream);
                pluginInfo.setJMenu(menu);
                return menu;
            }
        } catch (SAXException ex) {
            logger.warn("Parse error during loading menus for plugin: " + pluginInfo.getName(), ex);
        } catch (IOException ex) {
            logger.warn("IO error occured while loading menus for plugin: " + pluginInfo.getName(), ex);
        } catch (Throwable t) {
            logger.warn("Unknow error occured while loading menus for plugin: " + pluginInfo.getName(), t);
        } finally {
            if (menuStream != null) {
                try {
                    menuStream.close();
                } catch (Exception ex) {
                }
            }
        }
        return null;
    }

    /**
	 *  Loads plugins actions
	 */
    public void loadPluginsActions() {
        for (int i = 0; i < preparedPlugins.size(); i++) {
            PluginInfo p = (PluginInfo) preparedPlugins.get(i);
            loadPluginActions(p);
        }
    }

    public void loadPluginActions(PluginInfo pluginInfo) {
        ActionLoader loader = new ActionLoader();
        InputStream stream = null;
        try {
            stream = pluginInfo.getClassLoader().getResourceAsStream(pluginInfo.getActionPath());
            pluginInfo.getActionMap().clear();
            if (stream == null) {
                logger.debug("No actions.xml for plugin: " + pluginInfo.getName());
            } else {
                loader.loadActions(stream, pluginInfo);
            }
            pluginInfo.setLoadState(pluginInfo.getLoadState() | PluginInfo.ACTIONS_LOADED);
        } catch (SAXException ex) {
            logger.warn("Parse error during loading actions for plugin: " + pluginInfo.getName(), ex);
        } catch (IOException ex) {
            logger.warn("IO error occured while loading actions for plugin: " + pluginInfo.getName(), ex);
        } catch (Throwable t) {
            logger.warn("Unknow error occured while loading actions for plugin: " + pluginInfo.getName(), t);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
	 *  Unloads a plugin. Completly removes this plugin from memory.
	 *
	 * @param  p  The pluginInfo
	 */
    protected void unloadPlugin(PluginInfo p) {
        firePluginUnloading(p);
        if ((p.getLoadState() & PluginInfo.STARTED) > 0) {
            unregisterActions(p);
            p.getPlugin().stop();
        } else {
            logger.warn("Plugin not started, hence not stopping: " + p.getName());
        }
        p.setPlugin(null);
        p.setId(null);
        p.setActionMap(null);
        p.setActionPath(null);
        p.setClassLoader(null);
        p.setLongDescription(null);
        p.setShortDescription(null);
        p.setInstallDirectory(null);
        p.setLoadMode(null);
        p.setMenuPath(null);
        p.setName(null);
        p.setOptionTreeNode(null);
        p.setOptionPersister(null);
        p.setPluginClassName(null);
        p.setVersion(null);
        p.setLoadState(PluginInfo.NOT_LOADED);
        runningPlugins.remove(p);
        preparedPlugins.remove(p);
    }

    /**
	 *  Stops a plugin
	 *
	 * @param  p  The pluginInfo
	 */
    public void stopPlugin(PluginInfo p) {
        firePluginUnloading(p);
        if ((p.getLoadState() & PluginInfo.STARTED) > 0) {
            unregisterActions(p);
            p.getPlugin().stop();
        } else {
            logger.warn("Plugin not started, hence no need to stop: " + p.getName());
        }
        p.setPlugin(null);
        p.setClassLoader(null);
        p.setLoadState(PluginInfo.NOT_LOADED | PluginInfo.PREPARED);
    }

    /**
	 *  Description of the Method
	 *
	 * @param  p  Description of the Parameter
	 */
    public void reloadPlugin(PluginInfo p) {
        String dir = p.getInstallDirectory();
        unloadPlugin(p);
        PluginInfo newp = preparePlugin(dir);
        loadAndStartPlugin(newp);
    }

    /**
	 *  Returns the value of preparedPluginModel.
	 *
	 * @return    the preparedPluginModel value.
	 */
    public ListModel getPreparedPluginModel() {
        return preparedPluginModel;
    }

    /**
	 *  Returns the value of runningPluginModel.
	 *
	 * @return    the runningPluginModel value.
	 */
    public ListModel getRunningPluginModel() {
        return runningPluginModel;
    }

    /**
	 *  Gets the allPlugins attribute of the PluginManager object
	 *
	 * @return    The allPlugins value
	 */
    public ListModel getAllPlugins() {
        return allPlugins;
    }

    private FilenameFilter jarFilter = new FilenameFilter() {

        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }
    };

    /**
	 *  Downloads the available plugin list from the plugin site.
	 *
	 * @return                            List of InstallInfo List<InstallInfo>
	 * @exception  MalformedURLException
	 * @exception  IOException
	 */
    public List downloadPluginList() throws IOException, MalformedURLException {
        List list = new ArrayList();
        BufferedReader br = null;
        InputStream pluginStream = null;
        try {
            br = new BufferedReader(new InputStreamReader(new URL(pluginURLBase + "/list.txt").openStream(), "Latin1"));
            do {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                Properties props = new Properties();
                pluginStream = new URL(pluginURLBase + "/" + line).openStream();
                props.load(pluginStream);
                pluginStream.close();
                InstallInfo installInfo = new InstallInfo();
                installInfo.setId(props.getProperty("plugin.id", "?"));
                installInfo.setName(props.getProperty("plugin.name", "?"));
                installInfo.setVersion(props.getProperty("plugin.version", "?"));
                installInfo.setLongDescription(props.getProperty("plugin.long.description", "?"));
                installInfo.setShortDescription(props.getProperty("plugin.short.description", "?"));
                installInfo.setURL(props.getProperty("plugin.url", "?"));
                list.add(installInfo);
            } while (true);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
            }
            try {
                if (pluginStream != null) {
                    pluginStream.close();
                }
            } catch (IOException ex) {
            }
        }
        return list;
    }

    /**
	 *  Description of the Method
	 *
	 * @param  ii                         Description of the Parameter
	 * @exception  FileNotFoundException  Description of the Exception
	 * @exception  IOException            Description of the Exception
	 */
    private void downloadPlugin(InstallInfo ii) throws FileNotFoundException, IOException {
        URL url = new URL(pluginURLBase + '/' + ii.getURL());
        InputStream is = url.openStream();
        OutputStream os = new FileOutputStream(baseDir + SystemUtils.FILE_SEPARATOR + "temp" + SystemUtils.FILE_SEPARATOR + ii.getId() + ".tmp");
        copyStream(is, os);
    }

    /**
	 *  Description of the Method
	 *
	 * @param  is               Description of the Parameter
	 * @param  os               Description of the Parameter
	 * @exception  IOException  Description of the Exception
	 */
    private void copyStream(InputStream is, OutputStream os) throws IOException {
        final int BUFFER_SIZE = 128 * 1024;
        byte buffer[] = new byte[BUFFER_SIZE];
        int c;
        while ((c = is.read(buffer)) > 0) {
            os.write(buffer, 0, c);
        }
        is.close();
        os.close();
    }

    /**
	 *  Description of the Method
	 *
	 * @exception  IOException
	 * @exception  FileSystemException
	 */
    public void removePlugin(PluginInfo p) throws FileSystemException, IOException {
        String idir = p.getInstallDirectory();
        unloadPlugin(p);
        p = null;
        Visitor v = new Visitor() {

            public boolean shouldProcess(AbstractFile af) {
                return true;
            }

            public boolean shouldVisitChildren(AbstractFile dir) {
                return true;
            }

            public void process(AbstractFile af) throws VisitorException {
                try {
                    af.delete();
                } catch (FileSystemException ex) {
                    throw new VisitorException(ex);
                }
            }
        };
        try {
            VFS.visit(VFS.resolveFile(idir), v, Visitor.DEPTH_FIRST);
        } catch (VisitorException vex) {
            throw (FileSystemException) vex.getCause();
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  ii                       Description of the Parameter
	 * @exception  IOException          Description of the Exception
	 * @exception  FileSystemException  Description of the Exception
	 */
    public void installPlugin(InstallInfo ii) throws FileSystemException, IOException {
        File dldir = new File(baseDir + SystemUtils.FILE_SEPARATOR + "temp");
        if (!dldir.exists()) {
            dldir.mkdirs();
        }
        downloadPlugin(ii);
        final AbstractFile zip = VFS.resolveFile("zip:" + baseDir + SystemUtils.FILE_SEPARATOR + "temp" + SystemUtils.FILE_SEPARATOR + ii.getId() + ".tmp!/");
        final String dstPrefix = pluginsBaseDir + SystemUtils.FILE_SEPARATOR;
        final List tasks = new ArrayList();
        Visitor v = new Visitor() {

            public boolean shouldProcess(AbstractFile af) {
                return true;
            }

            public boolean shouldVisitChildren(AbstractFile dir) {
                return true;
            }

            public void process(AbstractFile af) throws VisitorException {
                String relPath = af.getPath().substring(zip.getPath().length());
                try {
                    AbstractFile ext = VFS.resolveFile(dstPrefix + relPath);
                    TransferTask tt = new CopyTask();
                    tt.setDestinationFile(ext);
                    tt.setSourceFile(af);
                    tasks.add(tt);
                } catch (FileSystemException ex) {
                    throw new VisitorException(ex);
                }
            }
        };
        try {
            VFS.visit(zip, v, Visitor.DEPTH_FIRST);
        } catch (VisitorException vex) {
            throw (FileSystemException) vex.getCause();
        }
        for (int i = 0; i < tasks.size(); i++) {
            TransferTask tt = (TransferTask) tasks.get(i);
            tt.process();
        }
    }

    /**
	 *  Finds a plugin by its unique id.
	 *
	 * @param  id  the unique id of the plugin to be found
	 * @return     the plugin if it exists, null if not found
	 */
    public PluginInfo findPluginById(String id) {
        for (int i = 0; i < allPlugins.getSize(); i++) {
            PluginInfo p = (PluginInfo) allPlugins.getElementAt(i);
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /**
	 *  Adds a listener to this PluginManager.
	 *
	 * @param  pl  Contains the PluginListener for handling events.
	 */
    public void addPluginListener(PluginListener pl) {
        if (pl != null) {
            listeners.add(pl);
        }
    }

    /**
	 *  Removes the specified Plugin listener.
	 *
	 * @param  pl  Contains the PluginListener.
	 */
    public void removePluginListener(PluginListener pl) {
        listeners.remove(pl);
    }

    /**
	 *  Description of the Method
	 *
	 * @param  p  Description of the Parameter
	 */
    protected void firePluginPrepared(PluginInfo p) {
        for (int i = 0; i < listeners.size(); i++) {
            PluginListener pl = (PluginListener) listeners.get(i);
            pl.pluginPrepared(p);
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  p  Description of the Parameter
	 */
    protected void firePluginStarted(PluginInfo p) {
        for (int i = 0; i < listeners.size(); i++) {
            PluginListener pl = (PluginListener) listeners.get(i);
            pl.pluginStarted(p);
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  p  Description of the Parameter
	 */
    protected void firePluginUnloading(PluginInfo p) {
        for (int i = 0; i < listeners.size(); i++) {
            PluginListener pl = (PluginListener) listeners.get(i);
            pl.pluginUnloading(p);
        }
    }

    private ListModel allPlugins = new AbstractListModel() {

        public Object getElementAt(int index) {
            if (index < preparedPlugins.size()) {
                return preparedPlugins.get(index);
            } else {
                return runningPlugins.get(index - preparedPlugins.size());
            }
        }

        public int getSize() {
            return preparedPlugins.size() + runningPlugins.size();
        }
    };

    private ListModel preparedPluginModel = new AbstractListModel() {

        public Object getElementAt(int index) {
            return preparedPlugins.get(index);
        }

        public int getSize() {
            return preparedPlugins.size();
        }
    };

    private ListModel runningPluginModel = new AbstractListModel() {

        public Object getElementAt(int index) {
            return runningPlugins.get(index);
        }

        public int getSize() {
            return runningPlugins.size();
        }
    };
}
