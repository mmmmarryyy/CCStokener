package com.iver.andami;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import com.iver.andami.authentication.IAuthentication;
import com.iver.andami.config.generate.Andami;
import com.iver.andami.config.generate.AndamiConfig;
import com.iver.andami.config.generate.Plugin;
import com.iver.andami.messages.Messages;
import com.iver.andami.messages.NotificationManager;
import com.iver.andami.plugins.ExtensionDecorator;
import com.iver.andami.plugins.PluginClassLoader;
import com.iver.andami.plugins.config.generate.ActionTool;
import com.iver.andami.plugins.config.generate.Depends;
import com.iver.andami.plugins.config.generate.Extension;
import com.iver.andami.plugins.config.generate.Extensions;
import com.iver.andami.plugins.config.generate.LabelSet;
import com.iver.andami.plugins.config.generate.Menu;
import com.iver.andami.plugins.config.generate.PluginConfig;
import com.iver.andami.plugins.config.generate.PopupMenu;
import com.iver.andami.plugins.config.generate.PopupMenus;
import com.iver.andami.plugins.config.generate.SelectableTool;
import com.iver.andami.plugins.config.generate.SkinExtension;
import com.iver.andami.plugins.config.generate.SkinExtensionType;
import com.iver.andami.plugins.config.generate.ToolBar;
import com.iver.andami.ui.AndamiEventQueue;
import com.iver.andami.ui.MDIManagerLoadException;
import com.iver.andami.ui.SplashWindow;
import com.iver.andami.ui.mdiFrame.MDIFrame;
import com.iver.andami.ui.mdiManager.MDIManagerFactory;
import com.iver.utiles.XMLEntity;
import com.iver.utiles.xmlEntity.generate.XmlTag;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 6128 $
 */
public class Launcher {

    private static Logger logger = Logger.getLogger(Launcher.class.getName());

    private static AndamiConfig andamiConfig;

    private static SplashWindow splashWindow;

    private static String userHome = System.getProperty("user.home");

    private static String appName;

    private static Locale locale;

    private static HashMap pluginsConfig = new HashMap();

    private static HashMap pluginsServices = new HashMap();

    private static MDIFrame frame;

    private static HashMap classesExtensions = new HashMap();

    private static String andamiConfigPath;

    private static String pluginsPersistencePath;

    private static ArrayList pluginsOrdered = new ArrayList();

    /**
	 * DOCUMENT ME!
	 *
	 * @param args DOCUMENT ME!
	 * @throws Exception
	 *
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 * @throws ConfigurationException
	 * @throws MDIManagerLoadException
	 * @throws IOException
	 */
    public static void main(String[] args) throws Exception {
        try {
            if (!validJVM()) {
                System.exit(-1);
            }
            if (args.length < 1) {
                System.err.println("Uso: Launcher appName plugins-directory [language=locale]");
            }
            Utilities.cleanUpTempFiles();
            appName = args[0];
            File parent = new File(System.getProperty("user.home") + File.separator + args[0] + File.separator);
            parent.mkdirs();
            andamiConfigPath = System.getProperty("user.home") + File.separator + appName + File.separator + "andami-config.xml";
            pluginsPersistencePath = System.getProperty("user.home") + File.separator + appName + File.separator + "plugins-persistence.xml";
            PropertyConfigurator.configure(Launcher.class.getClassLoader().getResource("log4j.properties"));
            PatternLayout l = new PatternLayout("%p %t %C - %m%n");
            RollingFileAppender fa = new RollingFileAppender(l, System.getProperty("user.home") + File.separator + args[0] + File.separator + args[0] + ".log", false);
            fa.setMaxFileSize("512KB");
            fa.setMaxBackupIndex(3);
            Logger.getRootLogger().addAppender(fa);
            andamiConfigFromXML(andamiConfigPath);
            andamiConfig.setPluginsDirectory(args[1]);
            PluginServices.setArguments(args);
            String localeStr = null;
            for (int i = 2; i < args.length; i++) {
                int index = args[i].indexOf("language=");
                if (index != -1) localeStr = args[i].substring(index + 9);
            }
            if (localeStr == null) {
                localeStr = andamiConfig.getLocaleLanguage();
            }
            if (localeStr.compareTo("va") == 0) {
                locale = new Locale("ca");
            } else {
                locale = getLocale(localeStr, andamiConfig.getLocaleCountry(), andamiConfig.getLocaleVariant());
            }
            org.gvsig.i18n.Messages.addLocale(locale);
            org.gvsig.i18n.Messages.addLocale(new Locale("en"));
            org.gvsig.i18n.Messages.addLocale(new Locale("es"));
            org.gvsig.i18n.Messages.addResourceFamily("com.iver.andami.text", "com.iver.andami.text");
            Locale.setDefault(locale);
            JComponent.setDefaultLocale(locale);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                logger.warn(Messages.getString("Launcher.look_and_feel"), e);
            }
            splashWindow = new SplashWindow(null);
            downloadExtensions(andamiConfig.getPluginsDirectory());
            validate();
            loadPlugins(andamiConfig.getPluginsDirectory());
            pluginsClassLoaders();
            skinPlugin();
            EventQueue waitQueue = new AndamiEventQueue();
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(waitQueue);
            pluginsMessages();
            updateAndamiConfig();
            frame = new MDIFrame();
            frameIcon();
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    frame.init();
                }
            });
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    installPluginsControls();
                    installPluginsMenus();
                    installPluginsLabels();
                }
            });
            loadPluginsPersistence();
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    initializeExtensions();
                }
            });
            frame.setClassesExtensions(classesExtensions);
            splashWindow.close();
            frame.show();
            GlobalKeyEventDispatcher keyDispatcher = GlobalKeyEventDispatcher.getInstance();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    frame.enableControls();
                }
            });
        } catch (Exception e) {
            logger.error("excepci�n al arrancar", e);
            System.exit(-1);
        }
    }

    /**
	 * Recupera la geometr�a (tama�o, posici�n y estado) de la ventana principal de Andami.
	 * TODO Pendiente de ver como se asigna un pluginServices para el launcher.
	 * @author LWS
	 */
    private static void restoreMDIStatus(XMLEntity xml) {
        if (xml == null) xml = new XMLEntity();
        Dimension sz = new Dimension(700, 580);
        if (xml.contains("MDIFrameSize")) {
            int[] wh = xml.getIntArrayProperty("MDIFrameSize");
            sz = new Dimension(wh[0], wh[1]);
        }
        frame.setSize(sz);
        Point pos = new Point(10, 10);
        if (xml.contains("MDIFramePos")) {
            int[] xy = xml.getIntArrayProperty("MDIFramePos");
            pos = new Point(xy[0], xy[1]);
        }
        frame.setLocation(pos);
        int state = java.awt.Frame.MAXIMIZED_BOTH;
        if (xml.contains("MDIFrameState")) {
            state = xml.getIntProperty("MDIFrameState");
        }
        frame.setExtendedState(state);
    }

    private static XMLEntity saveMDIStatus() {
        XMLEntity xml = new XMLEntity();
        int[] wh = new int[2];
        wh[0] = frame.getWidth();
        wh[1] = frame.getHeight();
        xml.putProperty("MDIFrameSize", wh);
        int[] xy = new int[2];
        xy[0] = frame.getX();
        xy[1] = frame.getY();
        xml.putProperty("MDIFramePos", xy);
        xml.putProperty("MDIFrameState", frame.getExtendedState());
        return xml;
    }

    /**
     * @return
     */
    private static boolean validJVM() {
        char thirdCharacter = System.getProperty("java.version").charAt(2);
        if (thirdCharacter < '4') {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @throws ConfigurationException
	 */
    private static void loadPluginsPersistence() throws ConfigurationException {
        XMLEntity entity = persistenceFromXML();
        for (int i = 0; i < entity.getNumChild(); i++) {
            XMLEntity plugin = entity.getChild(i);
            String pName = plugin.getStringProperty("com.iver.andami.pluginName");
            if (pluginsServices.get(pName) != null) {
                ((PluginServices) pluginsServices.get(pName)).setPersistentXML(plugin);
            } else {
                if (pName.startsWith("Andami.Launcher")) restoreMDIStatus(plugin);
            }
        }
    }

    /**
	 * Salva la persistencia de los plugins.
	 * @author LWS
	 */
    private static void savePluginPersistence() {
        Iterator i = pluginsConfig.keySet().iterator();
        XMLEntity entity = new XMLEntity();
        while (i.hasNext()) {
            String pName = (String) i.next();
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            XMLEntity ent = ps.getPersistentXML();
            if (ent != null) {
                ent.putProperty("com.iver.andami.pluginName", pName);
                entity.addChild(ent);
            }
        }
        XMLEntity ent = saveMDIStatus();
        if (ent != null) {
            ent.putProperty("com.iver.andami.pluginName", "Andami.Launcher");
            entity.addChild(ent);
        }
        try {
            persistenceToXML(entity);
        } catch (ConfigurationException e1) {
            logger.error(Messages.getString("Launcher.Se_produjo_un_error_guardando_la_configuracion_de_los_plugins"), e1);
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static void installPluginsLabels() {
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String name = (String) i.next();
            PluginConfig pc = (PluginConfig) pluginsConfig.get(name);
            PluginServices ps = (PluginServices) pluginsServices.get(name);
            LabelSet[] ls = pc.getLabelSet();
            for (int j = 0; j < ls.length; j++) {
                PluginClassLoader loader = ps.getClassLoader();
                try {
                    Class clase = loader.loadClass(ls[j].getClassName());
                    frame.setLabels(clase, ls[j].getLabel());
                } catch (ClassNotFoundException e) {
                    logger.error(Messages.getString("Launcher.labelset_class"), e);
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @throws MDIManagerLoadException
	 */
    private static void skinPlugin() throws MDIManagerLoadException {
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String name = (String) i.next();
            PluginConfig pc = (PluginConfig) pluginsConfig.get(name);
            PluginServices ps = (PluginServices) pluginsServices.get(name);
            if (pc.getExtensions().getSkinExtension() != null) {
                if (MDIManagerFactory.getSkinExtension() != null) {
                    logger.warn(Messages.getString("Launcher.Dos_skin_extension"));
                }
                SkinExtension se = pc.getExtensions().getSkinExtension();
                MDIManagerFactory.setSkinExtension(se, ps.getClassLoader());
                Class skinClass;
                try {
                    skinClass = ps.getClassLoader().loadClass(se.getClassName());
                    com.iver.andami.plugins.IExtension skinInstance = (com.iver.andami.plugins.IExtension) skinClass.newInstance();
                    ExtensionDecorator newExtensionDecorator = new ExtensionDecorator(skinInstance, ExtensionDecorator.INACTIVE);
                    classesExtensions.put(skinClass, newExtensionDecorator);
                } catch (ClassNotFoundException e) {
                    logger.error(Messages.getString("Launcher.No_se_encontro_la_clase_mdi_manager"), e);
                    throw new MDIManagerLoadException(e);
                } catch (InstantiationException e) {
                    logger.error(Messages.getString("Launcher.No_se_pudo_instanciar_la_clase_mdi_manager"), e);
                    throw new MDIManagerLoadException(e);
                } catch (IllegalAccessException e) {
                    logger.error(Messages.getString("Launcher.No_se_pudo_acceder_a_la_clase_mdi_manager"), e);
                    throw new MDIManagerLoadException(e);
                }
            }
        }
    }

    /**
	 *
	 */
    private static void frameIcon() {
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String pName = (String) i.next();
            PluginConfig pc = (PluginConfig) pluginsConfig.get(pName);
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            if (pc.getIcon() != null) {
                ImageIcon icon = new ImageIcon(ps.getClassLoader().getResource(pc.getIcon().getSrc()));
                frame.setIconImage(icon.getImage());
                frame.setTitlePrefix(pc.getIcon().getText());
            }
        }
    }

    /**
	 *
	 */
    private static void initializeExtensions() {
        Iterator i = pluginsOrdered.iterator();
        while (i.hasNext()) {
            String pName = (String) i.next();
            logger.debug("Initializing extensions from " + pName);
            PluginConfig pc = (PluginConfig) pluginsConfig.get(pName);
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            Extension[] exts = pc.getExtensions().getExtension();
            TreeMap orderedExtensions = new TreeMap(new ExtensionComparator());
            for (int j = 0; j < exts.length; j++) {
                if (!exts[j].getActive()) {
                    continue;
                }
                if (orderedExtensions.containsKey(exts[j])) {
                    logger.warn(Messages.getString("Launcher.Two_extensions_with_the_same_priority") + exts[j].getClassName());
                }
                orderedExtensions.put(exts[j], null);
            }
            Iterator e = orderedExtensions.keySet().iterator();
            while (e.hasNext()) {
                Extension extension = (Extension) e.next();
                com.iver.andami.plugins.IExtension extensionInstance;
                try {
                    Class extensionClass = ps.getClassLoader().loadClass(extension.getClassName());
                    extensionInstance = (com.iver.andami.plugins.IExtension) extensionClass.newInstance();
                    ExtensionDecorator newExtensionDecorator = new ExtensionDecorator(extensionInstance, ExtensionDecorator.INACTIVE);
                    classesExtensions.put(extensionClass, newExtensionDecorator);
                    System.err.println("Loading " + extension.getClassName() + "...");
                    extensionInstance.initialize();
                } catch (InstantiationException e1) {
                    logger.error(Messages.getString("Launcher.Error_instanciando_la_extension") + extension.getClassName(), e1);
                } catch (IllegalAccessException e1) {
                    logger.error(Messages.getString("Launcher.Error_instanciando_la_extension") + extension.getClassName(), e1);
                } catch (ClassNotFoundException e1) {
                    logger.error(Messages.getString("Launcher.No_se_encontro_la_clase_de_la_extension") + extension.getClassName(), e1);
                } catch (NoClassDefFoundError e1) {
                    logger.error(Messages.getString("Launcher.Error_localizando_la_clase_de_la_extension") + extension.getClassName(), e1);
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static void installPluginsMenus() {
        TreeMap orderedMenus = new TreeMap(new MenuComparator());
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String pName = (String) i.next();
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            PluginConfig pc = (PluginConfig) pluginsConfig.get(pName);
            Extension[] exts = pc.getExtensions().getExtension();
            for (int j = 0; j < exts.length; j++) {
                if (!exts[j].getActive()) {
                    continue;
                }
                Menu[] menus = exts[j].getMenu();
                for (int k = 0; k < menus.length; k++) {
                    SortableMenu sm = new SortableMenu(ps.getClassLoader(), exts[j], menus[k]);
                    if (orderedMenus.containsKey(sm)) {
                        logger.error(Messages.getString("Launcher.Two_menus_with_the_same_position") + exts[j].getClassName());
                    }
                    orderedMenus.put(sm, null);
                }
            }
            SkinExtension skinExt = pc.getExtensions().getSkinExtension();
            if (skinExt != null) {
                Menu[] menu = skinExt.getMenu();
                for (int k = 0; k < menu.length; k++) {
                    SortableMenu sm = new SortableMenu(ps.getClassLoader(), skinExt, menu[k]);
                    if (orderedMenus.containsKey(sm)) {
                        logger.error(Messages.getString("Launcher.Two_menus_with_the_same_position") + skinExt.getClassName());
                    }
                    orderedMenus.put(sm, null);
                }
            }
        }
        Iterator e = orderedMenus.keySet().iterator();
        while (e.hasNext()) {
            try {
                SortableMenu sm = (SortableMenu) e.next();
                frame.addMenu(sm.loader, sm.extension, sm.menu);
            } catch (ClassNotFoundException ex) {
                logger.error(Messages.getString("Launcher.No_se_encontro_la_clase_de_la_extension"), ex);
            }
        }
    }

    /**
	 * Installs the menus, toolbars, actiontools and selectable toolbars.
	 * The order in which they are shown is determined here. 
	 */
    private static void installPluginsControls() {
        Iterator i = pluginsConfig.keySet().iterator();
        HashMap extensionPluginServices = new HashMap();
        HashMap extensionPluginConfig = new HashMap();
        TreeMap orderedExtensions = new TreeMap(new ExtensionComparator());
        Object previous;
        while (i.hasNext()) {
            String pName = (String) i.next();
            PluginConfig pc = (PluginConfig) pluginsConfig.get(pName);
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            Extension[] exts = pc.getExtensions().getExtension();
            for (int j = 0; j < exts.length; j++) {
                if (exts[j].getActive()) {
                    if (orderedExtensions.containsKey(exts[j])) {
                        logger.error(Messages.getString("Launcher.Two_extensions_with_the_same_priority") + exts[j].getClassName());
                    }
                    orderedExtensions.put(exts[j], null);
                    extensionPluginServices.put(exts[j], ps);
                    extensionPluginConfig.put(exts[j], pc);
                }
            }
        }
        TreeMap orderedTools = new TreeMap(new ToolComparator());
        Iterator e = orderedExtensions.keySet().iterator();
        while (e.hasNext()) {
            Extension ext = (Extension) e.next();
            ToolBar[] toolbars = ext.getToolBar();
            for (int k = 0; k < toolbars.length; k++) {
                ActionTool[] tools = toolbars[k].getActionTool();
                for (int t = 0; t < tools.length; t++) {
                    SortableToolBar sm = new SortableToolBar(((PluginServices) extensionPluginServices.get(ext)).getClassLoader(), ext, toolbars[k], tools[t]);
                    previous = orderedTools.put(sm, null);
                }
                SelectableTool[] sTools = toolbars[k].getSelectableTool();
                for (int t = 0; t < sTools.length; t++) {
                    SortableToolBar sm = new SortableToolBar(((PluginServices) extensionPluginServices.get(ext)).getClassLoader(), ext, toolbars[k], sTools[t]);
                    previous = orderedTools.put(sm, null);
                }
            }
        }
        i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String pName = (String) i.next();
            PluginConfig pc = (PluginConfig) pluginsConfig.get(pName);
            PluginServices ps = (PluginServices) pluginsServices.get(pName);
            SkinExtension skinExt = pc.getExtensions().getSkinExtension();
            if (skinExt != null) {
                ToolBar[] toolbars = skinExt.getToolBar();
                for (int k = 0; k < toolbars.length; k++) {
                    ActionTool[] tools = toolbars[k].getActionTool();
                    for (int t = 0; t < tools.length; t++) {
                        SortableToolBar stb = new SortableToolBar(ps.getClassLoader(), skinExt, toolbars[k], tools[t]);
                        previous = orderedTools.put(stb, null);
                    }
                    SelectableTool[] sTools = toolbars[k].getSelectableTool();
                    for (int t = 0; t < sTools.length; t++) {
                        SortableToolBar stb = new SortableToolBar(ps.getClassLoader(), skinExt, toolbars[k], sTools[t]);
                        previous = orderedTools.put(stb, null);
                    }
                }
            }
            PopupMenus pus = pc.getPopupMenus();
            if (pus != null) {
                PopupMenu[] menus = pus.getPopupMenu();
                for (int j = 0; j < menus.length; j++) {
                    frame.addPopupMenu(ps.getClassLoader(), menus[j]);
                }
            }
        }
        Iterator tt = orderedTools.keySet().iterator();
        while (tt.hasNext()) {
            SortableToolBar kk = (SortableToolBar) tt.next();
            kk.getClass();
        }
        Iterator t = orderedTools.keySet().iterator();
        while (t.hasNext()) {
            try {
                SortableToolBar stb = (SortableToolBar) t.next();
                if (stb.actiontool != null) frame.addTool(stb.loader, stb.extension, stb.toolbar, stb.actiontool); else frame.addTool(stb.loader, stb.extension, stb.toolbar, stb.selectabletool);
            } catch (ClassNotFoundException ex) {
                logger.error(Messages.getString("Launcher.No_se_encontro_la_clase_de_la_extension"), ex);
            }
        }
    }

    /**
	 *
	 */
    private static void updateAndamiConfig() {
        HashSet olds = new HashSet();
        Plugin[] plugins = andamiConfig.getPlugin();
        for (int i = 0; i < plugins.length; i++) {
            olds.add(plugins[i].getName());
        }
        Iterator i = pluginsServices.values().iterator();
        while (i.hasNext()) {
            PluginServices ps = (PluginServices) i.next();
            if (!olds.contains(ps.getPluginName())) {
                Plugin p = new Plugin();
                p.setName(ps.getPluginName());
                p.setUpdate(false);
                andamiConfig.addPlugin(p);
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static void pluginsClassLoaders() {
        HashSet instalados = new HashSet();
        while (instalados.size() != pluginsConfig.size()) {
            boolean circle = true;
            Iterator i = pluginsConfig.keySet().iterator();
            while (i.hasNext()) {
                String pluginName = (String) i.next();
                PluginConfig config = (PluginConfig) pluginsConfig.get(pluginName);
                if (instalados.contains(pluginName)) {
                    continue;
                }
                boolean ready = true;
                Depends[] dependencies = config.getDepends();
                PluginClassLoader[] loaders = new PluginClassLoader[dependencies.length];
                for (int j = 0; j < dependencies.length; j++) {
                    if (pluginsConfig.get(dependencies[j].getPluginName()) == null) {
                        logger.error(Messages.getString("Launcher.Dependencia_no_resuelta_en_plugin") + pluginName + ": " + dependencies[j].getPluginName());
                        continue;
                    }
                    if (!instalados.contains(dependencies[j].getPluginName())) {
                        ready = false;
                    } else {
                        loaders[j] = ((PluginServices) pluginsServices.get(dependencies[j].getPluginName())).getClassLoader();
                    }
                }
                if (!ready) {
                    continue;
                }
                String jardir = config.getLibraries().getLibraryDir();
                File jarDir = new File(andamiConfig.getPluginsDirectory() + File.separator + pluginName + File.separator + jardir);
                File[] jarFiles = jarDir.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return (pathname.getName().toUpperCase().endsWith(".JAR")) || (pathname.getName().toUpperCase().endsWith(".ZIP"));
                    }
                });
                URL[] urls = new URL[jarFiles.length];
                for (int j = 0; j < jarFiles.length; j++) {
                    try {
                        urls[j] = new URL("file:" + jarFiles[j]);
                    } catch (MalformedURLException e) {
                        logger.error(Messages.getString("Launcher.No_se_puede_acceder_a") + jarFiles[j]);
                    }
                }
                PluginClassLoader loader;
                try {
                    loader = new PluginClassLoader(urls, andamiConfig.getPluginsDirectory() + File.separator + pluginName, Launcher.class.getClassLoader(), loaders);
                    PluginServices ps = new PluginServices(loader);
                    pluginsServices.put(ps.getPluginName(), ps);
                    instalados.add(pluginName);
                    pluginsOrdered.add(pluginName);
                    circle = false;
                } catch (IOException e) {
                    logger.error(Messages.getString("Launcher.Error_con_las_librerias_del_plugin"), e);
                    pluginsConfig.remove(pluginName);
                    i = pluginsConfig.keySet().iterator();
                }
            }
            if (circle) {
                logger.error(Messages.getString("Launcher.Hay_dependencias_circulares"));
                break;
            }
        }
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String pluginName = (String) i.next();
            PluginConfig config = (PluginConfig) pluginsConfig.get(pluginName);
            PluginServices ps = (PluginServices) pluginsServices.get(pluginName);
            if (ps == null) {
                pluginsConfig.remove(pluginName);
                i = pluginsConfig.keySet().iterator();
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static void pluginsMessages() {
        PluginConfig config = (PluginConfig) pluginsConfig.get("com.iver.cit.gvsig");
        PluginServices ps = (PluginServices) pluginsServices.get("com.iver.cit.gvsig");
        if (config.getResourceBundle() != null) {
            org.gvsig.i18n.Messages.addResourceFamily(config.getResourceBundle().getName(), ps.getClassLoader(), "com.iver.cit.gvsig");
        }
        Iterator i = pluginsConfig.keySet().iterator();
        while (i.hasNext()) {
            String pluginName = (String) i.next();
            if (!pluginName.equals("com.iver.cit.gvsig")) {
                config = (PluginConfig) pluginsConfig.get(pluginName);
                ps = (PluginServices) pluginsServices.get(pluginName);
                if (config.getResourceBundle() != null && !config.getResourceBundle().getName().equals("")) {
                    org.gvsig.i18n.Messages.addResourceFamily(config.getResourceBundle().getName(), ps.getClassLoader(), pluginName);
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    static PluginServices getPluginServices(String name) {
        return (PluginServices) pluginsServices.get(name);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    static String getPluginsDir() {
        return andamiConfig.getPluginsDirectory();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param s DOCUMENT ME!
	 */
    static void setPluginsDir(String s) {
        andamiConfig.setPluginsDirectory(s);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    static MDIFrame getMDIFrame() {
        return frame;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param pluginsDirectory
	 */
    private static void loadPlugins(String pluginsDirectory) {
        File pDir = new File(pluginsDirectory);
        if (!pDir.exists()) {
            return;
        }
        File[] pluginDirs = pDir.listFiles();
        for (int i = 0; i < pluginDirs.length; i++) {
            if (pluginDirs[i].isDirectory()) {
                File configXml = new File(pluginDirs[i].getAbsolutePath() + File.separator + "config.xml");
                try {
                    FileReader xml = new FileReader(configXml);
                    PluginConfig pConfig = (PluginConfig) PluginConfig.unmarshal(xml);
                    pluginsConfig.put(pluginDirs[i].getName(), pConfig);
                } catch (FileNotFoundException e) {
                    logger.info(Messages.getString("Launcher.Ignorando_el_directorio") + pluginDirs[i].getAbsolutePath() + Messages.getString("Launcher.config_no_encontrado"));
                } catch (MarshalException e) {
                    logger.info(Messages.getString("Launcher.Ignorando_el_directorio") + pluginDirs[i].getAbsolutePath() + Messages.getString("Launcher.config_mal_formado"), e);
                } catch (ValidationException e) {
                    logger.info(Messages.getString("Launcher.Ignorando_el_directorio") + pluginDirs[i].getAbsolutePath() + Messages.getString("Launcher.config_mal_formado"), e);
                }
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param language
	 * @param country
	 * @param variant
	 *
	 * @return DOCUMENT ME!
	 */
    private static Locale getLocale(String language, String country, String variant) {
        if (variant != null) {
            return new Locale(language, country, variant);
        } else if (country != null) {
            return new Locale(language, country);
        } else if (language != null) {
            return new Locale(language);
        } else {
            return new Locale("es");
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 * @throws MarshalException DOCUMENT ME!
	 * @throws ValidationException DOCUMENT ME!
	 */
    private static void andamiConfigToXML(String file) throws IOException, MarshalException, ValidationException {
        File xml = new File(file);
        File parent = xml.getParentFile();
        parent.mkdirs();
        FileWriter writer = new FileWriter(xml);
        andamiConfig.marshal(writer);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 *
	 * @throws ConfigurationException DOCUMENT ME!
	 */
    private static void andamiConfigFromXML(String file) throws ConfigurationException {
        File xml = new File(file);
        if (!xml.exists()) {
            andamiConfig = new AndamiConfig();
            Andami andami = new Andami();
            andami.setUpdate(true);
            andamiConfig.setAndami(andami);
            andamiConfig.setLocaleCountry(Locale.getDefault().getCountry());
            andamiConfig.setLocaleLanguage(Locale.getDefault().getLanguage());
            andamiConfig.setLocaleVariant(Locale.getDefault().getVariant());
            if (System.getProperty("javawebstart.version") != null) {
                andamiConfig.setPluginsDirectory(new File(System.getProperty("user.home") + File.separator + appName + File.separator + "extensiones").getAbsolutePath());
            } else {
                andamiConfig.setPluginsDirectory(new File(appName + File.separator + "extensiones").getAbsolutePath());
            }
            andamiConfig.setPlugin(new Plugin[0]);
        } else {
            FileReader reader;
            try {
                reader = new FileReader(xml);
                andamiConfig = (AndamiConfig) AndamiConfig.unmarshal(reader);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (MarshalException e) {
                throw new ConfigurationException(e);
            } catch (ValidationException e) {
                throw new ConfigurationException(e);
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws ConfigurationException DOCUMENT ME!
	 */
    private static XMLEntity persistenceFromXML() throws ConfigurationException {
        File xml = new File(pluginsPersistencePath);
        if (xml.exists()) {
            FileReader reader;
            try {
                reader = new FileReader(xml);
                XmlTag tag = (XmlTag) XmlTag.unmarshal(reader);
                return new XMLEntity(tag);
            } catch (FileNotFoundException e) {
                throw new ConfigurationException(e);
            } catch (MarshalException e) {
                throw new ConfigurationException(e);
            } catch (ValidationException e) {
                throw new ConfigurationException(e);
            }
        } else {
            return new XMLEntity();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param entity DOCUMENT ME!
	 *
	 * @throws ConfigurationException DOCUMENT ME!
	 */
    private static void persistenceToXML(XMLEntity entity) throws ConfigurationException {
        File xml = new File(pluginsPersistencePath);
        FileWriter writer;
        try {
            writer = new FileWriter(xml);
            entity.getXmlTag().marshal(writer);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(e);
        } catch (MarshalException e) {
            throw new ConfigurationException(e);
        } catch (ValidationException e) {
            throw new ConfigurationException(e);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
	 * Devuelve un array con los directorios de los plugins
	 *
	 * @param dirExt Directorio de las extensiones a partir del cual cuelgan
	 * 		  todos los directorios de los plugins
	 *
	 * @return ArrayList con los directorios
	 */
    private String[] getLocales(File dirExt) {
        ArrayList types = new ArrayList();
        File[] files = dirExt.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                File[] textFile = files[i].listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String fileName) {
                        return fileName.toLowerCase().startsWith("text_");
                    }
                });
                for (int j = 0; j < textFile.length; j++) {
                    String s = (textFile[j]).getName().replaceAll("text_", "");
                    s = s.replaceAll(".properties", "");
                    s = s.trim();
                    if (!types.contains(s)) {
                        types.add(s);
                    }
                }
            }
        }
        return (String[]) types.toArray(new String[0]);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return Returns the frame.
	 */
    static MDIFrame getFrame() {
        return frame;
    }

    /**
	 * Secuencia de cerrado de Andami
	 */
    public static void closeApplication() {
        try {
            andamiConfigToXML(andamiConfigPath);
        } catch (MarshalException e) {
            logger.error(Messages.getString("Launcher.No_se_pudo_guardar_la_configuracion_de_andami"), e);
        } catch (ValidationException e) {
            logger.error(Messages.getString("Launcher.No_se_pudo_guardar_la_configuracion_de_andami"), e);
        } catch (IOException e) {
            logger.error(Messages.getString("Launcher.No_se_pudo_guardar_la_configuracion_de_andami"), e);
        }
        savePluginPersistence();
        finalizeExtensions();
        Utilities.cleanUpTempFiles();
        System.gc();
        System.exit(0);
    }

    /**
	 * Exectutes the finalize method for all the extensions
	 *
	 */
    private static void finalizeExtensions() {
        Set extensions = getClassesExtensions().keySet();
        Object[] keys = extensions.toArray();
        for (int i = 0; i < keys.length; i++) {
            ExtensionDecorator extensionDecorator = (ExtensionDecorator) getClassesExtensions().get(keys[i]);
            extensionDecorator.getExtension().finalize();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    static HashMap getClassesExtensions() {
        return classesExtensions;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param extDir DOCUMENT ME!
	 */
    private static void downloadExtensions(String extDir) {
        java.util.Date fechaActual = null;
        try {
            if (System.getProperty("javawebstart.version") != null) {
                BasicService bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
                URL baseURL = bs.getCodeBase();
                SplashWindow.process(5, "Descargando las extensiones desde " + baseURL + " a " + extDir);
                URL url = new URL(baseURL + "extensiones.zip");
                URLConnection connection = url.openConnection();
                System.out.println(url.toExternalForm() + ":");
                System.out.println("  Content Type: " + connection.getContentType());
                System.out.println("  Content Length: " + connection.getContentLength());
                System.out.println("  Last Modified: " + new Date(connection.getLastModified()));
                System.out.println("  Expiration: " + connection.getExpiration());
                System.out.println("  Content Encoding: " + connection.getContentEncoding());
                Long miliSecondsInWeb = new Long(connection.getLastModified());
                File destDir = new File(extDir);
                if (!destDir.exists()) {
                    destDir.getParentFile().mkdir();
                    if (!destDir.mkdir()) {
                        System.err.println("Imposible crear el directorio " + destDir.getAbsolutePath());
                    }
                }
                File timeFile = new File(destDir.getParent() + File.separator + "timeStamp.properties");
                if (!timeFile.exists()) {
                    timeFile.createNewFile();
                }
                FileInputStream inAux = new FileInputStream(timeFile);
                Properties prop = new Properties();
                prop.load(inAux);
                inAux.close();
                if (prop.getProperty("timestamp") != null) {
                    Long lastMiliSeconds = (Long) new Long(prop.getProperty("timestamp"));
                    if (lastMiliSeconds.longValue() == miliSecondsInWeb.longValue()) {
                        System.out.println("No hay nueva actualizaci�n");
                        return;
                    }
                    System.out.println("timeStampWeb= " + miliSecondsInWeb);
                    System.out.println("timeStampLocal= " + lastMiliSeconds);
                } else {
                    System.out.println("El timeStamp no est� escrito en " + timeFile.getAbsolutePath());
                }
                InputStream stream = connection.getInputStream();
                File temp = File.createTempFile("gvsig", ".zip");
                temp.deleteOnExit();
                FileOutputStream file = new FileOutputStream(temp);
                BufferedInputStream in = new BufferedInputStream(stream);
                BufferedOutputStream out = new BufferedOutputStream(file);
                int i;
                int pct;
                int desde;
                int hasta;
                hasta = connection.getContentLength() / 1024;
                desde = 0;
                while ((i = in.read()) != -1) {
                    pct = ((desde / 1024) * 100) / hasta;
                    if (((desde % 10240) == 0) && (pct > 10) && ((pct % 10) == 0)) {
                        SplashWindow.process(pct, (desde / 1024) + "Kb de " + hasta + "Kb descargados...");
                    }
                    out.write(i);
                    desde++;
                }
                out.flush();
                out.close();
                in.close();
                SplashWindow.process(5, "Extensiones descargadas.");
                System.out.println("Extrayendo a " + destDir.getAbsolutePath());
                Date fechaDir = new Date(destDir.lastModified());
                System.out.println("Fecha del directorio " + extDir + " = " + fechaDir.toString());
                Utilities.extractTo(temp, new File(extDir), splashWindow);
                fechaActual = new java.util.Date();
                FileOutputStream outAux = new FileOutputStream(timeFile);
                prop.setProperty("timestamp", miliSecondsInWeb.toString());
                prop.store(outAux, "last download");
                outAux.close();
                System.out.println("Fecha actual guardada: " + fechaActual.toGMTString());
            }
        } catch (IOException e) {
            NotificationManager.addError("", e);
        } catch (UnavailableServiceException e) {
            NotificationManager.addError("", e);
        } catch (SecurityException e) {
            System.err.println("No se puede escribir el timeStamp " + fechaActual.toGMTString());
            NotificationManager.addError("", e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private static Extensions[] getExtensions() {
        ArrayList array = new ArrayList();
        Iterator iter = pluginsConfig.values().iterator();
        while (iter.hasNext()) {
            array.add(((PluginConfig) iter.next()).getExtensions());
        }
        return (Extensions[]) array.toArray(new Extensions[0]);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static HashMap getPluginConfig() {
        return pluginsConfig;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param s DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static Extension getExtension(String s) {
        Extensions[] exts = getExtensions();
        for (int i = 0; i < exts.length; i++) {
            for (int j = 0; j < exts[i].getExtensionCount(); j++) {
                if (exts[i].getExtension(j).getClassName().equals(s)) {
                    return exts[i].getExtension(j);
                }
            }
        }
        return null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static AndamiConfig getAndamiConfig() {
        return andamiConfig;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @author $author$
	 * @version $Revision: 6128 $
	 */
    private static class ExtensionComparator implements Comparator {

        /**
		 * DOCUMENT ME!
		 *
		 * @param o1 DOCUMENT ME!
		 * @param o2 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public int compare(Object o1, Object o2) {
            Extension e1 = (Extension) o1;
            Extension e2 = (Extension) o2;
            if (!e1.hasPriority() && !e2.hasPriority()) {
                return -1;
            }
            if (e1.hasPriority() && !e2.hasPriority()) {
                return -Integer.MAX_VALUE;
            }
            if (e2.hasPriority() && !e1.hasPriority()) {
                return Integer.MAX_VALUE;
            }
            if (e1.getPriority() != e2.getPriority()) {
                return e2.getPriority() - e1.getPriority();
            } else {
                return (e2.toString().compareTo(e1.toString()));
            }
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static class MenuComparator implements Comparator {

        private static ExtensionComparator extComp = new ExtensionComparator();

        /**
		 * DOCUMENT ME!
		 *
		 * @param o1 DOCUMENT ME!
		 * @param o2 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public int compare(Object o1, Object o2) {
            SortableMenu e1 = (SortableMenu) o1;
            SortableMenu e2 = (SortableMenu) o2;
            if (!e1.menu.hasPosition() && !e2.menu.hasPosition()) {
                if (e1.extension instanceof SkinExtensionType) {
                    return 1;
                } else if (e2.extension instanceof SkinExtensionType) {
                    return -1;
                } else {
                    return extComp.compare(e1.extension, e2.extension);
                }
            }
            if (e1.menu.hasPosition() && !e2.menu.hasPosition()) {
                return -Integer.MAX_VALUE;
            }
            if (e2.menu.hasPosition() && !e1.menu.hasPosition()) {
                return Integer.MAX_VALUE;
            }
            return e1.menu.getPosition() - e2.menu.getPosition();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @author $author$
	 * @version $Revision: 6128 $
	 */
    private static class SortableMenu {

        public PluginClassLoader loader;

        public Menu menu;

        public SkinExtensionType extension;

        /**
		 * DOCUMENT ME!
		 *
		 * @param loader DOCUMENT ME!
		 * @param skinExt
		 * @param menu2
		 */
        public SortableMenu(PluginClassLoader loader, SkinExtensionType skinExt, Menu menu2) {
            extension = skinExt;
            menu = menu2;
            this.loader = loader;
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static class SortableToolBar {

        public PluginClassLoader loader;

        public ToolBar toolbar;

        public ActionTool actiontool;

        public SelectableTool selectabletool;

        public SkinExtensionType extension;

        /**
		 * DOCUMENT ME!
		 *
		 * @param loader DOCUMENT ME!
		 * @param skinExt
		 * @param menu2
		 */
        public SortableToolBar(PluginClassLoader loader, SkinExtensionType skinExt, ToolBar toolbar2, ActionTool actiontool2) {
            extension = skinExt;
            toolbar = toolbar2;
            actiontool = actiontool2;
            this.loader = loader;
        }

        public SortableToolBar(PluginClassLoader loader, SkinExtensionType skinExt, ToolBar toolbar2, SelectableTool selectabletool2) {
            extension = skinExt;
            toolbar = toolbar2;
            selectabletool = selectabletool2;
            this.loader = loader;
        }
    }

    /**
	 * DOCUMENT ME!
	 */
    private static class ToolBarComparator implements Comparator {

        private static ExtensionComparator extComp = new ExtensionComparator();

        /**
		 * DOCUMENT ME!
		 *
		 * @param o1 DOCUMENT ME!
		 * @param o2 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public int compare(Object o1, Object o2) {
            SortableToolBar e1 = (SortableToolBar) o1;
            SortableToolBar e2 = (SortableToolBar) o2;
            if (e1.toolbar.getName().equals(e2.toolbar.getName())) return 0;
            if (!e1.toolbar.hasPosition() && !e2.toolbar.hasPosition()) {
                if (e1.extension instanceof SkinExtensionType) {
                    return 1;
                } else if (e2.extension instanceof SkinExtensionType) {
                    return -1;
                } else {
                    return extComp.compare(e1.extension, e2.extension);
                }
            }
            if (e1.toolbar.hasPosition() && !e2.toolbar.hasPosition()) {
                return -Integer.MAX_VALUE;
            }
            if (e2.toolbar.hasPosition() && !e1.toolbar.hasPosition()) {
                return Integer.MAX_VALUE;
            }
            if (e1.toolbar.getPosition() != e2.toolbar.getPosition()) return e1.toolbar.getPosition() - e2.toolbar.getPosition();
            if (e1.toolbar.getActionTool().equals(e2.toolbar.getActionTool()) && e1.toolbar.getSelectableTool().equals(e2.toolbar.getSelectableTool())) {
                return 0;
            }
            return (e1.toolbar.toString().compareTo(e2.toolbar.toString()));
        }
    }

    /**
	 * <p>This class is used to compare tools (selectabletool and actiontool),
	 * using the "position"
	 * attribute.</p>
	 * <p>The ordering criteria are:</p>
	 * <ul><li>If the tools are placed in different toolbars, they use the toolbars'
	 * order.
	 * (using the ToolBarComparator).</li>
	 * <li></li>
	 * <li>If any of the tools has not 'position' attribute, the tool which
	 * <strong>has</strong> the attribute will be placed first.</li>
	 * <li>If both tools have the same position (or they don't have a
	 * 'position' attribute), the priority of the extensions where the tool is defined.</li></ul>
	 *
	 * @author cesar
	 * @version $Revision: 6128 $
	 */
    private static class ToolComparator implements Comparator {

        private static ToolBarComparator toolBarComp = new ToolBarComparator();

        /**
		 * DOCUMENT ME!
		 *
		 * @param o1 DOCUMENT ME!
		 * @param o2 DOCUMENT ME!
		 *
		 * @return DOCUMENT ME!
		 */
        public int compare(Object o1, Object o2) {
            int result = toolBarComp.compare(o1, o2);
            if (result != 0) {
                return result;
            }
            SortableToolBar e1 = (SortableToolBar) o1;
            SortableToolBar e2 = (SortableToolBar) o2;
            int e1Position = -1, e2Position = -1;
            if (e1.actiontool != null) {
                if (e1.actiontool.hasPosition()) e1Position = e1.actiontool.getPosition();
            } else if (e1.selectabletool != null) {
                if (e1.selectabletool.hasPosition()) e1Position = e1.selectabletool.getPosition();
            }
            if (e2.actiontool != null) {
                if (e2.actiontool.hasPosition()) e2Position = e2.actiontool.getPosition();
            } else if (e2.selectabletool != null) {
                if (e2.selectabletool.hasPosition()) e2Position = e2.selectabletool.getPosition();
            }
            if (e1Position == -1 && e2Position != -1) {
                return 1;
            }
            if (e1Position != -1 && e2Position == -1) {
                return -1;
            }
            if (e1Position != -1 && e2Position != -1) {
                result = e1Position - e2Position;
                if (result != 0) return result;
            }
            return e1.toString().compareTo(e2.toString());
        }
    }

    /**
	 * validates the user before starting gvsig
	 *
	 */
    private static void validate() {
        IAuthentication session = null;
        try {
            session = (IAuthentication) Class.forName("com.iver.andami.authentication.Session").newInstance();
        } catch (ClassNotFoundException e) {
            return;
        } catch (InstantiationException e) {
            return;
        } catch (IllegalAccessException e) {
            return;
        }
        session.setPluginDirectory(andamiConfig.getPluginsDirectory());
        if (session.validationRequired()) {
            session.getUser();
            if (session.Login((String) session.get("user"), (String) session.get("pwd"))) {
                System.out.println("You are logged in");
            } else {
                JOptionPane.showMessageDialog((Component) PluginServices.getMainFrame(), "You are not logged in");
                System.exit(0);
            }
            PluginServices.setAuthentication(session);
        }
    }
}
