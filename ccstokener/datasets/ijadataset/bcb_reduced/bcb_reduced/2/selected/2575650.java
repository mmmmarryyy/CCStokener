package org.xito.boot;

import java.security.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import org.xito.dialog.*;
import org.xito.reflect.*;
import org.xito.boot.ui.Defaults;
import org.xito.boot.util.WeakVector;

/**
 * <p>
 * Boot is the main entry point to the BootStrap Environment. Boot will read properties from the command line and
 * from a boot.properties from the boot dir.
 * </p>
 * <p>
 * Unless a different main application is specified in boot.properties BootStrap
 * will call Shell.main which will startup specified boot and startup services
 * </p>
 * <p><b>Optional Command Line Arguments</b></p>
 * <pre>
 * -bootdir  Directory where bootstrap environment is loaded from<br>
 * -nogui    Do not show any gui messages etc. Run in headless mode<br>
 * </pre>
 * <p>
 * All command line options of the form -argname argvalue will be stored and can be read later by
 * calling Boot.getArgProperties()
 * </p>
 * <p><b>boot.properties</b></p>
 * <p>
 * The BootStrap will read a set of properties from a boot.properties file found in
 * the bootdir. The following properties can be specified:
 * </p>
 * <pre>
 * app.name   Short name of this application used to store user specific files in user's home
 * app.icon   URL to image file for Applications icon. Can be relative to bootdir
 * nativeLAF  Set to true to use System native look and feel false otherwise. Defaults to true.
 * app.main   Class name of main application defaults to Shell
 * </pre>
 * <p>All properties in boot.properties will also be added to System properties. Therefore you can use boot.properties to set System
 * wide VM settings etc.
 * </p>
 *
 * @author Deane Richan
 * @version $revision$
 */
public class Boot {

    private static final Logger logger = Logger.getLogger(Boot.class.getName());

    /** command line argument for nogui option */
    public static final String NOGUI_ARG = "nogui";

    /** command line argument for minimum service mode */
    public static final String MIN_MODE_ARG = "minmode";

    /** command line argument to launch an external app */
    public static final String LAUNCH_EXT_ARG = "launchExternal";

    /** property placed in System.properties for nogui setting */
    public static final String NOGUI_PROP = "boot.nogui";

    /** command line argument for passing additional system properties */
    public static final String PROPS_FILE_ARG = "propfile";

    /** command line argument for bootdir */
    public static final String BOOTDIR_ARG = "bootdir";

    /** property placed in System.properties for boot dir */
    public static final String BOOTDIR_PROP = "boot.dir";

    /** property used to turn off built-in security manager */
    public static final String NO_SECURITY_PROP = "boot.no.security";

    /** property used to turn off built-in security manager */
    public static final String NO_SECURITY_WARNING_PROP = "boot.no.security.warning";

    /** property used to turn off auto trusting of boot dir code. Defaults to true **/
    public static final String TRUST_BOOT_DIR = "trust.boot.dir";

    /** property used to turn off built-in caching, defaults to true*/
    public static final String BOOT_USE_CACHE = "boot.use.cache";

    /** property used to set the caching dir, defaults to {app.base.dir}/cache */
    public static final String BOOT_CACHE_DIR = "boot.cache.dir";

    /** property used to set whether we should check for UI. Defaults to true, if nogui mode this property has no effect */
    public static final String CHECK_UI = "boot.checkui";

    /** Short name of application bootstrap is booting */
    public static final String APP_NAME = "app.name";

    /** Descriptive Name of application  */
    public static final String APP_DISPLAY_NAME = "app.display.name";

    /** url to app icon relative to bootdir */
    public static final String APP_ICON = "app.icon";

    /** boot properties option for native L&F can be true false. Defaults to true */
    public static final String NATIVE_LAF_PROP = "native.laf";

    /** property name of application dir for user settings */
    public static final String APP_BASEDIR = "app.base.dir";

    /** property name of url to use to Check Online */
    public static final String CHECK_ONLINE_URL_PROP = "check.online.url";

    /** Default URL used to test Online Connection */
    public static final String DEFAULT_CHECK_ONLINE_URL = "http://www.google.com";

    /** File name of offline properties */
    public static final String OFFLINE_PROPS_FILE = "offline.properties";

    /** property name for offline status */
    public static final String OFFLINE_PROP = "offline";

    /** arg name for offline status */
    public static final String OFFLINE_ARG = "offline";

    /** Constant for WINDOWS_OS */
    public static final String WINDOWS_OS = NativeLibDesc.WINDOWS_OS;

    /** Constant for MAC_OS */
    public static final String MAC_OS = NativeLibDesc.MAC_OS;

    /** Constant for LINUX_OS */
    public static final String LINUX_OS = NativeLibDesc.LINUX_OS;

    private static boolean initialized_flag = false;

    private static boolean quicklaunch_flag = false;

    private static URL jnlpCodeBase = null;

    private static boolean nogui = false;

    private static boolean minMode = false;

    private static boolean offline = false;

    private static boolean launch_external = false;

    private static Properties bootProps = new Properties();

    private static Properties argProps = new Properties();

    private static File bootDir;

    private static ThreadGroup bootGroup;

    private static String appName;

    private static String appDisplayName;

    private static boolean lookAndFeelInstalled = false;

    private static CacheManager cacheManager;

    private static ImageIcon appIcon;

    private static ServiceManager serviceManager;

    private static Vector offlineListeners = new Vector();

    /**
    * Setup the Security Manager
    */
    private static void setupSecurity() {
        String noSecurityProp = bootProps.getProperty(NO_SECURITY_PROP);
        if (noSecurityProp != null && noSecurityProp.equals("true") && !isQuickLaunch()) {
            logger.log(Level.WARNING, "BUILT IN SECURITY MANAGER is DISABLED !!!");
            return;
        }
        bootGroup = Thread.currentThread().getThreadGroup();
        BootSecurityManager sm = new BootSecurityManager();
        Policy bootPolicy = new BootPolicy(sm);
        Policy.setPolicy(bootPolicy);
        System.setSecurityManager(sm);
    }

    /**
    * Check to make sure there is a Security Manager Installed
    */
    private static void checkSecurityManager() {
        if (System.getSecurityManager() != null) {
            return;
        }
        logger.log(Level.WARNING, "********************************/n NO SECURITY MANAGER INSTALLED !!!/n********************************");
        if (Boot.isHeadless()) {
            return;
        }
        String noSecurityWarning = bootProps.getProperty(NO_SECURITY_WARNING_PROP);
        if (Boolean.parseBoolean(noSecurityWarning)) {
            return;
        }
        invokeAndWait(new Runnable() {

            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(Boot.class.getClassLoader());
                    String title = Resources.bundle.getString("boot.security.warning.title");
                    title = MessageFormat.format(title, Boot.getAppDisplayName());
                    String subtitle = Resources.bundle.getString("boot.no.security.subtitle");
                    String msg = Resources.bundle.getString("boot.no.security.warning");
                    DialogDescriptor desc = new DialogDescriptor();
                    desc.setWindowTitle(title);
                    desc.setTitle(title);
                    desc.setSubtitle(subtitle);
                    desc.setMessage(msg);
                    desc.setMessageType(DialogManager.WARNING_MSG);
                    desc.setType(DialogManager.YES_NO);
                    desc.setWidth(300);
                    desc.setHeight(300);
                    int result = DialogManager.showDialog(desc);
                    if (result == DialogManager.NO) {
                        Boot.endSession(true);
                    }
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, t.getMessage(), t);
                }
            }
        });
    }

    /**
    * Return the BootStrap Thread Group used to 
    */
    public static ThreadGroup getThreadGroup() {
        return bootGroup;
    }

    /** 
    * Return true if BootStrap is in Debug Mode
    */
    public static boolean isDebug() {
        return false;
    }

    /**
    * return true if BootStrap is in Headless mode
    */
    public static boolean isHeadless() {
        return nogui;
    }

    /**
    * return true if BootStrap is in min-mode
    */
    public static boolean isMinMode() {
        return minMode;
    }

    /** 
    * Get the Application Name
    */
    public static String getAppName() {
        return appName;
    }

    /** 
    * Get the Application Display Name
    */
    public static String getAppDisplayName() {
        return appDisplayName;
    }

    /**
    * Get the Boot Dir
    */
    public static File getBootDir() {
        return bootDir;
    }

    /**
    * Main Entry point for Boot Environment
    */
    public static void main(String args[]) {
        if (!isInitialized()) {
            setArgProperties(args);
            initialize();
        }
    }

    /** 
    * Return true if the BootStrap has been Initialized
    */
    public static boolean isInitialized() {
        return initialized_flag;
    }

    /** 
    * Return true if the BootStrap has been launched from WebStart
    */
    public static boolean isQuickLaunch() {
        return quicklaunch_flag;
    }

    /**
    * Initialize the BootStrap. 
    * This will setup the Logging, Cache Manager, Security Manager, and boot services etc.
    *
    * If the Environment is already initialized then an error will be displayed.
    */
    public static void initialize() {
        if (isInitialized()) {
            showError("Initialize Error", "Cann't execute Boot.main. BootStrap already loaded.", null);
            return;
        }
        try {
            initialized_flag = true;
            System.out.println("Xito BootStrap Version:" + Boot.class.getPackage().getImplementationVersion());
            System.out.println("==============================================");
            checkQuickLaunch();
            processBootProperties();
            setupLogging();
            setupSecurity();
            setupCache();
            setupAppIcon();
            ProxyConfig.initialize();
            checkOnline();
            if (isQuickLaunch()) {
                QuickLaunch.init(jnlpCodeBase);
                return;
            }
            serviceManager = new ServiceManager();
            serviceManager.startAllServices();
            checkSecurityManager();
            processLaunchExternal();
            checkUIAvailable();
        } catch (Throwable t) {
            String msg = t.getMessage();
            if (msg == null || msg.equals("")) {
                msg = "Unknown Error occured.";
            }
            Boot.shutdownError(msg, t);
        }
    }

    /**
    * When running in QuickLaunch mode we need to check that the local
    * installed copy of BootStrap is up to date. If not update it and
    * then launch that BootStrap in a new Process
    */
    private static void updateLocalBootStrap() {
        showError("BootStrap", "Update Check!", null);
    }

    /**
    * Checks to see if BootStrap is being started from Java WebStart and then
    * will install itself on the local machine and then launch itself
    */
    private static void checkQuickLaunch() {
        Reflection reflectKit = Reflection.getToolKit();
        Object basicService;
        try {
            Class jnlpSrvMrgClass = Class.forName("javax.jnlp.ServiceManager");
            basicService = reflectKit.callStatic(jnlpSrvMrgClass, "lookup", "javax.jnlp.BasicService");
            jnlpCodeBase = (URL) reflectKit.call(basicService, "getCodeBase");
            offline = reflectKit.callBoolean(basicService, "isOffline");
            quicklaunch_flag = true;
            bootProps = new Properties();
            bootProps.setProperty(APP_NAME, "xito_quicklaunch");
            String displayName = System.getProperty("quicklaunch.app.display.name");
            if (displayName == null) displayName = "Xito QuickLaunch";
            bootProps.setProperty(APP_DISPLAY_NAME, displayName);
        } catch (Exception exp) {
        }
    }

    /**
    * Get the Stored Offline Status. This is the status the application was last 
    * in when it was last executed
    */
    private static boolean getStoredOfflineStatus() {
        File file = new File(getUserAppDir(), OFFLINE_PROPS_FILE);
        Properties props = new Properties();
        props.setProperty(OFFLINE_PROP, (isOffline() ? "true" : "false"));
        try {
            props.load(new FileInputStream(file));
            String status = props.getProperty(OFFLINE_PROP);
            if (status != null && status.equals("true")) {
                return true;
            } else {
                return false;
            }
        } catch (IOException ioExp) {
            logger.warning("Error Storing Offline Status:" + ioExp.getMessage());
        }
        return false;
    }

    /**
    * Store the Current Offline Status into a Properties file
    */
    private static void storeOfflineStatus() {
        File file = new File(getUserAppDir(), OFFLINE_PROPS_FILE);
        Properties props = new Properties();
        props.setProperty(OFFLINE_PROP, (isOffline() ? "true" : "false"));
        logger.info("Storing Offline Properties");
        try {
            FileOutputStream out = new FileOutputStream(file);
            props.store(out, null);
            out.close();
        } catch (IOException ioExp) {
            logger.warning("Error Storing Offline Status:" + ioExp.getMessage());
        }
    }

    /**
    * Checks to see if the Machine is Online. Shows a dialog while this is happening 
    * so the user can just choose to go into Offline Mode
    */
    private static void checkOnline() {
        if (isOffline()) {
            return;
        }
        String checkURL = System.getProperty(CHECK_ONLINE_URL_PROP);
        if (checkURL == null) {
            checkURL = DEFAULT_CHECK_ONLINE_URL;
        }
        OnlineTask task = new OnlineTask(checkURL);
        if (Boot.isHeadless()) {
            try {
                Thread checkThread = new Thread(task);
                checkThread.start();
                checkThread.join();
            } catch (Exception exp) {
                logger.log(Level.WARNING, exp.getMessage(), exp);
            }
            return;
        }
        ProgressDialogDescriptor desc = new ProgressDialogDescriptor();
        desc.setTitle(Resources.bundle.getString("check.online.dialog.title"));
        desc.setSubtitle(Resources.bundle.getString("check.online.dialog.subtitle"));
        desc.setMessage(Resources.bundle.getString("check.online.dialog.description"));
        boolean previousOffline = getStoredOfflineStatus();
        String btnText = null;
        if (previousOffline) {
            btnText = Resources.bundle.getString("check.online.dialog.previous.offline");
        } else {
            btnText = Resources.bundle.getString("check.online.dialog.previous.online");
        }
        int OFFLINE_MODE = 999;
        desc.setButtonTypes(new ButtonType[] { new ButtonType(btnText, OFFLINE_MODE) });
        desc.setShowButtonSeparator(true);
        desc.setWidth(375);
        desc.setHeight(225);
        desc.setRunnableTask(task);
        ProgressDialog dialog = new ProgressDialog(null, desc, true);
        dialog.setVisible(true);
        if (dialog.getResult() == OFFLINE_MODE) {
            dialog.cancelRunnableTask();
            setOffline(true);
        }
    }

    /**
    * Return true if the Application is in Offline Mode. False if Online
    */
    public static boolean isOffline() {
        return offline;
    }

    /**
    * Set to true if the application should be Offline. Set to false to be Online
    */
    public static void setOffline(boolean offlineMode) {
        offline = offlineMode;
        Iterator it = offlineListeners.iterator();
        while (it.hasNext()) {
            OfflineListener listener = (OfflineListener) it.next();
            if (listener != null && offline) {
                listener.offline();
            } else if (listener != null && !offline) {
                listener.online();
            }
        }
    }

    /**
    * Check to see if a UI is displayed
    */
    private static void checkUIAvailable() {
        String checkUIStr = System.getProperty(CHECK_UI);
        if (Boot.isHeadless() == false && (checkUIStr == null || checkUIStr.equals("true"))) {
            java.util.Timer timer = new java.util.Timer(true);
            timer.schedule(new TimerTask() {

                public void run() {
                    try {
                        BootSecurityManager sm = (BootSecurityManager) System.getSecurityManager();
                        if (sm.getExitClass() == null) sm.setExitClass(Boot.class);
                        if (sm.checkWindowVisible() == false) {
                            String msg = Resources.bundle.getString("boot.no.user.interface");
                            msg = MessageFormat.format(msg, Boot.getAppDisplayName());
                            Boot.shutdownError(msg, null);
                        } else {
                            logger.info("Visible User Interface Detected!");
                        }
                    } catch (ClassCastException badCast) {
                        logger.info("Not using BootSecurityManager. Can't check for No UI");
                    }
                }
            }, 20000);
        } else {
            logger.info("Visible User Interface Check Disabled!");
        }
    }

    /**
    * Return true if the BootStrap is in the mode of Launching an External App
    */
    public static boolean isLaunchingExternal() {
        return launch_external;
    }

    /**
    * Process any launchExternal Application to launch a Java Application
    * if this launch fails then end the session
    */
    private static void processLaunchExternal() {
        String externalFile = Boot.argProps.getProperty(LAUNCH_EXT_ARG);
        if (externalFile == null) {
            logger.info("No External File to Launch");
            return;
        }
        logger.info("Launching external file:" + externalFile);
        launch_external = true;
        try {
            File extFile = new File(externalFile);
            String type = extFile.getName();
            if (type.lastIndexOf('.') > -1) {
                type = type.substring(type.lastIndexOf('.') + 1);
                type = type.trim();
            } else {
                type = "";
            }
            logger.info("Getting launcher for type:" + type);
            FileInputStream in = new FileInputStream(extFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int cbytes = in.read(buf);
            while (cbytes != -1) {
                out.write(buf, 0, cbytes);
                cbytes = in.read(buf);
            }
            in.close();
            byte[] data = out.toByteArray();
            out.close();
            AppDesc appDesc = AppLauncher.getAppDesc(type, data);
            AppLauncher appLauncher = AppLauncher.getLauncher(type.toLowerCase());
            if (appLauncher != null && appDesc != null) {
                appLauncher.launchInternal(appDesc, false);
            } else {
                throw new Exception("Error Launching External Application: The Launcher type is unknown");
            }
        } catch (Throwable exp) {
            logger.log(Level.SEVERE, exp.getMessage(), exp);
            String expMsg = exp.getMessage();
            if (expMsg == null) expMsg = "unknown";
            String msg = MessageFormat.format(Resources.bundle.getString("launch.external.error"), expMsg);
            Boot.shutdownError(msg, exp);
        }
    }

    /**
    * End This Session
    * @param force if true does not ask the user if the session should end
    */
    public static void endSession(boolean force) {
        storeOfflineStatus();
        if (serviceManager != null) {
            serviceManager.endSession();
        }
        System.exit(0);
    }

    /**
    * Get the Directory where Services are cached
    */
    private static void setupCache() {
        URLStreamManager.main(new String[0]);
        String useCacheStr = System.getProperty(BOOT_USE_CACHE);
        boolean cacheDisabled = false;
        if (useCacheStr != null && useCacheStr.equals("false")) {
            cacheDisabled = true;
            logger.info("CACHE MANAGER IS DISABLED !!!");
        }
        String cacheDirStr = System.getProperty(BOOT_CACHE_DIR);
        File cacheDir = null;
        if (cacheDirStr != null) {
            cacheDir = new File(cacheDirStr);
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdir()) {
                    cacheDir = null;
                    logger.warning("COULD NOT USE SPECIFIED CACHE DIR: " + cacheDir);
                }
            }
        }
        if (cacheDir == null) {
            cacheDir = new File(getUserAppDir(), "cache");
        }
        logger.info("Using Cache Dir: " + cacheDir.toString());
        cacheManager = new CacheManager(cacheDir, cacheDisabled);
    }

    /**
    * Get the currently installed ServiceManager
    */
    public static ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
    * Get the currently installed CacheManager
    */
    public static CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
    * Check for logging File in Boot Directory
    */
    private static void setupLogging() {
        File logConfigFile = new File("./logging.properties");
        if (!logConfigFile.exists()) {
            logConfigFile = new File(bootDir, "/logging.properties");
        }
        if (!logConfigFile.exists()) return;
        try {
            FileInputStream in = new FileInputStream(logConfigFile);
            LogManager.getLogManager().readConfiguration(in);
        } catch (IOException ioExp) {
            logger.log(Level.SEVERE, "Error reading " + logConfigFile.getAbsolutePath(), ioExp);
        }
    }

    /**
    * Process Boot Properties using Arguments
    */
    private static void processBootProperties() {
        appName = null;
        String bootDir_str = argProps.getProperty(BOOTDIR_ARG);
        if (bootDir_str == null) {
            bootDir_str = System.getProperty(BOOTDIR_PROP);
            if (bootDir_str == null) {
                bootDir_str = ".";
            }
        }
        logger.info("bootdir arg:" + bootDir_str);
        try {
            bootDir = new File(bootDir_str);
            bootDir_str = bootDir.getCanonicalPath();
            bootDir = new File(bootDir_str);
            if (!bootDir.exists() || !bootDir.isDirectory()) {
                shutdownError(Resources.bundle.getString("no.boot.dir.error"), null);
            }
            logger.info("bootdir:" + bootDir_str);
            if (isQuickLaunch() == false) {
                bootProps.load(new FileInputStream(new File(bootDir_str, "boot.properties")));
            }
            logProperties(bootProps, "Boot Properties", Level.INFO);
            updateSystemProps(bootProps);
            logProperties(System.getProperties(), "System Properties", Level.INFO);
        } catch (FileNotFoundException exp) {
            shutdownError(Resources.bundle.getString("boot.properties.not.found.error"), exp);
        } catch (IOException ioExp) {
            shutdownError(Resources.bundle.getString("boot.properties.read.error"), ioExp);
        }
        appName = bootProps.getProperty(APP_NAME);
        if (appName == null) {
            shutdownError(Resources.bundle.getString("no.app.name.error"), null);
        }
        appDisplayName = bootProps.getProperty(APP_DISPLAY_NAME);
        if (appDisplayName == null) appDisplayName = appName;
        if (argProps.containsKey(NOGUI_ARG) && argProps.getProperty(NOGUI_ARG).equals("true")) {
            nogui = true;
            System.setProperty(NOGUI_PROP, "true");
        }
        if (bootProps.containsKey(NOGUI_PROP) && bootProps.getProperty(NOGUI_PROP).equals("true")) {
            nogui = true;
            System.setProperty(NOGUI_PROP, "true");
        }
        String appBaseDir = System.getProperty(APP_BASEDIR);
        File baseDir = null;
        if (appBaseDir == null) {
            baseDir = new File(System.getProperty("user.home"), '.' + appName);
        } else {
            baseDir = new File(appBaseDir);
        }
        if (baseDir.exists() == false) {
            if (baseDir.mkdir() == false) {
                String msg = MessageFormat.format(Resources.bundle.getString("no.user.settings.dir.error"), baseDir.toString());
                shutdownError(msg, null);
            }
        } else if (baseDir.isFile()) {
            String msg = MessageFormat.format(Resources.bundle.getString("no.user.settings.dir.error"), baseDir.toString());
            shutdownError(msg, null);
        }
        System.setProperty(APP_BASEDIR, baseDir.toString());
        logger.info("App Base Dir: " + baseDir.toString());
        if (!bootProps.containsKey(NATIVE_LAF_PROP) || bootProps.getProperty(NATIVE_LAF_PROP).equals("true")) {
            setupNativeLookAndFeel();
        }
        String windowWarning = Resources.bundle.getString("restricted.window.banner");
        if (System.getProperty("awt.appletWarning") == null) {
            System.setProperty("awt.appletWarning", windowWarning);
        }
        String minModeStr = getArgProperties().getProperty(MIN_MODE_ARG);
        if (minModeStr != null && minModeStr.equals("true")) minMode = true;
        String offlineStr = getArgProperties().getProperty(OFFLINE_ARG);
        if (offlineStr != null && offlineStr.equals("true")) {
            setOffline(true);
        }
        String propsFile = getArgProperties().getProperty(PROPS_FILE_ARG);
        if (propsFile != null) {
            try {
                File propFile = new File(propsFile);
                Properties props = new Properties();
                props.load(new FileInputStream(propFile));
                Enumeration ep = props.keys();
                while (ep.hasMoreElements()) {
                    String key = (String) ep.nextElement();
                    System.getProperties().put(key, props.getProperty(key));
                }
            } catch (IOException ioExp) {
                logger.log(Level.SEVERE, "Error loading system properties from file:" + propsFile, ioExp);
            }
        }
    }

    /**
    * Return the value of a boot property
    * @param name
    * @param defaultValue
    * @return
    */
    public static String getBootProperty(String name, String defaultValue) {
        return bootProps.getProperty(name, defaultValue);
    }

    /**
    * Log Properties to a logger
    */
    private static void logProperties(Properties props, String title, Level level) {
        if (logger.isLoggable(level)) {
            StringBuffer logMsg = new StringBuffer();
            logMsg.append(title + ":\n===================\n");
            Iterator propNames = props.keySet().iterator();
            while (propNames.hasNext()) {
                String propName = (String) propNames.next();
                logMsg.append(propName + "=" + props.getProperty(propName) + "\n");
            }
            logger.log(level, logMsg.toString());
        }
    }

    /** 
    * Setup the Swing LAF to use the Native Platform LAF.
    * This is the default unless boot.properties contains nativeLAF=false
    */
    private static void setupNativeLookAndFeel() {
        Class wlafClass = null;
        if (System.getProperty("os.name").startsWith("Windows")) {
            try {
                wlafClass = Class.forName("net.java.plaf.windows.WindowsLookAndFeel");
            } catch (ClassNotFoundException exp) {
            }
        }
        try {
            if (wlafClass != null) {
                UIManager.setLookAndFeel("net.java.plaf.windows.WindowsLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            lookAndFeelInstalled = true;
        } catch (Exception exp) {
            logger.log(Level.WARNING, "Could not load System Look and Feel Class.", exp);
        }
    }

    /**
    * Update the System properties with the properties provided
    */
    private static void updateSystemProps(Properties props) {
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String keyName = (String) keys.nextElement();
            System.setProperty(keyName, props.getProperty(keyName));
        }
    }

    /**
    * There was an error during boot. display error and exit
    */
    private static void shutdownError(String error, Throwable exp) {
        showError(Resources.bundle.getString("boot.error.title"), error, exp);
        System.err.println(error);
        endSession(true);
    }

    /**
    * Set Arg Properties from Arguments in string array
    * Each argument name and value will be placed in the returned properties object
    * @param arguments
    */
    private static void setArgProperties(String args[]) {
        argProps = processArgs(args);
    }

    /**
    * Return the Arguments passed to Boot on Startup
    */
    public static Properties getArgProperties() {
        return argProps;
    }

    /**
    * Returns the userdir for this application.
    */
    public static File getUserAppDir() {
        String dir = System.getProperty("user.home");
        return new File(dir, '.' + getAppName());
    }

    /**
    * Return Current OS. Returns WINDOWS, MAC, LINUX constansts or
    * os.name from System property if not WINDOWS, MAC, or LINUX
    */
    public static String getCurrentOS() {
        return NativeLibDesc.currentOS();
    }

    /**
    * Get App Icon
    */
    public static ImageIcon getAppIcon() {
        return appIcon;
    }

    /**
    * Setup the AppIcon
    */
    private static void setupAppIcon() {
        String appIconStr = System.getProperty("app.icon");
        logger.log(Level.INFO, "app.icon:" + appIconStr);
        if (appIconStr == null) {
            appIcon = null;
            return;
        }
        try {
            URL iconURL = null;
            iconURL = new URL(Boot.getBootDir().toURL(), appIconStr);
            logger.log(Level.INFO, "Icon URL:" + iconURL.toString());
            if (appIconStr.startsWith("http") || appIconStr.startsWith("file")) {
                Boot.getCacheManager().downloadResource(iconURL, null);
                appIcon = new ImageIcon(Boot.getCacheManager().getCachedFileForURL(iconURL).toURL());
            } else {
                appIcon = new ImageIcon(iconURL);
            }
        } catch (Exception exp) {
            showError(Resources.bundle.getString("boot.error.title"), Resources.bundle.getString("icon.url.error"), exp);
            appIcon = null;
        }
        if (Boot.isHeadless() == false && appIcon != null) {
            javax.swing.JDialog d = new javax.swing.JDialog();
            ((java.awt.Frame) d.getOwner()).setIconImage(appIcon.getImage());
        }
    }

    /**
    * Get VM Version
    */
    public static String getVMVersion() {
        return System.getProperty("java.vm.version");
    }

    /**
    * Process Startup Arguments
    * Each argument name and value will be placed in the returned properties object
    * @param arguments
    */
    public static Properties processArgs(String args[]) {
        Properties startupArgs = new Properties();
        if (args == null) return startupArgs;
        String _name = null;
        String _value = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                if (_name != null) {
                    _value = "true";
                    i--;
                } else {
                    _name = args[i].substring(1);
                    if (i == args.length - 1) _value = "true";
                }
            } else if (_name != null) {
                _value = args[i];
            } else {
                _value = "true";
            }
            if (_name != null && _value != null) {
                startupArgs.put(_name, _value);
                _name = null;
                _value = null;
            }
        }
        return startupArgs;
    }

    /**
    * Add an OfflineListener
    */
    public static void addOfflineListener(OfflineListener listener) {
        if (!offlineListeners.contains(listener)) {
            offlineListeners.add(0, listener);
        }
    }

    /**
    * Remove an OfflineListener
    */
    public static void removeOfflineListener(OfflineListener listener) {
        offlineListeners.remove(listener);
    }

    /**
    * Show an Error in the BootStrap
    */
    public static void showError(final DialogDescriptor desc) {
        java.awt.EventQueue q = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
        logger.log(Level.SEVERE, desc.getTitle() + ":" + desc.getMessage(), desc.getException());
        if (!isHeadless() && q.isDispatchThread() == false) {
            invokeAndWait(new Runnable() {

                public void run() {
                    DialogManager.showDialog(desc);
                }
            });
        } else if (!isHeadless()) {
            DialogManager.showDialog(desc);
        }
    }

    /**
    * Show an Error in the BootStrap
    */
    public static void showError(final String title, final String msg, final Throwable exp) {
        final String specificTitle = (Boot.getAppDisplayName() != null) ? Boot.getAppDisplayName() : "Xito BootStrap" + ": " + title;
        logger.log(Level.SEVERE, title + ":" + msg, exp);
        java.awt.EventQueue q = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
        if (!isHeadless() && !q.isDispatchThread()) {
            invokeAndWait(new Runnable() {

                public void run() {
                    try {
                        Thread.currentThread().setContextClassLoader(Boot.class.getClassLoader());
                        DialogManager.showError(null, specificTitle, msg, exp);
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, t.getMessage(), t);
                    }
                }
            });
        } else if (!isHeadless() && q.isDispatchThread()) {
            DialogManager.showError(null, specificTitle, msg, exp);
        }
    }

    /**
    * Invoke a Thread in the Context of this BootStraps ThreadGroup and wait for
    * it to complete
    */
    public static void invokeAndWait(final Runnable runnable) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                Thread t = new Thread(getThreadGroup(), runnable);
                t.setContextClassLoader(Boot.class.getClassLoader());
                t.setDaemon(true);
                t.start();
                try {
                    t.join();
                } catch (Exception exp) {
                    logger.log(Level.SEVERE, exp.getMessage(), exp);
                }
                return null;
            }
        });
    }

    /**
    * Invoke a Thread in the Context of this BootStraps ThreadGroup return
    */
    public static void invokeLater(Runnable runnable) {
        Thread t = new Thread(getThreadGroup(), runnable);
        t.setContextClassLoader(Boot.class.getClassLoader());
        t.setDaemon(true);
        t.start();
    }

    /********************************************
    * Runnable Task to Check for Online Status
    *********************************************/
    private static class OnlineTask implements Runnable {

        private String checkURL;

        public OnlineTask(String url) {
            checkURL = url;
        }

        public void run() {
            try {
                URL url = new URL(checkURL);
                url.openConnection().getContent();
                Boot.setOffline(false);
            } catch (MalformedURLException badURL) {
                String title = Resources.bundle.getString("check.online.dialog.title");
                String msg = Resources.bundle.getString("check.online.bad.url.error");
                logger.log(Level.WARNING, msg);
                Boot.showError(title, msg, null);
            } catch (IOException ioExp) {
                ioExp.printStackTrace();
                Boot.setOffline(true);
            }
        }
    }
}
