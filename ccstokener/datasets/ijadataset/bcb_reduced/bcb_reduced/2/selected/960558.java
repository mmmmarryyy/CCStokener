package org.inigma.iniglet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.LogManager;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.inigma.iniglet.about.AboutData;
import org.inigma.iniglet.about.AboutWindow;
import org.inigma.iniglet.utils.DynamicListener;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * @author <a href="mailto:sejal@inigma.org">Sejal Patel</a>
 */
public class Main implements MainMBean, WrapperListener {

    private static final String INIGLET_CONFIG_DIR = System.getProperty("user.home") + System.getProperty("file.separator") + ".iniglet";

    private static final String INIGLET_PLUGINS = "plugins";

    private static final int MAX_CLIPBOARD_SIZE = 8192;

    private static final long CLIPBOARD_POLLING = 200;

    private static Log logger = LogFactory.getLog(Main.class);

    private static long clipboardScanTime = -1;

    private static Clipboard clipboard;

    private static Queue<Notification> notificationList = new ConcurrentLinkedQueue<Notification>();

    private static boolean notificationVisible = false;

    private static String clipboardBuffer;

    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        File configsDirectory = new File(System.getProperty("iniglet.config.dir", INIGLET_CONFIG_DIR));
        if (!configsDirectory.exists()) {
            configsDirectory.mkdirs();
        }
        System.setProperty("iniglet.config.dir", configsDirectory.getAbsolutePath());
        try {
            Class.forName("org.hsqldb.jdbcDriver");
        } catch (ClassNotFoundException e) {
            logger.error("Unable to initialize the hsqldb drivers", e);
        }
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Main instance = new Main();
        try {
            server.registerMBean(instance, new ObjectName(Main.class.getPackage().getName() + ":type=Main"));
        } catch (InstanceAlreadyExistsException e) {
            logger.error("TODO Describe Error", e);
        } catch (MBeanRegistrationException e) {
            logger.error("TODO Describe Error", e);
        } catch (NotCompliantMBeanException e) {
            logger.error("TODO Describe Error", e);
        } catch (MalformedObjectNameException e) {
            logger.error("TODO Describe Error", e);
        } catch (NullPointerException e) {
            logger.error("TODO Describe Error", e);
        }
        if (System.getProperty("daemon") != null) {
            WrapperManager.start(instance, args);
        }
        instance.loadPlugins();
        instance.trayItem.setVisible(true);
        instance.startEventLoop();
    }

    static void addNotification(Notification notification) {
        notificationList.add(notification);
    }

    static void setClipboardText(String text) {
        clipboardBuffer = text;
        clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
    }

    private Display display;

    private Shell shell;

    private Menu menu;

    private Tray tray;

    private TrayItem trayItem;

    private Collection<Iniglet> inigletList;

    private Configuration configuration;

    private final long clipboardPollingInterval;

    private final int clipboardMaxTextSize;

    private AboutWindow about;

    public int getClipboardMaxSize() {
        return clipboardMaxTextSize;
    }

    public long getClipboardPolling() {
        return clipboardPollingInterval;
    }

    private Main() {
        shell = new Shell(display);
        try {
            configuration = new Configuration(Main.class.getName());
            configuration.initializeSchema(Main.class.getResourceAsStream("/org/inigma/iniglet/main.sql"));
        } catch (SQLException e) {
            logger.error("Unable to initialize system configurations! Another instance must be running!", e);
            MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK | SWT.APPLICATION_MODAL);
            box.setText("Iniglet Startup Error");
            box.setMessage("Unable to initialize system configurations! Another instance must be running! See error log for more details");
            box.open();
            System.exit(1);
        }
        this.clipboardPollingInterval = configuration.getLong("clipboard.polling", CLIPBOARD_POLLING);
        this.clipboardMaxTextSize = configuration.getInt("clipboard.maxSize", MAX_CLIPBOARD_SIZE);
        inigletList = new TreeSet<Iniglet>(new Comparator<Iniglet>() {

            public int compare(Iniglet o1, Iniglet o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        display = Display.getDefault();
        clipboard = new Clipboard(display);
        menu = new Menu(shell, SWT.POP_UP);
        tray = display.getSystemTray();
        trayItem = new TrayItem(tray, SWT.NULL);
        Image icon = new Image(display, getClass().getResourceAsStream("/images/inigma.png"));
        trayItem.setImage(icon);
        trayItem.setVisible(true);
        trayItem.addListener(SWT.MenuDetect, new Listener() {

            public void handleEvent(Event e) {
                menu.setVisible(true);
            }
        });
        trayItem.addListener(SWT.DefaultSelection, new Listener() {

            public void handleEvent(Event e) {
                logger.error("Implement Plugins Enabler Listing!");
            }
        });
        AboutData aboutData = new AboutData();
        aboutData.setName("Iniglets");
        aboutData.setImage(new Image(display, getClass().getResourceAsStream("/images/inigma-logo.png")));
        aboutData.setIcon(icon);
        aboutData.setCopyright("2006-2008 Sejal Patel");
        aboutData.setLicense("/LICENSE.txt");
        aboutData.addCredits("Author", "Sejal Patel", "sejal@inigma.org");
        aboutData.addCredits("Contributor", "André Almeida Santos", "moimeme99@hotmail.com");
        Properties config = new Properties();
        try {
            config.load(getClass().getResourceAsStream("/config.properties"));
            aboutData.setVerison(config.getProperty("version", ""));
            aboutData.setDescription(config.getProperty("description", null));
            aboutData.setUrl(config.getProperty("url", null));
        } catch (IOException ignore) {
        }
        about = new AboutWindow(display, aboutData);
    }

    private void addMenuEntry(Iniglet callback) {
        callback.registerMenuEntry(menu);
    }

    private void addMenuExit() {
        new MenuItem(menu, SWT.SEPARATOR);
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText("About");
        item.addListener(SWT.Selection, new DynamicListener(this, "handleEventAbout"));
        item = new MenuItem(menu, SWT.PUSH);
        item.setText("Exit");
        item.addListener(SWT.Selection, new DynamicListener(this, "handleEventExit"));
    }

    private void checkClipboard() {
        if (System.currentTimeMillis() - clipboardScanTime < clipboardPollingInterval) {
            return;
        }
        clipboardScanTime = System.currentTimeMillis();
        TextTransfer tt = TextTransfer.getInstance();
        String text = (String) clipboard.getContents(tt, DND.CLIPBOARD);
        if (text == null || text.length() > clipboardMaxTextSize) {
            return;
        }
        if (!text.equals(clipboardBuffer)) {
            clipboardBuffer = text;
            for (Iniglet iniglet : inigletList) {
                iniglet.onClipboardChange(text);
            }
        }
    }

    @SuppressWarnings("unused")
    private void handleEventAbout(Event event) {
        about.setVisible(true);
    }

    @SuppressWarnings("unused")
    private void handleEventExit(Event event) {
        for (Iniglet iniglet : inigletList) {
            iniglet.onExit();
        }
        System.exit(0);
    }

    private void loadPlugins() {
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar");
            }
        };
        File pluginsDirectory = new File(System.getProperty("iniglet.plugins", INIGLET_PLUGINS));
        File pluginsLibrariesDirectory = new File(pluginsDirectory, "lib");
        logger.info("Scanning for plugins at " + pluginsDirectory.getAbsolutePath());
        File[] jars = pluginsDirectory.listFiles(filter);
        if (jars == null) {
            jars = new File[] {};
        }
        File[] libraries = pluginsLibrariesDirectory.listFiles(filter);
        if (libraries == null) {
            libraries = new File[] {};
        }
        loadPlugins(jars, libraries);
        addMenuExit();
    }

    @SuppressWarnings("unchecked")
    private void loadPlugins(File[] jars, File[] libraries) {
        ArrayList<URL> libraryUrls = new ArrayList<URL>();
        for (File library : libraries) {
            try {
                libraryUrls.add(library.toURI().toURL());
            } catch (MalformedURLException e) {
                logger.error("Unable to load plugin library " + library, e);
            }
        }
        URLClassLoader libraryClassLoader = new URLClassLoader(libraryUrls.toArray(new URL[] {}));
        final Splash splash = new Splash(Display.getDefault(), jars.length);
        for (int i = 0; i < jars.length; i++) {
            splash.setProgress(i);
            logger.info("Loading library " + jars[i].getAbsolutePath());
            try {
                URL url = jars[i].toURI().toURL();
                try {
                    JarInputStream in = new JarInputStream(url.openStream());
                    JarEntry entry = null;
                    while ((entry = in.getNextJarEntry()) != null) {
                        if (!entry.getName().matches(".*class$")) {
                            continue;
                        }
                        String className = entry.getName();
                        className = className.substring(0, className.lastIndexOf("."));
                        className = className.replace("/", ".");
                        try {
                            URLClassLoader classLoader = new URLClassLoader(new URL[] { url }, libraryClassLoader);
                            Class instance = Class.forName(className, true, classLoader);
                            if (Iniglet.class.isAssignableFrom(instance) && !Modifier.isAbstract(instance.getModifiers())) {
                                logger.info("Iniglet: " + className);
                                Iniglet iniglet = null;
                                try {
                                    iniglet = (Iniglet) instance.newInstance();
                                    inigletList.add(iniglet);
                                    splash.setProgress(i + 1);
                                } catch (IllegalArgumentException e) {
                                    logger.warn("Illegal constructor for " + instance.getCanonicalName(), e);
                                } catch (InstantiationException e) {
                                    logger.warn("Unable to instantiate " + instance.getCanonicalName(), e);
                                } catch (IllegalAccessException e) {
                                    logger.warn("Illegal constructor access to " + instance.getCanonicalName(), e);
                                } catch (Throwable t) {
                                    logger.warn("Iniglet Failure " + instance.getCanonicalName(), t);
                                    t.printStackTrace();
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            logger.warn("Unable to load expected plugin " + className, e);
                        } catch (IllegalArgumentException e) {
                            logger.warn("Illegal constructor argument to " + className, e);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to scan " + url.getFile(), e);
                }
            } catch (MalformedURLException e) {
                logger.warn("Unable to load " + jars[i].getAbsolutePath(), e);
            }
        }
        for (Iniglet iniglet : inigletList) {
            addMenuEntry(iniglet);
        }
        display.asyncExec(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(getSplashDelay());
                } catch (InterruptedException e) {
                    logger.error("TODO Describe Error", e);
                } finally {
                    splash.close();
                }
            }
        });
    }

    private void startEventLoop() {
        while (!tray.isDisposed()) {
            try {
                if (!display.readAndDispatch()) {
                    display.sleep();
                    checkNotifications();
                    checkClipboard();
                }
            } catch (Exception e) {
                logger.error("Unhandled runtime exception thrown!", e);
            }
        }
        display.dispose();
    }

    private void checkNotifications() {
        if (!notificationVisible) {
            Notification notification = notificationList.poll();
            if (notification != null) {
                if (!notification.isAutoHide()) {
                    notificationVisible = true;
                }
                ToolTip tip = new ToolTip(shell, SWT.BALLOON | notification.getStyle());
                tip.addListener(SWT.Selection, new Listener() {

                    public void handleEvent(Event event) {
                        notificationVisible = false;
                    }
                });
                tip.setText(notification.getTitle());
                tip.setMessage(notification.getMessage());
                tip.setAutoHide(notification.isAutoHide());
                trayItem.setToolTip(tip);
                tip.setVisible(true);
            }
        }
    }

    public long getSplashDelay() {
        return configuration.getLong("splash.delay", 1000);
    }

    public int stop(int code) {
        logger.warn("What is stop: " + code);
        for (Iniglet iniglet : inigletList) {
            iniglet.onExit();
        }
        return 0;
    }

    public Integer start(String[] args) {
        logger.warn("What is start: " + args.length);
        return null;
    }

    public void controlEvent(int code) {
        logger.warn("What is a control event: " + code);
    }
}
